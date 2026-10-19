package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для [PatternRowId].
 */
class PatternRowIdTest {

    @Test
    fun create_withPositive_returnsValue() {
        assertEquals(42L, PatternRowId.create(42L).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withZero_throws() {
        PatternRowId.create(0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNegative_throws() {
        PatternRowId.create(-1L)
    }

    @Test
    fun zero_isValidZero() {
        assertEquals(0L, PatternRowId.ZERO.value)
    }

    @Test
    fun createOrNull_withZero_returnsZero() {
        assertEquals(PatternRowId.ZERO, PatternRowId.createOrNull(0L))
    }

    @Test
    fun createOrNull_withNegative_returnsNull() {
        assertNull(PatternRowId.createOrNull(-1L))
    }

    @Test
    fun createOrDefault_withNegative_returnsZero() {
        assertEquals(PatternRowId.ZERO, PatternRowId.createOrDefault(-1L))
    }
}
