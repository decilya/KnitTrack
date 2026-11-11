package com.knittrac.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.model.PatternRepeatWithId
import com.knittrac.app.domain.model.PatternRowWithId

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

    override suspend fun savePatternRowsBatch(rows: List<PatternRowWithId>) {
        database.patternDao().insertRows(rows.map { it.row.toEntity(it.patternId.value) })
    }

    override suspend fun saveSymbolsBatch(symbols: List<Symbol>) {
        database.symbolDao().insertSymbols(symbols.map { it.toEntity() })
    }

    override suspend fun savePatternRepeatsBatch(repeats: List<PatternRepeatWithId>) {
        database.patternRepeatDao().insertRepeats(
            repeats.map { it.repeat.toEntity(it.patternId.value) }
        )
    }

    override suspend fun savePatternOperationsBatch(operations: List<PatternOperation>): Result<Unit> {
        return try {
            database.patternDao().insertOperations(operations.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: SQLiteConstraintException) {
            // Нарушение FK (row/symbol) или триггера repeat_code.
            // Для Application-слоя это единая семантика: бэкап содержит
            // невалидные операции.
            Result.Error(AppError.ValidationError(ErrorCode.INVALID_BACKUP_OPERATIONS))
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

        override suspend fun forEachPatternRow(action: suspend (PatternRowWithId) -> Unit) {
            for (entity in database.patternDao().getAllRows()) {
                val patternId = PatternId.createOrDefault(entity.patternId)
                action(PatternRowWithId(patternId, entity.toDomain()))
            }
        }

        override suspend fun forEachSymbol(action: suspend (Symbol) -> Unit) {
            for (entity in database.symbolDao().getAllSymbols()) {
                action(entity.toDomain())
            }
        }

        override suspend fun forEachPatternRepeat(action: suspend (PatternRepeatWithId) -> Unit) {
            for (entity in database.patternRepeatDao().getAllRepeats()) {
                val patternId = PatternId.createOrDefault(entity.patternId)
                action(PatternRepeatWithId(patternId, entity.toDomain()))
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
