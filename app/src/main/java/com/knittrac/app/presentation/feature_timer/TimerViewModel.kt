package com.knittrac.app.presentation.feature_timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.ErrorStringProvider
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.StitchCount

import com.knittrac.app.domain.service.SaveSessionParams
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel экрана таймера (P1-C, 02.11.2026).
 *
 * Декомпозирован: 5 зависимостей (God Object) → 3 (оркестратор):
 * - [TimerSessionCoordinator] — start/pause/reset/save + elapsedTime/state.
 * - [ProjectProgressCoordinator] — projects flow + markNext.
 * - [SavedStateHandle] — projectId, rowCount (process death).
 *
 * SRP: TimerViewModel — **оркестратор**: собирает State через combine,
 * диспетчит Intent'ы, держит guards (isSaving / isMarkingRow).
 *
 * Два независимых счётчика (KnitCompanion-like UX):
 * - [TimerContract.State.rowCount] — локальный счётчик сессии
 *   (в `SavedStateHandle`, идёт в `SaveSessionParams.rowCount`);
 * - [TimerContract.State.currentPatternRowNumber] — текущий ряд по схеме
 *   (в БД через [ProjectProgressCoordinator.markNext]).
 *
 * Защита от double-save/double-tap: флаги [TimerContract.State.isSaving]
 * и [TimerContract.State.isMarkingRow] устанавливаются синхронно в
 * [onIntent] до запуска корутины.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val sessionCoordinator: TimerSessionCoordinator,
    private val progressCoordinator: ProjectProgressCoordinator,
    private val savedStateHandle: SavedStateHandle,
    private val errorStringProvider: ErrorStringProvider
) : ViewModel() {

    private val _state = MutableStateFlow(
        TimerContract.State(
            projectId = savedStateHandle.get<Long>("projectId"),
            rowCount = savedStateHandle.get<Int>("rowCount") ?: 0
        )
    )
    val state: StateFlow<TimerContract.State> = _state.asStateFlow()

    private val _effect = Channel<TimerContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // combine: elapsedTime + status из sessionCoordinator.
        //
        // ВАЖНО: не читать _state.value в combine-лямбде и не делать
        // _state.value = it в collect. combine-эмиссии и onIntent-апдейты
        // конкурируют: чтение снимка + полная замена затирают конкурентные
        // _state.update { } (rowCount, isSaving, projects и т.д.) — race.
        // Безопасно только _state.update { current -> current.copy(...) } —
        // CAS по контракту MutableStateFlow, non-flow поля сохраняются.
        viewModelScope.launch {
            combine(
                sessionCoordinator.elapsedTime,
                sessionCoordinator.state,
            ) { elapsed, status -> elapsed to status }
                .collect { (elapsed, status) ->
                    val startTimestamp = sessionCoordinator.sessionStartTime
                    _state.update { current ->
                        current.copy(
                            elapsedTime = elapsed,
                            status = status,
                            startTimestamp = startTimestamp,
                        )
                    }
                }
        }

        // Подписка на поток проектов — обновление State (targetRowCount, currentPatternRowNumber).
        viewModelScope.launch {
            progressCoordinator.projects().collect { result ->
                when (result) {
                    is Result.Success -> _state.update { currentState ->
                        val selected = result.data.find { it.id.value == currentState.projectId }
                        currentState.copy(
                            projects = result.data,
                            targetRowCount = selected?.targetRowCount?.value,
                            currentPatternRowNumber = currentPatternRowNumberFor(selected)
                        )
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to load projects in TimerViewModel")
                        _effect.send(TimerContract.Effect.ShowError(errorStringProvider.getStringRes(result.error.code)))
                    }
                }
            }
        }
    }

    /**
     * Единая точка входа для действий UI (MVI).
     */
    fun onIntent(intent: TimerContract.Intent) {
        when (intent) {
            is TimerContract.Intent.Start -> handleStart()
            is TimerContract.Intent.Pause -> sessionCoordinator.pause()
            is TimerContract.Intent.Reset -> {
                sessionCoordinator.reset()
                _state.update { it.copy(rowCount = 0) }
                savedStateHandle["rowCount"] = 0
            }
            is TimerContract.Intent.SelectProject -> handleSelectProject(intent)
            is TimerContract.Intent.SaveSession -> saveSession()
            is TimerContract.Intent.IncrementRow -> handleIncrementRow()
            is TimerContract.Intent.DecrementRow -> handleDecrementRow()
            is TimerContract.Intent.MarkNextPatternRow -> handleMarkNextPatternRow()
            is TimerContract.Intent.IncrementStitch -> handleIncrementStitch()
            is TimerContract.Intent.DecrementStitch -> handleDecrementStitch()
        }
    }

    /**
     * Запуск таймера: guard на выбранный проект.
     */
    private fun handleStart() {
        if (_state.value.projectId == null) {
            viewModelScope.launch { _effect.send(TimerContract.Effect.NavigateToProjects) }
            return
        }
        sessionCoordinator.start()
    }

    /**
     * Выбор проекта: State + savedStateHandle.
     */
    private fun handleSelectProject(intent: TimerContract.Intent.SelectProject) {
        val selectedProject = _state.value.projects.find { it.id.value == intent.projectId }
        _state.update {
            it.copy(
                projectId = intent.projectId,
                targetRowCount = selectedProject?.targetRowCount?.value,
                currentPatternRowNumber = currentPatternRowNumberFor(selectedProject)
            )
        }
        savedStateHandle["projectId"] = intent.projectId
    }

    /**
     * Инкремент локального счётчика сессии.
     * НЕ обновляет currentPatternRowNumber — это отдельный счётчик (One-Tap Marker).
     */
    private fun handleIncrementRow() {
        val newCount = _state.value.rowCount + 1
        _state.update { it.copy(rowCount = newCount) }
        savedStateHandle["rowCount"] = newCount
    }

    /**
     * Декремент локального счётчика сессии, не ниже нуля.
     */
    private fun handleDecrementRow() {
        val newCount = (_state.value.rowCount - 1).coerceAtLeast(0)
        _state.update { it.copy(rowCount = newCount) }
        savedStateHandle["rowCount"] = newCount
    }

    /**
     * Отметка следующего ряда схемы (One-Tap Marker).
     *
     * Guard [TimerContract.State.isMarkingRow] — синхронно до launch.
     * Guard [resolveProjectIdForMarking] — без projectId не вызываем.
     */
    private fun handleMarkNextPatternRow() {
        if (_state.value.isMarkingRow) return
        val projectId = resolveProjectIdForMarking() ?: return
        _state.update { it.copy(isMarkingRow = true) }
        viewModelScope.launch {
            try {
                when (val result = progressCoordinator.markNext(projectId)) {
                    is Result.Success -> _state.update {
                        it.copy(currentPatternRowNumber = result.data.value)
                    }
                    is Result.Error -> _effect.send(
                        TimerContract.Effect.ShowError(errorStringProvider.getStringRes(result.error.code))
                    )
                }
            } finally {
                _state.update { it.copy(isMarkingRow = false) }
            }
        }
    }

    /**
     * ProjectId выбранного проекта (или null).
     */
    private fun resolveProjectIdForMarking(): ProjectId? {
        val projectIdValue = _state.value.projectId ?: return null
        return ProjectId.createOrNull(projectIdValue)
    }

    /**
     * Текущий ряд схемы (из проекта) или null, если схема не привязана.
     */
    private fun currentPatternRowNumberFor(project: Project?): Int? =
        if (project?.currentPatternRowId != null) project.currentRowNumber.value else null

    /**
     * Сохранение сессии.
     *
     * Guard [TimerContract.State.isSaving] — синхронно до launch.
     * endTimestamp вычисляется из монотонного elapsedTime (устойчиво к NTP-сдвигу).
     */

    private fun handleIncrementStitch() {
        val projectId = _state.value.projectId ?: return
        val current = _state.value.currentStitch
        updateStitch(projectId, current + 1)
    }

    private fun handleDecrementStitch() {
        val projectId = _state.value.projectId ?: return
        val current = _state.value.currentStitch
        if (current > 0) {
            updateStitch(projectId, current - 1)
        }
    }

    private fun updateStitch(projectIdValue: Long, newStitchValue: Int) {
        val projectId = ProjectId.createOrDefault(projectIdValue)
        viewModelScope.launch {
            val result = progressCoordinator.updateStitch(projectId, StitchCount.createOrDefault(newStitchValue))
            if (result is Result.Error) {
                _effect.send(TimerContract.Effect.ShowError(errorStringProvider.getStringRes(result.error.code)))
            }
        }
    }
    private fun saveSession() {
        if (_state.value.isSaving) return

        val currentState = _state.value
        val projectId = currentState.projectId ?: return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val projectIdVo = ProjectId.createOrDefault(projectId)
                val endTimestamp = currentState.startTimestamp + (currentState.elapsedTime * 1000L)
                val params = SaveSessionParams(
                    projectId = projectIdVo,
                    startTimestamp = currentState.startTimestamp,
                    endTimestamp = endTimestamp,
                    rowCount = RowCount.createOrDefault(currentState.rowCount)
                )
                handleSessionResult(sessionCoordinator.saveSession(params))
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun handleSessionResult(result: Result<*>) {
        when (result) {
            is Result.Success -> {
                _effect.send(TimerContract.Effect.SessionSaved)
                sessionCoordinator.reset()
                _state.update { it.copy(rowCount = 0) }
                savedStateHandle["rowCount"] = 0
            }
            is Result.Error -> {
                Timber.e(result.error.cause, "Failed to save session")
                _effect.send(TimerContract.Effect.ShowError(errorStringProvider.getStringRes(result.error.code)))
            }
        }
    }
}
