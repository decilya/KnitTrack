package com.knittrac.app.presentation.feature_pattern_editor

import androidx.lifecycle.SavedStateHandle
import com.knittrac.app.R
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.SymbolLocalizer
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
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.domain.service.AddPatternOperationUseCase
import com.knittrac.app.domain.service.GetAllPatternSummariesService
import com.knittrac.app.domain.service.GetPatternByIdService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [PatternEditorViewModel] (7f-B-2).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PatternEditorViewModelTest : BaseTest() {

    private val getPatternByIdService: GetPatternByIdService = mockk()
    private val getAllPatternSummariesService: GetAllPatternSummariesService = mockk()
    private val addPatternOperationUseCase: AddPatternOperationUseCase = mockk()
    private val symbolLocalizer: SymbolLocalizer = mockk(relaxed = true)

    private fun symbol(code: String = "K"): Symbol = Symbol(
        code = code,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD,
        defaultColumnSpan = Span.ONE
    )

    private fun row(id: Long = 10L): PatternRow = PatternRow(
        id = PatternRowId.create(id),
        rowNumber = RowNumber.create(1)
    )

    private fun pattern(): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        legend = setOf(symbol()),
        rows = listOf(row())
    )

    private fun stubSummaries() {
        every { getAllPatternSummariesService(Unit) } returns flowOf(
            Result.Success(listOf(PatternSummary(PatternId.create(1L), PatternName.create("Схема"))))
        )
    }

    private fun createViewModel(savedState: SavedStateHandle = SavedStateHandle()): PatternEditorViewModel {
        stubSummaries()
        return PatternEditorViewModel(
            getPatternByIdService,
            getAllPatternSummariesService,
            addPatternOperationUseCase,
            symbolLocalizer,
            savedState
        )
    }

    @Test
    fun `init loads availablePatterns`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.availablePatterns.size)
        assertEquals("Схема", vm.state.value.availablePatterns[0].name.value)
    }

    @Test
    fun `loadPattern success sets pattern and currentPatternId`() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val vm = createViewModel()

        vm.onIntent(PatternEditorContract.Intent.LoadPattern(PatternId.create(1L)))
        advanceUntilIdle()

        assertEquals(PatternId.create(1L), vm.state.value.currentPatternId)
        assertTrue(vm.state.value.pattern != null)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test
    fun `selectPattern reloads by new id`() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val vm = createViewModel()

        vm.onIntent(PatternEditorContract.Intent.SelectPattern(PatternId.create(2L)))
        advanceUntilIdle()

        coVerify(exactly = 1) { getPatternByIdService(PatternId.create(2L)) }
        assertEquals(PatternId.create(2L), vm.state.value.currentPatternId)
    }

    @Test
    fun `toggleMode persists in SavedStateHandle`() = runTest {
        val savedState = SavedStateHandle()
        val vm = createViewModel(savedState)
        assertEquals(true, vm.state.value.isStickyMode)

        vm.onIntent(PatternEditorContract.Intent.ToggleMode)

        assertEquals(false, vm.state.value.isStickyMode)
        assertEquals(false, savedState.get<Boolean>("isStickyMode"))
    }

    @Test
    fun `addOperation success emits Effect and resets isSaving`() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        coEvery { addPatternOperationUseCase(any()) } returns Result.Success(
            PatternOperation(
                patternRowId = PatternRowId.create(10L),
                columnStart = ColumnStart.create(0),
                symbol = symbol()
            )
        )
        val vm = createViewModel()
        vm.onIntent(PatternEditorContract.Intent.LoadPattern(PatternId.create(1L)))
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.SelectSymbol("K"))

        val effects = mutableListOf<PatternEditorContract.Effect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(PatternEditorContract.Intent.AddOperation(
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0)
        ))
        advanceUntilIdle()

        job.cancel()
        assertEquals(false, vm.state.value.isSaving)
        assertTrue(effects.any { it is PatternEditorContract.Effect.OperationAdded })
    }

    @Test
    fun `addOperation error emits ShowError`() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        coEvery { addPatternOperationUseCase(any()) } returns
            Result.Error(AppError.ValidationError("overlap"))
        val vm = createViewModel()
        vm.onIntent(PatternEditorContract.Intent.LoadPattern(PatternId.create(1L)))
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.SelectSymbol("K"))

        val effects = mutableListOf<PatternEditorContract.Effect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(PatternEditorContract.Intent.AddOperation(
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0)
        ))
        advanceUntilIdle()

        job.cancel()
        val showError = effects.filterIsInstance<PatternEditorContract.Effect.ShowError>().firstOrNull()
        assertTrue(showError != null)
        assertEquals(R.string.pattern_editor_add_error, showError?.messageResId)
    }

    @Test
    fun `restore from SavedStateHandle loads pattern on init`() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val savedState = SavedStateHandle(mapOf<String, Any>("patternId" to 1L))

        val vm = createViewModel(savedState)
        advanceUntilIdle()

        coVerify(exactly = 1) { getPatternByIdService(PatternId.create(1L)) }
        assertEquals(PatternId.create(1L), vm.state.value.currentPatternId)
        // selectedSymbolCode null — не выбирали
        assertNull(vm.state.value.selectedSymbolCode)
    }
}
