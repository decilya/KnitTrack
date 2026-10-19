package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты для [Technique].
 */
class TechniqueTest {

    @Test
    fun fromNameOrDefault_withKnitting_returnsKnitting() {
        assertEquals(Technique.KNITTING, Technique.fromNameOrDefault("KNITTING"))
    }

    @Test
    fun fromNameOrDefault_withCrochet_returnsCrochet() {
        assertEquals(Technique.CROCHET, Technique.fromNameOrDefault("CROCHET"))
    }

    @Test
    fun fromNameOrDefault_withCrossStitch_returnsCrossStitch() {
        assertEquals(Technique.CROSS_STITCH, Technique.fromNameOrDefault("CROSS_STITCH"))
    }

    @Test
    fun fromNameOrDefault_withBeading_returnsBeading() {
        assertEquals(Technique.BEADING, Technique.fromNameOrDefault("BEADING"))
    }

    @Test
    fun fromNameOrDefault_withUnknown_returnsKnitting() {
        assertEquals(Technique.KNITTING, Technique.fromNameOrDefault("UNKNOWN"))
    }

    @Test
    fun name_isStable() {
        assertEquals("KNITTING", Technique.KNITTING.name)
        assertEquals("CROCHET", Technique.CROCHET.name)
        assertEquals("CROSS_STITCH", Technique.CROSS_STITCH.name)
        assertEquals("BEADING", Technique.BEADING.name)
    }
}
