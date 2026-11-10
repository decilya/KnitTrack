package com.knittrac.app.presentation.feature_pattern_editor

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.presentation.feature_pattern_editor.PatternEditorContract.Intent
import com.knittrac.app.presentation.feature_pattern_editor.PatternEditorContract.State

@Composable
internal fun PatternCanvas(
    pattern: Pattern,
    state: State,
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
