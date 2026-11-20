package com.knittrack.app.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для управления состоянием и логикой таймера.
 */
interface TimerService {
    val elapsedTime: StateFlow<Long>
    val state: StateFlow<TimerState>
    val sessionStartTime: Long

    /**
     * Становится `true` после завершения восстановления из snapshot
     * (включая случаи null/reboot). Foreground-сервис ждёт этого сигнала
     * перед принятием решения о состоянии таймера.
     */
    val isRestored: StateFlow<Boolean>

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
    PAUSED;   // На паузе

    /** Фабрики для восстановления [TimerState] из строкового представления. */
    companion object {
        /**
         * Восстанавливает [TimerState] из строкового представления.
         *
         * Возвращает null при неизвестном значении — fallback на [IDLE]
         * опасен: он маскирует повреждённые данные как «таймер остановлен»,
         * хотя пользователь мог вязать. Восстановление из DataStore при
         * null обязано очистить snapshot.
         *
         * @param value Строковое значение (обычно [name]).
         * @return Соответствующий [TimerState] или null.
         */
        fun fromStringOrNull(value: String): TimerState? =
            entries.firstOrNull { it.name == value }
    }
}