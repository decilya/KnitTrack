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
 * на уровне компилятора), инкапсуляция инварианта. Существующий
 * `Project.id: Long` и `ProjectRepository.getProjectById(id: Long)` — tech debt
 * (P2 в TECHDEBT); новый код (params, use cases) использует [ProjectId] с
 * конвертацией `.value` на границе репозитория.
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
