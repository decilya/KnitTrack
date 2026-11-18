package com.knittrack.app.core.common

import com.knittrack.app.domain.entity.ThemeMode
import com.knittrack.app.domain.model.TimerSnapshot
import com.knittrack.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [SettingsRepository] для тестов.
 *
 * Timer-методы реальные (через [MutableStateFlow]), остальные — заглушки
 * с разумными умолчаниями. Не для продакшена.
 */
class FakeSettingsRepository : SettingsRepository {

    private val _timerSnapshot = MutableStateFlow<TimerSnapshot?>(null)

    /** Текущее значение снимка (для проверки в тестах). */
    val snapshot: TimerSnapshot? get() = _timerSnapshot.value

    /** Устанавливает снимок напрямую (эмуляция восстановления после смерти). */
    fun setSnapshot(s: TimerSnapshot?) {
        _timerSnapshot.value = s
    }

    override val timerSnapshotFlow: Flow<TimerSnapshot?> = _timerSnapshot.asStateFlow()

    override suspend fun saveTimerSnapshot(snapshot: TimerSnapshot) = Result.Success(Unit).also {
        _timerSnapshot.value = snapshot
    }

    override suspend fun clearTimerSnapshot() = Result.Success(Unit).also {
        _timerSnapshot.value = null
    }

    override val themeModeFlow: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
    override val notificationsEnabledFlow: Flow<Boolean> = MutableStateFlow(true)
    override val legendAddButtonVisibleFlow: Flow<Boolean> = MutableStateFlow(true)
    override val editTutorialShownFlow: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun setThemeMode(mode: ThemeMode) = Result.Success(Unit)
    override suspend fun setNotificationsEnabled(enabled: Boolean) = Result.Success(Unit)
    override suspend fun setLegendAddButtonVisible(enabled: Boolean) = Result.Success(Unit)
    override suspend fun setEditTutorialShown(shown: Boolean) = Result.Success(Unit)
}
