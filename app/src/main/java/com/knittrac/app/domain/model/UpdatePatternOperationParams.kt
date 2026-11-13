package com.knittrac.app.domain.model

import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol

/**
 * Параметры обновления существующей операции (7f-D-1).
 * Обновляются только свойства, не меняющие первичный ключ (символ, цвет, span).
 * Позиция (rowId, columnStart) остается неизменной.
 */
data class UpdatePatternOperationParams(
    val patternId: PatternId,
    val rowId: PatternRowId,
    val columnStart: ColumnStart,
    val symbol: Symbol,
    val backSymbol: Symbol? = null,
    val colorHex: String? = null,
    val columnSpanOverride: Span? = null,
    val rowSpanOverride: Span? = null,
    val repeatCode: String? = null
)
