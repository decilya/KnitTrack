package com.knittrac.app.domain.model

import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Span

/**
 * Занятый интервал операции в ряду (domain-модель для overlap-check).
 *
 * Не entity и не VO с инвариантом: это компактная проекция уже сохранённой
 * операции для проверки пересечений в [AddPatternOperationService]. Инвариант
 * «start >= 0, span >= 1» уже защищён типами [ColumnStart] и [Span].
 *
 * Полуинтервал `[columnStart, endExclusive)` — операция занимает колонки
 * `columnStart`..`endExclusive - 1`.
 *
 * @property columnStart Начальная колонка (0-based, для Canvas).
 * @property span Ширина операции в колонках (≥ 1).
 */
data class OperationSpan(
    val columnStart: ColumnStart,
    val span: Span
) {
    /** Правая граница полуинтервала (не включительно). */
    val endExclusive: Int get() = columnStart.value + span.value

    /**
     * Пересекается ли с другим интервалом.
     *
     * Полуинтервалы `[a, b)` и `[c, d)` пересекаются, когда `a < d && c < b`.
     * Касание границ (`b == c` или `d == a`) пересечением НЕ считается.
     */
    fun overlapsWith(other: OperationSpan): Boolean =
        columnStart.value < other.endExclusive && other.columnStart.value < endExclusive
}
