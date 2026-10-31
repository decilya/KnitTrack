package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.PatternDto
import com.knittrac.app.data.dto.PatternOperationDto
import com.knittrac.app.data.dto.PatternRepeatDto
import com.knittrac.app.data.dto.PatternRowDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.data.dto.SymbolDto
import com.knittrac.app.data.mapper.toDto
import com.knittrac.app.domain.repository.BackupRepository
import com.knittrac.app.domain.repository.DataExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

/**
 * Реализация [DataExporter] с потоковой сериализацией через [JsonWriter].
 * 
 * Делегирует работу с БД в [BackupRepository], оставляя за собой только JSON.
 */
class DataExporterImpl @Inject constructor(
    private val backupRepository: BackupRepository,
    private val gson: Gson,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataExporter {

    override suspend fun exportTo(outputStream: OutputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                val writer = JsonWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).apply {
                    isLenient = true
                }

                backupRepository.exportSnapshot { context ->
                    writeBackup(writer, context)
                }
                writer.flush()

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

    /**
     * Порядок ключей — порядок FK при импорте.
     * Схемы раньше проектов, сессии последними.
     */
    private suspend fun writeBackup(
        writer: JsonWriter,
        context: BackupRepository.ExportContext
    ) {
        writer.beginObject()
        writer.name("version").value(ExportDataDto.CURRENT_VERSION)
        writer.name("exportDate").value(timeProvider.currentTimeMillis())
        writePatternTables(writer, context)
        writeProjectTables(writer, context)
        writer.endObject()
    }

    private suspend fun writePatternTables(
        writer: JsonWriter,
        context: BackupRepository.ExportContext
    ) {
        writer.writeItems("patterns") {
            context.forEachPattern { gson.toJson(it.toDto(), PatternDto::class.java, this) }
        }
        writer.writeItems("patternRows") {
            context.forEachPatternRow { patternId, row ->
                gson.toJson(row.toDto(patternId), PatternRowDto::class.java, this)
            }
        }
        writer.writeItems("symbols") {
            context.forEachSymbol { gson.toJson(it.toDto(), SymbolDto::class.java, this) }
        }
        writer.writeItems("patternRepeats") {
            context.forEachPatternRepeat { patternId, repeat ->
                gson.toJson(repeat.toDto(patternId), PatternRepeatDto::class.java, this)
            }
        }
        writer.writeItems("patternOperations") {
            context.forEachPatternOperation {
                gson.toJson(it.toDto(), PatternOperationDto::class.java, this)
            }
        }
    }

    private suspend fun writeProjectTables(
        writer: JsonWriter,
        context: BackupRepository.ExportContext
    ) {
        writer.writeItems("projects") {
            context.forEachProject { gson.toJson(it.toDto(), ProjectDto::class.java, this) }
        }
        writer.writeItems("sessions") {
            context.forEachSession { gson.toJson(it.toDto(), SessionDto::class.java, this) }
        }
    }

    private suspend fun JsonWriter.writeItems(
        arrayName: String,
        block: suspend JsonWriter.() -> Unit
    ) {
        name(arrayName)
        beginArray()
        block()
        endArray()
    }
}
