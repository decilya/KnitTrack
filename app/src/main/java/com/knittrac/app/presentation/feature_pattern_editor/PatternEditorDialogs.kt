package com.knittrac.app.presentation.feature_pattern_editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.knittrac.app.R
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.service.CreatePatternParams

internal data class PendingCell(
    val rowId: PatternRowId,
    val column: ColumnStart
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SymbolPickerBottomSheet(
    cell: PendingCell,
    legend: Set<Symbol>,
    onSymbolSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        SymbolPickerSheet(symbols = legend) { code ->
            onSymbolSelected(code)
        }
    }
}

/**
 * Диалог создания новой схемы.
 *
 * Позволяет ввести имя, количество рядов, выбрать технику и направление.
 *
 * @param onDismiss Колбэк отмены (закрытие диалога).
 * @param onCreate Колбэк подтверждения с валидными [CreatePatternParams].
 */
@Composable
internal fun CreatePatternDialog(
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

/**
 * Нижний шит (bottom sheet) с сеткой символов для выбора в sheet-режиме.
 *
 * @param symbols Множество доступных символов (легенда схемы).
 * @param onSymbolSelected Колбэк выбора символа (передаёт code).
 */
@Composable
internal fun SymbolPickerSheet(
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
