package com.knittrac.app.data.local.dao
import androidx.room.*
import com.knittrac.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC") fun getAllProjects(): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun getProjectById(id: Long): ProjectEntity?
    @Insert suspend fun insert(project: ProjectEntity): Long
    @Update suspend fun update(project: ProjectEntity)
    @Query("UPDATE projects SET totalTimeSeconds = totalTimeSeconds + :durationSeconds, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :projectId") suspend fun updateTotalTime(projectId: Long, durationSeconds: Long, updatedAt: Long)
    @Delete suspend fun delete(project: ProjectEntity)
}
