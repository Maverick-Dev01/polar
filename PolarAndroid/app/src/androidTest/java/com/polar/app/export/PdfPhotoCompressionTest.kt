package com.polar.app.export

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.exifinterface.media.ExifInterface
import com.polar.app.model.PhotoAsset
import com.polar.app.model.PhotoPlacement
import com.polar.app.model.PolarProject
import com.polar.app.model.PrintSettings
import com.polar.app.model.PhotoLook
import com.polar.app.model.TemplateStyle
import com.polar.app.model.ImportedTemplate
import com.polar.app.model.TemplateRegion
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Random

@RunWith(AndroidJUnit4::class)
class PdfPhotoCompressionTest {

    private val cache get() = ApplicationProvider.getApplicationContext<Context>().cacheDir

    private fun photograph(): Bitmap {
        val bitmap = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
        val random = Random(7)
        val pixels = IntArray(bitmap.width * bitmap.height) { i ->
            val noise = random.nextInt(31) - 15
            Color.rgb((40 + i % 900 / 6 + noise).coerceIn(0, 255),
                (60 + i / 900 / 8 + noise).coerceIn(0, 255), (130 + noise).coerceIn(0, 255))
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.setHasAlpha(false)
        return bitmap
    }

    @Test fun photographicPdfUsesJpegWhileKeepingTheVectorText() {
        val bitmap = photograph()
        val asset = PhotoAsset(path = "fixture", pixelWidth = bitmap.width, pixelHeight = bitmap.height)
        val project = PolarProject(settings = PrintSettings(columns = 2, rows = 3, title = "Texto vectorial"),
            photos = listOf(asset), placements = listOf(PhotoPlacement(assetID = asset.id)))
        val pdf = File(cache, "qa-photo-compression.pdf")
        try {
            PolarExporter.exportPdf(project, pdf, { bitmap })
            val source = pdf.readBytes().toString(Charsets.ISO_8859_1)
            println("Photographic PDF: ${pdf.length()} bytes")
            assertTrue("Las fotos deben usar JPEG dentro del PDF", source.contains("/DCTDecode"))
            assertTrue("El texto debe conservar su fuente vectorial", source.contains("/Font"))
            assertTrue("Una foto a 300 ppp no debe pesar varios MB: ${pdf.length()}", pdf.length() < 700_000)
        } finally { bitmap.recycle() }
    }

    @Test fun fittedRoundedPhotosCompressAndRenderLikeTheScreen() {
        val bitmap = photograph()
        val asset = PhotoAsset(path = "fixture", pixelWidth = bitmap.width, pixelHeight = bitmap.height)
        val project = PolarProject(settings = PrintSettings(columns = 3, rows = 3, roundedPhotos = true),
            photos = listOf(asset), placements = List(9) { PhotoPlacement(assetID = asset.id, zoom = 0.7) })
        val plain = File(cache, "qa-fit-plain.pdf")
        val compact = File(cache, "qa-fit-compact.pdf")
        try {
            PolarExporter.exportPdf(project, plain, { bitmap }, optimizePhotos = false)
            PolarExporter.exportPdf(project, compact, { bitmap })
            println("Ajustar: sin optimizar=${plain.length()}, optimizado=${compact.length()}")
            assertTrue("Ajustar debe comprimir a la mitad", compact.length() < plain.length() / 2)
            val screen = PolarExporter.renderPageToBitmap(project, 0, 72, { bitmap })
            PdfRenderer(ParcelFileDescriptor.open(compact, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                renderer.openPage(0).use { page ->
                    val rendered = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
                    page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    val a = IntArray(rendered.width * rendered.height); val b = IntArray(a.size)
                    rendered.getPixels(a, 0, rendered.width, 0, 0, rendered.width, rendered.height)
                    screen.getPixels(b, 0, screen.width, 0, 0, screen.width, screen.height)
                    var sum = 0L
                    for (i in a.indices) sum += Math.abs(Color.red(a[i]) - Color.red(b[i])) + Math.abs(Color.green(a[i]) - Color.green(b[i])) + Math.abs(Color.blue(a[i]) - Color.blue(b[i]))
                    val mean = sum / (a.size * 3.0)
                    assertTrue("Diferencia media por canal $mean", mean <= 3.0)
                }
            }
        } finally { bitmap.recycle() }
    }

    @Test fun thirtyFilteredPhotosKeepFivePrintablePagesAndOriginalPixels() {
        val bitmap = photograph()
        val original = bitmap.copy(bitmap.config!!, false)
        val asset = PhotoAsset(path = "fixture", pixelWidth = bitmap.width, pixelHeight = bitmap.height)
        val project = PolarProject(settings = PrintSettings(columns = 2, rows = 3, title = "Imprimir 100 %"),
            photos = listOf(asset), placements = List(30) { PhotoPlacement(assetID = asset.id,
                quarterTurns = it % 4, zoom = 1.0 + (it % 3) * .3, photoLook = PhotoLook(warmth = it / 100.0)) })
        val lossless = File(cache, "qa-30-photos-lossless.pdf")
        val compact = File(cache, "qa-30-photos-compact.pdf")
        try {
            PolarExporter.exportPdf(project, lossless, { bitmap }, optimizePhotos = false)
            PolarExporter.exportPdf(project, compact, { bitmap })
            println("30 photos / 5 pages: lossless=${lossless.length()}, compact=${compact.length()}")
            assertTrue("Comprimir debe reducir al menos la mitad", compact.length() < lossless.length() / 2)
            assertTrue("Los originales no deben cambiar", bitmap.sameAs(original))
            val png = File(cache, "qa-30-photos-page.png")
            val jpg = File(cache, "qa-30-photos-page.jpg")
            PolarExporter.exportPng(project, 0, png, bitmapProvider = { bitmap })
            PolarExporter.exportJpeg(project, 0, jpg, bitmapProvider = { bitmap })
            println("Page 1 at 300 dpi: PNG=${png.length()}, JPEG=${jpg.length()}")
            assertTrue("JPEG fotográfico debe ser menor que PNG", jpg.length() < png.length())
            for (file in listOf(lossless, compact)) {
                PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                    assertEquals(5, renderer.pageCount)
                    for (p in 0 until 5) renderer.openPage(p).use { page ->
                        assertEquals(612, page.width)
                        assertEquals(792, page.height)
                        val rendered = Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888)
                        page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        assertEquals(Color.WHITE, rendered.getPixel(0, 0))
                        rendered.recycle()
                    }
                }
            }
        } finally { original.recycle(); bitmap.recycle() }
    }

