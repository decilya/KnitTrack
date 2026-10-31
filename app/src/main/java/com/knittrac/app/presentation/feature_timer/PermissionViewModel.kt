package com.knittrac.app.presentation.feature_timer

import androidx.lifecycle.ViewModel
import com.knittrac.app.platform.notification.NotificationPermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel, отвечающая за проверку разрешения POST_NOTIFICATIONS (Android 13+).
 *
 * Выделена из [TimerViewModel] в 7g-D (F+):
 * - SRP: у TimerViewModel — 1 причина изменения (таймер/сессия/прогресс),
 *   у PermissionViewModel — 1 причина (permission).
 * - Clean Arch: permission — Android Framework забота, не бизнес-логика.
 *   TimerViewModel больше не зависит от [NotificationPermissionChecker].
 * - ISP: TimerScreen использует обе ViewModel независимо; тесты разделены.
 *
 * Синхронный [isGranted] достаточен: разрешение проверяется в момент нажатия
 * Start, реактивность не требуется (Android-диалог — одноразовое событие).
 */
@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val checker: NotificationPermissionChecker
) : ViewModel() {

    /**
     * @return true, если уведомления разрешены или не требуются (API < 33).
     */
    fun isGranted(): Boolean = checker.isGranted()
}
