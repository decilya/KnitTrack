package com.knittrac.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.preferences.SettingsKeys
import com.knittrac.app.domain.entity.ThemeMode
import com.knittrac.app.domain.repository.SettingsRepository
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
}
