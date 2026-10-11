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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [ProjectRepository], инкапсулирующая логику работы с [ProjectDao].
 * Отвечает за преобразование Entity в Domain-модели и обратно.
 *
 * Value Objects не покидают Domain слой — на границе с Room разворачиваются
 * в примитивы (Long, String). DAO не знает про VO.
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    /**
     * Реактивный поток всех проектов, отсортированных по дате создания
     * (сначала новые). Каждая Entity маппится в Domain-модель [Project].
     *
     * @return Flow списка Domain-проектов.
     */
    override fun getAllProjectsFlow(): Flow<List<Project>> =
        projectDao.getAllProjectsFlow().map { list -> list.map { it.toDomain() } }

    /**
     * Разовый запрос всех проектов.
     *
     * @return [Result.Success] со списком проектов или [Result.Error]
     *   с [AppError.DatabaseError] при сбое SQLite.
     */
    override suspend fun getAllProjects(): Result<List<Project>> = try {
        Result.Success(projectDao.getAllProjects().map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Запрос одного проекта по id.
     *
     * @param id Идентификатор проекта.
     * @return [Result.Success] с проектом, [Result.Error] с
     *   [AppError.ValidationError] если проект не найден,
     *   или [AppError.DatabaseError] при сбое SQLite.
     */
    override suspend fun getProjectById(id: Long): Result<Project> = try {
        val entity = projectDao.getProjectById(id)
        if (entity != null) Result.Success(entity.toDomain())
        else Result.Error(AppError.ValidationError("Проект не найден"))
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Добавляет новый проект. Принудительно проставляет актуальный
     * updatedAt и PENDING-статус синхронизации независимо от того,
     * что пришло в Domain-модели.
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
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Обновляет существующий проект (полная замена строки по id).
     * Аналогично [addProject] проставляет updatedAt и PENDING.
     *
     * @param project Domain-модель проекта с актуальными полями.
     * @return [Result.Success] или [Result.Error].
     */
    override suspend fun updateProject(project: Project): Result<Unit> = try {
        val entity = project.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        projectDao.updateProject(entity)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Удаляет проект по id. Каскадное удаление связанных сессий
     * выполняется на уровне БД через FK CASCADE.
     *
     * @param id Идентификатор проекта.
     * @return [Result.Success] или [Result.Error].
     */
    override suspend fun deleteProject(id: Long): Result<Unit> = try {
        projectDao.getProjectById(id)?.let { projectDao.deleteProject(it) }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Атомарно увеличивает общее время проекта.
     *
     * Value Object [Duration] гарантирует неотрицательность — отсекает
     * ошибки вида `updateTotalTime(id, -100)` до вызова DAO.
     *
     * @param projectId Идентификатор проекта.
     * @param duration Добавляемое время.
     * @return [Result.Success] или [Result.Error].
     */
    override suspend fun updateTotalTime(
        projectId: Long,
        duration: Duration
    ): Result<Unit> = try {
        projectDao.updateTotalTime(
            projectId = projectId,
            durationSeconds = duration.seconds,
            updatedAt = System.currentTimeMillis()
        )
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }
}
