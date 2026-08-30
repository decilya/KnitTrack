package com.knittrac.app.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "projects")
data class ProjectEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0L, val name: String, val category: String, val createdAt: Long, val totalTimeSeconds: Long = 0L, val updatedAt: Long, val syncStatus: String)
