package com.polar.app.template

import com.polar.app.model.RegionShape
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Decide la forma de un hueco a partir de su máscara: rectángulo, rectángulo redondeado u óvalo.
 * Elige el ajuste de mayor IoU con la máscara; si ninguno llega a [MIN_IOU], cae a rectángulo y avisa.
 */
object ShapeDetector {
    /**
     * Un hueco casi lleno es un rectángulo. El plan decía 90 %, pero un radio de 20 % sólo quita ~3 % del área:
     * con 90 % todo redondeado leve se leería como rectángulo, así que el corte es 99.5 %.
     */
    const val RECT_FILL = 0.995
    /** Lo que debe ganar un ajuste curvo sobre el rectángulo para que no lo decida el ruido de un borde irregular. */
    private const val MIN_GAIN = 0.008
    const val MIN_IOU = 0.85
    private const val ELLIPSE_BIAS = 0.005
    private const val MIN_ROUND_RADIUS = 0.02

    data class Fit(val shape: RegionShape, val radius: Double, val iou: Double, val approximate: Boolean)

    /** [mask] es de [w] × [h] (la caja del hueco); `true` = píxel del hueco. */
    fun detect(mask: BooleanArray, w: Int, h: Int): Fit {
        val area = w * h
        val count = mask.count { it }
        if (area == 0 || count == 0) return Fit(RegionShape.RECT, 0.0, 0.0, true)
        val rectIou = count.toDouble() / area
        if (rectIou >= RECT_FILL) return Fit(RegionShape.RECT, 0.0, rectIou, false)

        val ellipseIou = iou(mask, w, h, count) { px, py ->
            val nx = (px + 0.5 - w / 2.0) / (w / 2.0)
            val ny = (py + 0.5 - h / 2.0) / (h / 2.0)
            nx * nx + ny * ny <= 1.0
        }
        // Las esquinas que faltan miden (4 − π) r²: de ahí sale el radio.
        val minSide = min(w, h).toDouble()
        val r = min(sqrt((area - count) / (4.0 - Math.PI)), minSide / 2.0)
        val radius = r / minSide
        val roundIou = iou(mask, w, h, count) { px, py ->
            val dx = maxOf(r - (px + 0.5), (px + 0.5) - (w - r), 0.0)
            val dy = maxOf(r - (py + 0.5), (py + 0.5) - (h - r), 0.0)
            dx * dx + dy * dy <= r * r
        }

        val useEllipse = ellipseIou >= roundIou - ELLIPSE_BIAS
        val best = if (useEllipse) ellipseIou else roundIou
        if (best < MIN_IOU || best < rectIou + MIN_GAIN || (!useEllipse && radius < MIN_ROUND_RADIUS))
            return Fit(RegionShape.RECT, 0.0, rectIou, approximate = rectIou < MIN_IOU)
        return if (useEllipse) Fit(RegionShape.ELLIPSE, 0.0, best, false)
        else Fit(RegionShape.ROUND, radius, best, false)
    }

    private inline fun iou(mask: BooleanArray, w: Int, h: Int, count: Int, inside: (Int, Int) -> Boolean): Double {
        var both = 0
        var ideal = 0
        for (py in 0 until h) for (px in 0 until w) {
            val m = inside(px, py)
            if (m) ideal++
            if (m && mask[py * w + px]) both++
        }
        val union = count + ideal - both
        return if (union == 0) 0.0 else both.toDouble() / union
    }
}
