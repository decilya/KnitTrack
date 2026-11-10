package com.knittrac.app.presentation.feature_settings

import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.LanguageOption
import com.knittrac.app.core.localization.LocaleService
import com.knittrac.app.domain.repository.SettingsRepository
import com.knittrac.app.domain.entity.ThemeMode
import com.knittrac.app.domain.service.ExportDataService
import com.knittrac.app.domain.service.ImportDataService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream

/**
 * Юнит-тесты для [SettingsViewModel].
 *
 * Покрывают:
 * - инициализацию: supportedLanguages из LocaleService, combine трёх потоков;
 * - MVI-действия: смена темы, переключение уведомлений, смена языка;
 * - export/import: делегирование в сервисы и эмиссия корректных эффектов;
 * - защита от неподдерживаемого языка: setLanguage возвращает
 *   Result.Error, эмитится ShowError вместо RecreateActivity.
 *
 * Стиль изоляции: UnconfinedTestDispatcher и Dispatchers.setMain, чтобы
 * корутины в init выполнялись синхронно и тесты были детерминированными.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var localeService: LocaleService
    private lateinit var exportDataService: ExportDataService
    private lateinit var importDataService: ImportDataService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsRepository = mockk(relaxed = true)
        localeService = mockk(relaxed = true)
        exportDataService = mockk(relaxed = true)
        importDataService = mockk(relaxed = true)

        // Стабы для init-блока ViewModel: combine сразу получает начальные
        // значения, supportedLanguages не пустой вызов к реальному LocaleService.
        every { settingsRepository.themeModeFlow } returns flowOf(ThemeMode.SYSTEM)
        every { settingsRepository.legendAddButtonVisibleFlow } returns kotlinx.coroutines.flow.flowOf(true)
            every { settingsRepository.notificationsEnabledFlow } returns flowOf(true)
        every { localeService.supportedLanguages } returns emptyList()
        every { localeService.getCurrentLanguageFlow() } returns flowOf("ru")

        // set* методы возвращают Result<Unit> с P1.4. Дефолтные стабы для
        // happy-path тестов — Success; тесты с ошибкой переопределяют локально.
        coEvery { settingsRepository.setThemeMode(any()) } returns Result.Success(Unit)
        coEvery { settingsRepository.setNotificationsEnabled(any()) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            settingsRepository = settingsRepository,
            localeService = localeService,
            exportDataService = exportDataService,
            importDataService = importDataService
        )

    // --- init ---

    /**
     * init должен перенести supportedLanguages из LocaleService в State,
     * не дожидаясь первого эмита combine.
     */
    @Test
    fun init_fillsSupportedLanguagesFromLocaleManage() = runTest(testDispatcher) {
        val langs = listOf(
            LanguageOption("ru", "Русский"),
            LanguageOption("en", "English")
        )
        every { localeService.supportedLanguages } returns langs

        val viewModel = createViewModel()

        assertEquals(langs, viewModel.state.value.supportedLanguages)
    }

    /**
     * init подписывается на combine трёх потоков: тема, уведомления, язык.
     * После первого эмита isLoading снимается, а State содержит
     * актуальные значения из репозитория и LocaleService.
     */
    @Test
    fun init_subscribesToCombineAndClearsIsLoading() = runTest(testDispatcher) {
        every { settingsRepository.themeModeFlow } returns flowOf(ThemeMode.DARK)
        every { settingsRepository.legendAddButtonVisibleFlow } returns kotlinx.coroutines.flow.flowOf(true)
            every { settingsRepository.notificationsEnabledFlow } returns flowOf(false)
        every { localeService.getCurrentLanguageFlow() } returns flowOf("en")

        val viewModel = createViewModel()

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(false, state.notificationsEnabled)
        assertEquals("en", state.currentLanguage)
    }

    // --- Действия ---

    /**
     * ChangeThemeMode делегирует в settingsRepository.setThemeMode
     * без эмита эффектов.
     */
    @Test
    fun changeThemeMode_delegatesToRepository() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsContract.Action.ChangeThemeMode(ThemeMode.LIGHT))

        coVerify { settingsRepository.setThemeMode(ThemeMode.LIGHT) }
    }

    /**
     * ToggleNotifications делегирует в settingsRepository.setNotificationsEnabled.
     */
    @Test
    fun toggleNotifications_delegatesToRepository() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsContract.Action.ToggleNotifications(false))

        coVerify { settingsRepository.setNotificationsEnabled(false) }
    }

    /**
     * Успешная смена языка: localeService.setLanguage вызван,
     * RecreateActivity эмитится, чтобы Activity перезапустилась.
     */
    @Test
    fun changeLanguage_callsSetLanguageAndEmitsRecreateActivity() = runTest(testDispatcher) {
        coEvery { localeService.setLanguage("en") } returns Result.Success(Unit)

        val viewModel = createViewModel()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(SettingsContract.Action.ChangeLanguage("en"))

        val effect = effectDeferred.await()
        assertEquals(SettingsContract.Effect.RecreateActivity, effect)
        coVerify { localeService.setLanguage("en") }
    }

    /**
     * Неподдерживаемый язык: LocaleService.setLanguage возвращает
     * Result.Error. ViewModel логирует ошибку и эмитит ShowError,
     * но НЕ RecreateActivity — иначе пользователь увидел бы
     * бесполезный restart без смены языка.
     */
    @Test
    fun changeLanguage_onUnsupportedCode_emitsShowError() = runTest(testDispatcher) {
        coEvery { localeService.setLanguage("xx") } returns
            Result.Error(AppError.ValidationError(ErrorCode.INVALID_ARGUMENT, "Unsupported language: xx"))

        val viewModel = createViewModel()

        val effectDeferred = async {
            withTimeoutOrNull(50L) { viewModel.effect.first() }
        }
        viewModel.onAction(SettingsContract.Action.ChangeLanguage("xx"))

        val effect = effectDeferred.await()
        assertEquals(SettingsContract.Effect.ShowError, effect)
        coVerify { localeService.setLanguage("xx") }
    }

    // --- Export ---

    /**
     * RequestExport при Result.Success: сервис вызван, эффект ExportSuccess,
     * isExporting снят после завершения.
     */
    @Test
    fun requestExport_onSuccess_emitsExportSuccessAndClearsIsExporting() = runTest(testDispatcher) {
        val stream = mockk<OutputStream>(relaxed = true)
        coEvery { exportDataService(stream) } returns Result.Success(Unit)

        val viewModel = createViewModel()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(SettingsContract.Action.RequestExport(stream))

        val effect = effectDeferred.await()
        assertEquals(SettingsContract.Effect.ExportSuccess, effect)
        assertEquals(false, viewModel.state.value.isExporting)
        coVerify { exportDataService(stream) }
    }

    /**
     * RequestExport при Result.Error: сервис вызван, эффект ExportError,
     * isExporting снят.
     */
    @Test
    fun requestExport_onError_emitsExportError() = runTest(testDispatcher) {
        val stream = mockk<OutputStream>(relaxed = true)
        coEvery { exportDataService(stream) } returns
            Result.Error(AppError.UnknownError(RuntimeException("export failed")))

        val viewModel = createViewModel()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(SettingsContract.Action.RequestExport(stream))

        val effect = effectDeferred.await()
        assertEquals(SettingsContract.Effect.ExportError, effect)
        assertEquals(false, viewModel.state.value.isExporting)
        coVerify { exportDataService(stream) }
    }

    // --- Import ---

    /**
     * ImportData при Result.Success: сервис вызван, эффект ImportSuccess,
     * isImporting снят.
     */
    @Test
    fun importData_onSuccess_emitsImportSuccessAndClearsIsImporting() = runTest(testDispatcher) {
        val stream = mockk<InputStream>(relaxed = true)
        coEvery { importDataService(stream) } returns Result.Success(Unit)

        val viewModel = createViewModel()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(SettingsContract.Action.ImportData(stream))

        val effect = effectDeferred.await()
        assertEquals(SettingsContract.Effect.ImportSuccess, effect)
        assertEquals(false, viewModel.state.value.isImporting)
        coVerify { importDataService(stream) }
    }

    /**
     * ImportData при Result.Error: сервис вызван, эффект ImportError,
     * isImporting снят.
     */
    @Test
    fun importData_onError_emitsImportError() = runTest(testDispatcher) {
        val stream = mockk<InputStream>(relaxed = true)
        coEvery { importDataService(stream) } returns
            Result.Error(AppError.UnknownError(RuntimeException("import failed")))

        val viewModel = createViewModel()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(SettingsContract.Action.ImportData(stream))

        val effect = effectDeferred.await()
        assertEquals(SettingsContract.Effect.ImportError, effect)
        assertEquals(false, viewModel.state.value.isImporting)
        coVerify { importDataService(stream) }
    }

    @Test
    fun toggleLegendAddButtonSuccess() = runTest {
        coEvery { settingsRepository.setLegendAddButtonVisible(any()) } returns Result.Success(Unit)
        val vm = SettingsViewModel(
            settingsRepository,
            localeService,
            exportDataService,
            importDataService
        )
        advanceUntilIdle()

        vm.onAction(SettingsContract.Action.ToggleLegendAddButton(false))
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setLegendAddButtonVisible(false) }
    }

    /**
     * P3 (QA) guard: повторный [SettingsContract.Action.RequestExport], пока
     * первый ещё не завершён (`isExporting == true`), НЕ запускает сервис
     * второй раз. Guard синхронный до `launch`, поэтому `return` срабатывает
     * сразу.
     */
    @Test
    fun requestExport_whileExportingInProgress_doesNotStartSecondRun() = runTest(testDispatcher) {
        val firstStream = mockk<OutputStream>(relaxed = true)
        val secondStream = mockk<OutputStream>(relaxed = true)
        val gate = CompletableDeferred<Unit>()

        coEvery { exportDataService(firstStream) } coAnswers {
            gate.await()
            Result.Success(Unit)
        }

        val viewModel = createViewModel()

        viewModel.onAction(SettingsContract.Action.RequestExport(firstStream))
        assertTrue(viewModel.state.value.isExporting)

        viewModel.onAction(SettingsContract.Action.RequestExport(secondStream))

        coVerify(exactly = 1) { exportDataService(firstStream) }
        coVerify(exactly = 0) { exportDataService(secondStream) }

        val effectDeferred = async { viewModel.effect.first() }
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(SettingsContract.Effect.ExportSuccess, effectDeferred.await())
        assertFalse(viewModel.state.value.isExporting)
    }

    /**
     * P3 (QA) Channel: два последовательных `RequestExport` (первый завершился,
     * `isExporting` сброшен) дают ДВА `ExportSuccess`. `Channel(BUFFERED)` не
     * теряет повторный эмит, guard не мешает легитимной последовательности.
     */
    @Test
    fun requestExport_twiceSequentially_bothEmitEffects() = runTest(testDispatcher) {
        val firstStream = mockk<OutputStream>(relaxed = true)
        val secondStream = mockk<OutputStream>(relaxed = true)
        coEvery { exportDataService(firstStream) } returns Result.Success(Unit)
        coEvery { exportDataService(secondStream) } returns Result.Success(Unit)

        val viewModel = createViewModel()
        val effects = async { viewModel.effect.take(2).toList() }

        viewModel.onAction(SettingsContract.Action.RequestExport(firstStream))
        advanceUntilIdle()
        viewModel.onAction(SettingsContract.Action.RequestExport(secondStream))
        advanceUntilIdle()

        val received = effects.await()
        assertEquals(2, received.size)
        assertTrue(received.all { it == SettingsContract.Effect.ExportSuccess })
        coVerify(exactly = 1) { exportDataService(firstStream) }
        coVerify(exactly = 1) { exportDataService(secondStream) }
    }

    /**
     * P3 (QA) Channel: `RequestExport` и `ImportData` идут параллельно,
     * флаги `isExporting`/`isImporting` независимы, оба эффекта доходят.
     */
    @Test
    fun exportAndImport_inParallel_bothEmitEffects() = runTest(testDispatcher) {
        val exportStream = mockk<OutputStream>(relaxed = true)
        val importStream = mockk<InputStream>(relaxed = true)
        val exportGate = CompletableDeferred<Unit>()
        val importGate = CompletableDeferred<Unit>()

        coEvery { exportDataService(exportStream) } coAnswers {
            exportGate.await()
            Result.Success(Unit)
        }
        coEvery { importDataService(importStream) } coAnswers {
            importGate.await()
            Result.Success(Unit)
        }

        val viewModel = createViewModel()
        val effects = async { viewModel.effect.take(2).toList() }

        viewModel.onAction(SettingsContract.Action.RequestExport(exportStream))
        viewModel.onAction(SettingsContract.Action.ImportData(importStream))

        assertTrue(viewModel.state.value.isExporting)
        assertTrue(viewModel.state.value.isImporting)

        exportGate.complete(Unit)
        importGate.complete(Unit)
        advanceUntilIdle()

        val received = effects.await()
        assertEquals(2, received.size)
        assertTrue(received.contains(SettingsContract.Effect.ExportSuccess))
        assertTrue(received.contains(SettingsContract.Effect.ImportSuccess))
    }
}
