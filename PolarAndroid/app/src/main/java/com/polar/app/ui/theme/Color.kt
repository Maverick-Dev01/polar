package com.polar.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF7A293B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2DDE1), onPrimaryContainer = Color(0xFF4D1522),
    secondary = Color(0xFF92394A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2DDE1), onSecondaryContainer = Color(0xFF4D1522),
    background = Color(0xFFF7F2EB), onBackground = Color(0xFF2B2221),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF2B2221),
    surfaceVariant = Color(0xFFF1EAE2), onSurfaceVariant = Color(0xFF6B5D5A),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFBF7F2),
    surfaceContainer = Color(0xFFF1EAE2), surfaceContainerHigh = Color(0xFFEDE5DC), surfaceContainerHighest = Color(0xFFE8E0D6),
    outline = Color(0xFFB5A6A1), outlineVariant = Color(0xFFE4DAD0),
    inverseSurface = Color(0xFF362F2E), inverseOnSurface = Color(0xFFF8EEEC), inversePrimary = Color(0xFFFFB2BC),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF), scrim = Color(0xFF000000)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2BC), onPrimary = Color(0xFF561D2B),
    primaryContainer = Color(0xFF5E2231), onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = Color(0xFFFFB2BC), onSecondary = Color(0xFF561D2B),
    secondaryContainer = Color(0xFF5E2231), onSecondaryContainer = Color(0xFFFFD9DE),
    background = Color(0xFF1D1A19), onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF2A2524), onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF332D2C), onSurfaceVariant = Color(0xFFB9AAA7),
    surfaceContainerLowest = Color(0xFF181514), surfaceContainerLow = Color(0xFF241F1E),
    surfaceContainer = Color(0xFF2A2524), surfaceContainerHigh = Color(0xFF332D2C), surfaceContainerHighest = Color(0xFF3A3332),
    outline = Color(0xFF7D6D6A), outlineVariant = Color(0xFF3D3534),
    inverseSurface = Color(0xFFEDE0DD), inverseOnSurface = Color(0xFF362F2E), inversePrimary = Color(0xFF7A293B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), scrim = Color(0xFF000000)
)

@Immutable
data class PolarExtraColors(
    val table: Color,
    val warningContainer: Color, val onWarningContainer: Color,
    val successContainer: Color, val onSuccessContainer: Color
)

val LightExtra = PolarExtraColors(Color(0xFFE8E3DC), Color(0xFFFBEBD0), Color(0xFF7A5200), Color(0xFFE2EFE5), Color(0xFF2F6B45))
val DarkExtra = PolarExtraColors(Color(0xFF141211), Color(0xFF3A2D12), Color(0xFFF2C46B), Color(0xFF1F3427), Color(0xFF8FD3A3))

val LocalPolarColors = staticCompositionLocalOf { LightExtra }

object PolarColors {
    val table: Color @Composable get() = LocalPolarColors.current.table
    val warningContainer: Color @Composable get() = LocalPolarColors.current.warningContainer
    val onWarningContainer: Color @Composable get() = LocalPolarColors.current.onWarningContainer
    val successContainer: Color @Composable get() = LocalPolarColors.current.successContainer
    val onSuccessContainer: Color @Composable get() = LocalPolarColors.current.onSuccessContainer
}

/** Líneas de tercios sobre la foto en Encuadrar (blanco translúcido, legible sobre cualquier imagen). */
val CropGuide = Color(0x8CFFFFFF)

/** Papel impreso y fotos de muestra: no cambian con el tema, porque el papel siempre es blanco. */
object PaperColors {
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF7A293B)
    val InkDark = Color(0xFF4D1522)
    val PhotoRose = Color(0xFFC9A99A)
    val PhotoSage = Color(0xFF9FB3A3)
    val PhotoSky = Color(0xFFA9B7C9)
    val Table = Color(0xFFE8E3DC)
}
