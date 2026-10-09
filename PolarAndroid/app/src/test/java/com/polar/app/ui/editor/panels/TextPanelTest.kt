package com.polar.app.ui.editor.panels

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.ThemeMode
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.editor.TextScope
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class TextPanelTest {
    @get:Rule val compose = createComposeRule()
    private val log = mutableListOf<String>()
    private val applied = mutableListOf<TextAppearance>()
    private var base = TextAppearance()
    private val cb = TextCallbacks(
        onRole = { log += "rol:${it.key}" }, onRoleText = { r, t -> log += "cancion:${r.key}:$t" }, onScope = { log += "alcance:${it.name}" }, onText = { log += "texto:$it" },
        onFocus = {}, onRevert = { log += "volver" }, onApplyAll = { log += "todas" }, onAppearance = { change -> applied += change(base) },
        onReset = {}, onDateSource = { log += "fecha:${it.name}" }, onChosenDate = {}, onDateStyle = {}, onSongUrl = {},
        onGestureStart = {}, onGestureEnd = {}
    )

    private fun show(state: EditorUiState) = compose.setContent { PolarTheme(ThemeMode.LIGHT) { TextPanel(textPanelState(state), cb) } }

    @Test
    fun typingAndApplyToAll() {
        val p = ProjectEdits.setText(PolarProject().normalized(), TextRole.TITLE, "propio", card = 3)
        show(EditorUiState(loading = false, project = p))
        compose.onNodeWithText("1 tarjeta tiene su propio texto").assertExists()
        compose.onNodeWithText("Aplicar a todas").performClick()
        compose.onNodeWithText("Nuestros momentos").performTextReplacement("Lu y Max")
        assertEquals(listOf("todas", "texto:Lu y Max"), log)
    }

    @Test
    fun ownTextShowsRevert() {
        val p = ProjectEdits.setText(PolarProject().normalized(), TextRole.TITLE, "El brindis", card = 1)
        show(EditorUiState(loading = false, project = p, selectedSlot = 1, textScope = TextScope.CARD))
        compose.onNodeWithText("Sólo tarjeta 2").assertIsSelected()
        compose.onNodeWithText("Volver al texto general").performClick()
        assertEquals(listOf("volver"), log)
    }

    @Test
    fun dateRoleOffersSources() {
        show(EditorUiState(loading = false, project = PolarProject().normalized(), textRole = TextRole.DATE))
        compose.onNodeWithText("De la foto").performClick()
        assertEquals(listOf("fecha:PHOTO"), log)
    }

    private fun showTitle() = show(EditorUiState(loading = false, project = PolarProject().normalized()))

    @Test
    fun fontChipAppliesItsFont() {
        showTitle()
        compose.onNodeWithText("Caveat").performScrollTo().performClick()
        assertEquals("Caveat", applied.single().fontName)
    }

    @Test
    fun colorSwatchAndDesignColor() {
        showTitle()
        compose.onNodeWithContentDescription("Rojo").performScrollTo().performClick()
        assertEquals("C34048", applied.single().hex)
        base = TextAppearance(hex = "C34048")
        compose.onAllNodesWithText("Del diseño")[0].performScrollTo().performClick()
        assertEquals("", applied.last().hex)
    }

    @Test
    fun sizeBoldAndAlignment() {
        showTitle()
        compose.onNodeWithText("Grande").performScrollTo().performClick()
        assertEquals(18.0, applied.last().size, 0.0)
        compose.onNodeWithContentDescription("Negrita").performScrollTo().performClick()
        assertEquals(true, applied.last().bold)
        compose.onNodeWithText("Más opciones").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Izquierda").performScrollTo().performClick()
        assertEquals(TextAlignment.LEFT, applied.last().alignment)
    }

    @Test
    fun noDateAndChooseDateDialog() {
        show(EditorUiState(loading = false, project = PolarProject().normalized(), textRole = TextRole.DATE))
        compose.onNodeWithText("Sin fecha").performClick()
        assertEquals(listOf("fecha:NONE"), log)
        compose.onNodeWithText("Elegir fecha").performClick()
        compose.onNodeWithText("Aceptar").assertExists()
    }

    @Test
    fun musicalDesignShowsSongFieldsAndQrLinkWithoutScrolling() {
        show(EditorUiState(loading = false, project = PolarProject().normalized().withStyle(TemplateStyle.SPOTIFY)))
        compose.onNodeWithText("Canción para todas").assertIsDisplayed()
        compose.onNodeWithText("Artista para todas").assertIsDisplayed()
        compose.onNodeWithText("Enlace para el QR (opcional)").assertIsDisplayed()
    }

    @Test
    fun songSectionDoesNotExistInOtherDesigns() {
        showTitle()
        compose.onNodeWithText("Enlace para el QR (opcional)").assertDoesNotExist()
        compose.onNodeWithText("Canción para todas").assertDoesNotExist()
    }

    @Test
    fun visibleAndSizeAreOutsideMoreOptionsAndAlignmentInside() {
        showTitle()
        compose.onNodeWithText("Mostrar este texto").performScrollTo().assertExists()
        compose.onNodeWithText("Tamaño exacto").performScrollTo().assertExists()
        compose.onNodeWithText("Mediana").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Izquierda").assertDoesNotExist()
        compose.onNodeWithText("Más opciones").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Izquierda").performScrollTo().assertExists()
        compose.onNodeWithText("Mover a los lados").performScrollTo().assertExists()
    }

    @Test
    fun typingSongTitleEditsTheSongRole() {
        show(EditorUiState(loading = false, project = PolarProject().normalized().withStyle(TemplateStyle.SPOTIFY)))
        compose.onNodeWithText("Canción para todas").performTextReplacement("Mi canción")
        assertEquals(listOf("cancion:song:Mi canción"), log)
    }
}
