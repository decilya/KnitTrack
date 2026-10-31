package com.knittrac.app.data.mapper

import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Technique
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternDtoMappersTest {

    @Test
    fun patternAndRow_roundTripKeepsIds() {
        val pattern = Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Схема"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            imageScale = 1.5f
        )
        val restored = pattern.toDto().toDomain()
        assertEquals(pattern.id, restored.id)
        assertEquals(pattern.name, restored.name)
        assertEquals(Technique.KNITTING, restored.technique)
        assertEquals(1.5f, restored.imageScale)

        val row = PatternRow(
            id = PatternRowId.create(10L),
            rowNumber = RowNumber.create(1)
        )
        val dto = row.toDto(patternId = 1L)
        assertEquals(1L, dto.patternId)
        assertEquals(10L, dto.toDomain().id.value)
        assertEquals(1L, dto.toEntity().patternId)
        assertEquals(1, dto.toEntity().rowNumber)
    }
}
