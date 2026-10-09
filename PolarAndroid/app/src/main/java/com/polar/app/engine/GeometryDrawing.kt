package com.polar.app.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.polar.app.model.*
import kotlin.math.min
import kotlin.math.roundToInt

/** Dibujo genérico de las primitivas de `estilos-geometria.json`: igual que en la Mac. */
internal object GeometryDrawing {

    fun parseColor(hex: String): Int = try { Color.parseColor(hex) } catch (_: IllegalArgumentException) { Color.BLACK }

    fun rect(card: PolarRect, x: Double, y: Double, w: Double, h: Double) = PolarRect(
        card.minX + x * card.width, card.minY + y * card.height,
        card.minX + (x + w) * card.width, card.minY + (y + h) * card.height
    )

    fun photoRects(card: PolarRect, geo: StyleGeometry): List<PolarRect> = geo.photoSlots.map { rect(card, it.x, it.y, it.w, it.h) }

    /** QR en puntos: x, y fracciones de la tarjeta; el lado es fracción del ancho. */
    fun qrRect(card: PolarRect, slot: QrSlotGeo): PolarRect {
        val side = slot.size * card.width
        val left = card.minX + slot.x * card.width
        val top = card.minY + slot.y * card.height
        return PolarRect(left, top, left + side, top + side)
    }

    /** Recorte de una foto: `radius` es fracción del lado menor del hueco, en puntos. */
    fun shapePath(rect: RectF, shape: RegionShape, radius: Double): Path = Path().apply {
        when (shape) {
            RegionShape.ELLIPSE -> addOval(rect, Path.Direction.CW)
            RegionShape.ROUND -> {
                val r = (radius.coerceIn(0.0, 0.5) * min(rect.width(), rect.height())).toFloat()
                addRoundRect(rect, r, r, Path.Direction.CW)
            }
            RegionShape.RECT -> addRect(rect, Path.Direction.CW)
        }
    }

    fun drawDecorations(canvas: Canvas, card: PolarRect, geo: StyleGeometry, layer: String, scale: Float) {
        for (d in geo.decorations) {
            if (d.layer != layer) continue
            val r = rect(card, d.x, d.y, d.w, d.h).toAndroidRectF(scale)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = parseColor(d.color)
                alpha = (d.opacity.coerceIn(0.0, 1.0) * 255).roundToInt()
            }
            canvas.save()
            if (d.rotationDeg != 0.0) canvas.rotate(d.rotationDeg.toFloat(), r.centerX(), r.centerY())
            when (d.type) {
                "rect" -> canvas.drawRect(r, paint)
                "roundRect" -> {
                    val rad = (d.radius * min(r.width(), r.height())).toFloat()
                    canvas.drawRoundRect(r, rad, rad, paint)
                }
                "ellipse" -> canvas.drawOval(r, paint)
                "ring" -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = (d.strokeW * card.width * scale).toFloat()
                    canvas.drawOval(r, paint)
                }
                "stripes" -> if (d.count > 0) {
                    val bw = r.width() / (2 * d.count - 1)
                    for (i in 0 until d.count) canvas.drawRect(r.left + 2 * i * bw, r.top, r.left + (2 * i + 1) * bw, r.bottom, paint)
                }
            }
            canvas.restore()
        }
    }
}
