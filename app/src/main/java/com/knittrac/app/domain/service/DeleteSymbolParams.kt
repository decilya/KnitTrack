package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.PatternId

/**
 * Параметры удаления кастомного символа из схемы (7f-D-4).
 */
data class DeleteSymbolParams(
    val patternId: PatternId,
    val code: String
)
