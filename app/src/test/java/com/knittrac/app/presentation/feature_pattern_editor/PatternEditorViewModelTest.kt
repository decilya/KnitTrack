package com.knittrac.app.presentation.feature_pattern_editor

import androidx.lifecycle.SavedStateHandle
import com.knittrac.app.R
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.model.PatternSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Базовые lifecycle-тесты [PatternEditorViewModel]: инициализация,
 * загрузка схемы, переключение режима, туториал, retry.
 *
 * Тесты редактирования (операции, ряды, легенда) — в
 * [PatternEditorViewModelEditingTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatternEditorViewModelTest : PatternEditorViewModelTestBase() {

    /**
     * Проверяет, что init подписывается на summaries и заполняет
     * availablePatterns.
     */
    @Test
    fun initLoadsAvailablePatterns() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.availablePatterns.size)
        assertEquals("Схема", vm.state.value.availablePatterns[0].name.value)
    }

    /**
     * Проверяет, что LoadPattern обновляет pattern и currentPatternId.
     */
    @Test
    fun loadPatternSuccessSetsPatternAndCurrentPatternId() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val vm = createViewModel()

        vm.onIntent(PatternEditorContract.Intent.LoadPattern(PatternId.create(1L)))
        advanceUntilIdle()

        assertEquals(PatternId.create(1L), vm.state.value.currentPatternId)
        assertNotNull(vm.state.value.pattern)
        assertFalse(vm.state.value.isLoading)
    }

    /**
     * Проверяет, что SelectPattern перезагружает схему по новому id.
     */
    @Test
    fun selectPatternReloadsByNewId() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.SelectPattern(PatternId.create(2L)))
        advanceUntilIdle()

        coVerify(exactly = 1) { getPatternByIdService(PatternId.create(2L)) }
        assertEquals(PatternId.create(2L), vm.state.value.currentPatternId)
    }

    /**
     * Проверяет, что ToggleMode сохраняет isStickyMode в SavedStateHandle.
     */
    @Test
    fun toggleModePersistsInSavedStateHandle() = runTest {
        val savedState = SavedStateHandle()
        val vm = createViewModel(savedState)
        assertTrue(vm.state.value.isStickyMode)

        vm.onIntent(PatternEditorContract.Intent.ToggleMode)

        assertFalse(vm.state.value.isStickyMode)
        assertEquals(false, savedState.get<Boolean>("isStickyMode"))
    }

    /**
     * Проверяет, что init восстанавливает patternId из SavedStateHandle
     * и загружает схему.
     */
    @Test
    fun restoreFromSavedStateHandleLoadsPatternOnInit() = runTest {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val savedState = SavedStateHandle(mapOf<String, Any>("patternId" to 1L))

        val vm = createViewModel(savedState)
        advanceUntilIdle()

        coVerify(exactly = 1) { getPatternByIdService(PatternId.create(1L)) }
        assertEquals(PatternId.create(1L), vm.state.value.currentPatternId)
        assertNull(vm.state.value.selectedSymbolCode)
    }

    /**
     * Проверяет, что CreatePattern при успехе загружает созданную схему.
     */
    @Test
    fun createPatternSuccessLoadsCreatedId() = runTest {
        coEvery { createPatternUseCase(any()) } returns Result.Success(PatternId.create(3L))
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.CreatePattern(createParams()))
        advanceUntilIdle()

        assertEquals(PatternId.create(3L), vm.state.value.currentPatternId)
    }

    /**
     * Проверяет, что CreatePattern при ошибке эмитит ShowError.
     */
    @Test
    fun createPatternErrorEmitsShowError() = runTest {
        coEvery { createPatternUseCase(any()) } returns Result.Error(AppError.ValidationError("x"))
        val vm = createViewModel()
        advanceUntilIdle()
        val effectDeferred = async { vm.effect.first() }

        vm.onIntent(PatternEditorContract.Intent.CreatePattern(createParams()))
        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertTrue(effect is PatternEditorContract.Effect.ShowError)
        assertEquals(
            R.string.pattern_editor_create_error,
            (effect as PatternEditorContract.Effect.ShowError).messageResId
        )
    }

    /**
     * Проверяет, что DismissEditTutorial скрывает туториал и сохраняет
     * флаг в DataStore (setEditTutorialShown(true)).
     */
    @Test
    fun dismissEditTutorialHidesAndPersists() = runTest {
        coEvery { settingsRepository.setEditTutorialShown(any()) } returns Result.Success(Unit)
        val vm = loadedVm()
        advanceUntilIdle()
        assertTrue(vm.state.value.showEditTutorial)

        vm.onIntent(PatternEditorContract.Intent.DismissEditTutorial)
        advanceUntilIdle()

        assertFalse(vm.state.value.showEditTutorial)
        coVerify(exactly = 1) { settingsRepository.setEditTutorialShown(true) }
    }

    /**
     * Проверяет, что при editTutorialShownFlow = true (уже показан)
     * state.showEditTutorial = false.
     */
    @Test
    fun initReadsTutorialShownFromDataStore() = runTest {
        val vm = createViewModel(tutorialShown = true)
        advanceUntilIdle()
        assertFalse(vm.state.value.showEditTutorial)
    }

    /**
     * Проверяет, что Retry перезапускает подписку на summaries и
     * сбрасывает errorRes после ошибки.
     */
    @Test
    fun retryRestartsSummariesAndClearsError() = runTest {
        every { getAllPatternSummariesService(Unit) } returns flowOf(
            Result.Error(AppError.DatabaseError(RuntimeException("test")))
        )
        val vm = createViewModel()
        advanceUntilIdle()
        assertNotNull(vm.state.value.errorRes)

        every { getAllPatternSummariesService(Unit) } returns flowOf(
            Result.Success(listOf(PatternSummary(PatternId.create(1L), PatternName.create("P1"))))
        )
        coEvery { getPatternByIdService(PatternId.create(1L)) } returns Result.Success(
            pattern().copy(
                id = PatternId.create(1L),
                name = PatternName.create("P1"),
                technique = Technique.KNITTING,
                direction = PatternDirection.LTR,
                isReversible = false
            )
        )

        vm.onIntent(PatternEditorContract.Intent.Retry)
        advanceUntilIdle()

        assertNull(vm.state.value.errorRes)
        assertEquals(1, vm.state.value.availablePatterns.size)
        assertEquals(PatternId.create(1L), vm.state.value.currentPatternId)
    }

    /**
     * Проверяет, что Retry загружает первую доступную схему, если
     * currentPatternId == null.
     */
    @Test
    fun retryFallsBackToFirstAvailableWhenCurrentIsNull() = runTest {
        every { getAllPatternSummariesService(Unit) } returns flowOf(
            Result.Error(AppError.DatabaseError(RuntimeException("test")))
        )
        val vm = createViewModel()
        advanceUntilIdle()
        assertNull(vm.state.value.currentPatternId)

        every { getAllPatternSummariesService(Unit) } returns flowOf(
            Result.Success(listOf(PatternSummary(PatternId.create(1L), PatternName.create("P1"))))
        )
        coEvery { getPatternByIdService(PatternId.create(1L)) } returns Result.Success(
            pattern().copy(
                id = PatternId.create(1L),
                name = PatternName.create("P1"),
                technique = Technique.KNITTING,
                direction = PatternDirection.LTR,
                isReversible = false
            )
        )

        vm.onIntent(PatternEditorContract.Intent.Retry)
        advanceUntilIdle()

        assertEquals(PatternId.create(1L), vm.state.value.currentPatternId)
    }

    /**
     * Проверяет, что ShowEditTutorial открывает диалог вручную, даже если
     * editTutorialShownFlow = true (пользователь уже видел туториал).
     * tutorialManuallyRequested защищает от затирания следующим эмитом DataStore.
     */
    @Test
    fun showEditTutorialManually_overridesStoredFlag() = runTest {
        val vm = createViewModel(tutorialShown = true)
        advanceUntilIdle()
        assertFalse(vm.state.value.showEditTutorial)

        vm.onIntent(PatternEditorContract.Intent.ShowEditTutorial)
        advanceUntilIdle()

        assertTrue(vm.state.value.showEditTutorial)
        assertTrue(vm.state.value.tutorialManuallyRequested)
    }

    /**
     * Проверяет, что DismissEditTutorial после ручного показа сбрасывает
     * tutorialManuallyRequested и сохраняет флаг в DataStore.
     */
    @Test
    fun dismissEditTutorial_resetsManualFlag() = runTest {
        coEvery { settingsRepository.setEditTutorialShown(any()) } returns Result.Success(Unit)
        val vm = createViewModel(tutorialShown = true)
        advanceUntilIdle()

        vm.onIntent(PatternEditorContract.Intent.ShowEditTutorial)
        advanceUntilIdle()
        assertTrue(vm.state.value.tutorialManuallyRequested)

        vm.onIntent(PatternEditorContract.Intent.DismissEditTutorial)
        advanceUntilIdle()

        assertFalse(vm.state.value.showEditTutorial)
        assertFalse(vm.state.value.tutorialManuallyRequested)
        coVerify(exactly = 1) { settingsRepository.setEditTutorialShown(true) }
    }
}
