package com.knittrac.app.domain.entity

/**
 * Режим отображения темы приложения.
 *
 * Используется для выбора между светлой, тёмной и системной темой.
 * Хранится в DataStore как [name] (String) — стабильный формат,
 * не зависящий от порядка констант (в отличие от ordinal).
 */
enum class ThemeMode {
    /** Тема следует за настройками системы. */
    SYSTEM,

    /** Принудительно светлая тема. */
    LIGHT,

    /** Принудительно тёмная тема. */
    DARK;

    companion object {
        /**
         * Безопасно восстанавливает [ThemeMode] из строкового представления.
         *
         * Защищает от старых/повреждённых данных в DataStore: если строка
         * неизвестна — возвращает [SYSTEM] вместо краша.
         *
         * @param value Строковое значение (например, "SYSTEM").
         * @return Соответствующий [ThemeMode] или [SYSTEM], если значение неизвестно.
         */
        fun fromString(value: String): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
