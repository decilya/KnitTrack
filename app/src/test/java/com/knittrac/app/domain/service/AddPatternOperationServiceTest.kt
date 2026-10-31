package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRepeat
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.model.AddPatternOperationParams
import com.knittrac.app.domain.model.OperationSpan
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [AddPatternOperationService].
 *
 * Покрывают: успешное добавление, символ не в легенде, раппорт не найден,
 * overlap (точное совпадение стартовой колонки и частичное перекрытие),
 * касание границ (не считается overlap), и проброс ошибок репозитория.
 */
class AddPatternOperationServiceTest {

    private val patternRepository: PatternRepository = mockk()
    private val service = AddPatternOperationService(patternRepository)

    private fun symbol(
        code: String = "K",
        span: Span = Span.ONE
    ): Symbol = Symbol(
        code = code,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD,
        defaultColumnSpan = span
    )

    private fun row(id: Long = 10L, number: Int = 1): PatternRow = PatternRow(
        id = PatternRowId.create(id),
        rowNumber = RowNumber.create(number)
    )

    private fun pattern(
        legend: Set<Symbol> = setOf(symbol()),
        repeats: List<PatternRepeat> = emptyList(),
        rows: List<PatternRow> = listOf(row())
    ): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        legend = legend,
        rows = rows,
        repeats = repeats
    )

    private fun params(
        columnStart: Int = 0,
        repeatCode: String? = null,
        overrideSpan: Span? = null,
        sym: Symbol = symbol()
    ): AddPatternOperationParams = AddPatternOperationParams(
        patternId = PatternId.create(1L),
        row = row(),
        columnStart = ColumnStart.create(columnStart),
        symbol = sym,
        repeatCode = repeatCode,
        overrideSpan = overrideSpan
    )

    @Test
    fun add_success_returnsOperation() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns Result.Success(emptyList())
        coEvery { patternRepository.addOperation(any()) } returns Result.Success(Unit)

        val result = service.add(params())

        assertTrue(result is Result.Success)
        val op = (result as Result.Success).data
        assertEquals("K", op.symbol.code)
        assertEquals(0, op.columnStart.value)
    }

    @Test
    fun add_symbolNotInLegend_returnsValidationError() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(legend = emptySet()))

        val result = service.add(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun add_repeatCodeNotFound_returnsValidationError() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())

        val result = service.add(params(repeatCode = "X"))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun add_overlap_sameColumn_returnsValidationError() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Success(listOf(OperationSpan(ColumnStart.create(0), Span.ONE)))

        val result = service.add(params(columnStart = 0))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun add_overlap_partial_returnsValidationError() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Success(listOf(OperationSpan(ColumnStart.create(2), Span.create(3))))

        val result = service.add(params(columnStart = 4))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun add_adjacentSpans_noOverlap_success() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Success(listOf(OperationSpan(ColumnStart.create(0), Span.ONE)))
        coEvery { patternRepository.addOperation(any()) } returns Result.Success(Unit)

        val result = service.add(params(columnStart = 1))

        assertTrue(result is Result.Success)
    }

    @Test
    fun add_patternRepositoryError_propagates() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Error(AppError.NotFoundError("нет"))

        val result = service.add(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    @Test
    fun add_spansRepositoryError_propagates() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = service.add(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }

    @Test
    fun add_addOperationError_propagates() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns Result.Success(emptyList())
        coEvery { patternRepository.addOperation(any()) } returns
            Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = service.add(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }

    /**
     * Регрессия 7g-C (D8): overlap-check использует defaultColumnSpan символа
     * из legend схемы, а не из params.symbol. params.symbol может прийти
     * из UI-кеша с устаревшим span — иначе реальный overlap по legend-span
     * будет пропущен.
     *
     * Сценарий:
     * - params.symbol.defaultColumnSpan = 1 (устаревший, слишком узкий)
     * - legendSymbol.defaultColumnSpan = 3 (истинный)
     * - columnStart = 0, overrideSpan = null
     * - существующая операция: [2, 3)
     *
     * stale-span: [0, 1) не пересекается с [2, 3) -> Success (баг до фикса)
     * legend-span: [0, 3) пересекается с [2, 3) -> ValidationError (после)
     */
    @Test
    fun add_staleSpanInParams_usesLegendSpanForOverlap() = runTest {
        val staleSymbol = symbol(span = Span.ONE)
        val legendSymbol = symbol(span = Span.create(3))
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(legend = setOf(legendSymbol)))
        coEvery { patternRepository.getOperationSpansForRow(any()) } returns
            Result.Success(listOf(OperationSpan(ColumnStart.create(2), Span.ONE)))

        val result = service.add(params(columnStart = 0, sym = staleSymbol))

        assertTrue(
            "Ожидался ValidationError: overlap должен считаться по legend-span, не params-span",
            result is Result.Error
        )
        assertTrue(
            (result as Result.Error).error is AppError.ValidationError
        )
    }
}
