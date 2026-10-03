package com.polar.app.engine

import android.graphics.*
import com.polar.app.model.PhotoBackground
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** La máscara cambia alpha, nunca reconstruye los píxeles del rostro. */
internal object PhotoCompositor {
    fun draw(canvas: Canvas, bounds: RectF, photo: Bitmap, mask: Bitmap, background: Bitmap?, options: PhotoBackground,
             fit: PhotoFitResult, scale: Float, photoPaint: Paint) {
        val compositeLayer = canvas.saveLayer(bounds, photoPaint)
        try {
        options.colorHex?.let { canvas.drawRect(bounds, Paint().apply { color = Color.parseColor("#$it") }) }
        background?.let { image ->
            val factor = max(bounds.width()/image.width, bounds.height()/image.height)
            val w = image.width*factor; val h = image.height*factor
            canvas.drawBitmap(image, null, RectF(bounds.centerX()-w/2, bounds.centerY()-h/2, bounds.centerX()+w/2, bounds.centerY()+h/2), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        // RGBA conserva el alpha al componer con DST_IN en Canvas y PdfDocument.
        val alpha = Bitmap.createBitmap(ceil(bounds.width()).toInt().coerceAtLeast(1), ceil(bounds.height()).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        try {
            val mc = Canvas(alpha)
            mc.translate((fit.centerX*scale).toFloat(), (fit.centerY*scale).toFloat())
            mc.rotate(fit.degrees)
            val target = RectF((-fit.width*scale/2).toFloat(), (-fit.height*scale/2).toFloat(), (fit.width*scale/2).toFloat(), (fit.height*scale/2).toFloat())
            mc.drawBitmap(mask, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                val radius = min(alpha.width, alpha.height) * .006f * options.feather.toFloat()
                if (radius > 0) maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            })
            if (options.shadow > 0) canvas.drawBitmap(alpha, bounds.left + bounds.width()*.012f, bounds.top + bounds.height()*.018f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = Color.BLACK; this.alpha = (options.shadow*110).toInt()
                colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                maskFilter = BlurMaskFilter(max(1f, min(alpha.width, alpha.height)*.025f), BlurMaskFilter.Blur.NORMAL)
            })
            val layer = canvas.saveLayer(bounds, null)
            canvas.save()
            canvas.translate(bounds.left + (fit.centerX*scale).toFloat(), bounds.top + (fit.centerY*scale).toFloat())
            canvas.rotate(fit.degrees)
            canvas.drawBitmap(photo, null, target, Paint(photoPaint).apply { colorFilter = null })
            canvas.restore()
            canvas.drawBitmap(alpha, null, bounds, Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
            canvas.restoreToCount(layer)
        } finally { alpha.recycle() }
        } finally { canvas.restoreToCount(compositeLayer) }
    }
}
