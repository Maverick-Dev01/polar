package com.polar.app.export

import com.polar.app.engine.PhotoFit
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.max

class AndroidExportServiceTest {
    private val photo = PhotoAsset(path = "original.jpg", pixelWidth = 6000, pixelHeight = 8000)
    private val settings = PrintSettings(style = TemplateStyle.BORDERLESS, columns = 1, rows = 1,
        paperSize = PaperSize.A3, cardFormat = CardFormat.FILL)

    @Test
    fun a3AndZoomKeepAllUsefulOriginalPixelsInsteadOfCappingAt3600() {
        for (turn in 0..3) for (zoom in listOf(1.0, 2.0, 4.0)) {
            val placement = PhotoPlacement(photo.id, zoom = zoom, quarterTurns = turn)
            val project = PolarProject(settings = settings, photos = listOf(photo), placements = listOf(placement))
            val card = PolarRenderer.calculateCardRects(settings).single()
            val fit = PhotoFit.compute(card.width, card.height, photo.pixelWidth, photo.pixelHeight, placement)
            val expected = ceil(max(fit.width, fit.height) * 300 / 72).toInt().coerceAtMost(8000)
            assertTrue(expected > 3600)
            assertEquals("rotation=$turn zoom=$zoom", expected, requiredPhotoPixels(project)[photo.id])
        }
    }

    @Test
    fun targetUsesLargestPlacementAndPageExportOnlyUsesItsPage() {
        val project = PolarProject(settings = settings, photos = listOf(photo), placements = listOf(
            PhotoPlacement(photo.id), PhotoPlacement(photo.id, zoom = 4.0)))
        assertEquals(8000, requiredPhotoPixels(project)[photo.id])
        assertTrue(requiredPhotoPixels(project, 0).getValue(photo.id) < 8000)
        assertEquals(8000, requiredPhotoPixels(project, 1)[photo.id])
    }

    @Test
    fun pageAndCardDesignsAndReplacementBackgroundUseTheirRenderedGeometry() {
        val background = PhotoAsset(path = "fondo.jpg", pixelWidth = 6000, pixelHeight = 4000)
        val placement = PhotoPlacement(photo.id, background = PhotoBackground(imageID = background.id))
        val project = PolarProject(settings = settings, photos = listOf(photo, background),
            placements = listOf(placement, placement),
            pageDesigns = mapOf("1" to PageDesign(TemplateStyle.SQUARE)),
            cardOverrides = mapOf("0" to CardOverride(designStyle = TemplateStyle.POSTCARD)))
        for (page in 0..1) {
            val rect = PolarRenderer.photoRects(project, page).single()
            val targets = requiredPhotoPixels(project, page)
            for (asset in project.photos) {
                val fit = PhotoFit.compute(rect.width, rect.height, asset.pixelWidth, asset.pixelHeight, PhotoPlacement(asset.id))
                val expected = ceil(max(fit.width, fit.height) * 300 / 72).toInt()
                    .coerceAtMost(max(asset.pixelWidth, asset.pixelHeight))
                assertEquals(expected, targets[asset.id])
            }
        }
        assertNotEquals(requiredPhotoPixels(project, 0)[photo.id], requiredPhotoPixels(project, 1)[photo.id])
    }
}
