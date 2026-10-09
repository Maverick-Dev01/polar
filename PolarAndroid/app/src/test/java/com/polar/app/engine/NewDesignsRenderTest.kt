package com.polar.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NewDesignsRenderTest {
    private val newStyles = listOf(TemplateStyle.PHOTOBOOTH, TemplateStyle.INSTAX_WIDE, TemplateStyle.VINYL, TemplateStyle.CASSETTE, TemplateStyle.COLLAGE, TemplateStyle.WASHI)
    private val photo = PhotoAsset(id = "12345678-1234-1234-1234-123456789ABC", path = "red", pixelWidth = 40, pixelHeight = 40)
    private val red = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(220, 20, 20)) }

    private fun project(style: TemplateStyle, paper: PaperSize, orientation: PaperOrientation, url: String = "https://example.com/a"): PolarProject {
        val base = PolarProject(settings = PrintSettings(paperSize = paper, orientation = orientation, songURL = url, cutGuides = false), photos = listOf(photo))
        val p = ProjectEdits.selectStyle(base, style)
        val filled = p.copy(placements = List(p.placements.size) { PhotoPlacement(photo.id) })
        return ProjectEdits.setDateSource(filled, DateSource.CHOSEN).let { ProjectEdits.setChosenDate(it, 1_771_070_400_000) }
    }

    @Test fun everyNewStyleRendersOnEveryPaperAndOrientation() {
        for (style in newStyles) for (paper in listOf(PaperSize.LETTER, PaperSize.A4, PaperSize.PHOTO4X6)) for (o in PaperOrientation.entries) {
            val p = project(style, paper, o)
            p.validated()
            val size = p.settings.paperSizePoints
            for (preview in listOf(true, false)) {
                val bitmap = Bitmap.createBitmap(size.width.toInt() / 2, size.height.toInt() / 2, Bitmap.Config.ARGB_8888)
                PolarRenderer.drawPage(Canvas(bitmap), p, 0, preview, 0.5f, { red })
                assertTrue("$style $paper $o", bitmap.getPixel(bitmap.width / 2, bitmap.height / 2) != 0)
                bitmap.recycle()
            }
        }
    }

    @Test fun photoboothHasFourPhotosPerCardAndCollageThree() {
        assertEquals(4, ProjectEdits.selectStyle(PolarProject(), TemplateStyle.PHOTOBOOTH).settings.style.photosPerCard)
        val c = ProjectEdits.selectStyle(PolarProject(), TemplateStyle.COLLAGE)
        assertEquals(3, c.settings.style.photosPerCard)
        assertEquals(c.settings.columns * c.settings.rows * 3, c.settings.capacity)
    }

    /** Dibuja una sola tarjeta de 180 pt de ancho con la foto roja a escala 4. */
    private fun renderCard(style: TemplateStyle, url: String = ""): Pair<Bitmap, PolarRect> {
        val p = project(style, PaperSize.LETTER, PaperOrientation.PORTRAIT, url)
        val card = PolarRect(0.0, 0.0, 180.0, 180.0 / style.defaultAspect)
        val bitmap = Bitmap.createBitmap(720, (card.height * 4).toInt(), Bitmap.Config.ARGB_8888)
        val single = p.copy(settings = p.settings.copy(cutGuides = false))
        val canvas = Canvas(bitmap)
        PolarRenderer.drawCard(canvas, card, single, 0, 0, false, 4f, { red }, SystemFontProvider)
        return bitmap to card
    }

    private fun isRed(c: Int) = Color.red(c) > 180 && Color.green(c) < 80 && Color.blue(c) < 80

    @Test fun vinylClipsThePhotoToACircle() {
        val (bitmap, card) = renderCard(TemplateStyle.VINYL)
        val slot = SharedGeometry.of(TemplateStyle.VINYL)!!.photoSlots.single()
        val left = (slot.x * card.width * 4).toInt(); val top = (slot.y * card.height * 4).toInt()
        val w = (slot.w * card.width * 4).toInt(); val h = (slot.h * card.height * 4).toInt()
        assertTrue("centro de la etiqueta", isRed(bitmap.getPixel(left + w / 2, top + h / 4)))
        assertFalse("esquina del cuadro", isRed(bitmap.getPixel(left + 3, top + 3)))
        assertTrue("borde del círculo", isRed(bitmap.getPixel(left + w / 2, top + 6)))
    }

    @Test fun cassetteRoundsThePhotoCorners() {
        val (bitmap, card) = renderCard(TemplateStyle.CASSETTE)
        val slot = SharedGeometry.of(TemplateStyle.CASSETTE)!!.photoSlots.single()
        val left = (slot.x * card.width * 4).toInt(); val top = (slot.y * card.height * 4).toInt()
        assertFalse(isRed(bitmap.getPixel(left + 2, top + 2)))
        assertTrue(isRed(bitmap.getPixel(left + (slot.w * card.width * 2).toInt(), top + (slot.h * card.height * 2).toInt())))
    }

    @Test fun musicalDesignsDrawTheQrOnlyWhenThereIsALink() {
        for (style in listOf(TemplateStyle.VINYL, TemplateStyle.CASSETTE, TemplateStyle.PLAYER_RED, TemplateStyle.PLAYER_GRAY)) {
            val slot = QrSlots.of(style)!!
            val (without, card) = renderCard(style)
            val (with, _) = renderCard(style, "https://example.com/cancion")
            val rect = GeometryDrawing.qrRect(card, slot)
            val x = (rect.left * 4).toInt(); val y = (rect.top * 4).toInt(); val side = (rect.width * 4).toInt()
            fun blackPixels(b: Bitmap): Int { var n = 0; for (i in 0 until side step 2) for (j in 0 until side step 2) { val c = b.getPixel(x + i, y + j); if (Color.red(c) + Color.green(c) + Color.blue(c) < 120) n++ }; return n }
            val total = (side / 2) * (side / 2)
            assertTrue("$style con enlace", blackPixels(with) > total / 5)
            assertTrue("$style sin enlace", blackPixels(without) < total / 20)
            // La zona de silencio es blanca aun sobre fondo de color.
            val pad = (rect.width * QrSlots.QUIET_ZONE * 4 / 2).toInt()
            assertEquals("$style silencio", Color.WHITE, with.getPixel(x - 1 - pad, y + 20))
        }
    }

    @Test fun styleCardThumbnailsRenderForTheNewDesigns() {
        val thumbs = Thumbnailer(SystemFontProvider)
        for (style in newStyles) {
            val b = thumbs.styleCard(style, 240)
            assertEquals(240, b.width)
            assertEquals(Math.round(240 / style.defaultAspect).toDouble(), b.height.toDouble(), 2.0)
        }
    }

    @Test fun defaultFontOfTheDesignAppliesUntilTheCardPicksAnotherOne() {
        val p = project(TemplateStyle.WASHI, PaperSize.LETTER, PaperOrientation.PORTRAIT)
        val card = PolarRect(0.0, 0.0, 180.0, 225.0)
        val items = CardTextLayout.items(p, card, 0, PolarRenderer.calculatePhotoRects(card, TemplateStyle.WASHI, p.settings), 0)
        assertEquals("Caveat", items.single { it.role == TextRole.CAPTION }.defaultFont)
    }
}
