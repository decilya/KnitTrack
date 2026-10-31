package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteOperationUseCaseTest : BaseTest() {

    private val repository: PatternRepository = mockk()
    private val useCase = DeleteOperationUseCase(repository, coroutineRule.testDispatcher)

    private fun params(): DeleteOperationParams = DeleteOperationParams(
        rowId = PatternRowId.create(10L),
        columnStart = ColumnStart.create(0)
    )

    @Test
    fun execute_success_callsRepositoryDelete() = runTest {
        coEvery { repository.deleteOperation(any(), any()) } returns Result.Success(Unit)

        val result = useCase(params())

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            repository.deleteOperation(PatternRowId.create(10L), ColumnStart.create(0))
        }
    }

    @Test
    fun execute_repositoryError_propagates() = runTest {
        val error = AppError.DatabaseError(IllegalStateException("db"))
        coEvery { repository.deleteOperation(any(), any()) } returns Result.Error(error)

        val result = useCase(params())

        assertEquals(error, (result as Result.Error).error)
    }
}
