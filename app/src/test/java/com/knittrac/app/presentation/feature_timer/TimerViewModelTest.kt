package com.knittrac.app.presentation.feature_timer

import androidx.lifecycle.SavedStateHandle
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.service.GetAllProjectsService
import com.knittrac.app.domain.service.MarkNextRowUseCase
import com.knittrac.app.domain.service.SaveSessionService
import com.knittrac.app.domain.service.TimerService
import com.knittrac.app.domain.service.TimerState
import com.knittrac.app.platform.notification.NotificationPermissionChecker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты для [TimerViewModel].
 *
 * Покрывают:
 * - проверку разрешения POST_NOTIFICATIONS перед стартом таймера;
 * - защиту от запуска без выбранного проекта;
 * - идемпотентность сохранения сессии (double-tap guard).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var timerManager: TimerService
    private lateinit var saveSessionService: SaveSessionService
    private lateinit var getAllProjectsService: GetAllProjectsService
    private lateinit var markNextRowUseCase: MarkNextRowUseCase
    private lateinit var permissionChecker: NotificationPermissionChecker

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        timerManager = mockk(relaxed = true)
        saveSessionService = mockk()
        getAllProjectsService = mockk()
        markNextRowUseCase = mockk()
        permissionChecker = mockk()

        // combine в init ViewModel подписывается на StateFlow.
        // relaxed mock без этих every упал бы на cast.
        every { timerManager.elapsedTime } returns MutableStateFlow(0L)
        every { timerManager.state } returns MutableStateFlow(TimerState.IDLE)
        every { timerManager.sessionStartTime } returns 0L

        coEvery { getAllProjectsService(Unit) } returns flowOf(Result.Success(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Permission granted: таймер стартует, RequestNotificationPermission
     * не эмитится.
     */
    @Test
    fun permissionGranted_startDoesNotEmitPermissionRequest() = runTest {
        every { permissionChecker.isGranted() } returns true

        val viewModel = TimerViewModel(
            timerManager,
            saveSessionService,
            getAllProjectsService,
            markNextRowUseCase,
            permissionChecker,
            SavedStateHandle()
        )

        viewModel.onIntent(TimerContract.Intent.SelectProject(1L))

        val effectDeferred = async {
            withTimeoutOrNull(50L) { viewModel.effect.first() }
        }
        viewModel.onIntent(TimerContract.Intent.Start)

        val effect = effectDeferred.await()

        verify { timerManager.start() }
        verify { permissionChecker.isGranted() }
        assertNull(
            "RequestNotificationPermission не должен эмититься при granted, получен: $effect",
            effect
        )
    }

    /**
     * Permission denied: таймер всё равно стартует (permission не блокирует),
     * но эмитится RequestNotificationPermission, чтобы Screen показал диалог.
     */
    @Test
    fun permissionDenied_startEmitsPermissionRequest() = runTest {
        every { permissionChecker.isGranted() } returns false

        val viewModel = TimerViewModel(
            timerManager,
            saveSessionService,
            getAllProjectsService,
            markNextRowUseCase,
            permissionChecker,
            SavedStateHandle()
        )

        viewModel.onIntent(TimerContract.Intent.SelectProject(1L))

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onIntent(TimerContract.Intent.Start)

        val effect = effectDeferred.await()

        assertEquals(
            "Ожидался RequestNotificationPermission",
            TimerContract.Effect.RequestNotificationPermission,
            effect
        )
        verify { timerManager.start() }
        verify { permissionChecker.isGranted() }
    }

    /**
     * Без выбранного проекта Start эмитит NavigateToProjects
     * и НЕ вызывает timerManager.start().
     */
    @Test
    fun startWithoutProjectId_navigatesToProjects() = runTest {
        every { permissionChecker.isGranted() } returns true

        val viewModel = TimerViewModel(
            timerManager,
            saveSessionService,
            getAllProjectsService,
            markNextRowUseCase,
            permissionChecker,
            SavedStateHandle()
        )

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onIntent(TimerContract.Intent.Start)

        val effect = effectDeferred.await()

        assertEquals(TimerContract.Effect.NavigateToProjects, effect)
        verify(exactly = 0) { timerManager.start() }
    }

    /**
     * Double-tap SaveSession: второй вызов игнорируется, пока первый
     * не завершён. saveSessionService вызывается ровно один раз.
     */
    @Test
    fun doubleTapSaveSession_secondCallIgnored() = runTest {
        every { permissionChecker.isGranted() } returns true
        every { timerManager.elapsedTime } returns MutableStateFlow(120L)
        every { timerManager.sessionStartTime } returns 1_000L

        // Заставляем сохранение "висеть" — иначе на Unconfined оно завершится
        // мгновенно, и второй вызов пройдёт guard.
        coEvery { saveSessionService(any()) } coAnswers {
            delay(100)
            Result.Success(1L)
        }

        val viewModel = TimerViewModel(
            timerManager,
            saveSessionService,
            getAllProjectsService,
            markNextRowUseCase,
            permissionChecker,
            SavedStateHandle()
        )
        viewModel.onIntent(TimerContract.Intent.SelectProject(1L))

        viewModel.onIntent(TimerContract.Intent.SaveSession)
        viewModel.onIntent(TimerContract.Intent.SaveSession)

        advanceUntilIdle()

        coVerify(exactly = 1) { saveSessionService(any()) }
        assertEquals(false, viewModel.state.value.isSaving)
    }

    /**
     * IncrementRow увеличивает rowCount на 1. Reset сбрасывает в 0.
     */
    @Test
    fun incrementRow_increasesCount() = runTest {
        every { permissionChecker.isGranted() } returns true
        val viewModel = TimerViewModel(timerManager, saveSessionService, getAllProjectsService, markNextRowUseCase, permissionChecker, SavedStateHandle())

        assertEquals(0, viewModel.state.value.rowCount)
        viewModel.onIntent(TimerContract.Intent.IncrementRow)
        assertEquals(1, viewModel.state.value.rowCount)

        viewModel.onIntent(TimerContract.Intent.Reset)
        assertEquals(0, viewModel.state.value.rowCount)
    }

    /**
     * DecrementRow уменьшает rowCount на 1, если он больше нуля.
     */
    @Test
    fun decrementRow_decreasesCount() = runTest {
        every { permissionChecker.isGranted() } returns true
        val viewModel = TimerViewModel(timerManager, saveSessionService, getAllProjectsService, markNextRowUseCase, permissionChecker, SavedStateHandle())

        repeat(5) { viewModel.onIntent(TimerContract.Intent.IncrementRow) }
        assertEquals(5, viewModel.state.value.rowCount)

        viewModel.onIntent(TimerContract.Intent.DecrementRow)
        assertEquals(4, viewModel.state.value.rowCount)
    }

    /**
     * DecrementRow на нуле оставляет rowCount = 0 (coerceAtLeast).
     */
    @Test
    fun decrementRow_atZero_staysZero() = runTest {
        every { permissionChecker.isGranted() } returns true
        val viewModel = TimerViewModel(timerManager, saveSessionService, getAllProjectsService, markNextRowUseCase, permissionChecker, SavedStateHandle())

        assertEquals(0, viewModel.state.value.rowCount)
        viewModel.onIntent(TimerContract.Intent.DecrementRow)
        assertEquals(0, viewModel.state.value.rowCount)
    }

    /**
     * SelectProject переносит targetRowCount из выбранного проекта в State.
     */
    @Test
    fun selectProject_setsTargetRowCount() = runTest {
        every { permissionChecker.isGranted() } returns true
        val project = Project(
            id = 1L,
            name = ProjectName.create("Test"),
            category = Category.KNITTING,
            createdAt = 0L,
            updatedAt = 0L,
            syncStatus = SyncStatus.PENDING,
            targetRowCount = RowCount.create(120)
        )
        coEvery { getAllProjectsService(Unit) } returns flowOf(Result.Success(listOf(project)))

        val viewModel = TimerViewModel(timerManager, saveSessionService, getAllProjectsService, markNextRowUseCase, permissionChecker, SavedStateHandle())
        viewModel.onIntent(TimerContract.Intent.SelectProject(1L))

        assertEquals(120, viewModel.state.value.targetRowCount)
    }

    @Test
    fun init_restoresRowCountAndProjectIdFromSavedStateHandle() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf<String, Any>("projectId" to 5L, "rowCount" to 42))
        val viewModel = TimerViewModel(
            timerManager, saveSessionService, getAllProjectsService, markNextRowUseCase, permissionChecker, savedStateHandle
        )

        assertEquals(5L, viewModel.state.value.projectId)
        assertEquals(42, viewModel.state.value.rowCount)
    }
}
