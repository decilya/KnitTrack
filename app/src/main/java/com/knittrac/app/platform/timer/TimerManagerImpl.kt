package com.knittrac.app.platform.timer

import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.service.TimerManager
import com.knittrac.app.domain.service.TimerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [TimerManager] с корректной работой при NTP-синхронизации.
 *
 * Расчёт elapsedTime базируется на [TimeProvider.elapsedRealtime] —
 * монотонном времени, которое не идёт назад при переводе часов,
 * смене часового пояса или NTP-синхронизации. Wall-clock время
 * сохраняется отдельно в [sessionStartTime] для записи в БД.
 */
@Singleton
class TimerManagerImpl @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider
) : TimerManager {

    private val _elapsedTime = MutableStateFlow(0L)
    override val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _state = MutableStateFlow(TimerState.IDLE)
    override val state: StateFlow<TimerState> = _state

    /**
     * Wall clock (Unix epoch) первой сессии. Идёт в [Session.startTimestamp].
     * Не меняется при pause/resume — это момент старта.
     */
    override var sessionStartTime: Long = 0L
        private set

    /**
     * Monotonic-время ([TimeProvider.elapsedRealtime]) первой сессии.
     * Используется только для расчёта [elapsedTime].
     */
    private var sessionStartElapsed: Long = 0L

    /** Накопленное время пауз (monotonic), миллисекунды. */
    private var totalPausedDuration: Long = 0L

    /** Monotonic-время последней паузы. */
    private var lastPauseElapsed: Long = 0L

    private var job: Job? = null

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    override fun start() {
        if (job?.isActive == true) return

        val nowWallClock = timeProvider.currentTimeMillis()
        val nowElapsed = timeProvider.elapsedRealtime()

        if (sessionStartTime == 0L) {
            // Первый старт сессии
            sessionStartTime = nowWallClock
            sessionStartElapsed = nowElapsed
        } else {
            // Resume после паузы: добавляем длительность паузы
            totalPausedDuration += (nowElapsed - lastPauseElapsed)
        }

        _state.value = TimerState.RUNNING

        job = scope.launch {
            while (isActive) {
                val current = timeProvider.elapsedRealtime()
                _elapsedTime.value =
                    (current - sessionStartElapsed - totalPausedDuration) / 1000L
                delay(1000L)
            }
        }
    }

    /**
     * Ставит таймер на паузу.
     *
     * Идемпотентен: повторный вызов в состоянии PAUSED или IDLE
     * игнорируется. Без guard повторный pause() перезаписывал
     * lastPauseElapsed, и следующий start() недоучитывал паузу —
     * elapsedTime скакал вперёд.
     */
    override fun pause() {
        if (_state.value != TimerState.RUNNING) return
        job?.cancel()
        _state.value = TimerState.PAUSED
        lastPauseElapsed = timeProvider.elapsedRealtime()
    }

    override fun reset() {
        job?.cancel()
        sessionStartTime = 0L
        sessionStartElapsed = 0L
        totalPausedDuration = 0L
        lastPauseElapsed = 0L
        _elapsedTime.value = 0L
        _state.value = TimerState.IDLE
    }
}
