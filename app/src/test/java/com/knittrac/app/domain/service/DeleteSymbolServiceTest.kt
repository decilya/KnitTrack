package com.knittrac.app.domain.service

import com.knittrac.app.core.common.ErrorCode
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

class DeleteSymbolServiceTest {
    private val repo: PatternRepository = mockk()
    private val service = DeleteSymbolService(repo)

    private fun params(): DeleteSymbolParams = DeleteSymbolParams(PatternId.create(1L), "CUSTOM")

    /**
     * Проверяет успешное удаление кастомного символа, принадлежащего схеме.
     */
    @Test
    fun deleteCustomSymbolSuccess() = runTest {
        val custom = Symbol(
            code = "CUSTOM",
            ownerPatternId = PatternId.create(1L),
            displayName = "Мой",
            textGlyph = "X",
            category = SymbolCategory.CUSTOM
        )
        coEvery { repo.getSymbolsForPattern(any()) } returns Result.Success(listOf(custom))
        coEvery { repo.deleteSymbol(any(), any()) } returns Result.Success(Unit)

        val result = service.delete(params())

        assertTrue(result is Result.Success)
    }

    /**
     * Проверяет, что системный символ (ownerPatternId = null) удалить нельзя.
     */
    @Test
    fun deleteSystemSymbolReturnsValidationError() = runTest {
        val system = Symbol(
            code = "K",
            ownerPatternId = null,
            displayName = "Лицевая",
            textGlyph = "V",
            category = SymbolCategory.STANDARD
        )
        coEvery { repo.getSymbolsForPattern(any()) } returns Result.Success(listOf(system))

        val result = service.delete(DeleteSymbolParams(PatternId.create(1L), "K"))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * Проверяет, что если символ используется в операциях, сервис
     * возвращает ValidationError (от репозитория), а не падает с ошибкой БД.
     */
    @Test
    fun deleteSymbolInUsePropagatesValidationError() = runTest {
        val custom = Symbol(
            code = "CUSTOM",
            ownerPatternId = PatternId.create(1L),
            displayName = "Мой",
            textGlyph = "X",
            category = SymbolCategory.CUSTOM
        )
        coEvery { repo.getSymbolsForPattern(any()) } returns Result.Success(listOf(custom))
        coEvery { repo.deleteSymbol(any(), any()) } returns Result.Error(AppError.ValidationError(ErrorCode.INVALID_ARGUMENT, "используется"))

        val result = service.delete(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }
}
