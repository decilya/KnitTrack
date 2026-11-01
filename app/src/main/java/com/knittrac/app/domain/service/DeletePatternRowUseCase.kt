package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase для удаления ряда.
 * Делегирует логику в [DeletePatternRowService].
 */
class DeletePatternRowUseCase @Inject constructor(
    private val service: DeletePatternRowService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<DeletePatternRowParams, Unit>(dispatcher) {

    override suspend fun execute(params: DeletePatternRowParams): Result<Unit> =
        service.delete(params)
}
