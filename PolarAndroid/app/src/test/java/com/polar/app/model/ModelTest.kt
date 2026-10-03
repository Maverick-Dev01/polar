package com.polar.app.model

import org.junit.Assert.*
import org.junit.Test

class ModelTest {
    @Test
    fun defaultProject() {
        val p = PolarProject().normalized()
        assertEquals(9, p.settings.capacity)
        assertEquals(1, p.pageCount)
        assertEquals(9, p.placements.size)
        assertEquals(9, p.cardsPerPage)
    }

    @Test
    fun pageCountFromPlacements() {
        val p = PolarProject(placements = List(20) { null })
        assertEquals(3, p.pageCount)
        assertEquals(27, p.normalized().placements.size)
    }

    @Test
    fun filmHasFivePhotosPerCard() {
        val s = PrintSettings(style = TemplateStyle.FILM_VERTICAL, columns = 2, rows = 1)
        val p = PolarProject(settings = s)
        assertEquals(10, s.capacity)
        assertEquals(2, p.cardsPerPage)
        assertEquals(1, p.cardOfSlot(7))
        assertEquals(5, p.firstSlotOfCard(1))
    }

    @Test
    fun paperSizesInPoints() {
        assertEquals(612.0, PrintSettings(paperSize = PaperSize.LETTER).paperSizePoints.width, 0.01)
        assertEquals(792.0, PrintSettings(paperSize = PaperSize.LETTER).paperSizePoints.height, 0.01)
        val landscape = PrintSettings(paperSize = PaperSize.LETTER, orientation = PaperOrientation.LANDSCAPE).paperSizePoints
        assertEquals(792.0, landscape.width, 0.01)
        assertEquals(595.28, PrintSettings(paperSize = PaperSize.A4).paperSizePoints.width, 0.01)
    }

    @Test
    fun dateRoleGoesToExtraStyles() {
        val s = PrintSettings().withTextStyle(TextRole.DATE, TextAppearance(hex = "112233"))
        assertFalse(s.textStyles.containsKey("date"))
        assertEquals("112233", s.extraTextStyles["date"]!!.hex)
        assertEquals("112233", s.textStyle(TextRole.DATE).hex)
    }

    @Test
    fun withStyleSetsDefaultGridAndTrimsTrailingEmpty() {
        val photo = PhotoAsset(path = "a", pixelWidth = 1, pixelHeight = 1)
        val p = PolarProject(photos = listOf(photo), placements = listOf(PhotoPlacement(photo.id), null, null))
        val film = p.withStyle(TemplateStyle.FILM_VERTICAL)
        assertEquals(2, film.settings.columns)
        assertEquals(1, film.settings.rows)
        assertEquals(10, film.placements.size)
    }

    @Test
    fun swiftDateRoundTrip() {
        val ms = 1_771_070_400_000L
        assertEquals(ms, SwiftDate.toEpochMs(SwiftDate.fromEpochMs(ms)))
        assertEquals(0.0, SwiftDate.fromEpochMs(978_307_200_000L), 0.0)
    }

    @Test
    fun validationRejectsBadColorAndUnknownOverrideRole() {
        assertThrows(PolarException::class.java) { PolarProject(settings = PrintSettings(accentHex = "zzz")).validated() }
        val bad = PolarProject(cardOverrides = mapOf("0" to CardOverride(texts = mapOf("nope" to "x"))))
        assertThrows(PolarException::class.java) { bad.validated() }
    }

    @Test
    fun newIdsAreUppercaseUuids() {
        assertTrue(Regex("^[0-9A-F-]{36}$").matches(newId()))
    }
}
