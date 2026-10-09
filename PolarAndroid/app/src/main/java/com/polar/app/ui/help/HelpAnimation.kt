package com.polar.app.ui.help

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.polar.app.ui.rememberReduceMotion
import kotlin.math.floor
import kotlin.math.min

private class Palette(val ink: Color, val accent: Color, val paper: Color, val soft: Color, val photoA: Color, val photoB: Color, val sun: Color)

/**
 * Mini ilustración animada de un artículo o paso. Dibujada a mano con Canvas: sin dependencias nuevas.
 * `t` va de 0 a 1 en bucle; con «reducir movimiento» se queda en `t = 1`, el cuadro final completo y estático.
 * Es decorativa (el texto ya dice todo), por eso TalkBack la omite.
 */
@Composable
fun HelpAnimation(id: String, modifier: Modifier = Modifier, reduceMotion: Boolean = rememberReduceMotion()) {
    val t = if (reduceMotion) 1f else {
        val transition = rememberInfiniteTransition(label = "ayuda")
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "t")
        value
    }
    val scheme = MaterialTheme.colorScheme
    val p = Palette(scheme.onSurface, scheme.primary, scheme.surface, scheme.surfaceVariant, Color(0xFFE8A87C), Color(0xFF7FB3A6), Color(0xFFF4D35E))
    Canvas(modifier.fillMaxWidth().height(104.dp).clearAndSetSemantics { }) {
        // Si se pierde el movimiento el cuadro final debe verse completo: las escenas usan `final` para ello.
        scene(id, t, p)
    }
}

private fun seg(t: Float, from: Float, to: Float): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)
private fun ease(x: Float): Float = x * x * (3 - 2 * x)

