package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.local.PatternRepeatDao
import com.knittrac.app.data.local.SymbolDao
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRepeatEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.data.local.entity.SymbolEntity
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.SQLException

/**
 * Юнит-тесты для [PatternRepositoryImpl].
 *
 * Проверяют сборку агрегата [Pattern] из 4 таблиц: patterns + rows +
 * symbols (legend) + pattern_repeats, а также обработку ошибок
 * (NotFound, DatabaseError).
 *
 * После 7d-prep [getPatternById] делает 4 запроса, все 3 DAO замоканы.
 */
class PatternRepositoryImplTest {

    private val patternDao: PatternDao = mockk()
    private val symbolDao: SymbolDao = mockk()
    private val patternRepeatDao: PatternRepeatDao = mockk()
    private val repository = PatternRepositoryImpl(patternDao, symbolDao, patternRepeatDao)

    private val entity = PatternEntity(
        id = 1L,
        name = "Схема",
        technique = "KNITTING",
        direction = "LTR",
        isReversible = false
    )
    private val rows = listOf(
        PatternRowEntity(id = 10L, patternId = 1L, rowNumber = 1),
        PatternRowEntity(id = 11L, patternId = 1L, rowNumber = 2)
    )
    private val symbols = listOf(
        SymbolEntity(code = "K", displayName = "Лицевая", textGlyph = "V", category = "STANDARD")
    )
    private val repeats = listOf(
        PatternRepeatEntity(patternId = 1L, code = "A", stitchCount = 10, rowCount = 4)
    )

    /**
     * Найденный pattern возвращается с непустыми legend и repeats.
     */
    @Test
    fun getPatternById_found_returnsPatternWithRowsAndLegendAndRepeats() = runTest {
        coEvery { patternDao.getPatternById(1L) } returns entity
        coEvery { patternDao.getRowsForPattern(1L) } returns rows
        coEvery { symbolDao.getSymbolsForPattern(1L) } returns symbols
        coEvery { patternRepeatDao.getRepeatsForPattern(1L) } returns repeats

        val result = repository.getPatternById(PatternId.create(1L))

        assertTrue(result is Result.Success)
        val pattern = (result as Result.Success).data
        assertEquals(2, pattern.rows.size)
        assertEquals(1, pattern.legend.size)
        assertEquals(SymbolCategory.STANDARD, pattern.legend.first().category)
        assertEquals(1, pattern.repeats.size)
        assertEquals("A", pattern.repeats.first().code)
    }

    /**
     * Пустые таблицы symbols/pattern_repeats — не ошибка, Pattern валиден.
     */
    @Test
    fun getPatternById_found_returnsEmptyLegendAndRepeats_whenDbEmpty() = runTest {
        coEvery { patternDao.getPatternById(1L) } returns entity
        coEvery { patternDao.getRowsForPattern(1L) } returns rows
        coEvery { symbolDao.getSymbolsForPattern(1L) } returns emptyList()
        coEvery { patternRepeatDao.getRepeatsForPattern(1L) } returns emptyList()

        val result = repository.getPatternById(PatternId.create(1L))

        assertTrue(result is Result.Success)
        val pattern = (result as Result.Success).data
        assertTrue(pattern.legend.isEmpty())
        assertTrue(pattern.repeats.isEmpty())
    }

    /**
     * Схема не найдена — NotFoundError, без обращения к rows/symbols/repeats.
     */
    @Test
    fun getPatternById_notFound_returnsNotFoundError() = runTest {
        coEvery { patternDao.getPatternById(99L) } returns null

        val result = repository.getPatternById(PatternId.create(99L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * Сбой БД при первом запросе — DatabaseError, без утечки throwable.
     */
    @Test
    fun getPatternById_sqlException_returnsDatabaseError() = runTest {
        coEvery { patternDao.getPatternById(1L) } throws SQLException("boom")

        val result = repository.getPatternById(PatternId.create(1L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }

    /**
     * getOperationSpansForRow мапит проекции в OperationSpan с VO.
     */
    @Test
    fun getOperationSpansForRow_mapsProjections() = runTest {
        coEvery { patternDao.getOperationSpansForRow(10L) } returns listOf(
            com.knittrac.app.data.local.OperationSpanProjection(columnStart = 0, effectiveSpan = 1),
            com.knittrac.app.data.local.OperationSpanProjection(columnStart = 2, effectiveSpan = 3)
        )

        val result = repository.getOperationSpansForRow(PatternRowId.create(10L))

        assertTrue(result is Result.Success)
        val spans = (result as Result.Success).data
        assertEquals(2, spans.size)
        assertEquals(0, spans[0].columnStart.value)
        assertEquals(1, spans[0].span.value)
        assertEquals(2, spans[1].columnStart.value)
        assertEquals(3, spans[1].span.value)
    }

    /**
     * Пустой список операций — не ошибка.
     */
    @Test
    fun getOperationSpansForRow_empty() = runTest {
        coEvery { patternDao.getOperationSpansForRow(10L) } returns emptyList()

        val result = repository.getOperationSpansForRow(PatternRowId.create(10L))

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    /**
     * Сбой БД при getOperationSpansForRow — DatabaseError.
     */
    @Test
    fun getOperationSpansForRow_sqlException() = runTest {
        coEvery { patternDao.getOperationSpansForRow(10L) } throws SQLException("boom")

        val result = repository.getOperationSpansForRow(PatternRowId.create(10L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }

    /**
     * addOperation делегирует в DAO и возвращает Success.
     */
    @Test
    fun addOperation_success() = runTest {
        coEvery { patternDao.insertOperations(any()) } returns Unit

        val op = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0),
            symbol = Symbol(
                code = "K",
                displayName = "Лицевая",
                textGlyph = "V",
                category = SymbolCategory.STANDARD,
                defaultColumnSpan = Span.ONE
            )
        )
        val result = repository.addOperation(op)

        assertTrue(result is Result.Success)
    }

    /**
     * Сбой БД при addOperation — DatabaseError.
     */
    @Test
    fun addOperation_sqlException() = runTest {
        coEvery { patternDao.insertOperations(any()) } throws SQLException("boom")

        val op = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0),
            symbol = Symbol(
                code = "K",
                displayName = "Лицевая",
                textGlyph = "V",
                category = SymbolCategory.STANDARD
            )
        )
        val result = repository.addOperation(op)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}
