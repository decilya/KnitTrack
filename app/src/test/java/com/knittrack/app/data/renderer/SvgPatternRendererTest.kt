package com.knittrack.app.data.renderer

import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.ErrorCode
import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.ColumnStart
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.entity.PatternRow
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.domain.entity.RowNumber
import com.knittrack.app.domain.entity.Span
import com.knittrack.app.domain.entity.Symbol
import com.knittrack.app.domain.entity.SymbolCategory
import com.knittrack.app.domain.entity.Technique
import com.knittrack.app.domain.entity.PatternDirection
import com.knittrack.app.domain.entity.PatternOperation
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
        // Операция на row=0, col=0, span=2x2. cellSize=32, padding=50.
        // Ожидаемый x=50, y=50, width=64, height=64.
        assertTrue(svg.contains("""<rect x="50" y="50" width="64" height="64"""))
        assertTrue(svg.contains("<text"))
        assertTrue(svg.contains("V"))     
    }
}
