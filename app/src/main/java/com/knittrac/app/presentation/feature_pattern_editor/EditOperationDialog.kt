package com.knittrac.app.presentation.feature_pattern_editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.knittrac.app.R
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol

/**
 * Диалог редактирования операции (7f-D-6).
 *
 * Позволяет сменить символ, HEX-цвет, span, удалить операцию.
 */
@Composable
fun EditOperationDialog(
    operation: PatternOperation,
    legend: Set<Symbol>,
    onDismiss: () -> Unit,
    onUpdate: (symbolCode: String, newSpan: Span?, newColorHex: String?) -> Unit,
    onDelete: () -> Unit
) {
    var selectedSymbolCode by remember { mutableStateOf(operation.symbol.code) }
    var spanText by remember { mutableStateOf(operation.overrideSpan?.value?.toString() ?: "") }
    var colorHex by remember { mutableStateOf(operation.colorHex ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.pattern_editor_edit_operation_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.pattern_editor_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.pattern_editor_cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_editor_edit_operation_title)) },
        text = {
            Column {
                Text(stringResource(R.string.pattern_editor_edit_operation_symbol))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).padding(vertical = 8.dp)
                ) {
                    items(legend.toList(), key = { it.code }) { symbol ->
                        FilterChip(
                            selected = symbol.code == selectedSymbolCode,
                            onClick = { selectedSymbolCode = symbol.code },
                            label = { Text(symbol.textGlyph) }
                        )
                    }
                }

                OutlinedTextField(
                    value = spanText,
                    onValueChange = { spanText = it },
                    label = { Text(stringResource(R.string.pattern_editor_edit_operation_span)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text(stringResource(R.string.pattern_editor_edit_operation_color)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val span = spanText.toIntOrNull()?.let { Span.createOrNull(it) }
                onUpdate(selectedSymbolCode, span, colorHex.ifBlank { null })
            }) {
                Text(stringResource(R.string.pattern_editor_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text(stringResource(R.string.pattern_editor_edit_operation_delete))
            }
        }
    )
}
