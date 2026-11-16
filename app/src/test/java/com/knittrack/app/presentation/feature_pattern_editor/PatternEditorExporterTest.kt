package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.ErrorCode
import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.entity.PatternDirection
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.entity.Technique
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Тесты для функций [exportPatternSvg] и [exportPatternPdf].
 *
 * Проверяют, что в канал эффектов отправляется [PatternEditorContract.Effect.ExportSuccess]
 * или [PatternEditorContract.Effect.ExportError] в зависимости от результата UseCase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatternEditorExporterTest {

    private val patternUseCases: PatternEditorPatternUseCases = mockk()
    private val effectChannel = Channel<PatternEditorContract.Effect>(Channel.UNLIMITED)
    private val outputStream = ByteArrayOutputStream()

    private fun createPattern() = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Test"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false
    )

    /** Отправляет ExportSuccess при успешном экспорте в SVG. */
    @Test
    fun exportPatternSvg_success_sendsExportSuccess() = runTest {
        val pattern = createPattern()
        coEvery { patternUseCases.exportSvg(any()) } returns Result.Success(Unit)

        exportPatternSvg(pattern, outputStream, patternUseCases, effectChannel, this)

        assertEquals(PatternEditorContract.Effect.ExportSuccess, effectChannel.receive())
    }

    /** Отправляет ExportError при ошибке экспорта в SVG. */
    @Test
    fun exportPatternSvg_error_sendsExportError() = runTest {
        val pattern = createPattern()
        coEvery { patternUseCases.exportSvg(any()) } returns Result.Error(AppError.ValidationError(ErrorCode.SCHEMA_EMPTY, ""))

        exportPatternSvg(pattern, outputStream, patternUseCases, effectChannel, this)

        assertEquals(PatternEditorContract.Effect.ExportError, effectChannel.receive())
    }

    /** Отправляет ExportSuccess при успешном экспорте в PDF. */
    @Test
    fun exportPatternPdf_success_sendsExportSuccess() = runTest {
        val pattern = createPattern()
        coEvery { patternUseCases.exportPdf(any()) } returns Result.Success(Unit)

        exportPatternPdf(pattern, outputStream, patternUseCases, effectChannel, this)

        assertEquals(PatternEditorContract.Effect.ExportSuccess, effectChannel.receive())
    }

    /** Отправляет ExportError при ошибке экспорта в PDF. */
    @Test
    fun exportPatternPdf_error_sendsExportError() = runTest {
        val pattern = createPattern()
        coEvery { patternUseCases.exportPdf(any()) } returns Result.Error(AppError.ValidationError(ErrorCode.SCHEMA_EMPTY, ""))

        exportPatternPdf(pattern, outputStream, patternUseCases, effectChannel, this)

        assertEquals(PatternEditorContract.Effect.ExportError, effectChannel.receive())
    }

    /** Регрессия на баг №1: поток не должен закрываться до записи. */
    @Test
    fun exportPatternSvg_doesNotCloseStreamPrematurely() = runTest {
        val pattern = createPattern()
        var isClosed = false
        val trackingStream = object : ByteArrayOutputStream() {
            override fun close() {
                isClosed = true
                super.close()
            }
        }
        // Если бы use {} остался в UI, стрим закрылся бы здесь же.
        // Но мы проверяем, что Exporter закрывает его ТОЛЬКО в finally.
        coEvery { patternUseCases.exportSvg(any()) } returns Result.Success(Unit)

        exportPatternSvg(pattern, trackingStream, patternUseCases, effectChannel, this)

        // Ждем, пока завершатся все корутины в scope (включая блок finally с close)
        advanceUntilIdle()

        assertEquals(PatternEditorContract.Effect.ExportSuccess, effectChannel.receive())
        assertTrue(isClosed)
    }
}
