package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePatternUseCaseTest : BaseTest() {

    private val repository: PatternRepository = mockk()
    private val useCase = CreatePatternUseCase(repository, coroutineRule.testDispatcher)

    private fun params(rows: Int): CreatePatternParams = CreatePatternParams(
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        rowCount = RowCount.create(rows)
    )

    @Test
    fun execute_callsRepositoryWithEmptyRows() = runTest {
        val captured = slot<Pattern>()
        coEvery { repository.addPattern(capture(captured), RowCount.create(2)) } returns
            Result.Success(PatternId.create(4L))

        val result = useCase(params(2))

        assertTrue(result is Result.Success)
        assertTrue(captured.captured.rows.isEmpty())
        assertEquals("Схема", captured.captured.name.value)
    }

    @Test
    fun execute_rowCountZero_returnsValidationError() = runTest {
        val result = useCase(params(0))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
        coVerify(exactly = 0) { repository.addPattern(any(), any()) }
    }

    @Test
    fun execute_repositoryError_propagates() = runTest {
        val error = AppError.DatabaseError(IllegalStateException("db"))
        coEvery { repository.addPattern(any(), any()) } returns Result.Error(error)

        val result = useCase(params(2))

        assertEquals(error, (result as Result.Error).error)
    }
}
