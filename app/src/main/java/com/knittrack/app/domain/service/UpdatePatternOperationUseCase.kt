package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseUseCase
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.entity.PatternOperation
import com.knittrack.app.domain.model.UpdatePatternOperationParams
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase обновления операции.
 * Делегирует всю логику в [UpdatePatternOperationService].
 */
class UpdatePatternOperationUseCase @Inject constructor(
    private val service: UpdatePatternOperationService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<UpdatePatternOperationParams, PatternOperation>(dispatcher) {

    override suspend fun execute(params: UpdatePatternOperationParams): Result<PatternOperation> =
        service.update(params)
}
