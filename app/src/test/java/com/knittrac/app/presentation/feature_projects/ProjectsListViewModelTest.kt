package com.knittrac.app.presentation.feature_projects

import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.R
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.CategoryLocalizer
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
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
 * с локализованным названием категории. Domain-модель использует VO
 * [ProjectName] и [Duration], UI-модель — примитивы.
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

    /**
     * Проверяет, что init загружает проекты и маппит их в UI-модели
     * с локализованным названием категории.
     */
    @Test
    fun init_loadsProjectsAndMapsToUiModels() = runTest {
        // Arrange: один доменный проект и его ожидаемое отображение в UI
        val mockProject = Project(
            id = 1L,
            name = ProjectName.create("Тестовый проект"),
            category = Category.KNITTING,
            createdAt = 0L,
            totalTimeSeconds = Duration.ZERO,
            totalRows = RowCount.create(50),
            targetRowCount = RowCount.create(120),
            updatedAt = 0L,
            syncStatus = SyncStatus.SYNCED
        )
        val expectedUiModel = ProjectsContract.ProjectUiModel(
            id = 1L,
            name = "Тестовый проект",
            isUntitled = false,
            localizedCategoryName = "Вязание спицами",
            totalRows = 50,
            targetRowCount = 120
        )

        coEvery { getAllProjectsService(Unit) } returns flowOf(Result.Success(listOf(mockProject)))
        every { categoryLocalizer.localize(Category.KNITTING) } returns "Вязание спицами"

        // Act
        viewModel = ProjectsListViewModel(getAllProjectsService, categoryLocalizer)

        // Assert: State.projects — список UI-моделей, а не доменных сущностей
        val state = viewModel.state.value
        assertEquals(listOf(expectedUiModel), state.projects)
    }

    /**
     * Проверяет, что при ошибке сервиса State получает errorRes
     * и isLoading снимается.
     */
    @Test
    fun loadProjects_setsErrorResWhenServiceReturnsError() = runTest {
        val cause = RuntimeException("test db error")
        coEvery { getAllProjectsService(Unit) } returns
            flowOf(Result.Error(com.knittrac.app.core.common.AppError.UnknownError(cause)))

        viewModel = ProjectsListViewModel(getAllProjectsService, categoryLocalizer)

        val state = viewModel.state.value
        assertEquals(R.string.error_load_projects, state.errorRes)
        assertEquals(false, state.isLoading)
        assertEquals(emptyList<ProjectsContract.ProjectUiModel>(), state.projects)
    }

    /**
     * Проверяет, что retry сбрасывает errorRes и перезагружает проекты.
     */
    @Test
    fun retry_clearsErrorResAndReloadsProjects() = runTest {
        val cause = RuntimeException("test error")
        coEvery { getAllProjectsService(Unit) } returns
            flowOf(Result.Error(com.knittrac.app.core.common.AppError.UnknownError(cause)))

        viewModel = ProjectsListViewModel(getAllProjectsService, categoryLocalizer)
        assertEquals(R.string.error_load_projects, viewModel.state.value.errorRes)

        // Следующий вызов вернёт успех
        val mockProject = Project(
            id = 2L,
            name = ProjectName.create("После retry"),
            category = Category.KNITTING,
            createdAt = 0L,
            totalTimeSeconds = Duration.ZERO,
            updatedAt = 0L,
            syncStatus = SyncStatus.SYNCED
        )
        coEvery { getAllProjectsService(Unit) } returns
            flowOf(Result.Success(listOf(mockProject)))
        every { categoryLocalizer.localize(Category.KNITTING) } returns "Вязание спицами"

        viewModel.onAction(ProjectsContract.Action.Retry)

        val state = viewModel.state.value
        assertNull(state.errorRes)
        assertEquals(1, state.projects.size)
        coVerify(atLeast = 2) { getAllProjectsService(Unit) }
    }

    @Test
    fun init_mapsNullTargetRowCountForProjectsWithoutTarget() = runTest {
        val mockProject = Project(
            id = 3L,
            name = ProjectName.create("Без цели"),
            category = Category.KNITTING,
            createdAt = 0L,
            totalTimeSeconds = Duration.ZERO,
            totalRows = RowCount.create(30),
            targetRowCount = null,
            updatedAt = 0L,
            syncStatus = SyncStatus.SYNCED
        )
        coEvery { getAllProjectsService(Unit) } returns flowOf(Result.Success(listOf(mockProject)))
        every { categoryLocalizer.localize(Category.KNITTING) } returns "Вязание спицами"

        viewModel = ProjectsListViewModel(getAllProjectsService, categoryLocalizer)

        val uiModel = viewModel.state.value.projects.single()
        assertEquals(30, uiModel.totalRows)
        assertNull(uiModel.targetRowCount)
    }
}
