package com.polar.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
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
class QrGeneratorTest {
    private val long = "https://example.com/" + "a".repeat(3000)

    @Test fun longLinkIsTooLongNotSilent() {
        assertEquals(QrResult.TooLong, QrGenerator.generate(long, 200))
        assertFalse(QrGenerator.fits(long))
        assertTrue(QrGenerator.fits("https://example.com/cancion"))
        assertEquals(QrResult.Empty, QrGenerator.generate("  ", 200))
    }

    @Test fun normalLinkDecodes() {
        val url = "https://open.spotify.com/track/abc123"
        val bmp = (QrGenerator.generate(url, 300) as QrResult.Ok).bitmap
        val px = IntArray(bmp.width * bmp.height); bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bmp.width, bmp.height, px))))
        assertEquals(url, result.text)
    }

    @Test fun renderDrawsMarkerForLongLinkWithoutThrowing() {
        val asset = PhotoAsset(path = "x", pixelWidth = 40, pixelHeight = 40)
        val photo = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        val project = PolarProject(settings = PrintSettings(style = TemplateStyle.SPOTIFY, songURL = long), photos = listOf(asset), placements = listOf(PhotoPlacement(asset.id)))
        val bitmap = Bitmap.createBitmap(612 * 4, 792 * 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        PolarRenderer.drawPage(Canvas(bitmap), project, 0, false, 4f, { photo }, null)
        val card = PolarRenderer.cardRects(project, 0).first()
        val side = minOf(card.width * 0.21, card.height * 0.15)
        val x0 = ((card.maxX - card.width * 0.035 - side) * 4).toInt(); val y0 = ((card.maxY - card.height * 0.03 - side) * 4).toInt()
        var red = 0
        for (x in x0..(x0 + (side * 4).toInt())) for (y in y0..(y0 + (side * 4).toInt())) {
            val c = bitmap.getPixel(x, y); if (Color.red(c) > 150 && Color.green(c) < 90 && Color.blue(c) < 90) red++
        }
        assertTrue("marcador rojo visible ($red px)", red > 20)
    }
}
