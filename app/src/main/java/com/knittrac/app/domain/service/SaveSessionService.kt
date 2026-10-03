package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.domain.entity.Session
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис для сохранения завершенной сессии вязания в локальную базу данных.
 */
class SaveSessionService @Inject constructor(
    private val sessionDao: SessionDao,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<SaveSessionParams, Long>(dispatcher) {

    override suspend fun execute(params: SaveSessionParams): Result<Long> {
        return try {
            // Маппим параметры доменной модели в сущность базы данных
            val session = Session(
                projectId = params.projectId,
                startTime = params.startTimestamp,
                endTime = params.endTimestamp,
                rowCount = params.rowCount,
                syncStatus = SyncStatus.PENDING.name
            )
            
            // Сохраняем в Room и возвращаем ID
            val sessionId = sessionDao.insertSession(session)
            Result.Success(sessionId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Неизвестная ошибка при сохранении сессии")
        }
    }
}
