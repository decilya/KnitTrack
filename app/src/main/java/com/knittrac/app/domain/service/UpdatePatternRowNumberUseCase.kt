package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase для перемещения ряда (7f-D-3).
 */
class UpdatePatternRowNumberUseCase @Inject constructor(
    private val service: UpdatePatternRowNumberService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<UpdatePatternRowNumberParams, Unit>(dispatcher) {

    override suspend fun execute(params: UpdatePatternRowNumberParams): Result<Unit> =
        service.update(params)
}
