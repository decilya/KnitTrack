package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.model.OperationSpan
import com.knittrac.app.domain.model.UpdatePatternOperationParams
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для [UpdatePatternOperationService] (7f-D-1).
 * Покрывают сценарии успешного обновления, ошибки легенды и пересечений.
 */
class UpdatePatternOperationServiceTest {

    private val patternRepository: PatternRepository = mockk()
    private val service = UpdatePatternOperationService(patternRepository)

    private fun symbol(code: String = "K", span: Span = Span.ONE): Symbol = Symbol(
        code = code,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD,
        defaultColumnSpan = span
    )

    private fun pattern(legend: Set<Symbol> = setOf(symbol())): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        legend = legend,
        rows = listOf(PatternRow(PatternRowId.create(10L), RowNumber.ONE))
    )

    private fun params(
        columnStart: Int = 0, 
        sym: Symbol = symbol(),
        overrideSpan: Span? = null
    ): UpdatePatternOperationParams =
        UpdatePatternOperationParams(
            patternId = PatternId.create(1L),
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(columnStart),
            symbol = sym,
            overrideSpan = overrideSpan
        )

    @Test
    fun `update success returns updated operation`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns Result.Success(emptyList())
        coEvery { patternRepository.updateOperation(any()) } returns Result.Success(Unit)

        val result = service.update(params())

        assertTrue(result is Result.Success)
    }

    @Test
    fun `update with symbol not in legend returns ValidationError`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern(legend = emptySet()))

        val result = service.update(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun `update creating overlap with another column returns ValidationError`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        // Существующая операция на колонке 1 (соседняя)
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Success(listOf(OperationSpan(ColumnStart.create(1), Span.ONE)))

        // Пытаемся обновить операцию на колонке 0, но расширяем до span 2 (заденет колонку 1)
        val result = service.update(params(columnStart = 0, overrideSpan = Span.create(2)))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun `update overlap with self is ignored`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        // Существующая операция на колонке 0 (это мы ее обновляем)
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Success(listOf(OperationSpan(ColumnStart.create(0), Span.ONE)))
        coEvery { patternRepository.updateOperation(any()) } returns Result.Success(Unit)

        // Обновляем операцию на колонке 0, меняем символ, но не создаем новых пересечений
        val result = service.update(params(columnStart = 0))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `update propagating NotFoundError when operation missing`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns Result.Success(emptyList())
        coEvery { patternRepository.updateOperation(any()) } returns Result.Error(AppError.NotFoundError("нет"))

        val result = service.update(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    @Test
    fun `update with larger span no overlap returns success`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        // Соседняя операция на колонке 5
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Success(listOf(OperationSpan(ColumnStart.create(5), Span.ONE)))
        coEvery { patternRepository.updateOperation(any()) } returns Result.Success(Unit)

        // Обновляем операцию на колонке 0, расширяем до span 2 (0 и 1). До 5 не достает.
        val result = service.update(params(columnStart = 0, overrideSpan = Span.create(2)))

        assertTrue(result is Result.Success)
    }
}