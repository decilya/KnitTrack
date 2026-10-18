package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/** Room-сущность раппорта (повторяющегося фрагмента) схемы. */
@Entity(
    tableName = "pattern_repeats",
    primaryKeys = ["pattern_id", "code"],
    foreignKeys = [ForeignKey(entity = PatternEntity::class, parentColumns = ["id"], childColumns = ["pattern_id"], onDelete = ForeignKey.CASCADE)]
)
data class PatternRepeatEntity(
    @ColumnInfo(name = "pattern_id") val patternId: Long,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "stitch_count") val stitchCount: Int,
    @ColumnInfo(name = "row_count") val rowCount: Int
)
