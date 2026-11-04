package com.knittrac.app.data.mapper

import com.knittrac.app.data.dto.SymbolDto
import com.knittrac.app.data.local.entity.SymbolEntity
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory

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
