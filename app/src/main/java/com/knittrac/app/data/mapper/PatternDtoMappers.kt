package com.knittrac.app.data.mapper

import com.knittrac.app.data.dto.PatternDto
import com.knittrac.app.data.dto.PatternRowDto
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Technique

fun Pattern.toDto(): PatternDto = PatternDto(
    id = id.value,
    name = name.value,
    technique = technique.name,
    direction = direction.name,
    isReversible = isReversible,
    backgroundImageUri = backgroundImageUri,
    imageOffsetX = imageOffsetX,
    imageOffsetY = imageOffsetY,
    imageScale = imageScale
)

/** Только метаданные. Ряды, легенда и раппорты приходят отдельными массивами. */
fun PatternDto.toDomain(): Pattern = Pattern(
    id = PatternId.create(id),
    name = PatternName.create(name),
    technique = Technique.fromNameOrDefault(technique),
    direction = PatternDirection.fromNameOrDefault(direction),
    isReversible = isReversible,
    backgroundImageUri = backgroundImageUri,
    imageOffsetX = imageOffsetX,
    imageOffsetY = imageOffsetY,
    imageScale = imageScale
)

fun PatternRow.toDto(patternId: Long): PatternRowDto = PatternRowDto(
    id = id.value,
    patternId = patternId,
    rowNumber = rowNumber.value
)

fun PatternRowDto.toDomain(): PatternRow = PatternRow(
    id = PatternRowId.create(id),
    rowNumber = RowNumber.create(rowNumber)
)

fun PatternRowDto.toEntity(): PatternRowEntity = PatternRowEntity(
    id = id,
    patternId = patternId,
    rowNumber = rowNumber
)
