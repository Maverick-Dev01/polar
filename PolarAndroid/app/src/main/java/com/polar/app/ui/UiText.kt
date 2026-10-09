package com.polar.app.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Texto de interfaz que viaja desde un ViewModel sin perder su recurso de strings.xml. */
data class UiText(@StringRes val id: Int, val args: List<Any> = emptyList()) {
    fun resolve(context: Context): String = context.getString(id, *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray())
}

@Composable
fun UiText.resolve(): String = resolve(LocalContext.current)
