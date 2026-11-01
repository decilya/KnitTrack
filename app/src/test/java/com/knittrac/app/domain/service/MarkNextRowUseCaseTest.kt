package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [MarkNextRowUseCase].
 *
 * Проверяют цепочку: загрузка [Project] → делегирование в
 * [NextRowService.markNextRow], проброс ошибок репозитория и сервиса.
 */
class MarkNextRowUseCaseTest : BaseTest() {

    private val projectRepository: ProjectRepository = mockk()
    private val nextRowService: NextRowService = mockk()
    private val useCase = MarkNextRowUseCase(
        projectRepository,
        nextRowService,
        coroutineRule.testDispatcher
    )

    private fun project(): Project = Project(
        id = 1L,
        name = ProjectName.create("Тест"),
        category = Category.KNITTING,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING
    )

    private fun params(): MarkNextRowParams =
        MarkNextRowParams(projectId = ProjectId.create(1L))

    /**
     * Успешный путь: проект найден → NextRowService вернул RowNumber.
     */
    @Test
    fun `execute returns RowNumber from NextRowService`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { nextRowService.markNextRow(any()) } returns Result.Success(RowNumber.create(5))

        val result = useCase(params())

        assertTrue(result is Result.Success)
        assertEquals(5, (result as Result.Success).data.value)
        coVerify { nextRowService.markNextRow(any()) }
    }

    /**
     * Проект не найден — NotFoundError без обращения к NextRowService.
     */
    @Test
    fun `execute returns NotFoundError when project missing`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns
            Result.Error(AppError.NotFoundError("нет"))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * NextRowService вернул ValidationError (нет схемы / последний ряд /
     * пустые rows) — пробрасываем.
     */
    @Test
    fun `execute returns ValidationError from NextRowService`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { nextRowService.markNextRow(any()) } returns
            Result.Error(AppError.ValidationError("нет схемы"))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * NextRowService вернул DatabaseError — пробрасываем.
     */
    @Test
    fun `execute returns DatabaseError from NextRowService`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { nextRowService.markNextRow(any()) } returns
            Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}
