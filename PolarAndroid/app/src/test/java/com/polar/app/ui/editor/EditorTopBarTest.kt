package com.polar.app.ui.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EditorTopBarTest {
    @get:Rule val compose = createComposeRule()
    private val log = mutableListOf<String>()
    private val actions = EditorTopBarActions({ log += "atras" }, { log += "nombre" }, { log += "deshacer" }, { log += "rehacer" }, { log += "imprimir" }, {}, {}, {}, {})

    private fun show(fontScale: Float = 1f, name: String = "Boda de Ana y Luis en la playa con la familia") = compose.setContent {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            PolarTheme(ThemeMode.LIGHT) { EditorTopBar(name, "Foto + canción", "Guardado", SaveStatusKind.SAVED, canUndo = true, canRedo = true, actions = actions) }
        }
    }

    private fun bounds(description: String): Rect = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    private fun assertNoOverlap(vararg rects: Rect) {
        for (i in rects.indices) for (j in i + 1 until rects.size) {
            val a = rects[i]; val b = rects[j]
            assertFalse("Los controles $i y $j se enciman: $a vs $b", a.left < b.right - 0.5f && b.left < a.right - 0.5f && a.top < b.bottom - 0.5f && b.top < a.bottom - 0.5f)
        }
    }

    @Test
    fun redoRuleSwitchesAtWidthAndFontScale() {
        assertTrue(redoInMenu(360f, 1f)); assertTrue(redoInMenu(399f, 1f))
        assertFalse(redoInMenu(400f, 1f)); assertFalse(redoInMenu(411f, 1.15f))
        assertTrue(redoInMenu(411f, 1.3f)); assertFalse(redoInMenu(1280f, 1f))
    }

    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
    fun wideBarShowsEveryButtonWithAccessibleNamesWithoutOverlap() {
        show()
        compose.onNodeWithContentDescription("Volver a tus diseños").assertExists()
        compose.onNodeWithContentDescription("Más opciones").assertExists()
        compose.onNodeWithContentDescription("Imprimir").assertExists()
        assertNoOverlap(bounds("Volver a tus diseños"), bounds("Deshacer"), bounds("Rehacer"), bounds("Más opciones"),
            compose.onNodeWithContentDescription("Imprimir").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Rehacer").performClick()
        compose.onNodeWithContentDescription("Imprimir").performClick()
        assertEquals(listOf("rehacer", "imprimir"), log)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
    fun narrowBarWithLargeFontMovesRedoToMenuAndNothingOverlaps() {
        show(fontScale = 1.3f)
        compose.onNodeWithContentDescription("Rehacer").assertDoesNotExist()
        val print = compose.onNodeWithContentDescription("Imprimir").fetchSemanticsNode().boundsInRoot
        assertNoOverlap(bounds("Volver a tus diseños"), bounds("Deshacer"), bounds("Más opciones"), print)
        assertTrue("Imprimir debe caber en pantalla", print.right <= 360f * compose.density.density + 1f)
        compose.onNodeWithContentDescription("Guardado").assertExists()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Rehacer").performClick()
        assertEquals(listOf("rehacer"), log)
    }
}
