package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты для [Pattern].
 */
class PatternTest {

    private fun basePattern(
        rows: List<PatternRow> = emptyList(),
        repeats: List<PatternRepeat> = emptyList()
    ) = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Тест"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        rows = rows,
        repeats = repeats
    )

    @Test
    fun create_withEmptyRows_ok() {
        val pattern = basePattern()
        assertEquals(0, pattern.rows.size)
    }

    @Test
    fun create_withOrderedRows_ok() {
        val pattern = basePattern(
            rows = listOf(
                PatternRow(PatternRowId.create(1L), RowNumber.create(1)),
                PatternRow(PatternRowId.create(2L), RowNumber.create(2))
            )
        )
        assertEquals(2, pattern.rows.size)
    }

    @Test
    fun create_withIndependentIdsAndOrderedRowNumbers_ok() {
        // Порядок проверяется по rowNumber, не по id.
        val pattern = basePattern(
            rows = listOf(
                PatternRow(id = PatternRowId.create(100L), rowNumber = RowNumber.create(1)),
                PatternRow(id = PatternRowId.create(50L), rowNumber = RowNumber.create(2))
            )
        )
        assertEquals(2, pattern.rows.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withUnorderedRows_throws() {
        basePattern(
            rows = listOf(
                PatternRow(PatternRowId.create(1L), RowNumber.create(2)),
                PatternRow(PatternRowId.create(2L), RowNumber.create(1))
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withDuplicateRows_throws() {
        basePattern(
            rows = listOf(
                PatternRow(PatternRowId.create(1L), RowNumber.create(1)),
                PatternRow(PatternRowId.create(2L), RowNumber.create(1))
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withZeroScale_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageScale = 0f
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNaNScale_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageScale = Float.NaN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withInfiniteScale_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageScale = Float.POSITIVE_INFINITY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withInfiniteOffsetX_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageOffsetX = Float.NEGATIVE_INFINITY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNaNOffsetY_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageOffsetY = Float.NaN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNaNOffsetX_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageOffsetX = Float.NaN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withPositiveInfiniteOffsetX_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageOffsetX = Float.POSITIVE_INFINITY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withInfiniteOffsetYPositive_throws() {
        Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Тест"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageOffsetY = Float.POSITIVE_INFINITY
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withDuplicateRowId_throws() {
        basePattern(
            rows = listOf(
                PatternRow(PatternRowId.create(1L), RowNumber.create(1)),
                PatternRow(PatternRowId.create(1L), RowNumber.create(2))
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withDuplicateRepeatCode_throws() {
        basePattern(
            repeats = listOf(
                PatternRepeat("A", StitchCount.create(10), RowCount.create(1)),
                PatternRepeat("A", StitchCount.create(20), RowCount.create(2))
            )
        )
    }

    // --- Тесты для getDirectionForRow ---

    @Test
    fun getDirectionForRow_ltr_returnsLtrForAllRows() {
        val rows = listOf(
            PatternRow(PatternRowId.create(1L), RowNumber.create(1)),
            PatternRow(PatternRowId.create(2L), RowNumber.create(2))
        )
        val pattern = basePattern(rows = rows).copy(direction = PatternDirection.LTR)
        assertEquals(PatternDirection.LTR, pattern.getDirectionForRow(rows[0]))
        assertEquals(PatternDirection.LTR, pattern.getDirectionForRow(rows[1]))
    }

    @Test
    fun getDirectionForRow_rtl_returnsRtlForAllRows() {
        val rows = listOf(
            PatternRow(PatternRowId.create(1L), RowNumber.create(1)),
            PatternRow(PatternRowId.create(2L), RowNumber.create(2))
        )
        val pattern = basePattern(rows = rows).copy(direction = PatternDirection.RTL)
        assertEquals(PatternDirection.RTL, pattern.getDirectionForRow(rows[0]))
        assertEquals(PatternDirection.RTL, pattern.getDirectionForRow(rows[1]))
    }

    @Test
    fun getDirectionForRow_alternating_returnsAlternating() {
        val rows = listOf(
            PatternRow(PatternRowId.create(1L), RowNumber.create(1)),
            PatternRow(PatternRowId.create(2L), RowNumber.create(2)),
            PatternRow(PatternRowId.create(3L), RowNumber.create(3)),
            PatternRow(PatternRowId.create(4L), RowNumber.create(4))
        )
        val pattern = basePattern(rows = rows).copy(direction = PatternDirection.ALTERNATING)
        assertEquals(PatternDirection.LTR, pattern.getDirectionForRow(rows[0])) // index 0
        assertEquals(PatternDirection.RTL, pattern.getDirectionForRow(rows[1])) // index 1
        assertEquals(PatternDirection.LTR, pattern.getDirectionForRow(rows[2])) // index 2
        assertEquals(PatternDirection.RTL, pattern.getDirectionForRow(rows[3])) // index 3
    }

    @Test
    fun getDirectionForRow_alternatingSingleRow_returnsLtr() {
        val rows = listOf(PatternRow(PatternRowId.create(1L), RowNumber.create(1)))
        val pattern = basePattern(rows = rows).copy(direction = PatternDirection.ALTERNATING)
        assertEquals(PatternDirection.LTR, pattern.getDirectionForRow(rows[0])) // index 0
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDirectionForRow_emptyRows_throws() {
        val pattern = basePattern().copy(direction = PatternDirection.LTR)
        pattern.getDirectionForRow(PatternRow(PatternRowId.create(1L), RowNumber.create(1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDirectionForRow_rowNotInRows_throws() {
        val rows = listOf(PatternRow(PatternRowId.create(1L), RowNumber.create(1)))
        val pattern = basePattern(rows = rows).copy(direction = PatternDirection.ALTERNATING)
        pattern.getDirectionForRow(PatternRow(PatternRowId.create(99L), RowNumber.create(99)))
    }

    @Test
    fun getDirectionForRow_rowWithSameIdDifferentObject_returnsDirection() {
        val rows = listOf(
            PatternRow(PatternRowId.create(1L), RowNumber.create(1)),
            PatternRow(PatternRowId.create(2L), RowNumber.create(2))
        )
        val pattern = basePattern(rows = rows).copy(direction = PatternDirection.ALTERNATING)
        // row — другой объект, но тот же id (rows[1].id)
        val sameIdRow = PatternRow(PatternRowId.create(2L), RowNumber.create(2))
        assertEquals(PatternDirection.RTL, pattern.getDirectionForRow(sameIdRow))
    }
}
