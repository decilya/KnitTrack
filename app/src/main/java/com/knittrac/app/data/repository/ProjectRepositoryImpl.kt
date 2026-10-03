package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.AppErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getAllProjectsFlow(): Flow<List<Project>> =
        projectDao.getAllProjectsFlow()

    override suspend fun getProjectById(id: Long): Result<Project> = try {
        val project = projectDao.getProjectById(id)
        if (project != null) {
            Result.Success(project)
        } else {
            Result.Error(AppError.ValidationError("Проект не найден"))
        }
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    override suspend fun addProject(project: Project): Result<Long> = try {
        val entityToInsert = project.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        Result.Success(projectDao.insertProject(entityToInsert))
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    override suspend fun updateProject(project: Project): Result<Unit> = try {
        val entityToUpdate = project.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        projectDao.updateProject(entityToUpdate)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    override suspend fun deleteProject(id: Long): Result<Unit> = try {
        projectDao.deleteProject(id)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    override suspend fun updateTotalTime(projectId: Long, additionalSeconds: Long): Result<Unit> = try {
        projectDao.updateTotalTime(
            projectId = projectId,
            durationSeconds = additionalSeconds,
            updatedAt = System.currentTimeMillis()
        )
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }
}
