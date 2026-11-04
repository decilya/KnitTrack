package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.repository.PatternRepository
import javax.inject.Inject

/**
 * Сервис добавления кастомного символа в легенду схемы (7f-D-4).
 *
 * Символ создается с категорией [SymbolCategory.CUSTOM] и привязкой к схеме
 * (ownerPatternId = patternId). Системные символы (ownerPatternId = null)
 * через этот сервис не добавляются.
 */
class AddSymbolService @Inject constructor(
    private val patternRepository: PatternRepository
) {
    /**
     * Добавляет новый кастомный символ.
     *
     * @param params Параметры создаваемого символа.
     * @return [Result.Success] с созданным [Symbol] или [Result.Error] с причиной отказа.
     */
    suspend fun add(params: AddSymbolParams): Result<Symbol> {
        // Шаг 1. Загружаем текущую легенду схемы, чтобы проверить уникальность кода.
        return when (val existingSymbols = patternRepository.getSymbolsForPattern(params.patternId)) {
            is Result.Error -> existingSymbols
            is Result.Success -> continueAfterLegendLoaded(existingSymbols.data, params)
        }
    }

    /**
     * Продолжает добавление после загрузки легенды: проверка уникальности и сохранение.
     */
    private suspend fun continueAfterLegendLoaded(
        existingSymbols: List<Symbol>,
        params: AddSymbolParams
    ): Result<Symbol> {
        // Шаг 2. Проверяем, что символ с таким кодом еще не существует в этой схеме.
        if (existingSymbols.any { it.code == params.code }) {
            return Result.Error(AppError.ValidationError("Символ с кодом ${params.code} уже существует"))
        }

        // Шаг 3. Формируем Domain-объект кастомного символа.
        val symbol = Symbol(
            code = params.code,
            ownerPatternId = params.patternId,
            displayName = params.displayName,
            textGlyph = params.textGlyph,
            category = SymbolCategory.CUSTOM,
            defaultColumnSpan = params.defaultColumnSpan
        )

        // Шаг 4. Сохраняем в БД через репозиторий.
        return when (val saveRes = patternRepository.addSymbol(symbol)) {
            is Result.Error -> saveRes
            is Result.Success -> Result.Success(symbol)
        }
    }
}
