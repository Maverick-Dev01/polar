package com.polar.app.help

import org.junit.Assert.*
import org.junit.Test

class TourFlowTest {
    private val steps = listOf("a", "b", "c", "d", "e").map { TourStep(it, "t.$it", "tap", it, it) }

    @Test fun startsAtTheFirstAvailableStep() {
        assertEquals(0, TourFlow.first(steps, setOf("t.a", "t.b")))
        assertEquals(1, TourFlow.first(steps, setOf("t.b", "t.c")))
    }

    @Test fun nextSkipsStepsWhoseTargetIsNotVisible() {
        val visible = setOf("t.a", "t.c", "t.e") // b y d no se ven (panel cerrado, ventana estrecha…)
        assertEquals(2, TourFlow.next(steps, 0, visible))
        assertEquals(4, TourFlow.next(steps, 2, visible))
        assertNull("Tras el último paso termina, no se queda atascado", TourFlow.next(steps, 4, visible))
    }

    @Test fun finishesWhenNothingIsVisible() {
        assertNull(TourFlow.first(steps, emptySet()))
        assertNull(TourFlow.next(steps, 0, emptySet()))
    }

    @Test fun positionCountsOnlyTheVisibleSteps() {
        val visible = setOf("t.a", "t.c", "t.e")
        assertEquals(1 to 3, TourFlow.position(steps, 0, visible))
        assertEquals(2 to 3, TourFlow.position(steps, 2, visible))
        assertEquals(3 to 3, TourFlow.position(steps, 4, visible))
    }

    @Test fun shouldAutoStartOnlyWhenNotSeenAndEditorIsReady() {
        assertTrue(TourFlow.shouldAutoStart(seen = false, loading = false, editing = true))
        assertFalse(TourFlow.shouldAutoStart(seen = true, loading = false, editing = true))
        assertFalse("Aún no se sabe si ya se vio", TourFlow.shouldAutoStart(seen = null, loading = false, editing = true))
        assertFalse(TourFlow.shouldAutoStart(seen = false, loading = true, editing = true))
        assertFalse(TourFlow.shouldAutoStart(seen = false, loading = false, editing = false))
    }
}
