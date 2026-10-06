package com.knittrac.app.presentation.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.repository.SettingsRepository
import com.knittrac.app.domain.service.ExportDataService
import com.knittrac.app.domain.service.ImportDataParams
import com.knittrac.app.domain.service.ImportDataService
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

/**
 * ViewModel для экрана настроек.
 * Эмитит только маркеры ошибок — UI сам подбирает локализованный текст.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportDataService: ExportDataService,
    private val importDataService: ImportDataService
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsContract.State())
    val state: StateFlow<SettingsContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsContract.Effect>()
    val effect: SharedFlow<SettingsContract.Effect> = _effect.asSharedFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.themeModeFlow,
                settingsRepository.notificationsEnabledFlow
            ) { theme, enabled ->
                SettingsContract.State(
                    themeMode = theme,
                    notificationsEnabled = enabled,
                    isLoading = false
                )
            }.collect { _state.value = it }
        }
    }

    fun onAction(action: SettingsContract.Action) {
        when (action) {
            is SettingsContract.Action.ChangeThemeMode ->
                viewModelScope.launch { settingsRepository.setThemeMode(action.mode) }

            is SettingsContract.Action.ToggleNotifications ->
                viewModelScope.launch { settingsRepository.setNotificationsEnabled(action.enabled) }

            SettingsContract.Action.RequestExport -> requestExport()

            is SettingsContract.Action.ImportData -> importData(action.json)
        }
    }

    private fun requestExport() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            when (val result = exportDataService()) {
                is Result.Success ->
                    _effect.emit(SettingsContract.Effect.ExportReady(result.data))
                is Result.Error -> {
                    Timber.e("Export failed: ${result.error.message}")
                    _effect.emit(SettingsContract.Effect.ExportError)
                }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    private fun importData(json: String) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            // Передаем объект ImportDataParams вместо голой строки
            when (val result = importDataService(ImportDataParams(json))) {
                is Result.Success ->
                    _effect.emit(SettingsContract.Effect.ImportSuccess)
                is Result.Error -> {
                    Timber.e("Import failed: ${result.error.message}")
                    _effect.emit(SettingsContract.Effect.ImportError)
                }
            }
            _state.update { it.copy(isImporting = false) }
        }
    }
}
