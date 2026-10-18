package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room-сущность прогресса проекта.
 */
@Entity(
    tableName = "project_progress",
    primaryKeys = ["project_id", "pattern_row_id"],
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["project_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PatternRowEntity::class, parentColumns = ["id"], childColumns = ["pattern_row_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["pattern_row_id"])]
)
data class ProjectProgressEntity(
    @ColumnInfo(name = "project_id") val projectId: Long,
    @ColumnInfo(name = "pattern_row_id") val patternRowId: Long
)
