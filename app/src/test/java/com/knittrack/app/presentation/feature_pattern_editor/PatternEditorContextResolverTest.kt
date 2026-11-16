package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.domain.entity.ColumnStart
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.entity.PatternDirection
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.entity.PatternOperation
import com.knittrack.app.domain.entity.PatternRow
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.domain.entity.RowNumber
import com.knittrack.app.domain.entity.Span
import com.knittrack.app.domain.entity.Symbol
import com.knittrack.app.domain.entity.SymbolCategory
import com.knittrack.app.domain.entity.Technique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для [resolveAddContext] и [resolveUpdateContext].
 *
 * Проверяют валидацию контекста перед добавлением и обновлением операции:
 * наличие схемы, id, выбранного символа, ряда.
 */
class PatternEditorContextResolverTest {

    private fun createSymbol() = Symbol(
        code = "K",
        ownerPatternId = null,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD,
        defaultColumnSpan = Span.ONE,
        defaultRowSpan = Span.ONE
    )

    private fun createPattern() = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Test"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        legend = setOf(createSymbol()),
        rows = listOf(PatternRow(id = PatternRowId.create(10L), rowNumber = RowNumber.create(1)))
    )

    private fun createValidState(): PatternEditorContract.State {
        return PatternEditorContract.State(
            data = PatternEditorContract.DataState(
                pattern = createPattern(),
                currentPatternId = PatternId.create(1L)
            ),
            selection = PatternEditorContract.SelectionState(
                selectedSymbolCode = "K"
            )
        )
    }

    /** Возвращает null, если паттерн отсутствует. */
    @Test
    fun resolveAddContext_missingPattern_returnsNull() {
        val state = createValidState().copy(data = createValidState().data.copy(pattern = null))
        val intent = PatternEditorContract.Intent.AddOperation(PatternRowId.create(10L), ColumnStart.create(0))
        assertNull(resolveAddContext(state, intent))
    }

    /** Возвращает null, если currentPatternId отсутствует. */
    @Test
    fun resolveAddContext_missingPatternId_returnsNull() {
        val state = createValidState().copy(data = createValidState().data.copy(currentPatternId = null))
        val intent = PatternEditorContract.Intent.AddOperation(PatternRowId.create(10L), ColumnStart.create(0))
        assertNull(resolveAddContext(state, intent))
    }

    /** Возвращает null, если не выбран символ. */
    @Test
    fun resolveAddContext_missingSymbolCode_returnsNull() {
        val state = createValidState().copy(selection = createValidState().selection.copy(selectedSymbolCode = null))
        val intent = PatternEditorContract.Intent.AddOperation(PatternRowId.create(10L), ColumnStart.create(0))
        assertNull(resolveAddContext(state, intent))
    }

    /** Возвращает null, если ряд с указанным id не найден в схеме. */
    @Test
    fun resolveAddContext_rowNotFound_returnsNull() {
        val state = createValidState()
        val intent = PatternEditorContract.Intent.AddOperation(PatternRowId.create(99L), ColumnStart.create(0))
        assertNull(resolveAddContext(state, intent))
    }

    /** Возвращает null, если символ с указанным кодом не найден в легенде. */
    @Test
    fun resolveAddContext_symbolNotFound_returnsNull() {
        val state = createValidState().copy(selection = createValidState().selection.copy(selectedSymbolCode = "Unknown"))
        val intent = PatternEditorContract.Intent.AddOperation(PatternRowId.create(10L), ColumnStart.create(0))
        assertNull(resolveAddContext(state, intent))
    }

    /** Возвращает корректный контекст при валидном состоянии. */
    @Test
    fun resolveAddContext_valid_returnsContext() {
        val state = createValidState()
        val intent = PatternEditorContract.Intent.AddOperation(PatternRowId.create(10L), ColumnStart.create(2))
        val context = resolveAddContext(state, intent)
        assertNotNull(context)
        assertEquals(PatternId.create(1L), context!!.patternId)
        assertEquals(ColumnStart.create(2), context.params.columnStart)
        assertEquals("K", context.params.symbol.code)
    }

    /** Возвращает null для UpdateContext, если паттерн отсутствует. */
    @Test
    fun resolveUpdateContext_missingPattern_returnsNull() {
        val state = createValidState().copy(data = createValidState().data.copy(pattern = null))
        val intent = PatternEditorContract.Intent.UpdateOperation(
            oldOperation = PatternOperation(PatternRowId.create(10L), ColumnStart.create(0), createSymbol()),
            newSymbolCode = "K",
            newColumnSpan = null,
            newColorHex = null
        )
        assertNull(resolveUpdateContext(state, intent))
    }

    /** Возвращает null для UpdateContext, если символ не найден. */
    @Test
    fun resolveUpdateContext_symbolNotFound_returnsNull() {
        val state = createValidState()
        val intent = PatternEditorContract.Intent.UpdateOperation(
            oldOperation = PatternOperation(PatternRowId.create(10L), ColumnStart.create(0), createSymbol()),
            newSymbolCode = "Unknown",
            newColumnSpan = null,
            newColorHex = null
        )
        assertNull(resolveUpdateContext(state, intent))
    }

    /** Возвращает корректный UpdateContext при валидном состоянии. */
    @Test
    fun resolveUpdateContext_valid_returnsContext() {
        val state = createValidState()
        val intent = PatternEditorContract.Intent.UpdateOperation(
            oldOperation = PatternOperation(PatternRowId.create(10L), ColumnStart.create(0), createSymbol()),
            newSymbolCode = "K",
            newColumnSpan = Span.create(2),
            newColorHex = "#FFFFFF"
        )
        val context = resolveUpdateContext(state, intent)
        assertNotNull(context)
        assertEquals(PatternId.create(1L), context!!.patternId)
        assertEquals(Span.create(2), context.params.columnSpanOverride)
    }
}
