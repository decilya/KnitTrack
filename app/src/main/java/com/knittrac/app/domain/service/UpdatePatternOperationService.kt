package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.model.OperationSpan
import com.knittrac.app.domain.model.UpdatePatternOperationParams
import com.knittrac.app.domain.repository.PatternRepository
import javax.inject.Inject

/**
 * Сервис обновления операции в ряду (7f-D-1).
 *
 * Соблюдает инварианты агрегата [Pattern]:
 * 1. Символ должен присутствовать в легенде схемы.
 * 2. Обновленная операция не должна пересекаться по колонкам с другими операциями в этом же ряду.
 * 3. При сохранении используется [Symbol] из легенды (D8), чтобы избежать рассинхронизации
 *    defaultColumnSpan с UI-кэшем.
 */
class UpdatePatternOperationService @Inject constructor(
    private val patternRepository: PatternRepository
) {
    /**
     * Выполняет валидацию и обновление операции.
     *
     * @param params Параметры обновления.
     * @return [Result.Success] с обновленной операцией или [Result.Error] с причиной отказа.
     */
    suspend fun update(params: UpdatePatternOperationParams): Result<PatternOperation> {
        // Шаг 1. Загружаем схему, чтобы получить актуальную легенду.
        return when (val patternResult = patternRepository.getPatternById(params.patternId)) {
            is Result.Error -> patternResult
            is Result.Success -> continueAfterPatternLoaded(patternResult.data, params)
        }
    }

    /**
     * Продолжает обновление после загрузки схемы.
     *
     * Легенда схемы содержит истинные [Symbol] с актуальными span'ами.
     * Валидация возвращает уже проверенный non-null [Symbol], который
     * затем используется при сохранении.
     */
    private suspend fun continueAfterPatternLoaded(
        pattern: Pattern,
        params: UpdatePatternOperationParams
    ): Result<PatternOperation> {
        val legendSymbol = pattern.legend.firstOrNull { it.code == params.symbol.code }
        return when (val validation = validateOperation(legendSymbol, params)) {
            is Result.Error -> validation
            is Result.Success -> persistUpdate(params, validation.data)
        }
    }

    /**
     * Проверяет, что символ есть в легенде и что новая операция не пересекается с соседними.
     *
     * @return [Result.Success] с валидированным [Symbol] или [Result.Error].
     */
    private suspend fun validateOperation(
        legendSymbol: Symbol?,
        params: UpdatePatternOperationParams
    ): Result<Symbol> {
        if (legendSymbol == null) {
            return Result.Error(
                AppError.ValidationError("Символ ${params.symbol.code} отсутствует в легенде схемы")
            )
        }
        return when (val existingSpans = patternRepository.getOperationSpansForRow(params.rowId)) {
            is Result.Error -> existingSpans
            is Result.Success -> checkOverlap(existingSpans.data, legendSymbol, params)
        }
    }

    /**
     * Проверяет пересечения новой операции с соседними.
     * При успехе возвращает [legendSymbol] для дальнейшего сохранения (избегаем повторного lookup).
     */
    private fun checkOverlap(
        existingSpans: List<OperationSpan>,
        legendSymbol: Symbol,
        params: UpdatePatternOperationParams
    ): Result<Symbol> {
        // Исключаем из проверки саму обновляемую операцию.
        val otherSpans = existingSpans.filter { it.columnStart != params.columnStart }

        // Эффективная ширина: либо явно заданная (override), либо из легенды.
        val effectiveSpan = params.overrideSpan ?: legendSymbol.defaultColumnSpan
        val newSpan = OperationSpan(columnStart = params.columnStart, span = effectiveSpan)

        val hasOverlap = otherSpans.any { it.overlapsWith(newSpan) }

        return if (hasOverlap) {
            Result.Error(
                AppError.ValidationError("Обновление создает пересечение с другой операцией в этом ряду")
            )
        } else {
            Result.Success(legendSymbol)
        }
    }

    /**
     * Сохраняет обновленную операцию, используя валидированный [legendSymbol]
     * (D8: не доверяем params.symbol.defaultColumnSpan).
     */
    private suspend fun persistUpdate(
        params: UpdatePatternOperationParams,
        legendSymbol: Symbol
    ): Result<PatternOperation> {
        val operation = PatternOperation(
            patternRowId = params.rowId,
            columnStart = params.columnStart,
            symbol = legendSymbol,
            backSymbol = params.backSymbol,
            colorHex = params.colorHex,
            overrideSpan = params.overrideSpan,
            repeatCode = params.repeatCode
        )
        return when (val saveResult = patternRepository.updateOperation(operation)) {
            is Result.Error -> saveResult
            is Result.Success -> Result.Success(operation)
        }
    }
}
