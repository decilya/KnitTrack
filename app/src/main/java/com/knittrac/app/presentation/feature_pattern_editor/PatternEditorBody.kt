package com.knittrac.app.presentation.feature_pattern_editor

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.knittrac.app.R
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.presentation.feature_pattern_editor.PatternEditorContract.Intent

/**
 * Тело экрана Pattern Editor.
 *
 * Содержит вертикальный стек из трёх элементов:
 * 1. Горизонтальный список рядов (LazyRow с FilterChip для выбора ряда).
 * 2. Canvas с паттерном (PatternCanvas).
 * 3. Сетка символов (LazyVerticalGrid) в режиме sticky (если легенда не пуста).
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
