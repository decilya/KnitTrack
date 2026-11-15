package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.domain.service.AddSymbolUseCase
import com.knittrack.app.domain.service.DeleteSymbolUseCase
import javax.inject.Inject

/**
 * Легенда схемы (P1-A, 02.11.2026).
 *
 * Единая причина изменения: жизненный цикл Symbol (add CUSTOM, delete CUSTOM).
 * Системные символы — read-only, не изменяются.
 */
data class PatternEditorSymbolUseCases @Inject constructor(
    val addSymbol: AddSymbolUseCase,
    val deleteSymbol: DeleteSymbolUseCase
)
