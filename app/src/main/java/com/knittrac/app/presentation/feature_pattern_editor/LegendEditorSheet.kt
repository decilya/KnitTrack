package com.knittrac.app.presentation.feature_pattern_editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.knittrac.app.R
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory

/**
 * Коллбэки [LegendEditorSheet] (Урок 26 — DTO вместо 7 позиционных параметров).
 *
 * Сгруппированы, чтобы держать сигнатуру Composable ниже Detekt LongParameterList = 6.
 *
 * @property onAddSymbol Вызывается из диалога «Добавить символ».
 * @property onDeleteSymbol Вызывается при тапе на иконку удаления у кастомного символа.
 * @property onDismiss Закрытие sheet (свайп вниз или крестик).
 */
data class LegendEditorCallbacks(
    val onAddSymbol: (code: String, displayName: String, textGlyph: String, span: Span) -> Unit,
    val onDeleteSymbol: (code: String) -> Unit,
    val onDismiss: () -> Unit
)

/**
 * Sheet управления легендой (7f-D-7).
 *
 * **Поведение:**
 * - Partial detent по умолчанию (swipe up → full).
 * - Секция «Системные» (STANDARD, ownerPatternId = null) — read-only, без кнопок удаления.
 * - Секция «Кастомные» (CUSTOM, ownerPatternId = patternId) — с иконкой удаления.
 * - Кнопка «Добавить» — если [showAddButton] = true (из настроек SettingsScreen).
 * - AlertDialog для добавления нового символа (code, displayName, textGlyph, span).
 *
 * **DDD:** Domain не меняется. UI делегирует в `PatternEditorViewModel`
 * (`AddSymbolUseCase` / `DeleteSymbolUseCase` — готовы в 7f-D-4).
 *
 * @param pattern Схема (legend, rows).
 * @param showAddButton Видимость кнопки «Добавить» (из настроек, default = true).
 * @param callbacks Коллбэки (Урок 26 — DTO).
 * @param modifier Modifier (default — без изменений).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegendEditorSheet(
    pattern: Pattern,
    showAddButton: Boolean,
    callbacks: LegendEditorCallbacks,
    modifier: Modifier = Modifier
) {
    // Partial detent по умолчанию (skipPartiallyExpanded = false).
    // Пользователь видит Canvas сверху и легенду снизу — как в Pattern Keeper.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Делим легенду на системные и кастомные (по SymbolCategory).
    val systemSymbols = pattern.legend.filter { it.category == SymbolCategory.STANDARD }
    val customSymbols = pattern.legend.filter { it.category == SymbolCategory.CUSTOM }

    // Состояние диалога «Добавить символ».
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddSymbolDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { code, displayName, glyph, span ->
                callbacks.onAddSymbol(code, displayName, glyph, span)
                showAddDialog = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = callbacks.onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.pattern_editor_legend_title),
                style = MaterialTheme.typography.titleLarge
            )

            // Кнопка «Добавить» — видна по настройке (default = true).
            if (showAddButton) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pattern_editor_legend_add))
                }
            }

            LazyColumn(Modifier.fillMaxWidth()) {
                // Секция «Системные» (read-only).
                if (systemSymbols.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.pattern_editor_legend_system),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(systemSymbols.toList(), key = { it.code }) { symbol ->
                        SymbolRow(
                            symbol = symbol,
                            canDelete = false,
                            onDelete = {}
                        )
                    }
                }

                // Секция «Кастомные» (с кнопкой удаления).
                if (customSymbols.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.pattern_editor_legend_custom),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(customSymbols.toList(), key = { it.code }) { symbol ->
                        SymbolRow(
                            symbol = symbol,
                            canDelete = true,
                            onDelete = { callbacks.onDeleteSymbol(symbol.code) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Строка символа в легенде: глиф, название, кнопка удаления (для кастомных).
 *
 * @param symbol Символ (Domain).
 * @param canDelete `true` — показать кнопку удаления (CUSTOM).
 * @param onDelete Коллбэк удаления (вызывается при тапе на иконку).
 */
@Composable
private fun SymbolRow(
    symbol: Symbol,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${symbol.textGlyph}  ${symbol.displayName}")
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.pattern_editor_legend_delete)
                )
            }
        }
    }
}

/**
 * Диалог добавления кастомного символа (7f-D-7).
 *
 * Поля: code (уникальный), displayName (внутреннее имя), textGlyph (глиф),
 * defaultColumnSpan (число, default = 1).
 *
 * @param onDismiss Закрытие диалога без сохранения.
 * @param onAdd Коллбэк с параметрами (code, displayName, textGlyph, span).
 */
@Composable
private fun AddSymbolDialog(
    onDismiss: () -> Unit,
    onAdd: (code: String, displayName: String, textGlyph: String, span: Span) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var glyph by remember { mutableStateOf("") }
    var spanText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_editor_legend_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.pattern_editor_legend_code)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.pattern_editor_legend_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = glyph,
                    onValueChange = { glyph = it },
                    label = { Text(stringResource(R.string.pattern_editor_legend_glyph)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = spanText,
                    onValueChange = { spanText = it },
                    label = { Text(stringResource(R.string.pattern_editor_legend_span)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // span: из текста → Span (default = ONE при невалидном).
                val span = spanText.toIntOrNull()?.let { Span.createOrNull(it) } ?: Span.ONE
                // Валидация: code / displayName / glyph не пустые.
                if (code.isNotBlank() && displayName.isNotBlank() && glyph.isNotEmpty()) {
                    onAdd(code, displayName, glyph, span)
                }
            }) {
                Text(stringResource(R.string.pattern_editor_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pattern_editor_cancel))
            }
        }
    )
}
