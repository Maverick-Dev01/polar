package com.polar.app.template

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.polar.app.data.BitmapMath
import com.polar.app.data.applyExifOrientation
import com.polar.app.model.ImportedTemplate
import com.polar.app.model.PolarException
import com.polar.app.model.TemplateRegion
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class TemplateLoad(val template: ImportedTemplate, val detectedCount: Int)

object TemplateImporter {

    fun loadTemplate(file: File): TemplateLoad {
        if (!file.exists() || !file.canRead()) {
            throw PolarException("No pudimos leer esa imagen. Prueba con un PNG o JPG.")
        }

        // Check dimensions without loading entire bitmap into memory
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        var origWidth = options.outWidth
        var origHeight = options.outHeight
        var orientation = ExifInterface.ORIENTATION_NORMAL

        if (origWidth <= 0 || origHeight <= 0) {
            throw PolarException("No pudimos abrir esa imagen. Prueba con un PNG o JPG.")
        }

        if (origWidth > 30000 || origHeight > 30000 ||
            origWidth.toDouble() * origHeight.toDouble() > 150_000_000
        ) {
            throw PolarException("La imagen es demasiado grande. Usa una de menos de 150 megapíxeles.")
        }

        // Handle EXIF orientation
        try {
            val exif = ExifInterface(file.absolutePath)
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            if (BitmapMath.isQuarterTurned(orientation)) {
                val temp = origWidth
                origWidth = origHeight
                origHeight = temp
            }
        } catch (e: Exception) {
            Log.w("Polar", "No se pudo leer EXIF de la plantilla", e)
        }

        // Downsample for region detection if needed
        val maxDim = max(origWidth, origHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > 1600) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw PolarException("No pudimos abrir esa imagen. Prueba con un PNG o JPG.")
        val bitmap = applyExifOrientation(raw, orientation)

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()

        val found = findRegionsFromPixels(pixels, width, height)
        val detectedCount = if (found.size == 1 && found[0].x == 0.2 && found[0].width == 0.6) 0 else found.size

        return TemplateLoad(
            template = ImportedTemplate(path = file.absolutePath, pixelWidth = origWidth, pixelHeight = origHeight, regions = found),
            detectedCount = detectedCount
        )
    }

    fun findRegionsFromPixels(pixels: IntArray, width: Int, height: Int): List<TemplateRegion> {
        val transparent = scanRegions(pixels, width, height, transparent = true)
        val white = scanRegions(pixels, width, height, transparent = false)
        val combined = transparent + white

        val ordered = rowOrder(combined)
        val accepted = ordered.take(64)
        if (accepted.isEmpty()) {
            return listOf(TemplateRegion(x = 0.2, y = 0.2, width = 0.6, height = 0.6))
        }
        return accepted
    }

    private fun scanRegions(
        pixels: IntArray,
        width: Int,
        height: Int,
        transparent: Boolean
    ): List<TemplateRegion> {
        val visited = BooleanArray(width * height)

        fun matches(index: Int): Boolean {
            val p = pixels[index]
            val a = (p ushr 24) and 0xFF
            if (transparent) {
                return a <= 24
            }
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            return a >= 245 && r >= 235 && g >= 235 && b >= 235
        }

        val output = mutableListOf<TemplateRegion>()
        val queue = IntArray(width * height)

        for (start in 0 until width * height) {
            if (visited[start] || !matches(start)) continue
            visited[start] = true

            var head = 0
            var tail = 0
            queue[tail++] = start

            var minX = width
            var minY = height
            var maxX = 0
            var maxY = 0
            var edge = false

            while (head < tail) {
                val i = queue[head++]
                val x = i % width
                val y = i / width

                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y

                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    edge = true
                }

                fun visit(n: Int) {
                    if (!visited[n] && matches(n)) {
                        visited[n] = true
                        queue[tail++] = n
                    }
                }

                if (x > 0) visit(i - 1)
                if (x + 1 < width) visit(i + 1)
                if (y > 0) visit(i - width)
                if (y + 1 < height) visit(i + width)
            }

            val w = maxX - minX + 1
            val h = maxY - minY + 1
            val area = w * h
            val count = tail

            if (!edge &&
                w >= max(8, width / 30) &&
                h >= max(8, height / 30) &&
                area >= (width * height) / 200 &&
                area < (width * height) * 9 / 10 &&
                (count.toDouble() / area.toDouble()) >= 0.90
            ) {
                output.add(
                    TemplateRegion(
                        x = minX.toDouble() / width,
                        y = minY.toDouble() / height,
                        width = w.toDouble() / width,
                        height = h.toDouble() / height,
                        isTransparent = transparent
                    )
                )
            }
        }
        return output
    }

    private fun rowOrder(regions: List<TemplateRegion>): List<TemplateRegion> {
        val sorted = regions.sortedWith(compareBy({ it.y }, { it.x }))
        val rows = mutableListOf<MutableList<TemplateRegion>>()

        for (region in sorted) {
            val lastRow = rows.lastOrNull()
            val first = lastRow?.firstOrNull()
            if (first != null && abs(region.y - first.y) <= min(region.height, first.height) * 0.25) {
                lastRow.add(region)
            } else {
                rows.add(mutableListOf(region))
            }
        }

        return rows.flatMap { row -> row.sortedBy { it.x } }
    }
}
