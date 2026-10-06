package com.knittrac.app.presentation.feature_timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.service.GetAllProjectsService
import com.knittrac.app.domain.service.SaveSessionParams
import com.knittrac.app.domain.service.SaveSessionService
import com.knittrac.app.domain.service.TimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager,
    private val saveSessionService: SaveSessionService,
    private val getAllProjectsService: GetAllProjectsService
) : ViewModel() {

    private val _state = MutableStateFlow(TimerContract.State())
    val state: StateFlow<TimerContract.State> = _state.asStateFlow()

    private val _effect = Channel<TimerContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(timerManager.elapsedTime, timerManager.state) { elapsed, status ->
                TimerContract.State(
                    elapsedTime = elapsed,
                    status = status,
                    projectId = _state.value.projectId,
                    startTimestamp = timerManager.sessionStartTime,
                    rowCount = _state.value.rowCount,
                    projects = _state.value.projects
                )
            }.collect { _state.value = it }
        }

        viewModelScope.launch {
            getAllProjectsService(Unit).collect { result ->
                if (result is Result.Success) {
                    _state.update { it.copy(projects = result.data) }
                }
            }
        }
    }

    fun onIntent(intent: TimerContract.Intent) {
        when (intent) {
            is TimerContract.Intent.Start -> {
                if (_state.value.projectId == null) {
                    viewModelScope.launch { _effect.send(TimerContract.Effect.NavigateToProjects) }
                    return
                }
                timerManager.start()
            }
            is TimerContract.Intent.Pause -> timerManager.pause()
            is TimerContract.Intent.Reset -> {
                timerManager.reset()
                _state.update { it.copy(rowCount = 0) }
            }
            is TimerContract.Intent.SelectProject -> {
                _state.update { it.copy(projectId = intent.projectId) }
            }
            is TimerContract.Intent.SaveSession -> {
                viewModelScope.launch {
                    val currentState = _state.value
                    val projectId = currentState.projectId ?: return@launch
                    
                    val params = SaveSessionParams(
                        projectId = projectId,
                        startTimestamp = currentState.startTimestamp,
                        endTimestamp = System.currentTimeMillis(),
                        rowCount = currentState.rowCount
                    )
                    
                    val result = saveSessionService(params)
                    if (result is Result.Success) {
                        _effect.send(TimerContract.Effect.SessionSaved)
                        timerManager.reset()
                        _state.update { it.copy(rowCount = 0) }
                    } else {
                        _effect.send(TimerContract.Effect.ShowError(com.knittrac.app.R.string.timer_save_error))
                    }
                }
            }
            is TimerContract.Intent.UpdateRowCount -> {
                _state.update { it.copy(rowCount = intent.count) }
            }
        }
    }
}
