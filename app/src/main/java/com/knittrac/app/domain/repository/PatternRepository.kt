package com.knittrac.app.domain.repository

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
    suspend fun addOperation(operation: PatternOperation): Result<Unit>
}
