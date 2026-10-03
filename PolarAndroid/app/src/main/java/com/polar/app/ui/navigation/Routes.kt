package com.polar.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object HomeRoute
@Serializable object CatalogRoute
@Serializable data class EditorRoute(val projectId: String, val notice: String? = null)
@Serializable object SettingsRoute
