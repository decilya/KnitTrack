package com.knittrack.app.data.mapper

import com.knittrack.app.data.dto.SymbolDto
import com.knittrack.app.data.local.entity.SymbolEntity
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.Span
import com.knittrack.app.domain.entity.Symbol
import com.knittrack.app.domain.entity.SymbolCategory

fun Symbol.toDto(): SymbolDto = SymbolDto(
    code = code,
    ownerPatternId = ownerPatternId?.value,
    displayName = displayName,
    textGlyph = textGlyph,
    category = category.name,
    defaultColumnSpan = defaultColumnSpan.value,
    defaultRowSpan = defaultRowSpan.value
)

fun SymbolDto.toDomain(): Symbol = Symbol(
    code = code,
    ownerPatternId = ownerPatternId?.let { PatternId.createOrNull(it) },
    displayName = displayName,
    textGlyph = textGlyph,
    category = SymbolCategory.fromNameOrDefault(category),
    defaultColumnSpan = Span.createOrDefault(defaultColumnSpan),
    defaultRowSpan = Span.createOrDefault(defaultRowSpan)
)

fun SymbolDto.toEntity(): SymbolEntity = toDomain().toEntity()
