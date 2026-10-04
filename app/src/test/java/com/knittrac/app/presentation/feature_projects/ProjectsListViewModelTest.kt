package com.knittrac.app.presentation.feature_projects

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.service.GetAllProjectsService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getAllProjectsService: GetAllProjectsService
    private lateinit var viewModel: ProjectsListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAllProjectsService = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load projects and update state`() = runTest {
        // Arrange
        val mockProjects = listOf(
            Project(1L, "Тестовый проект", Category.KNITTING, 0L, 0L, 0L, "SYNCED")
        )
        coEvery { getAllProjectsService(Unit) } returns flowOf(Result.Success(mockProjects))

        // Act
        viewModel = ProjectsListViewModel(getAllProjectsService)

        // Assert
        val state = viewModel.state.value
        // Проверяем только то, что точно есть в State: список проектов
        assertEquals(mockProjects, state.projects)
    }
}