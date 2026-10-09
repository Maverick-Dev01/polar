package com.polar.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object HomeRoute
/** `landing`: «moldes.asistente» o «mis-moldes» abre eso directo (desde Ayuda). */
@Serializable data class CatalogRoute(val landing: String? = null)
@Serializable data class EditorRoute(val projectId: String, val notice: String? = null, val landing: String? = null)
@Serializable object SettingsRoute
/** `projectId`: el diseño abierto desde el que se pidió la ayuda (para «Llévame ahí»). */
@Serializable data class HelpRoute(val projectId: String? = null)
