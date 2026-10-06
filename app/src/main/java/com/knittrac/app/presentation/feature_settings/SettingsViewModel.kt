package com.knittrac.app.presentation.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.repository.SettingsRepository
import com.knittrac.app.domain.service.ExportDataService
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
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * ViewModel для экрана настроек.
 *
 * Эмитит только маркеры ошибок — UI сам подбирает локализованный текст.
 * Не зависит от Android Framework: работает с потоками, а не с Uri.
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

    /**
     * combine + update сохраняет isExporting/isImporting при смене темы.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.themeModeFlow,
                settingsRepository.notificationsEnabledFlow
            ) { theme, enabled -> theme to enabled }
                .collect { (theme, enabled) ->
                    _state.update {
                        it.copy(
                            themeMode = theme,
                            notificationsEnabled = enabled,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onAction(action: SettingsContract.Action) {
        when (action) {
            is SettingsContract.Action.ChangeThemeMode ->
                viewModelScope.launch { settingsRepository.setThemeMode(action.mode) }

            is SettingsContract.Action.ToggleNotifications ->
                viewModelScope.launch { settingsRepository.setNotificationsEnabled(action.enabled) }

            is SettingsContract.Action.RequestExport -> exportData(action.outputStream)

            is SettingsContract.Action.ImportData -> importData(action.inputStream)
        }
    }

    private fun exportData(outputStream: OutputStream) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            when (val result = exportDataService(outputStream)) {
                is Result.Success -> _effect.emit(SettingsContract.Effect.ExportSuccess)
                is Result.Error -> {
                    Timber.e("Export failed: ${result.error.message}")
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
                    Timber.e("Import failed: ${result.error.message}")
                    _effect.emit(SettingsContract.Effect.ImportError)
                }
            }
            _state.update { it.copy(isImporting = false) }
        }
    }
}
