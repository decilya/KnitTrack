package com.knittrac.app.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.knittrac.app.core.common.SyncStatus

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: Category,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "total_time_seconds")
    val totalTimeSeconds: Long = 0,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.PENDING.name
)
