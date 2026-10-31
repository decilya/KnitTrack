package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseFlowUseCase
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.domain.repository.PatternRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Сервис потока всех схем (лёгкие проекции) для селектора схем (7f-B).
 *
 * Эталон — [GetAllProjectsService]. ISP: только `id + name`.
 */
class GetAllPatternSummariesService @Inject constructor(
    private val patternRepository: PatternRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseFlowUseCase<Unit, List<PatternSummary>>(dispatcher) {

    override fun execute(params: Unit): Flow<List<PatternSummary>> =
        patternRepository.getAllPatternSummaries()
}
