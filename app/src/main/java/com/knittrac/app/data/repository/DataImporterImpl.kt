package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.BackupRepository
import com.knittrac.app.domain.repository.DataImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * Реализация [DataImporter] с потоковым парсингом через [JsonReader].
 * 
 * Делегирует работу с БД в [BackupRepository], оставляя за собой только JSON.
 */
class DataImporterImpl @Inject constructor(
    private val backupRepository: BackupRepository,
    private val gson: Gson,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataImporter {

    companion object {
        private const val BATCH_SIZE = 100
    }

    override suspend fun importFrom(inputStream: InputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                val reader = JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).apply {
                    isLenient = true
                }

                backupRepository.withTransaction {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "version" -> {
                                val version = reader.nextString()
                                if (version != ExportDataDto.CURRENT_VERSION) {
                                    Timber.w("Backup version '%s' != current '%s'", version, ExportDataDto.CURRENT_VERSION)
                                }
                            }
                            "exportDate" -> reader.skipValue()
                            "projects" -> readAndInsertProjects(reader)
                            "sessions" -> readAndInsertSessions(reader)
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
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
            } catch (e: Exception) {
                Timber.e(e, "importFrom failed")
                Result.Error(AppError.DatabaseError(e))
            }
        }

    private suspend fun readAndInsertProjects(reader: JsonReader) {
        val batch = mutableListOf<Project>()
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = gson.fromJson<ProjectDto>(reader, ProjectDto::class.java)
            batch.add(dto.toDomain())
            if (batch.size >= BATCH_SIZE) {
                backupRepository.saveProjectsBatch(batch)
                batch.clear()
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) backupRepository.saveProjectsBatch(batch)
    }

    private suspend fun readAndInsertSessions(reader: JsonReader) {
        val batch = mutableListOf<Session>()
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = gson.fromJson<SessionDto>(reader, SessionDto::class.java)
            batch.add(dto.toDomain())
            if (batch.size >= BATCH_SIZE) {
                backupRepository.saveSessionsBatch(batch)
                batch.clear()
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) backupRepository.saveSessionsBatch(batch)
    }
}