    @Test fun jpegAlternativeKeeps300DpiAndPngRemainsLossless() {
        val project = PolarProject()
        val jpeg = File(cache, "qa-300dpi.jpg")
        val png = File(cache, "qa-300dpi.png")
        PolarExporter.exportJpeg(project, 0, jpeg)
        PolarExporter.exportPng(project, 0, png)
        assertTrue(jpeg.readBytes().take(2) == listOf(0xff.toByte(), 0xd8.toByte()))
        assertTrue(png.readBytes().take(8) == listOf(137,80,78,71,13,10,26,10).map { it.toByte() })
        val exif = ExifInterface(jpeg)
        assertEquals(300.0, exif.getAttributeDouble(ExifInterface.TAG_X_RESOLUTION, 0.0), 0.0)
        assertEquals(300.0, exif.getAttributeDouble(ExifInterface.TAG_Y_RESOLUTION, 0.0), 0.0)
        assertEquals(2, exif.getAttributeInt(ExifInterface.TAG_RESOLUTION_UNIT, 0))
        for (file in listOf(jpeg, png)) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            assertEquals(2550, bounds.outWidth)
            assertEquals(3300, bounds.outHeight)
        }
    }

    @Test fun qrRemainsReadableBesideACompressedPhotograph() {
        val bitmap = photograph()
        val asset = PhotoAsset(path = "fixture", pixelWidth = bitmap.width, pixelHeight = bitmap.height)
        val url = "https://polar.example/song"
        val project = PolarProject(settings = PrintSettings(style = TemplateStyle.SPOTIFY, columns = 2, rows = 3, songURL = url),
            photos = listOf(asset), placements = listOf(PhotoPlacement(assetID = asset.id)))
        val pdf = File(cache, "qa-compressed-photo-qr.pdf")
        try {
            PolarExporter.exportPdf(project, pdf, { bitmap })
            val source = pdf.readBytes().toString(Charsets.ISO_8859_1)
            assertEquals(1, Regex("/DCTDecode").findAll(source).count())
            assertTrue("El QR conserva su imagen sin JPEG", source.contains("/FlateDecode"))
            PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                renderer.openPage(0).use { page ->
                    val rendered = Bitmap.createBitmap(page.width * 3, page.height * 3, Bitmap.Config.ARGB_8888)
                    try {
                        page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        val pixels = IntArray(rendered.width * rendered.height)
                        rendered.getPixels(pixels, 0, rendered.width, 0, 0, rendered.width, rendered.height)
                        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(
                            RGBLuminanceSource(rendered.width, rendered.height, pixels))), mapOf(DecodeHintType.TRY_HARDER to true))
                        assertEquals(url, result.text)
                    } finally { rendered.recycle() }
                }
            }
        } finally { bitmap.recycle() }
    }

    @Test fun transparentPhotoAndImportedTemplateStayLossless() {
        val photo = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.argb(128, 20, 160, 80)) }
        val template = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val asset = PhotoAsset(path = "alpha", pixelWidth = 300, pixelHeight = 400)
        val imported = ImportedTemplate(path = "template", pixelWidth = 400, pixelHeight = 400,
            regions = listOf(TemplateRegion(x = .25, y = .25, width = .5, height = .5)))
        val project = PolarProject(settings = PrintSettings(style = TemplateStyle.IMPORTED, importedTemplate = imported),
            photos = listOf(asset), placements = listOf(PhotoPlacement(assetID = asset.id)))
        val pdf = File(cache, "qa-template-alpha.pdf")
        try {
            PolarExporter.exportPdf(project, pdf, { photo }, template)
            val source = pdf.readBytes().toString(Charsets.ISO_8859_1)
            assertTrue("La foto con alpha no se convierte a JPEG", !source.contains("/DCTDecode"))
            assertTrue("La transparencia conserva su máscara", source.contains("/SMask"))
            PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                renderer.openPage(0).use { page ->
                    val rendered = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    val color = rendered.getPixel(page.width / 2, page.height / 2)
                    assertEquals(137.0, Color.red(color).toDouble(), 2.0)
                    assertEquals(80.0, Color.green(color).toDouble(), 2.0)
                    assertEquals(40.0, Color.blue(color).toDouble(), 2.0)
                    rendered.recycle()
                }
            }
        } finally { photo.recycle(); template.recycle() }
    }
}
