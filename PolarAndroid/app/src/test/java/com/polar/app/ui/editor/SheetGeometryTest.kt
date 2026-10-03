package com.polar.app.ui.editor

import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test

class SheetGeometryTest {
    @Test
    fun oneRectPerSlotInRendererOrder() {
        assertEquals(9, SheetGeometry.slotRects(PrintSettings()).size)
        val film = PrintSettings(style = TemplateStyle.FILM_VERTICAL, columns = 2, rows = 1)
        assertEquals(10, SheetGeometry.slotRects(film).size)
        val tpl = ImportedTemplate("t.png", 100, 100, listOf(TemplateRegion(x = 0.1, y = 0.1, width = 0.3, height = 0.3), TemplateRegion(x = 0.5, y = 0.5, width = 0.3, height = 0.3)))
        assertEquals(2, SheetGeometry.slotRects(PrintSettings(style = TemplateStyle.IMPORTED, columns = 1, rows = 1, importedTemplate = tpl)).size)
    }

    @Test
    fun hitTestFindsSlotOrNothing() {
        val rects = SheetGeometry.slotRects(PrintSettings())
        val r = rects[4]
        assertEquals(4, SheetGeometry.slotAt(rects, r.midX, r.midY))
        assertNull(SheetGeometry.slotAt(rects, 1.0, 1.0)) // margen de la hoja
    }

    @Test
    fun tapOnCardCaptionSelectsThatCardsFirstSlot() {
        val settings = PrintSettings()
        val cards = PolarRenderer.calculateCardRects(settings)
        val card = cards[4]
        val x = card.midX
        val y = card.bottom - 2.0 // pie de la tarjeta, fuera de la foto
        val rects = SheetGeometry.slotRects(settings)
        assertNull(SheetGeometry.slotAt(rects, x, y))
        assertEquals(4, SheetGeometry.cardSlotAt(settings, x, y))
        assertNull(SheetGeometry.cardSlotAt(settings, 1.0, 1.0))
    }
}
