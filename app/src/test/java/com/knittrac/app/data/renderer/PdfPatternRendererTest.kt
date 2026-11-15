package com.knittrac.app.data.renderer

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.Technique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Тесты [PdfPatternRenderer] на уровне контракта.
 *
 * ВНИМАНИЕ: рендер (PdfDocument + Canvas) НЕ тестируется здесь —
 * Robolectric shadow не реализует PdfDocument.writeTo (см. TECHDEBT P3).
 * Покрывается только ветка валидации до вызова PdfDocument.
 * Рендер проверяется вручную в APK (см. §4.6 чек-лист).
 */
class PdfPatternRendererTest {

    private val renderer = PdfPatternRenderer()

    /**
     * Пустая схема -> Result.Error(SCHEMA_EMPTY), поток не тронут.
     * Эта ветка исполняется до создания PdfDocument, поэтому тест
     * не требует Robolectric.
     */
    @Test
    fun exportToPdf_emptyPattern_returnsValidationError() {
        val empty = Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Test"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false
        )
        val output = ByteArrayOutputStream()

        val result = renderer.exportToPdf(empty, output)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.ValidationError)
        assertEquals(ErrorCode.SCHEMA_EMPTY, (error as AppError.ValidationError).code)
        assertEquals(0, output.size())
    }
}
