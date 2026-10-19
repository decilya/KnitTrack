package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Раппорт схемы — повторяющийся фрагмент с кодом и размерами.
 *
 * В БД идентифицируется парой (pattern_id, code), но в Domain
 * pattern_id не хранится (Pattern владеет repeats). Композитный PK
 * гарантирует уникальность code в рамках одной схемы.
 *
 * @property code Код раппорта (например, "A", "B").
 * @property stitchCount Количество петель в раппорте.
 * @property rowCount Количество рядов в раппорте.
 */
@Keep
data class PatternRepeat(
    val code: String,
    val stitchCount: StitchCount,
    val rowCount: RowCount
) {
    init {
        require(code.isNotBlank()) { "Код раппорта не может быть пустым" }
        require(stitchCount != StitchCount.ZERO) { "Количество петель в раппорте должно быть > 0" }
        require(rowCount != RowCount.ZERO) { "Количество рядов в раппорте должно быть > 0" }
    }
}
