package com.knittrac.app.domain.model

import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName

/**
 * Лёгкая проекция [com.knittrac.app.domain.entity.Pattern] для селектора схем
 * (7f-B).
 *
 * ISP: только `id + name`. Полный `Pattern` (с `legend`, `rows`, `repeats`)
 * загружается отдельно при выборе схемы (`GetPatternByIdService`), чтобы
 * не делать N+1 запросов на список.
 */
data class PatternSummary(
    val id: PatternId,
    val name: PatternName
)
