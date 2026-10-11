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
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [GetAllProjectsService].
 * Проверяют, что сервис возвращает поток [Result.Success] с маппингом
 * Domain-проектов без изменений.
 */
class GetAllProjectsServiceTest : BaseTest() {

    private val projectRepository: ProjectRepository = mockk()
    private val service = GetAllProjectsService(projectRepository, coroutineRule.testDispatcher)

    /**
     * Проверяет, что сервис эмитит [Result.Success] со списком проектов,
     * полученных из репозитория.
     */
    @Test
    fun `execute returns flow of projects`() = runTest {
        val projects = listOf(
            Project(
                id = 1L,
                name = ProjectName.create("P1"),
                category = Category.KNITTING,
                createdAt = 0L,
                totalTimeSeconds = Duration.ZERO,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED.name
            )
        )
        coEvery { projectRepository.getAllProjectsFlow() } returns flowOf(projects)
        val resultList = service(Unit).toList()
        assertEquals(1, resultList.size)
        assertTrue(resultList[0] is Result.Success<*>)
        assertEquals(projects, (resultList[0] as Result.Success<*>).data)
    }
}
