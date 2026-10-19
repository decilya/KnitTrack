package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StitchCountTest {

    @Test
    fun create_withZero_returnsZero() {
        assertEquals(0, StitchCount.create(0).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNegative_throws() {
        StitchCount.create(-1)
    }

    @Test
    fun createOrDefault_withNegative_returnsZero() {
        assertEquals(StitchCount.ZERO, StitchCount.createOrDefault(-5))
    }

    @Test
    fun createOrNull_withNegative_returnsNull() {
        assertNull(StitchCount.createOrNull(-5))
    }

    @Test
    fun createOrNull_withPositive_returnsValue() {
        assertEquals(StitchCount.create(42), StitchCount.createOrNull(42))
    }
}
