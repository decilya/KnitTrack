package com.knittrac.app.data.local

import androidx.room.*
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.data.local.entity.PatternOperationEntity
import kotlinx.coroutines.flow.Flow

/** DAO для работы со схемами, их рядами и операциями. */
@Dao
interface PatternDao {
    @Query("SELECT * FROM patterns ORDER BY name ASC")
    fun getAllPatternsFlow(): Flow<List<PatternEntity>>

    @Query("SELECT * FROM patterns ORDER BY id ASC")
    suspend fun getAllPatterns(): List<PatternEntity>

    @Query("SELECT * FROM pattern_rows ORDER BY pattern_id ASC, row_number ASC")
    suspend fun getAllRows(): List<PatternRowEntity>

    @Query("SELECT * FROM pattern_operations ORDER BY pattern_row_id ASC, column_start ASC")
    suspend fun getAllOperations(): List<PatternOperationEntity>

    /**
     * Все операции схемы одним SQL. Не N+1 по рядам.
     *
     * @param patternId Схема-владелец.
     */
    @Query(
        """
        SELECT pattern_operations.* FROM pattern_operations
        INNER JOIN pattern_rows ON pattern_operations.pattern_row_id = pattern_rows.id
        WHERE pattern_rows.pattern_id = :patternId
        ORDER BY pattern_operations.pattern_row_id, pattern_operations.column_start
        """
    )
    suspend fun getAllOperationsForPattern(patternId: Long): List<PatternOperationEntity>

    @Query("SELECT * FROM patterns WHERE id = :id")
    suspend fun getPatternById(id: Long): PatternEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: PatternEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRows(rows: List<PatternRowEntity>)

    @Query("SELECT * FROM pattern_rows WHERE pattern_id = :patternId ORDER BY row_number ASC")
    suspend fun getRowsForPattern(patternId: Long): List<PatternRowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(operations: List<PatternOperationEntity>)

    @Query("SELECT * FROM pattern_operations WHERE pattern_row_id = :rowId ORDER BY column_start ASC")
    suspend fun getOperationsForRow(rowId: Long): List<PatternOperationEntity>

    /**
     * Удаляет операцию по композитному ключу ряда и колонки.
     * @return Количество удаленных строк.
     */
    @Query("SELECT COUNT(*) FROM pattern_rows WHERE pattern_id = :patternId")
    suspend fun getRowCount(patternId: Long): Int

    @Query("SELECT row_number FROM pattern_rows WHERE id = :rowId")
    suspend fun getRowNumber(rowId: Long): Int?

    @Query("SELECT EXISTS(SELECT 1 FROM pattern_rows WHERE id = :rowId AND pattern_id = :patternId)")
    suspend fun existsRow(rowId: Long, patternId: Long): Boolean

    @Query("UPDATE pattern_rows SET row_number = :newNumber WHERE id = :rowId")
    suspend fun updateRowNumber(rowId: Long, newNumber: Int): Int

    @Query("UPDATE pattern_rows SET row_number = row_number - 1 WHERE pattern_id = :patternId AND row_number > :oldNumber AND row_number <= :newNumber")
    suspend fun shiftRowNumbersDown(patternId: Long, oldNumber: Int, newNumber: Int): Int

    @Query("UPDATE pattern_rows SET row_number = row_number + 1 WHERE pattern_id = :patternId AND row_number >= :newNumber AND row_number < :oldNumber")
    suspend fun shiftRowNumbersUp(patternId: Long, newNumber: Int, oldNumber: Int): Int


    @Query("DELETE FROM pattern_rows WHERE id = :rowId")
    suspend fun deleteRow(rowId: Long): Int

    @Query("DELETE FROM pattern_operations WHERE pattern_row_id = :rowId AND column_start = :columnStart")
    suspend fun deleteOperation(rowId: Long, columnStart: Int): Int

    /**
     * Обновляет параметры существующей операции.
     * Не меняет первичный ключ (pattern_row_id, column_start).
     * 
     * @return Количество обновленных строк (0, если операции не было).
     */
    @Suppress("LongParameterList")
    @Query(
        """
        UPDATE pattern_operations
        SET symbol_code = :symbolCode,
            back_symbol_code = :backSymbolCode,
            color_hex = :colorHex,
            column_span_override = :columnSpanOverride,
            row_span_override = :rowSpanOverride,
            repeat_code = :repeatCode
        WHERE pattern_row_id = :rowId AND column_start = :columnStart
        """
    )
    suspend fun updateOperation(
        rowId: Long,
        columnStart: Int,
        symbolCode: String,
        backSymbolCode: String?,
        colorHex: String?,
        columnSpanOverride: Int?,
        rowSpanOverride: Int?,
        repeatCode: String?
    ): Int

    @Query("""
        SELECT po.column_start AS columnStart,
               COALESCE(po.column_span_override, s.default_column_span) AS effectiveSpan
        FROM pattern_operations po
        INNER JOIN symbols s ON s.code = po.symbol_code
        WHERE po.pattern_row_id = :rowId
        ORDER BY po.column_start ASC
    """)
    suspend fun getOperationSpansForRow(rowId: Long): List<OperationSpanProjection>
}
