package com.polar.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import com.polar.app.model.PolarException
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

data class PhotoInfo(val width: Int, val height: Int, val takenAtEpochMs: Long?)

/** Decodifica fotos con muestreo y orientación EXIF; guarda en memoria las recientes. */
class BitmapLoader(private val context: Context) {
    companion object {
        const val PREVIEW_MAX = 1600
        const val EXPORT_MAX = 3600
        private const val TAG = "Polar"
    }

    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private fun open(path: String): InputStream? =
        when {
            path.startsWith("content:") -> context.contentResolver.openInputStream(Uri.parse(path))
            path.startsWith("file:") -> Uri.parse(path).path?.let { File(it) }?.takeIf { it.exists() }?.inputStream()
            else -> File(path).takeIf { it.exists() }?.inputStream()
        }

    private fun orientation(path: String): Int = try {
        open(path)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: 1
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo leer la orientación EXIF: $path", e)
        1
    }

    fun load(path: String, maxDim: Int): Bitmap? = load(path, maxDim, false)

    fun loadForPrint(path: String, maxDim: Int): Bitmap = load(path, maxDim, true)
        ?: throw PolarException("No se pudo leer una foto del diseño. Revisa que el original esté disponible.")

    private fun load(path: String, maxDim: Int, exactSize: Boolean): Bitmap? {
        require(maxDim > 0)
        val key = "$path@$maxDim@$exactSize"
        cache.get(key)?.takeIf { !it.isRecycled }?.let { return it }
        return try {
            if (exactSize && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Decode to the print size directly: no full 48 MP bitmap plus resized copy in memory.
                val source = when {
                    path.startsWith("content:") -> ImageDecoder.createSource(context.contentResolver, Uri.parse(path))
                    path.startsWith("file:") -> ImageDecoder.createSource(File(Uri.parse(path).path ?: return null))
                    else -> ImageDecoder.createSource(File(path))
                }
                val result = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                    val scale = minOf(1.0, maxDim.toDouble() / maxOf(info.size.width, info.size.height))
                    decoder.setTargetSize((info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1))
                }
                cache.put(key, result)
                return result // ImageDecoder applies EXIF orientation itself.
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // Con inJustDecodeBounds decodeStream devuelve null por contrato: sólo importa que el archivo abra.
            open(path)?.use { BitmapFactory.decodeStream(it, null, bounds); true } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                // Para imprimir se decodifica por encima del objetivo y se reduce exactamente, nunca por debajo de 300 ppp.
                val samplingTarget = if (exactSize) (maxDim.toLong() * 2 - 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else maxDim
                inSampleSize = BitmapMath.sampleSize(bounds.outWidth, bounds.outHeight, samplingTarget)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = open(path)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
            val oriented = applyExifOrientation(raw, orientation(path))
            val longSide = maxOf(oriented.width, oriented.height)
            val result = if (exactSize && longSide > maxDim) {
                val scale = maxDim.toDouble() / longSide
                Bitmap.createScaledBitmap(oriented, (oriented.width * scale).roundToInt().coerceAtLeast(1),
                    (oriented.height * scale).roundToInt().coerceAtLeast(1), true).also { if (it != oriented) oriented.recycle() }
            } else oriented
            cache.put(key, result)
            result
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Sin memoria al decodificar la foto: $path", e)
            cache.evictAll()
            if (exactSize) throw PolarException("No hay memoria suficiente para imprimir esta foto con su resolución original. Intenta exportar una hoja a la vez.")
            null
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo decodificar la foto: $path", e)
            null
        }
    }

    /** Medidas ya orientadas y fecha de captura (EXIF `DateTimeOriginal`, si existe). */
    fun readInfo(uri: Uri): PhotoInfo? = try {
        val path = uri.toString()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(path)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
        else {
            var taken: Long? = null
            var exifOrientation = 1
            try {
                open(path)?.use { stream ->
                    val exif = ExifInterface(stream)
                    exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                    val text = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    taken = text?.let { runCatching { SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ROOT).parse(it)?.time }.getOrNull() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo leer el EXIF de la foto: $path", e)
                taken = null
                exifOrientation = 1
            }
            val (w, h) = BitmapMath.orientedSize(bounds.outWidth, bounds.outHeight, exifOrientation)
            PhotoInfo(w, h, taken)
        }
    } catch (e: Exception) {
        Log.w(TAG, "No se pudieron leer las medidas de la foto: $uri", e)
        null
    }

    fun clear() = cache.evictAll()
}

/** Fotos y plantillas deben usar el mismo giro y reflejo EXIF. */
internal fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            2 -> m.setScale(-1f, 1f)
            3 -> m.setRotate(180f)
            4 -> m.setScale(1f, -1f)
            5 -> { m.setRotate(90f); m.postScale(-1f, 1f) }
            6 -> m.setRotate(90f)
            7 -> { m.setRotate(-90f); m.postScale(-1f, 1f) }
            8 -> m.setRotate(-90f)
            else -> return bitmap
        }
        val out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        if (out != bitmap) bitmap.recycle()
        return out
}
