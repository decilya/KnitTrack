package com.knittrack.app.data.mapper

import com.knittrack.app.data.dto.PatternRepeatDto
import com.knittrack.app.data.local.entity.PatternRepeatEntity
import com.knittrack.app.domain.entity.PatternRepeat
import com.knittrack.app.domain.entity.RowCount
import com.knittrack.app.domain.entity.StitchCount

fun PatternRepeat.toDto(patternId: Long): PatternRepeatDto = PatternRepeatDto(
    patternId = patternId,
    code = code,
    stitchCount = stitchCount.value,
    rowCount = rowCount.value
)

fun PatternRepeatDto.toDomain(): PatternRepeat = PatternRepeat(
    code = code,
    stitchCount = StitchCount.create(stitchCount),
    rowCount = RowCount.create(rowCount)
)

fun PatternRepeatDto.toEntity(): PatternRepeatEntity = PatternRepeatEntity(
    patternId = patternId,
    code = code,
    stitchCount = stitchCount,
    rowCount = rowCount
)
