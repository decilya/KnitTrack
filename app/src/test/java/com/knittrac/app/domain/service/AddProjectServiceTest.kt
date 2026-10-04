package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddProjectServiceTest {

    private lateinit var projectRepository: ProjectRepository
    private lateinit var addProjectService: AddProjectService

    @Before
    fun setUp() {
        projectRepository = mockk()
        addProjectService = AddProjectService(
            projectRepository = projectRepository,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `execute should save project with PENDING sync status`() = runTest {
        // Arrange
        val params = AddProjectParams(
            name = "Тестовый проект",
            category = Category.KNITTING
        )
        
        coEvery { projectRepository.addProject(any()) } returns Result.Success(1L)

        // Act
        val result = addProjectService(params)

        // Assert
        assertTrue(result is Result.Success)
        coVerify { 
            projectRepository.addProject(match { project ->
                project.name == "Тестовый проект" &&
                project.category == Category.KNITTING &&
                project.syncStatus == SyncStatus.PENDING.name
            }) 
        }
    }
}
