package com.knittrac.app.domain.model

import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRepeat

/**
 * Раппорт схемы вместе с идентификатором его схемы-владельца.
 *
 * Используется в бэкапе, потому что `PatternRepeat` не содержит `patternId`.
 * Явный тип вместо `Pair<Long, PatternRepeat>`.
 */
data class PatternRepeatWithId(
    val patternId: PatternId,
    val repeat: PatternRepeat
)
