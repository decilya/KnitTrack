package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RowCountTest {

    @Test
    fun create_withZero_returnsZero() {
        assertEquals(0, RowCount.create(0).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNegative_throws() {
        RowCount.create(-1)
    }

    @Test
    fun createOrDefault_withNegative_returnsZero() {
        assertEquals(RowCount.ZERO, RowCount.createOrDefault(-5))
    }

    @Test
    fun createOrNull_withNegative_returnsNull() {
        assertNull(RowCount.createOrNull(-5))
    }

    @Test
    fun createOrNull_withPositive_returnsValue() {
        assertEquals(RowCount.create(42), RowCount.createOrNull(42))
    }
}
