package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Юнит-тесты для [ProjectId].
 *
 * Покрывают фабрики: [ProjectId.create] (с валидацией `> 0`),
 * [ProjectId.createOrNull] (`0` → [ProjectId.ZERO], `< 0` → null),
 * [ProjectId.createOrDefault] (`< 0` → [ProjectId.ZERO]).
 *
 * Граница `0` — маркер нового объекта: валиден для [ProjectId.ZERO],
 * но не для [ProjectId.create].
 */
class ProjectIdTest {

    /**
     * Положительное значение создаёт валидный [ProjectId].
     */
    @Test
    fun create_withPositiveValue_returnsId() {
        val id = ProjectId.create(42L)
        assertEquals(42L, id.value)
    }

    /**
     * `0` недопустим для [ProjectId.create] (это маркер нового объекта,
     * создаётся только через [ProjectId.ZERO]).
     */
    @Test
    fun create_withZero_throws() {
        try {
            ProjectId.create(0L)
            throw AssertionError("Ожидалось IllegalArgumentException для 0")
        } catch (e: IllegalArgumentException) {
            // ожидаемо
        }
    }

    /**
     * Отрицательное значение недопустимо для [ProjectId.create].
     */
    @Test
    fun create_withNegative_throws() {
        try {
            ProjectId.create(-1L)
            throw AssertionError("Ожидалось IllegalArgumentException для -1")
        } catch (e: IllegalArgumentException) {
            // ожидаемо
        }
    }

    /**
     * [ProjectId.createOrNull] с `0` возвращает [ProjectId.ZERO]
     * (новый объект — валидное состояние до сохранения в БД).
     */
    @Test
    fun createOrNull_withZero_returnsZero() {
        val id = ProjectId.createOrNull(0L)
        assertEquals(ProjectId.ZERO, id)
    }

    /**
     * [ProjectId.createOrNull] с положительным значением возвращает [ProjectId].
     */
    @Test
    fun createOrNull_withPositive_returnsId() {
        val id = ProjectId.createOrNull(7L)
        assertEquals(7L, id?.value)
    }

    /**
     * [ProjectId.createOrNull] с отрицательным значением возвращает null
     * (повреждённые данные — безопасный отказ).
     */
    @Test
    fun createOrNull_withNegative_returnsNull() {
        assertNull(ProjectId.createOrNull(-5L))
    }

    /**
     * [ProjectId.createOrDefault] с отрицательным значением возвращает
     * [ProjectId.ZERO] (safe-fallback при восстановлении из БД/DTO).
     */
    @Test
    fun createOrDefault_withNegative_returnsZero() {
        assertEquals(ProjectId.ZERO, ProjectId.createOrDefault(-1L))
    }

    /**
     * [ProjectId.createOrDefault] с положительным значением возвращает [ProjectId].
     */
    @Test
    fun createOrDefault_withPositive_returnsId() {
        assertEquals(15L, ProjectId.createOrDefault(15L).value)
    }
}
