package com.polar.app.engine

import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class CardTextLayoutTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val card = PolarRect(0.0, 0.0, 100.0, 140.0)
    private val accent = 0xFF92394A.toInt()
    private val photo = PhotoAsset(path = "a.jpg", pixelWidth = 10, pixelHeight = 10, takenAtEpochMs = 1_771_070_400_000)

    private fun project(style: TemplateStyle = TemplateStyle.POLAROID) =
        PolarProject(settings = PrintSettings(style = style, title = "Lu y Max"), photos = listOf(photo),
            placements = List(9) { PhotoPlacement(photo.id) }).normalized()

    private fun items(p: PolarProject, index: Int) =
        CardTextLayout.items(p, card, index, PolarRenderer.calculatePhotoRects(card, p.settings.style, p.settings), accent, utc)

    @Test
    fun polaroidShowsTitleAndSubtitleWithoutDateByDefault() {
        val roles = items(project(), 0).map { it.role }
        assertEquals(listOf(TextRole.TITLE, TextRole.SUBTITLE), roles)
    }

    @Test
    fun dateAppearsWhenEnabled() {
        val p = ProjectEdits.setDateSource(project(), DateSource.PHOTO)
        val date = items(p, 0).single { it.role == TextRole.DATE }
        assertEquals("14 feb 2026", date.text)
    }

    @Test
    fun ownTextAppliesOnlyToItsCard() {
        val p = ProjectEdits.setText(project(), TextRole.TITLE, "El brindis", card = 1)
        assertEquals("Lu y Max", items(p, 0).first { it.role == TextRole.TITLE }.text)
        assertEquals("El brindis", items(p, 1).first { it.role == TextRole.TITLE }.text)
    }

    @Test
    fun emptyOwnTextHidesOnlyThatCard() {
        val p = ProjectEdits.setText(project(), TextRole.TITLE, "", card = 1)
        assertEquals("", items(p, 1).first { it.role == TextRole.TITLE }.text)
        assertEquals("Lu y Max", items(p, 2).first { it.role == TextRole.TITLE }.text)
    }

    @Test
    fun ownAppearanceTravelsWithItem() {
        val p = ProjectEdits.editAppearance(project(), TextRole.TITLE, 1) { it.copy(hex = "C34048", fontName = "Caveat") }
        assertEquals("C34048", items(p, 1).first { it.role == TextRole.TITLE }.appearance!!.hex)
        assertEquals("", items(p, 0).first { it.role == TextRole.TITLE }.appearance!!.hex)
    }

    @Test
    fun filmAndCalendarHaveNoUserText() {
        val film = ProjectEdits.setDateSource(project(TemplateStyle.FILM_VERTICAL), DateSource.PHOTO)
        assertTrue(items(film, 0).none { it.role != null })
    }

    @Test
    fun ticketNumberUsesCardSlot() {
        val fixed = items(project(TemplateStyle.TICKET), 3).filter { it.role == null }.map { it.text }
        assertTrue(fixed.contains("Nº 004"))
    }

    @Test
    fun instagramUsesTitleAndCaptionRoles() {
        val roles = items(project(TemplateStyle.INSTAGRAM), 0).mapNotNull { it.role }
        assertEquals(listOf(TextRole.TITLE, TextRole.CAPTION), roles)
    }

    @Test
    fun cardDesignOverrideDrivesItsOwnTextsOnOldDesigns() {
        val base = project(TemplateStyle.POLAROID)
        for ((style, roles) in listOf(
            TemplateStyle.SPOTIFY to listOf(TextRole.SONG, TextRole.ARTIST),
            TemplateStyle.TICKET to listOf(TextRole.TITLE, TextRole.SUBTITLE),
            TemplateStyle.POLAROID to listOf(TextRole.TITLE, TextRole.SUBTITLE)
        )) {
            val p = ProjectEdits.setCardDesign(base, 1, style)
            assertEquals(style, p.settingsForCard(1).style)
            assertEquals(1, p.firstSlotOfCard(1))
            assertEquals(roles, items(p, 1).mapNotNull { it.role })
            assertEquals(listOf(TextRole.TITLE, TextRole.SUBTITLE), items(p, 0).map { it.role }) // la tarjeta 0 sigue siendo Polaroid
        }
        val ticket = items(ProjectEdits.setCardDesign(base, 1, TemplateStyle.TICKET), 1)
        assertTrue(ticket.any { it.role == null && it.text == "POLAR" })
        assertEquals("Nº 002", ticket.first { it.text.startsWith("Nº") }.text)
    }
}
