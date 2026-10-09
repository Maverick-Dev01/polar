package com.polar.app.template

import java.io.File
import java.security.MessageDigest

/** Huella de un molde: dHash perceptual de 64 bits y SHA-256 del archivo. */
data class TemplateFingerprint(val dhash: Long, val sha256: String) {
    val dhashHex: String get() = hex(dhash)

    companion object {
        const val DUPLICATE_DISTANCE = 6

        fun hex(hash: Long): String = "%016x".format(hash)
        fun parse(hex: String): Long = java.lang.Long.parseUnsignedLong(hex, 16)
        fun distance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * dHash de 64 bits: se compone sobre blanco, se pasa a gris (0.299 R + 0.587 G + 0.114 B), se reduce a 9×8 por
         * promedio de área exacto (límites fraccionarios) y cada celda se compara con la de su derecha
         * (con 1 nivel de gris de tolerancia contra empates). El primer bit es el más significativo.
         */
        fun dhash(pixels: IntArray, width: Int, height: Int): Long {
            require(width > 0 && height > 0 && pixels.size >= width * height)
            val cells = Array(8) { DoubleArray(9) }
            for (cy in 0 until 8) {
                val y0 = cy * height / 8.0
                val y1 = (cy + 1) * height / 8.0
                for (cx in 0 until 9) {
                    val x0 = cx * width / 9.0
                    val x1 = (cx + 1) * width / 9.0
                    var sum = 0.0
                    for (py in y0.toInt() until minOf(height, Math.ceil(y1).toInt())) {
                        val wy = minOf(py + 1.0, y1) - maxOf(py.toDouble(), y0)
                        if (wy <= 0) continue
                        for (px in x0.toInt() until minOf(width, Math.ceil(x1).toInt())) {
                            val wx = minOf(px + 1.0, x1) - maxOf(px.toDouble(), x0)
                            if (wx > 0) sum += gray(pixels[py * width + px]) * wx * wy
                        }
                    }
                    cells[cy][cx] = sum / ((x1 - x0) * (y1 - y0))
                }
            }
            var hash = 0L
            for (row in cells) for (x in 0 until 8) hash = (hash shl 1) or if (row[x] > row[x + 1] + 1.0) 1L else 0L
            return hash
        }

        private fun gray(p: Int): Double {
            val a = ((p ushr 24) and 0xFF) / 255.0
            val r = ((p ushr 16) and 0xFF) * a + 255.0 * (1 - a)
            val g = ((p ushr 8) and 0xFF) * a + 255.0 * (1 - a)
            val b = (p and 0xFF) * a + 255.0 * (1 - a)
            return 0.299 * r + 0.587 * g + 0.114 * b
        }
    }
}
