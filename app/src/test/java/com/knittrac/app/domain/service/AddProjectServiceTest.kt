package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты для [AddProjectService].
 *
 * Покрывают: валидацию name через Value Object, валидацию targetRowCount
 * через Project.init, трансляцию IllegalArgumentException в ValidationError,
 * установку PENDING-статуса синхронизации.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddProjectServiceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var projectRepository: ProjectRepository
    private lateinit var addProjectService: AddProjectService
    private lateinit var timeProvider: FakeTimeProvider

    @Before
    fun setUp() {
        projectRepository = mockk()
        timeProvider = FakeTimeProvider()
        addProjectService = AddProjectService(
            projectRepository = projectRepository,
            dispatcher = testDispatcher,
            timeProvider = timeProvider
        )
    }

    @Test
    fun execute_withPendingSyncStatus_savesProject() = runTest {
        val params = AddProjectParams(
            name = "Test",
            category = Category.KNITTING
        )

        coEvery { projectRepository.addProject(any()) } returns Result.Success(1L)

        val result = addProjectService(params)

        assertTrue(result is Result.Success)
        coVerify {
            projectRepository.addProject(match { project ->
                project.name.value == "Test" &&
                project.category == Category.KNITTING &&
                project.syncStatus == SyncStatus.PENDING
            })
        }
    }

    @Test
    fun execute_withValidTargetRowCount_passesToProject() = runTest {
        coEvery { projectRepository.addProject(any()) } returns Result.Success(1L)

        val params = AddProjectParams(
            name = "Test",
            category = Category.KNITTING,
            targetRowCount = 120
        )

        val result = addProjectService(params)

        assertTrue(result is Result.Success)
        coVerify {
            projectRepository.addProject(match { it.targetRowCount == RowCount.create(120) })
        }
    }

    @Test
    fun execute_withNegativeTargetRowCount_returnsValidationError() = runTest {
        val params = AddProjectParams(
            name = "Test",
            category = Category.KNITTING,
            targetRowCount = -5
        )

        val result = addProjectService(params)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun execute_withNullTargetRowCount_passesNullToProject() = runTest {
        coEvery { projectRepository.addProject(any()) } returns Result.Success(1L)

        val params = AddProjectParams(
            name = "Test",
            category = Category.KNITTING,
            targetRowCount = null
        )

        val result = addProjectService(params)

        assertTrue(result is Result.Success)
        coVerify {
            projectRepository.addProject(match { it.targetRowCount == null })
        }
    }
}
