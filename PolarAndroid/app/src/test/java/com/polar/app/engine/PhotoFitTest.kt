package com.polar.app.engine

import com.polar.app.model.PhotoPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoFitTest {
    @Test
    fun coversBoxAtZoomOne() {
        val d = PhotoFit.compute(100.0, 100.0, 400, 200, PhotoPlacement("x"))
        assertEquals(50.0, d.centerX, 0.01); assertEquals(50.0, d.centerY, 0.01)
        assertEquals(200.0, d.width, 0.01); assertEquals(100.0, d.height, 0.01) // cubre el alto, sobra a los lados
    }

    @Test
    fun offsetMovesWithinOverflowAndZoomScales() {
        val d = PhotoFit.compute(100.0, 100.0, 400, 200, PhotoPlacement("x", zoom = 2.0, offsetX = 1.0))
        assertEquals(400.0, d.width, 0.01)
        assertEquals(50.0 + (400.0 - 100.0) / 2.0, d.centerX, 0.01)
    }

    @Test
    fun negativeVerticalOffsetMovesUpWithinOverflow() {
        // 200×400 en un hueco 100×100: se dibuja de 100×200, sobran 100 en vertical
        val d = PhotoFit.compute(100.0, 100.0, 200, 400, PhotoPlacement("x", offsetY = -1.0))
        assertEquals(100.0, d.width, 0.01); assertEquals(200.0, d.height, 0.01)
        assertEquals(50.0, d.centerX, 0.01)
        assertEquals(50.0 - (200.0 - 100.0) / 2.0, d.centerY, 0.01)
    }

    @Test
    fun quarterTurnSwapsSides() {
        val d = PhotoFit.compute(100.0, 200.0, 400, 200, PhotoPlacement("x", quarterTurns = 1))
        assertEquals(90f, d.degrees, 0f)
        // girada, la foto mide 200×400 y cubre 100×200 con escala 0.5: el bitmap se dibuja de 200×100
        assertEquals(200.0, d.width, 0.01); assertEquals(100.0, d.height, 0.01)
    }

    @Test
    fun halfTurnKeepsSidesAndThreeQuartersSwapsThem() {
        val half = PhotoFit.compute(100.0, 100.0, 400, 200, PhotoPlacement("x", quarterTurns = 2))
        assertEquals(180f, half.degrees, 0f)
        assertEquals(200.0, half.width, 0.01); assertEquals(100.0, half.height, 0.01)
        val three = PhotoFit.compute(100.0, 200.0, 400, 200, PhotoPlacement("x", quarterTurns = 3))
        assertEquals(270f, three.degrees, 0f)
        assertEquals(200.0, three.width, 0.01); assertEquals(100.0, three.height, 0.01)
    }

    @Test
    fun rotationCombinedWithOffsetUsesRotatedOverflow() {
        // 400×200 girada 90° pasa a 200×400; en un hueco 100×200 se escala 0.5 → 100×200, no sobra nada;
        // con zoom 2 se dibuja a 200×400: sobran 100 en X y 200 en Y.
        val d = PhotoFit.compute(100.0, 200.0, 400, 200, PhotoPlacement("x", zoom = 2.0, offsetX = 1.0, offsetY = -0.5, quarterTurns = 1))
        assertEquals(50.0 + 100.0 / 2.0, d.centerX, 0.01)
        assertEquals(100.0 - 0.5 * 200.0 / 2.0, d.centerY, 0.01)
        assertEquals(400.0, d.width, 0.01); assertEquals(200.0, d.height, 0.01)
    }    @Test fun fitCanShowTheWholePhotoAndDraggingUsesTheSameRotatedGeometry() {
        val zoom=PhotoFit.fitZoom(100.0,100.0,400,200,0)
        assertEquals(.5,zoom,0.0)
        val fitted=PhotoFit.compute(100.0,100.0,400,200,PhotoPlacement("x",zoom=zoom))
        assertEquals(100.0,fitted.width,0.0);assertEquals(50.0,fitted.height,0.0)
        val p=PhotoPlacement("x",zoom=2.0,quarterTurns=1)
        val moved=PhotoFit.moved(p,10.0,-20.0,100.0,200.0,400,200)
        val before=PhotoFit.compute(100.0,200.0,400,200,p)
        val after=PhotoFit.compute(100.0,200.0,400,200,moved)
        assertEquals(before.centerX+10,after.centerX,.001);assertEquals(before.centerY-20,after.centerY,.001)
    }

}
