package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.Span

/**
 * Параметры добавления кастомного символа в схему (7f-D-4).
 */
data class AddSymbolParams(
    val patternId: PatternId,
    val code: String,
    val displayName: String,
    val textGlyph: String,
    val defaultColumnSpan: Span = Span.ONE
)
