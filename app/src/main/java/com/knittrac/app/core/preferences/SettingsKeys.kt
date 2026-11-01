package com.knittrac.app.core.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    
    /** Видимость кнопки «Добавить» в LegendEditorSheet (default = true, 7f-D-7). */
    val LEGEND_ADD_BUTTON_VISIBLE = booleanPreferencesKey("legend_add_button_visible")
}
