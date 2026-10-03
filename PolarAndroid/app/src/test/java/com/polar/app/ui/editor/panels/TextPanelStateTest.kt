package com.polar.app.ui.editor.panels

import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.editor.TextScope
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class TextPanelStateTest {
    private val base = PolarProject(settings = PrintSettings(title = "Lu y Max")).normalized()

    @Test
    fun cardScopeShowsOwnTextAndGeneralForComparison() {
        val p = ProjectEdits.setText(base, TextRole.TITLE, "El brindis", card = 1)
        val s = textPanelState(EditorUiState(loading = false, project = p, selectedSlot = 1, textScope = TextScope.CARD))
        assertTrue(s.cardScope)
        assertEquals(2, s.cardNumber)
        assertEquals("El brindis", s.text)
        assertEquals("Lu y Max", s.generalText)
        assertTrue(s.hasOwnText)
    }

    @Test
    fun allScopeCountsOwnTexts() {
        val p = ProjectEdits.setText(ProjectEdits.setText(base, TextRole.TITLE, "a", 0), TextRole.TITLE, "b", 4)
        val s = textPanelState(EditorUiState(loading = false, project = p))
        assertFalse(s.cardScope)
        assertEquals("Lu y Max", s.text)
        assertEquals(2, s.ownCount)
    }

    @Test
    fun rolesFollowDesignAndDateSamplesAreSpanish() {
        val s = textPanelState(EditorUiState(loading = false, project = base), TimeZone.getTimeZone("UTC"))
        assertEquals(listOf(TextRole.TITLE, TextRole.SUBTITLE, TextRole.DATE), s.roles)
        assertEquals(listOf("14 feb 2026", "14.02.26", "febrero 2026"), s.dateSamples)
        val film = textPanelState(EditorUiState(loading = false, project = base.withStyle(TemplateStyle.FILM_VERTICAL)))
        assertTrue(film.roles.isEmpty())
        assertNotNull(film.emptyReason)
    }
}
