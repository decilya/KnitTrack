package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * Проекция `projects` + derived `current_row_number` из `pattern_rows`.
 *
 * В БД v10 колонки `current_row_number` нет (3NF). Значение вычисляется
 * через `LEFT JOIN pattern_rows pr ON pr.id = projects.current_pattern_row_id`.
 * Если `current_pattern_row_id = null` — `derivedCurrentRowNumber = null`,
 * маппер вернёт [com.knittrac.app.domain.entity.RowCount.ZERO].
 *
 * Data-only projection: в Domain не протекает, маппинг — в EntityMappers.
 */
data class ProjectWithCurrentRowNumber(
    @Embedded
    val project: ProjectEntity,

    @ColumnInfo(name = "derived_current_row_number")
    val derivedCurrentRowNumber: Int?
)
