package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toDto
import com.knittrac.app.domain.repository.DataExporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [DataExporter] с потоковой сериализацией через [JsonWriter].
 *
 * Преимущества перед загрузкой всего JSON в String:
 * - Heap-память: только текущий объект + 8 КБ буфера (вместо всего файла).
 * - Нет риска OutOfMemoryError на больших бэкапах.
 *
 * Зависимости (DIP):
 * - [ProjectDao], [SessionDao] — извлечение данных из Room.
 * - [Gson] — сериализация отдельных объектов.
 * - [dispatcher] — I/O-диспетчер, инжектится через [@IoDispatcher].
 */
@Singleton
class DataExporterImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val sessionDao: SessionDao,
    private val gson: Gson,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataExporter {

    override suspend fun exportTo(outputStream: OutputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                val writer = JsonWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).apply {
                    isLenient = true
                }

                writer.beginObject()
                writer.name("version").value(ExportDataDto.CURRENT_VERSION)
                writer.name("exportDate").value(System.currentTimeMillis())

                // Потоковая запись проектов
                writer.name("projects").beginArray()
                projectDao.getAllProjects().forEach { entity ->
                    gson.toJson(entity.toDomain().toDto(), ProjectDto::class.java, writer)
                }
                writer.endArray()

                // Потоковая запись сессий
                writer.name("sessions").beginArray()
                sessionDao.getAllSessions().forEach { entity ->
                    gson.toJson(entity.toDomain().toDto(), SessionDto::class.java, writer)
                }
                writer.endArray()

                writer.endObject()
                writer.flush()

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(AppError.DatabaseError(e))
            }
            // Поток НЕ закрываем — это ответственность вызывающей стороны (SettingsScreen)
        }
}
