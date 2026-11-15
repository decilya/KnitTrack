package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Юнит-тесты для мапперов [PatternOperation] (Entity и DTO).
 *
 * Покрывают маппинг [PatternOperation] → [PatternOperationEntity]
 * (включая `rowSpanOverride` — Phase 1, dd2a083), null-поля
 * опциональных атрибутов и round-trip Domain → DTO → Domain
 * с сохранением оверрайдов спанов.
 */
class PatternOperationMappersTest {

    private fun symbol(code: String = "K"): Symbol = Symbol(
        code = code,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD
    )

    @Test
    fun patternOperation_toEntity_mapsFields() {
        val op = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(3),
            symbol = symbol("K"),
            backSymbol = symbol("P"),
            colorHex = "#FF0000",
            columnSpanOverride = Span.create(2),
            repeatCode = "A"
        )

        val entity = op.toEntity()

        assertEquals(10L, entity.patternRowId)
        assertEquals(3, entity.columnStart)
        assertEquals("K", entity.symbolCode)
        assertEquals("P", entity.backSymbolCode)
        assertEquals("#FF0000", entity.colorHex)
        assertEquals(2, entity.columnSpanOverride)
        assertNull(entity.rowSpanOverride)
        assertEquals("A", entity.repeatCode)
    }

    @Test
    fun patternOperation_toEntity_rowSpanAlwaysNull() {
        val op = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0),
            symbol = symbol()
        )
        assertNull(op.toEntity().rowSpanOverride)
    }

    @Test
    fun patternOperation_toEntity_nullOptionals() {
        val op = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0),
            symbol = symbol()
        )

        val entity = op.toEntity()

        assertNull(entity.backSymbolCode)
        assertNull(entity.colorHex)
        assertNull(entity.columnSpanOverride)
        assertNull(entity.repeatCode)
    }

    @Test
    fun entityToDomain_mapsFields() {
        val symbol = Symbol(
            code = "K", displayName = "Лицевая", textGlyph = "V",
            category = SymbolCategory.STANDARD
        )
        val entity = PatternOperationEntity(
            patternRowId = 10L, columnStart = 3, symbolCode = "K",
            backSymbolCode = null, colorHex = "#FF0000",
            columnSpanOverride = null, rowSpanOverride = null, repeatCode = null
        )
        val op = entity.toDomain(symbol)
        assertEquals(10L, op.patternRowId.value)
        assertEquals(3, op.columnStart.value)
        assertEquals("K", op.symbol.code)
        assertEquals("#FF0000", op.colorHex)
    }

    /**
     * Round-trip Domain → DTO → Domain для операции с двойным оверрайдом спана.
     *
     * Регрессия Phase 1 (rowSpanOverride): если [PatternOperation.toDto]
     * или DTO-маппер обратно забудут `rowSpanOverride`/`columnSpanOverride`,
     * effective-спаны сбросятся к дефолту символа (1) и тест упадёт.
     */
    @Test
    fun patternOperation_toDto_toDomain_roundTrip_preservesSpanOverrides() {
        val operation = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(3),
            symbol = symbol("B"),
            columnSpanOverride = Span.create(2),
            rowSpanOverride = Span.create(3)
        )

        val dto = operation.toDto()
        val restored = dto.toDomain()

        assertEquals(2, dto.columnSpanOverride)
        assertEquals(3, dto.rowSpanOverride)
        assertEquals(2, restored.effectiveColumnSpan.value)
        assertEquals(3, restored.effectiveRowSpan.value)
    }
}
