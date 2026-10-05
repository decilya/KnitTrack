package com.knittrac.app.core.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}
