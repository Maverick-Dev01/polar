package com.polar.app.ui.help

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.SharedFixtures
import com.polar.app.data.ThemeMode
import com.polar.app.help.HelpContent
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class HelpUiTest {
    @get:Rule val compose = createComposeRule()
    private val content = HelpContent.parse(SharedFixtures.file("help.json").readText())
    private val clicks = mutableListOf<String>()
    private var finished = 0
    private var unshown = 0

    /** Sin animaciones del sistema: también prueba la ruta de «reducir movimiento» (cuadro final estático). */
    @Before fun noAnimations() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun harness(present: List<String>, fontScale: Float = 1f, overlay: @androidx.compose.runtime.Composable (HelpTargets) -> Unit) {
        val targets = HelpTargets()
        compose.setContent {
            CompositionLocalProvider(LocalHelpTargets provides targets, LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PolarTheme(ThemeMode.LIGHT) {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().padding(top = 96.dp, bottom = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            present.forEach { id ->
                                Button(onClick = { clicks += id }, Modifier.helpTarget(id).testTag(id).height(56.dp).width(150.dp)) { Text(id) }
                            }
                        }
                        overlay(targets)
                    }
                }
            }
        }
    }

    private val tourIds = listOf("tool.fotos", "hoja", "tool.texto", "tool.filtros", "top.imprimir")

    private fun tour() = harness(tourIds) { t -> TourOverlay(content.recorrido, t, onFinish = { shown -> if (shown) finished++ else unshown++ }) }

    @Test fun tourAdvancesThroughFiveStepsAndEnds() {
        tour()
        compose.waitForIdle()
        content.recorrido.forEachIndexed { i, step ->
            compose.onNodeWithText(step.titulo).assertIsDisplayed()
            compose.onNodeWithText("Paso ${i + 1} de 5").assertExists()
            assertEquals("No debe terminar antes del último paso", 0, finished)
            compose.onNodeWithText(if (i == 4) "Listo" else "Siguiente").performClick()
            compose.waitForIdle()
        }
        assertEquals(1, finished)
    }

    @Test fun skipEndsTheTourAtOnce() {
        tour(); compose.waitForIdle()
        compose.onNodeWithText("Saltar").performClick()
        assertEquals(1, finished)
    }

    @Test fun lastStepHasNoSkipButDone() {
        tour(); compose.waitForIdle()
        repeat(4) { compose.onNodeWithText("Siguiente").performClick(); compose.waitForIdle() }
        compose.onNodeWithText("Saltar").assertDoesNotExist()
        compose.onNodeWithText("Listo").assertExists()
    }

    @Test fun missingTargetsAreSkippedInsteadOfGettingStuck() {
        harness(listOf("tool.fotos", "tool.filtros", "top.imprimir")) { t -> TourOverlay(content.recorrido, t, onFinish = { shown -> if (shown) finished++ else unshown++ }) }
        compose.waitForIdle()
        compose.onNodeWithText("Paso 1 de 3").assertExists()
        compose.onNodeWithText("Siguiente").performClick(); compose.waitForIdle()
        compose.onNodeWithText(content.recorrido[3].titulo).assertIsDisplayed() // Filtros: saltó la hoja y Texto
        compose.onNodeWithText("Paso 2 de 3").assertExists()
        compose.onNodeWithText("Siguiente").performClick(); compose.waitForIdle()
        compose.onNodeWithText("Listo").performClick()
        assertEquals(1, finished)
    }

    @Test fun tourEndsByItselfWhenNothingIsOnScreen() {
        harness(emptyList()) { t -> TourOverlay(content.recorrido, t, onFinish = { shown -> if (shown) finished++ else unshown++ }) }
        compose.waitForIdle()
        assertEquals("Sin pasos mostrados no cuenta como visto", 1, unshown)
        assertEquals(0, finished)
    }

    @Test fun tourBlocksTouchesToTheEditorBelow() {
        tour(); compose.waitForIdle()
        compose.onNodeWithTag("tool.fotos", useUnmergedTree = true).performTouchInput { click() }
        assertTrue("Un toque sobre el recorrido no debe llegar al botón de abajo", clicks.isEmpty())
    }

    @Test @Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
    fun bubbleStaysOnScreenWithLargeFont() {
        harness(tourIds, fontScale = 1.3f) { t -> TourOverlay(content.recorrido, t, onFinish = { shown -> if (shown) finished++ else unshown++ }) }
        compose.waitForIdle()
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        content.recorrido.forEach { step ->
            val title = compose.onNodeWithText(step.titulo).fetchSemanticsNode().boundsInRoot
            assertTrue("«${step.titulo}» se sale de la pantalla: $title vs $screen", title.left >= 0f && title.right <= screen.right + 1f && title.top >= 0f && title.bottom <= screen.bottom + 1f)
            val button = compose.onNodeWithText(if (step == content.recorrido.last()) "Listo" else "Siguiente")
            button.assertIsDisplayed()
            val b = button.fetchSemanticsNode().boundsInRoot
            assertTrue("El botón se sale: $b", b.right <= screen.right + 1f && b.bottom <= screen.bottom + 1f)
            button.performClick(); compose.waitForIdle()
        }
    }

    // ----- Modo «?» -----

    @Test fun helpModeShowsTheExplanationAndNeverRunsTheAction() {
        harness(listOf("top.deshacer", "tool.filtros")) { t -> HelpModeOverlay(content, t, onDone = { finished++ }) }
        compose.waitForIdle()
        compose.onNodeWithText("Toca un botón", substring = true).assertExists()
        // Un toque real sobre el botón de abajo: lo recibe la capa de ayuda.
        compose.onNodeWithTag("top.deshacer", useUnmergedTree = true).performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithText(content.control("top.deshacer")!!.texto).assertIsDisplayed()
        compose.onNodeWithTag("tool.filtros", useUnmergedTree = true).performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithText(content.control("tool.filtros")!!.texto).assertIsDisplayed()
        assertEquals("Tocar en modo «?» nunca ejecuta la acción", emptyList<String>(), clicks)
    }

    @Test fun helpModeControlsAreAccessibleByName() {
        harness(listOf("top.imprimir")) { t -> HelpModeOverlay(content, t, onDone = {}) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Imprimir").assertHasClickAction().performClick()
        compose.waitForIdle()
        compose.onNodeWithText(content.control("top.imprimir")!!.texto).assertIsDisplayed()
        assertTrue(clicks.isEmpty())
    }

    @Test fun helpModeDoneAndCloseWork() {
        harness(listOf("top.deshacer")) { t -> HelpModeOverlay(content, t, onDone = { finished++ }) }
        compose.waitForIdle()
        compose.onNodeWithTag("top.deshacer", useUnmergedTree = true).performTouchInput { click() }
        compose.onNodeWithText("Cerrar explicación").performClick()
        compose.onNodeWithText(content.control("top.deshacer")!!.texto).assertDoesNotExist()
        compose.onNodeWithText("Listo").performClick()
        assertEquals(1, finished)
    }

    // ----- Centro de ayuda -----

    private var went: String? = null
    private var toured = 0
    private fun helpScreen() = compose.setContent { PolarTheme(ThemeMode.LIGHT) { HelpScreen(content, onBack = { finished++ }, onGo = { went = it }, onTour = { toured++ }) } }

    @Test fun helpCenterSearchIgnoresAccentsAndFiltersByCategory() {
        helpScreen()
        compose.onNodeWithText("Buscar en la ayuda").performTextInput("CANCION")
        compose.onNodeWithText("QR de una canción").assertExists()
        compose.onNodeWithText("Crear un diseño").assertDoesNotExist()
        compose.onNodeWithContentDescription("Borrar búsqueda").performClick()
        compose.onNodeWithText("Crear un diseño").assertExists()
    }

    @Test fun helpCenterShowsAFriendlyEmptyState() {
        helpScreen()
        compose.onNodeWithText("Buscar en la ayuda").performTextInput("zzzqqq")
        compose.onNodeWithText("No encontramos nada con «zzzqqq»").assertExists()
        compose.onNodeWithText("Ver todos los temas").performClick()
        compose.onNodeWithText("Crear un diseño").assertExists()
    }

    @Test fun articleShowsStepsAndLlevameAhiGoesToItsDestino() {
        helpScreen()
        compose.onNodeWithText("Quitar el fondo de una foto").performClick()
        compose.onNodeWithText("Paso a paso").assertExists()
        compose.onNodeWithText(content.article("quitar-fondo")!!.pasosAqui.first()).assertExists()
        compose.onNodeWithText("Llévame ahí").performScrollTo().performClick()
        assertEquals("editor.encuadrar", went)
    }

    @Test fun articleWithoutDestinoHasNoLlevameAhi() {
        helpScreen()
        compose.onNodeWithText("Deshacer y rehacer").performClick()
        compose.onNodeWithText("Paso a paso").assertExists()
        compose.onNodeWithText("Llévame ahí").assertDoesNotExist()
    }

    @Test fun tourCardRepeatsTheTour() {
        helpScreen()
        compose.onNodeWithText("Ver el recorrido otra vez").performClick()
        assertEquals(1, toured)
    }

    @Test fun missingContentShowsAMessageInsteadOfCrashing() {
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { HelpScreen(null, onBack = {}, onGo = {}, onTour = {}) } }
        compose.onNodeWithText("No pudimos abrir la guía", substring = true).assertExists()
    }

    // ----- Registro de objetivos -----

    @Test fun registryKeepsTheOtherCopyWhenOneLeaves() {
        val t = HelpTargets()
        val a = Any(); val b = Any()
        t.update("x", a, androidx.compose.ui.geometry.Rect(0f, 0f, 10f, 10f))
        t.update("x", b, androidx.compose.ui.geometry.Rect(0f, 20f, 10f, 30f))
        t.remove("x", b)
        assertEquals(setOf("x"), t.visible())
        t.remove("x", a)
        assertTrue(t.visible().isEmpty())
    }
}
