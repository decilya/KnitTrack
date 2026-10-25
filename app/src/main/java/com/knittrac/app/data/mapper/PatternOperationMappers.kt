package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.domain.entity.PatternOperation

/**
 * Маппер [PatternOperation] (Domain) в [PatternOperationEntity] (Data).
 *
 * ВАЖНО: `PatternOperation` (Domain) имеет один `overrideSpan: Span?`, а
 * `PatternOperationEntity` — два поля (`columnSpanOverride`, `rowSpanOverride`).
 * `rowSpanOverride` всегда пишется как null (P2 в TECHDEBT: потеря вертикального
 * span при маппинге; восстановление — при появлении rowSpan в Domain).
 *
 * Символы в БД — по `code` (PK), поэтому `symbol.code` / `backSymbol?.code`.
 *
 * @return Entity, готовая к `INSERT` в `pattern_operations`.
 */
fun PatternOperation.toEntity(): PatternOperationEntity = PatternOperationEntity(
    patternRowId = patternRowId.value,
    columnStart = columnStart.value,
    symbolCode = symbol.code,
    backSymbolCode = backSymbol?.code,
    colorHex = colorHex,
    columnSpanOverride = overrideSpan?.value,
    rowSpanOverride = null,
    repeatCode = repeatCode
)
