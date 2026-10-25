package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.model.AddPatternOperationParams
import com.knittrac.app.domain.model.OperationSpan
import com.knittrac.app.domain.repository.PatternRepository
import javax.inject.Inject

/**
 * UseCase добавления операции (петли) в ряд схемы.
 *
 * Проверяет инварианты уровня агрегата [Pattern] до записи:
 * - символ принадлежит легенде схемы (`symbol.code in legend codes`);
 * - раппорт (если задан) присутствует в [Pattern.repeats];
 * - новая операция не пересекается по колонкам с существующими в ряду.
 *
 * Без Timber и без диспетчера — эталон [AddProjectService] / [ProgressService]
 * (правило HANDOVER §4.5.1: Domain не знает про Timber).
 *
 * @see PatternOperation
 * @see AddPatternOperationParams
 */
class AddPatternOperationService @Inject constructor(
    private val patternRepository: PatternRepository
) {

    /**
     * Добавляет операцию в ряд.
     *
     * @param params Параметры: схема, ряд, колонка, символ, опциональные
     *   backSymbol / colorHex / overrideSpan / repeatCode.
     * @return [Result.Success] с созданной [PatternOperation];
     *   [Result.Error] с [AppError.ValidationError] при нарушении инварианта
     *   (символ не в легенде, раппорт не найден, overlap по колонкам)
     *   или [AppError.NotFoundError] / [AppError.DatabaseError] при сбое БД.
     */
    suspend fun add(params: AddPatternOperationParams): Result<PatternOperation> {
        return when (val patternResult = patternRepository.getPatternById(params.patternId)) {
            is Result.Error -> patternResult
            is Result.Success -> continueAfterLoad(patternResult.data, params)
        }
    }

    /**
     * Продолжает после загрузки [Pattern]: валидация + сохранение.
     */
    private suspend fun continueAfterLoad(
        pattern: Pattern,
        params: AddPatternOperationParams
    ): Result<PatternOperation> {
        return when (val validation = validateAll(pattern, params)) {
            is Result.Error -> validation
            is Result.Success -> persist(params)
        }
    }

    /**
     * Запускает все проверки по очереди (fail-fast на первой ошибке).
     */
    private suspend fun validateAll(
        pattern: Pattern,
        params: AddPatternOperationParams
    ): Result<Unit> {
        return when (val legendCheck = validateLegend(pattern, params)) {
            is Result.Error -> legendCheck
            is Result.Success -> validateRepeatThenOverlap(pattern, params)
        }
    }

    /**
     * Проверка: раппорт (если задан) есть в схеме, затем overlap.
     */
    private suspend fun validateRepeatThenOverlap(
        pattern: Pattern,
        params: AddPatternOperationParams
    ): Result<Unit> {
        return when (val repeatCheck = validateRepeat(pattern, params)) {
            is Result.Error -> repeatCheck
            is Result.Success -> validateOverlap(params)
        }
    }

    /**
     * Проверяет, что символ операции присутствует в легенде схемы.
     *
     * Символы в легенде идентифицируются по `code` (PK в БД), не по
     * полному равенству data class: тот же код может прийти с другим
     * displayName (например, из UI-справочника).
     */
    private fun validateLegend(
        pattern: Pattern,
        params: AddPatternOperationParams
    ): Result<Unit> {
        return if (pattern.legend.none { it.code == params.symbol.code }) {
            Result.Error(
                AppError.ValidationError("Символ ${params.symbol.code} отсутствует в легенде схемы")
            )
        } else {
            Result.Success(Unit)
        }
    }

    /**
     * Проверяет, что `repeatCode` (если задан) присутствует в раппортах схемы.
     */
    private fun validateRepeat(
        pattern: Pattern,
        params: AddPatternOperationParams
    ): Result<Unit> {
        val repeatCode = params.repeatCode ?: return Result.Success(Unit)
        return if (pattern.repeats.none { it.code == repeatCode }) {
            Result.Error(
                AppError.ValidationError("Раппорт '$repeatCode' не найден в схеме")
            )
        } else {
            Result.Success(Unit)
        }
    }

    /**
     * Проверяет, что новая операция не пересекается с существующими в ряду.
     */
    private suspend fun validateOverlap(params: AddPatternOperationParams): Result<Unit> {
        val effectiveSpan = params.overrideSpan ?: params.symbol.defaultColumnSpan
        val newSpan = OperationSpan(columnStart = params.columnStart, span = effectiveSpan)

        return when (val existing = patternRepository.getOperationSpansForRow(params.row.id)) {
            is Result.Error -> existing
            is Result.Success -> checkOverlap(newSpan, existing.data)
        }
    }

    /**
     * Проверяет, что [newSpan] не пересекается ни с одним интервалом из [existing].
     */
    private fun checkOverlap(newSpan: OperationSpan, existing: List<OperationSpan>): Result<Unit> {
        val conflict = existing.firstOrNull { it.overlapsWith(newSpan) }
        return if (conflict != null) {
            Result.Error(
                AppError.ValidationError(
                    "Операция пересекается с существующей по колонке ${conflict.columnStart.value}"
                )
            )
        } else {
            Result.Success(Unit)
        }
    }

    /**
     * Создаёт [PatternOperation] и сохраняет через репозиторий.
     */
    private suspend fun persist(params: AddPatternOperationParams): Result<PatternOperation> {
        val operation = PatternOperation(
            patternRowId = params.row.id,
            columnStart = params.columnStart,
            symbol = params.symbol,
            backSymbol = params.backSymbol,
            colorHex = params.colorHex,
            overrideSpan = params.overrideSpan,
            repeatCode = params.repeatCode
        )
        return when (val saveResult = patternRepository.addOperation(operation)) {
            is Result.Error -> saveResult
            is Result.Success -> Result.Success(operation)
        }
    }
}
