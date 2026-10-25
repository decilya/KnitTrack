package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.PatternRepeatEntity
import com.knittrac.app.data.local.entity.SymbolEntity
import com.knittrac.app.domain.entity.SymbolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Юнит-тесты для [SymbolMappers].
 *
 * Покрывают маппинг [SymbolEntity] → [Symbol] и
 * [PatternRepeatEntity] → [PatternRepeat], включая fallback на неизвестную
 * категорию (защита от повреждённых данных в БД).
 */
class SymbolMappersTest {

    /**
     * Системный символ (owner_pattern_id = NULL) маппится без потерь.
     */
    @Test
    fun symbolEntity_toDomain_mapsStandardSymbol() {
        val entity = SymbolEntity(
            code = "K",
            ownerPatternId = null,
            displayName = "Лицевая",
            textGlyph = "V",
            category = "STANDARD",
            defaultColumnSpan = 1,
            defaultRowSpan = 1
        )

        val symbol = entity.toDomain()

        assertEquals("K", symbol.code)
        assertNull(symbol.ownerPatternId)
        assertEquals("Лицевая", symbol.displayName)
        assertEquals("V", symbol.textGlyph)
        assertEquals(SymbolCategory.STANDARD, symbol.category)
        assertEquals(1, symbol.defaultColumnSpan.value)
        assertEquals(1, symbol.defaultRowSpan.value)
    }

    /**
     * Неизвестная категория (опечатка в БД, старый бэкап) — fallback на STANDARD.
     */
    @Test
    fun symbolEntity_toDomain_unknownCategory_fallsBackToStandard() {
        val entity = SymbolEntity(
            code = "X",
            displayName = "X",
            textGlyph = "x",
            category = "INVALID"
        )
        assertEquals(SymbolCategory.STANDARD, entity.toDomain().category)
    }

    /**
     * Маппинг раппорта: code/stitchCount/rowCount переносятся без потерь.
     */
    @Test
    fun patternRepeatEntity_toDomain_mapsFields() {
        val entity = PatternRepeatEntity(patternId = 1L, code = "A", stitchCount = 10, rowCount = 4)
        val repeat = entity.toDomain()
        assertEquals("A", repeat.code)
        assertEquals(10, repeat.stitchCount.value)
        assertEquals(4, repeat.rowCount.value)
    }
}
