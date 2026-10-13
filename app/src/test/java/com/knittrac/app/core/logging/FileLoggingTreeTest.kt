package com.knittrac.app.core.logging

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
 * устойчивость к I/O-ошибкам (не должно бросать), ротацию файлов.
 *
 * Тесты не зависят от Android SDK: Timber.Tree.w/e — публичные методы,
 * не требующие Context. Проверяется поведение (что записано в файл),
 * а не protected-метод isLoggable — он недоступен из другого пакета.
 */
class FileLoggingTreeTest {

    private lateinit var tempDir: File
    private lateinit var tree: FileLoggingTree

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("knittrac-log-test").toFile()
        tree = FileLoggingTree(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    /**
     * DEBUG и INFO ниже порога — Timber не вызывает log(),
     * файл не создаётся или остаётся пустым.
     */
    @Test
    fun debugAndInfoAreNotWritten() {
        tree.v("verbose msg")
        tree.d("debug msg")
        tree.i("info msg")

        val logFile = File(tempDir, "knittrac.log")
        val content = if (logFile.exists()) logFile.readText() else ""
        assertTrue("log should be empty, got: '$content'", content.isEmpty())
    }

    /**
     * Warning записывается в knittrac.log с уровнем W и сообщением.
     */
    @Test
    fun log_writesWarningToFile() {
        tree.w("test warning")

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
    fun log_writesErrorWithStacktrace() {
        val exception = RuntimeException("boom")
        tree.e(exception, "test error")

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
    fun log_swallowsIOExceptionOnWrite() {
        val blockedDir = File(tempDir, "blocked")
        blockedDir.writeText("this is a file, not a directory")

        val badTree = FileLoggingTree(blockedDir)
        // Если бы IOException не была проглочена, тест упал бы здесь.
        badTree.w("should be swallowed")
    }

    /**
     * При превышении лимита файл сдвигается в .1, старые копии сдвигаются.
     * Общее число файлов не превышает maxFiles.
     */
    @Test
    fun rotateIfNeeded_shiftsFilesWhenExceeded() {
        val smallTree = FileLoggingTree(
            logDir = tempDir,
            maxFileSizeBytes = 100L,
            maxFiles = 3
        )

        // 20 записей по ~50+ байт переполнят 100-байтный лимит многократно.
        repeat(20) { smallTree.w("filler $it") }

        val base = File(tempDir, "knittrac.log")
        val rotated1 = File(tempDir, "knittrac.log.1")
        assertTrue("base file should exist", base.exists())
        assertTrue("rotated file .1 should exist", rotated1.exists())

        val count = tempDir.listFiles()?.size ?: 0
        assertTrue("no more than 3 files, found $count", count <= 3)
    }
}
