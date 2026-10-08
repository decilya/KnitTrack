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
    OTHER
}