package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.StitchCount
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import timber.log.Timber

/**
 * Реализация [ProjectRepository], инкапсулирующая логику работы с [ProjectDao].
 * Отвечает за преобразование Entity в Domain-модели и обратно.
 *
 * Value Objects не покидают Domain слой — на границе с Room разворачиваются
 * в примитивы. UPDATE-методы проверяют rows affected: 0 строк = запись
 * не найдена, возвращаем [AppError.NotFoundError].
 *
 * Все catch-блоки сначала отлавливают [CancellationException] и пробрасывают
 * её дальше: отмена корутины — не ошибка, а сигнал. Без этого
 * viewModelScope.cancel() превращался бы в DatabaseError и ломал бы
 * cleanup в finally-блоках.
 */
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val timeProvider: TimeProvider
) : ProjectRepository {

    /**
     * Реактивный поток всех проектов, сначала новые.
     *
     * @return Flow списка Domain-проектов.
     */
    override fun getAllProjectsFlow(): Flow<List<Project>> =
        projectDao.getAllProjectsFlow().map { list -> list.map { it.toDomain() } }

    /**
     * Разовый запрос всех проектов.
     *
     * @return [Result.Success] со списком или [Result.Error] с [AppError.DatabaseError].
     */
    override suspend fun getAllProjects(): Result<List<Project>> = try {
        Result.Success(projectDao.getAllProjects().map { it.toDomain() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "getAllProjects failed")
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Запрос одного проекта по id.
     *
     * @param id Идентификатор проекта.
     * @return [Result.Success] с проектом, [Result.Error] с
     *   [AppError.NotFoundError] если не найден, или [AppError.DatabaseError].
     */
    override suspend fun getProjectById(id: ProjectId): Result<Project> = try {
        val entity = projectDao.getProjectById(id.value)
        if (entity != null) Result.Success(entity.toDomain())
        else Result.Error(AppError.NotFoundError("Проект id=${id.value} не найден"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "getProjectById id=%d failed", id.value)
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Добавляет новый проект. Проставляет актуальный updatedAt и PENDING.
     *
     * @param project Domain-модель проекта.
     * @return [Result.Success] с id вставленной строки или [Result.Error].
     */
    override suspend fun addProject(project: Project): Result<ProjectId> = try {
        val entity = project.toEntity().copy(
            updatedAt = timeProvider.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        val newId = projectDao.insertProject(entity)
        if (newId <= 0L) {
            Result.Error(AppError.DatabaseError(
                IllegalStateException("insertProject вернул невалидный id=$newId")
            ))
        } else {
            Result.Success(ProjectId.create(newId))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "addProject failed: category=%s", project.category)
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Обновляет существующий проект (полная замена строки по id).
     *
     * ВАЖНО: поля updatedAt и syncStatus входной модели игнорируются —
     * репозиторий проставляет их сам (now() и PENDING).
     *
     * @param project Domain-модель с актуальными полями.
     * @return [Result.Success] если обновлена 1 строка, [Result.Error] с
     *   [AppError.NotFoundError] если запись не найдена, или [AppError.DatabaseError].
     */
    override suspend fun updateProject(project: Project): Result<Unit> = try {
        val entity = project.toEntity().copy(
            updatedAt = timeProvider.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        val rows = projectDao.updateProject(entity)
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Проект id=${project.id.value} не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "updateProject id=%d failed", project.id.value)
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Удаляет проект по id. Каскад на сессии — FK CASCADE в БД.
     *
     * rows=0 → [AppError.NotFoundError]. Идемпотентность — на уровне
     * вызывающего кода: повторный вызов после успешного удаления
     * получает NotFoundError, а не Success.
     *
     * Одним SQL-запросом DELETE без предварительного чтения entity.
     *
     * @param id Идентификатор проекта.
     * @return [Result.Success] если удалена 1 строка, [Result.Error] с
     *   [AppError.NotFoundError] если строки не было, или [AppError.DatabaseError].
     */
    override suspend fun deleteProject(id: ProjectId): Result<Unit> = try {
        val rows = projectDao.deleteProjectById(id.value)
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Проект id=${id.value} не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "deleteProject id=%d failed", id.value)
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Атомарный инкремент total_time_seconds.
     *
     * @param projectId Идентификатор проекта.
     * @param duration Добавляемое время (VO [Duration] гарантирует ≥ 0).
     * @return [Result.Success], [Result.Error] с [AppError.NotFoundError]
     *   если проекта нет, или [AppError.DatabaseError].
     */
    override suspend fun updateProjectTotals(
        projectId: ProjectId,
        duration: Duration,
        rows: Int
    ): Result<Unit> = try {
        val affected = projectDao.incrementProjectTotals(
            projectId = projectId.value,
            durationSeconds = duration.seconds,
            rows = rows,
            updatedAt = timeProvider.currentTimeMillis()
        )
        if (affected == 0) {
            Result.Error(AppError.NotFoundError("Проект id=${projectId.value} не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "updateProjectTotals projectId=%d sec=%d failed", projectId.value, duration.seconds)
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Обновляет статус синхронизации проекта.
     *
     * @param projectId Идентификатор проекта.
     * @param status Новый статус.
     * @return [Result.Success], [Result.Error] с [AppError.NotFoundError]
     *   если проекта нет, или [AppError.DatabaseError].
     */
    override suspend fun updateSyncStatus(
        projectId: ProjectId,
        status: SyncStatus
    ): Result<Unit> = try {
        val rows = projectDao.updateSyncStatus(projectId.value, status.name)
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Проект id=${projectId.value} не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "updateSyncStatus projectId=%d failed", projectId.value)
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Атомарно обновляет прогресс по схеме.
     *
     * @param projectId Идентификатор проекта.
     * @param currentPatternRowId Текущий ряд схемы (null — нет прогресса).
     * @param currentRowNumber Денормализованный номер ряда для UI badge.
     * @param currentStitch Текущая петля (0 до per-stitch tracking, P3).
     * @return [Result.Success], [Result.Error] с [AppError.NotFoundError]
     *   если проекта нет, или [AppError.DatabaseError].
     */
    override suspend fun updateProjectProgress(
        projectId: ProjectId,
        currentPatternRowId: PatternRowId?,
        currentRowNumber: RowCount,
        currentStitch: StitchCount
    ): Result<Unit> = try {
        val rows = projectDao.updateProjectProgress(
            projectId = projectId.value,
            currentPatternRowId = currentPatternRowId?.value,
            currentRowNumber = currentRowNumber.value,
            currentStitch = currentStitch.value,
            updatedAt = timeProvider.currentTimeMillis()
        )
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Проект id=${projectId.value} не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "updateProjectProgress projectId=%d failed", projectId.value)
        Result.Error(AppError.DatabaseError(e))
    }
}
