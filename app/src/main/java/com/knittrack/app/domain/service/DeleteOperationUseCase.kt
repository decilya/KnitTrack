package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseUseCase
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.repository.PatternRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Удаляет операцию. Тонкая обёртка над [PatternRepository.deleteOperation].
 */
class DeleteOperationUseCase @Inject constructor(
    private val patternRepository: PatternRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<DeleteOperationParams, Unit>(dispatcher) {

    override suspend fun execute(params: DeleteOperationParams): Result<Unit> =
        patternRepository.deleteOperation(params.rowId, params.columnStart)
}
