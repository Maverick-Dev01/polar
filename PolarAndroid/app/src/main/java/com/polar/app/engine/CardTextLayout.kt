package com.polar.app.engine

import com.polar.app.core.text.TextResolver
import com.polar.app.model.*
import java.util.Locale
import java.util.TimeZone

enum class TextAlign { LEFT, CENTER, RIGHT }

data class TextItem(
    val role: TextRole?,
    val text: String,
    val rect: PolarRect,
    val sizePt: Double,
    val defaultColor: Int,
    val defaultBold: Boolean,
    val align: TextAlign,
    val appearance: TextAppearance?,
    /** Fuente propia del diseño; se usa mientras la tarjeta no elija otra. */
    val defaultFont: String = FontNames.SYSTEM
)

/** Qué textos lleva cada tarjeta y dónde. Puro Kotlin: se prueba sin Canvas. */
object CardTextLayout {
    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val DARK = 0xFF444444.toInt()
    private const val GRAY = 0xFF888888.toInt()
    private const val STAMP = 0xFFFF8C2E.toInt() // naranja de fechador de cámara

    fun items(
        project: PolarProject,
        card: PolarRect,
        cardIndex: Int,
        photoSlots: List<PolarRect>,
        accent: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): List<TextItem> {
        val style = project.settingsForCard(cardIndex).style
        val fontSize = card.width * 0.06
        val firstSlot = project.firstSlotOfCard(cardIndex)
        val out = mutableListOf<TextItem>()

        fun r(x: Double, y: Double, w: Double, h: Double) = PolarRect(
            card.minX + x * card.width, card.minY + y * card.height,
            card.minX + (x + w) * card.width, card.minY + (y + h) * card.height
        )
        fun role(role: TextRole, rect: PolarRect, size: Double, color: Int, bold: Boolean = false, align: TextAlign = TextAlign.CENTER, font: String = FontNames.SYSTEM) {
            out += TextItem(role, TextResolver.text(project, cardIndex, role, zone), rect, size, color, bold, align,
                TextResolver.appearance(project, cardIndex, role), font)
        }
        val geo = SharedGeometry.of(style)
        if (geo != null) {
            for (slot in geo.textSlots) {
                val textRole = slot.textRole ?: continue
                if (textRole == TextRole.DATE) continue // la fecha sigue la misma regla que en los demás diseños
                if (textRole !in style.textRoles) continue
                out += geoItem(project, cardIndex, card, slot, zone)
            }
        }
        fun fixed(text: String, rect: PolarRect, size: Double, color: Int, bold: Boolean = false, align: TextAlign = TextAlign.CENTER) {
            out += TextItem(null, text, rect, size, color, bold, align, null)
        }

        when (style) {
            TemplateStyle.POLAROID, TemplateStyle.MINI -> {
                role(TextRole.TITLE, r(0.07, 0.82, 0.86, 0.075), fontSize, accent, true)
                role(TextRole.SUBTITLE, r(0.07, 0.905, 0.86, 0.05), fontSize * 0.66, DARK)
            }
            TemplateStyle.SPOTIFY -> {
                role(TextRole.SONG, r(0.065, 0.686, 0.78, 0.065), fontSize * 1.12, BLACK, true, TextAlign.LEFT)
                role(TextRole.ARTIST, r(0.065, 0.757, 0.78, 0.045), fontSize * 0.78, GRAY, false, TextAlign.LEFT)
            }
            TemplateStyle.PLAYER_RED -> {
                role(TextRole.SONG, r(0.07, 0.634, 0.80, 0.055), fontSize * 0.83, WHITE, true, TextAlign.LEFT)
                role(TextRole.ARTIST, r(0.07, 0.693, 0.80, 0.04), fontSize * 0.62, WHITE, false, TextAlign.LEFT)
            }
            TemplateStyle.PLAYER_GRAY -> {
                role(TextRole.SONG, r(0.56, 0.16, 0.38, 0.11), card.height * 0.064, WHITE, true, TextAlign.LEFT)
                role(TextRole.ARTIST, r(0.56, 0.30, 0.38, 0.09), card.height * 0.05, WHITE, false, TextAlign.LEFT)
            }
            TemplateStyle.TICKET -> {
                role(TextRole.TITLE, r(0.06, 0.79, 0.64, 0.095), card.height * 0.075, accent, true)
                role(TextRole.SUBTITLE, r(0.06, 0.895, 0.64, 0.07), card.height * 0.05, DARK)
                fixed("POLAR", r(0.76, 0.14, 0.22, 0.12), card.height * 0.10, WHITE, true)
                fixed(String.format(Locale.ROOT, "Nº %03d", firstSlot + 1), r(0.76, 0.65, 0.22, 0.12), card.height * 0.075, WHITE, true)
            }
            TemplateStyle.INSTAGRAM -> {
                fixed("Instagram", r(0.16, 0.026, 0.53, 0.06), fontSize * 0.88, BLACK, true, TextAlign.LEFT)
                role(TextRole.TITLE, r(0.17, 0.116, 0.69, 0.044), fontSize * 0.7, BLACK, true, TextAlign.LEFT)
                fixed("⋮", r(0.89, 0.106, 0.06, 0.065), fontSize, BLACK, true)
                role(TextRole.CAPTION, r(0.065, 0.863, 0.87, 0.04), fontSize * 0.63, BLACK, false, TextAlign.LEFT)
            }
            TemplateStyle.CUSTOM -> {
                role(TextRole.TITLE, r(0.06, 0.81, 0.88, 0.08), fontSize * 1.15, accent, true)
                role(TextRole.SUBTITLE, r(0.06, 0.91, 0.88, 0.045), fontSize * 0.75, DARK)
            }
            TemplateStyle.SQUARE -> {
                role(TextRole.TITLE, r(0.06, 0.845, 0.88, 0.075), fontSize, accent, true)
                role(TextRole.SUBTITLE, r(0.06, 0.93, 0.88, 0.045), fontSize * 0.66, DARK)
            }
            TemplateStyle.POSTCARD -> {
                role(TextRole.TITLE, r(0.70, 0.23, 0.26, 0.19), card.height * 0.09, accent, true)
                role(TextRole.SUBTITLE, r(0.70, 0.46, 0.26, 0.16), card.height * 0.055, DARK)
                role(TextRole.CAPTION, r(0.70, 0.69, 0.26, 0.15), card.height * 0.043, DARK)
            }
            TemplateStyle.BOTANICAL -> {
                role(TextRole.TITLE, r(0.08, 0.80, 0.84, 0.08), fontSize * 1.10, accent, true)
                role(TextRole.SUBTITLE, r(0.08, 0.90, 0.84, 0.055), fontSize * 0.70, DARK)
            }
            TemplateStyle.CELEBRATION -> {
                role(TextRole.TITLE, r(0.07, 0.78, 0.86, 0.07), fontSize * 1.1, accent, true)
                role(TextRole.CAPTION, r(0.07, 0.85, 0.86, 0.08), fontSize * 0.70, DARK)
                role(TextRole.SUBTITLE, r(0.07, 0.94, 0.86, 0.035), fontSize * 0.55, accent)
            }
            TemplateStyle.PETS -> {
                role(TextRole.TITLE, r(0.14, 0.79, 0.72, 0.08), fontSize * 1.15, accent, true)
                role(TextRole.SUBTITLE, r(0.07, 0.90, 0.86, 0.055), fontSize * 0.75, DARK)
            }
            TemplateStyle.HEART -> {
                role(TextRole.TITLE, r(0.07, 0.80, 0.86, 0.075), fontSize * 1.05, accent, true)
                role(TextRole.SUBTITLE, r(0.07, 0.905, 0.86, 0.05), fontSize * 0.72, DARK)
            }
            TemplateStyle.EDITORIAL -> {
                role(TextRole.TITLE, r(0.065, 0.045, 0.87, 0.08), fontSize * 1.5, accent, true)
                role(TextRole.SUBTITLE, r(0.065, 0.135, 0.87, 0.04), fontSize * 0.55, BLACK, false, TextAlign.LEFT)
                role(TextRole.CAPTION, r(0.065, 0.79, 0.87, 0.12), fontSize * 0.80, DARK, false, TextAlign.LEFT)
                fixed(String.format(Locale.ROOT, "POLAR / %03d", firstSlot + 1), r(0.065, 0.95, 0.87, 0.025), fontSize * 0.42, accent, false, TextAlign.RIGHT)
            }
            TemplateStyle.FILM_VERTICAL, TemplateStyle.FILM_HORIZONTAL, TemplateStyle.CALENDAR,
            TemplateStyle.BORDERLESS, TemplateStyle.IMPORTED,
            TemplateStyle.PHOTOBOOTH, TemplateStyle.INSTAX_WIDE, TemplateStyle.VINYL,
            TemplateStyle.CASSETTE, TemplateStyle.COLLAGE, TemplateStyle.WASHI -> Unit
        }

        if (geo != null) {
            val date = geo.textSlots.firstOrNull { it.textRole == TextRole.DATE }
            if (date != null && style.supportsDate && TextResolver.dateSource(project, cardIndex) != DateSource.NONE)
                out += geoItem(project, cardIndex, card, date, zone)
        } else if (style.supportsDate && TextResolver.dateSource(project, cardIndex) != DateSource.NONE) {
            dateSlot(style, ::r, fontSize, card, accent)?.let { (rect, size, color, align) ->
                role(TextRole.DATE, rect, size, color, false, align)
            }
        }
        return out
    }

