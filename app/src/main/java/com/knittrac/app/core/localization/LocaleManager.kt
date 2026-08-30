package com.knittrac.app.core.localization

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.knittrac.app.R
import com.knittrac.app.domain.entity.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// Расширение для создания DataStore с именем "settings"
// Это позволяет хранить настройки приложения (включая выбранный язык)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Менеджер управления локализацией приложения.
 *
 * Этот класс отвечает за:
 * 1. Хранение выбранного пользователем языка в DataStore
 * 2. Применение локали к ресурсам приложения
 * 3. Предоставление локализованных названий для категорий
 *
 * Использует DataStore Preferences для надежного хранения настроек
 * и Flow для реактивного обновления UI при смене языка.
 *
 * @param context Контекст приложения (помечен [@ApplicationContext] для избежания утечек памяти).
 *
 * @see Category Категории проектов, требующие локализации.
 */
@Singleton
class LocaleManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // Ключ для хранения выбранного языка в DataStore
    private val languageKey = stringPreferencesKey("app_language")

    /**
     * Возвращает поток текущей локали приложения.
     *
     * Этот Flow автоматически обновляется при изменении настроек языка
     * в DataStore, что позволяет UI реактивно обновляться при смене языка.
     *
     * @return [Flow] с текущей [Locale]. Если язык не выбран, возвращается
     *         системная локаль по умолчанию.
     */
    fun getCurrentLocaleFlow(): Flow<Locale> = context.dataStore.data.map { prefs ->
        // Получаем код языка из DataStore или используем системную локаль
        val languageCode = prefs[languageKey] ?: Locale.getDefault().language
        Locale(languageCode)
    }

    /**
     * Устанавливает новый язык приложения и обновляет конфигурацию ресурсов.
     *
     * Этот метод:
     * 1. Сохраняет выбранный язык в DataStore (персистентное хранение)
     * 2. Создает новую [Locale] с указанным кодом
     * 3. Устанавливает её как локаль по умолчанию для JVM
     * 4. Обновляет конфигурацию ресурсов контекста
     *
     * После вызова этого метода необходимо перезапустить Activity
     * (через [androidx.activity.ComponentActivity.recreate]) для применения изменений.
     *
     * @param langCode Код языка (например, "ru" для русского, "en" для английского).
     *
     * @see getCurrentLocaleFlow Для наблюдения за изменениями языка.
     */
    suspend fun setLanguage(langCode: String) {
        // Сохраняем выбранный язык в DataStore
        context.dataStore.edit { prefs ->
            prefs[languageKey] = langCode
        }

        // Создаем новую локаль и применяем её
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        // Обновляем конфигурацию ресурсов
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }

        // Применяем конфигурацию к ресурсам (подавляем предупреждение о депрекации,
        // так как это единственный способ для Android < S)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /**
     * Возвращает локализованное название категории.
     *
     * Этот метод преобразует enum [Category] в строковое представление
     * на текущем языке, используя строковые ресурсы из `strings.xml`.
     *
     * @param category Категория проекта для локализации.
     * @return Строковое название категории на текущем языке.
     *
     * @see Category Enum с категориями проектов.
     * @see R.string.category_knitting Строковые ресурсы для категорий.
     */
    @Suppress("unused")
    fun getLocalizedCategoryName(category: Category): String = when (category) {
        Category.KNITTING -> context.getString(R.string.category_knitting)
        Category.CROCHET -> context.getString(R.string.category_crochet)
        Category.BEADING -> context.getString(R.string.category_beading)
        Category.MACRAME -> context.getString(R.string.category_macrame)
        Category.OTHER -> context.getString(R.string.category_other)
    }
}