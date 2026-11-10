package com.knittrac.app.presentation.feature_pattern_editor

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knittrac.app.R
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.service.CreatePatternParams
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
        ModalBottomSheet(
            onDismissRequest = { pendingCell = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            SymbolPickerSheet(symbols = legend) { code ->
                viewModel.onIntent(Intent.SelectSymbol(code))
                viewModel.onIntent(Intent.AddOperation(cell.rowId, cell.column))
                pendingCell = null
            }
        }
    }
}

/**
 * Коллбэки экрана Pattern Editor (Parameter Object).
 *
 * ISP: 3 коллбэка сгруппированы в data class, чтобы уменьшить LongParameterList
 * на PatternEditorBody/PatternCanvas. SRP: одна ответственность — передача событий.
 */
/**
 * DTO для ожидающей ячейки (sheet-mode).
 * Явный DTO вместо Pair: читателю не нужно помнить, что first = rowId, second = column.
 */
private data class PendingCell(
    val rowId: PatternRowId,
    val column: ColumnStart
)

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

@Composable
private fun PatternCanvas(
    pattern: Pattern,
    state: PatternEditorContract.State,
    callbacks: PatternEditorCallbacks,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cellPx = with(density) { 32.dp.toPx() }
    val headerPx = with(density) { 40.dp.toPx() }
    var scale by rememberSaveable { mutableStateOf(1f) }
    var offset by rememberSaveable(stateSaver = offsetSaver) { mutableStateOf(Offset.Zero) }
    val stitchColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline
    val highlight = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
    val haptic = LocalHapticFeedback.current
    val maxColumns = maxColumnsOf(pattern)
    val textMeasurer = rememberTextMeasurer()
    val rowNumberStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 4f)
                    offset += pan
                }
            }
            .pointerInput(
                pattern,
                cellPx,
                headerPx,
                state.isStickyMode,
                state.selectedSymbolCode
            ) {
                detectTapGestures(
                    onLongPress = { tap ->
                        val localX = (tap.x - offset.x) / scale
                        val localY = (tap.y - offset.y) / scale
                        if (localX < headerPx || localY < 0f) return@detectTapGestures
                        val col = ((localX - headerPx) / cellPx).toInt()
                        val rowIndex = (localY / cellPx).toInt()
                        val row = pattern.rows.getOrNull(rowIndex) ?: return@detectTapGestures
                        if (col !in 0 until maxColumns) return@detectTapGestures
                        val column = ColumnStart.create(col)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        callbacks.onCellLongPress(row.id, column)
                    },
                    onTap = { tap ->
                        val localX = (tap.x - offset.x) / scale
                        val localY = (tap.y - offset.y) / scale
                        if (localX < headerPx) return@detectTapGestures
                        if (localY < 0f) return@detectTapGestures
                        val col = ((localX - headerPx) / cellPx).toInt()
                        val rowIndex = (localY / cellPx).toInt()
                        val row = pattern.rows.getOrNull(rowIndex) ?: return@detectTapGestures
                        if (col !in 0 until maxColumns) return@detectTapGestures
                        val column = ColumnStart.create(col)
                        callbacks.onIntent(Intent.SelectRow(row.id))
                        callbacks.onIntent(Intent.SelectColumn(column))
                        if (state.isStickyMode) {
                            if (state.selectedSymbolCode != null) {
                                callbacks.onIntent(Intent.AddOperation(row.id, column))
                            }
                        } else {
                            callbacks.onCellTap(row.id, column)
                        }
                    }
                )
            }
    ) {
        translate(left = offset.x, top = offset.y) {
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
                val width = headerPx + maxColumns * cellPx
                val height = pattern.rows.size * cellPx
                for (rowIndex in 0..pattern.rows.size) {
                    val y = rowIndex * cellPx
                    drawLine(gridColor, Offset(0f, y), Offset(width, y))
                }
                for (column in 0..maxColumns) {
                    val x = headerPx + column * cellPx
                    drawLine(gridColor, Offset(x, 0f), Offset(x, height))
                }
                pattern.rows.forEachIndexed { rowIndex, row ->
                    val layout = textMeasurer.measure(
                        text = row.rowNumber.value.toString(),
                        style = rowNumberStyle
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = (headerPx - layout.size.width) / 2f,
                            y = rowIndex * cellPx + (cellPx - layout.size.height) / 2f
                        )
                    )
                }
                pattern.rows.forEachIndexed { rowIndex, row ->
                    row.operations.forEach { operation ->
                        drawRect(
                            color = colorOrDefault(operation.colorHex, stitchColor),
                            topLeft = Offset(
                                headerPx + operation.columnStart.value * cellPx,
                                rowIndex * cellPx
                            ),
                            size = Size(operation.effectiveColumnSpan.value * cellPx, cellPx)
                        )
                    }
                }
                val selectedRow = state.selectedRowId
                val selectedColumn = state.selectedColumn
                val selectedIndex = pattern.rows.indexOfFirst { it.id == selectedRow }
                if (selectedRow != null && selectedColumn != null && selectedIndex >= 0) {
                    drawRect(
                        color = highlight,
                        topLeft = Offset(
                            headerPx + selectedColumn.value * cellPx,
                            selectedIndex * cellPx
                        ),
                        size = Size(cellPx, cellPx)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePatternDialog(
    onDismiss: () -> Unit,
    onCreate: (CreatePatternParams) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rowCountText by remember { mutableStateOf("1") }
    var technique by remember { mutableStateOf(Technique.KNITTING) }
    var direction by remember { mutableStateOf(PatternDirection.LTR) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_editor_create_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.pattern_editor_name)) }
                )
                Text(stringResource(R.string.pattern_editor_row_count))
                OutlinedTextField(
                    value = rowCountText,
                    onValueChange = { rowCountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Technique.entries.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = technique == item, onClick = { technique = item })
                        Text(item.name)
                    }
                }
                PatternDirection.entries.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = direction == item, onClick = { direction = item })
                        Text(item.name)
                    }
                }
                if (error) Text(stringResource(R.string.pattern_editor_create_error))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val params = parseCreateInput(name, rowCountText, technique, direction)
                if (params == null) error = true else onCreate(params)
            }) { Text(stringResource(R.string.pattern_editor_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pattern_editor_cancel)) }
        }
    )
}

