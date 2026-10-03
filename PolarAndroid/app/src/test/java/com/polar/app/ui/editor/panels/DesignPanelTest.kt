package com.polar.app.ui.editor.panels

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.core.edit.MoodPreset
import com.polar.app.data.ThemeMode
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class DesignPanelTest {
    @get:Rule val compose = createComposeRule()

    private fun callbacks(log: MutableList<String>) = DesignCallbacks(
        onChangeDesign = { log += "cambiar" }, onAccent = { log += "color:$it" }, onLayout = { log += "layout:$it" },
        onMood = { log += "mood:${it.name}" }, onSuggested = { log += "frases" }, onFormat = {}, onGrid = { _, _ -> },
        onGap = {}, onRounded = {}, onYear = {}, onHighlight = {}, onSpecialDate = {}, onEditRegions = { log += "huecos:$it" },
        onRegion = {}, onAddRegion = {}, onRemoveRegion = {}, onGestureStart = {}, onGestureEnd = {}
    )

    @Test
    fun layoutMoodAndChangeDesign() {
        val log = mutableListOf<String>()
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { DesignPanel(EditorUiState(loading = false), callbacks(log)) } }
        compose.onNodeWithText("Cambiar diseño").performClick()
        compose.onNodeWithText("4").performScrollTo().performClick()
        compose.onNodeWithText("Familia").performScrollTo().performClick()
        compose.onNodeWithText("Usar frases sugeridas").performScrollTo().performClick()
        assertEquals(listOf("cambiar", "layout:4", "mood:FAMILY", "frases"), log)
    }

    @Test
    fun importedTemplateShowsHoleEditing() {
        val log = mutableListOf<String>()
        val tpl = ImportedTemplate("t.png", 100, 100, listOf(TemplateRegion(x = 0.1, y = 0.1, width = 0.3, height = 0.3)))
        val state = EditorUiState(loading = false, project = PolarProject(settings = PrintSettings(style = TemplateStyle.IMPORTED, columns = 1, rows = 1, importedTemplate = tpl)).normalized())
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { DesignPanel(state, callbacks(log)) } }
        compose.onNodeWithText("4").assertDoesNotExist()
        compose.onNodeWithText("Editar los huecos").performScrollTo().performClick()
        assertEquals(listOf("huecos:true"), log)
    }
}
