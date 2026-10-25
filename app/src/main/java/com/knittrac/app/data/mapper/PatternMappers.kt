package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Technique

fun PatternEntity.toDomain(rows: List<PatternRowEntity>): Pattern = Pattern(
    id = PatternId.createOrDefault(id),
    name = PatternName.createOrDefault(name),
    technique = Technique.fromNameOrDefault(technique),
    direction = PatternDirection.fromNameOrDefault(direction),
    isReversible = isReversible,
    backgroundImageUri = backgroundImageUri,
    imageOffsetX = imageOffsetX,
    imageOffsetY = imageOffsetY,
    imageScale = imageScale,
    legend = emptySet(),
    rows = rows.map { it.toDomain() },
    repeats = emptyList()
)

fun PatternRowEntity.toDomain(): PatternRow = PatternRow(
    id = PatternRowId.createOrDefault(id),
    rowNumber = RowNumber.createOrDefault(rowNumber),
    operations = emptyList()
)
