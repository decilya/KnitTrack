package com.knittrac.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.knittrac.app.domain.entity.Project
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс доступа к данным для сущности Project.
 */
@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): Project?

    @Query("SELECT * FROM projects WHERE sync_status = 'PENDING'")
    suspend fun getPendingProjects(): List<Project>

    @Query("UPDATE projects SET sync_status = :status WHERE id = :projectId")
    suspend fun updateSyncStatus(projectId: Long, status: String)
}
