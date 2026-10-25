package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.Technique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternMappersTest {

    private val entity = PatternEntity(
        id = 5L,
        name = "Схема",
        technique = "CROCHET",
        direction = "ALTERNATING",
        isReversible = true
    )

    @Test
    fun patternEntity_toDomain_mapsRows() {
        val rows = listOf(
            PatternRowEntity(id = 10L, patternId = 5L, rowNumber = 1),
            PatternRowEntity(id = 11L, patternId = 5L, rowNumber = 2)
        )
        val pattern = entity.toDomain(rows)
        assertEquals(5L, pattern.id.value)
        assertEquals("Схема", pattern.name.value)
        assertEquals(Technique.CROCHET, pattern.technique)
        assertEquals(PatternDirection.ALTERNATING, pattern.direction)
        assertTrue(pattern.isReversible)
        assertEquals(2, pattern.rows.size)
        assertTrue(pattern.legend.isEmpty())
        assertTrue(pattern.repeats.isEmpty())
    }

    @Test
    fun patternEntity_toDomain_emptyRows() {
        val pattern = entity.toDomain(emptyList())
        assertTrue(pattern.rows.isEmpty())
    }

    @Test
    fun patternRowEntity_toDomain_mapsFields() {
        val row = PatternRowEntity(id = 10L, patternId = 5L, rowNumber = 3).toDomain()
        assertEquals(10L, row.id.value)
        assertEquals(3, row.rowNumber.value)
        assertTrue(row.operations.isEmpty())
    }
}
