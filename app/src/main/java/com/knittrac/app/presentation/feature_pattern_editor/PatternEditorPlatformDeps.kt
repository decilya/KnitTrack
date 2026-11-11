package com.knittrac.app.presentation.feature_pattern_editor

import androidx.lifecycle.SavedStateHandle
import com.knittrac.app.domain.repository.SettingsRepository
import com.knittrac.app.core.common.ErrorStringProvider
import javax.inject.Inject

/**
 * Платформенные зависимости PatternEditorViewModel (P1-A, 02.11.2026).
 *
 * Сгруппированы, чтобы держать конструктор VM ниже Detekt LongParameterList = 6
 * без @Suppress. Не нарушает SRP: DataStore + SavedStateHandle — инфраструктура
 * Presentation, единая причина изменения — платформенные нужды экрана.
 *
 * @property settingsRepository DataStore-репозиторий (настройка «видимость кнопки Add»).
 * @property savedStateHandle Process death state (patternId, selectedRowId, isStickyMode).
 * @property errorStringProvider Провайдер строковых ресурсов для ошибок.
 */
data class PatternEditorPlatformDeps @Inject constructor(
    val settingsRepository: SettingsRepository,
    val savedStateHandle: SavedStateHandle,
    val errorStringProvider: ErrorStringProvider
)
