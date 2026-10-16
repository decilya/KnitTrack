package com.knittrac.app.core.logging

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
 * Асинхронность: [log] не делает I/O. Сообщение кладётся в неограниченный
 * [Channel] через trySend, а отдельный consumer-корутин на выделенном
 * диспетчере (по умолчанию [Dispatchers.IO]) читает канал и пишет в файл.
 * Это защищает Main-поток и потоки корутин от блокировки на диске —
 * I/O может занять 10-50 мс на медленном eMMC или при ротации файлов.
 *
 * Особенности:
 * - Фильтр по уровню: пишутся только записи >= [minLevel] (по умолчанию WARN).
 * - Ротация: при превышении [maxFileSizeBytes] файл переименовывается в .1,
 *   старые копии сдвигаются. Хранится не больше [maxFiles] файлов.
 * - Безопасность: любые ошибки записи (диск полон, нет прав, гонка)
 *   проглатываются. Логирование не должно крашить приложение.
 * - Потокобезопасность: I/O сериализуется одним consumer-корутином,
 *   дополнительная синхронизация не нужна.
 * - Порядок не гарантируется между разными потоками-писателями
 *   (trySend не даёт детерминированного порядка), но это приемлемо
 *   для диагностических логов.
 *
 * ВАЖНО: последние записи могут не успеть на диск, если процесс убит
 * сразу после Timber.e. Логи — вспомогательный канал, не user data.
 *
 * @param logDir Директория для файлов логов. Создаётся при необходимости.
 * @param minLevel Минимальный уровень для записи (Log.WARN по умолчанию).
 * @param maxFileSizeBytes Максимальный размер одного файла до ротации.
 * @param maxFiles Сколько файлов хранить (включая текущий).
 * @param dispatcher Диспетчер для consumer-корутина. По умолчанию [Dispatchers.IO],
 *   в тестах подменяется на тестовый диспетчер.
 */
class FileLoggingTree(
    private val logDir: File,
    private val minLevel: Int = Log.WARN,
    private val maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
    private val maxFiles: Int = DEFAULT_MAX_FILES,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
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

    /**
     * Запись лога, передаваемая из [log] в consumer через канал.
     *
     * Отдельный data class, потому что Timber.Tree не даёт структуры
     * для сообщения — только четыре аргумента. Собираем их в один
     * объект, чтобы положить в канал одним trySend.
     */
    private data class LogEntry(
        val priority: Int,
        val tag: String?,
        val message: String,
        val throwable: Throwable?
    )

    private val timestampFormat = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)
    private val file: File = File(logDir, LOG_FILE_NAME)

    /**
     * Неограниченный канал: логи не должны терять сообщения при переполнении.
     * UNLIMITED выбран осознанно — CONFLATED теряет промежуточные,
     * BUFFERED при переполнении возвращает failure из trySend.
     */
    private val channel = Channel<LogEntry>(Channel.UNLIMITED)

    /**
     * Scope для consumer-корутина. SupervisorJob — чтобы падение на I/O
     * не отменяло scope, если в будущем появятся другие дочерние корутины.
     */
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    init {
        scope.launch {
            for (entry in channel) {
                writeEntry(entry)
            }
        }
    }

    /**
     * Фильтрует по уровню. Timber вызывает [log] только если здесь true.
     */
    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= minLevel

    /**
     * Кладёт запись в канал. Не выполняет I/O, не блокирует вызывающий поток.
     *
     * [Channel.trySend] — атомарная неблокирующая операция. При закрытом
     * канале возвращает failure, который мы игнорируем: если tree закрыт,
     * логи больше не пишутся.
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        channel.trySend(LogEntry(priority, tag, message, t))
    }

    /**
     * Закрывает канал. Consumer завершается после обработки оставшихся
     * сообщений. Явный вызов нужен только в тестах и при graceful shutdown;
     * в приложении tree живёт весь жизненный цикл процесса.
     */
    fun close() {
        channel.close()
    }

    /**
     * Записывает одну запись на диск. Вызывается только из consumer-корутина.
     */
    private fun writeEntry(entry: LogEntry) {
        try {
            ensureLogDir()
            rotateIfNeeded()
            file.appendText(
                formatEntry(entry.priority, entry.tag, entry.message, entry.throwable),
                Charsets.UTF_8
            )
        } catch (_: IOException) {
            // Логирование не должно крашить приложение — даже если диск полон.
        } catch (_: Exception) {
            // Catch всех остальных исключений: если consumer упадёт от
            // RuntimeException, канал перестанет читаться и начнёт расти
            // в памяти. Логирование никогда не должно ронять приложение.
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

        // Порядок важен: сначала освобождаем самые старые слоты
        // (shift down), только потом переименовываем текущий в .1.
        // Обратный порядок затёр бы существующий .1 до того, как он
        // сдвинулся в .2.
        val oldest = File(logDir, LOG_FILE_NAME + ROTATION_SUFFIX.format(maxFiles - 1))
        if (oldest.exists()) oldest.delete()

        for (i in (maxFiles - 2) downTo 1) {
            val src = File(logDir, LOG_FILE_NAME + ROTATION_SUFFIX.format(i))
            val dst = File(logDir, LOG_FILE_NAME + ROTATION_SUFFIX.format(i + 1))
            if (src.exists()) src.renameTo(dst)
        }

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
