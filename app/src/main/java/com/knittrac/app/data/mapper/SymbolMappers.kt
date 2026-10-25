package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.PatternRepeatEntity
import com.knittrac.app.data.local.entity.SymbolEntity
import com.knittrac.app.domain.entity.PatternRepeat
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.StitchCount
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory

/**
 * Маппер [SymbolEntity] (Data слой) в [Symbol] (Domain слой).
 *
 * Категория и span восстанавливаются безопасно с fallback:
 * неизвестная категория → [SymbolCategory.STANDARD], невалидный span → [Span.ONE].
 * Это защищает от повреждённых данных в БД и не падает на устаревших бэкапах.
 *
 * ВАЖНО: `ownerPatternId` в Domain остаётся `Long?` (P2 #1 в TECHDEBT,
 * миграция на `PatternId?` отложена до 7d+).
 */
fun SymbolEntity.toDomain(): Symbol = Symbol(
    code = code,
    ownerPatternId = ownerPatternId,
    displayName = displayName,
    textGlyph = textGlyph,
    category = SymbolCategory.fromNameOrDefault(category),
    defaultColumnSpan = Span.createOrDefault(defaultColumnSpan),
    defaultRowSpan = Span.createOrDefault(defaultRowSpan)
)

/**
 * Маппер [PatternRepeatEntity] (Data слой) в [PatternRepeat] (Domain слой).
 *
 * Инварианты `stitchCount > 0` и `rowCount > 0` защищены в [PatternRepeat.init];
 * при повреждённых данных с нулевыми значениями маппинг бросит
 * IllegalArgumentException — это P2 в TECHDEBT (нужен CHECK в БД, миграция v6).
 */
fun PatternRepeatEntity.toDomain(): PatternRepeat = PatternRepeat(
    code = code,
    stitchCount = StitchCount.createOrDefault(stitchCount),
    rowCount = RowCount.createOrDefault(rowCount)
)
