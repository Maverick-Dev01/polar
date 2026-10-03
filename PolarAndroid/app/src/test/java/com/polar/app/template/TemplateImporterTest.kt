package com.polar.app.template

import com.polar.app.model.TemplateRegion
import org.junit.Assert.*
import org.junit.Test

class TemplateImporterTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    @Test
    fun testDetectTransparentAndWhiteHoles() {
        val width = 400
        val height = 400
        val pixels = IntArray(width * height) { argb(255, 30, 30, 30) }

        // Draw transparent hole (50, 50, w: 100, h: 100)
        for (y in 50 until 150) {
            for (x in 50 until 150) {
                pixels[y * width + x] = argb(0, 0, 0, 0)
            }
        }

        // Draw white hole (200, 50, w: 100, h: 100)
        for (y in 50 until 150) {
            for (x in 200 until 300) {
                pixels[y * width + x] = argb(255, 255, 255, 255)
            }
        }

        val regions = TemplateImporter.findRegionsFromPixels(pixels, width, height)
        assertEquals(2, regions.size)

        val first = regions[0]
        val second = regions[1]

        // Should be ordered by row and left-to-right:
        // First is x ~ 50/400 = 0.125, width ~ 100/400 = 0.25
        assertEquals(0.125, first.x, 0.01)
        assertEquals(0.125, first.y, 0.01)
        assertEquals(0.25, first.width, 0.01)
        assertEquals(0.25, first.height, 0.01)
        assertTrue(first.isTransparent)

        // Second is x ~ 200/400 = 0.50, width ~ 100/400 = 0.25
        assertEquals(0.50, second.x, 0.01)
        assertEquals(0.125, second.y, 0.01)
        assertEquals(0.25, second.width, 0.01)
        assertEquals(0.25, second.height, 0.01)
        assertFalse(second.isTransparent)
    }

    @Test
    fun testFallbackWhenNoHolesDetected() {
        val width = 200
        val height = 200
        val pixels = IntArray(width * height) { argb(255, 100, 100, 100) }

        val regions = TemplateImporter.findRegionsFromPixels(pixels, width, height)
        assertEquals(1, regions.size)
        val fallback = regions.first()
        assertEquals(0.2, fallback.x, 0.001)
        assertEquals(0.2, fallback.y, 0.001)
        assertEquals(0.6, fallback.width, 0.001)
        assertEquals(0.6, fallback.height, 0.001)
    }

    @Test
    fun regionIdsAreMacCompatibleUuids() {
        val w = 40; val h = 40
        val pixels = IntArray(w * h) { 0xFF333333.toInt() }
        for (y in 10 until 30) for (x in 10 until 30) pixels[y * w + x] = 0xFFFFFFFF.toInt()
        val regions = TemplateImporter.findRegionsFromPixels(pixels, w, h)
        assertTrue(regions.isNotEmpty())
        assertTrue(regions.all { Regex("^[0-9A-F-]{36}$").matches(it.id) })
    }
}
