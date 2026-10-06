package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-сущность проекта.
 * 
 * ВАЖНО: Kotlin-поля названы в camelCase (по стилю Kotlin),
 * но физические имена колонок в БД — snake_case.
 * Это реализуется через аннотацию @ColumnInfo(name = "...").
 */
@Entity(tableName = "projects")
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
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String
)
