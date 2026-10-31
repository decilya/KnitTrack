package com.knittrac.app.presentation.feature_pattern_editor

import com.knittrac.app.domain.service.AddPatternOperationUseCase
import com.knittrac.app.domain.service.CreatePatternUseCase
import com.knittrac.app.domain.service.GetAllPatternSummariesService
import com.knittrac.app.domain.service.GetPatternByIdService
import javax.inject.Inject

/** Группа use case редактора. Держит конструктор ViewModel ниже порога detekt. */
data class PatternEditorUseCases @Inject constructor(
    val getPatternById: GetPatternByIdService,
    val getAllPatternSummaries: GetAllPatternSummariesService,
    val addPatternOperation: AddPatternOperationUseCase,
    val createPattern: CreatePatternUseCase
)
