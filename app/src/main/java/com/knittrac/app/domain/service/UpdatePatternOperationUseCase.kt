package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.model.UpdatePatternOperationParams
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
