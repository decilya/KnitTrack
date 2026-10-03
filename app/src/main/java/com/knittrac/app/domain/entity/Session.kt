package com.knittrac.app.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.knittrac.app.core.common.SyncStatus

/**
 * Сущность завершенной сессии вязания.
 * Хранится в локальной базе данных Room.
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long,

    @ColumnInfo(name = "row_count")
    val rowCount: Int,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.PENDING.name
)
