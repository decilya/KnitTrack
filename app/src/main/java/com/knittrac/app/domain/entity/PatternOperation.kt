package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Операция (петля) в ряду схемы.
 *
 * ВАЖНО: это entity-значение внутри [PatternRow], а не самостоятельный
 * агрегат и не value object. Имеет идентичность — (patternRowId, columnStart),
 * соответствующую композитному PK в БД (pattern_operations). Не имеет
 * собственного суррогатного id.
 *
 * Overlap операций по columnStart НЕ проверяется здесь — это обязанность
 * AddPatternOperationService (Шаг 7d), у которого есть контекст Pattern.
 * [PatternRow.init] проверяет только уникальность columnStart.
 *
 * @property patternRowId Идентификатор ряда-владельца.
 * @property columnStart Начальная колонка (0-based, для Canvas).
 * @property symbol Символ операции.
 * @property backSymbol Символ для изнаночной стороны (опционально).
 * @property colorHex Цвет в HEX (опционально, для цветных схем).
 * @property columnSpanOverride Переопределение ширины (если отличается от symbol.defaultColumnSpan).
 * @property rowSpanOverride Переопределение высоты (если отличается от symbol.defaultRowSpan).
 * @property repeatCode Код раппорта (если операция входит в раппорт).
 */
@Keep
data class PatternOperation(
    val patternRowId: PatternRowId,
    val columnStart: ColumnStart,
    val symbol: Symbol,
    val backSymbol: Symbol? = null,
    val colorHex: String? = null,
    /**
     * Переопределение ширины операции в колонках.
     * Если null, используется symbol.defaultColumnSpan.
     */
    val columnSpanOverride: Span? = null,
    /**
     * Переопределение высоты операции в рядах.
     * Если null, используется symbol.defaultRowSpan.
     */
    val rowSpanOverride: Span? = null,
    val repeatCode: String? = null
) {
    /**
     * Эффективная ширина операции в колонках.
     * columnSpanOverride имеет приоритет, иначе используется symbol.defaultColumnSpan.
     */
    val effectiveColumnSpan: Span
        get() = columnSpanOverride ?: symbol.defaultColumnSpan

    /**
     * Эффективная высота операции в рядах.
     * rowSpanOverride имеет приоритет, иначе используется symbol.defaultRowSpan.
     */
    val effectiveRowSpan: Span
        get() = rowSpanOverride ?: symbol.defaultRowSpan
}
