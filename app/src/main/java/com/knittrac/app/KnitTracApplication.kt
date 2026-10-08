package com.knittrac.app

import android.app.Application
import com.knittrac.app.core.localization.LocaleManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Точка входа приложения.
 *
 * Инициализация языка запускается асинхронно в [applicationScope] — это
 * заменяет прежний `runBlocking`, который подвешивал главный поток на время
 * чтения DataStore (~50ms) и создавал jank на старте.
 *
 * Гарантия корректного первого кадра обеспечивается на уровне UI:
 * [com.knittrac.app.MainActivity] удерживает splash-экран, пока
 * [LocaleManager.isInitialized] не станет true.
 */
@HiltAndroidApp
class KnitTracApplication : Application() {

    @Inject
    lateinit var localeManager: LocaleManager

    /**
     * Долгоживущий scope приложения. Не отменяется — живёт весь жизненный цикл
     * процесса. SupervisorJob — чтобы падение одного дочернего корутина не
     * отменяло остальные.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        applicationScope.launch {
            try {
                localeManager.ensureLanguageInitialized()
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize locale, using system default")
            }
        }
    }
}
