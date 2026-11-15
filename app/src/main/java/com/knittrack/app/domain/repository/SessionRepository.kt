package com.knittrack.app.domain.repository

import com.knittrack.app.core.common.Result
import com.knittrack.app.core.common.SyncStatus
import com.knittrack.app.domain.entity.DailyStat
import com.knittrack.app.domain.entity.ProjectId
import com.knittrack.app.domain.entity.Session
import com.knittrack.app.domain.entity.SessionId
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория для работы с сессиями вязания.
 *
 * P1-D (02.11.2026): на границе репозитория — VO ([SessionId], [ProjectId]).
 * Реализация [SessionRepositoryImpl] разворачивает их в примитивы для DAO.
 */
interface SessionRepository {
    fun getSessionsByProjectIdFlow(projectId: ProjectId): Flow<List<Session>>
    suspend fun getSessionsByProjectId(projectId: ProjectId): Result<List<Session>>
    suspend fun getAllSessions(): Result<List<Session>>
    suspend fun getPendingSessions(): Result<List<Session>>

    /**
     * Атомарно добавляет сессию и обновляет время проекта.
     *
     * @param session Domain-модель.
     * @return [Result.Success] с [SessionId] новой сессии;
     *   [com.knittrack.app.core.common.AppError.NotFoundError], если проект не найден;
     *   [com.knittrack.app.core.common.AppError.DatabaseError] при сбое БД.
     */
    suspend fun addSessionAtomically(session: Session): Result<SessionId>

    /**
     * Обновляет существующую сессию по id.
     */
    suspend fun updateSession(session: Session): Result<Unit>
    
    suspend fun updateSyncStatus(sessionId: SessionId, status: SyncStatus): Result<Unit>
    
    suspend fun deleteSessionAndDecrementProjectTotals(sessionId: SessionId): Result<Unit>
    
    suspend fun getDailyStats(projectId: ProjectId): Result<List<DailyStat>>
}
