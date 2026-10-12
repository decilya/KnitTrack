package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
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
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Запрос одного проекта по id.
     *
     * @param id Идентификатор проекта.
     * @return [Result.Success] с проектом, [Result.Error] с
     *   [AppError.NotFoundError] если не найден, или [AppError.DatabaseError].
     */
    override suspend fun getProjectById(id: Long): Result<Project> = try {
        val entity = projectDao.getProjectById(id)
        if (entity != null) Result.Success(entity.toDomain())
        else Result.Error(AppError.NotFoundError("Проект id=$id не найден"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Добавляет новый проект. Проставляет актуальный updatedAt и PENDING.
     *
     * @param project Domain-модель проекта.
     * @return [Result.Success] с id вставленной строки или [Result.Error].
     */
    override suspend fun addProject(project: Project): Result<Long> = try {
        val entity = project.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        Result.Success(projectDao.insertProject(entity))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
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
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        val rows = projectDao.updateProject(entity)
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Проект id=${project.id} не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Удаляет проект по id. Каскад на сессии — FK CASCADE в БД.
     *
     * Идемпотентно: если проекта с таким id уже нет, возвращается Success,
     * а не NotFoundError. Защищает UI от гонки между нажатием «Удалить»
     * и параллельным удалением (например, после перезагрузки списка).
     *
     * Одним SQL-запросом DELETE без предварительного чтения entity.
     *
     * @param id Идентификатор проекта.
     * @return [Result.Success] или [Result.Error].
     */
    override suspend fun deleteProject(id: Long): Result<Unit> = try {
        projectDao.deleteProjectById(id)
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
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
    override suspend fun updateTotalTime(
        projectId: Long,
        duration: Duration
    ): Result<Unit> = try {
        val rows = projectDao.updateTotalTime(
            projectId = projectId,
            durationSeconds = duration.seconds,
            updatedAt = System.currentTimeMillis()
        )
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Проект id=$projectId не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }
}
