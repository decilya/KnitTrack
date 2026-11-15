package com.knittrack.app.domain.service

import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.domain.entity.RowNumber

/**
 * Параметры изменения номера ряда (7f-D-3).
 * 
 * @property patternId Схема, к которой относится ряд.
 * @property rowId Ряд, который перемещаем.
 * @property newRowNumber Новый номер (1-based).
 */
data class UpdatePatternRowNumberParams(
    val patternId: PatternId,
    val rowId: PatternRowId,
    val newRowNumber: RowNumber
)
