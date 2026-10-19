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
    fun create_withDuplicateRepeatCode_throws() {
        basePattern(
            repeats = listOf(
                PatternRepeat("A", StitchCount.create(10), RowCount.create(1)),
                PatternRepeat("A", StitchCount.create(20), RowCount.create(2))
            )
        )
    }
}
