package com.knittrac.app.presentation.feature_stats

import androidx.lifecycle.SavedStateHandle
import com.knittrac.app.R
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Юнит-тесты для [StatsViewModel].
 *
 * Цель: убедиться, что ViewModel корректно извлекает аргумент projectId
 * из навигации (SavedStateHandle), запрашивает дневную статистику из
 * репозитория и правильно отрабатывает Result.Error — выставляет
 * errorRes с @StringRes id и снимает isLoading.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sessionRepository: SessionRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk()

        // Имитируем переход на экран статистики с аргументом "projectId" = 1L
        savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Успешная загрузка статистики: state.dailyStats заполнен,
     * isLoading снят, errorRes отсутствует.
     *
     * Arrange: репозиторий возвращает Result.Success с одним днём (1 час).
     * Act:     создание ViewModel читает projectId=1L и делает запрос.
     * Assert:  проверяем все поля итогового State.
     */
    @Test
    fun `loadStats should update state with daily stats on success`() = runTest {
        // --- Arrange ---
        val mockStats = listOf(
            DailyStat(dayTimestamp = 1000L, totalSeconds = 3600L)
        )
        coEvery { sessionRepository.getDailyStats(1L) } returns Result.Success(mockStats)

        // --- Act ---
        viewModel = StatsViewModel(sessionRepository, savedStateHandle)

        // --- Assert ---
        val state = viewModel.state.value

        assertEquals(mockStats, state.dailyStats)
        assertEquals(false, state.isLoading)

        // При успешном сценарии поле ошибки остаётся пустым
        assertNull(state.errorRes)
    }

    /**
     * Ошибка репозитория: state.dailyStats пуст, isLoading снят,
     * errorRes содержит R.string.error_load_stats.
     *
     * Arrange: репозиторий возвращает Result.Error.
     * Act:     создание ViewModel.
     * Assert:  проверяем errorRes и что список статистики пуст.
     */
    @Test
    fun `loadStats sets errorRes when repository returns Error`() = runTest {
        // --- Arrange ---
        val cause = RuntimeException("test db error")
        coEvery { sessionRepository.getDailyStats(1L) } returns
            Result.Error(AppError.UnknownError(cause))

        // --- Act ---
        viewModel = StatsViewModel(sessionRepository, savedStateHandle)

        // --- Assert ---
        val state = viewModel.state.value

        assertEquals(R.string.error_load_stats, state.errorRes)
        assertEquals(false, state.isLoading)
        assertEquals(emptyList<DailyStat>(), state.dailyStats)
    }
}
