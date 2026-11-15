package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.repository.PdfPatternExporter
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Тесты [ExportPatternPdfUseCase].
 *
 * Проверяют контракт оркестрации: проброс Success/Error из
 * [PdfPatternExporter] и отсутствие закрытия [OutputStream] со стороны
 * UseCase (жизненным циклом потока управляет Presentation через SAF).
 */
class ExportPatternPdfUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val pdfExporter: PdfPatternExporter = mockk()
    private val useCase = ExportPatternPdfUseCase(pdfExporter, testDispatcher)

    private fun stubPattern(): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Test"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false
    )

    /** Result.Success из exporter'а пробрасывается без изменений. */
    @Test
    fun invoke_exporterSuccess_propagatesSuccess() = runTest {
        val pattern = stubPattern()
        val stream = ByteArrayOutputStream()
        every { pdfExporter.exportToPdf(pattern, stream) } returns Result.Success(Unit)

        val result = useCase(ExportPatternPdfParams(pattern, stream))

        assertTrue(result is Result.Success)
    }

    /** Result.Error из exporter'а пробрасывается без изменений. */
    @Test
    fun invoke_exporterError_propagatesError() = runTest {
        val pattern = stubPattern()
        val stream = ByteArrayOutputStream()
        every { pdfExporter.exportToPdf(pattern, stream) } returns
            Result.Error(AppError.ValidationError(ErrorCode.SCHEMA_EMPTY, "test"))

        val result = useCase(ExportPatternPdfParams(pattern, stream))

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.ValidationError)
        assertEquals(ErrorCode.SCHEMA_EMPTY, (error as AppError.ValidationError).code)
    }

    /** UseCase не закрывает OutputStream — это ответственность Presentation. */
    @Test
    fun invoke_doesNotCloseOutputStream() = runTest {
        val pattern = stubPattern()
        val stream = TrackingOutputStream()
        every { pdfExporter.exportToPdf(pattern, stream) } returns Result.Success(Unit)

        useCase(ExportPatternPdfParams(pattern, stream))

        assertFalse("UseCase не должен закрывать поток", stream.closed)
    }

    /** OutputStream, запоминающий факт close(). */
    private class TrackingOutputStream : OutputStream() {
        var closed = false
        override fun write(b: Int) = Unit
        override fun close() { closed = true }
    }
}
