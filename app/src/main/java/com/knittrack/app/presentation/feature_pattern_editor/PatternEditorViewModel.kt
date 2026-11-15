package com.knittrack.app.presentation.feature_pattern_editor

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrack.app.R
import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.ColumnStart
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternOperation
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.domain.service.CreatePatternParams
import com.knittrack.app.domain.service.DeleteOperationParams
import com.knittrack.app.domain.service.DeletePatternRowParams
import com.knittrack.app.domain.service.UpdatePatternRowNumberParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.knittrack.app.domain.service.AddSymbolParams
import com.knittrack.app.domain.service.DeleteSymbolParams

/**
 * ViewModel экрана Pattern Editor (7f-B).
 *
 * Загружает список схем (для селектора), выбранную схему (по id), оркестрирует
 * добавление операций. Валидация (legend, repeat, overlap) — в
 * [com.knittrack.app.domain.service.AddPatternOperationService].
 *
 * State management:
 * - `patternId`, `selectedRowId`, `selectedColumn`, `selectedSymbolCode`,
 *   `isStickyMode` — через [SavedStateHandle] (переживают process death).
 * - `zoom`, `pan` — в Composable через `rememberSaveable` (UI-only, не VM).
 *
 * @see PatternEditorContract
 */
@HiltViewModel
/**
 * ViewModel экрана Pattern Editor (7f-B, расширен 7f-D-6/7, декомпозиция P1-A 02.11.2026).
 *
 * UseCase разбиты на 4 группы по причине изменения (SRP, P1-A):
 * - [PatternEditorPatternUseCases] — жизненный цикл Pattern (read + create).
 * - [PatternEditorOperationUseCases] — операции в рядах (add/update/delete).
 * - [PatternEditorRowUseCases] — ряды (update rowNumber, delete).
 * - [PatternEditorSymbolUseCases] — легенда (add/delete CUSTOM).
 *
 * [PatternEditorPlatformDeps] — платформенные зависимости
 * (DataStore + SavedStateHandle).
 *
 * 5 параметров (< порога Detekt LongParameterList = 6) — @Suppress("LongParameterList")
 * не нужен. Domain не меняется.
 */
