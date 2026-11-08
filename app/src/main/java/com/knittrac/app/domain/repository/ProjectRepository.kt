package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.StitchCount
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория для работы с проектами.
 *
 * P1-E (02.11.2026): на границе репозитория — VO [ProjectId].
 * Реализация [ProjectRepositoryImpl] разворачивает его в примитив для DAO.
 */
interface ProjectRepository {
    fun getAllProjectsFlow(): Flow<List<Project>>

    suspend fun getAllProjects(): Result<List<Project>>

    suspend fun getProjectById(id: ProjectId): Result<Project>

    suspend fun addProject(project: Project): Result<ProjectId>

    suspend fun updateProject(project: Project): Result<Unit>

    suspend fun deleteProject(id: ProjectId): Result<Unit>

    suspend fun updateSyncStatus(projectId: ProjectId, status: SyncStatus): Result<Unit>

    suspend fun updateProjectTotals(projectId: ProjectId, duration: Duration, rows: Int): Result<Unit>

    suspend fun updateProjectProgress(
        projectId: ProjectId,
        currentPatternRowId: PatternRowId?,
        currentStitch: StitchCount
    ): Result<Unit>
}
