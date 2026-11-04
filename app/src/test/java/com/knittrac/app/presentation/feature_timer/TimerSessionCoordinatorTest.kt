package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.service.SaveSessionParams
import com.knittrac.app.domain.service.SaveSessionService
import com.knittrac.app.domain.service.TimerService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты [TimerSessionCoordinator] (P1-C, 02.11.2026).
 *
 * Проверяют, что координатор — тонкий прокси к [TimerService] и
 * [SaveSessionService] (без собственной логики).
 */
class TimerSessionCoordinatorTest {

    private val timerService: TimerService = mockk(relaxed = true)
    private val saveSessionService: SaveSessionService = mockk()
    private val coordinator = TimerSessionCoordinator(timerService, saveSessionService)

    private fun params(): SaveSessionParams = SaveSessionParams(
        projectId = 1L,
        startTimestamp = 0L,
        endTimestamp = 60_000L,
        rowCount = 10
    )

    @Test
    fun start_delegatesToTimerService() {
        coordinator.start()

        verify(exactly = 1) { timerService.start() }
    }

    @Test
    fun pause_delegatesToTimerService() {
        coordinator.pause()

        verify(exactly = 1) { timerService.pause() }
    }

    @Test
    fun reset_delegatesToTimerService() {
        coordinator.reset()

        verify(exactly = 1) { timerService.reset() }
    }

    @Test
    fun saveSession_success_delegatesToService() = runTest {
        coEvery { saveSessionService(any()) } returns Result.Success(42L)

        val result = coordinator.saveSession(params())

        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data)
        coVerify(exactly = 1) { saveSessionService(any()) }
    }

    @Test
    fun saveSession_error_propagates() = runTest {
        coEvery { saveSessionService(any()) } returns
            Result.Error(AppError.ValidationError("пустая сессия"))

        val result = coordinator.saveSession(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }
}
