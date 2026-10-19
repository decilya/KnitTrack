package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Начальная колонка операции в ряду схемы.
 *
 * ВАЖНО: 0-based индекс (для Canvas-рендеринга).
 * Overlap операций проверяется как полуинтервал [start, start + span).
 * Не путать с [RowNumber] (1-based).
 *
 * Защищает инвариант: значение >= 0.
 */
@Keep
@JvmInline
value class ColumnStart private constructor(val value: Int) {

    companion object {
        /** Нулевая колонка (начало ряда). */
        val ZERO = ColumnStart(0)

        /**
         * Создает [ColumnStart] или бросает IllegalArgumentException.
         *
         * @param value 0-based индекс колонки.
         * @throws IllegalArgumentException Если значение отрицательное.
         */
        fun create(value: Int): ColumnStart {
            require(value >= 0) { "Начальная колонка не может быть отрицательной" }
            return ColumnStart(value)
        }

        /**
         * Создает [ColumnStart] или возвращает null при отрицательном значении.
         *
         * @param value 0-based индекс колонки.
         * @return [ColumnStart] или null.
         */
        fun createOrNull(value: Int): ColumnStart? =
            if (value >= 0) ColumnStart(value) else null

        /**
         * Создает [ColumnStart] или возвращает [ZERO] при отрицательном значении.
         *
         * @param value 0-based индекс колонки.
         * @return [ColumnStart] или [ZERO].
         */
        fun createOrDefault(value: Int): ColumnStart =
            if (value >= 0) ColumnStart(value) else ZERO
    }
}
