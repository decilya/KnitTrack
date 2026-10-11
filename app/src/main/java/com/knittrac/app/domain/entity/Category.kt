package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Категории проектов для вязания и рукоделия.
 *
 * Этот enum определяет все доступные категории проектов в приложении.
 * Важно: enum НЕ содержит поля displayName для локализации.
 * Вместо этого используется LocaleManager (com.knittrac.app.core.localization.LocaleManager),
 * который получает локализованные названия из строковых ресурсов.
 *
 * Такой подход соответствует правилу разделения доменной логики
 * и представления (Domain/Presentation separation).
 *
 * @see com.knittrac.app.core.localization.LocaleManager Менеджер для получения локализованных названий.
 * @see com.knittrac.app.R Строковые ресурсы для каждой категории.
 */
@Keep
enum class Category {
    /** Вязание спицами. */
    KNITTING,

    /** Вязание крючком. */
    CROCHET,

    /** Бисероплетение. */
    BEADING,

    /** Макраме (узелковое плетение). */
    MACRAME,

    /** Другие виды рукоделия. */
    OTHER;

    companion object {
        /**
         * Безопасно восстанавливает [Category] из строкового представления.
         *
         * Используется при чтении из БД, DTO или JSON, где данные могли быть
         * повреждены (опечатка, миграция из будущей версии, ручное редактирование
         * бэкапа). Вместо падения с IllegalArgumentException возвращает [OTHER].
         *
         * @param name Строковое имя категории.
         * @return Соответствующая [Category] или [OTHER] при неизвестном значении.
         */
        fun fromNameOrDefault(name: String): Category =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
