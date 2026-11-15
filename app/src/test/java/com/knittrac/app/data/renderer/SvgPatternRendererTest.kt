package com.knittrac.app.data.renderer

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternOperation
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для [SvgPatternRenderer].
 *
 * Проверяют корректность генерации SVG и обработку пустой схемы.
 */
class SvgPatternRendererTest {

    private val renderer = SvgPatternRenderer()

    private fun createSymbol() = Symbol(
        code = "K",
        ownerPatternId = null,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD,
        defaultColumnSpan = Span.ONE,
        defaultRowSpan = Span.ONE
    )

    private fun createPattern(withOperations: Boolean = true): Pattern {
        val symbol = createSymbol()
        val operations = if (withOperations) {
            listOf(
                PatternOperation(
                    patternRowId = PatternRowId.create(1L),
                    columnStart = ColumnStart.create(0),
                    symbol = symbol,
                    columnSpanOverride = Span.create(2),
                    rowSpanOverride = Span.create(2)
                )
            )
        } else emptyList()

        return Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Test Pattern"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false,
            legend = setOf(symbol),
            rows = listOf(PatternRow(id = PatternRowId.create(1L), rowNumber = RowNumber.create(1), operations = operations)),
            repeats = emptyList()
        )
    }

    /**
     * Проверяет, что пустая схема возвращает [AppError.ValidationError] с кодом [ErrorCode.SCHEMA_EMPTY].
     */
    @Test
    fun exportToSvg_emptyPattern_returnsValidationError() {
        val pattern = createPattern(withOperations = false).copy(rows = emptyList())
        val result = renderer.exportToSvg(pattern)
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.ValidationError)
        assertTrue((error as AppError.ValidationError).code == ErrorCode.SCHEMA_EMPTY)
    }

    /**
     * Проверяет, что валидная схема возвращает [Result.Success] со строкой SVG,
     * содержащей теги `<svg>`, `<rect>`, `<text>` и глиф символа.
     */
    @Test
    fun exportToSvg_validPattern_returnsSvgString() {
        val pattern = createPattern()
        val result = renderer.exportToSvg(pattern)
        
        assertTrue(result is Result.Success)
        val svg = (result as Result.Success).data
        
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
        assertTrue(svg.contains("<rect")) 
        assertTrue(svg.contains("<text")) 
        assertTrue(svg.contains("V"))     
    }
}
