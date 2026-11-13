package com.knittrac.app.presentation.feature_pattern_editor

import androidx.compose.ui.graphics.Color
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.entity.Technique
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * `maxColumnsOf` и `colorOrDefault` — file-level хелперы Canvas.
 * Robolectric нужен для `android.graphics.Color.parseColor`.
 */
@RunWith(RobolectricTestRunner::class)
class PatternEditorCanvasHelpersTest {

    private val symbol = Symbol(
        code = "K",
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD
    )

    private fun operation(columnStart: Int, span: Int = 1): PatternOperation =
        PatternOperation(
            patternRowId = PatternRowId.create(1L),
            columnStart = ColumnStart.create(columnStart),
            symbol = symbol,
            columnSpanOverride = Span.create(span)
        )

    private fun pattern(rows: List<PatternRow>): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        legend = setOf(symbol),
        rows = rows
    )

    private fun row(id: Long, number: Int, ops: List<PatternOperation>) =
        PatternRow(
            id = PatternRowId.create(id),
            rowNumber = RowNumber.create(number),
            operations = ops
        )

    @Test
    fun maxColumnsOf_emptyRows_returns10() {
        assertEquals(10, maxColumnsOf(pattern(emptyList())))
    }

    /**
     * Пол ниже 10: колонка 5 + span 3 = 8, результат всё равно 10.
     * Кейс выше пола: колонка 8 + span 3 = 11.
     */
    @Test
    fun maxColumnsOf_singleOperation_returnsColumnPlusSpan() {
        val belowFloor = pattern(listOf(row(1L, 1, listOf(operation(columnStart = 5, span = 3)))))
        assertEquals(10, maxColumnsOf(belowFloor))

        val aboveFloor = pattern(listOf(row(1L, 1, listOf(operation(columnStart = 8, span = 3)))))
        assertEquals(11, maxColumnsOf(aboveFloor))
    }

    @Test
    fun colorOrDefault_null_returnsFallback() {
        val fallback = Color.Red
        assertEquals(fallback, colorOrDefault(null, fallback))
    }

    @Test
    fun colorOrDefault_invalidHex_returnsFallback() {
        val fallback = Color.Red
        assertEquals(fallback, colorOrDefault("not-a-hex", fallback))
    }
}
