package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для [Span] (>= 1).
 */
class SpanTest {

    @Test
    fun create_withOne_returnsOne() {
        assertEquals(1, Span.create(1).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withZero_throws() {
        Span.create(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withNegative_throws() {
        Span.create(-1)
    }

    @Test
    fun createOrNull_withZero_returnsNull() {
        assertNull(Span.createOrNull(0))
    }

    @Test
    fun createOrDefault_withZero_returnsOne() {
        assertEquals(Span.ONE, Span.createOrDefault(0))
    }
}
