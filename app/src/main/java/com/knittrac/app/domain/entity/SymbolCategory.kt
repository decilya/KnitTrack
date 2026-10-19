package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Категория символа схемы.
 *
 * [STANDARD] — системный символ (глобальный справочник, ownerPatternId = null).
 * [CUSTOM] — кастомный символ конкретной схемы (ownerPatternId != null).
 *
 * ВАЖНО: enum НЕ содержит displayName. Локализация — через
 * SymbolCategoryLocalizer (Presentation), по аналогии с Category.
 */
@Keep
enum class SymbolCategory {
    /** Системный символ (глобальный справочник). */
    STANDARD,

    /** Кастомный символ конкретной схемы. */
    CUSTOM;

    companion object {
        /**
         * Безопасно восстанавливает [SymbolCategory] из строкового представления.
         *
         * @param name Строковое имя категории.
         * @return Соответствующая [SymbolCategory] или [STANDARD] при неизвестном значении.
         */
        fun fromNameOrDefault(name: String): SymbolCategory =
            entries.firstOrNull { it.name == name } ?: STANDARD
    }
}
