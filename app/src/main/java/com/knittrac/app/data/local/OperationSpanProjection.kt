package com.knittrac.app.data.local

/**
 * Проекция интервала, занятого операцией в ряду.
 *
 * Результат JOIN `pattern_operations` + `symbols`: эффективная ширина =
 * `COALESCE(column_span_override, symbols.default_column_span)`.
 *
 * Только для чтения (overlap-check в [AddPatternOperationService]). Не entity,
 * не DTO, не покидает Data-слой как есть — маппится в
 * [com.knittrac.app.domain.model.OperationSpan] в репозитории.
 *
 * @property columnStart Начальная колонка (0-based).
 * @property effectiveSpan Ширина операции в колонках (≥ 1 по данным БД).
 */
data class OperationSpanProjection(
    val columnStart: Int,
    val effectiveSpan: Int
)
