package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room-сущность символа (системного или кастомного). */
@Entity(
    tableName = "symbols",
    foreignKeys = [ForeignKey(entity = PatternEntity::class, parentColumns = ["id"], childColumns = ["owner_pattern_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["owner_pattern_id"])]
)
data class SymbolEntity(
    @PrimaryKey val code: String,
    @ColumnInfo(name = "owner_pattern_id") val ownerPatternId: Long? = null,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "text_glyph") val textGlyph: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "default_column_span") val defaultColumnSpan: Int = 1,
    @ColumnInfo(name = "default_row_span") val defaultRowSpan: Int = 1
)
