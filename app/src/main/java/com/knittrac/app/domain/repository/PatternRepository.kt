package com.knittrac.app.domain.repository

import kotlinx.coroutines.flow.Flow
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.model.OperationSpan

interface PatternRepository {
    /**
     * Загружает схему по id вместе с рядами, легендой и раппортами.
     */
    suspend fun getPatternById(id: PatternId): Result<Pattern>

    /**
     * Возвращает интервалы (columnStart + effectiveSpan), занятые операциями
     * в ряду [rowId]. Используется [AddPatternOperationService] для
     * overlap-check в памяти (O(n), n ≤ 100).
     *
     * @param rowId Идентификатор ряда.
     * @return [Result.Success] со списком интервалов или [Result.Error].
     */
    suspend fun getOperationSpansForRow(rowId: PatternRowId): Result<List<OperationSpan>>

    /**
     * Вставляет операцию в ряд.
     *
     * @param operation Domain-модель операции.
     * @return [Result.Success] или [Result.Error] с [com.knittrac.app.core.common.AppError.DatabaseError].
     */
    /**
     * Загружает операции ряда с полной доменной моделью [com.knittrac.app.domain.entity.PatternOperation].
     *
     * Символы операций (`pattern_operations.symbol_code` → `symbols.code`) резолвятся
     * через legend схемы — поэтому нужен `patternId`, а не только `rowId`.
     *
     * @param patternId Схема-владелец (для загрузки legend).
     * @param rowId Ряд-владелец.
     * @return [Result.Success] со списком операций, упорядоченных по `columnStart`;
     *   [Result.Error] с [com.knittrac.app.core.common.AppError.DatabaseError] при сбое БД.
     */
    suspend fun getOperationsForRow(
        patternId: PatternId,
        rowId: PatternRowId
    ): Result<List<PatternOperation>>

    /**
     * Поток лёгких проекций всех схем для селектора (7f-B).
     *
     * ISP: только `id + name`. Полный `Pattern` — через [getPatternById].
     */
    fun getAllPatternSummaries(): Flow<List<PatternSummary>>

    suspend fun addOperation(operation: PatternOperation): Result<Unit>
}
