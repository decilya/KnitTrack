package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [GetProjectByIdService].
 * Проверяют, что сервис корректно возвращает проект по id или ошибку,
 * если проект не найден.
 */
class GetProjectByIdServiceTest : BaseTest() {

    private val projectRepository: ProjectRepository = mockk()
    private val service = GetProjectByIdService(projectRepository, coroutineRule.testDispatcher)

    /**
     * Проверяет, что при наличии проекта в репозитории сервис возвращает
     * [Result.Success] с той же Domain-моделью.
     */
    @Test
    fun `execute returns project when exists`() = runTest {
        val now = System.currentTimeMillis()
        val project = Project(
            id = 1L,
            name = ProjectName.create("Тест"),
            category = Category.KNITTING,
            createdAt = now,
            totalTimeSeconds = Duration.ZERO,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED.name
        )
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project)
        val result = service(1L)
        assertTrue(result is Result.Success)
        assertEquals(project, (result as Result.Success).data)
        coVerify { projectRepository.getProjectById(1L) }
    }
}
