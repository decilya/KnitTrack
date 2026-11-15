package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseUseCase
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.entity.Symbol
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase (Application-слой) для добавления кастомного символа.
 *
 * Является тонкой оберткой над [AddSymbolService].
 */
class AddSymbolUseCase @Inject constructor(
    private val service: AddSymbolService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<AddSymbolParams, Symbol>(dispatcher) {

    /**
     * Выполняет добавление символа.
     *
     * @param params Параметры символа.
     * @return [Result.Success] с созданным [Symbol] или [Result.Error].
     */
    override suspend fun execute(params: AddSymbolParams): Result<Symbol> =
        service.add(params)
}
