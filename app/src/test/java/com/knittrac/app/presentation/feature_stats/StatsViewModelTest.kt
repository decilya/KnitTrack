package com.knittrac.app.presentation.feature_stats

import com.knittrac.app.domain.entity.ProjectId
import androidx.lifecycle.SavedStateHandle
import com.knittrac.app.R
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.service.GetProjectStatisticsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    private lateinit var getProjectStatisticsService: GetProjectStatisticsService
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getProjectStatisticsService = mockk()

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
    fun loadstatsShouldUpdateStateWithDailyStatsOnSuccess() = runTest {
        // --- Arrange ---
        val mockStats = listOf(
            DailyStat(dayTimestamp = 1000L, totalSeconds = 3600L)
        )
        coEvery { getProjectStatisticsService(ProjectId.create(1L)) } returns Result.Success(mockStats)

        // --- Act ---
        viewModel = StatsViewModel(getProjectStatisticsService, savedStateHandle)

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
    fun loadstatsSetsErrorresWhenRepositoryReturnsError() = runTest {
        // --- Arrange ---
        val cause = RuntimeException("test db error")
        coEvery { getProjectStatisticsService(ProjectId.create(1L)) } returns
            Result.Error(AppError.UnknownError(cause))

        // --- Act ---
        viewModel = StatsViewModel(getProjectStatisticsService, savedStateHandle)

        // --- Assert ---
        val state = viewModel.state.value

        assertEquals(R.string.error_load_stats, state.errorRes)
        assertEquals(false, state.isLoading)
        assertEquals(emptyList<DailyStat>(), state.dailyStats)
    }

    /**
     * Без nav-аргумента projectId VM не делает запрос к репозиторию
     * и сразу выставляет errorRes — UI покажет ошибку вместо пустой
     * статистики, которая вводила бы в заблуждение.
     */
    @Test
    fun missingProjectidSetsErrorresWithoutRepositoryCall() = runTest {
        viewModel = StatsViewModel(getProjectStatisticsService, SavedStateHandle())

        val state = viewModel.state.value
        assertEquals(R.string.error_load_stats, state.errorRes)
        assertEquals(false, state.isLoading)
        assertEquals(emptyList<DailyStat>(), state.dailyStats)
        coVerify(exactly = 0) { getProjectStatisticsService(any()) }
    }

    /**
     * Второй retry отменяет ещё не завершившийся первый запрос.
     * В state остаётся ответ последнего вызова.
     */
    @Test
    fun retry_twice_cancelsPrevious() = runTest {
        val first = listOf(DailyStat(dayTimestamp = 1L, totalSeconds = 1L))
        val second = listOf(DailyStat(dayTimestamp = 2L, totalSeconds = 2L))
        var calls = 0
        coEvery { getProjectStatisticsService(ProjectId.create(1L)) } coAnswers {
            calls += 1
            if (calls == 1) {
                delay(60_000)
                Result.Success(first)
            } else {
                Result.Success(second)
            }
        }

        viewModel = StatsViewModel(getProjectStatisticsService, savedStateHandle)
        viewModel.onAction(StatsContract.Action.Retry)

        assertEquals(second, viewModel.state.value.dailyStats)
        assertEquals(false, viewModel.state.value.isLoading)
        coVerify(exactly = 2) { getProjectStatisticsService(ProjectId.create(1L)) }
    }
}
