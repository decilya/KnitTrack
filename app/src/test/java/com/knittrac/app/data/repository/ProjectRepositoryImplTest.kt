package com.knittrac.app.data.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectRepositoryImplTest {

    private lateinit var projectDao: ProjectDao
    private lateinit var repository: ProjectRepositoryImpl

    @Before
    fun setUp() {
        projectDao = mockk()
        repository = ProjectRepositoryImpl(projectDao)
    }

    @Test
    fun `addProject should set PENDING sync status and current timestamp`() = runTest {
        // Arrange
        val project = Project(
            id = 0,
            name = "Тест",
            category = Category.KNITTING,
            createdAt = 1000L,
            updatedAt = 1000L,
            totalTimeSeconds = 0,
            syncStatus = SyncStatus.SYNCED.name
        )
        
        coEvery { projectDao.insertProject(any()) } returns 1L

        // Act
        val result = repository.addProject(project)

        // Assert
        assertTrue(result is Result.Success)
        coVerify {
            projectDao.insertProject(match { p ->
                p.syncStatus == SyncStatus.PENDING.name &&
                p.updatedAt > 1000L
            })
        }
    }

    @Test
    fun `getAllProjectsFlow should return projects from DAO`() = runTest {
        // Arrange
        val projects = listOf(
            Project(1L, "P1", Category.KNITTING, 0L, 0L, 0L, SyncStatus.SYNCED.name)
        )
        coEvery { projectDao.getAllProjectsFlow() } returns flowOf(projects)

        // Act: используем .first() для получения первого значения из холодного Flow
        val result = repository.getAllProjectsFlow().first()

        // Assert
        assertEquals(projects, result)
    }
}
