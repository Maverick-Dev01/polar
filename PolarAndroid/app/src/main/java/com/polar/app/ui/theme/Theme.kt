package com.polar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.polar.app.data.ThemeMode

@Composable
fun PolarTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalPolarColors provides if (dark) DarkExtra else LightExtra, LocalContentColor provides scheme.onBackground) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PolarTypography,
            shapes = PolarShapes,
            content = content
        )
    }
}
