package com.knittrac.app.presentation.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsContract.State())
    val state: StateFlow<SettingsContract.State> = _state.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collect { mode ->
                _state.update { it.copy(themeMode = mode, isLoading = false) }
            }
        }
        viewModelScope.launch {
            settingsRepository.notificationsEnabledFlow.collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled, isLoading = false) }
            }
        }
    }

    fun onAction(action: SettingsContract.Action) {
        when (action) {
            is SettingsContract.Action.ChangeThemeMode -> {
                viewModelScope.launch { settingsRepository.setThemeMode(action.mode) }
            }
            is SettingsContract.Action.ToggleNotifications -> {
                viewModelScope.launch { settingsRepository.setNotificationsEnabled(action.enabled) }
            }
        }
    }
}
