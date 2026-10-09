package com.polar.app.template

import com.polar.app.model.RegionShape
import org.junit.Assert.*
import org.junit.Test

class ShapeDetectorTest {
    private fun mask(w: Int, h: Int, inside: (Double, Double) -> Boolean) =
        BooleanArray(w * h) { inside((it % w) + 0.5, (it / w) + 0.5) }

    private fun roundRect(w: Int, h: Int, radiusFraction: Double): BooleanArray {
        val r = radiusFraction * minOf(w, h)
        return mask(w, h) { x, y ->
            val dx = maxOf(r - x, x - (w - r), 0.0); val dy = maxOf(r - y, y - (h - r), 0.0)
            dx * dx + dy * dy <= r * r
        }
    }

    @Test fun squareIsARectangle() {
        val fit = ShapeDetector.detect(BooleanArray(100 * 100) { true }, 100, 100)
        assertEquals(RegionShape.RECT, fit.shape); assertFalse(fit.approximate)
    }

    @Test fun roundedRectangleWithTwentyPercentRadius() {
        for ((w, h) in listOf(200 to 200, 300 to 150, 150 to 300)) {
            val fit = ShapeDetector.detect(roundRect(w, h, 0.20), w, h)
            assertEquals("$w×$h", RegionShape.ROUND, fit.shape)
            assertEquals("$w×$h", 0.20, fit.radius, 0.05 * 0.20 + 0.005) // ±5 % del radio
            assertFalse(fit.approximate)
        }
    }

    @Test fun circleIsAnEllipse() {
        val fit = ShapeDetector.detect(mask(200, 200) { x, y -> (x - 100) * (x - 100) + (y - 100) * (y - 100) <= 100 * 100 }, 200, 200)
        assertEquals(RegionShape.ELLIPSE, fit.shape); assertFalse(fit.approximate)
    }

    @Test fun wideOvalIsAnEllipseNotAStadium() {
        val fit = ShapeDetector.detect(mask(300, 150) { x, y -> ((x - 150) / 150).let { it * it } + ((y - 75) / 75).let { it * it } <= 1 }, 300, 150)
        assertEquals(RegionShape.ELLIPSE, fit.shape)
    }

    @Test fun smallRoundingStaysARectangle() {
        assertEquals(RegionShape.RECT, ShapeDetector.detect(roundRect(200, 200, 0.03), 200, 200).shape)
    }

    @Test fun triangleFallsBackToRectangleWithApproximateWarning() {
        val fit = ShapeDetector.detect(mask(200, 200) { x, y -> y >= 200 - x * 2 && y >= 2 * (x - 100) && y >= 0 && x <= 200 && y >= 200 - (200 - x) * 2 }, 200, 200)
        assertEquals(RegionShape.RECT, fit.shape); assertTrue(fit.approximate)
    }
}
