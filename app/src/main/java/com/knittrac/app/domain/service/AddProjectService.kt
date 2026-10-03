package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddProjectService @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<AddProjectParams, Long>(dispatcher) {

    override suspend fun execute(params: AddProjectParams): Result<Long> {
        val now = System.currentTimeMillis()
        val project = Project(
            name = params.name,
            category = params.category,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name // <-- ИСПРАВЛЕНО: добавлено .name
        )
        return projectRepository.addProject(project)
    }
}
