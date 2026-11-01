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
     */
    private suspend fun continueAfterPatternLoaded(
        pattern: Pattern,
        params: UpdatePatternOperationParams
    ): Result<PatternOperation> {
        // Шаг 2. Валидация символа и проверка пересечений.
        // Сначала ищем символ в легенде, чтобы взять его истинную ширину (defaultColumnSpan).
        val legendSymbol = pattern.legend.firstOrNull { it.code == params.symbol.code }
        
        return when (val validationResult = validateOperation(legendSymbol, params)) {
            is Result.Error -> validationResult
            is Result.Success -> persistUpdate(params)
        }
    }

    /**
     * Проверяет, что символ есть в легенде и что новая операция не пересекается с соседними.
     */
    private suspend fun validateOperation(
        legendSymbol: Symbol?,
        params: UpdatePatternOperationParams
    ): Result<Unit> {
        if (legendSymbol == null) {
            return Result.Error(
                AppError.ValidationError("Символ ${params.symbol.code} отсутствует в легенде схемы")
            )
        }

        // Загружаем существующие операции в ряду, чтобы проверить пересечения.
        return when (val existingSpans = patternRepository.getOperationSpansForRow(params.rowId)) {
            is Result.Error -> existingSpans
            is Result.Success -> checkOverlap(existingSpans.data, legendSymbol, params)
        }
    }

    /**
     * Непосредственно проверка на пересечение (Overlap).
     */
    private fun checkOverlap(
        existingSpans: List<OperationSpan>,
        legendSymbol: Symbol,
        params: UpdatePatternOperationParams
    ): Result<Unit> {
        // Исключаем из проверки саму обновляемую операцию, так как мы её и меняем.
        val otherSpans = existingSpans.filter { it.columnStart != params.columnStart }
        
        // Эффективная ширина: либо явно заданная (override), либо из легенды.
        val effectiveSpan = params.overrideSpan ?: legendSymbol.defaultColumnSpan
        val newSpan = OperationSpan(columnStart = params.columnStart, span = effectiveSpan)

        // Проверяем, не задевает ли новая ширина соседние операции.
        val hasOverlap = otherSpans.any { it.overlapsWith(newSpan) }
        
        return if (hasOverlap) {
            Result.Error(
                AppError.ValidationError("Обновление создает пересечение с другой операцией в этом ряду")
            )
        } else {
            Result.Success(Unit)
        }
    }

    /**
     * Сохраняет обновленную операцию в БД.
     */
    private suspend fun persistUpdate(params: UpdatePatternOperationParams): Result<PatternOperation> {
        val operation = PatternOperation(
            patternRowId = params.rowId,
            columnStart = params.columnStart,
            symbol = params.symbol,
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
