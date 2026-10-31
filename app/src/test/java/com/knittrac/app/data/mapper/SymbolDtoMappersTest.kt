package com.knittrac.app.data.mapper

import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymbolDtoMappersTest {

    @Test
    fun symbol_roundTripKeepsCodeAndSpans() {
        val symbol = Symbol(
            code = "K",
            displayName = "Лицевая",
            textGlyph = "V",
            category = SymbolCategory.STANDARD,
            defaultColumnSpan = Span.create(2),
            defaultRowSpan = Span.create(1)
        )
        val dto = symbol.toDto()
        val restored = dto.toDomain()
        assertEquals("K", restored.code)
        assertNull(restored.ownerPatternId)
        assertEquals(2, restored.defaultColumnSpan.value)
        assertEquals("K", dto.toEntity().code)
        assertEquals(2, dto.toEntity().defaultColumnSpan)
    }
}
