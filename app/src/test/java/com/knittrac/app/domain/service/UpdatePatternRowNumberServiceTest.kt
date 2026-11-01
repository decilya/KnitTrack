package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для [UpdatePatternRowNumberService] (7f-D-3).
 *
 * Используют легковесные моки existsRow/getRowNumber/getRowCount,
 * чтобы не тянуть весь Pattern-агрегат.
 */
class UpdatePatternRowNumberServiceTest {
    private val patternRepository: PatternRepository = mockk()
    private val service = UpdatePatternRowNumberService(patternRepository)

    private fun params(newNumber: Int = 2, rowId: Long = 10L): UpdatePatternRowNumberParams =
        UpdatePatternRowNumberParams(
            patternId = PatternId.create(1L),
            rowId = PatternRowId.create(rowId),
            newRowNumber = RowNumber.create(newNumber)
        )

    @Test
    fun `update success`() = runTest {
        coEvery { patternRepository.existsRow(any(), any()) } returns Result.Success(true)
        coEvery { patternRepository.getRowNumber(any()) } returns Result.Success(1)
        coEvery { patternRepository.getRowCount(any()) } returns Result.Success(5)
        coEvery { patternRepository.updateRowNumber(any(), any(), any()) } returns Result.Success(Unit)

        val result = service.update(params(newNumber = 3))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `update row not found returns NotFound`() = runTest {
        coEvery { patternRepository.existsRow(any(), any()) } returns Result.Success(false)

        val result = service.update(params(rowId = 99L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    @Test
    fun `update out of bounds returns ValidationError`() = runTest {
        coEvery { patternRepository.existsRow(any(), any()) } returns Result.Success(true)
        coEvery { patternRepository.getRowNumber(any()) } returns Result.Success(1)
        coEvery { patternRepository.getRowCount(any()) } returns Result.Success(2)

        val result = service.update(params(newNumber = 5))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun `update same number early-returns success`() = runTest {
        coEvery { patternRepository.existsRow(any(), any()) } returns Result.Success(true)
        coEvery { patternRepository.getRowNumber(any()) } returns Result.Success(2)
        coEvery { patternRepository.getRowCount(any()) } returns Result.Success(5)

        val result = service.update(params(newNumber = 2))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `update database error on getRowNumber propagates`() = runTest {
        coEvery { patternRepository.existsRow(any(), any()) } returns Result.Success(true)
        coEvery { patternRepository.getRowNumber(any()) } returns
            Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = service.update(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}