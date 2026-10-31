package com.knittrac.app.presentation.feature_pattern_editor

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
    val pattern = state.pattern
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
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.padding(padding))
            pattern == null -> Text(
                text = stringResource(R.string.pattern_editor_select_pattern),
                modifier = Modifier.padding(padding)
            )
            pattern.rows.isEmpty() -> Text(
                text = stringResource(R.string.pattern_editor_no_rows),
                modifier = Modifier.padding(padding)
            )
            else -> PatternEditorBody(
                pattern = pattern,
                state = state,
                onIntent = viewModel::onIntent,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun PatternEditorBody(
    pattern: Pattern,
    state: PatternEditorContract.State,
    onIntent: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            items(pattern.rows, key = { it.id.value }) { row ->
                FilterChip(
                    selected = row.id == state.selectedRowId,
                    onClick = { onIntent(Intent.SelectRow(row.id)) },
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
            onIntent = onIntent,
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
                        onClick = { onIntent(Intent.SelectSymbol(symbol.code)) },
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
    onIntent: (Intent) -> Unit,
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
                detectTapGestures { tap ->
                    val localX = (tap.x - offset.x) / scale
                    val localY = (tap.y - offset.y) / scale
                    if (localX < headerPx) return@detectTapGestures
                    if (localY < 0f) return@detectTapGestures
                    val col = ((localX - headerPx) / cellPx).toInt()
                    val rowIndex = (localY / cellPx).toInt()
                    val row = pattern.rows.getOrNull(rowIndex) ?: return@detectTapGestures
                    if (col !in 0 until maxColumns) return@detectTapGestures
                    val column = ColumnStart.create(col)
                    onIntent(Intent.SelectRow(row.id))
                    onIntent(Intent.SelectColumn(column))
                    if (state.isStickyMode && state.selectedSymbolCode != null) {
                        onIntent(Intent.AddOperation(row.id, column))
                    }
                }
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
