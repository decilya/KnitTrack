package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Идентификатор ряда схемы.
 *
 * Инвариант: сохранённый PatternRow имеет id > 0.
 * ZERO — маркер нового объекта, ещё не сохранённого в БД (аналог
 * Project.id = 0L). ZERO создаётся только через companion, не через create(0).
 *
 * Отдельный тип от PatternId (типобезопасность: нельзя перепутать).
 */
@Keep
@JvmInline
value class PatternRowId private constructor(val value: Long) {

    companion object {
        /** Новый PatternRow, ещё не сохранён в БД. */
        val ZERO = PatternRowId(0L)

        /**
         * Создает [PatternRowId] или бросает IllegalArgumentException.
         *
         * @param value Идентификатор сохранённого PatternRow.
         * @throws IllegalArgumentException Если value <= 0.
         */
        fun create(value: Long): PatternRowId {
            require(value > 0) { "PatternRowId сохранённого объекта должен быть > 0" }
            return PatternRowId(value)
        }

        /**
         * Создает [PatternRowId] или возвращает null при отрицательном значении.
         *
         * @param value Идентификатор (0 — ZERO, > 0 — валидный, < 0 — null).
         * @return [PatternRowId] или null.
         */
        fun createOrNull(value: Long): PatternRowId? =
            if (value >= 0) PatternRowId(value) else null

        /**
         * Создает [PatternRowId] или возвращает [ZERO] при отрицательном значении.
         *
         * @param value Идентификатор (может быть невалидным).
         * @return [PatternRowId] или [ZERO].
         */
        fun createOrDefault(value: Long): PatternRowId =
            if (value >= 0) PatternRowId(value) else ZERO
    }
}
