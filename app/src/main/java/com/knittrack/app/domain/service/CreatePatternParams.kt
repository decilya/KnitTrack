package com.knittrack.app.domain.service

import com.knittrack.app.domain.entity.PatternDirection
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.entity.RowCount
import com.knittrack.app.domain.entity.Technique

data class CreatePatternParams(
    val name: PatternName,
    val technique: Technique,
    val direction: PatternDirection,
    val rowCount: RowCount
)
