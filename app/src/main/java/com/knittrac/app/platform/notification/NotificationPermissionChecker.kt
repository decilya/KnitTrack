package com.knittrac.app.platform.notification

/**
 * Проверка разрешения на показ уведомлений (POST_NOTIFICATIONS).
 *
 * Вынесено в отдельный интерфейс (DIP):
 * - ViewModel зависит от абстракции, не от Android Framework.
 * - Легко мокается в unit-тестах.
 *
 * На API < 33 (Android 13) разрешение не требуется — метод возвращает true.
 * На API 33+ проверяется через NotificationManagerCompat.areNotificationsEnabled().
 */
interface NotificationPermissionChecker {
    /**
     * @return true, если уведомления разрешены или не требуются (API < 33).
     */
    fun isGranted(): Boolean
}
