package com.knittrack.app.platform.timer

import com.knittrack.app.core.common.TimeProvider
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.model.TimerSnapshot
import com.knittrack.app.domain.repository.SettingsRepository
import com.knittrack.app.domain.service.TimerService
import com.knittrack.app.domain.service.TimerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [TimerService] с корректной работой при NTP-синхронизации
 * и персистентностью через [SettingsRepository].
 *
 * Расчёт elapsedTime базируется на [TimeProvider.elapsedRealtime] —
 * монотонном времени, которое не идёт назад при переводе часов,
 * смене часового пояса или NTP-синхронизации. Wall-clock время
 * сохраняется отдельно в [sessionStartTime] для записи в БД.
 *
 * ## Персистентность (P2 таймер-персистентность)
 *
 * В `init` читает `SettingsRepository.timerSnapshotFlow` и восстанавливает
 * состояние после смерти процесса (system kill, user swipe). При reboot
 * `elapsedRealtime` сбрасывается в 0 — если `snapshot.sessionStartElapsed >
 * nowElapsed`, снимок невалиден, сбрасываем в [TimerState.IDLE] и очищаем.
 *
 * `start()` и `pause()` сохраняют снимок через `saveSnapshot()` (async,
 * в scope Singleton'а). `reset()` очищает.
 */
@Singleton
class TimerServiceImpl @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider,
    private val settingsRepository: SettingsRepository
) : TimerService {

    private val _elapsedTime = MutableStateFlow(0L)
    override val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _state = MutableStateFlow(TimerState.IDLE)
    override val state: StateFlow<TimerState> = _state

    private val _isRestored = MutableStateFlow(false)
    override val isRestored: StateFlow<Boolean> = _isRestored.asStateFlow()

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

    init {
        scope.launch { restoreFromSnapshot() }
    }

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
        saveSnapshot()
        startJob()
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
        saveSnapshot()
    }

    override fun reset() {
        job?.cancel()
        sessionStartTime = 0L
        sessionStartElapsed = 0L
        totalPausedDuration = 0L
        lastPauseElapsed = 0L
        _elapsedTime.value = 0L
        _state.value = TimerState.IDLE
        scope.launch { settingsRepository.clearTimerSnapshot() }
    }

    /**
     * Запускает бесконечный цикл обновления elapsedTime.
     *
     * Вынесен из [start], чтобы восстановление в [restoreFromSnapshot]
     * могло запустить тикер для `RUNNING` без сброса полей.
     */
    private fun startJob() {
        job?.cancel()
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
     * Проверяет, был ли reboot (monotonic-время сброшено в 0).
     *
     * Если `sessionStartElapsed > nowElapsed`, значит устройство
     * перезагрузилось и `elapsedRealtime` сбросился. Снимок невалиден.
     */
    private fun isReboot(snapshot: TimerSnapshot, nowElapsed: Long): Boolean {
        if (snapshot.sessionStartElapsed > nowElapsed) {
            Timber.w(
                "Timer snapshot invalid: sessionStartElapsed=%d > nowElapsed=%d (reboot)",
                snapshot.sessionStartElapsed, nowElapsed
            )
            return true
        }
        return false
    }


    /**
     * Восстанавливает состояние из [SettingsRepository] при создании
     * Singleton'а.
     *
     * Сценарии:
     * - snapshot == null — таймер не запускался или был сброшен, остаёмся
     *   в [TimerState.IDLE].
     * - `snapshot.sessionStartElapsed > nowElapsed` — устройство было
     *   перезагружено, `elapsedRealtime` сброшен, снимок невалиден.
     *   Очищаем и остаёмся в IDLE.
     * - Иначе — восстанавливаем поля и state. Для `RUNNING` запускаем
     *   тикер через [startJob]; для `PAUSED` elapsedTime фиксирован на
     *   момент паузы.
     */
    private suspend fun restoreFromSnapshot() {
        try {
            val snapshot = settingsRepository.timerSnapshotFlow.first() ?: return
            val nowElapsed = timeProvider.elapsedRealtime()

            if (isReboot(snapshot, nowElapsed)) {
                settingsRepository.clearTimerSnapshot()
                return
            }

            sessionStartTime = snapshot.sessionStartTime
            sessionStartElapsed = snapshot.sessionStartElapsed
            totalPausedDuration = snapshot.totalPausedDuration
            lastPauseElapsed = snapshot.lastPauseElapsed
            _state.value = snapshot.state
            _elapsedTime.value = when (snapshot.state) {
                TimerState.RUNNING ->
                    (nowElapsed - sessionStartElapsed - totalPausedDuration) / 1000L
                TimerState.PAUSED ->
                    (lastPauseElapsed - sessionStartElapsed - totalPausedDuration) / 1000L
                TimerState.IDLE -> 0L
            }

            if (snapshot.state == TimerState.RUNNING) {
                startJob()
            }
        } finally {
            _isRestored.value = true
        }
    }

    /** Сохраняет текущее состояние в [SettingsRepository] (async). */
    private fun saveSnapshot() {
        val snapshot = TimerSnapshot(
            state = _state.value,
            sessionStartTime = sessionStartTime,
            sessionStartElapsed = sessionStartElapsed,
            totalPausedDuration = totalPausedDuration,
            lastPauseElapsed = lastPauseElapsed
        )
        scope.launch { settingsRepository.saveTimerSnapshot(snapshot) }
    }
}
