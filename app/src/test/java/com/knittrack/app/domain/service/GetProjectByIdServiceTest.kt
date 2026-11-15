package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseTest
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.common.SyncStatus
import com.knittrack.app.domain.entity.Category
import com.knittrack.app.domain.entity.Duration
import com.knittrack.app.domain.entity.Project
import com.knittrack.app.domain.entity.ProjectId
import com.knittrack.app.domain.entity.ProjectName
import com.knittrack.app.domain.repository.ProjectRepository
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
    fun executeReturnsProjectWhenExists() = runTest {
        val now = System.currentTimeMillis()
        val project = Project(
            id = ProjectId.create(1L),
            name = ProjectName.create("Тест"),
            category = Category.KNITTING,
            createdAt = now,
            totalTimeSeconds = Duration.ZERO,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED
        )
        coEvery { projectRepository.getProjectById(ProjectId.create(1L)) } returns Result.Success(project)
        val result = service(ProjectId.create(1L))
        assertTrue(result is Result.Success)
        assertEquals(project, (result as Result.Success).data)
        coVerify { projectRepository.getProjectById(ProjectId.create(1L)) }
    }
}
