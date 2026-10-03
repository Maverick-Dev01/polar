package com.polar.app.core.edit

import com.polar.app.core.text.TextResolver
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test

class ProjectEditsTest {
    private fun photos(n: Int) = List(n) { PhotoAsset(path = "p$it.jpg", pixelWidth = 100, pixelHeight = 100) }
    private fun filled(n: Int): PolarProject {
        val ps = photos(n)
        return PolarProject(photos = ps, placements = ps.map { PhotoPlacement(it.id) }).normalized()
    }

    @Test
    fun setTextGeneralAndOwn() {
        var p = filled(3)
        p = ProjectEdits.setText(p, TextRole.TITLE, "Lu y Max")
        p = ProjectEdits.setText(p, TextRole.TITLE, "El brindis", card = 1)
        assertEquals("Lu y Max", TextResolver.text(p, 0, TextRole.TITLE))
        assertEquals("El brindis", TextResolver.text(p, 1, TextRole.TITLE))
        p = ProjectEdits.clearOwnText(p, 1, TextRole.TITLE)
        assertEquals("Lu y Max", TextResolver.text(p, 1, TextRole.TITLE))
        assertTrue("un override vacío se elimina", p.cardOverrides.isEmpty())
    }

    @Test
    fun applyTextToAllRemovesOnlyThatRole() {
        var p = filled(3)
        p = ProjectEdits.setText(p, TextRole.TITLE, "A", card = 0)
        p = ProjectEdits.setText(p, TextRole.SUBTITLE, "B", card = 0)
        p = ProjectEdits.setText(p, TextRole.TITLE, "C", card = 2)
        p = ProjectEdits.applyTextToAll(p, TextRole.TITLE)
        assertEquals(0, TextResolver.ownTextCount(p, TextRole.TITLE))
        assertEquals("B", TextResolver.text(p, 0, TextRole.SUBTITLE))
    }

    @Test
    fun ownAppearanceStartsFromGeneral() {
        var p = filled(2)
        p = ProjectEdits.editAppearance(p, TextRole.TITLE, null) { it.copy(fontName = "Caveat", bold = true) }
        p = ProjectEdits.editAppearance(p, TextRole.TITLE, 1) { it.copy(hex = "C34048") }
        val own = TextResolver.appearance(p, 1, TextRole.TITLE)
        assertEquals("Caveat", own.fontName)
        assertTrue(own.bold)
        assertEquals("C34048", own.hex)
        assertEquals("", TextResolver.appearance(p, 0, TextRole.TITLE).hex)
    }

    @Test
    fun dateSourceGeneralAndPerCard() {
        var p = filled(2)
        p = ProjectEdits.setDateSource(p, DateSource.PHOTO)
        p = ProjectEdits.setChosenDate(p, 1_791_806_400_000, card = 1)
        assertEquals(DateSource.PHOTO, TextResolver.dateSource(p, 0))
        assertEquals(DateSource.CHOSEN, TextResolver.dateSource(p, 1))
    }

    @Test
    fun moodChangesFontAndColorButKeepsPhrases() {
        var p = ProjectEdits.setText(filled(1), TextRole.TITLE, "Mi frase")
        p = ProjectEdits.applyMood(p, MoodPreset.CELEBRATION)
        assertEquals("C34048", p.settings.accentHex)
        assertEquals("Dancing Script", p.settings.textStyle(TextRole.TITLE).fontName)
        assertEquals("Dancing Script", p.settings.textStyle(TextRole.DATE).fontName)
        assertEquals("Mi frase", p.settings.title)
        assertFalse(p.settings.textStyles.containsKey("date"))
    }

    @Test
    fun suggestedPhrasesReplaceGeneralAndOwnTexts() {
        var p = ProjectEdits.setText(filled(2), TextRole.TITLE, "Propio", card = 1)
        p = ProjectEdits.applySuggestedPhrases(p, MoodPreset.FRIENDS)
        assertEquals("Siempre juntos", TextResolver.text(p, 1, TextRole.TITLE))
        assertEquals("Amigos de verdad", p.settings.subtitle)
    }

    @Test
    fun selectStyleRemapsOverridesWhenPhotosPerCardChanges() {
        var p = filled(12)
        p = ProjectEdits.setText(p, TextRole.TITLE, "cero", card = 0)
        p = ProjectEdits.setText(p, TextRole.TITLE, "uno", card = 1)  // fotos 1 → tarjeta 0 de película: choca, gana "cero"
        p = ProjectEdits.setText(p, TextRole.TITLE, "seis", card = 6) // foto 6 → tarjeta 1 de película
        val film = ProjectEdits.selectStyle(p, TemplateStyle.FILM_VERTICAL)
        assertEquals("cero", film.cardOverrides["0"]!!.texts["title"])
        assertEquals("seis", film.cardOverrides["1"]!!.texts["title"])
        assertEquals(2, film.cardOverrides.size)
        val sameShape = ProjectEdits.selectStyle(p, TemplateStyle.MINI)
        assertEquals(p.cardOverrides, sameShape.cardOverrides)
    }

