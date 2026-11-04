package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.service.GetAllProjectsService
import com.knittrac.app.domain.service.MarkNextRowUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты [ProjectProgressCoordinator] (P1-C, 02.11.2026).
 *
 * Проверяют, что координатор — тонкий прокси к [GetAllProjectsService]
 * и [MarkNextRowUseCase].
 */
class ProjectProgressCoordinatorTest {

    private val getAllProjectsService: GetAllProjectsService = mockk()
    private val markNextRowUseCase: MarkNextRowUseCase = mockk()
    private val coordinator = ProjectProgressCoordinator(getAllProjectsService, markNextRowUseCase)

    private fun project(id: Long = 1L): Project = Project(
        id = ProjectId.create(id),
        name = ProjectName.create("P$id"),
        category = Category.KNITTING,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING
    )

    @Test
    fun projects_delegatesToGetAllProjectsService() = runTest {
        val projects = listOf(project(1L), project(2L))
        every { getAllProjectsService(Unit) } returns flowOf(Result.Success(projects))

        val result = coordinator.projects().first()

        assertTrue(result is Result.Success)
        assertEquals(projects, (result as Result.Success).data)
    }

    @Test
    fun markNext_success_returnsRowNumber() = runTest {
        coEvery { markNextRowUseCase(any()) } returns Result.Success(RowNumber.create(5))

        val result = coordinator.markNext(ProjectId.create(1L))

        assertTrue(result is Result.Success)
        assertEquals(5, (result as Result.Success).data.value)
        coVerify(exactly = 1) { markNextRowUseCase(any()) }
    }

    @Test
    fun markNext_error_propagates() = runTest {
        coEvery { markNextRowUseCase(any()) } returns
            Result.Error(AppError.ValidationError("нет схемы"))

        val result = coordinator.markNext(ProjectId.create(1L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }
}
