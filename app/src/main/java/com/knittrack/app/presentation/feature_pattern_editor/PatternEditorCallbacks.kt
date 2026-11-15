package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.domain.entity.ColumnStart
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.presentation.feature_pattern_editor.PatternEditorContract.Intent

/**
 * Колбэки для UI-компонентов Pattern Editor.
 *
 * Вынесены из [PatternEditorScreen] в отдельный файл, чтобы вложенные
 * Composable ([PatternCanvas], диалоги, шиты) могли их принимать
 * в отдельных файлах.
 *
 * @property onIntent Общий MVI-диспетчер (MVI Intent).
 * @property onCellTap Тап по ячейке Canvas (row + column).
 * @property onCellLongPress Long-press по ячейке Canvas (row + column).
 */
internal data class PatternEditorCallbacks(
    val onIntent: (Intent) -> Unit,
    val onCellTap: (PatternRowId, ColumnStart) -> Unit,
    val onCellLongPress: (PatternRowId, ColumnStart) -> Unit
)
