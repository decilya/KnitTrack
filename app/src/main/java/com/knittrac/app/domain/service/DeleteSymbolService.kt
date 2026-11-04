package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.repository.PatternRepository
import javax.inject.Inject

/**
 * Сервис удаления кастомного символа из легенды схемы (7f-D-4).
 *
 * Удалять можно только кастомные символы (ownerPatternId == patternId).
 * Если символ используется в операциях (pattern_operations), БД выдаст ошибку
 * (ON DELETE RESTRICT), которую мы конвертируем в понятный ValidationError.
 */
class DeleteSymbolService @Inject constructor(
    private val patternRepository: PatternRepository
) {
    /**
     * Удаляет кастомный символ.
     *
     * @param params Параметры удаления.
     * @return [Result.Success] или [Result.Error] с причиной отказа.
     */
    suspend fun delete(params: DeleteSymbolParams): Result<Unit> {
        // Шаг 1. Загружаем все символы, доступные схеме.
        return when (val symbols = patternRepository.getSymbolsForPattern(params.patternId)) {
            is Result.Error -> symbols
            is Result.Success -> continueAfterLegendLoaded(symbols.data, params)
        }
    }

    /**
     * Продолжает удаление после загрузки легенды: проверка типа и удаление.
     */
    private suspend fun continueAfterLegendLoaded(
        symbols: List<Symbol>,
        params: DeleteSymbolParams
    ): Result<Unit> {
        // Шаг 2. Ищем нужный символ по коду.
        val symbol = symbols.firstOrNull { it.code == params.code }

        // Шаг 3. Валидация: символ должен существовать и быть кастомным.
        return when {
            symbol == null -> Result.Error(AppError.NotFoundError("Символ ${params.code} не найден в схеме"))
            symbol.ownerPatternId != params.patternId -> Result.Error(
                AppError.ValidationError("Нельзя удалить системный символ ${params.code}")
            )
            else -> {
                // Шаг 4. Пытаемся удалить. Если символ используется в операциях,
                // БД выдаст ошибку (FK RESTRICT), которую репозиторий обработает.
                patternRepository.deleteSymbol(params.patternId, params.code)
            }
        }
    }
}
