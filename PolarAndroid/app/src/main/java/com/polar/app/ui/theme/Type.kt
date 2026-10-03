package com.polar.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.polar.app.R

val GelasioFamily = FontFamily(Font(R.font.gelasio, FontWeight.Normal), Font(R.font.gelasio, FontWeight.Medium))
val CaveatFamily = FontFamily(Font(R.font.caveat))

private val base = Typography()

val PolarTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    headlineLarge = base.headlineLarge.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    headlineMedium = base.headlineMedium.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    headlineSmall = base.headlineSmall.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall,
    bodyLarge = base.bodyLarge, bodyMedium = base.bodyMedium, bodySmall = base.bodySmall,
    labelLarge = base.labelLarge, labelMedium = base.labelMedium, labelSmall = base.labelSmall
)

/** Marca grande "Polar" del inicio. */
val BrandStyle = TextStyle(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp)
/** Nombre escrito a mano bajo cada polaroid. */
val HandwrittenStyle = TextStyle(fontFamily = CaveatFamily, fontSize = 21.sp, lineHeight = 24.sp)
