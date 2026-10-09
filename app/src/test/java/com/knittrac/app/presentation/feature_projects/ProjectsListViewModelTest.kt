package com.knittrac.app.presentation.feature_projects

import com.knittrac.app.R
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.CategoryLocalizer
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.service.GetAllProjectsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты для [ProjectsListViewModel].
 *
 * Проверяют загрузку проектов из [GetAllProjectsService] и корректный маппинг
 * Domain-сущностей [Project] в UI-модели [ProjectsContract.ProjectUiModel]
 * с локализованным названием категории.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getAllProjectsService: GetAllProjectsService
    private lateinit var categoryLocalizer: CategoryLocalizer
    private lateinit var viewModel: ProjectsListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAllProjectsService = mockk()
        categoryLocalizer = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load projects and map them to UI models`() = runTest {
        // Arrange: один доменный проект и его ожидаемое отображение в UI
        val mockProject = Project(
            id = 1L,
            name = "Тестовый проект",
            category = Category.KNITTING,
            createdAt = 0L,
            totalTimeSeconds = 0L,
            updatedAt = 0L,
            syncStatus = "SYNCED"
        )
        val expectedUiModel = ProjectsContract.ProjectUiModel(
            id = 1L,
            name = "Тестовый проект",
            localizedCategoryName = "Вязание спицами"
        )

        coEvery { getAllProjectsService(Unit) } returns flowOf(Result.Success(listOf(mockProject)))
        every { categoryLocalizer.localize(Category.KNITTING) } returns "Вязание спицами"

        // Act
        viewModel = ProjectsListViewModel(getAllProjectsService, categoryLocalizer)

        // Assert: State.projects — список UI-моделей, а не доменных сущностей
        val state = viewModel.state.value
        assertEquals(listOf(expectedUiModel), state.projects)
    }

    @Test
    fun `loadProjects sets errorRes when service returns Error`() = runTest {
        val cause = RuntimeException("test db error")
        coEvery { getAllProjectsService(Unit) } returns
            flowOf(Result.Error(com.knittrac.app.core.common.AppError.UnknownError(cause)))

        viewModel = ProjectsListViewModel(getAllProjectsService, categoryLocalizer)

        val state = viewModel.state.value
        assertEquals(R.string.error_load_projects, state.errorRes)
        assertEquals(false, state.isLoading)
        assertEquals(emptyList<ProjectsContract.ProjectUiModel>(), state.projects)
    }

    @Test
    fun `retry clears errorRes and reloads projects`() = runTest {
        val cause = RuntimeException("test error")
        coEvery { getAllProjectsService(Unit) } returns
            flowOf(Result.Error(com.knittrac.app.core.common.AppError.UnknownError(cause)))

        viewModel = ProjectsListViewModel(getAllProjectsService, categoryLocalizer)
        assertEquals(R.string.error_load_projects, viewModel.state.value.errorRes)

        // Следующий вызов вернёт успех
        val mockProject = Project(
            id = 2L, name = "После retry", category = Category.KNITTING,
            createdAt = 0L, totalTimeSeconds = 0L, updatedAt = 0L, syncStatus = "SYNCED"
        )
        coEvery { getAllProjectsService(Unit) } returns
            flowOf(Result.Success(listOf(mockProject)))
        every { categoryLocalizer.localize(Category.KNITTING) } returns "Вязание спицами"

        viewModel.retry()

        val state = viewModel.state.value
        assertNull(state.errorRes)
        assertEquals(1, state.projects.size)
        coVerify(atLeast = 2) { getAllProjectsService(Unit) }
    }
}
