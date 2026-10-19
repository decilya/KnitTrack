package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для [PatternName].
 */
class PatternNameTest {

    @Test
    fun create_withValidName_returnsValue() {
        assertEquals("Свитер", PatternName.create("Свитер").value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withBlankName_throws() {
        PatternName.create("   ")
    }

    @Test
    fun create_withExactlyMaxLength_ok() {
        val name = "a".repeat(PatternName.MAX_LENGTH)
        assertEquals(PatternName.MAX_LENGTH, PatternName.create(name).value.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_withTooLongName_throws() {
        PatternName.create("a".repeat(PatternName.MAX_LENGTH + 1))
    }

    @Test
    fun createOrNull_withBlankName_returnsNull() {
        assertNull(PatternName.createOrNull("   "))
    }

    @Test
    fun createOrDefault_withBlankName_returnsDefault() {
        assertEquals(PatternName.DEFAULT, PatternName.createOrDefault("   "))
    }

    @Test
    fun valueClass_structuralEquality() {
        assertEquals(PatternName.create("A"), PatternName.create("A"))
    }
}
