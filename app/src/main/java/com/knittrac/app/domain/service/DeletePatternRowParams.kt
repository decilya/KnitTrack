package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRowId

/**
 * Параметры удаления ряда из схемы (7f-D-2).
 * 
 * @property patternId Идентификатор схемы-владельца (для валидации).
 * @property rowId Идентификатор удаляемого ряда.
 */
data class DeletePatternRowParams(
    val patternId: PatternId,
    val rowId: PatternRowId
)
