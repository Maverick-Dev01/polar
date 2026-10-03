package com.polar.app.data

import org.junit.Assert.*
import org.junit.Test

class BitmapMathTest {
    @Test
    fun sampleSizeKeepsLongSideUnderLimit() {
        assertEquals(1, BitmapMath.sampleSize(1000, 800, 1600))
        assertEquals(4, BitmapMath.sampleSize(8000, 6000, 2000))
        assertEquals(4, BitmapMath.sampleSize(4001, 10, 2000))
        assertEquals(8, BitmapMath.sampleSize(10000, 7500, 1600)) // foto de 75 MP
    }

    @Test
    fun exifQuarterTurnsSwapSize() {
        assertEquals(3000 to 4000, BitmapMath.orientedSize(4000, 3000, 6))
        assertEquals(3000 to 4000, BitmapMath.orientedSize(4000, 3000, 8))
        assertEquals(4000 to 3000, BitmapMath.orientedSize(4000, 3000, 3))
        assertEquals(4000 to 3000, BitmapMath.orientedSize(4000, 3000, 1))
        assertTrue(BitmapMath.isQuarterTurned(5))
        assertFalse(BitmapMath.isQuarterTurned(2))
    }
}
