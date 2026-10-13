package com.knittrac.app.core.logging

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timber-дерево, пишущее WARN и ERROR в файл.
 *
 * Используется в release-сборках: [Timber.DebugTree] в проде недоступен,
 * а без дерева все вызовы Timber.e превращаются в no-op. Это дерево
 * сохраняет критичные логи на диск для последующей диагностики.
 *
 * Особенности:
 * - Фильтр по уровню: пишутся только записи >= [minLevel] (по умолчанию WARN).
 * - Ротация: при превышении [maxFileSizeBytes] файл переименовывается в .1,
 *   старые копии сдвигаются. Хранится не больше [maxFiles] файлов.
 * - Безопасность: любые ошибки записи (диск полон, нет прав, гонка)
 *   проглатываются. Логирование не должно крашить приложение.
 * - Потокобезопасность: запись защищена synchronized по this.
 *
 * @param logDir Директория для файлов логов. Создаётся при необходимости.
 * @param minLevel Минимальный уровень для записи (Log.WARN по умолчанию).
 * @param maxFileSizeBytes Максимальный размер одного файла до ротации.
 * @param maxFiles Сколько файлов хранить (включая текущий).
 */
class FileLoggingTree(
    private val logDir: File,
    private val minLevel: Int = Log.WARN,
    private val maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
    private val maxFiles: Int = DEFAULT_MAX_FILES
) : Timber.Tree() {

    companion object {
        /** Имя основного файла логов. */
        private const val LOG_FILE_NAME = "knittrac.log"

        /** 1 МБ — компромисс между глубиной истории и размером бэкапа. */
        const val DEFAULT_MAX_FILE_SIZE_BYTES = 1L * 1024 * 1024

        /** 3 файла: текущий + 2 архивных. */
        const val DEFAULT_MAX_FILES = 3

        private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
        private const val ROTATION_SUFFIX = ".%d"
        private const val FALLBACK_TAG = "KnitTrac"
    }

    private val timestampFormat = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)
    private val file: File = File(logDir, LOG_FILE_NAME)

    /**
     * Фильтрует по уровню. Timber вызывает [log] только если здесь true.
     */
    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= minLevel

    /**
     * Записывает сообщение в файл. Ошибки I/O проглатываются.
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            synchronized(this) {
                ensureLogDir()
                rotateIfNeeded()
                file.appendText(formatEntry(priority, tag, message, t), Charsets.UTF_8)
            }
        } catch (e: IOException) {
            // Логирование не должно крашить приложение — даже если диск полон.
        }
    }

    /**
     * Создаёт директорию логов, если её нет.
     */
    private fun ensureLogDir() {
        if (!logDir.exists()) logDir.mkdirs()
    }

    /**
     * Сдвигает файлы при превышении лимита.
     *
     * Схема: knittrac.log -> knittrac.log.1 -> knittrac.log.2 -> удаление.
     */
    private fun rotateIfNeeded() {
        if (!file.exists() || file.length() < maxFileSizeBytes) return

        // Удаляем самый старый (maxFiles - 1), чтобы не хранить лишнее.
        val oldest = File(logDir, LOG_FILE_NAME + ROTATION_SUFFIX.format(maxFiles - 1))
        if (oldest.exists()) oldest.delete()

        // Сдвигаем: .(N-2) -> .(N-1), ..., .1 -> .2
        for (i in (maxFiles - 2) downTo 1) {
            val src = File(logDir, LOG_FILE_NAME + ROTATION_SUFFIX.format(i))
            val dst = File(logDir, LOG_FILE_NAME + ROTATION_SUFFIX.format(i + 1))
            if (src.exists()) src.renameTo(dst)
        }

        // Текущий -> .1
        file.renameTo(File(logDir, LOG_FILE_NAME + ROTATION_SUFFIX.format(1)))
    }

    /**
     * Формирует одну запись лога.
     *
     * Формат: `2026-10-13 21:05:23.456 W/Tag: message` со stacktrace
     * (если есть) на следующей строке.
     */
    private fun formatEntry(priority: Int, tag: String?, message: String, t: Throwable?): String {
        val timestamp = timestampFormat.format(Date())
        val level = priorityToChar(priority)
        val tagPart = if (tag.isNullOrEmpty()) FALLBACK_TAG else tag
        val stack = t?.stackTraceToString() ?: ""
        return "$timestamp $level/$tagPart: $message\n$stack"
    }

    /**
     * Мапит приоритет в однобуквенный код (как в Logcat).
     */
    private fun priorityToChar(priority: Int): Char = when (priority) {
        Log.VERBOSE -> 'V'
        Log.DEBUG -> 'D'
        Log.INFO -> 'I'
        Log.WARN -> 'W'
        Log.ERROR -> 'E'
        Log.ASSERT -> 'A'
        else -> '?'
    }
}
