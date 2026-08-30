package com.knittrac.app.data.mapper
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.Session
object SessionMapper {
    fun SessionEntity.toDomain(): Session {
        val sync = runCatching { SyncStatus.valueOf(this.syncStatus) }.getOrDefault(SyncStatus.PENDING)
        return Session(id, projectId, startTimestamp, endTimestamp, durationSeconds, rowCount, updatedAt, sync)
    }
    fun Session.toEntity(): SessionEntity = SessionEntity(id, projectId, startTimestamp, endTimestamp, durationSeconds, rowCount, updatedAt, syncStatus.name)
}
