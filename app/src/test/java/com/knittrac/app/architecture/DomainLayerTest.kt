package com.knittrac.app.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * Архитектурный тест: domain-слой не должен зависеть от Android,
 * Room, Gson, DataStore или слоёв data/presentation/platform.
 *
 * Проверка основана на правилах Clean Architecture (раздел 4.5.1):
 * - domain — центр, не знает про инфраструктуру;
 * - домен не импортирует android.*, androidx.* (кроме androidx.annotation.*),
 *   com.google.gson.*;
 * - домен не импортирует com.knittrac.app.data.*, presentation.*, platform.*.
 *
 * Разрешённые исключения:
 * - androidx.annotation.* — это чистые Java-аннотации без Android-рантайма.
 *   @Keep нужен для R8, @VisibleForTesting — для тестов. Не нарушают
 *   Persistence Ignorance и не создают зависимости на Android.
 *
 * Если тест падает — значит в domain попал импорт, нарушающий границу.
 * Правильный fix: перенести код выше (в domain через интерфейс) или ниже
 * (в data/platform).
 */
class DomainLayerTest {

    /**
     * Директория domain-слоя относительно рабочей директории Gradle-модуля
     * app (unit-тесты запускаются из app/).
     */
    private val domainDir: Path = Paths.get(
        "src", "main", "java", "com", "knittrac", "app", "domain"
    )

    /**
     * Список запрещённых префиксов import-строк. Проверяется только начало
     * строки, начинающейся с "import ". Содержимое комментариев и строковых
     * литералов не анализируется.
     */
    private val forbiddenPrefixes = listOf(
        "import android.",
        "import androidx.",
        "import com.google.gson.",
        "import com.knittrac.app.data.",
        "import com.knittrac.app.presentation.",
        "import com.knittrac.app.platform.",
    )

    /**
     * Разрешённый префикс: androidx.annotation.* не тянет Android-рантайм.
     */
    private val annotationExceptionPrefix = "import androidx.annotation."

    /**
     * Проверяет, что ни один .kt-файл в domain не импортирует запрещённые
     * пакеты. При нарушении выводит полный список (файл:строка).
     */
    @Test
    fun domainDoesNotImportAndroidOrInfrastructure() {
        val ktFiles = collectKtFiles(domainDir)
        assertTrue(
            "Domain directory not found: ${domainDir.toAbsolutePath()}",
            ktFiles.isNotEmpty()
        )

        val violations = mutableListOf<String>()

        for (file in ktFiles) {
            Files.readAllLines(file, Charsets.UTF_8).forEachIndexed { index, line ->
                val trimmed = line.trimStart()
                if (!trimmed.startsWith("import ")) return@forEachIndexed

                // Разрешаем androidx.annotation.* целиком.
                if (trimmed.startsWith(annotationExceptionPrefix)) return@forEachIndexed

                forbiddenPrefixes.forEach { prefix ->
                    if (trimmed.startsWith(prefix)) {
                        violations += "${file.toAbsolutePath()}:${index + 1}: $trimmed"
                    }
                }
            }
        }

        assertTrue(
            buildViolationMessage(violations),
            violations.isEmpty()
        )
    }

    /**
     * Собирает все .kt-файлы рекурсивно из директории.
     */
    private fun collectKtFiles(root: Path): List<Path> {
        if (!Files.isDirectory(root)) return emptyList()
        return Files.walk(root).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".kt") }
                .collect(Collectors.toList())
        }
    }

    /**
     * Формирует сообщение об ошибке со списком нарушений.
     */
    private fun buildViolationMessage(violations: List<String>): String =
        if (violations.isEmpty()) ""
        else buildString {
            append("Domain layer imports forbidden packages:\n")
            violations.forEach { append("  ").append(it).append('\n') }
            append(
                "\nFix: переместить код выше (в domain через интерфейс) " +
                "или ниже (в data/platform)."
            )
        }
}
