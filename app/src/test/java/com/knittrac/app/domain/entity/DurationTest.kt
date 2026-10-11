package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Юнит-тесты для [Duration].
 *
 * Покрывают:
 * - валидацию неотрицательности в [Duration.create];
 * - конвертацию из миллисекунд в [Duration.fromMillis];
 * - безопасный парсинг через [Duration.createOrNull];
 * - контракт [Duration.ZERO];
 * - структурное равенство value class.
 */
class DurationTest {

    /**
     * create(0) возвращает Duration с нулём секунд.
     */
    @Test
    fun `create accepts zero seconds`() {
        val d = Duration.create(0L)

        assertEquals(0L, d.seconds)
    }

    /**
     * create с положительным значением сохраняет его без изменений.
     */
    @Test
    fun `create preserves positive value`() {
        val d = Duration.create(3600L)

        assertEquals(3600L, d.seconds)
    }

    /**
     * create на отрицательном значении бросает IllegalArgumentException.
     */
    @Test
    fun `create throws on negative seconds`() {
        assertThrows(IllegalArgumentException::class.java) {
            Duration.create(-1L)
        }
    }

    /**
     * fromMillis округляет вниз до целых секунд.
     */
    @Test
    fun `fromMillis rounds down to whole seconds`() {
        assertEquals(1L, Duration.fromMillis(1500L).seconds)
        assertEquals(0L, Duration.fromMillis(999L).seconds)
        assertEquals(2L, Duration.fromMillis(2999L).seconds)
    }

    /**
     * fromMillis на отрицательном значении бросает IllegalArgumentException.
     */
    @Test
    fun `fromMillis throws on negative millis`() {
        assertThrows(IllegalArgumentException::class.java) {
            Duration.fromMillis(-1L)
        }
    }

    /**
     * createOrNull на отрицательном значении возвращает null без исключения.
     */
    @Test
    fun `createOrNull returns null on negative seconds`() {
        assertNull(Duration.createOrNull(-1L))
        assertNull(Duration.createOrNull(Long.MIN_VALUE))
    }

    /**
     * createOrNull на валидном значении возвращает Duration.
     */
    @Test
    fun `createOrNull returns Duration on valid seconds`() {
        val d = Duration.createOrNull(60L)

        assertEquals(60L, d?.seconds)
    }

    /**
     * ZERO — константа с нулём секунд. Используется в мапперах
     * как fallback для невалидных данных.
     */
    @Test
    fun `ZERO has zero seconds`() {
        assertEquals(0L, Duration.ZERO.seconds)
    }

    /**
     * value class с одинаковым value равны по структурному равенству.
     */
    @Test
    fun `Duration with same value is equal`() {
        val a = Duration.create(100L)
        val b = Duration.create(100L)

        assertEquals(a, b)
    }
}