class PatternEditorViewModel @Inject constructor(
    private val patternUseCases: PatternEditorPatternUseCases,
    private val operationUseCases: PatternEditorOperationUseCases,
    private val rowUseCases: PatternEditorRowUseCases,
    private val symbolUseCases: PatternEditorSymbolUseCases,
    private val platformDeps: PatternEditorPlatformDeps
) : ViewModel() {

    private val _state = MutableStateFlow(
        PatternEditorContract.State(
            selection = PatternEditorContract.SelectionState(
                selectedRowId = platformDeps.savedStateHandle.get<Long>(KEY_SELECTED_ROW_ID)
                    ?.let { PatternRowId.createOrNull(it) },
                selectedColumn = platformDeps.savedStateHandle.get<Int>(KEY_SELECTED_COLUMN)
                    ?.let { ColumnStart.createOrNull(it) },
                selectedSymbolCode = platformDeps.savedStateHandle.get<String>(KEY_SELECTED_SYMBOL),
                isStickyMode = platformDeps.savedStateHandle.get<Boolean>(KEY_IS_STICKY_MODE) ?: true
            )
        )
    )
    val state: StateFlow<PatternEditorContract.State> = _state.asStateFlow()

    private val _effect = Channel<PatternEditorContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /** Job активной загрузки — отменяется при повторном loadPattern. */
    private var loadJob: Job? = null
    private var summariesJob: Job? = null

    init {
        observePatternSummaries()
        observeLegendAddButtonSetting()
        platformDeps.savedStateHandle.get<Long>(KEY_PATTERN_ID)?.let { raw ->
            PatternId.createOrNull(raw)?.let { loadPattern(it) }
        }
    }

    /**
     * Единая точка входа для действий UI.
     */
    fun onIntent(intent: PatternEditorContract.Intent) {
        when (intent) {
            is PatternEditorContract.Intent.LoadPattern -> loadPattern(intent.patternId)
            is PatternEditorContract.Intent.SelectPattern -> selectPattern(intent.patternId)
            is PatternEditorContract.Intent.SelectRow -> selectRow(intent.rowId)
            is PatternEditorContract.Intent.SelectColumn -> selectColumn(intent.columnStart)
            is PatternEditorContract.Intent.SelectSymbol -> selectSymbol(intent.symbolCode)
            is PatternEditorContract.Intent.ToggleMode -> toggleMode()
            is PatternEditorContract.Intent.AddOperation -> addOperation(intent)
            is PatternEditorContract.Intent.CreatePattern -> createPattern(intent.params)
            is PatternEditorContract.Intent.UndoLastOperation -> undoLastOperation()
            is PatternEditorContract.Intent.ShowEditTutorial -> showEditTutorialManually()
            is PatternEditorContract.Intent.DismissEditTutorial -> dismissEditTutorial()
            is PatternEditorContract.Intent.Retry -> retry()
            is PatternEditorContract.Intent.ExportSvg -> exportSvg(intent.outputStream)
            is PatternEditorContract.Intent.ExportPdf -> exportPdf(intent.outputStream)
            is PatternEditorContract.Intent.OpenLegendEditor -> _state.update { it.copy(ui = it.ui.copy(isLegendEditorOpen = true)) }
            is PatternEditorContract.Intent.CloseLegendEditor -> _state.update { it.copy(ui = it.ui.copy(isLegendEditorOpen = false)) }
            is PatternEditorContract.Intent.AddSymbol -> addSymbol(intent)
            is PatternEditorContract.Intent.DeleteSymbol -> deleteSymbol(intent.code)
            is PatternEditorContract.Intent.EditOperation -> _state.update { it.copy(ui = it.ui.copy(editingOperation = intent.operation)) }
            is PatternEditorContract.Intent.EditRow -> _state.update { it.copy(ui = it.ui.copy(editingRow = intent.row)) }
            is PatternEditorContract.Intent.DismissEditor -> _state.update { it.copy(ui = it.ui.copy(editingOperation = null, editingRow = null)) }
            is PatternEditorContract.Intent.UpdateOperation -> updateOperation(intent)
            is PatternEditorContract.Intent.DeleteOperationFromDialog -> deleteOperationFromDialog(intent.operation)
            is PatternEditorContract.Intent.UpdateRowNumber -> updateRowNumber(intent)
            is PatternEditorContract.Intent.DeleteRow -> deleteRow(intent.rowId)
        }
    }

    private fun observePatternSummaries() {
        summariesJob?.cancel()
        summariesJob = viewModelScope.launch {
            patternUseCases.getAllPatternSummaries(Unit).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update { it.copy(data = it.data.copy(availablePatterns = result.data)) }
                        if (_state.value.data.currentPatternId == null && result.data.isNotEmpty()) {
                            val savedId = platformDeps.savedStateHandle.get<Long>(KEY_PATTERN_ID)
                            if (savedId == null) {
                                loadPattern(result.data.first().id)
                            }
                        }
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to load pattern summaries")
                        _state.update { it.copy(data = it.data.copy(errorRes = R.string.pattern_editor_load_error)) }
                    }
                }
            }
        }
    }

    /**
     * Подписка на настройку видимости кнопки «Добавить» в LegendEditorSheet (7f-D-7).
     *
     * Источник — [SettingsRepository.legendAddButtonVisibleFlow] (DataStore).
     * При смене в SettingsScreen State обновится автоматически.
     * Кнопка всегда видна по умолчанию (default = true в SettingsKeys).
     */
    private fun observeLegendAddButtonSetting() {
        viewModelScope.launch {
            platformDeps.settingsRepository.legendAddButtonVisibleFlow.collect { visible ->
                _state.update { it.copy(ui = it.ui.copy(showLegendAddButton = visible)) }
            }

            // 05.11.2026: туториал перенесён с SharedPreferences на DataStore (P2-баг).
            platformDeps.settingsRepository.editTutorialShownFlow.collect { shown ->
                _state.update { it.copy(ui = it.ui.copy(showEditTutorial = !shown || it.ui.tutorialManuallyRequested)) }
            }
        }
    }

    /**
     * Смена активной схемы: сброс undo-стека и загрузка новой схемы.
     * Вынесено из [onIntent] ради detekt LongMethod (30/30 = предел).
     */
    private fun selectPattern(patternId: PatternId) {
        _state.update { it.copy(data = it.data.copy(lastOperations = emptyList())) }
        loadPattern(patternId)
    }

    private fun exportSvg(outputStream: java.io.OutputStream) {
        _state.value.data.pattern?.let { exportPatternSvg(it, outputStream, patternUseCases, _effect, viewModelScope) }
    }

    /**
     * Экспорт текущей схемы в PDF.
     *
     * @param outputStream Поток для записи PDF. Не закрывается здесь.
     */
    private fun exportPdf(outputStream: java.io.OutputStream) {
        _state.value.data.pattern?.let { exportPatternPdf(it, outputStream, patternUseCases, _effect, viewModelScope) }
    }

    private fun loadPattern(id: PatternId) {
        loadJob?.cancel()
        _state.update { it.copy(data = it.data.copy(isLoading = true, errorRes = null)) }
        loadJob = viewModelScope.launch {
            when (val result = patternUseCases.getPatternById(id)) {
                is Result.Success -> {
                    platformDeps.savedStateHandle[KEY_PATTERN_ID] = id.value
                    _state.update {
                        it.copy(data = it.data.copy(
                            pattern = result.data,
                            currentPatternId = id,
                            isLoading = false
                        ))
                    }
                }
                is Result.Error -> {
                    Timber.e(result.error.cause, "Failed to load pattern id=%d", id.value)
                    _state.update {
                        it.copy(data = it.data.copy(isLoading = false, errorRes = R.string.pattern_editor_load_error))
                    }
                }
            }
        }
    }

    private fun selectRow(rowId: PatternRowId) {
        platformDeps.savedStateHandle[KEY_SELECTED_ROW_ID] = rowId.value
        _state.update { it.copy(selection = it.selection.copy(selectedRowId = rowId)) }
    }

    private fun selectColumn(columnStart: ColumnStart) {
        platformDeps.savedStateHandle[KEY_SELECTED_COLUMN] = columnStart.value
        _state.update { it.copy(selection = it.selection.copy(selectedColumn = columnStart)) }
    }

    private fun selectSymbol(symbolCode: String) {
        platformDeps.savedStateHandle[KEY_SELECTED_SYMBOL] = symbolCode
        _state.update { it.copy(selection = it.selection.copy(selectedSymbolCode = symbolCode)) }
    }

    private fun toggleMode() {
        val next = !_state.value.selection.isStickyMode
        platformDeps.savedStateHandle[KEY_IS_STICKY_MODE] = next
        _state.update { it.copy(selection = it.selection.copy(isStickyMode = next)) }
    }

    private fun <T> executeWithSaving(
        block: suspend () -> Result<T>,
        onError: (AppError) -> Unit = { showError(it) },
        onSuccess: suspend (T) -> Unit
    ) {
        if (_state.value.data.isSaving) return
        _state.update { it.copy(data = it.data.copy(isSaving = true)) }
        viewModelScope.launch {
            try {
                when (val result = block()) {
                    is Result.Success -> onSuccess(result.data)
                    is Result.Error -> onError(result.error)
                }
            } finally {
                _state.update { it.copy(data = it.data.copy(isSaving = false)) }
            }
        }
    }

    private fun createPattern(params: CreatePatternParams) {
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to create pattern"); showError(e) },
            block = { patternUseCases.createPattern(params) },
            onSuccess = { id ->
                _state.update { it.copy(data = it.data.copy(lastOperations = emptyList())) }
                loadPattern(id)
            }
        )
    }

    /** Повторная попытка после ошибки загрузки. */
    private fun retry() {
        _state.update { it.copy(data = it.data.copy(errorRes = null)) }
        observePatternSummaries()
        val current = _state.value.data.currentPatternId
            ?: platformDeps.savedStateHandle.get<Long>(KEY_PATTERN_ID)?.let { PatternId.createOrNull(it) }
            ?: return
        loadPattern(current)
    }

    private fun addOperation(intent: PatternEditorContract.Intent.AddOperation) {
        val context = resolveAddContext(_state.value, intent) 
            ?: return showError(R.string.pattern_editor_add_error)
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to add operation"); showError(e) },
            block = { operationUseCases.addPatternOperation(context.params) },
            onSuccess = { op ->
                pushToUndoStack(op)
                _effect.send(PatternEditorContract.Effect.OperationAdded(op))
                loadPattern(context.patternId)
            }
        )
    }

    private fun pushToUndoStack(operation: PatternOperation) {
        _state.update {
            it.copy(data = it.data.copy(lastOperations = (it.data.lastOperations + operation).takeLast(MAX_UNDO)))
        }
    }

    private fun undoLastOperation() {
        val last = _state.value.data.lastOperations.lastOrNull() ?: return
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to undo operation"); showError(e) },
            block = { operationUseCases.deleteOperation(DeleteOperationParams(last.patternRowId, last.columnStart)) },
            onSuccess = {
                _state.update { it.copy(data = it.data.copy(lastOperations = it.data.lastOperations.dropLast(1))) }
                _state.value.data.currentPatternId?.let { loadPattern(it) }
            }
        )
    }

    private fun updateOperation(intent: PatternEditorContract.Intent.UpdateOperation) {
        val context = resolveUpdateContext(_state.value, intent) 
            ?: return showError(R.string.pattern_editor_operation_not_found)
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to update operation"); showError(e) },
            block = { operationUseCases.updatePatternOperation(context.params) },
            onSuccess = {
                _state.update { it.copy(ui = it.ui.copy(editingOperation = null)) }
                loadPattern(context.patternId)
            }
        )
    }

    private fun deleteOperationFromDialog(operation: PatternOperation) {
        val patternId = _state.value.data.currentPatternId ?: return
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to delete operation"); showError(e) },
            block = { operationUseCases.deleteOperation(DeleteOperationParams(operation.patternRowId, operation.columnStart)) },
            onSuccess = {
                _state.update { it.copy(ui = it.ui.copy(editingOperation = null)) }
                loadPattern(patternId)
            }
        )
    }

    private fun updateRowNumber(intent: PatternEditorContract.Intent.UpdateRowNumber) {
        val patternId = _state.value.data.currentPatternId ?: return
        val params = UpdatePatternRowNumberParams(patternId, intent.rowId, intent.newRowNumber)
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to update row number"); showError(e) },
            block = { rowUseCases.updatePatternRowNumber(params) },
            onSuccess = {
                _state.update { it.copy(ui = it.ui.copy(editingRow = null)) }
                loadPattern(patternId)
            }
        )
    }

    private fun deleteRow(rowId: PatternRowId) {
        val patternId = _state.value.data.currentPatternId ?: return
        val params = DeletePatternRowParams(patternId, rowId)
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to delete row"); showError(e) },
            block = { rowUseCases.deletePatternRow(params) },
            onSuccess = {
                _state.update { it.copy(ui = it.ui.copy(editingRow = null)) }
                loadPattern(patternId)
            }
        )
    }

    private fun addSymbol(intent: PatternEditorContract.Intent.AddSymbol) {
        val patternId = _state.value.data.currentPatternId ?: return
        val params = AddSymbolParams(
            patternId = patternId,
            code = intent.code,
            displayName = intent.displayName,
            textGlyph = intent.textGlyph,
            defaultColumnSpan = intent.defaultColumnSpan
        )
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to add symbol"); showError(e) },
            block = { symbolUseCases.addSymbol(params) },
            onSuccess = { loadPattern(patternId) }
        )
    }

    private fun deleteSymbol(code: String) {
        val patternId = _state.value.data.currentPatternId ?: return
        val params = DeleteSymbolParams(patternId = patternId, code = code)
        executeWithSaving(
            onError = { e -> Timber.e(e.cause, "Failed to delete symbol"); showError(e) },
            block = { symbolUseCases.deleteSymbol(params) },
            onSuccess = { loadPattern(patternId) }
        )
    }

    private fun showError(@StringRes res: Int) {
        viewModelScope.launch { _effect.send(PatternEditorContract.Effect.ShowError(res)) }
    }

    private fun showError(error: AppError) {
        viewModelScope.launch { _effect.send(PatternEditorContract.Effect.ShowError(platformDeps.errorStringProvider.getStringRes(error.code))) }
    }

    private companion object {
        const val KEY_PATTERN_ID = "patternId"
        const val KEY_SELECTED_ROW_ID = "selectedRowId"
        const val KEY_SELECTED_COLUMN = "selectedColumn"
        const val KEY_SELECTED_SYMBOL = "selectedSymbolCode"
        const val KEY_IS_STICKY_MODE = "isStickyMode"
        const val MAX_UNDO = 25
    }

    /**
     * Закрывает туториал и сохраняет флаг в DataStore.
     * Если сохранение упало — туториал всё равно скрыт в текущей сессии
     * (UI не должен зависать), ошибка логируется в SettingsRepositoryImpl.
     */
    /**
     * Открывает туториал вручную (иконка `?` в TopAppBar).
     *
     * Устанавливает tutorialManuallyRequested = true, чтобы collect из
     * editTutorialShownFlow не закрыл диалог следующим эмитом (например,
     * если в DataStore уже true от предыдущего закрытия).
     */
    private fun showEditTutorialManually() {
        _state.update { it.copy(ui = it.ui.copy(showEditTutorial = true, tutorialManuallyRequested = true)) }
    }

    private fun dismissEditTutorial() {
        _state.update { it.copy(ui = it.ui.copy(showEditTutorial = false, tutorialManuallyRequested = false)) }
        viewModelScope.launch {
            platformDeps.settingsRepository.setEditTutorialShown(true)
        }
    }
}
