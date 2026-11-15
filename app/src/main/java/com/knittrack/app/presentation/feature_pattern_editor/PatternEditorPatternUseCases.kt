package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.domain.service.CreatePatternUseCase
import com.knittrack.app.domain.service.ExportPatternSvgUseCase
import com.knittrack.app.domain.service.ExportPatternPdfUseCase
import com.knittrack.app.domain.service.GetAllPatternSummariesService
import com.knittrack.app.domain.service.GetPatternByIdService
import javax.inject.Inject

/**
 * Жизненный цикл Pattern (P1-A, 02.11.2026).
 *
 * Чтение (getPatternById, getAllPatternSummaries) + создание (createPattern).
 * Единая причина изменения: жизненный цикл агрегата Pattern.
 *
 * Разбиение [PatternEditorUseCases] (10 полей, God Bag) по SRP:
 * 4 группы по причине изменения, каждая независима.
 */
data class PatternEditorPatternUseCases @Inject constructor(
    val getPatternById: GetPatternByIdService,
    val getAllPatternSummaries: GetAllPatternSummariesService,
    val createPattern: CreatePatternUseCase,
    val exportSvg: ExportPatternSvgUseCase,
    val exportPdf: ExportPatternPdfUseCase
)
