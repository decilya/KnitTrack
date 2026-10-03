package com.knittrac.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.knittrac.app.domain.entity.Session
import kotlinx.coroutines.flow.Flow

// Константы времени для читаемости SQL-запросов
private const val MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000L
private const val MILLIS_IN_A_SECOND = 1000L

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Query("SELECT * FROM sessions WHERE project_id = :projectId ORDER BY start_time DESC")
    fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE project_id = :projectId ORDER BY start_time DESC")
    suspend fun getSessionsByProjectId(projectId: Long): List<Session>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<Session>

    @Query("SELECT * FROM sessions WHERE sync_status = 'PENDING'")
    suspend fun getPendingSessions(): List<Session>

    @Query("UPDATE sessions SET sync_status = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: Long, status: String)

    /**
     * Агрегирует время сессий по дням для конкретного проекта.
     * dayTimestamp - начало дня в миллисекундах.
     * totalSeconds - сумма длительностей сессий в этот день в секундах.
     */
    @Query("""
        SELECT 
            (start_time / $MILLIS_IN_A_DAY) * $MILLIS_IN_A_DAY AS dayTimestamp, 
            SUM((end_time - start_time) / $MILLIS_IN_A_SECOND) AS totalSeconds 
        FROM sessions 
        WHERE project_id = :projectId 
        GROUP BY dayTimestamp 
        ORDER BY dayTimestamp DESC
    """)
    suspend fun getDailyStats(projectId: Long): List<com.knittrac.app.domain.entity.DailyStat>
}
