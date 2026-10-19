package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Ряд схемы.
 *
 * Владеет операциями (List<PatternOperation>). Композитный PK в БД
 * (pattern_rows) — id. Идентифицируется (patternId, rowNumber),
 * но patternId в Domain не хранится (Pattern владеет rows).
 *
 * ВАЖНО: init проверяет только уникальность columnStart среди операций.
 * Overlap (пересечение по columnStart с учетом span) — обязанность
 * AddPatternOperationService (Шаг 7d), т.к. требует контекста Pattern
 * (repeat_code разрешается через Pattern.repeats).
 *
 * @property id Идентификатор ряда (PK в pattern_rows).
 * @property rowNumber Номер ряда (1-based, для UI).
 * @property operations Упорядоченные по columnStart операции.
 */
@Keep
data class PatternRow(
    val id: PatternRowId,
    val rowNumber: RowNumber,
    val operations: List<PatternOperation> = emptyList()
) {
    init {
        val distinctStarts = operations.map { it.columnStart }.toSet()
        require(distinctStarts.size == operations.size) {
            "В ряду не может быть двух операций с одинаковым columnStart"
        }
    }
}
