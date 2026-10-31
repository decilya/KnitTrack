package com.knittrac.app.data.mapper

import com.knittrac.app.domain.entity.PatternRepeat
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.StitchCount
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternRepeatDtoMappersTest {

    @Test
    fun repeat_roundTripKeepsPatternId() {
        val repeat = PatternRepeat(
            code = "A",
            stitchCount = StitchCount.create(2),
            rowCount = RowCount.create(1)
        )
        val dto = repeat.toDto(patternId = 1L)
        assertEquals(1L, dto.patternId)
        assertEquals("A", dto.toDomain().code)
        assertEquals(2, dto.toDomain().stitchCount.value)
        assertEquals(1L, dto.toEntity().patternId)
        assertEquals(1, dto.toEntity().rowCount)
    }
}
