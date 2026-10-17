package com.knittrac.app.data.repository

import androidx.room.withTransaction
import com.google.gson.stream.JsonWriter
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toDto
import com.knittrac.app.domain.repository.DataExporter
import timber.log.Timber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

/**
 * Реализация [DataExporter] с потоковой сериализацией через [JsonWriter].
 *
 * Преимущества перед загрузкой всего JSON в String:
 * - Heap-память: только текущий объект + 8 КБ буфера (вместо всего файла).
 * - Нет риска OutOfMemoryError на больших бэкапах.
 *
 * Атомарность: чтение projects и sessions обёрнуто в
 * [AppDatabase.withTransaction] — оба SELECT видят один снапшот БД.
 * Без этого между двумя запросами могла завершиться сессия, и в бэкапе
 * totalTimeSeconds проекта не совпал бы с суммой duration его сессий.
 *
 * Зависимости (DIP):
 * - [deps] — Parameter Object (DataExportDependencies): DAO, Gson, БД и TimeProvider.
 * - [dispatcher] — I/O-диспетчер, инжектится через [@IoDispatcher].
 */
class DataExporterImpl @Inject constructor(
    private val deps: DataExportDependencies,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : DataExporter {

    /**
     * Экспортирует все проекты и сессии в предоставленный поток.
     *
     * Поток НЕ закрывает — это ответственность вызывающей стороны.
     * При ошибке возвращает [Result.Error], поток остаётся в неопределённом
     * состоянии (вызывающая сторона должна закрыть его сама).
     *
     * @param outputStream Поток для записи.
     * @return [Result.Success] при успехе, [Result.Error] при сбое.
     */
    override suspend fun exportTo(outputStream: OutputStream): Result<Unit> =
        withContext(dispatcher) {
            try {
                val writer = JsonWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).apply {
                    isLenient = true
                }

                // Транзакция гарантирует, что projects и sessions читаются
                // из одного снапшота БД. I/O (JsonWriter) внутри транзакции —
                // допустимый trade-off, чтобы не загружать всё в память.
                deps.appDatabase.withTransaction {
                    writer.beginObject()
                    writer.name("version").value(ExportDataDto.CURRENT_VERSION)
                    writer.name("exportDate").value(deps.timeProvider.currentTimeMillis())

                    // Потоковая запись проектов
                    writer.name("projects").beginArray()
                    deps.projectDao.getAllProjects().forEach { entity ->
                        deps.gson.toJson(entity.toDomain().toDto(), ProjectDto::class.java, writer)
                    }
                    writer.endArray()

                    // Потоковая запись сессий
                    writer.name("sessions").beginArray()
                    deps.sessionDao.getAllSessions().forEach { entity ->
                        deps.gson.toJson(entity.toDomain().toDto(), SessionDto::class.java, writer)
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
