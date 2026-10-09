package com.knittrac.app.core.localization

import android.content.Context
import android.content.res.Resources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.knittrac.app.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Юнит-тесты для [LocaleManager].
 *
 * Покрывают:
 * - стартовое состояние флага isInitialized;
 * - загрузку supportedLanguages из ресурсов (включая mismatch);
 * - валидацию кода в setLanguage;
 * - логику ensureLanguageInitialized (первый запуск, повторный, ошибка I/O).
 *
 * DataStore<Preferences> заменён на [FakePreferencesDataStore] —
 * in-memory реализацию. mockk<DataStore<Preferences>> не подходит,
 * потому что edit — extension-функция, её нельзя замокать.
 *
 * Android API (Resources, Configuration) мокается через MockK — тесты
 * не требуют Robolectric. Приватный метод updateLocale подменяется
 * через spyk: context.resources.configuration в unit-тестах бросает
 * RuntimeException("Stub!"), поэтому реальный вызов невозможен.
 *
 * Locale.setDefault мутирует глобальное JVM-состояние — оригинал
 * сохраняется в setUp и восстанавливается в tearDown.
 */
class LocaleManagerTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var dataStore: FakePreferencesDataStore

    private val languageKey = stringPreferencesKey("app_language")

    private var originalLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()

        context = mockk()
        resources = mockk()
        dataStore = FakePreferencesDataStore()

        every { context.resources } returns resources
        every { resources.getStringArray(R.array.supported_language_codes) } returns
            arrayOf("en", "ru")
        every { resources.getStringArray(R.array.supported_language_names) } returns
            arrayOf("English", "Русский")
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    /**
     * Флаг isInitialized должен стартовать false — splash держится,
     * пока ensureLanguageInitialized не отработает.
     */
    @Test
    fun `isInitialized starts as false`() {
        val manager = LocaleManager(context, dataStore)

        assertFalse(manager.isInitialized.value)
    }

    /**
     * FALLBACK_LANGUAGE — публичная константа. Проверяем значение,
     * чтобы случайное изменение не сломало fallback на английский.
     */
    @Test
    fun `FALLBACK_LANGUAGE is en`() {
        assertEquals("en", LocaleManager.FALLBACK_LANGUAGE)
    }

    /**
     * При рассогласовании размеров массивов codes и names
     * supportedLanguages должен вернуть пустой список,
     * а не упасть с IndexOutOfBounds.
     */
    @Test
    fun `supportedLanguages returns emptyList when arrays mismatch`() {
        every { resources.getStringArray(R.array.supported_language_codes) } returns
            arrayOf("en")
        every { resources.getStringArray(R.array.supported_language_names) } returns
            arrayOf("English", "Русский")

        val manager = LocaleManager(context, dataStore)

        assertTrue(manager.supportedLanguages.isEmpty())
    }

    /**
     * Нормальный случай: codes и names zip-ятся в LanguageOption
     * с сохранением порядка. Expected строится через тот же zip,
     * поэтому тест не сломается при добавлении нового языка в arrays.xml.
     */
    @Test
    fun `supportedLanguages zips codes and names correctly`() {
        val codes = arrayOf("en", "ru")
        val names = arrayOf("English", "Русский")
        every { resources.getStringArray(R.array.supported_language_codes) } returns codes
        every { resources.getStringArray(R.array.supported_language_names) } returns names

        val manager = LocaleManager(context, dataStore)

        val expected = codes.zip(names) { c, n -> LanguageOption(c, n) }.toList()
        assertEquals(expected, manager.supportedLanguages)
    }

    /**
     * setLanguage с неподдерживаемым кодом должен бросить
     * IllegalArgumentException до обращения к DataStore.
     *
     * try/catch вместо assertThrows: assertThrows использует
     * runBlocking, который конфликтует с runTest scheduler.
     */
    @Test
    fun `setLanguage throws IllegalArgumentException on unsupported code`() = runTest {
        val manager = LocaleManager(context, dataStore)

        var thrown: Throwable? = null
        try {
            manager.setLanguage("ja")
        } catch (e: Throwable) {
            thrown = e
        }

        assertTrue(
            "Ожидался IllegalArgumentException, получено: $thrown",
            thrown is IllegalArgumentException
        )
    }

    /**
     * Пустой код тоже не поддерживается — require-проверка
     * должна бросить IllegalArgumentException.
     */
    @Test
    fun `setLanguage throws IllegalArgumentException on empty code`() = runTest {
        val manager = LocaleManager(context, dataStore)

        var thrown: Throwable? = null
        try {
            manager.setLanguage("")
        } catch (e: Throwable) {
            thrown = e
        }

        assertTrue(
            "Ожидался IllegalArgumentException, получено: $thrown",
            thrown is IllegalArgumentException
        )
    }

    /**
     * ensureLanguageInitialized в любом случае (успех или ошибка)
     * выставляет isInitialized = true через finally — splash скроется.
     */
    @Test
    fun `ensureLanguageInitialized sets isInitialized to true on success`() = runTest {
        val manager = spyk(LocaleManager(context, dataStore))
        every { manager.updateLocale(any()) } returns Unit

        manager.ensureLanguageInitialized()

        assertTrue(manager.isInitialized.value)
    }

    /**
     * Даже если updateLocale упал — isInitialized должен стать true
     * через finally. Лучше показать UI на системном языке, чем
     * застрять на splash-экране навсегда.
     */
    @Test
    fun `ensureLanguageInitialized sets isInitialized to true even on error`() = runTest {
        val manager = spyk(LocaleManager(context, dataStore))
        every { manager.updateLocale(any()) } throws RuntimeException("test error")

        try {
            manager.ensureLanguageInitialized()
        } catch (_: RuntimeException) {
            // ожидаемо — падение из updateLocale
        }

        assertTrue(manager.isInitialized.value)
    }

    /**
     * Первый запуск: в DataStore нет сохранённого языка → берётся
     * defaultLanguage и сохраняется через edit для консистентности.
     * Проверяем состояние DataStore после вызова.
     */
    @Test
    fun `ensureLanguageInitialized saves default language when none stored`() = runTest {
        val manager = spyk(LocaleManager(context, dataStore))
        every { manager.updateLocale(any()) } returns Unit

        manager.ensureLanguageInitialized()

        val saved = dataStore.data.first()[languageKey]
        assertTrue("Язык должен быть сохранён после первого запуска", saved != null)
    }

    /**
     * Повторный запуск: язык уже сохранён → edit не должен вызываться,
     * иначе мы бы перезаписывали выбор пользователя при каждом старте.
     * Проверяем, что сохранённое значение не изменилось.
     */
    @Test
    fun `ensureLanguageInitialized does not overwrite existing language`() = runTest {
        // Предварительно сохраняем язык — имитация второго запуска.
        dataStore.edit { it[languageKey] = "en" }

        val manager = spyk(LocaleManager(context, dataStore))
        every { manager.updateLocale(any()) } returns Unit

        manager.ensureLanguageInitialized()

        val saved = dataStore.data.first()[languageKey]
        assertEquals("en", saved)
    }

    /**
     * In-memory реализация DataStore<Preferences> для тестов.
     *
     * Заменяет mockk, потому что edit — extension-функция на DataStore,
     * её нельзя замокать через MockK. Эта реализация ведёт себя как
     * настоящий DataStore: updateData + edit работают по контракту API.
     */
    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences {
            val newValue = transform(state.value)
            state.value = newValue
            return newValue
        }
    }
}
