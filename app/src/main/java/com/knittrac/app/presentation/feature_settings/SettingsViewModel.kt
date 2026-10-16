package com.knittrac.app.presentation.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.LocaleService
import com.knittrac.app.domain.repository.SettingsRepository
import com.knittrac.app.domain.service.ExportDataService
import com.knittrac.app.domain.service.ImportDataService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * ViewModel для экрана настроек.
 *
 * Объединяет 3 потока: тема, уведомления, текущий язык.
 * Язык влияет на Configuration, поэтому после смены требуется recreate() Activity.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localeService: LocaleService,
    private val exportDataService: ExportDataService,
    private val importDataService: ImportDataService
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsContract.State())
    val state: StateFlow<SettingsContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsContract.Effect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effect: SharedFlow<SettingsContract.Effect> = _effect.asSharedFlow()

    init {
        _state.update { it.copy(supportedLanguages = localeService.supportedLanguages) }
        observeSettings()
    }

    /**
     * Объединяет три потока в один.
     * combine + update сохраняет isExporting/isImporting при изменении любого потока.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.themeModeFlow,
                settingsRepository.notificationsEnabledFlow,
                localeService.getCurrentLanguageFlow()
            ) { theme, enabled, lang -> Triple(theme, enabled, lang) }
                .collect { (theme, enabled, lang) ->
                    _state.update {
                        it.copy(
                            themeMode = theme,
                            notificationsEnabled = enabled,
                            currentLanguage = lang,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onAction(action: SettingsContract.Action) {
        when (action) {
            is SettingsContract.Action.ChangeThemeMode ->
                viewModelScope.launch {
                    when (val r = settingsRepository.setThemeMode(action.mode)) {
                        is Result.Success -> Unit
                        is Result.Error -> {
                            Timber.e(r.error.cause, "Failed to set theme mode: %s", action.mode)
                            _effect.emit(SettingsContract.Effect.ShowError)
                        }
                    }
                }

            is SettingsContract.Action.ToggleNotifications ->
                viewModelScope.launch {
                    when (val r = settingsRepository.setNotificationsEnabled(action.enabled)) {
                        is Result.Success -> Unit
                        is Result.Error -> {
                            Timber.e(r.error.cause, "Failed to toggle notifications: %b", action.enabled)
                            _effect.emit(SettingsContract.Effect.ShowError)
                        }
                    }
                }

            is SettingsContract.Action.ChangeLanguage ->
                changeLanguage(action.code)

            is SettingsContract.Action.RequestExport ->
                exportData(action.outputStream)

            is SettingsContract.Action.ImportData ->
                importData(action.inputStream)
        }
    }

    /**
     * Меняет язык и эмитит [SettingsContract.Effect.RecreateActivity], чтобы Screen
     * переприменил ресурсы.
     */
    private fun changeLanguage(code: String) {
        viewModelScope.launch {
            when (val r = localeService.setLanguage(code)) {
                is Result.Success -> _effect.emit(SettingsContract.Effect.RecreateActivity)
                is Result.Error -> {
                    Timber.e(r.error.cause, "Failed to change language to: %s", code)
                    _effect.emit(SettingsContract.Effect.ShowError)
                }
            }
        }
    }

    private fun exportData(outputStream: OutputStream) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            when (val result = exportDataService(outputStream)) {
                is Result.Success -> _effect.emit(SettingsContract.Effect.ExportSuccess)
                is Result.Error -> {
                    Timber.e(result.error.cause, "Export failed")
                    _effect.emit(SettingsContract.Effect.ExportError)
                }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    private fun importData(inputStream: InputStream) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            when (val result = importDataService(inputStream)) {
                is Result.Success -> _effect.emit(SettingsContract.Effect.ImportSuccess)
                is Result.Error -> {
                    Timber.e(result.error.cause, "Import failed")
                    _effect.emit(SettingsContract.Effect.ImportError)
                }
            }
            _state.update { it.copy(isImporting = false) }
        }
    }
}
