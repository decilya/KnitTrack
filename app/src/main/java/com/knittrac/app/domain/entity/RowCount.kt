package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Количество рядов.
 *
 * Защищает инвариант: количество рядов не может быть отрицательным.
 */
@Keep
@JvmInline
value class RowCount private constructor(val value: Int) {
    companion object {
        val ZERO = RowCount(0)

        /** Создает [RowCount] или бросает IllegalArgumentException. */
        fun create(value: Int): RowCount {
            require(value >= 0) { "Количество рядов не может быть отрицательным" }
            return RowCount(value)
        }

        /** Создает [RowCount] или возвращает [ZERO] при отрицательном значении. */
        fun createOrDefault(value: Int): RowCount =
            if (value >= 0) RowCount(value) else ZERO

        /** Создает [RowCount] или возвращает null при отрицательном значении. */
        fun createOrNull(value: Int): RowCount? =
            if (value >= 0) RowCount(value) else null
    }
}
