package com.knittrac.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.knittrac.app.domain.entity.Session
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс доступа к данным для сущности Session.
 */
@Dao
interface SessionDao {

    /**
     * Вставляет новую сессию в базу данных.
     * @return ID созданной сессии.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    /**
     * Получает поток всех сессий для конкретного проекта.
     * Идеально подходит для отображения статистики в реальном времени.
     */
    @Query("SELECT * FROM sessions WHERE project_id = :projectId ORDER BY start_time DESC")
    fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<Session>>

    /**
     * Получает все сессии, ожидающие синхронизации.
     */
    @Query("SELECT * FROM sessions WHERE sync_status = 'PENDING'")
    suspend fun getPendingSessions(): List<Session>

    /**
     * Обновляет статус синхронизации для конкретной сессии.
     */
    @Query("UPDATE sessions SET sync_status = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: Long, status: String)
}
