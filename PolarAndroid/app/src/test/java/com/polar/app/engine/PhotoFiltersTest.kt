package com.polar.app.engine
import com.polar.app.model.PhotoLook
import org.junit.Assert.*
import org.junit.Test

class PhotoFiltersTest {
    @Test fun fiveColorFixtureMatchesAllEightDocumentedMatrices() {
        val source=listOf(intArrayOf(12,36,90,255),intArrayOf(200,70,40,255),intArrayOf(20,190,120,255),intArrayOf(240,235,220,255),intArrayOf(128,128,128,255))
        val expected=listOf(
            listOf("12,36,90","35,35,35","21,21,21","49,44,34","23,36,81","0,36,102","33,48,82","0,27,101"),
            listOf("200,70,40","95,95,95","91,91,91","140,125,97","217,69,25","188,70,52","168,86,67","236,57,16"),
            listOf("20,190,120","149,149,149","152,152,152","177,157,123","26,192,107","8,190,132","69,176,132","0,208,111"),
            listOf("240,235,220","235,235,235","251,251,251","255,255,220","252,235,207","228,235,232","231,227,218","253,246,225"),
            listOf("128,128,128","128,128,128","128,128,128","173,154,120","140,128,116","116,128,140","131,131,131","128,128,128")
        )
        for((s,channels) in source.withIndex()) for((p,preset) in PhotoLook.PRESETS.withIndex()) {
            val actual=PhotoFilters.rgba(channels,PhotoFilters.matrix(PhotoLook(preset=preset)))
            assertEquals("$preset/$s",expected[s][p],actual.take(3).joinToString(","));assertEquals(255,actual[3])
        }
    }
    @Test fun zeroIntensityAndGrainAreNeutralAndNoiseIsSpatiallyDeterministic() {
        for(preset in PhotoLook.PRESETS) {
            val look=PhotoLook(preset=preset,intensity=0.0)
            assertArrayEquals(PhotoFilters.identity,PhotoFilters.matrix(look),1e-10)
            assertEquals(0.0,PhotoFilters.grainStrength(look),0.0)
        }
        val seed=PhotoFilters.seed("12345678-1234-1234-1234-123456789abc",2)
        assertEquals(seed,PhotoFilters.seed("12345678-1234-1234-1234-123456789ABC",2))
        val first=(0..100).map { PhotoFilters.noise(seed,it,7,1.0) }
        assertEquals(first,(0..100).map { PhotoFilters.noise(seed,it,7,1.0) })
        assertTrue(first.distinct().size>8)
        assertArrayEquals(intArrayOf(255,255,244,100),PhotoFilters.rgba(intArrayOf(255,255,255,100),PhotoFilters.matrix(PhotoLook(preset="sepia")),5))
    }
    @Test fun adjustmentsComposeInDocumentedOrderWithoutIntermediateClamping() {
        val output=PhotoFilters.rgba(intArrayOf(200,70,40,255),PhotoFilters.matrix(PhotoLook(preset="cool",intensity=.5,light=.5,contrast=-.4,warmth=.3)))
        assertArrayEquals(intArrayOf(212,107,82,255),output)
    }
}
