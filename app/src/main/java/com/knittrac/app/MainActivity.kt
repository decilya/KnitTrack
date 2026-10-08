package com.knittrac.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.knittrac.app.core.localization.LocaleManager
import com.knittrac.app.presentation.navigation.AppNavHost
import com.knittrac.app.ui.theme.KnitTracTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Главная Activity приложения.
 *
 * Использует SplashScreen API (androidx.core:core-splashscreen):
 * - [installSplashScreen] вызывается ДО super.onCreate — требование API.
 * - [setKeepOnScreenCondition] удерживает splash, пока
 *   [LocaleManager.isInitialized] == false. Это позволяет убрать
 *   `runBlocking` из Application.onCreate и избежать jank на старте.
 * - Когда флаг становится true (в том числе при ошибке инициализации),
 *   splash скрывается, применяется `postSplashScreenTheme`, и UI
 *   показывается уже на правильном языке.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var localeManager: LocaleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Удерживаем splash до инициализации локали.
        splashScreen.setKeepOnScreenCondition {
            !localeManager.isInitialized.value
        }

        setContent {
            KnitTracTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }
}
