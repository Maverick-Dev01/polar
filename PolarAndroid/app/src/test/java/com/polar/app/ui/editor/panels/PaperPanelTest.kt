package com.polar.app.ui.editor.panels

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.data.Units
import com.polar.app.model.*
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class PaperPanelTest {
    @get:Rule val compose = createComposeRule()
    private val log = mutableListOf<String>()
    private val cb = PaperCallbacks(
        onPaper = { log += "papel:${it.name}" }, onOrientation = { log += "orient:${it.name}" },
        onCustom = { w, h -> log += "custom:${w.toInt()}x${h.toInt()}" }, onGuides = {}, onCutStyle = {}, onBorders = {},
        onMargin = {}, onGestureStart = {}, onGestureEnd = {}
    )

    @Test
    fun choosingPaperAndOrientation() {
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { PaperPanel(PrintSettings(), Units.MM, cb) } }
        compose.onNodeWithText("A4").performClick()
        compose.onNodeWithText("Horizontal").performClick()
        assertEquals(listOf("papel:A4", "orient:LANDSCAPE"), log)
    }

    @Test
    fun customPaperValidatesRange() {
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { PaperPanel(PrintSettings(paperSize = PaperSize.CUSTOM), Units.MM, cb) } }
        compose.onNodeWithText("Ancho (mm)").performTextReplacement("50")
        compose.onNodeWithText("Aplicar medidas").performClick()
        compose.onNodeWithText("Usa medidas entre 80 y 600 mm.").assertExists()
        compose.onNodeWithText("Ancho (mm)").performTextReplacement("150")
        compose.onNodeWithText("Aplicar medidas").performClick()
        assertEquals(listOf("custom:150x279"), log)
    }

    @Test
    fun parseMeasureConvertsInches() {
        assertEquals(152.4, parseMeasure("6", Units.INCHES)!!, 0.01)
        assertEquals(100.0, parseMeasure("100,0", Units.MM)!!, 0.01)
        assertNull(parseMeasure("2", Units.INCHES))
        assertNull(parseMeasure("abc", Units.MM))
    }

    @Test
    fun marginAndBordersAreVisibleWithoutMoreOptionsInTheChosenUnits() {
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { PaperPanel(PrintSettings(margin = 72.0), Units.INCHES, cb) } }
        compose.onNodeWithText("Margen de la hoja").assertExists()
        compose.onNodeWithText("1.00 pulg.").assertExists()
        compose.onNodeWithText("Imprimir borde de las tarjetas").assertExists()
        compose.onNodeWithText("Más opciones").assertDoesNotExist()
    }

    @Test
    fun marginShowsMillimetersByDefault() {
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { PaperPanel(PrintSettings(margin = 24.0), Units.MM, cb) } }
        compose.onNodeWithText("8.47 mm").assertExists()
        compose.onNodeWithContentDescription("Margen de la hoja")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "8.47 mm"))
    }
}
