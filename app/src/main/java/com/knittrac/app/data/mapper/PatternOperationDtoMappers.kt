package com.knittrac.app.data.mapper

import com.knittrac.app.data.dto.PatternOperationDto
import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory

fun PatternOperation.toDto(): PatternOperationDto = PatternOperationDto(
    patternRowId = patternRowId.value,
    columnStart = columnStart.value,
    symbolCode = symbol.code,
    backSymbolCode = backSymbol?.code,
    colorHex = colorHex,
    columnSpanOverride = columnSpanOverride?.value,
    rowSpanOverride = rowSpanOverride?.value,
    repeatCode = repeatCode
)

/**
 * Символы собираются по коду: в JSON нет displayName.
 * Для INSERT достаточно кода — его заберёт [PatternOperation.toEntity].
 */
fun PatternOperationDto.toDomain(): PatternOperation = PatternOperation(
    patternRowId = PatternRowId.create(patternRowId),
    columnStart = ColumnStart.create(columnStart),
    symbol = symbolByCode(symbolCode),
    backSymbol = backSymbolCode?.let { symbolByCode(it) },
    colorHex = colorHex,
    columnSpanOverride = columnSpanOverride?.let { Span.createOrNull(it) },
    rowSpanOverride = rowSpanOverride?.let { Span.createOrNull(it) },
    repeatCode = repeatCode
)

fun PatternOperationDto.toEntity(): PatternOperationEntity = PatternOperationEntity(
    patternRowId = patternRowId,
    columnStart = columnStart,
    symbolCode = symbolCode,
    backSymbolCode = backSymbolCode,
    colorHex = colorHex,
    columnSpanOverride = columnSpanOverride,
    rowSpanOverride = rowSpanOverride,
    repeatCode = repeatCode
)

private fun symbolByCode(code: String): Symbol = Symbol(
    code = code,
    displayName = code,
    textGlyph = "?",
    category = SymbolCategory.STANDARD
)
