package com.knittrac.app.presentation.feature_pattern_editor

import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.R
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.SymbolCategory
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты редактирования в [PatternEditorViewModel]: операции (add/update/
 * delete/undo), ряды (move/delete), символы (add/delete), открытие
 * диалогов и legend-sheet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatternEditorViewModelEditingTest : PatternEditorViewModelTestBase() {

    /**
     * Проверяет, что AddOperation при успехе эмитит OperationAdded
     * и сбрасывает isSaving.
     */
    @Test
    fun addOperationSuccessEmitsEffectAndResetsIsSaving() = runTest {
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

        val effectDeferred = async { vm.effect.first() }

        vm.onIntent(PatternEditorContract.Intent.AddOperation(
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0)
        ))
        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertEquals(false, vm.state.value.isSaving)
        assertTrue(effect is PatternEditorContract.Effect.OperationAdded)
    }

    /**
     * Проверяет, что AddOperation при ошибке UseCase эмитит ShowError
     * с ресурсом pattern_editor_add_error.
     */
    @Test
    fun addOperationErrorEmitsShowError() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        coEvery { addPatternOperationUseCase(any()) } returns
            Result.Error(AppError.ValidationError(ErrorCode.INVALID_ARGUMENT, "overlap"))
        val vm = createViewModel()
        vm.onIntent(PatternEditorContract.Intent.LoadPattern(PatternId.create(1L)))
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.SelectSymbol("K"))

        val effectDeferred = async { vm.effect.first() }

        vm.onIntent(PatternEditorContract.Intent.AddOperation(
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0)
        ))
        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertTrue(effect is PatternEditorContract.Effect.ShowError)
        assertEquals(
            R.string.pattern_editor_add_error,
            (effect as PatternEditorContract.Effect.ShowError).messageResId
        )
    }

    /**
     * Проверяет, что успешный AddOperation кладёт операцию в undo-стек.
     */
    @Test
    fun addOperationSuccessPushesToUndoStack() = runTest {
        coEvery { addPatternOperationUseCase(any()) } returns Result.Success(operation(0))
        val vm = loadedVm()
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.SelectSymbol("K"))

        vm.onIntent(PatternEditorContract.Intent.AddOperation(
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0)
        ))
        advanceUntilIdle()

        assertEquals(listOf(operation(0)), vm.state.value.lastOperations)
    }

    /**
     * Проверяет, что undo-стек ограничен 25 элементами (старые
     * вытесняются).
     */
    @Test
    fun addOperationSuccessStackCapsAt25() = runTest {
        var column = 0
        coEvery { addPatternOperationUseCase(any()) } answers {
            Result.Success(operation(column++))
        }
        val vm = loadedVm()
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.SelectSymbol("K"))

        repeat(26) { index ->
            vm.onIntent(PatternEditorContract.Intent.AddOperation(
                rowId = PatternRowId.create(10L),
                columnStart = ColumnStart.create(index)
            ))
            advanceUntilIdle()
        }

        val stack = vm.state.value.lastOperations
        assertEquals(25, stack.size)
        assertEquals(1, stack.first().columnStart.value)
        assertEquals(25, stack.last().columnStart.value)
    }

    /**
     * Проверяет, что UndoLastOperation при успехе удаляет операцию,
     * очищает стек и перезагружает схему.
     */
    @Test
    fun undoLastOperationSuccessPopsAndReloads() = runTest {
        coEvery { addPatternOperationUseCase(any()) } returns Result.Success(operation(0))
        coEvery { deleteOperationUseCase(any()) } returns Result.Success(Unit)
        val vm = loadedVm()
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.SelectSymbol("K"))
        vm.onIntent(PatternEditorContract.Intent.AddOperation(
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0)
        ))
        advanceUntilIdle()
        coVerify(exactly = 2) { getPatternByIdService(any()) }

        vm.onIntent(PatternEditorContract.Intent.UndoLastOperation)
        advanceUntilIdle()

        assertTrue(vm.state.value.lastOperations.isEmpty())
        coVerify(exactly = 3) { getPatternByIdService(any()) }
    }

    /**
     * Проверяет, что ошибка при UndoLastOperation эмитит ShowError
     * и НЕ очищает undo-стек (позволяет retry).
     */
    @Test
    fun undoLastOperationErrorEmitsShowErrorAndKeepsStack() = runTest {
        coEvery { addPatternOperationUseCase(any()) } returns Result.Success(operation(0))
        coEvery { deleteOperationUseCase(any()) } returns
            Result.Error(AppError.NotFoundError(ErrorCode.PROJECT_NOT_FOUND, "нет"))
        val vm = loadedVm()
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.SelectSymbol("K"))
        vm.onIntent(PatternEditorContract.Intent.AddOperation(
            rowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0)
        ))
        advanceUntilIdle()
        vm.effect.first()
        val effectDeferred = async { vm.effect.first() }

        vm.onIntent(PatternEditorContract.Intent.UndoLastOperation)
        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertEquals(listOf(operation(0)), vm.state.value.lastOperations)
        assertTrue(effect is PatternEditorContract.Effect.ShowError)
        assertEquals(
            R.string.pattern_editor_undo_error,
            (effect as PatternEditorContract.Effect.ShowError).messageResId
        )
    }

    /**
     * Проверяет, что UndoLastOperation на пустом стеке ничего не делает
     * (UseCase не вызывается).
     */
    @Test
    fun undoLastOperationEmptyStackDoesNothing() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.UndoLastOperation)
        advanceUntilIdle()

        coVerify(exactly = 0) { deleteOperationUseCase(any()) }
        assertTrue(vm.state.value.lastOperations.isEmpty())
    }

    /**
     * Проверяет, что EditOperation кладёт операцию в editingOperation.
     */
    @Test
    fun editOperationOpensDialog() = runTest {
        val op = operation(0)
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.EditOperation(op))

        assertEquals(op, vm.state.value.editingOperation)
    }

    /**
     * Проверяет, что DismissEditor очищает editingOperation и editingRow.
     */
    @Test
    fun dismissEditorClearsEditingState() = runTest {
        val op = operation(0)
        val vm = loadedVm()
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.EditOperation(op))

        vm.onIntent(PatternEditorContract.Intent.DismissEditor)

        assertNull(vm.state.value.editingOperation)
        assertNull(vm.state.value.editingRow)
    }

    /**
     * Проверяет, что UpdateOperation при успехе закрывает диалог и
     * перезагружает схему.
     */
    @Test
    fun updateOperationSuccessReloadsPattern() = runTest {
        coEvery { updatePatternOperationUseCase(any()) } returns
            Result.Success(operation(0))
        val vm = loadedVm()
        advanceUntilIdle()
        coVerify(exactly = 1) { getPatternByIdService(any()) }

        vm.onIntent(PatternEditorContract.Intent.UpdateOperation(
            oldOperation = operation(0),
            newSymbolCode = "K",
            newOverrideSpan = null,
            newColorHex = null
        ))
        advanceUntilIdle()

        assertNull(vm.state.value.editingOperation)
        coVerify(exactly = 2) { getPatternByIdService(any()) }
    }

    /**
     * Проверяет, что UpdateOperation при ошибке UseCase эмитит ShowError.
     */
    @Test
    fun updateOperationErrorEmitsShowError() = runTest {
        coEvery { updatePatternOperationUseCase(any()) } returns
            Result.Error(AppError.ValidationError(ErrorCode.INVALID_ARGUMENT, "overlap"))
        val vm = loadedVm()
        advanceUntilIdle()
        val effectDeferred = async { vm.effect.first() }

        vm.onIntent(PatternEditorContract.Intent.UpdateOperation(
            oldOperation = operation(0),
            newSymbolCode = "K",
            newOverrideSpan = null,
            newColorHex = null
        ))
        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertTrue(effect is PatternEditorContract.Effect.ShowError)
    }

    /**
     * Проверяет, что DeleteRow при успехе очищает editingRow.
     */
    @Test
    fun deleteRowSuccessReloads() = runTest {
        coEvery { deletePatternRowUseCase(any()) } returns Result.Success(Unit)
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.DeleteRow(PatternRowId.create(10L)))
        advanceUntilIdle()

        assertNull(vm.state.value.editingRow)
    }

    /**
     * Проверяет, что UpdateRowNumber при успехе очищает editingRow.
     */
    @Test
    fun updateRowNumberSuccessReloads() = runTest {
        coEvery { updatePatternRowNumberUseCase(any()) } returns Result.Success(Unit)
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.UpdateRowNumber(
            rowId = PatternRowId.create(10L),
            newRowNumber = RowNumber.create(2)
        ))
        advanceUntilIdle()

        assertNull(vm.state.value.editingRow)
    }

    /**
     * Проверяет, что OpenLegendEditor выставляет isLegendEditorOpen = true.
     */
    @Test
    fun openLegendEditorSetsFlag() = runTest {
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.OpenLegendEditor)

        assertTrue(vm.state.value.isLegendEditorOpen)
    }

    /**
     * Проверяет, что CloseLegendEditor сбрасывает isLegendEditorOpen.
     */
    @Test
    fun closeLegendEditorClearsFlag() = runTest {
        val vm = loadedVm()
        advanceUntilIdle()
        vm.onIntent(PatternEditorContract.Intent.OpenLegendEditor)

        vm.onIntent(PatternEditorContract.Intent.CloseLegendEditor)

        assertTrue(!vm.state.value.isLegendEditorOpen)
    }

    /**
     * Проверяет, что AddSymbol при успехе перезагружает схему.
     */
    @Test
    fun addSymbolSuccessReloadsPattern() = runTest {
        coEvery { addSymbolUseCase(any()) } returns Result.Success(
            symbol("NEW").copy(
                ownerPatternId = PatternId.create(1L),
                category = SymbolCategory.CUSTOM,
                displayName = "New",
                textGlyph = "X"
            )
        )
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.AddSymbol(
            code = "NEW",
            displayName = "New",
            textGlyph = "X",
            defaultColumnSpan = Span.ONE
        ))
        advanceUntilIdle()

        coVerify(exactly = 2) { getPatternByIdService(any()) }
    }

    /**
     * Проверяет, что AddSymbol при ошибке (дубликат) эмитит ShowError.
     */
    @Test
    fun addSymbolErrorEmitsShowError() = runTest {
        coEvery { addSymbolUseCase(any()) } returns
            Result.Error(AppError.ValidationError(ErrorCode.INVALID_ARGUMENT, "duplicate"))
        val vm = loadedVm()
        advanceUntilIdle()
        val effectDeferred = async { vm.effect.first() }

        vm.onIntent(PatternEditorContract.Intent.AddSymbol(
            code = "NEW",
            displayName = "New",
            textGlyph = "X",
            defaultColumnSpan = Span.ONE
        ))
        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertTrue(effect is PatternEditorContract.Effect.ShowError)
    }

    /**
     * Проверяет, что DeleteSymbol при успехе перезагружает схему.
     */
    @Test
    fun deleteSymbolSuccessReloadsPattern() = runTest {
        coEvery { deleteSymbolUseCase(any()) } returns Result.Success(Unit)
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.DeleteSymbol("CUSTOM"))
        advanceUntilIdle()

        coVerify(exactly = 2) { getPatternByIdService(any()) }
    }
}
