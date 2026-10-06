package com.knittrac.app.data.local

import androidx.room.*
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.DailyStat
import kotlinx.coroutines.flow.Flow

private const val MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000L

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
}
