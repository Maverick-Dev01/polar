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

    @Test
    fun musicalDesignPutsSongSectionFirstWithLinkOnlyWhereThereIsAQr() {
        val spotify = textPanelState(EditorUiState(loading = false, project = base.withStyle(TemplateStyle.SPOTIFY)))
        assertEquals(TextPanelSection.SONG, spotify.sections.first())
        assertTrue(spotify.isMusical)
        assertTrue(spotify.showSongUrl)
        val player = textPanelState(EditorUiState(loading = false, project = base.withStyle(TemplateStyle.PLAYER_RED)))
        assertEquals(TextPanelSection.SONG, player.sections.first())
        assertFalse(player.showSongUrl)
    }

    @Test
    fun songSectionAppearsOnlyInMusicalDesigns() {
        val polaroid = textPanelState(EditorUiState(loading = false, project = base))
        assertFalse(polaroid.isMusical)
        assertFalse(TextPanelSection.SONG in polaroid.sections)
    }

    @Test
    fun visibleAndSizeComeBeforeMoreOptionsAndAlignmentGoesInside() {
        val s = textPanelState(EditorUiState(loading = false, project = base))
        val order = s.sections
        assertEquals(
            listOf(TextPanelSection.SCOPE, TextPanelSection.ROLES, TextPanelSection.TEXT, TextPanelSection.VISIBLE, TextPanelSection.FONT,
                TextPanelSection.COLOR, TextPanelSection.SIZE, TextPanelSection.MORE),
            order
        )
        assertTrue(order.indexOf(TextPanelSection.VISIBLE) < order.indexOf(TextPanelSection.MORE))
        assertTrue(order.indexOf(TextPanelSection.SIZE) < order.indexOf(TextPanelSection.MORE))
    }

    @Test
    fun musicalDesignSkipsTheGenericTextEditorBecauseSongFieldsReplaceIt() {
        val s = textPanelState(EditorUiState(loading = false, project = base.withStyle(TemplateStyle.SPOTIFY)))
        assertEquals(
            listOf(TextPanelSection.SONG, TextPanelSection.SCOPE, TextPanelSection.ROLES, TextPanelSection.VISIBLE, TextPanelSection.FONT,
                TextPanelSection.COLOR, TextPanelSection.SIZE, TextPanelSection.MORE),
            s.sections
        )
    }

    @Test
    fun dateRoleUsesTheDateSectionInsteadOfTheTextEditor() {
        val s = textPanelState(EditorUiState(loading = false, project = base, textRole = TextRole.DATE))
        assertTrue(TextPanelSection.DATE in s.sections)
        assertFalse(TextPanelSection.TEXT in s.sections)
    }

    @Test
    fun songAndArtistTextFollowTheScope() {
        val spotify = base.withStyle(TemplateStyle.SPOTIFY)
        val p = ProjectEdits.setText(ProjectEdits.setText(spotify, TextRole.SONG, "Mi canción", null), TextRole.ARTIST, "Ella", null)
        val all = textPanelState(EditorUiState(loading = false, project = p))
        assertEquals("Mi canción", all.songText)
        assertEquals("Ella", all.artistText)
        val own = ProjectEdits.setText(p, TextRole.SONG, "Otra", card = 1)
        val card = textPanelState(EditorUiState(loading = false, project = own, selectedSlot = 1, textScope = TextScope.CARD))
        assertEquals("Otra", card.songText)
        assertEquals("Ella", card.artistText)
    }

    @Test
    fun emptyDesignHasNoSections() {
        val film = textPanelState(EditorUiState(loading = false, project = base.withStyle(TemplateStyle.FILM_VERTICAL)))
        assertTrue(film.sections.isEmpty())
    }
}
