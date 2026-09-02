package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseFlowUseCase
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllProjectsService @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseFlowUseCase<Unit, List<Project>>(dispatcher) {
    
    override fun execute(params: Unit): Flow<List<Project>> = 
        projectRepository.getAllProjectsFlow()
}
