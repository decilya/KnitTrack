package com.knittrac.app.presentation.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.LocaleService
import com.knittrac.app.domain.repository.SettingsRepository
import com.knittrac.app.domain.service.ExportDataService
import com.knittrac.app.domain.service.ImportDataService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
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

    private val _effect = Channel<SettingsContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

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
                localeService.getCurrentLanguageFlow(),
                settingsRepository.legendAddButtonVisibleFlow
            ) { theme, enabled, lang, legendAdd -> SettingsSnapshot(theme, enabled, lang, legendAdd) }
                .collect { snapshot ->
                    _state.update {
                        it.copy(
                            themeMode = snapshot.theme,
                            notificationsEnabled = snapshot.notificationsEnabled,
                            currentLanguage = snapshot.language,
                            legendAddButtonVisible = snapshot.legendAddButtonVisible,
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
                            _effect.send(SettingsContract.Effect.ShowError)
                        }
                    }
                }

            is SettingsContract.Action.ToggleNotifications ->
                viewModelScope.launch {
                    when (val r = settingsRepository.setNotificationsEnabled(action.enabled)) {
                        is Result.Success -> Unit
                        is Result.Error -> {
                            Timber.e(r.error.cause, "Failed to toggle notifications: %b", action.enabled)
                            _effect.send(SettingsContract.Effect.ShowError)
                        }
                    }
                }

            is SettingsContract.Action.ToggleLegendAddButton ->
                viewModelScope.launch {
                    when (val r = settingsRepository.setLegendAddButtonVisible(action.enabled)) {
                        is Result.Success -> Unit
                        is Result.Error -> {
                            Timber.e(r.error.cause, "Failed to toggle legend add button: %b", action.enabled)
                            _effect.send(SettingsContract.Effect.ShowError)
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
                is Result.Success -> _effect.send(SettingsContract.Effect.RecreateActivity)
                is Result.Error -> {
                    Timber.e(r.error.cause, "Failed to change language to: %s", code)
                    _effect.send(SettingsContract.Effect.ShowError)
                }
            }
        }
    }

    private fun exportData(outputStream: OutputStream) {
        if (_state.value.isExporting) return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                when (val result = exportDataService(outputStream)) {
                    is Result.Success -> _effect.send(SettingsContract.Effect.ExportSuccess)
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Export failed")
                        _effect.send(SettingsContract.Effect.ExportError)
                    }
                }
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun importData(inputStream: InputStream) {
        if (_state.value.isImporting) return
        _state.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            try {
                when (val result = importDataService(inputStream)) {
                    is Result.Success -> _effect.send(SettingsContract.Effect.ImportSuccess)
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Import failed")
                        _effect.send(SettingsContract.Effect.ImportError)
                    }
                }
            } finally {
                _state.update { it.copy(isImporting = false) }
            }
        }
    }


    /**
     * Снапшот настроек для combine (4 потока -> 4 поля).
     * Используем DTO вместо Triple (Урок 26).
     */
    private data class SettingsSnapshot(
        val theme: com.knittrac.app.domain.entity.ThemeMode,
        val notificationsEnabled: Boolean,
        val language: String,
        val legendAddButtonVisible: Boolean
    )
}
