package com.knittrack.app

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.knittrack.app.core.localization.LocaleService
import com.knittrack.app.core.logging.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Точка входа приложения.
 *
 * Инициализация языка запускается асинхронно в [applicationScope] — это
 * заменяет прежний `runBlocking`, который подвешивал главный поток на время
 * чтения DataStore (~50ms) и создавал jank на старте.
 *
 * Диспетчер [Dispatchers.Default]: инициализация локали не требует Main.
 * Чтение DataStore уходит на IO внутри самой библиотеки, мутация
 * Configuration и Locale.setDefault — потокобезопасны. Main-поток остаётся
 * свободным для первого кадра.
 *
 * Гарантия корректного первого кадра обеспечивается на уровне UI:
 * [com.knittrack.app.MainActivity] удерживает splash-экран, пока
 * [LocaleService.isInitialized] не станет true.
 */
@HiltAndroidApp
class KnitTracApplication : Application() {

    @Inject
    lateinit var localeService: LocaleService

    /**
     * Долгоживущий scope приложения. Не отменяется — живёт весь жизненный цикл
     * процесса. SupervisorJob — чтобы падение одного дочернего корутина не
     * отменяло остальные.
     *
     * Диспетчер [Dispatchers.Default], а не [Dispatchers.Main]: scope
     * предназначен для фоновой инициализации, ничего UI-специфичного здесь
     * быть не должно. Если в будущем понадобится Main — конкретная задача
     * должна явно указать его через launch(Dispatchers.Main).
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // В release DebugTree недоступен — plant file-logger, иначе
            // все вызовы Timber.e превращаются в no-op.
            Timber.plant(FileLoggingTree(File(filesDir, "logs")))
            Timber.plant(CrashlyticsTree())
        }

        applicationScope.launch {
            try {
                localeService.ensureLanguageInitialized()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize locale, using system default")
            }
        }
    }
}

/**
 * Дерево Timber, которое пересылает логи уровня ERROR и исключения в Firebase Crashlytics.
 *
 * Обычные логи не уходят (чтобы не засорять Crashlytics и не жечь квоту).
 * Работает только в release-сборках.
 */
private class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.ERROR) {
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.log("$tag: $message")
                t?.let { crashlytics.recordException(it) }
            } catch (e: Exception) {
                // Firebase не инициализирован (нет google-services.json или это unit-тест).
                // Не используем Timber, чтобы избежать рекурсии.
                android.util.Log.w("CrashlyticsTree", "Firebase not initialized", e)
            }
        }
    }
}
