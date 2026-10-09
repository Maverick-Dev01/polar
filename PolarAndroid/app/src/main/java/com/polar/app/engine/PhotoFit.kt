package com.polar.app.engine

import com.polar.app.model.PhotoPlacement
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

/** Por debajo de estos puntos por pulgada una foto se avisa como de baja resolución. */
const val LOW_RES_DPI = 150.0

/** Entre LOW_RES_DPI y este valor la foto es aceptable pero no ideal. */
const val FAIR_DPI = 220.0

enum class PhotoQuality { LOW, FAIR, GOOD }

/** Nivel de calidad de impresión por puntos por pulgada efectivos. */
fun photoQuality(dpi: Double): PhotoQuality = when {
    dpi < LOW_RES_DPI -> PhotoQuality.LOW
    dpi < FAIR_DPI -> PhotoQuality.FAIR
    else -> PhotoQuality.GOOD
}

/** Foto ya colocada: centro relativo a la esquina superior izquierda del hueco, tamaño del bitmap sin girar y giro. */
data class PhotoFitResult(val centerX: Double, val centerY: Double, val width: Double, val height: Double, val degrees: Float)

/** Único cálculo de encuadre: lo usan la impresión (PolarRenderer) y la vista previa de Encuadrar. */
object PhotoFit {
    /** El hueco puede estar en cualquier unidad; el resultado queda en la misma. */
    fun compute(boxW: Double, boxH: Double, bmpW: Int, bmpH: Int, p: PhotoPlacement): PhotoFitResult {
        val rotated = p.quarterTurns % 2 != 0
        val srcW = if (rotated) bmpH.toDouble() else bmpW.toDouble()
        val srcH = if (rotated) bmpW.toDouble() else bmpH.toDouble()
        val fit = max(boxW / srcW, boxH / srcH) * p.zoom
        val drawnW = srcW * fit
        val drawnH = srcH * fit
        val cx = boxW / 2.0 + p.offsetX * (drawnW - boxW) / 2.0
        val cy = boxH / 2.0 + p.offsetY * (drawnH - boxH) / 2.0
        return PhotoFitResult(cx, cy, bmpW * fit, bmpH * fit, (p.quarterTurns * 90).toFloat())
    }
    fun fitZoom(boxW: Double, boxH: Double, bmpW: Int, bmpH: Int, quarterTurns: Int): Double {
        val w = if(quarterTurns%2!=0) bmpH.toDouble() else bmpW.toDouble()
        val h = if(quarterTurns%2!=0) bmpW.toDouble() else bmpH.toDouble()
        return min(boxW/w,boxH/h)/max(boxW/w,boxH/h)
    }
    fun moved(p: PhotoPlacement, dx: Double, dy: Double, boxW: Double, boxH: Double, bmpW: Int, bmpH: Int): PhotoPlacement {
        val fit=compute(boxW,boxH,bmpW,bmpH,p)
        val rotated=p.quarterTurns%2!=0
        val w=if(rotated) fit.height else fit.width; val h=if(rotated) fit.width else fit.height
        return p.copy(offsetX=if(abs(w-boxW)>.001) (p.offsetX+dx*2/(w-boxW)).coerceIn(-1.0,1.0) else p.offsetX,
            offsetY=if(abs(h-boxH)>.001) (p.offsetY+dy*2/(h-boxH)).coerceIn(-1.0,1.0) else p.offsetY)
    }

}
