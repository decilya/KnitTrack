package com.knittrac.app.domain.entity

/**
 * Название проекта вязания.
 *
 * Value Object гарантирует:
 * - имя не пустое (после trim);
 * - длина не превышает [MAX_LENGTH] символов;
 * - нет ведущих и замыкающих пробелов.
 *
 * Компилятор запрещает создать [ProjectName] из произвольной строки —
 * только через [create] (throws), [createOrNull] (null) или
 * [createOrDefault] (fallback). Это переносит валидацию из рантайма
 * в тип и защищает от ошибок вида `Project(name = "")`.
 *
 * @property value Нормализованное название (trim).
 */
@JvmInline
value class ProjectName private constructor(val value: String) {

    companion object {
        /** Максимальная длина названия. */
        const val MAX_LENGTH: Int = 100

        /** Fallback для миграции пустых значений и битых бэкапов. */
        val DEFAULT: ProjectName = ProjectName("Без названия")

        /**
         * Создаёт [ProjectName] из строки, применяя trim.
         *
         * @throws IllegalArgumentException Если строка пустая или длиннее [MAX_LENGTH].
         */
        fun create(raw: String): ProjectName {
            val trimmed = raw.trim()
            require(trimmed.isNotEmpty()) { "Название проекта не может быть пустым" }
            require(trimmed.length <= MAX_LENGTH) {
                "Название проекта длиннее $MAX_LENGTH символов"
            }
            return ProjectName(trimmed)
        }

        /**
         * Безопасно создаёт [ProjectName] из строки.
         *
         * @return [ProjectName] или null, если строка пустая или длиннее [MAX_LENGTH].
         */
        fun createOrNull(raw: String): ProjectName? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) return null
            return ProjectName(trimmed)
        }

        /**
         * Безопасно создаёт [ProjectName] с fallback на [DEFAULT].
         *
         * Используется при чтении из БД и DTO, где старые данные
         * могут быть невалидными — вместо краша подставляется DEFAULT.
         */
        fun createOrDefault(raw: String): ProjectName =
            createOrNull(raw) ?: DEFAULT
    }
}
