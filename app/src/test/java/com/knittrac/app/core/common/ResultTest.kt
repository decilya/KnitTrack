package com.knittrac.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для комбинаторов [Result].
 *
 * Проверяют: преобразование успеха (`map`/`flatMap`), побочные эффекты
 * (`onSuccess`/`onError`), извлечение значения (`getOrNull`/`getOrDefault`/
 * `errorOrNull`), сохранение ошибки при цепочках.
 */
class ResultTest {

    private val error = AppError.ValidationError("test error")

    // --- map ---

    /** map применяет transform к Success. */
    @Test
    fun map_onSuccess_transformsValue() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertEquals(Result.Success(10), mapped)
    }

    /** map сохраняет Error без изменений. */
    @Test
    fun map_onError_preservesError() {
        val result: Result<Int> = Result.Error(error)
        val mapped = result.map { it * 2 }
        assertEquals(Result.Error(error), mapped)
    }

    // --- flatMap ---

    /** flatMap разворачивает вложенный Success. */
    @Test
    fun flatMap_onSuccess_chainsResult() {
        val result: Result<Int> = Result.Success(5)
        val chained = result.flatMap { Result.Success(it + 1) }
        assertEquals(Result.Success(6), chained)
    }

    /** flatMap сохраняет Error без вызова transform. */
    @Test
    fun flatMap_onError_preservesError() {
        val result: Result<Int> = Result.Error(error)
        val chained = result.flatMap { Result.Success(it + 1) }
        assertEquals(Result.Error(error), chained)
    }

    // --- onSuccess ---

    /** onSuccess вызывает action при Success. */
    @Test
    fun onSuccess_onSuccess_callsAction() {
        val result: Result<Int> = Result.Success(5)
        var called = false
        result.onSuccess { called = true }
        assertTrue(called)
    }

    /** onSuccess не вызывает action при Error. */
    @Test
    fun onSuccess_onError_doesNotCallAction() {
        val result: Result<Int> = Result.Error(error)
        var called = false
        result.onSuccess { called = true }
        assertTrue(!called)
    }

    // --- onError ---

    /** onError вызывает action при Error. */
    @Test
    fun onError_onError_callsAction() {
        val result: Result<Int> = Result.Error(error)
        var called = false
        result.onError { called = true }
        assertTrue(called)
    }

    /** onError не вызывает action при Success. */
    @Test
    fun onError_onSuccess_doesNotCallAction() {
        val result: Result<Int> = Result.Success(5)
        var called = false
        result.onError { called = true }
        assertTrue(!called)
    }

    // --- getOrNull ---

    /** getOrNull возвращает значение при Success. */
    @Test
    fun getOrNull_onSuccess_returnsData() {
        val result: Result<Int> = Result.Success(5)
        assertEquals(5, result.getOrNull())
    }

    /** getOrNull возвращает null при Error. */
    @Test
    fun getOrNull_onError_returnsNull() {
        val result: Result<Int> = Result.Error(error)
        assertNull(result.getOrNull())
    }

    // --- getOrDefault ---

    /** getOrDefault возвращает значение при Success. */
    @Test
    fun getOrDefault_onSuccess_returnsData() {
        val result: Result<Int> = Result.Success(5)
        assertEquals(5, result.getOrDefault(0))
    }

    /** getOrDefault возвращает default при Error. */
    @Test
    fun getOrDefault_onError_returnsDefault() {
        val result: Result<Int> = Result.Error(error)
        assertEquals(0, result.getOrDefault(0))
    }

    // --- errorOrNull ---

    /** errorOrNull возвращает null при Success. */
    @Test
    fun errorOrNull_onSuccess_returnsNull() {
        val result: Result<Int> = Result.Success(5)
        assertNull(result.errorOrNull())
    }

    /** errorOrNull возвращает ошибку при Error. */
    @Test
    fun errorOrNull_onError_returnsError() {
        val result: Result<Int> = Result.Error(error)
        assertEquals(error, result.errorOrNull())
    }
}
