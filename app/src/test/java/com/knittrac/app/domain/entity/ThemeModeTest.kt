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
    fun `fromString restores SYSTEM from canonical name`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
    }

    @Test
    fun `fromString restores LIGHT from canonical name`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
    }

    @Test
    fun `fromString restores DARK from canonical name`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
    }

    // ---------- Fallback ----------

    @Test
    fun `fromString falls back to SYSTEM on unknown value`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("INVALID"))
    }

    @Test
    fun `fromString falls back to SYSTEM on empty string`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
    }

    @Test
    fun `fromString falls back to SYSTEM on whitespace`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("   "))
    }

    @Test
    fun `fromString is case-sensitive — lowercase falls back to SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("light"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("dark"))
    }

    @Test
    fun `fromString does not trim — trailing space falls back to SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM "))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(" SYSTEM"))
    }

    // ---------- Контракт enum ----------

    @Test
    fun `entries contain three modes in stable order`() {
        assertEquals(
            listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
            ThemeMode.entries.toList()
        )
    }

    @Test
    fun `name values match canonical strings stored in DataStore`() {
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeMode.DARK.name)
    }

    @Test
    fun `fromString is inverse of name for all entries`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromString(mode.name))
        }
    }
}