    /** Texto de un diseño con geometría compartida: tamaños a 180 pt de ancho, escalados al ancho real. */
    private fun geoItem(project: PolarProject, cardIndex: Int, card: PolarRect, slot: TextSlotGeo, zone: TimeZone): TextItem {
        val role = slot.textRole!!
        return TextItem(
            role, TextResolver.text(project, cardIndex, role, zone),
            GeometryDrawing.rect(card, slot.x, slot.y, slot.w, slot.h),
            slot.defaultSizePt * card.width / SharedGeometry.referenceCardWidthPt,
            GeometryDrawing.parseColor(slot.color), slot.bold,
            when (slot.align) { "left" -> TextAlign.LEFT; "right" -> TextAlign.RIGHT; else -> TextAlign.CENTER },
            TextResolver.appearance(project, cardIndex, role), slot.defaultFont
        )
    }

    private data class DateSlot(val rect: PolarRect, val size: Double, val color: Int, val align: TextAlign)

    private fun dateSlot(
        style: TemplateStyle, r: (Double, Double, Double, Double) -> PolarRect, fontSize: Double, card: PolarRect, accent: Int
    ): DateSlot? = when (style) {
        TemplateStyle.POLAROID, TemplateStyle.MINI, TemplateStyle.PETS, TemplateStyle.HEART ->
            DateSlot(r(0.07, 0.955, 0.86, 0.035), fontSize * 0.5, GRAY, TextAlign.CENTER)
        TemplateStyle.SPOTIFY -> DateSlot(r(0.55, 0.757, 0.30, 0.045), fontSize * 0.6, GRAY, TextAlign.RIGHT)
        TemplateStyle.PLAYER_RED -> DateSlot(r(0.55, 0.693, 0.32, 0.04), fontSize * 0.55, WHITE, TextAlign.RIGHT)
        TemplateStyle.PLAYER_GRAY -> DateSlot(r(0.56, 0.86, 0.38, 0.08), card.height * 0.045, WHITE, TextAlign.LEFT)
        TemplateStyle.TICKET -> DateSlot(r(0.76, 0.40, 0.22, 0.10), card.height * 0.06, WHITE, TextAlign.CENTER)
        TemplateStyle.INSTAGRAM -> DateSlot(r(0.065, 0.91, 0.87, 0.035), fontSize * 0.5, GRAY, TextAlign.LEFT)
        TemplateStyle.CUSTOM -> DateSlot(r(0.06, 0.955, 0.88, 0.035), fontSize * 0.5, GRAY, TextAlign.CENTER)
        TemplateStyle.SQUARE -> DateSlot(r(0.06, 0.975, 0.88, 0.022), fontSize * 0.42, GRAY, TextAlign.CENTER)
        TemplateStyle.POSTCARD -> DateSlot(r(0.70, 0.86, 0.26, 0.07), card.height * 0.04, GRAY, TextAlign.CENTER)
        TemplateStyle.BOTANICAL -> DateSlot(r(0.08, 0.955, 0.84, 0.035), fontSize * 0.5, GRAY, TextAlign.CENTER)
        TemplateStyle.CELEBRATION -> DateSlot(r(0.07, 0.07, 0.86, 0.05), fontSize * 0.55, accent, TextAlign.CENTER)
        TemplateStyle.EDITORIAL -> DateSlot(r(0.065, 0.95, 0.40, 0.025), fontSize * 0.42, accent, TextAlign.LEFT)
        TemplateStyle.BORDERLESS -> DateSlot(r(0.40, 0.90, 0.56, 0.06), card.height * 0.045, STAMP, TextAlign.RIGHT)
        else -> null
    }
}
