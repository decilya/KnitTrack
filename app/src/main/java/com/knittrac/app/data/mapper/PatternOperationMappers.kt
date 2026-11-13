package com.knittrac.app.data.mapper

import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.domain.entity.PatternOperation

/**
 * Маппер [PatternOperation] (Domain) в [PatternOperationEntity] (Data).
 *
 * ВАЖНО: `PatternOperation` (Domain) имеет две оси спана (`columnSpanOverride`,
 * `rowSpanOverride`), которые мапятся в соответствующие поля Entity.
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
    columnSpanOverride = columnSpanOverride?.value,
    rowSpanOverride = rowSpanOverride?.value,
    repeatCode = repeatCode
)

/**
 * Маппер [PatternOperationEntity] (Data) → [PatternOperation] (Domain).
 *
 * Требует `symbol` и `backSymbol` параметрами: `PatternOperationEntity`
 * хранит `symbolCode: String` (FK на `symbols.code`), а `PatternOperation`
 * — полный [com.knittrac.app.domain.entity.Symbol]. Резолв символов — в
 * `PatternRepositoryImpl.getOperationsForRow` через legend схемы.
 */
fun PatternOperationEntity.toDomain(
    symbol: Symbol,
    backSymbol: Symbol? = null
): PatternOperation = PatternOperation(
    patternRowId = PatternRowId.createOrDefault(patternRowId),
    columnStart = ColumnStart.createOrDefault(columnStart),
    symbol = symbol,
    backSymbol = backSymbol,
    colorHex = colorHex,
    columnSpanOverride = columnSpanOverride?.let { Span.createOrNull(it) },
    rowSpanOverride = rowSpanOverride?.let { Span.createOrNull(it) },
    repeatCode = repeatCode
)
