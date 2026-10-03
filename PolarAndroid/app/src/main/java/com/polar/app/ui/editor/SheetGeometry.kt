package com.polar.app.ui.editor

import com.polar.app.engine.PolarRect
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.PrintSettings
import com.polar.app.model.TemplateStyle

/** Rectángulos (en puntos) de cada espacio de una hoja, en el mismo orden que usa el motor. */
object SheetGeometry {
    fun slotRects(settings: PrintSettings): List<PolarRect> {
        val cards = PolarRenderer.calculateCardRects(settings)
        if (settings.style == TemplateStyle.IMPORTED) return cards
        return cards.flatMap { PolarRenderer.calculatePhotoRects(it, settings.style, settings) }
    }

    fun slotAt(rects: List<PolarRect>, xPt: Double, yPt: Double): Int? =
        rects.indexOfFirst { xPt in it.left..it.right && yPt in it.top..it.bottom }.takeIf { it >= 0 }

    /** Si el punto cae en una tarjeta pero no en una foto (pie, margen), devuelve el primer espacio de esa tarjeta. */
    fun cardSlotAt(settings: PrintSettings, xPt: Double, yPt: Double): Int? {
        val cards = PolarRenderer.calculateCardRects(settings)
        if (settings.style == TemplateStyle.IMPORTED) return slotAt(cards, xPt, yPt)
        var offset = 0
        for (card in cards) {
            val count = PolarRenderer.calculatePhotoRects(card, settings.style, settings).size
            if (count > 0 && xPt in card.left..card.right && yPt in card.top..card.bottom) return offset
            offset += count
        }
        return null
    }
}
