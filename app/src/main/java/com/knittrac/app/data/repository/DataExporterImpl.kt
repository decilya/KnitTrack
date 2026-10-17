package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
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
                    writer.beginObject()
                    writer.name("version").value(ExportDataDto.CURRENT_VERSION)
                    writer.name("exportDate").value(timeProvider.currentTimeMillis())

                    writer.name("projects").beginArray()
                    context.forEachProject { project ->
                        gson.toJson(project.toDto(), ProjectDto::class.java, writer)
                    }
                    writer.endArray()

                    writer.name("sessions").beginArray()
                    context.forEachSession { session ->
                        gson.toJson(session.toDto(), SessionDto::class.java, writer)
                    }
                    writer.endArray()

                    writer.endObject()
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
}
