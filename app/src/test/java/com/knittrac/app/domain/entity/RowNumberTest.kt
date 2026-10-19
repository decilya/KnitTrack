package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для [RowNumber] (1-based).
 */
class RowNumberTest {

    @Test
    fun create_withOne_returnsOne() {
        assertEquals(1, RowNumber.create(1).value)
    }

    @Test
    fun create_withPositive_returnsValue() {
        assertEquals(10, RowNumber.create(10).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withZero_throws() {
        RowNumber.create(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNegative_throws() {
        RowNumber.create(-1)
    }

    @Test
    fun createOrNull_withZero_returnsNull() {
        assertNull(RowNumber.createOrNull(0))
    }

    @Test
    fun createOrDefault_withZero_returnsOne() {
        assertEquals(RowNumber.ONE, RowNumber.createOrDefault(0))
    }
}
