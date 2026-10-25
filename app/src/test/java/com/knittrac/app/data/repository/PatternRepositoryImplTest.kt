package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.domain.entity.PatternId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.SQLException

class PatternRepositoryImplTest {

    private val patternDao: PatternDao = mockk()
    private val repository = PatternRepositoryImpl(patternDao)

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

    @Test
    fun getPatternById_found_returnsPatternWithRows() = runTest {
        coEvery { patternDao.getPatternById(1L) } returns entity
        coEvery { patternDao.getRowsForPattern(1L) } returns rows

        val result = repository.getPatternById(PatternId.create(1L))

        assertTrue(result is Result.Success)
        val pattern = (result as Result.Success).data
        assertEquals(2, pattern.rows.size)
        assertTrue(pattern.legend.isEmpty())
        assertTrue(pattern.repeats.isEmpty())
    }

    @Test
    fun getPatternById_notFound_returnsNotFoundError() = runTest {
        coEvery { patternDao.getPatternById(99L) } returns null

        val result = repository.getPatternById(PatternId.create(99L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    @Test
    fun getPatternById_sqlException_returnsDatabaseError() = runTest {
        coEvery { patternDao.getPatternById(1L) } throws SQLException("boom")

        val result = repository.getPatternById(PatternId.create(1L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}
