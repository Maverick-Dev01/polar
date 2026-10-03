package com.polar.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.ui.onboarding.OnboardingScreen
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun threeStepsThenDone() {
        var done = false
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { OnboardingScreen(onDone = { done = true }) } }
        compose.onNodeWithText("Elige un diseño").assertExists()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Pon tus fotos").assertExists()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Empezar").performClick()
        assertTrue(done)
    }

    @Test
    fun skipFinishesImmediately() {
        var done = false
        compose.setContent { PolarTheme(ThemeMode.DARK) { OnboardingScreen(onDone = { done = true }) } }
        compose.onNodeWithText("Saltar").performClick()
        assertTrue(done)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp")
    fun onboardingWorksAtLargeFontOnSmallScreen() {
        var done = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                PolarTheme(ThemeMode.LIGHT) { OnboardingScreen(onDone = { done = true }) }
            }
        }
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Empezar").performClick()
        assertTrue(done)
    }
}
