package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты для [SymbolCategory].
 */
class SymbolCategoryTest {

    @Test
    fun fromNameOrDefault_withStandard_returnsStandard() {
        assertEquals(SymbolCategory.STANDARD, SymbolCategory.fromNameOrDefault("STANDARD"))
    }

    @Test
    fun fromNameOrDefault_withCustom_returnsCustom() {
        assertEquals(SymbolCategory.CUSTOM, SymbolCategory.fromNameOrDefault("CUSTOM"))
    }

    @Test
    fun fromNameOrDefault_withUnknown_returnsStandard() {
        assertEquals(SymbolCategory.STANDARD, SymbolCategory.fromNameOrDefault("UNKNOWN"))
    }

    @Test
    fun name_isStable() {
        assertEquals("STANDARD", SymbolCategory.STANDARD.name)
        assertEquals("CUSTOM", SymbolCategory.CUSTOM.name)
    }
}
