package com.knittrac.app.domain.entity

/**
 * Длительность в секундах.
 *
 * Value Object гарантирует неотрицательность: компилятор запрещает
 * создать [Duration] из произвольного Long — только через [create],
 * [fromMillis] или [createOrNull]. Это исключает ошибки вида
 * `durationSeconds = -100` на уровне типа.
 *
 * @property seconds Количество секунд (≥ 0).
 */
@JvmInline
value class Duration private constructor(val seconds: Long) {

    companion object {
        /** Нулевая длительность. */
        val ZERO: Duration = Duration(0L)

        /**
         * Создаёт [Duration] из секунд.
         *
         * @throws IllegalArgumentException Если [seconds] отрицательный.
         */
        fun create(seconds: Long): Duration {
            require(seconds >= 0L) { "Длительность не может быть отрицательной" }
            return Duration(seconds)
        }

        /**
         * Создаёт [Duration] из миллисекунд с округлением вниз до секунд.
         *
         * @throws IllegalArgumentException Если [millis] отрицательный.
         */
        fun fromMillis(millis: Long): Duration {
            require(millis >= 0L) { "Длительность не может быть отрицательной" }
            return Duration(millis / 1000L)
        }

        /**
         * Безопасно создаёт [Duration] из секунд.
         *
         * @return [Duration] или null, если [seconds] отрицательный.
         */
        fun createOrNull(seconds: Long): Duration? =
            if (seconds >= 0L) Duration(seconds) else null
    }
}
