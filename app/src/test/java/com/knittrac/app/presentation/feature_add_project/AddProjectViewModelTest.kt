package com.knittrac.app.presentation.feature_add_project

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.service.AddProjectService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Guard `isSaving` в [AddProjectViewModel]: повторный SaveProject не создаёт второй проект.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddProjectViewModelTest : BaseTest() {

    private val addProjectService: AddProjectService = mockk()
    private lateinit var viewModel: AddProjectViewModel

    @Before
    fun createViewModel() {
        viewModel = AddProjectViewModel(addProjectService)
    }

    private fun fillValidForm() {
        viewModel.onIntent(AddProjectContract.Intent.UpdateName("Шарф"))
        viewModel.onIntent(AddProjectContract.Intent.SelectCategory(Category.KNITTING))
    }

    /**
     * Один SaveProject вызывает сервис ровно один раз и снимает isSaving.
     */
    @Test
    fun saveProject_singleTap_callsService() = runTest {
        coEvery { addProjectService(any()) } returns Result.Success(1L)
        fillValidForm()

        viewModel.onIntent(AddProjectContract.Intent.SaveProject)
        advanceUntilIdle()

        coVerify(exactly = 1) { addProjectService(any()) }
        assertEquals(false, viewModel.state.value.isSaving)
    }

    /**
     * Второй SaveProject, пока первый ещё не закончился, сервис не вызывает.
     */
    @Test
    fun saveProject_doubleTap_secondIgnored() = runTest {
        coEvery { addProjectService(any()) } coAnswers {
            delay(100)
            Result.Success(1L)
        }
        fillValidForm()

        viewModel.onIntent(AddProjectContract.Intent.SaveProject)
        viewModel.onIntent(AddProjectContract.Intent.SaveProject)
        advanceUntilIdle()

        coVerify(exactly = 1) { addProjectService(any()) }
        assertEquals(false, viewModel.state.value.isSaving)
    }
}
