package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.service.SaveSessionParams
import com.knittrac.app.domain.service.SaveSessionService
import com.knittrac.app.domain.service.TimerService
import com.knittrac.app.domain.service.TimerState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Координатор жизненного цикла таймера (P1-C, 02.11.2026).
 *
 * SRP: одна причина изменения — таймер сессии (start/pause/reset/save).
 * Выделен из [TimerViewModel] (5 причин изменения → 2 координатора + VM-оркестратор).
 *
 * Ответственность:
 * - Прокси к [TimerService] (start/pause/reset, elapsedTime, state, sessionStartTime).
 * - Сохранение сессии через [SaveSessionService].
 *
 * НЕ содержит State — его собирает [TimerViewModel] через combine.
 * Координатор отдаёт только Flows и Result'ы.
 *
 * @property timerService Движок таймера (Domain interface).
 * @property saveSessionService Сохранение сессии (Domain service).
 */
class TimerSessionCoordinator @Inject constructor(
    private val timerService: TimerService,
    private val saveSessionService: SaveSessionService
) {

    /** Реактивный поток прошедшего времени (мс). */
    val elapsedTime: Flow<Long> = timerService.elapsedTime

    /** Реактивный поток состояния таймера (IDLE / RUNNING / PAUSED). */
    val state: Flow<TimerState> = timerService.state

    /** Метка старта сессии (wall clock, мс). */
    val sessionStartTime: Long
        get() = timerService.sessionStartTime

    /** Запускает таймер. */
    fun start() = timerService.start()

    /** Ставит таймер на паузу. */
    fun pause() = timerService.pause()

    /** Сбрасывает таймер в IDLE (elapsedTime = 0). */
    fun reset() = timerService.reset()

    /**
     * Сохраняет сессию.
     *
     * @param params Параметры (projectId, startTimestamp, endTimestamp, rowCount).
     * @return [Result.Success] с ID сессии; [Result.Error] с ValidationError (пустая)
     *   или DatabaseError (сбой БД).
     */
    suspend fun saveSession(params: SaveSessionParams): Result<Long> =
        saveSessionService(params)
}
