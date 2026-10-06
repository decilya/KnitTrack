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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Модель поддерживаемого языка.
 *
 * @property code ISO 639-1 код языка (например, "ru", "en").
 * @property displayName Название языка на самом языке ("Русский", "English").
 */
data class LanguageOption(val code: String, val displayName: String)

/**
 * Менеджер локализации приложения.
 *
 * Особенности:
 * - Список поддерживаемых языков читается из res/values/arrays.xml,
 *   что позволяет добавлять новые языки БЕЗ правок Kotlin-кода.
 * - Первый запуск: берёт язык системы, если он поддерживается; иначе fallback "en".
 * - Смена языка: сохраняет в DataStore + применяет к Configuration + требует recreate().
 */
@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val languageKey = stringPreferencesKey("app_language")

    /**
     * Список поддерживаемых языков, читается из ресурсов.
     * Кэшируется через lazy — ресурсы не меняются в рантайме.
     * 
     * ВАЖНО: включает защиту от рассогласования размеров массивов codes и names,
     * что критично при ручном добавлении новых языков в arrays.xml.
     */
    val supportedLanguages: List<LanguageOption> by lazy {
        val codes = context.resources.getStringArray(R.array.supported_language_codes)
        val names = context.resources.getStringArray(R.array.supported_language_names)
        
        if (codes.size != names.size) {
            Timber.e(
                "Language arrays mismatch: codes=%d, names=%d. " +
                "Check res/values/arrays.xml — количество элементов должно совпадать!",
                codes.size, names.size
            )
            emptyList()
        } else {
            codes.zip(names) { code, name -> LanguageOption(code, name) }
        }
    }

    /**
     * Язык по умолчанию.
     * Если системный язык поддерживается — используется он, иначе fallback (en).
     */
    private val defaultLanguage: String
        get() {
            val systemLang = Locale.getDefault().language
            return if (supportedLanguages.any { it.code == systemLang }) systemLang
            else FALLBACK_LANGUAGE
        }

    /**
     * Инициализирует язык при первом запуске и применяет сохранённый язык при каждом старте.
     *
     * Вызывается синхронно из Application.onCreate — до первого рендера UI,
     * чтобы первый кадр показывался на нужном языке.
     */
    suspend fun ensureLanguageInitialized() {
        val prefs = context.dataStore.data.first()
        val storedLanguage = prefs[languageKey]
        val initial = storedLanguage ?: defaultLanguage

        // Если язык ранее не сохранялся — сохраняем его для консистентности.
        if (storedLanguage == null) {
            context.dataStore.edit { it[languageKey] = initial }
        }

        // Всегда применяем к Configuration — на первом и последующих запусках.
        updateLocale(initial)
    }

    /**
     * Поток текущей локали (для чтения, если нужно в UI).
     */
    fun getCurrentLocaleFlow(): Flow<Locale> =
        context.dataStore.data.map { prefs -> Locale(prefs[languageKey] ?: defaultLanguage) }

    /**
     * Поток текущего кода языка (для UI-селектора в настройках).
     */
    fun getCurrentLanguageFlow(): Flow<String> =
        context.dataStore.data.map { prefs -> prefs[languageKey] ?: defaultLanguage }

    /**
     * Устанавливает язык пользователя.
     *
     * @throws IllegalArgumentException если код языка не поддерживается.
     * @param langCode ISO 639-1 код языка.
     */
    suspend fun setLanguage(langCode: String) {
        require(supportedLanguages.any { it.code == langCode }) {
            "Unsupported language: $langCode. Supported: ${supportedLanguages.map { it.code }}"
        }
        context.dataStore.edit { it[languageKey] = langCode }
        updateLocale(langCode)
    }

    /**
     * Применяет локаль к ресурсам приложения.
     * Использует deprecated API updateConfiguration — актуально до Android 13 (T).
     */
    private fun updateLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /**
     * Возвращает локализованное название категории.
     */
    fun getLocalizedCategoryName(category: Category): String = when (category) {
        Category.KNITTING -> context.getString(R.string.category_knitting)
        Category.CROCHET -> context.getString(R.string.category_crochet)
        Category.BEADING -> context.getString(R.string.category_beading)
        Category.MACRAME -> context.getString(R.string.category_macrame)
        Category.OTHER -> context.getString(R.string.category_other)
    }

    companion object {
        /** Fallback-язык, если системный не поддерживается. */
        const val FALLBACK_LANGUAGE = "en"
    }
}
