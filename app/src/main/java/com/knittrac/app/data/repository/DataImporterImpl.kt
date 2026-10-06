package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
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
 * - Heap: только текущий объект (вместо загрузки всего JSON в память).
 * - Нет риска OOM на больших бэкапах.
 *
 * Данные вставляются пачками для эффективности Room.
 */
@Singleton
class DataImporterImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val sessionDao: SessionDao,
    private val gson: Gson,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataImporter {

    override suspend fun importFrom(inputStream: InputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                val reader = JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).apply {
                    isLenient = true
                }

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "projects" -> {
                            val projects = mutableListOf<com.knittrac.app.data.local.entity.ProjectEntity>()
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val project = gson.fromJson<Project>(reader, Project::class.java)
                                projects.add(project.toEntity())
                            }
                            reader.endArray()
                            if (projects.isNotEmpty()) {
                                projectDao.insertProjects(projects)
                            }
                        }
                        "sessions" -> {
                            val sessions = mutableListOf<com.knittrac.app.data.local.entity.SessionEntity>()
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val session = gson.fromJson<Session>(reader, Session::class.java)
                                sessions.add(session.toEntity())
                            }
                            reader.endArray()
                            if (sessions.isNotEmpty()) {
                                sessionDao.insertSessions(sessions)
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(AppError.DatabaseError(e))
            }
            // Поток НЕ закрываем — это ответственность вызывающей стороны (SettingsScreen)
        }
}
