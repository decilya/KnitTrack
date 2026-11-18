package com.knittrack.app.domain.model

import com.knittrack.app.domain.service.TimerState

/**
 * Снимок состояния таймера для персистентности.
 *
 * Нужен, чтобы таймер переживал смерть процесса (system kill, reboot,
 * user swipe). Хранится в DataStore Preferences через
 * `SettingsRepository`. При восстановлении `TimerServiceImpl`
 * пересчитывает `elapsedTime` из [sessionStartElapsed] и [state].
 *
 * @property state Состояние таймера на момент сохранения.
 * @property sessionStartTime Wall clock (Unix epoch) старта сессии.
 * @property sessionStartElapsed Monotonic-время (`elapsedRealtime`)
 *   старта сессии. При reboot сбрасывается — восстановление
 *   возвращает [TimerState.IDLE] и очищает snapshot.
 * @property totalPausedDuration Накопленное время пауз (monotonic).
 * @property lastPauseElapsed Monotonic-время последней паузы.
 */
data class TimerSnapshot(
    val state: TimerState,
    val sessionStartTime: Long,
    val sessionStartElapsed: Long,
    val totalPausedDuration: Long,
    val lastPauseElapsed: Long
)
