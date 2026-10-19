package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Направление чтения схемы.
 *
 * [LTR] — слева направо (лицевые ряды), [RTL] — справа налево (изнаночные),
 * [ALTERNATING] — чередование по чётности индекса ряда в списке.
 */
@Keep
enum class PatternDirection {
    /** Слева направо (по умолчанию для лицевых рядов). */
    LTR,

    /** Справа налево (изнаночные ряды). */
    RTL,

    /** Чередование: чётные ряды — LTR, нечётные — RTL. */
    ALTERNATING;

    companion object {
        /**
         * Безопасно восстанавливает [PatternDirection] из строкового представления.
         *
         * @param name Строковое имя направления.
         * @return Соответствующая [PatternDirection] или [LTR] при неизвестном значении.
         */
        fun fromNameOrDefault(name: String): PatternDirection =
            entries.firstOrNull { it.name == name } ?: LTR
    }
}
