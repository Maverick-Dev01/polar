package com.polar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.polar.app.data.AppSettings
import com.polar.app.data.ThemeMode
import com.polar.app.ui.ProvideLayout
import com.polar.app.ui.navigation.PolarNavHost
import com.polar.app.ui.theme.PolarTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PolarApplication).container
        // Leer ajustes fuera del render evita que el splash espere el mismo primer frame que está bloqueando.
        val settingsState = container.settings.settings.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
        splash.setKeepOnScreenCondition { settingsState.value == null }
        setContent {
            val settings: AppSettings? by settingsState.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            // Las barras del sistema siguen el tema elegido en Ajustes, no el del sistema.
            val dark = when (current.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                )
                onDispose { }
            }
            PolarTheme(current.theme) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    ProvideLayout { PolarNavHost(container, startOnboarding = !current.onboardingSeen) }
                }
            }
        }
    }

    private companion object {
        val LIGHT_SCRIM = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
