package com.polar.app.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

enum class LayoutKind { COMPACT, EXPANDED }

val LocalLayout = staticCompositionLocalOf { LayoutKind.COMPACT }

/** ≥ 840dp = tres paneles (barra lateral + hoja + panel fijo); si no, barra inferior y paneles a media altura. */
@Composable
fun ProvideLayout(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val kind = if (maxWidth >= 840.dp) LayoutKind.EXPANDED else LayoutKind.COMPACT
        CompositionLocalProvider(LocalLayout provides kind) { content() }
    }
}
