package com.knittrack.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты [TimerState.fromStringOrNull].
 *
 * Контракт: неизвестное значение → null (не fallback на IDLE — это
 * маскировало бы повреждённые данные DataStore как «таймер остановлен»).
 */
class TimerStateTest {

    /** Корректное значение IDLE. */
    @Test
    fun fromStringOrNull_idle_returnsIdle() {
        assertEquals(TimerState.IDLE, TimerState.fromStringOrNull("IDLE"))
    }

    /** Корректное значение RUNNING. */
    @Test
    fun fromStringOrNull_running_returnsRunning() {
        assertEquals(TimerState.RUNNING, TimerState.fromStringOrNull("RUNNING"))
    }

    /** Корректное значение PAUSED. */
    @Test
    fun fromStringOrNull_paused_returnsPaused() {
        assertEquals(TimerState.PAUSED, TimerState.fromStringOrNull("PAUSED"))
    }

    /** Неизвестное значение — null, не IDLE. */
    @Test
    fun fromStringOrNull_unknown_returnsNull() {
        assertNull(TimerState.fromStringOrNull("BROKEN"))
        assertNull(TimerState.fromStringOrNull(""))
        assertNull(TimerState.fromStringOrNull("running"))
    }
}
