package com.polar.app.core.edit

import com.polar.app.model.*
import com.polar.app.engine.PolarRenderer
import org.junit.Assert.*
import org.junit.Test

class PhotoStudioTest {
    private fun project(): PolarProject {
        val photos = List(10) { PhotoAsset(path = "original$it.jpg", pixelWidth = 4800, pixelHeight = 3200, maskPath = "mask$it.png") }
        return PolarProject(photos = photos, placements = photos.map { PhotoPlacement(it.id, photoLook = PhotoLook(preset = "warm")) }).normalized()
    }
    @Test fun pageCardDesignsKeepSlotsAndUseSameGeometry() {
        val original = project()
        val page = ProjectEdits.setPageDesign(original, 1, TemplateStyle.POSTCARD)
        val mixed = ProjectEdits.setCardDesign(page, 0, TemplateStyle.HEART)
        mixed.validated()
        assertEquals(original.placements, mixed.placements)
        assertEquals(TemplateStyle.POLAROID, mixed.settingsForCard(1).style)
        assertEquals(TemplateStyle.HEART, mixed.settingsForCard(0).style)
        assertEquals(TemplateStyle.POSTCARD, mixed.settingsForCard(9).style)
        assertNotEquals(PolarRenderer.photoRects(original, 0)[0], PolarRenderer.photoRects(mixed, 0)[0])
        assertEquals(mixed, PolarJson.decode(PolarJson.encode(mixed)))
        assertEquals(mixed, ProjectEdits.setPageDesign(mixed, 0, TemplateStyle.FILM_VERTICAL))
        val removed = ProjectEdits.removePage(mixed, 0)
        assertEquals(TemplateStyle.POSTCARD, removed.settingsForPage(0).style)
        assertEquals(original.placements[9], removed.placements[0])
    }
    @Test fun batchCopyAndRemovePreserveOriginalsCropAndCaption() {
        var original = project()
        original = ProjectEdits.setText(original, TextRole.TITLE, "Mi título", 2)
        original = ProjectEdits.editPlacement(original, 2) { it.copy(zoom = 2.0, quarterTurns = 1, background = PhotoBackground(colorHex = "000000")) }
        original = ProjectEdits.setCardDesign(original, 2, TemplateStyle.HEART)
        val copied = ProjectEdits.copySlotsToNewPage(original, setOf(2, 5))
        assertEquals(3, copied.pageCount)
        assertEquals(original.placements[2], copied.placements[18])
        assertEquals(original.placements[5], copied.placements[19])
        assertEquals(original.override(2)?.texts, copied.override(18)?.texts)
        assertEquals(original.settingsForCard(2), copied.settingsForCard(18))
        assertEquals(original.photos, copied.photos)
        val removed = ProjectEdits.clearSlots(copied, setOf(2, 5))
        assertNull(removed.placements[2]); assertNull(removed.placements[5])
        assertEquals(copied.photos, removed.photos)
        assertEquals(copied.placements[18], removed.placements[18])
    }
    @Test fun transparentBackgroundRoundTripAndInvalidInputs() {
        val original = project()
        val transparent = ProjectEdits.editPlacement(original, 0) { it.copy(background = PhotoBackground(colorHex = null)) }
        assertNull(PolarJson.decode(PolarJson.encode(transparent)).placements[0]!!.background!!.colorHex)
        val invalid = transparent.copy(placements = transparent.placements.toMutableList().also { it[0] = it[0]!!.copy(background = PhotoBackground(imageID = "missing")) })
        assertThrows(PolarException::class.java) { invalid.validated() }
        assertThrows(PolarException::class.java) { transparent.copy(pageDesigns = mapOf("00" to PageDesign(TemplateStyle.HEART))).validated() }
        assertThrows(PolarException::class.java) { transparent.copy(pageDesigns = mapOf("999" to PageDesign(TemplateStyle.HEART))).validated() }
    }
}
