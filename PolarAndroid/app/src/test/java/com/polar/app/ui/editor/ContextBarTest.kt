package com.polar.app.ui.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.ui.theme.PolarTheme
import androidx.compose.ui.layout.boundsInRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ContextBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fullCardShowsAllActions() {
        var last = ""
        compose.setContent {
            PolarTheme(ThemeMode.LIGHT) {
                ContextBar(true, { last = "cambiar" }, { last = "encuadrar" }, { last = "fondo" }, { last = "girar" }, { last = "texto" }, { last = "quitar" })
            }
        }
        compose.onNodeWithText("Quitar").performClick(); assertEquals("quitar", last)
        compose.onNodeWithText("Quitar fondo").performClick(); assertEquals("fondo", last)
        compose.onNodeWithText("Texto").performClick(); assertEquals("texto", last)
        compose.onNodeWithText("Encuadrar").performClick(); assertEquals("encuadrar", last)
    }

    @Test
    fun emptyCardOffersToPlacePhoto() {
        var changed = false
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { ContextBar(false, { changed = true }, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("Poner una foto aquí").performClick()
        assertEquals(true, changed)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
    fun narrowOrLargeFontWrapsIntoTwoEqualRowsWithEveryLabelVisible() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                PolarTheme(ThemeMode.LIGHT) { ContextBar(true, {}, {}, {}, {}, {}, {}) }
            }
        }
        listOf("Cambiar", "Encuadrar", "Quitar fondo", "Girar", "Texto", "Quitar").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        val b = listOf("Cambiar", "Encuadrar", "Quitar fondo").map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        val d = listOf("Girar", "Texto", "Quitar").map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        assertTrue("Dos filas", d.first().top > b.first().bottom - 1f)
        assertEquals(b[0].width, b[1].width, 1f)
        assertEquals(b[1].width, b[2].width, 1f)
    }
}