private fun DrawScope.scene(id: String, t: Float, p: Palette) {
    val w = size.width; val h = size.height
    val cx = w / 2; val cy = h / 2
    // Cada escena se dibuja centrada en un área de 200 × 100 escalada al lienzo.
    val s = min(w / 200f, h / 100f)
    fun x(v: Float) = cx + (v - 100f) * s
    fun y(v: Float) = cy + (v - 50f) * s
    fun rect(l: Float, tp: Float, r: Float, b: Float, color: Color, radius: Float = 4f) =
        drawRoundRect(color, Offset(x(l), y(tp)), Size((r - l) * s, (b - tp) * s), CornerRadius(radius * s))
    fun frame(l: Float, tp: Float, r: Float, b: Float, color: Color, stroke: Float = 2f, radius: Float = 4f) =
        drawRoundRect(color, Offset(x(l), y(tp)), Size((r - l) * s, (b - tp) * s), CornerRadius(radius * s), style = Stroke(stroke * s))
    fun photo(l: Float, tp: Float, r: Float, b: Float, gray: Boolean = false) {
        val bg = if (gray) Color(0xFFB8B8B8) else p.photoB
        val sun = if (gray) Color(0xFFEDEDED) else p.sun
        val hill = if (gray) Color(0xFF707070) else p.photoA
        rect(l, tp, r, b, bg, 3f)
        drawCircle(sun, (r - l) * 0.11f * s, Offset(x(l + (r - l) * 0.72f), y(tp + (b - tp) * 0.3f)))
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(x(l), y(b)); lineTo(x(l), y(tp + (b - tp) * 0.7f)); lineTo(x(l + (r - l) * 0.35f), y(tp + (b - tp) * 0.45f))
            lineTo(x(l + (r - l) * 0.62f), y(tp + (b - tp) * 0.68f)); lineTo(x(r), y(tp + (b - tp) * 0.55f)); lineTo(x(r), y(b)); close()
        }
        drawPath(path, hill)
    }
    fun card(l: Float, tp: Float, r: Float, b: Float, gray: Boolean = false) {
        rect(l, tp, r, b, p.paper, 4f); frame(l, tp, r, b, p.soft, 1.5f)
        photo(l + 5, tp + 5, r - 5, b - (b - tp) * 0.28f, gray)
    }
    fun lines(l: Float, tp: Float, widths: List<Float>, done: Float) {
        widths.forEachIndexed { i, wd -> rect(l, tp + i * 7f, l + wd * done, tp + i * 7f + 3.5f, p.ink.copy(alpha = 0.55f), 2f) }
    }
    fun pointer(px: Float, py: Float, press: Float) {
        drawCircle(p.accent.copy(alpha = 0.25f * press), (10f + 10f * press) * s, Offset(x(px), y(py)))
        drawCircle(p.accent, 6f * s, Offset(x(px), y(py)))
        drawCircle(p.paper, 2.2f * s, Offset(x(px), y(py)))
    }
    fun qr(l: Float, tp: Float, side: Float, shown: Float) {
        rect(l, tp, l + side, tp + side, Color.White, 2f)
        val n = 7; val cell = (side - 4f) / n
        for (i in 0 until n) for (j in 0 until n) {
            val on = ((i * 5 + j * 3 + i * j) % 3 != 0) || (i < 2 && j < 2) || (i > 4 && j < 2) || (i < 2 && j > 4)
            if (on && (i * n + j) < shown * n * n) rect(l + 2 + i * cell, tp + 2 + j * cell, l + 2 + (i + 1) * cell - 0.4f, tp + 2 + (j + 1) * cell - 0.4f, Color(0xFF222222), 0.4f)
        }
    }

    when (id) {
        "tap" -> {
            card(60f, 8f, 140f, 92f)
            val a = ease(seg(t, 0f, 0.5f)); val press = if (t < 0.5f) 0f else 1f - seg(t, 0.5f, 0.95f)
            val highlight = if (t >= 0.5f) 1f else 0f
            if (highlight > 0f) frame(56f, 4f, 144f, 96f, p.accent, 3f, 6f)
            pointer(190f + (110f - 190f) * a, 105f + (62f - 105f) * a, press)
        }
        "swipe" -> {
            val m = ease(seg(t, 0.15f, 0.8f))
            card(60f - 100f * m, 8f, 140f - 100f * m, 92f)
            card(160f - 100f * m, 8f, 240f - 100f * m, 92f, gray = false)
            pointer(130f - 70f * m, 70f, if (m in 0.01f..0.99f) 1f else 0f)
        }
        "fill" -> {
            rect(40f, 6f, 160f, 94f, p.soft, 5f)
            for (i in 0 until 6) {
                val l = 48f + (i % 3) * 37f; val tp = 12f + (i / 3) * 40f
                val a = ease(seg(t, 0.1f + i * 0.12f, 0.25f + i * 0.12f))
                rect(l, tp, l + 32f, tp + 36f, p.paper, 3f)
                if (a > 0f) {
                    val inset = 16f * (1 - a)
                    drawIntoPhoto(l + 2f + inset, tp + 2f + inset, l + 30f - inset, tp + 34f - inset, i, p, ::x, ::y, s)
                } else frame(l, tp, l + 32f, tp + 36f, p.ink.copy(alpha = 0.25f), 1.5f, 3f)
            }
        }
        "crop-ring" -> {
            photo(30f, 8f, 170f, 92f)
            val m = ease(if (t < 0.5f) seg(t, 0f, 0.5f) else 1f)
            val l = 40f + 30f * m; val tp = 18f + 6f * m; val r = 150f - 10f * m; val b = 90f - 14f * m
            // Lo de afuera se atenúa; el encuadre queda.
            rect(30f, 8f, 170f, tp, p.paper.copy(alpha = 0.6f), 0f); rect(30f, b, 170f, 92f, p.paper.copy(alpha = 0.6f), 0f)
            rect(30f, tp, l, b, p.paper.copy(alpha = 0.6f), 0f); rect(r, tp, 170f, b, p.paper.copy(alpha = 0.6f), 0f)
            frame(l, tp, r, b, p.accent, 2.5f, 2f)
            listOf(l to tp, r to tp, l to b, r to b).forEach { (hx, hy) -> drawCircle(p.accent, 3.5f * s, Offset(x(hx), y(hy))) }
        }
        "filter-swap" -> {
            photo(35f, 8f, 165f, 92f, gray = true)
            val m = ease(if (t < 0.7f) seg(t, 0.1f, 0.7f) else 1f)
            val cut = 35f + 130f * m
            // Mitad a color a la izquierda de la barra (al final, toda la foto en color).
            clipRect(x(35f), y(8f), x(cut), y(92f)) { photo(35f, 8f, 165f, 92f, gray = false) }
            if (m < 1f) { rect(cut - 1f, 4f, cut + 1f, 96f, p.paper, 1f); drawCircle(p.paper, 6f * s, Offset(x(cut), y(50f))); drawCircle(p.accent, 3f * s, Offset(x(cut), y(50f))) }
        }
        "text-type" -> {
            card(55f, 4f, 145f, 96f)
            val n = floor(ease(seg(t, 0.1f, 0.85f)) * 14f).toInt()
            val total = 14f
            lines(63f, 74f, listOf(74f, 46f), n / total)
            if (t < 1f && n < 14) rect(63f + 74f * n / total + 2f, 74f, 63f + 74f * n / total + 4.5f, 77.5f, p.accent, 1f)
        }
        "print" -> {
            rect(55f, 8f, 145f, 36f, p.soft, 6f); rect(75f, 28f, 125f, 33f, p.ink.copy(alpha = 0.6f), 2f)
            val m = ease(seg(t, 0.05f, 0.8f))
            val top = 30f + 8f * m
            rect(72f, top, 128f, top + 62f * m.coerceAtLeast(0.05f), p.paper, 2f)
            frame(72f, top, 128f, top + 62f * m.coerceAtLeast(0.05f), p.soft, 1.5f, 2f)
            if (m > 0.3f) photo(78f, top + 6f, 122f, top + 6f + 30f * m, false)
            if (t >= 0.8f || m >= 1f) { rect(143f, 60f, 190f, 84f, p.accent, 8f); drawLabel100(x(166f), y(72f), s) }
        }
        "qr" -> {
            card(60f, 6f, 140f, 94f)
            val m = seg(t, 0.15f, 0.8f)
            qr(100f, 56f, 34f, m)
            if (m in 0.01f..0.99f) frame(98f, 54f, 136f, 92f, p.accent, 2f, 3f)
        }
        "mold-detect" -> {
            rect(45f, 4f, 155f, 96f, p.ink.copy(alpha = 0.78f), 6f)
            val holes = listOf(Triple(53f, 12f, 83f), Triple(53f, 52f, 83f), Triple(103f, 12f, 83f), Triple(103f, 52f, 83f))
            holes.forEachIndexed { i, (l, tp, _) ->
                rect(l, tp, l + 44f, tp + 34f, Color.White, 3f)
                val a = ease(seg(t, 0.1f + i * 0.15f, 0.3f + i * 0.15f))
                if (a > 0f) frame(l - 2f * (1 - a), tp - 2f * (1 - a), l + 44f + 2f * (1 - a), tp + 34f + 2f * (1 - a), p.accent, 2.5f, 3f)
            }
        }
        "bg-remove" -> {
            val m = ease(seg(t, 0.15f, 0.8f))
            rect(45f, 6f, 155f, 94f, p.photoB, 5f)
            // Tablero de transparencia que aparece cuando el fondo se va.
            if (m > 0f) for (i in 0 until 11) for (j in 0 until 9) {
                val c = if ((i + j) % 2 == 0) Color(0xFFDDDDDD) else Color(0xFFF6F6F6)
                rect(45f + i * 10f, 6f + j * 9.8f, 45f + (i + 1) * 10f, 6f + (j + 1) * 9.8f, c.copy(alpha = m), 0f)
            }
            drawCircle(p.photoA, 14f * s, Offset(x(100f), y(40f)))
            rect(78f, 56f, 122f, 94f, p.photoA, 14f)
        }
        else -> card(60f, 8f, 140f, 92f)
    }
}

