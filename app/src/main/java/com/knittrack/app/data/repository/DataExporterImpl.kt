package com.knittrack.app.data.repository

import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.common.TimeProvider
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.data.dto.ExportDataDto
import com.knittrack.app.data.dto.PatternDto
import com.knittrack.app.data.dto.PatternOperationDto
import com.knittrack.app.data.dto.PatternRepeatDto
import com.knittrack.app.data.dto.PatternRowDto
import com.knittrack.app.data.dto.ProjectDto
import com.knittrack.app.data.dto.SessionDto
import com.knittrack.app.data.dto.SymbolDto
import com.knittrack.app.data.mapper.toDto
import com.knittrack.app.data.parser.BackupExportSource
import com.knittrack.app.data.parser.BackupJsonParser
import com.knittrack.app.domain.repository.BackupRepository
import com.knittrack.app.domain.repository.DataExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/**
 * Реализация [DataExporter].
 *
 * Делегирует JSON-запись в [BackupJsonParser], а чтение из БД — в [BackupRepository].
 */
class DataExporterImpl @Inject constructor(
    private val backupRepository: BackupRepository,
    private val parser: BackupJsonParser,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataExporter {

    override suspend fun exportTo(outputStream: OutputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                backupRepository.exportSnapshot { context ->
                    val source = object : BackupExportSource {
                        override val version: String = ExportDataDto.CURRENT_VERSION
                        override val exportDate: Long = timeProvider.currentTimeMillis()

                        override suspend fun forEachPattern(action: suspend (PatternDto) -> Unit) {
                            context.forEachPattern { action(it.toDto()) }
                        }

                        override suspend fun forEachPatternRow(action: suspend (PatternRowDto) -> Unit) {
                            context.forEachPatternRow { action(it.row.toDto(it.patternId.value)) }
                        }

                        override suspend fun forEachSymbol(action: suspend (SymbolDto) -> Unit) {
                            context.forEachSymbol { action(it.toDto()) }
                        }

                        override suspend fun forEachPatternRepeat(action: suspend (PatternRepeatDto) -> Unit) {
                            context.forEachPatternRepeat { action(it.repeat.toDto(it.patternId.value)) }
                        }

                        override suspend fun forEachPatternOperation(action: suspend (PatternOperationDto) -> Unit) {
                            context.forEachPatternOperation { action(it.toDto()) }
                        }

                        override suspend fun forEachProject(action: suspend (ProjectDto) -> Unit) {
                            context.forEachProject { action(it.toDto()) }
                        }

                        override suspend fun forEachSession(action: suspend (SessionDto) -> Unit) {
                            context.forEachSession { action(it.toDto()) }
                        }
                    }
                    parser.write(outputStream, source)
                }
                Result.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.e(e, "exportTo failed: I/O error")
                Result.Error(AppError.IoError(e))
            } catch (e: Exception) {
                Timber.e(e, "exportTo failed")
                Result.Error(AppError.DatabaseError(e))
            }
        }
}
