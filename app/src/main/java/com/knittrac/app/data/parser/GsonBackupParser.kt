package com.knittrac.app.data.parser

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.knittrac.app.data.dto.PatternDto
import com.knittrac.app.data.dto.PatternOperationDto
import com.knittrac.app.data.dto.PatternRepeatDto
import com.knittrac.app.data.dto.PatternRowDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.data.dto.SymbolDto
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

/**
 * Реализация [BackupJsonParser] на базе Gson с потоковой обработкой.
 */
class GsonBackupParser @Inject constructor(
    private val gson: Gson
) : BackupJsonParser {

    /** Размер батча при потоковом чтении/записи. */
    companion object {
        private const val BATCH_SIZE = 100
    }

    override suspend fun parse(inputStream: InputStream, visitor: BackupImportVisitor) {
        val reader = JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).apply {
            isLenient = true
        }
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "version" -> visitor.onVersion(reader.nextString())
                "exportDate" -> reader.skipValue()
                "patterns" -> readBatch(reader, PatternDto::class.java) { visitor.onPatternsBatch(it) }
                "patternRows" -> readBatch(reader, PatternRowDto::class.java) { visitor.onPatternRowsBatch(it) }
                "symbols" -> readBatch(reader, SymbolDto::class.java) { visitor.onSymbolsBatch(it) }
                "patternRepeats" -> readBatch(reader, PatternRepeatDto::class.java) { visitor.onPatternRepeatsBatch(it) }
                "patternOperations" -> readBatch(reader, PatternOperationDto::class.java) { visitor.onPatternOperationsBatch(it) }
                "projects" -> readBatch(reader, ProjectDto::class.java) { visitor.onProjectsBatch(it) }
                "sessions" -> readBatch(reader, SessionDto::class.java) { visitor.onSessionsBatch(it) }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    override suspend fun write(outputStream: OutputStream, source: BackupExportSource) {
        val writer = JsonWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).apply {
            isLenient = true
        }
        writer.beginObject()
        writer.name("version").value(source.version)
        writer.name("exportDate").value(source.exportDate)
        writer.writeItems("patterns") { source.forEachPattern { gson.toJson(it, PatternDto::class.java, this) } }
        writer.writeItems("patternRows") { source.forEachPatternRow { gson.toJson(it, PatternRowDto::class.java, this) } }
        writer.writeItems("symbols") { source.forEachSymbol { gson.toJson(it, SymbolDto::class.java, this) } }
        writer.writeItems("patternRepeats") { source.forEachPatternRepeat { gson.toJson(it, PatternRepeatDto::class.java, this) } }
        writer.writeItems("patternOperations") { source.forEachPatternOperation { gson.toJson(it, PatternOperationDto::class.java, this) } }
        writer.writeItems("projects") { source.forEachProject { gson.toJson(it, ProjectDto::class.java, this) } }
        writer.writeItems("sessions") { source.forEachSession { gson.toJson(it, SessionDto::class.java, this) } }
        writer.endObject()
        writer.flush()
    }

    private suspend fun <D> readBatch(reader: JsonReader, dtoClass: Class<D>, save: suspend (List<D>) -> Unit) {
        val batch = mutableListOf<D>()
        reader.beginArray()
        while (reader.hasNext()) {
            batch.add(gson.fromJson(reader, dtoClass))
            if (batch.size >= BATCH_SIZE) {
                save(batch.toList())
                batch.clear()
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) save(batch.toList())
    }

    private suspend fun JsonWriter.writeItems(arrayName: String, block: suspend JsonWriter.() -> Unit) {
        name(arrayName)
        beginArray()
        block()
        endArray()
    }
}
