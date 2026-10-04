package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.entity.Session

interface SessionRepository {
    suspend fun addSession(session: Session): Result<Long>
    suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>>
    suspend fun getAllSessions(): Result<List<Session>>
    suspend fun getDailyStats(projectId: Long): Result<List<DailyStat>>
}
