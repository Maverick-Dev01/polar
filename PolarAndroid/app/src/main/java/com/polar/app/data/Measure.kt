package com.polar.app.data

import java.util.Locale

/** Los diseños guardan medidas en puntos (1 pt = 1/72 pulg.); estas funciones las llevan a las unidades elegidas. */
val Units.pointsPerUnit: Double get() = if (this == Units.INCHES) 72.0 else 72.0 / 25.4

fun pointsToUnit(points: Double, units: Units): Double = points / units.pointsPerUnit

fun unitToPoints(value: Double, units: Units): Double = value * units.pointsPerUnit

/** Presentación redondeada a dos decimales, como en la Mac: «8.47 mm» o «0.33 pulg.». */
fun formatMeasure(points: Double, units: Units, unitLabel: String): String =
    String.format(Locale.ROOT, "%.2f %s", pointsToUnit(points, units), unitLabel)
