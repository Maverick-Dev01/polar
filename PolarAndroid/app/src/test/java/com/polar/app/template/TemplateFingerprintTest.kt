package com.polar.app.template

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.polar.app.SharedFixtures
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.serialization.json.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TemplateFingerprintTest {
    @get:Rule val tmp = TemporaryFolder()
    private val manifest = Json.parseToJsonElement(SharedFixtures.file("moldes/moldes.json").readText()).jsonObject

    private fun bitmap(name: String): Bitmap = BitmapFactory.decodeFile(SharedFixtures.file("moldes/$name").absolutePath)!!
    private fun hash(b: Bitmap): Long {
        val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        return TemplateFingerprint.dhash(px, b.width, b.height)
    }
    private fun hash(name: String) = hash(bitmap(name))
    private fun dist(a: String, b: String) = TemplateFingerprint.distance(hash(a), hash(b))

    @Test fun matchesTheReferenceValuesOfTheSharedFixtures() {
        val values = manifest["dhash"]!!.jsonObject["values"]!!.jsonObject
        for ((name, hex) in values) {
            // 1 bit de diferencia como máximo: la referencia fue calculada con otro lenguaje.
            val d = TemplateFingerprint.distance(hash(name), TemplateFingerprint.parse(hex.jsonPrimitive.content))
            assertTrue("$name: distancia $d contra la referencia", d <= 1)
        }
    }

    @Test fun exactReferenceForTheFullSizeImages() {
        assertEquals("0101011111111180", TemplateFingerprint.hex(hash("molde-rects.png")))
    }

    @Test fun stableWhenRescaledHalfAndDouble() {
        val original = bitmap("molde-rects.png")
        assertEquals(0, TemplateFingerprint.distance(hash(original), hash("molde-rects-50.png")))
        val half = Bitmap.createScaledBitmap(original, original.width / 2, original.height / 2, true)
        val double = Bitmap.createScaledBitmap(original, original.width * 2, original.height * 2, true)
        assertTrue(TemplateFingerprint.distance(hash(original), hash(half)) <= 6)
        assertTrue(TemplateFingerprint.distance(hash(original), hash(double)) <= 6)
    }

    @Test fun stableWhenSavedAsJpegQuality80() {
        // Un JPG no tiene transparencia: se guarda ya compuesto sobre blanco, como lo haría cualquier editor.
        val original = bitmap("molde-rects.png")
        val flat = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888).also {
            android.graphics.Canvas(it).apply { drawColor(Color.WHITE); drawBitmap(original, 0f, 0f, null) }
        }
        val out = ByteArrayOutputStream(); flat.compress(Bitmap.CompressFormat.JPEG, 80, out)
        val jpg = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        assertTrue(TemplateFingerprint.distance(hash(original), hash(jpg)) <= 6)
    }

    @Test fun differentTemplatesAreFarApart() {
        assertTrue(dist("molde-rects.png", "molde-distinto-tira.png") > 12)
        assertTrue(dist("molde-circles.png", "molde-distinto-tira.png") > 12)
    }

    @Test fun similarTemplateWithRoundedHolesIsWithinTheThreshold() {
        assertTrue(dist("molde-rects.png", "molde-rounded.png") <= TemplateFingerprint.DUPLICATE_DISTANCE)
    }

    @Test fun shaIsStableAndSensitiveToBytes() {
        val a = SharedFixtures.file("moldes/molde-rects.png")
        val copy = File(tmp.root, "x.png").also { a.copyTo(it) }
        assertEquals(TemplateFingerprint.sha256(a), TemplateFingerprint.sha256(copy))
        assertNotEquals(TemplateFingerprint.sha256(a), TemplateFingerprint.sha256(SharedFixtures.file("moldes/molde-rects-50.png")))
        assertEquals(64, TemplateFingerprint.sha256(a).length)
    }

    @Test fun hexRoundTripKeepsAllSixteenDigits() {
        assertEquals("0000000000000001", TemplateFingerprint.hex(1L))
        assertEquals(-1L, TemplateFingerprint.parse("ffffffffffffffff"))
        assertEquals(0x0101011111111180L, TemplateFingerprint.parse("0101011111111180"))
    }
}
