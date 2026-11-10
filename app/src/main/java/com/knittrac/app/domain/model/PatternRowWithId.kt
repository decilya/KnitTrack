package com.knittrac.app.domain.model

import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRow

/**
 * Ряд схемы вместе с идентификатором его схемы-владельца.
 *
 * Используется в бэкапе, потому что `PatternRow` не содержит `patternId`
 * (владеет `Pattern`). Явный тип вместо `Pair<Long, PatternRow>`.
 */
data class PatternRowWithId(
    val patternId: PatternId,
    val row: PatternRow
)
