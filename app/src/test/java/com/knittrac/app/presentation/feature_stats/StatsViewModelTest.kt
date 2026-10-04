package com.knittrac.app.presentation.feature_stats

import androidx.lifecycle.SavedStateHandle
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
 * Цель: Убедиться, что ViewModel корректно извлекает аргумент `projectId` из
 * навигации (через SavedStateHandle) и запрашивает дневную статистику из репозитория.
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
        
        // Имитируем переход на экран статистики с аргументом навигации "projectId" = 1L
        savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Тест: Успешная загрузка статистики обновляет состояние без ошибок.
     *
     * Логика (Arrange-Act-Assert):
     * 1. Arrange: Мок репозитория возвращает успешный результат с тестовыми данными (1 час = 3600 сек).
     * 2. Act: Создаем ViewModel, которая считывает projectId=1L и делает запрос.
     * 3. Assert: Проверяем все поля итогового State.
     */
    @Test
    fun `loadStats should update state with daily stats on success`() = runTest {
        // --- Arrange ---
        val mockStats = listOf(
            DailyStat(dayTimestamp = 1000L, totalSeconds = 3600L)
        )
        // Ожидаем вызов метода getDailyStats именно с projectId = 1L
        coEvery { sessionRepository.getDailyStats(1L) } returns Result.Success(mockStats)

        // --- Act ---
        viewModel = StatsViewModel(sessionRepository, savedStateHandle)

        // --- Assert ---
        val state = viewModel.state.value
        
        assertEquals(mockStats, state.dailyStats)
        assertEquals(false, state.isLoading)
        
        // Убеждаемся, что поле ошибки осталось пустым (null) при успешном сценарии
        assertNull(state.error)
    }
}
