package com.knittrack.app.core.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    
    /** Видимость кнопки «Добавить» в LegendEditorSheet (default = true, 7f-D-7). */
    val LEGEND_ADD_BUTTON_VISIBLE = booleanPreferencesKey("legend_add_button_visible")

    /** Показан ли одноразовый туториал Pattern Editor (P2-баг, 05.11.2026). */
    val EDIT_TUTORIAL_SHOWN = booleanPreferencesKey("edit_tutorial_shown")

    /**
     * Состояние таймера для персистентности (P2 таймер-персистентность).
     * Строковое имя [com.knittrack.app.domain.service.TimerState.name].
     */
    val TIMER_STATE = stringPreferencesKey("timer_state")

    /** Wall clock (Unix epoch) старта сессии. */
    val TIMER_SESSION_START_TIME = longPreferencesKey("timer_session_start_time")

    /** Monotonic-время (`elapsedRealtime`) старта сессии. */
    val TIMER_SESSION_START_ELAPSED = longPreferencesKey("timer_session_start_elapsed")

    /** Накопленное время пауз (monotonic). */
    val TIMER_TOTAL_PAUSED = longPreferencesKey("timer_total_paused")

    /** Monotonic-время последней паузы. */
    val TIMER_LAST_PAUSE_ELAPSED = longPreferencesKey("timer_last_pause_elapsed")
}
