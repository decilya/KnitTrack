package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Technique

data class CreatePatternParams(
    val name: PatternName,
    val technique: Technique,
    val direction: PatternDirection,
    val rowCount: RowCount
)
