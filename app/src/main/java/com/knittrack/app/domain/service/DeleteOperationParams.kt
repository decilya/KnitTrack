package com.knittrack.app.domain.service

import com.knittrack.app.domain.entity.ColumnStart
import com.knittrack.app.domain.entity.PatternRowId

/**
 * Параметры удаления операции (для undo).
 *
 * @property rowId Ряд-владелец.
 * @property columnStart Колонка-владелец (PK вместе с rowId).
 */
data class DeleteOperationParams(
    val rowId: PatternRowId,
    val columnStart: ColumnStart
)
