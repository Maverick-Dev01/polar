package com.polar.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.LruCache
import com.polar.app.model.*
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class Thumbnailer(private val fonts: FontProvider) {
    private companion object { const val CARD_POINTS = 200.0 }
    private val styles = LruCache<String, Bitmap>(48)

    fun styleCard(style: TemplateStyle, widthPx: Int): Bitmap {
        val key = "${style.name}@$widthPx"
        styles.get(key)?.let { return it }
        val aspect = if (style == TemplateStyle.IMPORTED) 0.75 else style.defaultAspect
        // Tarjeta del tamaño aproximado de una real (≈ 200 pt): el piso de 6 pt del texto no debe cortar la miniatura.
        val card = PolarRect(0.0, 0.0, CARD_POINTS, CARD_POINTS / aspect)
        val scale = widthPx / CARD_POINTS.toFloat()
        val bitmap = Bitmap.createBitmap(widthPx, (card.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val project = PolarProject(settings = PrintSettings(style = style, cutGuides = false, caption = "Tu mensaje", song = "Mi canción")).normalized()
        PolarRenderer.drawCardPreview(Canvas(bitmap), project, card, scale, fonts)
        styles.put(key, bitmap)
        return bitmap
    }

    fun page(project: PolarProject, page: Int, widthPx: Int, bitmapProvider: (PhotoAsset) -> Bitmap?, template: Bitmap?): Bitmap {
        val paper = project.settings.paperSizePoints
        val scale = widthPx / paper.width.toFloat()
        val bitmap = Bitmap.createBitmap(widthPx, (paper.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }
        PolarRenderer.drawPage(canvas, project, page, isPreview = false, scale = scale, bitmapProvider = bitmapProvider, templateBitmap = template, fonts = fonts)
        return bitmap
    }

    fun projectPng(project: PolarProject, bitmapProvider: (PhotoAsset) -> Bitmap?, template: Bitmap?): ByteArray {
        val bitmap = page(project, 0, 480, bitmapProvider, template)
        return ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out); bitmap.recycle(); out.toByteArray() }
    }
}
