package com.knittrac.app.presentation.feature_settings

/**
 * Контракт для экрана настроек (MVI).
 *
 * ВАЖНО: эффекты не несут сырых сообщений об ошибках.
 * Локализация выполняется в UI через stringResource() / getString().
 * Детали ошибок логируются через Timber в ViewModel для безопасности.
 */
object SettingsContract {

    data class State(
        val themeMode: String = "SYSTEM",
        val notificationsEnabled: Boolean = true,
        val isLoading: Boolean = true,
        val isExporting: Boolean = false,
        val isImporting: Boolean = false
    )

    sealed class Action {
        data class ChangeThemeMode(val mode: String) : Action()
        data class ToggleNotifications(val enabled: Boolean) : Action()
        data object RequestExport : Action()
        data class ImportData(val json: String) : Action()
    }

    sealed class Effect {
        /** JSON готов, Screen запишет его в Uri. */
        data class ExportReady(val json: String) : Effect()
        
        /** 
         * Ошибка экспорта. 
         * UI покажет R.string.settings_export_error, а детали останутся в логах.
         */
        data object ExportError : Effect()
        
        /** Импорт успешен. */
        data object ImportSuccess : Effect()
        
        /** 
         * Ошибка импорта. 
         * UI покажет R.string.settings_import_error, а детали останутся в логах.
         */
        data object ImportError : Effect()
    }
}
