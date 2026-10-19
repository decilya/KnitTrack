package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты для [PatternRepeat].
 *
 * Покрывают инварианты: code не пустой, stitchCount > 0, rowCount > 0.
 */
class PatternRepeatTest {

    @Test
    fun create_withValidValues_ok() {
        val repeat = PatternRepeat(
            code = "A",
            stitchCount = StitchCount.create(10),
            rowCount = RowCount.create(2)
        )
        assertEquals("A", repeat.code)
        assertEquals(10, repeat.stitchCount.value)
        assertEquals(2, repeat.rowCount.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withBlankCode_throws() {
        PatternRepeat(
            code = "   ",
            stitchCount = StitchCount.create(10),
            rowCount = RowCount.create(2)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withZeroStitchCount_throws() {
        PatternRepeat(
            code = "A",
            stitchCount = StitchCount.ZERO,
            rowCount = RowCount.create(2)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withZeroRowCount_throws() {
        PatternRepeat(
            code = "A",
            stitchCount = StitchCount.create(10),
            rowCount = RowCount.ZERO
        )
    }
}
