package com.knittrac.app.data.local

import androidx.room.*
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.DailyStat
import kotlinx.coroutines.flow.Flow

private const val MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000L

/**
 * Интерфейс доступа к данным (DAO) для сущности Session.
 * 
 * Содержит типобезопасные SQL-запросы для CRUD-операций и агрегации статистики.
 * Все имена колонок в @Query строго соответствуют значениям из @ColumnInfo в SessionEntity (snake_case).
 */
@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>): List<Long>

    @Query("SELECT * FROM sessions WHERE project_id = :projectId ORDER BY start_time DESC")
    fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE project_id = :projectId ORDER BY start_time DESC")
    suspend fun getSessionsByProjectId(projectId: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE sync_status = 'PENDING'")
    suspend fun getPendingSessions(): List<SessionEntity>

    @Query("UPDATE sessions SET sync_status = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: Long, status: String)

    @Query("""
        SELECT 
            (start_time / $MILLIS_IN_A_DAY) * $MILLIS_IN_A_DAY AS dayTimestamp, 
            SUM(duration_seconds) AS totalSeconds 
        FROM sessions 
        WHERE project_id = :projectId 
        GROUP BY dayTimestamp 
        ORDER BY dayTimestamp DESC
    """)
    suspend fun getDailyStats(projectId: Long): List<DailyStat>

    /**
     * Внутренний метод для обновления общего времени проекта.
     * Используется исключительно внутри атомарной транзакции [insertSessionAndIncrementProjectTime].
     * 
     * @param projectId Идентификатор проекта.
     * @param durationSeconds Добавляемое время в секундах.
     * @param updatedAt Временная метка обновления.
     */
    @Query("""
        UPDATE projects 
        SET total_time_seconds = total_time_seconds + :durationSeconds, 
            updated_at = :updatedAt, 
            sync_status = 'PENDING' 
        WHERE id = :projectId
    """)
    suspend fun updateProjectTotalTime(projectId: Long, durationSeconds: Long, updatedAt: Long)

    /**
     * Атомарно добавляет сессию и обновляет время соответствующего проекта в одной транзакции.
     * 
     * Аннотация @Transaction гарантирует, что обе операции ([insertSession] и [updateProjectTotalTime]) 
     * выполнятся как единое целое. Если произойдет любая ошибка (например, сбой БД или нарушение ограничений), 
     * вся транзакция будет откачена (rolled back). Это предотвращает рассинхронизацию данных, 
     * когда сессия сохранена, а общее время проекта не обновилось.
     * 
     * @param session Сущность сессии для сохранения.
     * @param durationSeconds Длительность сессии в секундах для добавления к проекту.
     * @param updatedAt Временная метка обновления.
     * @return ID новой строки сессии в базе данных.
     */
    @Transaction
    suspend fun insertSessionAndIncrementProjectTime(
        session: SessionEntity,
        durationSeconds: Long,
        updatedAt: Long
    ): Long {
        // 1. Сначала сохраняем сессию
        val newRowId = insertSession(session)
        // 2. Затем атомарно обновляем время проекта
        updateProjectTotalTime(session.projectId, durationSeconds, updatedAt)
        return newRowId
    }
}
