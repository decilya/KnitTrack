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
    fun fromstringRestoresSystemFromCanonicalName() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
    }

    @Test
    fun fromstringRestoresLightFromCanonicalName() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
    }

    @Test
    fun fromstringRestoresDarkFromCanonicalName() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
    }

    // ---------- Fallback ----------

    @Test
    fun fromstringFallsBackToSystemOnUnknownValue() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("INVALID"))
    }

    @Test
    fun fromstringFallsBackToSystemOnEmptyString() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
    }

    @Test
    fun fromstringFallsBackToSystemOnWhitespace() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("   "))
    }

    @Test
    fun fromstringIsCaseSensitiveLowercaseFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("light"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("dark"))
    }

    @Test
    fun fromstringDoesNotTrimTrailingSpaceFallsBackToSystem() {
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
    fun nameValuesMatchCanonicalStringsStoredInDatastore() {
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeMode.DARK.name)
    }

    @Test
    fun fromstringIsInverseOfNameForAllEntries() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromString(mode.name))
        }
    }
}
