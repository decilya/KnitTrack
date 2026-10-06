package com.knittrac.app.presentation.feature_settings

import java.io.InputStream
import java.io.OutputStream

/**
 * Контракт для экрана настроек (MVI).
 *
 * Особенность: Action несёт потоки [OutputStream]/[InputStream] для экспорта/импорта.
 * ViewModel не знает про Uri/ContentResolver/Context, соблюдая Clean Architecture.
 */
object SettingsContract {

    data class State(
        val themeMode: String = "SYSTEM",
        val notificationsEnabled: Boolean = true,
        val isLoading: Boolean = true,
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        /** Текущий код языка (например, "ru", "en"). */
        val currentLanguage: String = ""
    )

    sealed class Action {
        data class ChangeThemeMode(val mode: String) : Action()
        data class ToggleNotifications(val enabled: Boolean) : Action()
        /** Смена языка. Требует recreate() Activity для применения. */
        data class ChangeLanguage(val code: String) : Action()
        data class RequestExport(val outputStream: OutputStream) : Action()
        data class ImportData(val inputStream: InputStream) : Action()
    }

    sealed class Effect {
        data object ExportSuccess : Effect()
        data object ExportError : Effect()
        data object ImportSuccess : Effect()
        data object ImportError : Effect()
        /** Требуется перезапуск Activity для применения нового языка. */
        data object RecreateActivity : Effect()
    }
}
