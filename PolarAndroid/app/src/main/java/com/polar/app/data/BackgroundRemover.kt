package com.polar.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.polar.app.model.PhotoAsset
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Guarda sólo la máscara; los colores y detalle de impresión siguen viniendo del original. */
class BackgroundRemover(private val context: Context, private val bitmaps: BitmapLoader) {
    suspend fun mask(photo: PhotoAsset, directory: File): String {
        val segmenter = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build())
        try {
            val modules = ModuleInstall.getClient(context)
            try { withTimeout(120_000) {
                if (!modules.areModulesAvailable(segmenter).awaitValue().areModulesAvailable()) {
                    modules.installModules(ModuleInstallRequest.newBuilder().addApi(segmenter).build()).awaitValue()
                    while (!modules.areModulesAvailable(segmenter).awaitValue().areModulesAvailable()) delay(500)
                }
            }
            } catch (e: TimeoutCancellationException) { throw IOException("El modelo no terminó de descargarse. Comprueba la conexión y vuelve a intentarlo.", e) }
            val input = bitmaps.loadForPrint(photo.path, 1600)
            val result = try { withTimeout(60_000) { segmenter.process(InputImage.fromBitmap(input, 0)).awaitValue() } }
            catch (e: TimeoutCancellationException) { throw IOException("El recorte tardó demasiado. Se conserva el original; vuelve a intentarlo.", e) }
            val confidence = result.foregroundConfidenceMask ?: error("No se pudo detectar un sujeto. Prueba una foto con el fondo más definido.")
            confidence.rewind()
            val pixels = IntArray(input.width * input.height)
            require(confidence.remaining() == pixels.size) { "La máscara recibida no coincide con la foto." }
            var foreground = 0
            pixels.indices.forEach { i ->
                val c = confidence.get().coerceIn(0f, 1f)
                if (c > .5f) foreground++
                pixels[i] = Color.argb((c * 255).toInt(), 255, 255, 255)
            }
            require(foreground > pixels.size / 1000) { "No se encontró un sujeto claro. Se conserva la foto original." }
            val mask = Bitmap.createBitmap(pixels, input.width, input.height, Bitmap.Config.ARGB_8888)
            val output = File(directory.apply { mkdirs() }, "mask-${UUID.randomUUID()}.png")
            try {
                output.outputStream().use { require(mask.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                return output.absolutePath
            } catch (e: Throwable) { output.delete(); throw e }
            finally { mask.recycle() }
        } finally { segmenter.close() }
    }
}

private suspend fun <T> Task<T>.awaitValue(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
