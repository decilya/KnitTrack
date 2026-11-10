package com.knittrac.app.presentation.feature_pattern_editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knittrac.app.R
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.presentation.feature_pattern_editor.PatternEditorContract.Intent

/**
 * Экран редактора схемы: вся схема на Canvas, sticky-добавление операции.
 * Sheet и undo — 7f-B-4.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternEditorScreen(
    viewModel: PatternEditorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingCell by remember { mutableStateOf<PendingCell?>(null) }
    val pattern = state.pattern

    // 05.11.2026: источник — SettingsRepository.editTutorialShownFlow (DataStore),
    // не SharedPreferences (§25 Урок 27, §11.3).
    if (state.showEditTutorial) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(PatternEditorContract.Intent.DismissEditTutorial) },
            title = { Text(stringResource(R.string.pattern_editor_tutorial_title)) },
            text = { Text(stringResource(R.string.pattern_editor_tutorial_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(PatternEditorContract.Intent.DismissEditTutorial) }) {
                    Text(stringResource(R.string.pattern_editor_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pattern_editor_back)
                        )
                    }
                },
                title = {
                    Box {
                        Text(
                            text = pattern?.name?.value
                                ?: stringResource(R.string.pattern_editor_select_pattern),
                            modifier = Modifier.clickable { menuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            state.availablePatterns.forEach { summary ->
                                DropdownMenuItem(
                                    text = { Text(summary.name.value) },
                                    onClick = {
                                        viewModel.onIntent(Intent.SelectPattern(summary.id))
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(Intent.ShowEditTutorial) }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = stringResource(R.string.pattern_editor_tutorial_open)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.onIntent(Intent.UndoLastOperation) },
                        enabled = state.lastOperations.isNotEmpty() && !state.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.pattern_editor_undo)
                        )
                    }
                    IconButton(onClick = { viewModel.onIntent(Intent.ToggleMode) }) {
                        Icon(
                            imageVector = if (state.isStickyMode) {
                                Icons.Default.TouchApp
                            } else {
                                Icons.Default.AdsClick
                            },
                            contentDescription = stringResource(
                                if (state.isStickyMode) {
                                    R.string.pattern_editor_mode_sticky
                                } else {
                                    R.string.pattern_editor_mode_sheet
                                }
                            )
                        )
                    }
                    // 7f-D-7: открывает LegendEditorSheet (управление легендой).
                    // Иконка Palette — стандарт для «справочника символов» (M3).
                    IconButton(onClick = { viewModel.onIntent(Intent.OpenLegendEditor) }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = stringResource(R.string.pattern_editor_legend_open)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.padding(padding))
            pattern == null -> Column(Modifier.padding(padding).padding(16.dp)) {
                Text(stringResource(R.string.pattern_editor_select_pattern))
                Button(onClick = { showCreateDialog = true }) {
                    Text(stringResource(R.string.pattern_editor_create))
                }
            }
            pattern.rows.isEmpty() -> Text(
                text = stringResource(R.string.pattern_editor_no_rows),
                modifier = Modifier.padding(padding)
            )
            else -> PatternEditorBody(
                pattern = pattern,
                state = state,
                callbacks = PatternEditorCallbacks(
                    onIntent = viewModel::onIntent,
                    onCellTap = { rowId, column -> pendingCell = PendingCell(rowId, column) },
                    onCellLongPress = { rowId, column ->
                        val row = pattern.rows.find { it.id == rowId }
                        val op = row?.operations?.find { it.columnStart == column }
                        when {
                            op != null -> viewModel.onIntent(Intent.EditOperation(op))
                            row != null -> viewModel.onIntent(Intent.EditRow(row))
                        }
                    }
                ),
                modifier = Modifier.padding(padding)
            )
        }
    }
    if (showCreateDialog) {
        CreatePatternDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { params ->
                viewModel.onIntent(PatternEditorContract.Intent.CreatePattern(params))
                showCreateDialog = false
            }
        )
    }
    val opToEdit = state.editingOperation
    if (opToEdit != null && pattern != null) {
        EditOperationDialog(
            operation = opToEdit,
            legend = pattern.legend,
            onDismiss = { viewModel.onIntent(Intent.DismissEditor) },
            onUpdate = { code, span, color ->
                viewModel.onIntent(Intent.UpdateOperation(opToEdit, code, span, color))
            },
            onDelete = { viewModel.onIntent(Intent.DeleteOperationFromDialog(opToEdit)) }
        )
    }

    val rowToEdit = state.editingRow
    if (rowToEdit != null) {
        RowEditorSheet(
            row = rowToEdit,
            onDismiss = { viewModel.onIntent(Intent.DismissEditor) },
            onMove = { newNum -> viewModel.onIntent(Intent.UpdateRowNumber(rowToEdit.id, newNum)) },
            onDelete = { viewModel.onIntent(Intent.DeleteRow(rowToEdit.id)) }
        )
    }

    // 7f-D-7: LegendEditorSheet — управление легендой.
    // Открывается по IconButton Palette в TopAppBar. Partial detent по умолчанию.
    if (state.isLegendEditorOpen && pattern != null) {
        LegendEditorSheet(
            pattern = pattern,
            showAddButton = state.showLegendAddButton,
            callbacks = LegendEditorCallbacks(
                onAddSymbol = { code, name, glyph, span ->
                    viewModel.onIntent(Intent.AddSymbol(code, name, glyph, span))
                },
                onDeleteSymbol = { code ->
                    viewModel.onIntent(Intent.DeleteSymbol(code))
                },
                onDismiss = { viewModel.onIntent(Intent.CloseLegendEditor) }
            )
        )
    }

    val cell = pendingCell
    val legend = pattern?.legend
    if (cell != null && legend != null) {
        val cell = pendingCell
        val legend = pattern?.legend
        if (cell != null && legend != null) {
            SymbolPickerBottomSheet(
                cell = cell,
                legend = legend,
                onSymbolSelected = { code ->
                    viewModel.onIntent(Intent.SelectSymbol(code))
                    viewModel.onIntent(Intent.AddOperation(cell.rowId, cell.column))
                    pendingCell = null
                },
                onDismiss = { pendingCell = null }
            )
        }
    }
}


@Composable
private fun PatternEditorBody(
    pattern: Pattern,
    state: PatternEditorContract.State,
    callbacks: PatternEditorCallbacks,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            items(pattern.rows, key = { it.id.value }) { row ->
                FilterChip(
                    selected = row.id == state.selectedRowId,
                    onClick = { callbacks.onIntent(Intent.SelectRow(row.id)) },
                    label = {
                        Text(stringResource(R.string.pattern_editor_row_prefix, row.rowNumber.value))
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        PatternCanvas(
            pattern = pattern,
            state = state,
            callbacks = callbacks,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        if (state.isStickyMode && pattern.legend.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(8.dp)
            ) {
                items(pattern.legend.toList(), key = { it.code }) { symbol ->
                    FilterChip(
                        selected = symbol.code == state.selectedSymbolCode,
                        onClick = { callbacks.onIntent(Intent.SelectSymbol(symbol.code)) },
                        label = { Text(symbol.textGlyph) }
                    )
                }
            }
        }
    }
}

