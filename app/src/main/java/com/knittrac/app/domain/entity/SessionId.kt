package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Идентификатор сессии вязания.
 *
 * Инвариант: сохранённая Session имеет id > 0.
 * ZERO — маркер новой сущности, ещё не сохранённой в БД.
 *
 * Аналог [ProjectId] / [PatternId]. Типобезопасность: SessionId нельзя
 * перепутать с ProjectId или PatternId на уровне компилятора.
 *
 * @property value Числовое значение идентификатора.
 */
@Keep
@JvmInline
value class SessionId private constructor(val value: Long) {

    companion object {
        /** Новая Session, ещё не сохранённая в БД. */
        val ZERO = SessionId(0L)

        /**
         * Создает [SessionId] или бросает IllegalArgumentException.
         *
         * @param value Идентификатор сохранённой Session.
         * @throws IllegalArgumentException Если value <= 0.
         */
        fun create(value: Long): SessionId {
            require(value > 0) { "SessionId сохранённого объекта должен быть > 0" }
            return SessionId(value)
        }

        /**
         * Создает [SessionId] или возвращает null при отрицательном значении.
         *
         * @param value Идентификатор.
         * @return [SessionId] или null.
         */
        fun createOrNull(value: Long): SessionId? =
            if (value >= 0) SessionId(value) else null

        /**
         * Создает [SessionId] или возвращает [ZERO] при отрицательном значении.
         *
         * @param value Идентификатор (может быть невалидным).
         * @return [SessionId] или [ZERO].
         */
        fun createOrDefault(value: Long): SessionId =
            if (value >= 0) SessionId(value) else ZERO
    }
}
