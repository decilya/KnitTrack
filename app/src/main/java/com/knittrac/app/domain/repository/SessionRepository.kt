package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.entity.Session
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория для работы с сессиями вязания.
 * Инкапсулирует всю бизнес-логику и доступ к данным, возвращая только Domain-модели.
 */
interface SessionRepository {
    /** Получает реактивный поток сессий для конкретного проекта. */
    fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<Session>>
    
    /** Получает статический список сессий для проекта. */
    suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>>
    
    /** Получает все сессии из хранилища. */
    suspend fun getAllSessions(): Result<List<Session>>
    
    /** Получает список сессий, ожидающих синхронизации. */
    suspend fun getPendingSessions(): Result<List<Session>>
    
    /** Добавляет новую сессию в хранилище. */
    suspend fun addSession(session: Session): Result<Long>
    
    /** Обновляет существующую сессию. */
    suspend fun updateSession(session: Session): Result<Unit>
    
    /** Обновляет статус синхронизации конкретной сессии. */
    suspend fun updateSyncStatus(sessionId: Long, status: String): Result<Unit>
    
    /** Получает агрегированную статистику по дням для конкретного проекта. */
    suspend fun getDailyStats(projectId: Long): Result<List<DailyStat>>
}
