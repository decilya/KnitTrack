package com.knittrac.app.presentation.feature_pattern_editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.knittrac.app.R
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.presentation.feature_pattern_editor.PatternEditorContract.Intent
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Тело экрана Pattern Editor.
 *
 * Содержит вертикальный стек из трёх элементов:
 * 1. Горизонтальный список рядов (LazyRow с FilterChip для выбора ряда).
 * 2. Canvas с паттерном (PatternCanvas).
 * 3. Сетка символов (LazyVerticalGrid) в режиме sticky (если легенда не пуста).
 *
 * D&D reorder (15.11.2026): ряды можно перетаскивать за иконку-ручку.
 *
 * @param pattern Схема для отображения.
 * @param state Состояние экрана (выбранный ряд, символ, режим sticky).
 * @param callbacks Колбэки для передачи событий в ViewModel.
 * @param modifier Модификатор для внешнего позиционирования (например, weight).
 */
@Composable
internal fun PatternEditorBody(
    pattern: Pattern,
    state: PatternEditorContract.State,
    callbacks: PatternEditorCallbacks,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val rowIdValue = from.key as? Long ?: return@rememberReorderableLazyListState
        val newRowNumber = RowNumber.createOrNull(to.index + 1) ?: return@rememberReorderableLazyListState
        callbacks.onIntent(Intent.UpdateRowNumber(PatternRowId.createOrDefault(rowIdValue), newRowNumber))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(modifier.fillMaxSize()) {
        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            items(pattern.rows, key = { it.id.value }) { row ->
                ReorderableItem(reorderableLazyListState, key = row.id.value) { isDragging ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = row.id == state.selectedRowId,
                            onClick = { callbacks.onIntent(Intent.SelectRow(row.id)) },
                            label = {
                                Text(stringResource(R.string.pattern_editor_row_prefix, row.rowNumber.value))
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder",
                            modifier = Modifier
                                .draggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                                .padding(end = 4.dp)
                        )
                    }
                }
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
