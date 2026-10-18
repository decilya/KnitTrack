package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room-сущность ряда схемы.
 */
@Entity(
    tableName = "pattern_rows",
    foreignKeys = [ForeignKey(entity = PatternEntity::class, parentColumns = ["id"], childColumns = ["pattern_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["pattern_id"]), Index(value = ["pattern_id", "row_number"], unique = true)]
)
data class PatternRowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "pattern_id") val patternId: Long,
    @ColumnInfo(name = "row_number") val rowNumber: Int
)
