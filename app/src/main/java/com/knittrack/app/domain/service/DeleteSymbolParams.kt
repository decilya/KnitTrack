package com.knittrack.app.domain.service

import com.knittrack.app.domain.entity.PatternId

/**
 * Параметры удаления кастомного символа из схемы (7f-D-4).
 */
data class DeleteSymbolParams(
    val patternId: PatternId,
    val code: String
)
