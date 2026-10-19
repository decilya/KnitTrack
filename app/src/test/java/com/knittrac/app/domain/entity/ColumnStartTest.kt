package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для [ColumnStart] (0-based).
 */
class ColumnStartTest {

    @Test
    fun create_withZero_returnsZero() {
        assertEquals(0, ColumnStart.create(0).value)
    }

    @Test
    fun create_withPositive_returnsValue() {
        assertEquals(5, ColumnStart.create(5).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNegative_throws() {
        ColumnStart.create(-1)
    }

    @Test
    fun createOrNull_withNegative_returnsNull() {
        assertNull(ColumnStart.createOrNull(-1))
    }

    @Test
    fun createOrDefault_withNegative_returnsZero() {
        assertEquals(ColumnStart.ZERO, ColumnStart.createOrDefault(-1))
    }
}
