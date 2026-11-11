package com.knittrac.app.data.parser

import java.io.InputStream
import java.io.OutputStream

/**
 * Парсер/сериализатор бэкапа.
 *
 * Абстрагирует JSON-библиотеку (Gson, kotlinx.serialization) от слоя Application.
 */
interface BackupJsonParser {
    /**
     * Читает бэкап из [inputStream] и передает данные в [visitor].
     */
    suspend fun parse(inputStream: InputStream, visitor: BackupImportVisitor)

    /**
     * Записывает бэкап в [outputStream], беря данные из [source].
     */
    suspend fun write(outputStream: OutputStream, source: BackupExportSource)
}
