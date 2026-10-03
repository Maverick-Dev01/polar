package com.polar.app.export

import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class PolarExporterTest {

    @Test
    fun test300DpiPixelDimensionsForLetter() {
        val settings = PrintSettings(paperSize = PaperSize.LETTER, orientation = PaperOrientation.PORTRAIT)
        val size = settings.paperSizePoints
        val pxWidth = (size.width * 300.0 / 72.0).roundToInt()
        val pxHeight = (size.height * 300.0 / 72.0).roundToInt()

        // 8.5 x 11 inches at 300 DPI = 2550 x 3300 px
        assertEquals(2550, pxWidth)
        assertEquals(3300, pxHeight)
    }

    @Test
    fun test300DpiPixelDimensionsForA4() {
        val settings = PrintSettings(paperSize = PaperSize.A4, orientation = PaperOrientation.PORTRAIT)
        val size = settings.paperSizePoints
        val pxWidth = (size.width * 300.0 / 72.0).roundToInt()
        val pxHeight = (size.height * 300.0 / 72.0).roundToInt()

        // 210 x 297 mm at 300 DPI = ~2480 x 3508 px
        assertEquals(2480, pxWidth)
        assertEquals(3508, pxHeight)
    }

    @Test
    fun testPageBoundsValidation() {
        val project = PolarProject()
        assertTrue(project.pageCount >= 1)
        assertFalse(0 !in 0 until project.pageCount)
    }
}
