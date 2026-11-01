package com.knittrac.app.presentation.feature_pattern_editor

import com.knittrac.app.domain.service.AddPatternOperationUseCase
import com.knittrac.app.domain.service.CreatePatternUseCase
import com.knittrac.app.domain.service.DeleteOperationUseCase
import com.knittrac.app.domain.service.DeletePatternRowUseCase
import com.knittrac.app.domain.service.GetAllPatternSummariesService
import com.knittrac.app.domain.service.GetPatternByIdService
import com.knittrac.app.domain.service.UpdatePatternOperationUseCase
import com.knittrac.app.domain.service.UpdatePatternRowNumberUseCase
import javax.inject.Inject

/**
 * Группа UseCase для редактора схем. Держит конструктор ViewModel ниже порога detekt.
 * 8 полей, data class (Detekt игнорирует).
 */
data class PatternEditorUseCases @Inject constructor(
    val getPatternById: GetPatternByIdService,
    val getAllPatternSummaries: GetAllPatternSummariesService,
    val addPatternOperation: AddPatternOperationUseCase,
    val deleteOperation: DeleteOperationUseCase,
    val createPattern: CreatePatternUseCase,
    val updatePatternOperation: UpdatePatternOperationUseCase,
    val updatePatternRowNumber: UpdatePatternRowNumberUseCase,
    val deletePatternRow: DeletePatternRowUseCase
)
