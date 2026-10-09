package com.polar.app.ui.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
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
}
