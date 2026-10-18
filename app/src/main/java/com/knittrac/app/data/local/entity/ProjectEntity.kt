package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room-сущность проекта.
 * 
 * ВАЖНО: Kotlin-поля названы в camelCase (по стилю Kotlin),
 * но физические имена колонок в БД — snake_case.
 * Это реализуется через аннотацию @ColumnInfo(name = "...").
 */
@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(entity = PatternEntity::class, parentColumns = ["id"], childColumns = ["pattern_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = PatternRowEntity::class, parentColumns = ["id"], childColumns = ["current_pattern_row_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["pattern_id"]), Index(value = ["current_pattern_row_id"])]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    val name: String,
    
    val category: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "total_time_seconds")
    val totalTimeSeconds: Long,
    
    @ColumnInfo(name = "total_rows", defaultValue = "0")
    val totalRows: Int = 0,

    @ColumnInfo(name = "target_row_count")
    val targetRowCount: Int? = null,

    @ColumnInfo(name = "pattern_id")
    val patternId: Long? = null,

    @ColumnInfo(name = "current_row_number", defaultValue = "0")
    val currentRowNumber: Int = 0,

    @ColumnInfo(name = "current_pattern_row_id")
    val currentPatternRowId: Long? = null,

    @ColumnInfo(name = "current_stitch", defaultValue = "0")
    val currentStitch: Int = 0,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String
)