private fun DrawScope.drawIntoPhoto(l: Float, tp: Float, r: Float, b: Float, i: Int, p: Palette, x: (Float) -> Float, y: (Float) -> Float, s: Float) {
    val color = if (i % 2 == 0) p.photoB else p.photoA
    drawRoundRect(color, Offset(x(l), y(tp)), Size((r - l) * s, (b - tp) * s), CornerRadius(2f * s))
    drawCircle(p.sun, (r - l) * 0.15f * s, Offset(x(l + (r - l) * 0.7f), y(tp + (b - tp) * 0.3f)))
}

private fun DrawScope.drawLabel100(cx: Float, cy: Float, s: Float) {
    // «100»: tres dígitos hechos con trazos, para no depender de una tipografía dentro del Canvas.
    val u = 3f * s; val stroke = Stroke(1.8f * s)
    val white = Color.White
    drawLine(white, Offset(cx - 11f * s, cy - u), Offset(cx - 11f * s, cy + u), 1.8f * s)
    drawRoundRect(white, Offset(cx - 5f * s, cy - u), Size(5f * s, 2 * u), CornerRadius(2.5f * s), style = stroke)
    drawRoundRect(white, Offset(cx + 3f * s, cy - u), Size(5f * s, 2 * u), CornerRadius(2.5f * s), style = stroke)
}
