package com.knittrac.app.presentation.feature_pattern_editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.RowNumber

/**
 * Sheet редактирования ряда (7f-D-6).
 *
 * Позволяет переместить ряд (ввод нового номера) или удалить.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowEditorSheet(
    row: PatternRow,
    onDismiss: () -> Unit,
    onMove: (newNumber: RowNumber) -> Unit,
    onDelete: () -> Unit
) {
    var numberText by remember { mutableStateOf(row.rowNumber.value.toString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.pattern_editor_row_editor_delete_confirm)) },
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.pattern_editor_row_editor_title))
            OutlinedTextField(
                value = numberText,
                onValueChange = { numberText = it },
                label = { Text(stringResource(R.string.pattern_editor_row_editor_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = {
                    numberText.toIntOrNull()?.let { RowNumber.createOrNull(it) }?.let { onMove(it) }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(stringResource(R.string.pattern_editor_ok))
            }
            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.pattern_editor_row_editor_delete))
            }
        }
    }
}
