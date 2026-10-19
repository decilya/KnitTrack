package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для [PatternId].
 */
class PatternIdTest {

    @Test
    fun create_withPositive_returnsValue() {
        assertEquals(42L, PatternId.create(42L).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withZero_throws() {
        PatternId.create(0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNegative_throws() {
        PatternId.create(-1L)
    }

    @Test
    fun zero_isValidZero() {
        assertEquals(0L, PatternId.ZERO.value)
    }

    @Test
    fun create_withMaxLong_returnsValue() {
        assertEquals(Long.MAX_VALUE, PatternId.create(Long.MAX_VALUE).value)
    }

    @Test
    fun createOrNull_withZero_returnsZero() {
        assertEquals(PatternId.ZERO, PatternId.createOrNull(0L))
    }

    @Test
    fun createOrNull_withNegative_returnsNull() {
        assertNull(PatternId.createOrNull(-1L))
    }

    @Test
    fun createOrDefault_withNegative_returnsZero() {
        assertEquals(PatternId.ZERO, PatternId.createOrDefault(-1L))
    }
}
