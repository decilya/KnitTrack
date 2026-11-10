package com.knittrac.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRepeat
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.entity.Symbol
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

    override suspend fun savePatternsBatch(patterns: List<Pattern>) {
        val patternDao = database.patternDao()
        for (pattern in patterns) {
            patternDao.insertPattern(pattern.toEntity())
        }
    }

    override suspend fun savePatternRowsBatch(rows: List<Pair<Long, PatternRow>>) {
        database.patternDao().insertRows(rows.map { (patternId, row) -> row.toEntity(patternId) })
    }

    override suspend fun saveSymbolsBatch(symbols: List<Symbol>) {
        database.symbolDao().insertSymbols(symbols.map { it.toEntity() })
    }

    override suspend fun savePatternRepeatsBatch(repeats: List<Pair<Long, PatternRepeat>>) {
        database.patternRepeatDao().insertRepeats(
            repeats.map { (patternId, repeat) -> repeat.toEntity(patternId) }
        )
    }

    override suspend fun savePatternOperationsBatch(operations: List<PatternOperation>) {
        try {
            database.patternDao().insertOperations(operations.map { it.toEntity() })
        } catch (e: SQLiteConstraintException) {
            // Нарушение FK (row/symbol) или триггера repeat_code.
            // Для Application-слоя это единая семантика: бэкап содержит
            // невалидные операции.
            throw InvalidBackupOperationsException(e)
        }
    }

    override suspend fun saveProjectsBatch(batch: List<Project>) {
        projectDao.insertProjects(batch.map { it.toEntity() })
    }

    override suspend fun saveSessionsBatch(batch: List<Session>) {
        sessionDao.insertSessions(batch.map { it.toEntity() })
    }

    override suspend fun <T> exportSnapshot(block: suspend (BackupRepository.ExportContext) -> T): T {
        return database.withTransaction { block(SnapshotContext()) }
    }

    private inner class SnapshotContext : BackupRepository.ExportContext {
        override suspend fun forEachPattern(action: suspend (Pattern) -> Unit) {
            for (entity in database.patternDao().getAllPatterns()) {
                action(entity.toDomain(emptyList()))
            }
        }

        override suspend fun forEachPatternRow(
            action: suspend (patternId: Long, row: PatternRow) -> Unit
        ) {
            for (entity in database.patternDao().getAllRows()) {
                action(entity.patternId, entity.toDomain())
            }
        }

        override suspend fun forEachSymbol(action: suspend (Symbol) -> Unit) {
            for (entity in database.symbolDao().getAllSymbols()) {
                action(entity.toDomain())
            }
        }

        override suspend fun forEachPatternRepeat(
            action: suspend (patternId: Long, repeat: PatternRepeat) -> Unit
        ) {
            for (entity in database.patternRepeatDao().getAllRepeats()) {
                action(entity.patternId, entity.toDomain())
            }
        }

        override suspend fun forEachPatternOperation(action: suspend (PatternOperation) -> Unit) {
            val symbols = database.symbolDao().getAllSymbols().associateBy { it.code }
            for (entity in database.patternDao().getAllOperations()) {
                val symbol = symbols[entity.symbolCode]?.toDomain()
                    ?: missingSymbol(entity.symbolCode)
                val back = entity.backSymbolCode?.let { code ->
                    symbols[code]?.toDomain() ?: missingSymbol(code)
                }
                action(entity.toDomain(symbol, back))
            }
        }

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

        private fun missingSymbol(code: String): Nothing =
            throw IllegalStateException("Символ $code не найден при экспорте операции")
    }
}
