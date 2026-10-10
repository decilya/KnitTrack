package com.knittrac.app.platform.notification

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [NotificationPermissionChecker] на Android.
 *
 * На API < 33 (Android 13) POST_NOTIFICATIONS не требует runtime-запроса —
 * уведомления разрешены автоматически, кроме случая, когда пользователь
 * вручную отключил их в системных настройках. Метод
 * [NotificationManagerCompat.areNotificationsEnabled] учитывает оба случая
 * на всех API.
 *
 * @param context Контекст приложения для доступа к NotificationManager.
 */
@Singleton
class AndroidNotificationPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationPermissionChecker {

    override fun isGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // API 26-32: разрешение выдаётся автоматически,
            // runtime-запрос POST_NOTIFICATIONS не нужен.
            return true
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
