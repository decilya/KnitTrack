package com.knittrack.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.preferences.SettingsKeys
import com.knittrack.app.domain.entity.ThemeMode
import com.knittrack.app.domain.model.TimerSnapshot
import com.knittrack.app.domain.repository.SettingsRepository
import com.knittrack.app.domain.service.TimerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Реализация [SettingsRepository] на DataStore Preferences.
 *
 * Все значения хранятся в общем DataStore<Preferences>, предоставляемом
 * core/di/DataStoreModule. Тема сериализуется как [ThemeMode.name] и
 * восстанавливается через [ThemeMode.fromString] с fallback на SYSTEM.
 *
 * @param dataStore Единый DataStore приложения (не создаётся локально —
 *   иначе был бы краш "multiple DataStores active for the same file").
 */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .map { prefs ->
            ThemeMode.fromString(prefs[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    override val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .map { it[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true }

    override val legendAddButtonVisibleFlow: Flow<Boolean> = dataStore.data
        .map { it[SettingsKeys.LEGEND_ADD_BUTTON_VISIBLE] ?: true }

    override val editTutorialShownFlow: Flow<Boolean> = dataStore.data
        .map { it[SettingsKeys.EDIT_TUTORIAL_SHOWN] ?: false }

    override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> = try {
        dataStore.edit { it[SettingsKeys.THEME_MODE] = mode.name }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.e(e, "setThemeMode(%s) failed", mode.name)
        Result.Error(AppError.IoError(e))
    } catch (e: Exception) {
        Timber.e(e, "setThemeMode(%s) unexpected error", mode.name)
        Result.Error(AppError.UnknownError(e))
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> = try {
        dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.e(e, "setNotificationsEnabled(%b) failed", enabled)
        Result.Error(AppError.IoError(e))
    } catch (e: Exception) {
        Timber.e(e, "setNotificationsEnabled(%b) unexpected error", enabled)
        Result.Error(AppError.UnknownError(e))
    }


    override suspend fun setLegendAddButtonVisible(enabled: Boolean): Result<Unit> = try {
        dataStore.edit { it[SettingsKeys.LEGEND_ADD_BUTTON_VISIBLE] = enabled }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.e(e, "setLegendAddButtonVisible(%b) failed", enabled)
        Result.Error(AppError.IoError(e))
    } catch (e: Exception) {
        Timber.e(e, "setLegendAddButtonVisible(%b) unexpected error", enabled)
        Result.Error(AppError.UnknownError(e))
    }

    override suspend fun setEditTutorialShown(shown: Boolean): Result<Unit> = try {
        dataStore.edit { it[SettingsKeys.EDIT_TUTORIAL_SHOWN] = shown }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.e(e, "setEditTutorialShown(%b) failed", shown)
        Result.Error(AppError.IoError(e))
    } catch (e: Exception) {
        Timber.e(e, "setEditTutorialShown(%b) unexpected error", shown)
        Result.Error(AppError.UnknownError(e))
    }

    override val timerSnapshotFlow: Flow<TimerSnapshot?> = dataStore.data
        .map { prefs ->
            val stateName = prefs[SettingsKeys.TIMER_STATE] ?: return@map null
            val state = TimerState.fromStringOrNull(stateName) ?: return@map null
            val startTime = prefs[SettingsKeys.TIMER_SESSION_START_TIME] ?: return@map null
            if (startTime <= 0L) return@map null
            TimerSnapshot(
                state = state,
                sessionStartTime = startTime,
                sessionStartElapsed = prefs[SettingsKeys.TIMER_SESSION_START_ELAPSED] ?: 0L,
                totalPausedDuration = prefs[SettingsKeys.TIMER_TOTAL_PAUSED] ?: 0L,
                lastPauseElapsed = prefs[SettingsKeys.TIMER_LAST_PAUSE_ELAPSED] ?: 0L
            )
        }

    override suspend fun saveTimerSnapshot(snapshot: TimerSnapshot): Result<Unit> = try {
        dataStore.edit {
            it[SettingsKeys.TIMER_STATE] = snapshot.state.name
            it[SettingsKeys.TIMER_SESSION_START_TIME] = snapshot.sessionStartTime
            it[SettingsKeys.TIMER_SESSION_START_ELAPSED] = snapshot.sessionStartElapsed
            it[SettingsKeys.TIMER_TOTAL_PAUSED] = snapshot.totalPausedDuration
            it[SettingsKeys.TIMER_LAST_PAUSE_ELAPSED] = snapshot.lastPauseElapsed
        }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.e(e, "saveTimerSnapshot failed")
        Result.Error(AppError.IoError(e))
    } catch (e: Exception) {
        Timber.e(e, "saveTimerSnapshot unexpected error")
        Result.Error(AppError.UnknownError(e))
    }

    override suspend fun clearTimerSnapshot(): Result<Unit> = try {
        dataStore.edit {
            it.remove(SettingsKeys.TIMER_STATE)
            it.remove(SettingsKeys.TIMER_SESSION_START_TIME)
            it.remove(SettingsKeys.TIMER_SESSION_START_ELAPSED)
            it.remove(SettingsKeys.TIMER_TOTAL_PAUSED)
            it.remove(SettingsKeys.TIMER_LAST_PAUSE_ELAPSED)
        }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.e(e, "clearTimerSnapshot failed")
        Result.Error(AppError.IoError(e))
    } catch (e: Exception) {
        Timber.e(e, "clearTimerSnapshot unexpected error")
        Result.Error(AppError.UnknownError(e))
    }
}