package com.knittrac.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.knittrac.app.core.preferences.SettingsKeys
import com.knittrac.app.domain.entity.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий пользовательских настроек (тема, уведомления).
 *
 * Все значения хранятся в общем DataStore<Preferences>, предоставляемом
 * core/di/DataStoreModule. Тема — типобезопасный [ThemeMode], хранится как [ThemeMode.name].
 */
interface SettingsRepository {
    val themeModeFlow: Flow<ThemeMode>
    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .map { prefs ->
            ThemeMode.fromString(prefs[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    override val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .map { it[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[SettingsKeys.THEME_MODE] = mode.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled }
    }
}
