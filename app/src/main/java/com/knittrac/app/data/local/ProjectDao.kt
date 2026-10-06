package com.knittrac.app.data.local

import androidx.room.*
import com.knittrac.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>): List<Long>

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("""
        UPDATE projects 
        SET total_time_seconds = total_time_seconds + :durationSeconds, 
            updated_at = :updatedAt, 
            sync_status = 'PENDING' 
        WHERE id = :projectId
    """)
    suspend fun updateTotalTime(projectId: Long, durationSeconds: Long, updatedAt: Long)
}
