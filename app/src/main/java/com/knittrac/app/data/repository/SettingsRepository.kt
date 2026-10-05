package com.knittrac.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.knittrac.app.core.preferences.SettingsKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val themeModeFlow: Flow<String>
    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun setThemeMode(mode: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences> // Hilt возьмет это из существующего core.di модуля!
) : SettingsRepository {

    override val themeModeFlow: Flow<String> = dataStore.data
        .map { it[SettingsKeys.THEME_MODE] ?: "SYSTEM" }

    override val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .map { it[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[SettingsKeys.THEME_MODE] = mode }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled }
    }
}
