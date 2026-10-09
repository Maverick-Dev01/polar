package com.polar.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MeasureTest {
    @Test fun pointsToMillimetersIsExact() {
        assertEquals(25.4, pointsToUnit(72.0, Units.MM), 1e-9)
        assertEquals(8.4667, pointsToUnit(24.0, Units.MM), 1e-3)
    }

    @Test fun pointsToInchesIsExact() {
        assertEquals(1.0, pointsToUnit(72.0, Units.INCHES), 1e-12)
        assertEquals(0.8333, pointsToUnit(60.0, Units.INCHES), 1e-3)
    }

    @Test fun roundTripKeepsThePointValue() {
        for (u in Units.entries) for (pt in listOf(0.0, 12.5, 24.0, 60.0)) {
            assertEquals(pt, unitToPoints(pointsToUnit(pt, u), u), 1e-9)
        }
    }

    @Test fun presentationRoundsToTwoDecimalsWithoutChangingTheStoredValue() {
        assertEquals("8.47 mm", formatMeasure(24.0, Units.MM, "mm"))
        assertEquals("0.33 pulg.", formatMeasure(24.0, Units.INCHES, "pulg."))
        assertEquals("0.00 mm", formatMeasure(0.0, Units.MM, "mm"))
        assertEquals("21.17 mm", formatMeasure(60.0, Units.MM, "mm"))
    }
}
