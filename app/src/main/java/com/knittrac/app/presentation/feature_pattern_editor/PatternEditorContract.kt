package com.knittrac.app.presentation.feature_pattern_editor

import androidx.annotation.StringRes
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.domain.service.CreatePatternParams

/**
 * MVI-контракт экрана Pattern Editor (7f-B).
 *
 * Отдельный файл (конвенция `TimerContract.kt`, `SettingsContract.kt`):
 * SRP — контракт описывает MVI, VM оркестрирует; OCP — расширение Intent
 * не требует правки VM-файла.
 */
object PatternEditorContract {

    /**
     * Единый снапшот состояния экрана.
     *
     * @property availablePatterns Все схемы для селектора (id + name).
     * @property currentPatternId Выбранная схема.
     * @property pattern Полная загруженная схема (legend, rows, repeats).
     * @property selectedRowId Выбранный ряд на Canvas.
     * @property selectedColumn Выбранная колонка на Canvas.
     * @property selectedSymbolCode Выбранный символ в палитре (sticky mode).
     * @property isStickyMode `true` — sticky (символ «прилип», тап по ячейке =
     *   добавить); `false` — sheet (тап по ячейке → палитра).
     * @property isLoading Загрузка схемы / списка.
     * @property isSaving Guard от double-tap при добавлении операции.
     * @property errorRes Ошибка для snackbar. null — ошибки нет.
     */
    data class State(
        val availablePatterns: List<PatternSummary> = emptyList(),
        val currentPatternId: PatternId? = null,
        val pattern: Pattern? = null,
        val selectedRowId: PatternRowId? = null,
        val selectedColumn: ColumnStart? = null,
        val selectedSymbolCode: String? = null,
        val isStickyMode: Boolean = true,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        @StringRes val errorRes: Int? = null,
        val lastOperations: List<PatternOperation> = emptyList(),
        val editingOperation: PatternOperation? = null,
        val editingRow: com.knittrac.app.domain.entity.PatternRow? = null,

        /**
         * Открыт ли [LegendEditorSheet] (7f-D-7).
         * Управляется Intent.OpenLegendEditor / CloseLegendEditor.
         */
        val isLegendEditorOpen: Boolean = false,

        /**
         * Видимость кнопки «Добавить» в [LegendEditorSheet] (7f-D-7).
         * Читается из SettingsRepository.legendAddButtonVisibleFlow
         * (настройка в SettingsScreen). Default = true.
         */
        val showLegendAddButton: Boolean = true
    )

    /**
     * Действия пользователя.
     */
    sealed class Intent {
        /** Загрузить схему по id (первый вход / restore). */
        data class LoadPattern(val patternId: PatternId) : Intent()
        /** Переключить схему через селектор. */
        data class SelectPattern(val patternId: PatternId) : Intent()
        data class SelectRow(val rowId: PatternRowId) : Intent()
        data class SelectColumn(val columnStart: ColumnStart) : Intent()
        data class SelectSymbol(val symbolCode: String) : Intent()
        /** Переключить sticky/sheet. */
        object ToggleMode : Intent()
        /** Добавить операцию в выбранную ячейку. */
        data class AddOperation(
            val rowId: PatternRowId,
            val columnStart: ColumnStart
        ) : Intent()
        data class CreatePattern(val params: CreatePatternParams) : Intent()
        /** Снять последнюю операцию со стека undo. */
        object UndoLastOperation : Intent()
        /** Открыть диалог редактирования операции. */
        data class EditOperation(val operation: PatternOperation) : Intent()
        /** Открыть диалог редактирования ряда. */
        data class EditRow(val row: com.knittrac.app.domain.entity.PatternRow) : Intent()
        /** Закрыть диалог редактирования. */
        object DismissEditor : Intent()
        /** Обновить операцию. */
        data class UpdateOperation(
            val oldOperation: PatternOperation,
            val newSymbolCode: String,
            val newOverrideSpan: com.knittrac.app.domain.entity.Span?,
            val newColorHex: String?
        ) : Intent()
        /** Удалить операцию (из диалога). */
        data class DeleteOperationFromDialog(val operation: PatternOperation) : Intent()
        /** Обновить номер ряда. */
        data class UpdateRowNumber(
            val rowId: PatternRowId,
            val newRowNumber: com.knittrac.app.domain.entity.RowNumber
        ) : Intent()
        /** Удалить ряд. */
        data class DeleteRow(val rowId: PatternRowId) : Intent()
        /**
         * Открыть [LegendEditorSheet] (7f-D-7).
         * Sheet показывает системные (read-only) и кастомные (с кнопкой удаления) символы.
         */
        object OpenLegendEditor : Intent()

        /**
         * Закрыть [LegendEditorSheet] (7f-D-7).
         * Свайп вниз или крестик.
         */
        object CloseLegendEditor : Intent()

        /**
         * Добавить кастомный символ в легенду схемы (7f-D-7).
         * Делегирует в AddSymbolUseCase → AddSymbolService → SymbolDao.insertSymbol.
         * Символ создаётся с category = CUSTOM, ownerPatternId = patternId.
         */
        data class AddSymbol(
            val code: String,
            val displayName: String,
            val textGlyph: String,
            val defaultColumnSpan: com.knittrac.app.domain.entity.Span
        ) : Intent()

        /**
         * Удалить кастомный символ из легенды схемы (7f-D-7).
         * Делегирует в DeleteSymbolUseCase → DeleteSymbolService → SymbolDao.deleteSymbol.
         * Системные символы (ownerPatternId = null) удалить нельзя → ValidationError.
         * Если символ используется в операциях (FK RESTRICT) → ValidationError от репозитория.
         */
        data class DeleteSymbol(val code: String) : Intent()

        /** Повторить загрузку после ошибки. */
        object Retry : Intent()
    }

    /**
     * Одноразовые события.
     */
    sealed class Effect {
        data class ShowError(@StringRes val messageResId: Int) : Effect()
        /** Operation несёт созданную операцию (toast / undo — P3). */
        data class OperationAdded(val operation: PatternOperation) : Effect()
    }
}
