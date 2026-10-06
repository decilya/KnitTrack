package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [ProjectRepository], инкапсулирующая логику работы с [ProjectDao].
 * Отвечает за преобразование Entity в Domain-модели и обратно.
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getAllProjectsFlow(): Flow<List<Project>> =
        projectDao.getAllProjectsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllProjects(): Result<List<Project>> = try {
        Result.Success(projectDao.getAllProjects().map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun getProjectById(id: Long): Result<Project> = try {
        val entity = projectDao.getProjectById(id)
        if (entity != null) Result.Success(entity.toDomain())
        else Result.Error(AppError.ValidationError("Проект не найден"))
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun addProject(project: Project): Result<Long> = try {
        val entity = project.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        Result.Success(projectDao.insertProject(entity))
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

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

    override suspend fun deleteProject(id: Long): Result<Unit> = try {
        projectDao.getProjectById(id)?.let { projectDao.deleteProject(it) }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun updateTotalTime(projectId: Long, additionalSeconds: Long): Result<Unit> = try {
        projectDao.updateTotalTime(
            projectId = projectId,
            durationSeconds = additionalSeconds,
            updatedAt = System.currentTimeMillis()
        )
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }
}
