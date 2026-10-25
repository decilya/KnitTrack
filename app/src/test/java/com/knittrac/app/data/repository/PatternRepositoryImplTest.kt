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
import com.knittrac.app.domain.entity.PatternId
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
}
