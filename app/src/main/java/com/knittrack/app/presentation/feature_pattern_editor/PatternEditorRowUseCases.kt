package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.domain.service.DeletePatternRowUseCase
import com.knittrack.app.domain.service.UpdatePatternRowNumberUseCase
import javax.inject.Inject

/**
 * Ряды схемы (P1-A, 02.11.2026).
 *
 * Единая причина изменения: жизненный цикл PatternRow (update rowNumber, delete).
 * Добавление ряда — отдельная задача (P3).
 */
data class PatternEditorRowUseCases @Inject constructor(
    val updatePatternRowNumber: UpdatePatternRowNumberUseCase,
    val deletePatternRow: DeletePatternRowUseCase
)
