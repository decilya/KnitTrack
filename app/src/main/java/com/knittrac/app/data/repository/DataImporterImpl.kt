package com.knittrac.app.data.repository

import com.knittrac.app.core.common.ErrorCode

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.knittrac.app.core.common.AppError
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
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.model.PatternRepeatWithId
import com.knittrac.app.domain.model.PatternRowWithId

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
                    readBackup(reader)
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

    private suspend fun readBackup(reader: JsonReader) {
        var version = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (val name = reader.nextName()) {
                "version" -> {
                    version = reader.nextString()
                    warnIfOld(version)
                }
                else -> readNamed(reader, name, version)
            }
        }
        reader.endObject()
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

    private suspend fun readNamed(reader: JsonReader, name: String, version: String) {
        if (readPatternArray(reader, name)) return
        readProjectArray(reader, name, version)
    }

    private suspend fun readPatternArray(reader: JsonReader, name: String): Boolean = when (name) {
        "patterns" -> readSaved(reader, PatternDto::class.java, { it.toDomain() }) {
            backupRepository.savePatternsBatch(it)
        }
        "patternRows" -> readSaved(reader, PatternRowDto::class.java, { dto -> PatternRowWithId(PatternId.createOrDefault(dto.patternId), dto.toDomain()) }) {
            backupRepository.savePatternRowsBatch(it)
        }
        "symbols" -> readSaved(reader, SymbolDto::class.java, { it.toDomain() }) {
            backupRepository.saveSymbolsBatch(it)
        }
        "patternRepeats" -> readSaved(reader, PatternRepeatDto::class.java, { dto -> PatternRepeatWithId(PatternId.createOrDefault(dto.patternId), dto.toDomain()) }) {
            backupRepository.savePatternRepeatsBatch(it)
        }
        "patternOperations" -> readSaved(reader, PatternOperationDto::class.java, { it.toDomain() }) {
            backupRepository.savePatternOperationsBatch(it)
        }
        else -> false
    }

    private suspend fun <D, T> readSaved(
        reader: JsonReader,
        dtoClass: Class<D>,
        map: (D) -> T,
        save: suspend (List<T>) -> Unit
    ): Boolean {
        readBatch(reader, dtoClass, map, save)
        return true
    }

    private suspend fun readProjectArray(reader: JsonReader, name: String, version: String) {
        when (name) {
            "exportDate" -> reader.skipValue()
            "projects" -> readBatch(reader, ProjectDto::class.java, { dto ->
                val safe = if (version == ExportDataDto.CURRENT_VERSION) dto else dto.withoutPatternLink()
                safe.toDomain()
            }) { backupRepository.saveProjectsBatch(it) }
            "sessions" -> readBatch(reader, SessionDto::class.java, { it.toDomain() }) {
                backupRepository.saveSessionsBatch(it)
            }
            else -> reader.skipValue()
        }
    }

    private suspend fun <D, T> readBatch(
        reader: JsonReader,
        dtoClass: Class<D>,
        map: (D) -> T,
        save: suspend (List<T>) -> Unit
    ) {
        val batch = mutableListOf<T>()
        reader.beginArray()
        while (reader.hasNext()) {
            batch.add(map(gson.fromJson(reader, dtoClass)))
            if (batch.size >= BATCH_SIZE) {
                save(batch.toList())
                batch.clear()
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) save(batch.toList())
    }

    /** Старый бэкап не содержит рядов. Триггер требует номер 0, если ряда нет. */
    private fun ProjectDto.withoutPatternLink(): ProjectDto = copy(
        patternId = null,
        currentPatternRowId = null
    )
}
