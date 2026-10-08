package com.knittrac.app.platform.timer

import android.os.SystemClock
import com.knittrac.app.core.common.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Продакшн-реализация [TimeProvider] на системных часах Android.
 *
 * - [currentTimeMillis] → [System.currentTimeMillis] (wall clock, для БД).
 * - [elapsedRealtime] → [SystemClock.elapsedRealtime] (монотонное, для таймера).
 *
 * Находится в platform-слое, потому что зависит от Android SDK.
 * Для KMP-Desktop будет отдельная реализация (System.nanoTime / 1_000_000).
 */
@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {

    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
