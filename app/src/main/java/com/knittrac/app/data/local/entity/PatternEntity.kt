package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-сущность схемы вязания.
 */
@Entity(tableName = "patterns")
data class PatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val technique: String,
    val direction: String,
    @ColumnInfo(name = "is_reversible") val isReversible: Boolean,
    @ColumnInfo(name = "background_image_uri") val backgroundImageUri: String? = null,
    @ColumnInfo(name = "image_offset_x") val imageOffsetX: Float = 0f,
    @ColumnInfo(name = "image_offset_y") val imageOffsetY: Float = 0f,
    @ColumnInfo(name = "image_scale") val imageScale: Float = 1f
)
