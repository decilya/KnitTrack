package com.knittrac.app.presentation.feature_pattern_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.R
import com.knittrac.app.core.common.ErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.findRowById
import com.knittrac.app.domain.entity.findSymbolByCode
import com.knittrac.app.domain.model.AddPatternOperationParams
import com.knittrac.app.domain.service.CreatePatternParams
import com.knittrac.app.domain.service.DeleteOperationParams
import com.knittrac.app.domain.model.UpdatePatternOperationParams
import com.knittrac.app.domain.service.DeletePatternRowParams
import com.knittrac.app.domain.service.UpdatePatternRowNumberParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.knittrac.app.domain.service.AddSymbolParams
import com.knittrac.app.domain.service.DeleteSymbolParams

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
@Suppress("LargeClass") // TODO(P2): декомпозиция после 7f-D-8 (см. TECHDEBT)
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
            selectedRowId = platformDeps.savedStateHandle.get<Long>(KEY_SELECTED_ROW_ID)
                ?.let { PatternRowId.createOrNull(it) },
            selectedColumn = platformDeps.savedStateHandle.get<Int>(KEY_SELECTED_COLUMN)
                ?.let { ColumnStart.createOrNull(it) },
            selectedSymbolCode = platformDeps.savedStateHandle.get<String>(KEY_SELECTED_SYMBOL),
            isStickyMode = platformDeps.savedStateHandle.get<Boolean>(KEY_IS_STICKY_MODE) ?: true
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
            is PatternEditorContract.Intent.OpenLegendEditor -> _state.update { it.copy(isLegendEditorOpen = true) }
            is PatternEditorContract.Intent.CloseLegendEditor -> _state.update { it.copy(isLegendEditorOpen = false) }
            is PatternEditorContract.Intent.AddSymbol -> addSymbol(intent)
            is PatternEditorContract.Intent.DeleteSymbol -> deleteSymbol(intent.code)
            is PatternEditorContract.Intent.EditOperation -> _state.update { it.copy(editingOperation = intent.operation) }
            is PatternEditorContract.Intent.EditRow -> _state.update { it.copy(editingRow = intent.row) }
            is PatternEditorContract.Intent.DismissEditor -> _state.update { it.copy(editingOperation = null, editingRow = null) }
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
                        _state.update { it.copy(availablePatterns = result.data) }
                        if (_state.value.currentPatternId == null && result.data.isNotEmpty()) {
                            val savedId = platformDeps.savedStateHandle.get<Long>(KEY_PATTERN_ID)
                            if (savedId == null) {
                                loadPattern(result.data.first().id)
                            }
                        }
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to load pattern summaries")
                        _state.update { it.copy(errorRes = R.string.pattern_editor_load_error) }
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
                _state.update { it.copy(showLegendAddButton = visible) }
            }

            // 05.11.2026: туториал перенесён с SharedPreferences на DataStore (P2-баг).
            platformDeps.settingsRepository.editTutorialShownFlow.collect { shown ->
                _state.update { it.copy(showEditTutorial = !shown || it.tutorialManuallyRequested) }
            }
        }
    }

    /**
     * Смена активной схемы: сброс undo-стека и загрузка новой схемы.
     * Вынесено из [onIntent] ради detekt LongMethod (30/30 = предел).
     */
    private fun selectPattern(patternId: PatternId) {
        _state.update { it.copy(lastOperations = emptyList()) }
        loadPattern(patternId)
    }

    private fun loadPattern(id: PatternId) {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, errorRes = null) }
        loadJob = viewModelScope.launch {
            when (val result = patternUseCases.getPatternById(id)) {
                is Result.Success -> {
                    platformDeps.savedStateHandle[KEY_PATTERN_ID] = id.value
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
        platformDeps.savedStateHandle[KEY_SELECTED_ROW_ID] = rowId.value
        _state.update { it.copy(selectedRowId = rowId) }
    }

    private fun selectColumn(columnStart: ColumnStart) {
        platformDeps.savedStateHandle[KEY_SELECTED_COLUMN] = columnStart.value
        _state.update { it.copy(selectedColumn = columnStart) }
    }

    private fun selectSymbol(symbolCode: String) {
        platformDeps.savedStateHandle[KEY_SELECTED_SYMBOL] = symbolCode
        _state.update { it.copy(selectedSymbolCode = symbolCode) }
    }

    private fun toggleMode() {
        val next = !_state.value.isStickyMode
        platformDeps.savedStateHandle[KEY_IS_STICKY_MODE] = next
        _state.update { it.copy(isStickyMode = next) }
    }

    private fun createPattern(params: CreatePatternParams) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                when (val result = patternUseCases.createPattern(params)) {
                    is Result.Success -> {
                        _state.update { it.copy(lastOperations = emptyList()) }
                        loadPattern(result.data)
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to create pattern")
                        _effect.send(
                            PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code))
                        )
                    }
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Повторная попытка после ошибки.
     *
     * Перезапускает подписку на список схем (могла упасть), сбрасывает
     * errorRes, и перезагружает текущую схему, если она выбрана. Если
     * currentPatternId == null (первая загрузка упала), пытается загрузить
     * схему из savedStateHandle или первую доступную.
     */
    private fun retry() {
        _state.update { it.copy(errorRes = null) }
        observePatternSummaries()

        val current = _state.value.currentPatternId
        if (current != null) {
            loadPattern(current)
            return
        }

        val savedId = platformDeps.savedStateHandle.get<Long>(KEY_PATTERN_ID)
            ?.let { PatternId.createOrNull(it) }
        if (savedId != null) {
            loadPattern(savedId)
            return
        }

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
        val symbol = symbolCode?.let { pattern?.findSymbolByCode(it) }
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
        when (val result = operationUseCases.addPatternOperation(context.params)) {
            is Result.Success -> {
                pushToUndoStack(result.data)
                _effect.send(PatternEditorContract.Effect.OperationAdded(result.data))
                loadPattern(context.patternId)
            }
            is Result.Error -> {
                Timber.e(result.error.cause, "Failed to add operation")
                _effect.send(
                    PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code))
                )
            }
        }
    }

    private fun pushToUndoStack(operation: PatternOperation) {
        _state.update {
            it.copy(lastOperations = (it.lastOperations + operation).takeLast(MAX_UNDO))
        }
    }

    private fun undoLastOperation() {
        if (_state.value.isSaving) return
        val last = _state.value.lastOperations.lastOrNull() ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                when (val result = operationUseCases.deleteOperation(
                    DeleteOperationParams(last.patternRowId, last.columnStart)
                )) {
                    is Result.Success -> {
                        _state.update { it.copy(lastOperations = it.lastOperations.dropLast(1)) }
                        val currentId = _state.value.currentPatternId
                        if (currentId != null) loadPattern(currentId)
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to undo operation")
                        _effect.send(
                            PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code))
                        )
                    }
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }


    private fun updateOperation(intent: PatternEditorContract.Intent.UpdateOperation) {
        if (_state.value.isSaving) return
        val context = resolveUpdateContext(intent) ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                executeUpdate(context)
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Валидирует вход и строит [UpdateContext]. null — вход невалиден
     * (нет pattern / patternId).
     */
    private fun resolveUpdateContext(
        intent: PatternEditorContract.Intent.UpdateOperation
    ): UpdateContext? {
        val pattern = _state.value.pattern
        val patternId = _state.value.currentPatternId
        if (pattern == null || patternId == null) return null
        return buildUpdateContext(pattern, patternId, intent)
    }

    /**
     * Ищет символ в легенде и строит [UpdateContext].
     * null — символ не найден в легенде (с ShowError).
     */
    private fun buildUpdateContext(
        pattern: Pattern,
        patternId: PatternId,
        intent: PatternEditorContract.Intent.UpdateOperation
    ): UpdateContext? {
        val newSymbol = pattern.findSymbolByCode(intent.newSymbolCode)
        if (newSymbol == null) {
            runError(R.string.pattern_editor_operation_not_found)
            return null
        }
        return UpdateContext(
            patternId = patternId,
            params = UpdatePatternOperationParams(
                patternId = patternId,
                rowId = intent.oldOperation.patternRowId,
                columnStart = intent.oldOperation.columnStart,
                symbol = newSymbol,
                backSymbol = intent.oldOperation.backSymbol,
                colorHex = intent.newColorHex,
                overrideSpan = intent.newOverrideSpan,
                repeatCode = intent.oldOperation.repeatCode
            )
        )
    }

    private suspend fun executeUpdate(context: UpdateContext) {
        when (val result = operationUseCases.updatePatternOperation(context.params)) {
            is Result.Success -> {
                _state.update { it.copy(editingOperation = null) }
                loadPattern(context.patternId)
            }
            is Result.Error -> {
                Timber.e(result.error.cause, "Failed to update operation")
                _effect.send(
                    PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code))
                )
            }
        }
    }

    private fun deleteOperationFromDialog(operation: PatternOperation) {
        if (_state.value.isSaving) return
        val patternId = _state.value.currentPatternId ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                when (val result = operationUseCases.deleteOperation(DeleteOperationParams(operation.patternRowId, operation.columnStart))) {
                    is Result.Success -> {
                        _state.update { it.copy(editingOperation = null) }
                        loadPattern(patternId)
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to delete operation")
                        _effect.send(PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code)))
                    }
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun updateRowNumber(intent: PatternEditorContract.Intent.UpdateRowNumber) {
        if (_state.value.isSaving) return
        val patternId = _state.value.currentPatternId ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val params = UpdatePatternRowNumberParams(patternId, intent.rowId, intent.newRowNumber)
                when (val result = rowUseCases.updatePatternRowNumber(params)) {
                    is Result.Success -> {
                        _state.update { it.copy(editingRow = null) }
                        loadPattern(patternId)
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to update row number")
                        _effect.send(PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code)))
                    }
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun deleteRow(rowId: PatternRowId) {
        if (_state.value.isSaving) return
        val patternId = _state.value.currentPatternId ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val params = DeletePatternRowParams(patternId, rowId)
                when (val result = rowUseCases.deletePatternRow(params)) {
                    is Result.Success -> {
                        _state.update { it.copy(editingRow = null) }
                        loadPattern(patternId)
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to delete row")
                        _effect.send(PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code)))
                    }
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Добавляет кастомный символ в легенду схемы (7f-D-7).
     *
     * Guard [State.isSaving] — от double-tap. Делегирует в
     * [PatternEditorSymbolUseCases.addSymbol] (AddSymbolService). После успеха
     * перезагружает схему — легенда обновляется на Canvas и в палитре.
     *
     * @param intent Параметры: code, displayName, textGlyph, defaultColumnSpan.
     */
    private fun addSymbol(intent: PatternEditorContract.Intent.AddSymbol) {
        if (_state.value.isSaving) return
        val patternId = _state.value.currentPatternId ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val params = AddSymbolParams(
                    patternId = patternId,
                    code = intent.code,
                    displayName = intent.displayName,
                    textGlyph = intent.textGlyph,
                    defaultColumnSpan = intent.defaultColumnSpan
                )
                when (val result = symbolUseCases.addSymbol(params)) {
                    // Успех — перезагружаем схему, чтобы legend обновилась.
                    is Result.Success -> loadPattern(patternId)
                    // ValidationError (код занят) или DatabaseError — показываем snackbar.
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to add symbol")
                        _effect.send(
                            PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code))
                        )
                    }
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Удаляет кастомный символ из легенды схемы (7f-D-7).
     *
     * Guard [State.isSaving]. Делегирует в [PatternEditorSymbolUseCases.deleteSymbol]
     * (DeleteSymbolService). Валидация:
     * - Системный символ (ownerPatternId = null) → ValidationError («Нельзя удалить системный»).
     * - Символ используется в операциях (FK RESTRICT) → ValidationError от репозитория.
     *
     * @param code Код символа (PK в symbols).
     */
    private fun deleteSymbol(code: String) {
        if (_state.value.isSaving) return
        val patternId = _state.value.currentPatternId ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val params = DeleteSymbolParams(patternId = patternId, code = code)
                when (val result = symbolUseCases.deleteSymbol(params)) {
                    // Успех — перезагружаем схему (легенда обновится).
                    is Result.Success -> loadPattern(patternId)
                    // ValidationError или DatabaseError — показываем snackbar.
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to delete symbol")
                        _effect.send(
                            PatternEditorContract.Effect.ShowError(ErrorMapper.map(result.error.code))
                        )
                    }
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private data class UpdateContext(
        val patternId: PatternId,
        val params: UpdatePatternOperationParams
    )

    private data class AddContext(
        val patternId: PatternId,
        val params: AddPatternOperationParams
    )

    private fun runError(@androidx.annotation.StringRes res: Int) {
        viewModelScope.launch { _effect.send(PatternEditorContract.Effect.ShowError(res)) }
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
        _state.update { it.copy(showEditTutorial = true, tutorialManuallyRequested = true) }
    }

    private fun dismissEditTutorial() {
        _state.update { it.copy(showEditTutorial = false, tutorialManuallyRequested = false) }
        viewModelScope.launch {
            platformDeps.settingsRepository.setEditTutorialShown(true)
        }
    }
}
