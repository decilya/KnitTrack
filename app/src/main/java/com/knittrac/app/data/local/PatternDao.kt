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
     * Возвращает интервалы (columnStart + effectiveSpan), занятые операциями
     * в ряду. Эффективная ширина = column_span_override, если задан,
     * иначе symbols.default_column_span.
     *
     * Используется [AddPatternOperationService] для overlap-check в памяти.
     */
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
