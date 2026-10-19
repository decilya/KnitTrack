package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Номер ряда схемы.
 *
 * ВАЖНО: 1-based (для отображения пользователю: "Ряд 5").
 * Не путать с [ColumnStart] (0-based, для Canvas).
 *
 * Защищает инвариант: значение >= 1.
 */
@Keep
@JvmInline
value class RowNumber private constructor(val value: Int) {

    companion object {
        /** Первый ряд схемы (1-based). */
        val ONE = RowNumber(1)

        /**
         * Создает [RowNumber] или бросает IllegalArgumentException.
         *
         * @param value 1-based номер ряда.
         * @throws IllegalArgumentException Если значение < 1.
         */
        fun create(value: Int): RowNumber {
            require(value >= 1) { "Номер ряда должен быть >= 1" }
            return RowNumber(value)
        }

        /**
         * Создает [RowNumber] или возвращает null при невалидном значении.
         *
         * @param value 1-based номер ряда.
         * @return [RowNumber] или null.
         */
        fun createOrNull(value: Int): RowNumber? =
            if (value >= 1) RowNumber(value) else null

        /**
         * Создает [RowNumber] или возвращает [ONE] при невалидном значении.
         *
         * @param value 1-based номер ряда.
         * @return [RowNumber] или [ONE].
         */
        fun createOrDefault(value: Int): RowNumber =
            if (value >= 1) RowNumber(value) else ONE
    }
}
