package com.knittrac.app.presentation.feature_timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.service.GetAllProjectsService
import com.knittrac.app.domain.service.MarkNextRowParams
import com.knittrac.app.domain.service.MarkNextRowUseCase
import com.knittrac.app.domain.service.SaveSessionParams
import com.knittrac.app.domain.service.SaveSessionService
import com.knittrac.app.domain.service.TimerService
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

/**
 * ViewModel экрана таймера.
 *
 * Отвечает за старт/паузу/сброс таймера, выбор проекта, счётчик рядов
 * (сессии и схемы), сохранение сессии. Проверяет разрешение
 * POST_NOTIFICATIONS перед стартом и эмитит effect, если нужно показать
 * системный диалог.
 *
 * Два независимых счётчика (KnitCompanion-like UX):
 * - [TimerContract.State.rowCount] — локальный счётчик сессии
 *   (в `SavedStateHandle`, идёт в `SaveSessionParams.rowCount`);
 * - [TimerContract.State.currentPatternRowNumber] — текущий ряд по схеме
 *   (в БД через [MarkNextRowUseCase] → [com.knittrac.app.domain.service.ProgressService.markNextRow]).
 *
 * Защита от double-save: флаг [TimerContract.State.isSaving] устанавливается
 * синхронно в [onIntent] до запуска корутины. Аналогично
 * [TimerContract.State.isMarkingRow] — для [TimerContract.Intent.MarkNextPatternRow].
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerService,
    private val saveSessionService: SaveSessionService,
    private val getAllProjectsService: GetAllProjectsService,
    private val markNextRowUseCase: MarkNextRowUseCase,
    private val savedStateHandle: SavedStateHandle
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
        viewModelScope.launch {
            combine(timerManager.elapsedTime, timerManager.state) { elapsed, status ->
                TimerContract.State(
                    elapsedTime = elapsed,
                    status = status,
                    projectId = _state.value.projectId,
                    startTimestamp = timerManager.sessionStartTime,
                    rowCount = _state.value.rowCount,
                    targetRowCount = _state.value.targetRowCount,
                    projects = _state.value.projects,
                    isSaving = _state.value.isSaving,
                    currentPatternRowNumber = _state.value.currentPatternRowNumber,
                    isMarkingRow = _state.value.isMarkingRow
                )
            }.collect { _state.value = it }
        }

        viewModelScope.launch {
            getAllProjectsService(Unit).collect { result ->
                if (result is Result.Success) {
                    _state.update { currentState ->
                        val selected = result.data.find { it.id == currentState.projectId }
                        currentState.copy(
                            projects = result.data,
                            targetRowCount = selected?.targetRowCount?.value,
                            currentPatternRowNumber = currentPatternRowNumberFor(selected)
                        )
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
            is TimerContract.Intent.Pause -> timerManager.pause()
            is TimerContract.Intent.Reset -> {
                timerManager.reset()
                _state.update { it.copy(rowCount = 0) }
                savedStateHandle["rowCount"] = 0
            }
            is TimerContract.Intent.SelectProject -> handleSelectProject(intent)
            is TimerContract.Intent.SaveSession -> saveSession()
            is TimerContract.Intent.IncrementRow -> handleIncrementRow()
            is TimerContract.Intent.DecrementRow -> handleDecrementRow()
            is TimerContract.Intent.MarkNextPatternRow -> handleMarkNextPatternRow()
        }
    }

    /**
     * Запуск таймера с проверкой выбранного проекта и разрешения
     * на foreground-уведомления.
     */
    private fun handleStart() {
        if (_state.value.projectId == null) {
            viewModelScope.launch { _effect.send(TimerContract.Effect.NavigateToProjects) }
            return
        }
        timerManager.start()
    }

    /**
     * Выбор проекта и синхронизация целевого количества рядов и текущего
     * ряда схемы в [TimerContract.State].
     */
    private fun handleSelectProject(intent: TimerContract.Intent.SelectProject) {
        val selectedProject = _state.value.projects.find { it.id == intent.projectId }
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
     * Увеличивает локальный счётчик рядов сессии на единицу.
     *
     * НЕ обновляет [TimerContract.State.currentPatternRowNumber] — это
     * отдельный счётчик (прогресс по схеме), см. [handleMarkNextPatternRow].
     */
    private fun handleIncrementRow() {
        val newCount = _state.value.rowCount + 1
        _state.update { it.copy(rowCount = newCount) }
        savedStateHandle["rowCount"] = newCount
    }

    /**
     * Уменьшает локальный счётчик рядов сессии на единицу, но не ниже нуля.
     */
    private fun handleDecrementRow() {
        val newCount = (_state.value.rowCount - 1).coerceAtLeast(0)
        _state.update { it.copy(rowCount = newCount) }
        savedStateHandle["rowCount"] = newCount
    }

    /**
     * Отмечает следующий ряд схемы как связанный (аналог One-Tap Marker
     * в KnitCompanion).
     *
     * ВАЖНО: [TimerContract.State.rowCount] (локальный счётчик сессии)
     * НЕ инкрементится — это отдельная сущность. Обновляется только
     * [TimerContract.State.currentPatternRowNumber] (источник — ответ
     * [MarkNextRowUseCase]).
     *
     * Guard от двойного тапа: флаг [TimerContract.State.isMarkingRow]
     * ставится синхронно до launch. Снимается в finally, чтобы не остался
     * висеть при ошибке сервиса.
     */
    private fun handleMarkNextPatternRow() {
        if (_state.value.isMarkingRow) return
        val projectId = resolveProjectIdForMarking() ?: return
        _state.update { it.copy(isMarkingRow = true) }
        viewModelScope.launch {
            try {
                when (val result = markNextRowUseCase(MarkNextRowParams(projectId = projectId))) {
                    is Result.Success -> _state.update {
                        it.copy(currentPatternRowNumber = result.data.value)
                    }
                    is Result.Error -> _effect.send(
                        TimerContract.Effect.ShowError(
                            com.knittrac.app.R.string.pattern_row_mark_error
                        )
                    )
                }
            } finally {
                _state.update { it.copy(isMarkingRow = false) }
            }
        }
    }

    /**
     * Возвращает [ProjectId] выбранного проекта, или null, если проект
     * не выбран / id некорректен (<= 0).
     */
    private fun resolveProjectIdForMarking(): ProjectId? {
        val projectIdValue = _state.value.projectId ?: return null
        return ProjectId.createOrNull(projectIdValue)
    }

    /**
     * Возвращает номер текущего ряда схемы для проекта или null,
     * если схема не привязана.
     */
    private fun currentPatternRowNumberFor(project: Project?): Int? =
        if (project?.currentPatternRowId != null) project.currentRowNumber.value else null

    /**
     * Сохраняет текущую сессию.
     *
     * Guard от двойного вызова: если [TimerContract.State.isSaving] уже true —
     * выходим немедленно. Флаг ставится синхронно (до launch), поэтому второй
     * вызов [onIntent] не создаёт вторую корутину. Снимается в finally, чтобы
     * не остался висеть при ошибке сервиса.
     */
    private fun saveSession() {
        if (_state.value.isSaving) return

        val currentState = _state.value
        val projectId = currentState.projectId ?: return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                // endTimestamp вычисляется из монотонного elapsedTime, а не из
                // wall clock. Это защищает от NTP-сдвига или ручного перевода
                // часов: duration всегда положителен и точен.
                val endTimestamp = currentState.startTimestamp +
                    (currentState.elapsedTime * 1000L)

                val params = SaveSessionParams(
                    projectId = projectId,
                    startTimestamp = currentState.startTimestamp,
                    endTimestamp = endTimestamp,
                    rowCount = currentState.rowCount
                )

                val result = saveSessionService(params)
                if (result is Result.Success) {
                    _effect.send(TimerContract.Effect.SessionSaved)
                    timerManager.reset()
                    _state.update { it.copy(rowCount = 0) }
                    savedStateHandle["rowCount"] = 0
                } else {
                    _effect.send(
                        TimerContract.Effect.ShowError(com.knittrac.app.R.string.timer_save_error)
                    )
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
