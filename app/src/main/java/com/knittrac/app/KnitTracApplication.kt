package com.knittrac.app

import android.app.Application
import com.knittrac.app.core.localization.LocaleManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * Точка входа приложения.
 *
 * В onCreate синхронно инициализируется язык:
 * - На первом запуске — берётся язык системы (если поддерживается) или fallback "en".
 * - На последующих — применяется сохранённый выбор пользователя.
 *
 * Синхронность гарантирует, что первый кадр UI отрисуется на нужном языке.
 * runBlocking оправдан: DataStore читает маленький файл с диска (< 50ms).
 * try/catch защищает от редких I/O сбоев, чтобы не уронить приложение.
 */
@HiltAndroidApp
class KnitTracApplication : Application() {

    @Inject
    lateinit var localeManager: LocaleManager

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Синхронно применяем язык ДО первого рендера UI
        // runBlocking — pragmatic trade-off для гарантии языка на первом кадре.
        // DataStore читается с диска, обычно < 50ms.
        // try/catch — защита от редких сбоев I/O, чтобы не уронить приложение.
        runBlocking {
            try {
                localeManager.ensureLanguageInitialized()
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize locale, using system default")
            }
        }
    }
}
