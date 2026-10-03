package com.knittrac.app.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для управления состоянием и логикой таймера.
 */
interface TimerManager {
    val elapsedTime: StateFlow<Long>
    val state: StateFlow<TimerState>
    val sessionStartTime: Long

    fun start()
    fun pause()
    fun reset()
}

/**
 * Состояния таймера
 */
enum class TimerState {
    IDLE,     // Остановлен
    RUNNING,  // Работает
    PAUSED    // На паузе
}