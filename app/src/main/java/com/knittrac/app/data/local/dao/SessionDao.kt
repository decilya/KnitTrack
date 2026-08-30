package com.knittrac.app.data.local.dao
import androidx.room.*
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.DailyStat
@Dao
interface SessionDao {
    @Insert suspend fun insert(session: SessionEntity): Long
    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startTimestamp DESC") suspend fun getSessionsByProjectId(projectId: Long): List<SessionEntity>
    @Query("SELECT * FROM sessions") suspend fun getAllSessions(): List<SessionEntity>
    @Query("SELECT (startTimestamp / 86400000) * 86400000 AS dayTimestamp, SUM(durationSeconds) AS totalSeconds FROM sessions WHERE projectId = :projectId GROUP BY dayTimestamp ORDER BY dayTimestamp ASC") suspend fun getDailyStats(projectId: Long): List<DailyStat>
}
