package com.polar.app.data

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DownscaleTest {
    private fun fineLines(): Bitmap {
        val b = Bitmap.createBitmap(4000, 40, Bitmap.Config.ARGB_8888)
        val row = IntArray(4000) { if (it % 3 == 0) Color.BLACK else Color.WHITE }
        for (y in 0 until 40) b.setPixels(row, 0, 4000, 0, y, 4000, 1)
        return b
    }

    /** Energía de alta frecuencia: diferencias absolutas entre vecinos. */
    private fun energy(b: Bitmap): Double {
        val row = IntArray(b.width); b.getPixels(row, 0, b.width, 0, 0, b.width, 1)
        return (1 until row.size).sumOf { Math.abs(Color.red(row[it]) - Color.red(row[it - 1])).toDouble() } / row.size
    }

    @Test fun progressiveReductionAliasesLessThanOneStep() {
        val src = fineLines()
        val single = Bitmap.createScaledBitmap(src, 1000, 10, true)
        val progressive = downscaleProgressive(src, 1000, 10)
        assertEquals(1000, progressive.width); assertEquals(10, progressive.height)
        assertTrue("progresivo=${energy(progressive)} único=${energy(single)}", energy(progressive) < energy(single))
    }

    @Test fun keepsExactTargetSizeForNonPowerOfTwoRatios() {
        val r = downscaleProgressive(fineLines(), 1234, 12)
        assertEquals(1234, r.width); assertEquals(12, r.height)
    }
}
