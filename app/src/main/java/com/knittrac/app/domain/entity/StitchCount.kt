package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Количество петель.
 *
 * Защищает инвариант: количество петель не может быть отрицательным.
 */
@Keep
@JvmInline
value class StitchCount private constructor(val value: Int) {
    companion object {
        val ZERO = StitchCount(0)

        /** Создает [StitchCount] или бросает IllegalArgumentException. */
        fun create(value: Int): StitchCount {
            require(value >= 0) { "Количество петель не может быть отрицательным" }
            return StitchCount(value)
        }

        /** Создает [StitchCount] или возвращает [ZERO] при отрицательном значении. */
        fun createOrDefault(value: Int): StitchCount =
            if (value >= 0) StitchCount(value) else ZERO

        /** Создает [StitchCount] или возвращает null при отрицательном значении. */
        fun createOrNull(value: Int): StitchCount? =
            if (value >= 0) StitchCount(value) else null
    }
}
