package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room-сущность сессии.
 * 
 * ВАЖНО: 
 * 1. Kotlin-поля в camelCase, физические колонки — snake_case (через @ColumnInfo).
 * 2. Связана с ProjectEntity через внешний ключ project_id с каскадным удалением.
 * 3. Добавлены индексы на project_id и start_time для ускорения агрегации статистики.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id"]),
        Index(value = ["start_time"])
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    
    @ColumnInfo(name = "start_time")
    val startTimestamp: Long,
    
    @ColumnInfo(name = "end_time")
    val endTimestamp: Long,
    
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long,
    
    @ColumnInfo(name = "row_count")
    val rowCount: Int,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String
)
