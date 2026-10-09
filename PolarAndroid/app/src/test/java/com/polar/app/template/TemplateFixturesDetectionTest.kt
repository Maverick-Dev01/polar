package com.polar.app.template

import com.polar.app.SharedFixtures
import com.polar.app.model.RegionShape
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La detección sobre las imágenes de `shared-fixtures/moldes` devuelve lo que declara `moldes.json` (±0.01). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TemplateFixturesDetectionTest {
    private val manifest = Json.parseToJsonElement(SharedFixtures.file("moldes/moldes.json").readText()).jsonObject["templates"]!!.jsonObject

    private fun shapeOf(name: String) = when (name) { "round" -> RegionShape.ROUND; "ellipse" -> RegionShape.ELLIPSE; else -> RegionShape.RECT }

    @Test fun detectionMatchesTheManifest() {
        for ((file, spec) in manifest) {
            val expected = (spec.jsonObject["regions"] ?: continue).jsonArray
            val load = TemplateImporter.loadTemplate(SharedFixtures.file("moldes/$file"))
            val regions = load.template.regions
            assertEquals(file, expected.size, regions.size)
            for ((i, e) in expected.withIndex()) {
                val rect = e.jsonObject["rect"]!!.jsonArray.map { it.jsonPrimitive.double }
                val r = regions[i]
                assertEquals("$file #$i x", rect[0], r.x, 0.01); assertEquals("$file #$i y", rect[1], r.y, 0.01)
                assertEquals("$file #$i w", rect[2], r.width, 0.01); assertEquals("$file #$i h", rect[3], r.height, 0.01)
                assertEquals("$file #$i forma", shapeOf(e.jsonObject["shape"]!!.jsonPrimitive.content), r.shape)
                assertEquals("$file #$i radio", e.jsonObject["radius"]!!.jsonPrimitive.double, r.radius, 0.01)
            }
            assertFalse(file, load.approximateShape)
            assertNotNull(load.fingerprint)
        }
    }

    @Test fun halfSizeImageDetectsTheSameRegions() {
        val full = TemplateImporter.loadTemplate(SharedFixtures.file("moldes/molde-rects.png")).template.regions
        val half = TemplateImporter.loadTemplate(SharedFixtures.file("moldes/molde-rects-50.png")).template.regions
        assertEquals(full.size, half.size)
        for ((a, b) in full.zip(half)) { assertEquals(a.x, b.x, 0.01); assertEquals(a.width, b.width, 0.01) }
    }
}
