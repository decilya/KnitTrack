package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Длина (span) операции в колонках или рядах.
 *
 * Защищает инвариант: значение >= 1. Span = 0 бессмыслен (операция
 * занимает 0 колонок/рядов). Используется в [Symbol] (defaultColumnSpan,
 * defaultRowSpan) и [PatternOperation] (overrideSpan).
 */
@Keep
@JvmInline
value class Span private constructor(val value: Int) {

    companion object {
        /** Минимальный span (одна колонка/ряд). */
        val ONE = Span(1)

        /**
         * Создает [Span] или бросает IllegalArgumentException.
         *
         * @param value Длина в колонках/рядах.
         * @throws IllegalArgumentException Если значение < 1.
         */
        fun create(value: Int): Span {
            require(value >= 1) { "Span должен быть >= 1" }
            return Span(value)
        }

        /**
         * Создает [Span] или возвращает null при невалидном значении.
         *
         * @param value Длина в колонках/рядах.
         * @return [Span] или null.
         */
        fun createOrNull(value: Int): Span? =
            if (value >= 1) Span(value) else null

        /**
         * Создает [Span] или возвращает [ONE] при невалидном значении.
         *
         * @param value Длина в колонках/рядах.
         * @return [Span] или [ONE].
         */
        fun createOrDefault(value: Int): Span =
            if (value >= 1) Span(value) else ONE
    }
}
