package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.core.common.ErrorStringProvider
import com.knittrack.app.core.common.ErrorMapper
import com.knittrack.app.core.common.ErrorCode
import androidx.lifecycle.SavedStateHandle
import com.knittrack.app.core.base.BaseTest
import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.ColumnStart
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.entity.PatternDirection
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.entity.PatternOperation
import com.knittrack.app.domain.entity.PatternRow
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.domain.entity.RowCount
import com.knittrack.app.domain.entity.RowNumber
import com.knittrack.app.domain.entity.Span
import com.knittrack.app.domain.entity.Symbol
import com.knittrack.app.domain.entity.SymbolCategory
import com.knittrack.app.domain.entity.Technique
import com.knittrack.app.domain.model.PatternSummary
import com.knittrack.app.domain.repository.SettingsRepository
import com.knittrack.app.domain.service.AddPatternOperationUseCase
import com.knittrack.app.domain.service.AddSymbolUseCase
import com.knittrack.app.domain.service.CreatePatternParams
import com.knittrack.app.domain.service.CreatePatternUseCase
import com.knittrack.app.domain.service.ExportPatternSvgUseCase
import com.knittrack.app.domain.service.ExportPatternPdfUseCase
import com.knittrack.app.domain.service.DeleteOperationUseCase
import com.knittrack.app.domain.service.DeletePatternRowUseCase
import com.knittrack.app.domain.service.DeleteSymbolUseCase
import com.knittrack.app.domain.service.GetAllPatternSummariesService
import com.knittrack.app.domain.service.GetPatternByIdService
import com.knittrack.app.domain.service.UpdatePatternOperationUseCase
import com.knittrack.app.domain.service.UpdatePatternRowNumberUseCase
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
    protected val exportPatternSvgUseCase: ExportPatternSvgUseCase = mockk()
    protected val exportPatternPdfUseCase: ExportPatternPdfUseCase = mockk()

    private val patternUseCases = PatternEditorPatternUseCases(
        getPatternById = getPatternByIdService,
        getAllPatternSummaries = getAllPatternSummariesService,
        createPattern = createPatternUseCase,
        exportSvg = exportPatternSvgUseCase,
        exportPdf = exportPatternPdfUseCase
    )

    protected val deleteOperationUseCase: DeleteOperationUseCase = mockk()
    protected val updatePatternOperationUseCase: UpdatePatternOperationUseCase = mockk()
    protected val updatePatternRowNumberUseCase: UpdatePatternRowNumberUseCase = mockk()
    protected val deletePatternRowUseCase: DeletePatternRowUseCase = mockk()
    protected val addSymbolUseCase: AddSymbolUseCase = mockk()
    protected val deleteSymbolUseCase: DeleteSymbolUseCase = mockk()
    protected val settingsRepository: SettingsRepository = mockk()

    private val errorStringProvider: ErrorStringProvider = object : ErrorStringProvider {
        override fun getStringRes(code: ErrorCode): Int = ErrorMapper.map(code)
    }

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
            patternUseCases = patternUseCases,
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
                savedStateHandle = savedState,
                errorStringProvider = errorStringProvider
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
