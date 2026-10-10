package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.service.GetAllProjectsService
import com.knittrac.app.domain.service.SaveSessionService
import com.knittrac.app.domain.service.TimerManager
import com.knittrac.app.domain.service.TimerState
import com.knittrac.app.platform.notification.NotificationPermissionChecker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты для [TimerViewModel].
 *
 * Покрывают проверку разрешения POST_NOTIFICATIONS перед стартом таймера:
 * - granted → таймер стартует, effect не эмитится;
 * - denied → таймер всё равно стартует (permission не блокирует),
 *   но эмитится [TimerContract.Effect.RequestNotificationPermission],
 *   чтобы Screen запустил системный диалог.
 *
 * TimerManager мокается как StateFlow-провайдер, чтобы combine в init
 * получил начальные значения и ViewModel корректно эмитила State.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var timerManager: TimerManager
    private lateinit var saveSessionService: SaveSessionService
    private lateinit var getAllProjectsService: GetAllProjectsService
    private lateinit var permissionChecker: NotificationPermissionChecker

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        timerManager = mockk(relaxed = true)
        saveSessionService = mockk()
        getAllProjectsService = mockk()
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
     * не эмитится. Проверяем через withTimeoutOrNull(50) — если effect
     * не пришёл за виртуальные 50 мс, значит не эмитился.
     */
    @Test
    fun `permission granted — start не эмитит RequestNotificationPermission`() = runTest {
        every { permissionChecker.isGranted() } returns true

        val viewModel = TimerViewModel(
            timerManager,
            saveSessionService,
            getAllProjectsService,
            permissionChecker
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
     * Permission denied: таймер стартует (работа не блокируется),
     * RequestNotificationPermission эмитится, чтобы Screen показал
     * системный диалог.
     */
    @Test
    fun `permission denied — start эмитит RequestNotificationPermission`() = runTest {
        every { permissionChecker.isGranted() } returns false

        val viewModel = TimerViewModel(
            timerManager,
            saveSessionService,
            getAllProjectsService,
            permissionChecker
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
     * Без выбранного проекта Start эмитит NavigateToProjects и НЕ
     * вызывает timerManager.start() — защита от запуска «в никуда».
     */
    @Test
    fun `start без projectId эмитит NavigateToProjects и не стартует таймер`() = runTest {
        every { permissionChecker.isGranted() } returns true

        val viewModel = TimerViewModel(
            timerManager,
            saveSessionService,
            getAllProjectsService,
            permissionChecker
        )

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onIntent(TimerContract.Intent.Start)

        val effect = effectDeferred.await()

        assertEquals(TimerContract.Effect.NavigateToProjects, effect)
        verify(exactly = 0) { timerManager.start() }
    }
}
