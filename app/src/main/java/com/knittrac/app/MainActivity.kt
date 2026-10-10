package com.knittrac.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knittrac.app.core.localization.LocaleManager
import com.knittrac.app.data.repository.SettingsRepository
import com.knittrac.app.domain.entity.ThemeMode
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
 *   runBlocking из Application.onCreate и избежать jank на старте.
 *
 * Подписка на [SettingsRepository.themeModeFlow]:
 * - ThemeMode.SYSTEM → следуем за системной темой (isSystemInDarkTheme).
 * - ThemeMode.LIGHT  → принудительно светлая.
 * - ThemeMode.DARK   → принудительно тёмная.
 * Без этой подписки выбор пользователя в настройках не влиял на UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var localeManager: LocaleManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !localeManager.isInitialized.value
        }

        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            KnitTracTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }
}
