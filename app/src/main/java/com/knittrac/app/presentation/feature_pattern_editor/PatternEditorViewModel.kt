package com.knittrac.app.presentation.feature_pattern_editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.R
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.SymbolLocalizer
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.findRowById
import com.knittrac.app.domain.entity.findSymbolByCode
import com.knittrac.app.domain.model.AddPatternOperationParams
import com.knittrac.app.domain.service.AddPatternOperationUseCase
import com.knittrac.app.domain.service.GetAllPatternSummariesService
import com.knittrac.app.domain.service.GetPatternByIdService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel экрана Pattern Editor (7f-B).
 *
 * Загружает список схем (для селектора), выбранную схему (по id), оркестрирует
 * добавление операций. Валидация (legend, repeat, overlap) — в
 * [com.knittrac.app.domain.service.AddPatternOperationService].
 *
 * State management:
 * - `patternId`, `selectedRowId`, `selectedColumn`, `selectedSymbolCode`,
 *   `isStickyMode` — через [SavedStateHandle] (переживают process death).
 * - `zoom`, `pan` — в Composable через `rememberSaveable` (UI-only, не VM).
 *
 * @see PatternEditorContract
 */
@HiltViewModel
class PatternEditorViewModel @Inject constructor(
    private val getPatternByIdService: GetPatternByIdService,
    private val getAllPatternSummariesService: GetAllPatternSummariesService,
    private val addPatternOperationUseCase: AddPatternOperationUseCase,
    private val symbolLocalizer: SymbolLocalizer,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        PatternEditorContract.State(
            selectedRowId = savedStateHandle.get<Long>(KEY_SELECTED_ROW_ID)
                ?.let { PatternRowId.createOrNull(it) },
            selectedColumn = savedStateHandle.get<Int>(KEY_SELECTED_COLUMN)
                ?.let { ColumnStart.createOrNull(it) },
            selectedSymbolCode = savedStateHandle.get<String>(KEY_SELECTED_SYMBOL),
            isStickyMode = savedStateHandle.get<Boolean>(KEY_IS_STICKY_MODE) ?: true
        )
    )
    val state: StateFlow<PatternEditorContract.State> = _state.asStateFlow()

    private val _effect = Channel<PatternEditorContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observePatternSummaries()
        savedStateHandle.get<Long>(KEY_PATTERN_ID)?.let { raw ->
            PatternId.createOrNull(raw)?.let { loadPattern(it) }
        }
    }

    /**
     * Единая точка входа для действий UI.
     */
    fun onIntent(intent: PatternEditorContract.Intent) {
        when (intent) {
            is PatternEditorContract.Intent.LoadPattern -> loadPattern(intent.patternId)
            is PatternEditorContract.Intent.SelectPattern -> loadPattern(intent.patternId)
            is PatternEditorContract.Intent.SelectRow -> selectRow(intent.rowId)
            is PatternEditorContract.Intent.SelectColumn -> selectColumn(intent.columnStart)
            is PatternEditorContract.Intent.SelectSymbol -> selectSymbol(intent.symbolCode)
            is PatternEditorContract.Intent.ToggleMode -> toggleMode()
            is PatternEditorContract.Intent.AddOperation -> addOperation(intent)
            is PatternEditorContract.Intent.Retry -> retry()
        }
    }

    private fun observePatternSummaries() {
        viewModelScope.launch {
            getAllPatternSummariesService(Unit).collect { result ->
                when (result) {
                    is Result.Success -> _state.update { it.copy(availablePatterns = result.data) }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to load pattern summaries")
                        _state.update { it.copy(errorRes = R.string.pattern_editor_load_error) }
                    }
                }
            }
        }
    }

    private fun loadPattern(id: PatternId) {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = getPatternByIdService(id)) {
                is Result.Success -> {
                    savedStateHandle[KEY_PATTERN_ID] = id.value
                    _state.update {
                        it.copy(
                            pattern = result.data,
                            currentPatternId = id,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    Timber.e(result.error.cause, "Failed to load pattern id=%d", id.value)
                    _state.update {
                        it.copy(isLoading = false, errorRes = R.string.pattern_editor_load_error)
                    }
                }
            }
        }
    }

    private fun selectRow(rowId: PatternRowId) {
        savedStateHandle[KEY_SELECTED_ROW_ID] = rowId.value
        _state.update { it.copy(selectedRowId = rowId) }
    }

    private fun selectColumn(columnStart: ColumnStart) {
        savedStateHandle[KEY_SELECTED_COLUMN] = columnStart.value
        _state.update { it.copy(selectedColumn = columnStart) }
    }

    private fun selectSymbol(symbolCode: String) {
        savedStateHandle[KEY_SELECTED_SYMBOL] = symbolCode
        _state.update { it.copy(selectedSymbolCode = symbolCode) }
    }

    private fun toggleMode() {
        val next = !_state.value.isStickyMode
        savedStateHandle[KEY_IS_STICKY_MODE] = next
        _state.update { it.copy(isStickyMode = next) }
    }

    private fun retry() {
        val current = _state.value.currentPatternId ?: return
        loadPattern(current)
    }

    private fun addOperation(intent: PatternEditorContract.Intent.AddOperation) {
        if (_state.value.isSaving) return
        val context = resolveAddContext(intent) ?: return runError(R.string.pattern_editor_add_error)
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                executeAdd(context)
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Валидирует вход и строит [AddContext]. null — вход невалиден
     * (нет pattern / patternId / row / symbol).
     */
    private fun resolveAddContext(
        intent: PatternEditorContract.Intent.AddOperation
    ): AddContext? {
        val state = _state.value
        val pattern = state.pattern
        val patternId = state.currentPatternId
        val symbolCode = state.selectedSymbolCode
        val row = pattern?.findRowById(intent.rowId)
        val symbol = if (symbolCode != null) pattern?.findSymbolByCode(symbolCode) else null
        return when {
            pattern == null || patternId == null || row == null || symbol == null -> null
            else -> AddContext(
                patternId = patternId,
                params = AddPatternOperationParams(
                    patternId = patternId,
                    row = row,
                    columnStart = intent.columnStart,
                    symbol = symbol
                )
            )
        }
    }

    private suspend fun executeAdd(context: AddContext) {
        when (val result = addPatternOperationUseCase(context.params)) {
            is Result.Success -> {
                _effect.send(PatternEditorContract.Effect.OperationAdded(result.data))
                // Reload: VM — единый source of truth, не мутируем Domain локально.
                loadPattern(context.patternId)
            }
            is Result.Error -> {
                Timber.e(result.error.cause, "Failed to add operation")
                _effect.send(
                    PatternEditorContract.Effect.ShowError(R.string.pattern_editor_add_error)
                )
            }
        }
    }

    private data class AddContext(
        val patternId: PatternId,
        val params: AddPatternOperationParams
    )

    private fun runError(@androidx.annotation.StringRes res: Int) {
        viewModelScope.launch { _effect.send(PatternEditorContract.Effect.ShowError(res)) }
    }

    /**
     * Символ для отображения в палитре. Пока — `textGlyph`; при расширении
     * (локализация `displayName`) — обновить [SymbolLocalizer], не VM.
     */
    @Suppress("unused")
    private fun localizeSymbol(code: String): String {
        val symbol = _state.value.pattern?.findSymbolByCode(code) ?: return code
        return symbolLocalizer.localize(symbol)
    }

    private companion object {
        const val KEY_PATTERN_ID = "patternId"
        const val KEY_SELECTED_ROW_ID = "selectedRowId"
        const val KEY_SELECTED_COLUMN = "selectedColumn"
        const val KEY_SELECTED_SYMBOL = "selectedSymbolCode"
        const val KEY_IS_STICKY_MODE = "isStickyMode"
    }
}
