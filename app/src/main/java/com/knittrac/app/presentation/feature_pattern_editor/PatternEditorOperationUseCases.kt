package com.knittrac.app.presentation.feature_pattern_editor

import com.knittrac.app.domain.service.AddPatternOperationUseCase
import com.knittrac.app.domain.service.DeleteOperationUseCase
import com.knittrac.app.domain.service.UpdatePatternOperationUseCase
import javax.inject.Inject

/**
 * Операции в рядах (P1-A, 02.11.2026).
 *
 * Единая причина изменения: жизненный цикл PatternOperation (add/update/delete).
 */
data class PatternEditorOperationUseCases @Inject constructor(
    val addPatternOperation: AddPatternOperationUseCase,
    val updatePatternOperation: UpdatePatternOperationUseCase,
    val deleteOperation: DeleteOperationUseCase
)
