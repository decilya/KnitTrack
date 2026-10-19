package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Идентификатор схемы.
 *
 * Инвариант: сохранённый Pattern имеет id > 0.
 * ZERO — маркер нового объекта, ещё не сохранённого в БД (аналог
 * Project.id = 0L). ZERO создаётся только через companion, не через create(0).
 *
 * Зачем VO, а не Long: типобезопасность (PatternId != PatternRowId != ProjectId
 * на уровне компилятора), инкапсуляция инварианта.
 */
@Keep
@JvmInline
value class PatternId private constructor(val value: Long) {

    companion object {
        /** Новый Pattern, ещё не сохранён в БД. */
        val ZERO = PatternId(0L)

        /**
         * Создает [PatternId] или бросает IllegalArgumentException.
         *
         * @param value Идентификатор сохранённого Pattern.
         * @throws IllegalArgumentException Если value <= 0.
         */
        fun create(value: Long): PatternId {
            require(value > 0) { "PatternId сохранённого объекта должен быть > 0" }
            return PatternId(value)
        }

        /**
         * Создает [PatternId] или возвращает null при отрицательном значении.
         *
         * @param value Идентификатор (0 — ZERO, > 0 — валидный, < 0 — null).
         * @return [PatternId] или null.
         */
        fun createOrNull(value: Long): PatternId? =
            if (value >= 0) PatternId(value) else null

        /**
         * Создает [PatternId] или возвращает [ZERO] при отрицательном значении.
         *
         * @param value Идентификатор (может быть невалидным).
         * @return [PatternId] или [ZERO].
         */
        fun createOrDefault(value: Long): PatternId =
            if (value >= 0) PatternId(value) else ZERO
    }
}
