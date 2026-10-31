package com.knittrac.app.data.mapper

import com.knittrac.app.data.dto.PatternOperationDto
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternOperationDtoMappersTest {

    @Test
    fun operation_domainDropsRowSpan_dtoKeepsIt() {
        val symbol = Symbol(
            code = "K",
            displayName = "Лицевая",
            textGlyph = "V",
            category = SymbolCategory.STANDARD
        )
        val operation = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0),
            symbol = symbol,
            repeatCode = "A"
        )
        val fromDomain = operation.toDto()
        assertEquals("K", fromDomain.symbolCode)
        assertNull(fromDomain.rowSpanOverride)
        assertEquals("A", fromDomain.toDomain().repeatCode)

        val fromFile = PatternOperationDto(
            patternRowId = 10L,
            columnStart = 0,
            symbolCode = "K",
            rowSpanOverride = 2
        )
        assertEquals(2, fromFile.toEntity().rowSpanOverride)
    }
}
