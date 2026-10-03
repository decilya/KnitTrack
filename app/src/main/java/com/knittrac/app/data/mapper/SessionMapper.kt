package com.knittrac.app.data.mapper

import com.knittrac.app.domain.entity.Session
import com.knittrac.app.core.common.SyncStatus

object SessionMapper {
    /**
     * Маппинг в доменную модель Session.
     */
    fun mapToDomain(
        id: Long = 0,
        projectId: Long,
        startTime: Long,
        endTime: Long,
        rowCount: Int,
        syncStatus: String = SyncStatus.PENDING.name
    ): Session {
        return Session(
            id = id,
            projectId = projectId,
            startTime = startTime,
            endTime = endTime,
            rowCount = rowCount,
            syncStatus = syncStatus
        )
    }
}