private fun parseCreateInput(
    name: String,
    rowCountText: String,
    technique: Technique,
    direction: PatternDirection
): CreatePatternParams? {
    val patternName = PatternName.createOrNull(name.trim())
    val rowCount = rowCountText.toIntOrNull()?.takeIf { it >= 1 }?.let { RowCount.createOrNull(it) }
    return if (patternName != null && rowCount != null) {
        CreatePatternParams(patternName, technique, direction, rowCount)
    } else {
        null
    }
}

@Composable
private fun SymbolPickerSheet(
    symbols: Set<Symbol>,
    onSymbolSelected: (String) -> Unit
) {
    if (symbols.isEmpty()) {
        Text(
            text = stringResource(R.string.pattern_editor_no_symbols),
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(8.dp)
    ) {
        items(symbols.toList(), key = { it.code }) { symbol ->
            FilterChip(
                selected = false,
                onClick = { onSymbolSelected(symbol.code) },
                label = { Text(symbol.textGlyph) }
            )
        }
    }
}

internal fun maxColumnsOf(pattern: Pattern): Int {
    val occupied = pattern.rows.maxOfOrNull { row ->
        row.operations.maxOfOrNull { operation ->
            operation.columnStart.value + operation.effectiveColumnSpan.value
        } ?: 0
    } ?: 0
    return occupied.coerceAtLeast(10)
}

internal fun colorOrDefault(hex: String?, fallback: Color): Color =
    hex?.let { value ->
        try {
            Color(AndroidColor.parseColor(value))
        } catch (e: IllegalArgumentException) {
            fallback
        }
    } ?: fallback

private val offsetSaver = listSaver<Offset, Float>(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) }
)
