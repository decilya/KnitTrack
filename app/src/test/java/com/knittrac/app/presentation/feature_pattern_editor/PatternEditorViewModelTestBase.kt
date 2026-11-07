package com.knittrac.app.presentation.feature_pattern_editor

import androidx.lifecycle.SavedStateHandle
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.domain.repository.SettingsRepository
import com.knittrac.app.domain.service.AddPatternOperationUseCase
import com.knittrac.app.domain.service.AddSymbolUseCase
import com.knittrac.app.domain.service.CreatePatternParams
import com.knittrac.app.domain.service.CreatePatternUseCase
import com.knittrac.app.domain.service.DeleteOperationUseCase
import com.knittrac.app.domain.service.DeletePatternRowUseCase
import com.knittrac.app.domain.service.DeleteSymbolUseCase
import com.knittrac.app.domain.service.GetAllPatternSummariesService
import com.knittrac.app.domain.service.GetPatternByIdService
import com.knittrac.app.domain.service.UpdatePatternOperationUseCase
import com.knittrac.app.domain.service.UpdatePatternRowNumberUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before

/**
 * Базовый класс для тестов [PatternEditorViewModel].
 *
 * Содержит общие моки всех UseCase'ов и фабричные методы для
 * Domain-объектов. Наследники — [PatternEditorViewModelTest]
 * (базовые lifecycle-тесты) и [PatternEditorViewModelEditingTest]
 * (тесты редактирования операций, рядов, легенды).
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class PatternEditorViewModelTestBase : BaseTest() {

    protected val getPatternByIdService: GetPatternByIdService = mockk()
    protected val getAllPatternSummariesService: GetAllPatternSummariesService = mockk()
    protected val addPatternOperationUseCase: AddPatternOperationUseCase = mockk()
    protected val createPatternUseCase: CreatePatternUseCase = mockk()
    protected val deleteOperationUseCase: DeleteOperationUseCase = mockk()
    protected val updatePatternOperationUseCase: UpdatePatternOperationUseCase = mockk()
    protected val updatePatternRowNumberUseCase: UpdatePatternRowNumberUseCase = mockk()
    protected val deletePatternRowUseCase: DeletePatternRowUseCase = mockk()
    protected val addSymbolUseCase: AddSymbolUseCase = mockk()
    protected val deleteSymbolUseCase: DeleteSymbolUseCase = mockk()
    protected val settingsRepository: SettingsRepository = mockk()

    /**
     * Стандартный символ для операций и легенды.
     */
    protected fun symbol(code: String = "K"): Symbol = Symbol(
        code = code,
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD,
        defaultColumnSpan = Span.ONE
    )

    /**
     * Ряд схемы с заданным id.
     */
    protected fun row(id: Long = 10L): PatternRow = PatternRow(
        id = PatternRowId.create(id),
        rowNumber = RowNumber.create(1)
    )

    /**
     * Минимальная схема с одним рядом и одним символом.
     */
    protected fun pattern(): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        legend = setOf(symbol()),
        rows = listOf(row())
    )

    /**
     * Операция на заданной колонке в ряду id=10.
     */
    protected fun operation(column: Int): PatternOperation = PatternOperation(
        patternRowId = PatternRowId.create(10L),
        columnStart = ColumnStart.create(column),
        symbol = symbol()
    )

    /**
     * Параметры создания схемы (имя "Новая", 1 ряд).
     */
    protected fun createParams(): CreatePatternParams = CreatePatternParams(
        name = PatternName.create("Новая"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        rowCount = RowCount.create(1)
    )

    /**
     * Инициализация mock-объектов перед каждым тестом.
     */
    @Before
    fun setUpMocks() {
        stubSummaries()
        coEvery { getPatternByIdService(any()) } returns Result.Success(
            Pattern(
                id = PatternId.create(1L),
                name = PatternName.create("Схема"),
                technique = Technique.KNITTING,
                direction = PatternDirection.LTR,
                isReversible = false
            )
        )
    }

    /**
     * Заглушка для [GetAllPatternSummariesService]: возвращает одну схему.
     */
    protected fun stubSummaries() {
        every { getAllPatternSummariesService(Unit) } returns flowOf(
            Result.Success(listOf(PatternSummary(PatternId.create(1L), PatternName.create("Схема"))))
        )
    }

    /**
     * Создаёт ViewModel со всеми UseCase'ами.
     */
    protected fun createViewModel(
        savedState: SavedStateHandle = SavedStateHandle(),
        tutorialShown: Boolean = false
    ): PatternEditorViewModel {
        every { settingsRepository.legendAddButtonVisibleFlow } returns flowOf(true)
        every { settingsRepository.editTutorialShownFlow } returns flowOf(tutorialShown)
        return PatternEditorViewModel(
            patternUseCases = PatternEditorPatternUseCases(
                getPatternById = getPatternByIdService,
                getAllPatternSummaries = getAllPatternSummariesService,
                createPattern = createPatternUseCase
            ),
            operationUseCases = PatternEditorOperationUseCases(
                addPatternOperation = addPatternOperationUseCase,
                updatePatternOperation = updatePatternOperationUseCase,
                deleteOperation = deleteOperationUseCase
            ),
            rowUseCases = PatternEditorRowUseCases(
                updatePatternRowNumber = updatePatternRowNumberUseCase,
                deletePatternRow = deletePatternRowUseCase
            ),
            symbolUseCases = PatternEditorSymbolUseCases(
                addSymbol = addSymbolUseCase,
                deleteSymbol = deleteSymbolUseCase
            ),
            platformDeps = PatternEditorPlatformDeps(
                settingsRepository = settingsRepository,
                savedStateHandle = savedState
            )
        )
    }

    /**
     * ViewModel с уже загруженной схемой (через LoadPattern).
     */
    protected fun loadedVm(): PatternEditorViewModel {
        coEvery { getPatternByIdService(any()) } returns Result.Success(pattern())
        val vm = createViewModel()
        vm.onIntent(PatternEditorContract.Intent.LoadPattern(PatternId.create(1L)))
        return vm
    }
}
