package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты для [PatternDirection].
 */
class PatternDirectionTest {

    @Test
    fun fromNameOrDefault_withLtr_returnsLtr() {
        assertEquals(PatternDirection.LTR, PatternDirection.fromNameOrDefault("LTR"))
    }

    @Test
    fun fromNameOrDefault_withRtl_returnsRtl() {
        assertEquals(PatternDirection.RTL, PatternDirection.fromNameOrDefault("RTL"))
    }

    @Test
    fun fromNameOrDefault_withAlternating_returnsAlternating() {
        assertEquals(PatternDirection.ALTERNATING, PatternDirection.fromNameOrDefault("ALTERNATING"))
    }

    @Test
    fun fromNameOrDefault_withUnknown_returnsLtr() {
        assertEquals(PatternDirection.LTR, PatternDirection.fromNameOrDefault("UNKNOWN"))
    }

    @Test
    fun name_isStable() {
        assertEquals("LTR", PatternDirection.LTR.name)
        assertEquals("RTL", PatternDirection.RTL.name)
        assertEquals("ALTERNATING", PatternDirection.ALTERNATING.name)
    }
}
