package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.findRowById
import com.knittrack.app.domain.entity.findSymbolByCode
import com.knittrack.app.domain.model.AddPatternOperationParams
import com.knittrack.app.domain.model.UpdatePatternOperationParams

/**
 * Вспомогательные классы и функции для валидации входа и сборки параметров.
 * Вынесено из ViewModel для соблюдения SRP и сокращения размера класса.
 */

internal data class AddContext(
    val patternId: PatternId,
    val params: AddPatternOperationParams
)

internal data class UpdateContext(
    val patternId: PatternId,
    val params: UpdatePatternOperationParams
)

internal fun resolveAddContext(
    state: PatternEditorContract.State,
    intent: PatternEditorContract.Intent.AddOperation
): AddContext? =
    state.data.pattern?.let { pattern ->
        state.data.currentPatternId?.let { patternId ->
            state.selection.selectedSymbolCode?.let { symbolCode ->
                pattern.findRowById(intent.rowId)?.let { row ->
                    pattern.findSymbolByCode(symbolCode)?.let { symbol ->
                        AddContext(patternId, AddPatternOperationParams(patternId, row, intent.columnStart, symbol))
                    }
                }
            }
        }
    }

internal fun resolveUpdateContext(
    state: PatternEditorContract.State,
    intent: PatternEditorContract.Intent.UpdateOperation
): UpdateContext? =
    state.data.pattern?.let { pattern ->
        state.data.currentPatternId?.let { patternId ->
            pattern.findSymbolByCode(intent.newSymbolCode)?.let { newSymbol ->
                UpdateContext(
                    patternId,
                    UpdatePatternOperationParams(
                        patternId = patternId,
                        rowId = intent.oldOperation.patternRowId,
                        columnStart = intent.oldOperation.columnStart,
                        symbol = newSymbol,
                        backSymbol = intent.oldOperation.backSymbol,
                        colorHex = intent.newColorHex,
                        columnSpanOverride = intent.newColumnSpan,
                        rowSpanOverride = intent.newRowSpan,
                        repeatCode = intent.oldOperation.repeatCode
                    )
                )
            }
        }
    }
