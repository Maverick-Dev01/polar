package com.polar.app.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfOpaqueLayerTest {
    private fun photograph(): Bitmap {
        val bitmap = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
        val random = Random(7)
        val pixels = IntArray(bitmap.width * bitmap.height) { i ->
            val noise = random.nextInt(31) - 15
            Color.rgb((40 + i % 900 / 6 + noise).coerceIn(0, 255),
                (60 + i / 900 / 8 + noise).coerceIn(0, 255), (130 + noise).coerceIn(0, 255))
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun project(zoom: Double, rounded: Boolean, style: TemplateStyle = TemplateStyle.POLAROID): PolarProject {
        val asset = PhotoAsset(path = "fixture", pixelWidth = 900, pixelHeight = 1200)
        return PolarProject(
            settings = PrintSettings(style = style, columns = 3, rows = 3, roundedPhotos = rounded),
            photos = listOf(asset),
            placements = List(9) { PhotoPlacement(assetID = asset.id, zoom = zoom) }
        )
    }

    @Test fun fittedAndRoundedPhotosAreAllRegisteredForJpeg() {
        val bitmap = photograph()
        for ((zoom, rounded) in listOf(0.7 to false, 1.0 to true, 0.7 to true)) {
            val registered = mutableListOf<PdfPhotoFingerprint?>()
            val canvas = Canvas(Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888))
            PolarRenderer.drawPage(canvas, project(zoom, rounded), 0, false, 1f, { bitmap }, null,
                pdfPhoto = { registered.add(PdfPhotoFingerprint.fromBitmap(it)) })
            assertEquals(9, registered.size)
            assertTrue("zoom=$zoom rounded=$rounded", registered.all { it != null })
        }
    }

    @Test fun layerOverCardColorMatchesOnScreenRender() {
        // La capa opaca compuesta sobre el color de la tarjeta debe verse igual que dibujar directo.
        val bitmap = photograph()
        val p = project(0.7, false)
        val pdfCanvasBitmap = Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888)
        PolarRenderer.drawPage(Canvas(pdfCanvasBitmap).apply { drawColor(Color.WHITE) }, p, 0, false, 1f, { bitmap }, null, pdfPhoto = { })
        val direct = Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888)
        PolarRenderer.drawPage(Canvas(direct).apply { drawColor(Color.WHITE) }, p, 0, false, 1f, { bitmap }, null)
        var sum = 0L
        val a = IntArray(612 * 792); val b = IntArray(612 * 792)
        pdfCanvasBitmap.getPixels(a, 0, 612, 0, 0, 612, 792); direct.getPixels(b, 0, 612, 0, 0, 612, 792)
        for (i in a.indices) sum += Math.abs(Color.red(a[i]) - Color.red(b[i])) + Math.abs(Color.green(a[i]) - Color.green(b[i])) + Math.abs(Color.blue(a[i]) - Color.blue(b[i]))
        assertTrue("diferencia media ${sum / (a.size * 3.0)}", sum / (a.size * 3.0) <= 3.0)
    }
}
