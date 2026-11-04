package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Идентификатор проекта.
 *
 * Инвариант: сохранённый Project имеет id > 0.
 * ZERO — маркер нового объекта, ещё не сохранённого в БД (аналог
 * PatternId.ZERO / PatternRowId.ZERO). ZERO создаётся только через companion,
 * не через create(0).
 *
 * Зачем VO, а не Long: типобезопасность (ProjectId != PatternId != PatternRowId
 * на уровне компилятора), инкапсуляция инварианта. На границе с Room/DTO
 * разворачивается в примитив (`.value`) в RepositoryImpl и мапперах.
 *
 * @property value Числовое значение идентификатора.
 */
@Keep
@JvmInline
value class ProjectId private constructor(val value: Long) {

    companion object {
        /** Новый Project, ещё не сохранён в БД. */
        val ZERO = ProjectId(0L)

        /**
         * Создает [ProjectId] или бросает IllegalArgumentException.
         *
         * @param value Идентификатор сохранённого Project.
         * @throws IllegalArgumentException Если value <= 0.
         */
        fun create(value: Long): ProjectId {
            require(value > 0) { "ProjectId сохранённого объекта должен быть > 0" }
            return ProjectId(value)
        }

        /**
         * Создает [ProjectId] или возвращает null при отрицательном значении.
         *
         * @param value Идентификатор (0 — ZERO, > 0 — валидный, < 0 — null).
         * @return [ProjectId] или null.
         */
        fun createOrNull(value: Long): ProjectId? =
            if (value >= 0) ProjectId(value) else null

        /**
         * Создает [ProjectId] или возвращает [ZERO] при отрицательном значении.
         *
         * @param value Идентификатор (может быть невалидным).
         * @return [ProjectId] или [ZERO].
         */
        fun createOrDefault(value: Long): ProjectId =
            if (value >= 0) ProjectId(value) else ZERO
    }
}
