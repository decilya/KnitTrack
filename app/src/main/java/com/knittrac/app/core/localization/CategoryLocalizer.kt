package com.knittrac.app.core.localization

import android.content.Context
import com.knittrac.app.R
import com.knittrac.app.domain.entity.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Локализатор категорий проектов.
 *
 * Отвечает исключительно за преобразование enum [Category] в локализованную
 * строку. Выделен из [LocaleManager] для соблюдения принципа единственной
 * ответственности (SRP): LocaleManager управляет языком приложения,
 * а CategoryLocalizer — отображением категорий.
 *
 * @param context Контекст приложения для доступа к строковым ресурсам.
 */
@Singleton
class CategoryLocalizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Возвращает локализованное название категории.
     *
     * Использует [when] по всем константам [Category] — компилятор
     * гарантирует полноту ветвления: при добавлении новой категории
     * сборка упадёт, пока не будет добавлена соответствующая ветка.
     *
     * @param category Категория проекта для локализации.
     * @return Строковое название категории на текущем языке.
     */
    fun localize(category: Category): String = when (category) {
        Category.KNITTING -> context.getString(R.string.category_knitting)
        Category.CROCHET -> context.getString(R.string.category_crochet)
        Category.BEADING -> context.getString(R.string.category_beading)
        Category.MACRAME -> context.getString(R.string.category_macrame)
        Category.OTHER -> context.getString(R.string.category_other)
    }
}
