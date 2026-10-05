package com.knittrac.app.presentation.feature_settings

object SettingsContract {
    data class State(
        val themeMode: String = "SYSTEM",
        val notificationsEnabled: Boolean = true,
        val isLoading: Boolean = true
    )

    sealed class Action {
        data class ChangeThemeMode(val mode: String) : Action()
        data class ToggleNotifications(val enabled: Boolean) : Action()
    }
}
