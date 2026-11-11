package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.PatternDto
import com.knittrac.app.data.dto.PatternOperationDto
import com.knittrac.app.data.dto.PatternRepeatDto
import com.knittrac.app.data.dto.PatternRowDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.data.dto.SymbolDto
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.parser.BackupImportVisitor
import com.knittrac.app.data.parser.BackupJsonParser
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.model.PatternRepeatWithId
import com.knittrac.app.domain.model.PatternRowWithId
import com.knittrac.app.domain.repository.BackupRepository
import com.knittrac.app.domain.repository.DataImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Реализация [DataImporter].
 *
 * Оркестрирует транзакцию и маппинг DTO -> Domain, делегируя парсинг JSON
 * и работу с БД в [BackupJsonParser] и [BackupRepository].
 */
class DataImporterImpl @Inject constructor(
    private val backupRepository: BackupRepository,
    private val parser: BackupJsonParser,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataImporter {

    override suspend fun importFrom(inputStream: InputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                backupRepository.withTransaction {
                    val visitor = object : BackupImportVisitor {
                        var currentVersion = ""
                        override suspend fun onVersion(version: String) {
                            currentVersion = version
                            warnIfOld(version)
                        }

                        override suspend fun onPatternsBatch(batch: List<PatternDto>) {
                            backupRepository.savePatternsBatch(batch.map { it.toDomain() })
                        }

                        override suspend fun onPatternRowsBatch(batch: List<PatternRowDto>) {
                            backupRepository.savePatternRowsBatch(
                                batch.map { PatternRowWithId(PatternId.createOrDefault(it.patternId), it.toDomain()) }
                            )
                        }

                        override suspend fun onSymbolsBatch(batch: List<SymbolDto>) {
                            backupRepository.saveSymbolsBatch(batch.map { it.toDomain() })
                        }

                        override suspend fun onPatternRepeatsBatch(batch: List<PatternRepeatDto>) {
                            backupRepository.savePatternRepeatsBatch(
                                batch.map { PatternRepeatWithId(PatternId.createOrDefault(it.patternId), it.toDomain()) }
                            )
                        }

                        override suspend fun onPatternOperationsBatch(batch: List<PatternOperationDto>) {
                            backupRepository.savePatternOperationsBatch(batch.map { it.toDomain() })
                        }

                        override suspend fun onProjectsBatch(batch: List<ProjectDto>) {
                            val safe = batch.map { dto ->
                                val safeDto = if (currentVersion == ExportDataDto.CURRENT_VERSION) dto else dto.withoutPatternLink()
                                safeDto.toDomain()
                            }
                            backupRepository.saveProjectsBatch(safe)
                        }

                        override suspend fun onSessionsBatch(batch: List<SessionDto>) {
                            backupRepository.saveSessionsBatch(batch.map { it.toDomain() })
                        }
                    }
                    parser.parse(inputStream, visitor)
                }
                Result.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.e(e, "importFrom failed: I/O error")
                Result.Error(AppError.IoError(e))
            } catch (e: IllegalStateException) {
                Timber.e(e, "importFrom failed: malformed JSON")
                Result.Error(AppError.IoError(e))
            } catch (e: InvalidBackupOperationsException) {
                Timber.e(e, "importFrom failed: invalid operations in backup")
                Result.Error(AppError.ValidationError(ErrorCode.INVALID_BACKUP_OPERATIONS))
            } catch (e: Exception) {
                Timber.e(e, "importFrom failed")
                Result.Error(AppError.DatabaseError(e))
            }
        }

    private fun warnIfOld(version: String) {
        if (version != ExportDataDto.CURRENT_VERSION) {
            Timber.w(
                "Backup version '%s' != current '%s'; pattern links will be dropped",
                version,
                ExportDataDto.CURRENT_VERSION
            )
        }
    }

    /** Старый бэкап не содержит рядов. Триггер требует номер 0, если ряда нет. */
    private fun ProjectDto.withoutPatternLink(): ProjectDto = copy(
        patternId = null,
        currentPatternRowId = null
    )
}
