package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseFlowUseCase
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.entity.Project
import com.knittrack.app.domain.repository.ProjectRepository
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
