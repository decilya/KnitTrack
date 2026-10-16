package com.knittrac.app.core.logging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Юнит-тесты для [FileLoggingTree].
 *
 * Покрывают: фильтрацию по уровню, запись WARN/ERROR со stacktrace,
 * устойчивость к I/O-ошибкам (не должно бросать), ротацию файлов,
 * неблокирующее поведение [FileLoggingTree.log].
 *
 * Асинхронность: запись выполняется consumer-корутином на тестовом
 * диспетчере. Чтобы assertion увидел результат, после каждого вызова
 * [FileLoggingTree.log] идёт [advanceUntilIdle] — он прогоняет
 * запланированные корутины до конца.
 *
 * Тесты не зависят от Android SDK: Timber.Tree.w/e — публичные методы,
 * не требующие Context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FileLoggingTreeTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("knittrac-log-test").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    /**
     * Создаёт tree на тестовом диспетчере.
     */
    private fun createTree(
        dir: File = tempDir,
        maxFileSizeBytes: Long = FileLoggingTree.DEFAULT_MAX_FILE_SIZE_BYTES,
        maxFiles: Int = FileLoggingTree.DEFAULT_MAX_FILES,
        testDispatcher: TestDispatcher
    ): FileLoggingTree = FileLoggingTree(
        logDir = dir,
        maxFileSizeBytes = maxFileSizeBytes,
        maxFiles = maxFiles,
        dispatcher = testDispatcher
    )

    /**
     * DEBUG и INFO ниже порога — Timber не вызывает log(),
     * файл не создаётся или остаётся пустым.
     */
    @Test
    fun debugAndInfoAreNotWritten() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tree = createTree(testDispatcher = dispatcher)

        tree.v("verbose msg")
        tree.d("debug msg")
        tree.i("info msg")
        advanceUntilIdle()

        val logFile = File(tempDir, "knittrac.log")
        val content = if (logFile.exists()) logFile.readText() else ""
        assertTrue("log should be empty, got: '$content'", content.isEmpty())
    }

    /**
     * Warning записывается в knittrac.log с уровнем W и сообщением.
     */
    @Test
    fun log_writesWarningToFile() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tree = createTree(testDispatcher = dispatcher)

        tree.w("test warning")
        advanceUntilIdle()

        val logFile = File(tempDir, "knittrac.log")
        assertTrue("log file should exist", logFile.exists())
        val content = logFile.readText()
        assertTrue("should contain W/ level", content.contains("W/"))
        assertTrue("should contain message", content.contains("test warning"))
    }

    /**
     * Error записывается со stacktrace исключения.
     */
    @Test
    fun log_writesErrorWithStacktrace() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tree = createTree(testDispatcher = dispatcher)

        val exception = RuntimeException("boom")
        tree.e(exception, "test error")
        advanceUntilIdle()

        val content = File(tempDir, "knittrac.log").readText()
        assertTrue("should contain E/ level", content.contains("E/"))
        assertTrue("should contain message", content.contains("test error"))
        assertTrue("should contain exception class", content.contains("RuntimeException"))
        assertTrue("should contain exception message", content.contains("boom"))
    }

    /**
     * I/O-ошибка при записи не пробрасывается наружу.
     *
     * Сценарий: logDir — это существующий файл, а не директория.
     * appendText к дочернему пути бросит FileNotFoundException (IOException),
     * который дерево должно проглотить.
     */
    @Test
    fun log_swallowsIOExceptionOnWrite() = runTest {
        val blockedDir = File(tempDir, "blocked")
        blockedDir.writeText("this is a file, not a directory")

        val dispatcher = StandardTestDispatcher(testScheduler)
        val badTree = FileLoggingTree(
            logDir = blockedDir,
            dispatcher = dispatcher
        )

        // Если бы IOException не была проглочена в consumer, runTest упал бы
        // после advanceUntilIdle.
        badTree.w("should be swallowed")
        advanceUntilIdle()
    }

    /**
     * При превышении лимита файл сдвигается в .1, старые копии сдвигаются.
     * Общее число файлов не превышает maxFiles.
     */
    @Test
    fun rotateIfNeeded_shiftsFilesWhenExceeded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val smallTree = createTree(
            maxFileSizeBytes = 100L,
            maxFiles = 3,
            testDispatcher = dispatcher
        )

        // 20 записей по ~50+ байт переполнят 100-байтный лимит многократно.
        repeat(20) { smallTree.w("filler $it") }
        advanceUntilIdle()

        val base = File(tempDir, "knittrac.log")
        val rotated1 = File(tempDir, "knittrac.log.1")
        assertTrue("base file should exist", base.exists())
        assertTrue("rotated file .1 should exist", rotated1.exists())

        val count = tempDir.listFiles()?.size ?: 0
        assertTrue("no more than 3 files, found $count", count <= 3)
    }

    /**
     * log() не выполняет I/O синхронно: файл создаётся consumer-корутином,
     * а не вызывающим потоком.
     *
     * Регрессионный тест на исходную проблему: до перехода на Channel
     * log() писал в файл в вызывающем потоке. Тогда после 1000 вызовов
     * файл существовал бы до advanceUntilIdle — этот тест упал бы.
     */
    @Test
    fun log_doesNotBlockCaller() = runTest {
        val logDir = File(tempDir, "async-check")
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tree = FileLoggingTree(
            logDir = logDir,
            dispatcher = dispatcher
        )

        repeat(1000) { tree.w("rapid $it") }

        // Consumer ещё не стартовал — файла нет. При синхронной реализации
        // log() файл был бы создан уже здесь.
        val logFile = File(logDir, "knittrac.log")
        assertTrue(
            "log() must not perform I/O before advanceUntilIdle",
            !logFile.exists()
        )

        advanceUntilIdle()

        // После прогона consumer выполнил I/O — файл создан.
        assertTrue(
            "consumer must create log file after advanceUntilIdle",
            logFile.exists()
        )
    }
}
