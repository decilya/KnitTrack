package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.entity.Session
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория для работы с сессиями вязания.
 * Инкапсулирует всю бизнес-логику и доступ к данным, возвращая только Domain-модели.
 */
interface SessionRepository {
    fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<Session>>
    suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>>
    suspend fun getAllSessions(): Result<List<Session>>
    suspend fun getPendingSessions(): Result<List<Session>>
    
    /**
     * Атомарно добавляет сессию и обновляет время соответствующего проекта.
     * 
     * Гарантирует целостность данных на уровне базы данных (через @Transaction в DAO): 
     * если обновление времени проекта не удастся, сессия также не будет сохранена (произойдет откат).
     * 
     * @param session Domain-модель сессии для сохранения.
     * @return [Result.Success] с ID новой сессии или [Result.Error] при сбое транзакции.
     */
    suspend fun addSessionAtomically(session: Session): Result<Long>
    
    /**
     * Обновляет существующую сессию по id.
     *
     * Не создаёт новую строку. Для create-пути использовать
     * [addSessionAtomically].
     *
     * @param session Domain-модель с актуальными полями.
     * @return [Result.Success], [Result.Error] с NotFoundError
     *   если сессии с таким id нет, или [Result.Error] с DatabaseError.
     */
    suspend fun updateSession(session: Session): Result<Unit>
    suspend fun updateSyncStatus(sessionId: Long, status: SyncStatus): Result<Unit>
    suspend fun getDailyStats(projectId: Long): Result<List<DailyStat>>
}
