package com.knittrac.app.data.local.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(tableName = "sessions", foreignKeys = [ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["projectId"])])
data class SessionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0L, val projectId: Long, val startTimestamp: Long, val endTimestamp: Long, val durationSeconds: Long, val rowCount: Int, val updatedAt: Long, val syncStatus: String)
