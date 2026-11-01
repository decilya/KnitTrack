package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletePatternRowServiceTest {
    private val patternRepository: PatternRepository = mockk()
    private val service = DeletePatternRowService(patternRepository)

    private fun pattern(): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        rows = listOf(PatternRow(PatternRowId.create(10L), RowNumber.ONE))
    )

    @Test
    fun `delete success returns Unit`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.deleteRow(any()) } returns Result.Success(Unit)

        val result = service.delete(DeletePatternRowParams(PatternId.create(1L), PatternRowId.create(10L)))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `delete row not in pattern returns NotFound`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())

        val result = service.delete(DeletePatternRowParams(PatternId.create(1L), PatternRowId.create(99L)))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    @Test
    fun `delete database error propagates`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { patternRepository.deleteRow(any()) } returns Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = service.delete(DeletePatternRowParams(PatternId.create(1L), PatternRowId.create(10L)))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}