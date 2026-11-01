package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase (Application-слой) для удаления кастомного символа.
 *
 * Является тонкой оберткой над [DeleteSymbolService].
 */
class DeleteSymbolUseCase @Inject constructor(
    private val service: DeleteSymbolService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<DeleteSymbolParams, Unit>(dispatcher) {

    /**
     * Выполняет удаление символа.
     *
     * @param params Параметры удаления.
     * @return [Result.Success] или [Result.Error].
     */
    override suspend fun execute(params: DeleteSymbolParams): Result<Unit> =
        service.delete(params)
}
