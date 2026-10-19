package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Название схемы вязания.
 *
 * Защищает инвариант: название не пустое и не длиннее [MAX_LENGTH] символов.
 * По паттерну [ProjectName], но MAX_LENGTH дублируется явно (а не
 * заимствуется), т.к. схема и проект могут иметь разные лимиты в будущем.
 *
 * MAX_LENGTH синхронизирован с ProjectName.MAX_LENGTH = 100.
 */
@Keep
@JvmInline
value class PatternName private constructor(val value: String) {

    companion object {
        /** Максимальная длина названия схемы. */
        const val MAX_LENGTH = 100

        /**
         * Название по умолчанию, используется при восстановлении поврежденных
         * данных из DTO/бэкапа (createOrDefault). Из БД не читается: там
         * гарантирован CHECK-констрейнт на непустое имя (миграция v5).
         */
        val DEFAULT = PatternName("Untitled")

        /**
         * Создает [PatternName] или бросает IllegalArgumentException.
         *
         * @param value Название схемы.
         * @throws IllegalArgumentException Если название пустое или длиннее [MAX_LENGTH].
         */
        fun create(value: String): PatternName {
            require(value.isNotBlank()) { "Название схемы не может быть пустым" }
            require(value.length <= MAX_LENGTH) {
                "Название схемы не может быть длиннее $MAX_LENGTH символов"
            }
            return PatternName(value)
        }

        /**
         * Создает [PatternName] или возвращает null при невалидном значении.
         *
         * @param value Название схемы (может быть невалидным).
         * @return [PatternName] или null.
         */
        fun createOrNull(value: String): PatternName? =
            if (value.isNotBlank() && value.length <= MAX_LENGTH) PatternName(value) else null

        /**
         * Создает [PatternName] или возвращает [DEFAULT] при невалидном значении.
         *
         * @param value Название схемы (может быть невалидным).
         * @return [PatternName] или [DEFAULT].
         */
        fun createOrDefault(value: String): PatternName =
            createOrNull(value) ?: DEFAULT
    }
}
