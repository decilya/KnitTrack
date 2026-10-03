package com.knittrac.app.platform.timer

import com.knittrac.app.domain.service.TimerState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TimerManagerImplTest {

    private lateinit var timerManager: TimerManagerImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        timerManager = TimerManagerImpl(testDispatcher)
    }

    @Test
    fun `start should set sessionStartTime only once and not change on resume`() = runTest {
        timerManager.start()
        val firstStartTime = timerManager.sessionStartTime

        timerManager.pause()
        advanceTimeBy(2000L) // Имитируем паузу
        timerManager.start()
        val secondStartTime = timerManager.sessionStartTime

        assertEquals(firstStartTime, secondStartTime)
    }

    @Test
    fun `pause should cancel job but not reset elapsedTime`() = runTest {
        timerManager.start()
        advanceTimeBy(2000L)
        val elapsedTimeBeforePause = timerManager.elapsedTime.value

        timerManager.pause()

        assertEquals(elapsedTimeBeforePause, timerManager.elapsedTime.value)
    }

    @Test
    fun `reset should clear sessionStartTime and elapsedTime`() = runTest {
        timerManager.start()
        timerManager.pause()

        timerManager.reset()

        assertEquals(0L, timerManager.sessionStartTime)
        assertEquals(0L, timerManager.elapsedTime.value)
        assertEquals(TimerState.IDLE, timerManager.state.value)
    }
}