    @Test
    fun setGridKeepsPhotoOrderAndOverrides() {
        val p0 = ProjectEdits.setText(filled(5), TextRole.TITLE, "x", card = 3)
        val p = ProjectEdits.setGrid(p0, 2, 2)
        assertEquals(4, p.settings.capacity)
        assertEquals(p0.placements.take(5), p.placements.take(5))
        assertEquals(8, p.placements.size)
        assertEquals("x", TextResolver.text(p, 3, TextRole.TITLE))
    }

    @Test
    fun layoutPresetsFollowOrientation() {
        val portrait = ProjectEdits.applyLayoutPreset(filled(1), 2)
        assertEquals(1 to 2, portrait.settings.columns to portrait.settings.rows)
        val landscape = ProjectEdits.applyLayoutPreset(
            ProjectEdits.updateSettings(filled(1)) { it.copy(orientation = PaperOrientation.LANDSCAPE) }, 8
        )
        assertEquals(4 to 2, landscape.settings.columns to landscape.settings.rows)
        val one = filled(1)
        assertEquals("un número no soportado no cambia nada", one, ProjectEdits.applyLayoutPreset(one, 5))
    }

    @Test
    fun addPhotosDedupesAndFillsEmptySlots() {
        val p0 = PolarProject().normalized()
        val ps = photos(3)
        val p1 = ProjectEdits.addPhotos(p0, ps, fillFrom = 0)
        assertEquals(3, p1.placedCount)
        val p2 = ProjectEdits.addPhotos(p1, ps + photos(1).map { it.copy(path = "nueva.jpg") }, fillFrom = 0)
        assertEquals(4, p2.photos.size)
        assertEquals(p2.photos.last().id, p2.placements[3]!!.assetID)
        val noFill = ProjectEdits.addPhotos(p0, ps, fillFrom = null)
        assertEquals(0, noFill.placedCount)
    }

    @Test
    fun fillAllUsesEveryPhotoAcrossPages() {
        val ps = photos(20)
        val p = ProjectEdits.fillAll(PolarProject(photos = ps))
        assertEquals(20, p.placedCount)
        assertEquals(3, p.pageCount)
    }

    @Test
    fun editPlacementClampsValues() {
        val p = ProjectEdits.editPlacement(filled(1), 0) { it.copy(zoom = 9.0, offsetX = -3.0, quarterTurns = 5) }
        val pl = p.placements[0]!!
        assertEquals(4.0, pl.zoom, 0.0)
        assertEquals(-1.0, pl.offsetX, 0.0)
        assertEquals(1, pl.quarterTurns)
        val untouched = filled(1)
        assertEquals("un espacio vacío no cambia", untouched, ProjectEdits.editPlacement(untouched, 5) { it })
    }

    @Test
    fun removePageShiftsLaterOverrides() {
        var p = filled(27) // 3 hojas de 9
        p = ProjectEdits.setText(p, TextRole.TITLE, "hoja1", card = 3)
        p = ProjectEdits.setText(p, TextRole.TITLE, "hoja2", card = 10)
        p = ProjectEdits.setText(p, TextRole.TITLE, "hoja3", card = 20)
        p = ProjectEdits.removePage(p, 1)
        assertEquals(2, p.pageCount)
        assertEquals("hoja1", p.cardOverrides["3"]!!.texts["title"])
        assertNull(p.cardOverrides["10"])
        assertEquals("hoja3", p.cardOverrides["11"]!!.texts["title"])
    }

    @Test
    fun removeOnlyPageClearsIt() {
        val p = ProjectEdits.removePage(filled(3), 0)
        assertEquals(0, p.placedCount)
        assertEquals(1, p.pageCount)
    }

    @Test
    fun customPaperIsClamped() {
        val p = ProjectEdits.setCustomPaper(filled(1), 10.0, 9000.0)
        assertEquals(80.0, p.settings.customWidthMM, 0.0)
        assertEquals(600.0, p.settings.customHeightMM, 0.0)
    }

    @Test
    fun selectStyleMergesDifferentRolesOnCollision() {
        var p = filled(12)
        p = ProjectEdits.setText(p, TextRole.TITLE, "cero", card = 0)
        p = ProjectEdits.setText(p, TextRole.SUBTITLE, "uno", card = 1)
        p = ProjectEdits.editAppearance(p, TextRole.TITLE, 1) { it.copy(hex = "C34048") }
        val film = ProjectEdits.selectStyle(p, TemplateStyle.FILM_VERTICAL)
        val o = film.cardOverrides["0"]!!
        assertEquals("cero", o.texts["title"])
        assertEquals("uno", o.texts["subtitle"])
        assertEquals("C34048", o.styles["title"]!!.hex)
        assertEquals(1, film.cardOverrides.size)
    }

    @Test
    fun pageEditsIgnoreOutOfRangePage() {
        val p = filled(3)
        assertEquals(p, ProjectEdits.clearPage(p, -1))
        assertEquals(p, ProjectEdits.clearPage(p, 5))
        assertEquals(p, ProjectEdits.removePage(p, 5))
    }
}
