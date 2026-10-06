package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.DataImporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [DataImporter] с потоковым парсингом через [JsonReader].
 *
 * Преимущества:
 * - Потоковое чтение: JSON не загружается в память целиком.
 * - Чанковая вставка: данные сохраняются в БД пакетами по [BATCH_SIZE] записей,
 *   что предотвращает OutOfMemoryError при импорте больших бэкапов.
 *
 * ВАЖНО: используется `OnConflictStrategy.REPLACE` — при импорте существующие
 * записи с теми же `id` будут перезаписаны. Это ожидаемое поведение для restore.
 *
 * Зависимости (DIP):
 * - [ProjectDao], [SessionDao] — сохранение данных в Room.
 * - [Gson] — парсинг отдельных объектов из потока.
 * - [dispatcher] — I/O-диспетчер, инжектится через [@IoDispatcher].
 */
@Singleton
class DataImporterImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val sessionDao: SessionDao,
    private val gson: Gson,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataImporter {

    companion object {
        /** Размер пакета для пакетной вставки в БД. */
        private const val BATCH_SIZE = 100
    }

    override suspend fun importFrom(inputStream: InputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                val reader = JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).apply {
                    isLenient = true
                }

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "projects" -> readAndInsertProjects(reader)
                        "sessions" -> readAndInsertSessions(reader)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(AppError.DatabaseError(e))
            }
        }

    /**
     * Потоково читает массив проектов и вставляет их в БД чанками по [BATCH_SIZE].
     */
    private suspend fun readAndInsertProjects(reader: JsonReader) {
        val batch = mutableListOf<ProjectEntity>()
        reader.beginArray()
        while (reader.hasNext()) {
            val project = gson.fromJson<Project>(reader, Project::class.java)
            batch.add(project.toEntity())
            if (batch.size >= BATCH_SIZE) {
                projectDao.insertProjects(batch)
                batch.clear()
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) projectDao.insertProjects(batch)
    }

    /**
     * Потоково читает массив сессий и вставляет их в БД чанками по [BATCH_SIZE].
     */
    private suspend fun readAndInsertSessions(reader: JsonReader) {
        val batch = mutableListOf<SessionEntity>()
        reader.beginArray()
        while (reader.hasNext()) {
            val session = gson.fromJson<Session>(reader, Session::class.java)
            batch.add(session.toEntity())
            if (batch.size >= BATCH_SIZE) {
                sessionDao.insertSessions(batch)
                batch.clear()
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) sessionDao.insertSessions(batch)
    }
}
