package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты для [PatternRow].
 */
class PatternRowTest {

    private fun symbol(code: String = "V") = Symbol(
        code = code,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD
    )

    @Test
    fun create_withEmptyOperations_ok() {
        val row = PatternRow(id = PatternRowId.create(1L), rowNumber = RowNumber.create(1))
        assertEquals(0, row.operations.size)
    }

    @Test
    fun create_withDistinctColumnsAndSameSymbol_ok() {
        // Один и тот же Symbol в двух операциях — допустимо.
        // PatternRow проверяет только уникальность columnStart.
        val sameSymbol = symbol()
        val row = PatternRow(
            id = PatternRowId.create(1L),
            rowNumber = RowNumber.create(1),
            operations = listOf(
                PatternOperation(PatternRowId.create(1L), ColumnStart.create(0), sameSymbol),
                PatternOperation(PatternRowId.create(1L), ColumnStart.create(1), sameSymbol)
            )
        )
        assertEquals(2, row.operations.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withDuplicateColumns_throws() {
        PatternRow(
            id = PatternRowId.create(1L),
            rowNumber = RowNumber.create(1),
            operations = listOf(
                PatternOperation(PatternRowId.create(1L), ColumnStart.create(0), symbol("V")),
                PatternOperation(PatternRowId.create(1L), ColumnStart.create(0), symbol("X"))
            )
        )
    }
}
