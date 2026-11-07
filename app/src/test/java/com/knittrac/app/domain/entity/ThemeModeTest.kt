package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Юнит-тесты для [ThemeMode].
 *
 * Покрываем контракт [ThemeMode.fromString]:
 * - корректное восстановление из канонических имён;
 * - fallback на [ThemeMode.SYSTEM] при неизвестных значениях;
 * - регистрозависимость;
 * - устойчивость к пустым строкам и пробелам.
 *
 * Отдельно фиксируем порядок [ThemeMode.entries] и значения name —
 * они идут в DataStore как строки и должны быть стабильны между версиями.
 */
class ThemeModeTest {

    // ---------- Валидные значения ----------

    @Test
    fun fromStringRestoresSYSTEMFromCanonicalName() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
    }

    @Test
    fun fromStringRestoresLIGHTFromCanonicalName() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
    }

    @Test
    fun fromStringRestoresDARKFromCanonicalName() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
    }

    // ---------- Fallback ----------

    @Test
    fun fromStringFallsBackToSYSTEMOnUnknownValue() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("INVALID"))
    }

    @Test
    fun fromStringFallsBackToSYSTEMOnEmptyString() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
    }

    @Test
    fun fromStringFallsBackToSYSTEMOnWhitespace() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("   "))
    }

    @Test
    fun fromStringIsCaseSensitiveLowercaseFallsBackToSYSTEM() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("light"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("dark"))
    }

    @Test
    fun fromStringDoesNotTrimTrailingSpaceFallsBackToSYSTEM() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM "))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(" SYSTEM"))
    }

    // ---------- Контракт enum ----------

    @Test
    fun entriesContainThreeModesInStableOrder() {
        assertEquals(
            listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
            ThemeMode.entries.toList()
        )
    }

    @Test
    fun nameValuesMatchCanonicalStringsStoredInDataStore() {
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeMode.DARK.name)
    }

    @Test
    fun fromStringIsInverseOfNameForAllEntries() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromString(mode.name))
        }
    }
}
