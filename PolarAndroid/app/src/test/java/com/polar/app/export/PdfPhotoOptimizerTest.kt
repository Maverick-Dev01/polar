package com.polar.app.export

import android.graphics.BitmapFactory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.Random
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfPhotoOptimizerTest {
    @get:Rule val folder = TemporaryFolder()
    private val latin = Charsets.ISO_8859_1
    private val rgb = ByteArray(300 * 400 * 3).also { Random(8).nextBytes(it) }

    private fun image(pixels: ByteArray): ByteArray {
        val compressed = ByteArrayOutputStream().apply {
            DeflaterOutputStream(this, Deflater(Deflater.NO_COMPRESSION)).use { it.write(pixels) }
        }.toByteArray()
        return ByteArrayOutputStream().apply {
            write("<</Type /XObject /Subtype /Image /Width 300 /Height 400 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length ${compressed.size}>> stream\n".toByteArray(latin))
            write(compressed)
            write("\nendstream".toByteArray(latin))
        }.toByteArray()
    }

    private fun fixture(): ByteArray {
        // Los marcadores están dentro del stream binario: sólo Length/xref delimitan los objetos.
        "endstream\n5 0 obj\nxref".toByteArray(latin).copyInto(rgb)
        val unrelated = rgb.clone().apply { this[100] = (this[100] + 1).toByte() }
        val objects = listOf("<</Type /Catalog /Pages 2 0 R>>".toByteArray(latin),
            "<</Type /Pages /Kids [3 0 R] /Count 1>>".toByteArray(latin),
            "<</Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources <</XObject <</Photo 5 0 R /Template 6 0 R>>>> /Contents 4 0 R>>".toByteArray(latin),
            "<</Length 8>> stream\nq Q q Q\n\nendstream".toByteArray(latin), image(rgb), image(unrelated))
        val pdf = ByteArrayOutputStream()
        pdf.write("%PDF-1.4\n".toByteArray(latin))
        val offsets = objects.mapIndexed { i, body ->
            pdf.size().also {
                pdf.write("${i + 1} 0 obj\n".toByteArray(latin)); pdf.write(body); pdf.write("\nendobj\n".toByteArray(latin))
            }
        }
        val xref = pdf.size()
        pdf.write("xref\n0 7\n0000000000 65535 f \n".toByteArray(latin))
        offsets.forEach { pdf.write(String.format(Locale.ROOT, "%010d 00000 n \n", it).toByteArray(latin)) }
        pdf.write("trailer\n<</Size 7 /Root 1 0 R>>\nstartxref\n$xref\n%%EOF".toByteArray(latin))
        return pdf.toByteArray()
    }

    @Test fun convertsOnlyTheRecordedPhotoAndRebuildsValidOffsets() {
        val source = folder.newFile("native.pdf").apply { writeBytes(fixture()) }
        val original = source.readBytes()
        val compact = folder.newFile("compact.pdf")
        assertTrue(PdfPhotoOptimizer.optimize(source, compact, setOf(PdfPhotoFingerprint.fromRgb(300, 400, rgb))))
        assertArrayEquals(original, source.readBytes())
        val text = compact.readBytes().toString(latin)
        assertEquals(1, Regex("/DCTDecode").findAll(text).count())
        assertEquals(1, Regex("/FlateDecode").findAll(text).count())
        val templateStart = original.toString(latin).indexOf("6 0 obj\n")
        val templateEnd = original.toString(latin).indexOf("xref\n0 7\n", templateStart)
        assertTrue(text.contains(String(original, templateStart, templateEnd - templateStart, latin)))
        val xref = Regex("startxref\\n(\\d+)").find(text)!!.groupValues[1].toInt()
        assertTrue(text.substring(xref).startsWith("xref\n0 7\n"))
        val rows = text.substring(xref).lines().drop(3).take(6)
        rows.forEachIndexed { i, row -> assertTrue(text.substring(row.take(10).toInt()).startsWith("${i + 1} 0 obj\n")) }
        val photo = text.substring(text.indexOf("5 0 obj\n"), text.indexOf("6 0 obj\n"))
        val length = Regex("/Length (\\d+)").find(photo)!!.groupValues[1].toInt()
        val start = photo.indexOf("stream\n") + 7
        val jpeg = photo.substring(start, start + length).toByteArray(latin)
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        assertEquals(300, decoded.width); assertEquals(400, decoded.height); decoded.recycle()
    }

    @Test fun unrecordedImagesAndUnsupportedXrefsKeepTheNativePdf() {
        val source = folder.newFile("native.pdf").apply { writeBytes(fixture()) }
        val output = folder.newFile("unused.pdf")
        assertFalse(PdfPhotoOptimizer.optimize(source, output, setOf(PdfPhotoFingerprint(300, 400, "not a photo"))))
        val original = source.readBytes()
        source.writeBytes(original.toString(latin).replace("xref\n0 7\n", "xref-stream\n0 7\n").toByteArray(latin))
        val unsupported = source.readBytes()
        assertFalse(PdfPhotoOptimizer.optimize(source, output, setOf(PdfPhotoFingerprint.fromRgb(300, 400, rgb))))
        assertArrayEquals(unsupported, source.readBytes())
    }
}
