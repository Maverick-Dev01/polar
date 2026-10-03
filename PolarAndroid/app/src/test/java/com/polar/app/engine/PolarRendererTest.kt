package com.polar.app.engine

import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test

class PolarRendererTest {

    @Test
    fun testCardRectsCountMatchesGrid() {
        val settings = PrintSettings(
            style = TemplateStyle.POLAROID,
            columns = 3,
            rows = 3,
            margin = 24.0,
            gap = 12.0
        )
        val cards = PolarRenderer.calculateCardRects(settings)
        assertEquals(9, cards.size)

        // Cards must be inside the paper bounds
        val paper = PolarRenderer.paperRect(settings)
        for (card in cards) {
            assertTrue(card.left >= settings.margin - 0.001)
            assertTrue(card.top >= settings.margin - 0.001)
            assertTrue(card.right <= paper.width - settings.margin + 0.001)
            assertTrue(card.bottom <= paper.height - settings.margin + 0.001)
            assertTrue(card.width > 0)
            assertTrue(card.height > 0)
        }
    }

    @Test
    fun testFilmStyleHas5PhotoRectsPerCard() {
        val settings = PrintSettings(style = TemplateStyle.FILM_VERTICAL)
        val cards = PolarRenderer.calculateCardRects(settings)
        assertFalse(cards.isEmpty())

        val photoRects = PolarRenderer.calculatePhotoRects(cards.first(), TemplateStyle.FILM_VERTICAL, settings)
        assertEquals(5, photoRects.size)

        // Ensure 5 slots are vertically stacked inside card
        for (i in 0 until 4) {
            assertTrue(photoRects[i].bottom <= photoRects[i + 1].top + 0.001)
        }
    }

    @Test
    fun testPhotoRectsStayWithinCardBounds() {
        for (style in TemplateStyle.entries) {
            if (style == TemplateStyle.IMPORTED) continue
            val settings = PrintSettings(style = style)
            val cards = PolarRenderer.calculateCardRects(settings)
            assertFalse(cards.isEmpty())
            val card = cards.first()
            val photos = PolarRenderer.calculatePhotoRects(card, style, settings)
            assertEquals(style.photosPerCard, photos.size)
            for (p in photos) {
                assertTrue(p.left >= card.left - 0.001)
                assertTrue(p.top >= card.top - 0.001)
                assertTrue(p.right <= card.right + 0.001)
                assertTrue(p.bottom <= card.bottom + 0.001)
            }
        }
    }
}
