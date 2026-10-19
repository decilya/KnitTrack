package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Техника выполнения схемы.
 *
 * Определяет способ вязания/плетения. Влияет на UI (текст-режим доступен
 * только для [KNITTING] и [CROCHET]) и на семантику рендеринга.
 */
@Keep
enum class Technique {
    /** Вязание спицами. */
    KNITTING,

    /** Вязание крючком. */
    CROCHET,

    /** Вышивка крестом. */
    CROSS_STITCH,

    /** Бисероплетение. */
    BEADING;

    companion object {
        /**
         * Безопасно восстанавливает [Technique] из строкового представления.
         *
         * Используется при чтении из БД, DTO или JSON, где данные могли быть
         * повреждены. Вместо падения возвращает [KNITTING].
         *
         * @param name Строковое имя техники.
         * @return Соответствующая [Technique] или [KNITTING] при неизвестном значении.
         */
        fun fromNameOrDefault(name: String): Technique =
            entries.firstOrNull { it.name == name } ?: KNITTING
    }
}
