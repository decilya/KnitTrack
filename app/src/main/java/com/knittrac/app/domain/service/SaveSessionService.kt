package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.AppErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.domain.entity.Session
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SaveSessionService @Inject constructor(
    private val sessionDao: SessionDao,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<SaveSessionParams, Long>(dispatcher) {

    override suspend fun execute(params: SaveSessionParams): Result<Long> {
        return try {
            val session = Session(
                projectId = params.projectId,
                startTime = params.startTimestamp,
                endTime = params.endTimestamp,
                rowCount = params.rowCount,
                syncStatus = SyncStatus.PENDING.name
            )
            val sessionId = sessionDao.insertSession(session)
            Result.Success(sessionId)
        } catch (e: Exception) {
            Result.Error(AppErrorMapper.map(e))
        }
    }
}
