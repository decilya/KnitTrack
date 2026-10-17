package com.knittrac.app.data.repository

import androidx.room.withTransaction
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [BackupRepository] на базе Room.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val projectDao: ProjectDao,
    private val sessionDao: SessionDao
) : BackupRepository {

    override suspend fun <T> withTransaction(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }

    override suspend fun saveProjectsBatch(batch: List<Project>) {
        projectDao.insertProjects(batch.map { it.toEntity() })
    }

    override suspend fun saveSessionsBatch(batch: List<Session>) {
        sessionDao.insertSessions(batch.map { it.toEntity() })
    }

    override suspend fun <T> exportSnapshot(block: suspend (BackupRepository.ExportContext) -> T): T {
        return database.withTransaction {
            val context = object : BackupRepository.ExportContext {
                override suspend fun forEachProject(action: suspend (Project) -> Unit) {
                    for (entity in projectDao.getAllProjects()) {
                        action(entity.toDomain())
                    }
                }
                override suspend fun forEachSession(action: suspend (Session) -> Unit) {
                    for (entity in sessionDao.getAllSessions()) {
                        action(entity.toDomain())
                    }
                }
            }
            block(context)
        }
    }
}
