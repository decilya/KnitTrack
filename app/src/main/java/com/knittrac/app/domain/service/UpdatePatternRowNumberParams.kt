package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber

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
