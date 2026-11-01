package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AddSymbolServiceTest {
    private val repo: PatternRepository = mockk()
    private val service = AddSymbolService(repo)

    private fun params(): AddSymbolParams = AddSymbolParams(
        patternId = PatternId.create(1L),
        code = "NEW",
        displayName = "Новый",
        textGlyph = "X"
    )

    /**
     * Проверяет успешное добавление нового кастомного символа.
     */
    @Test
    fun `add success`() = runTest {
        coEvery { repo.getSymbolsForPattern(any()) } returns Result.Success(emptyList())
        coEvery { repo.addSymbol(any()) } returns Result.Success(Unit)

        val result = service.add(params())

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.code == "NEW")
    }

    /**
     * Проверяет, что при попытке добавить символ с уже существующим кодом
     * возвращается ValidationError.
     */
    @Test
    fun `add duplicate code returns ValidationError`() = runTest {
        val existing = Symbol("NEW", null, "Существующий", "Y", SymbolCategory.STANDARD)
        coEvery { repo.getSymbolsForPattern(any()) } returns Result.Success(listOf(existing))

        val result = service.add(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * Проверяет, что ошибка БД при попытке сохранить символ пробрасывается наружу.
     */
    @Test
    fun `add database error on addSymbol propagates`() = runTest {
        coEvery { repo.getSymbolsForPattern(any()) } returns Result.Success(emptyList())
        coEvery { repo.addSymbol(any()) } returns Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = service.add(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}