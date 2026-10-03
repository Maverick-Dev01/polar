package com.polar.app.data

/** Cálculos de decodificación sin Android, para poder probarlos. Valores EXIF estándar 1–8. */
object BitmapMath {
    fun sampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        val longSide = maxOf(width, height)
        // BitmapFactory redondea hacia arriba el tamaño muestreado
        while ((longSide + sample - 1) / sample > maxDim) sample *= 2
        return sample
    }

    fun isQuarterTurned(exifOrientation: Int): Boolean = exifOrientation in 5..8

    fun orientedSize(width: Int, height: Int, exifOrientation: Int): Pair<Int, Int> =
        if (isQuarterTurned(exifOrientation)) height to width else width to height
}
