package com.polar.app.core.history

import org.junit.Assert.*
import org.junit.Test

class UndoStackTest {
    @Test fun editingInsideOpenGestureInvalidatesRedoImmediately() {
        val h = UndoStack<Int>()
        h.record(0)
        assertEquals(0, h.undo(1))
        assertTrue(h.canRedo)
        h.beginTransaction(0)
        h.record(0)
        assertFalse(h.canRedo)
        h.endTransaction(2)
        assertEquals(0, h.undo(2))
    }

    @Test
    fun recordUndoRedo() {
        val s = UndoStack<String>()
        s.record("a")            // estado previo a convertirlo en "b"
        assertTrue(s.canUndo)
        assertEquals("a", s.undo("b"))
        assertTrue(s.canRedo)
        assertEquals("b", s.redo("a"))
    }

    @Test
    fun newChangeClearsRedo() {
        val s = UndoStack<String>()
        s.record("a"); s.undo("b")
        assertTrue(s.canRedo)
        s.record("a")
        assertFalse(s.canRedo)
    }

    @Test
    fun transactionGroupsManyEditsInOneStep() {
        val s = UndoStack<String>()
        s.beginTransaction("")
        s.record("L"); s.record("Lu")   // se ignoran dentro de la transacción
        s.endTransaction("Lu y Max")
        assertEquals("", s.undo("Lu y Max"))
        assertFalse(s.canUndo)
    }

    @Test
    fun transactionWithoutChangeAddsNothing() {
        val s = UndoStack<String>()
        s.beginTransaction("x"); s.endTransaction("x")
        assertFalse(s.canUndo)
    }

    @Test
    fun respectsLimit() {
        val s = UndoStack<Int>(limit = 3)
        (1..5).forEach { s.record(it) }
        assertEquals(5, s.undo(6)); assertEquals(4, s.undo(5)); assertEquals(3, s.undo(4))
        assertNull(s.undo(3))
    }

    @Test
    fun undoDuringOpenTransactionRevertsWholeEdit() {
        val s = UndoStack<String>()
        s.record("X")
        s.beginTransaction("a")
        val r = s.undo("ab")
        assertEquals("a", r)
        s.endTransaction("a")
        assertEquals("X", s.undo("a"))
    }

    @Test
    fun clearResetsOpenTransaction() {
        val s = UndoStack<String>()
        s.beginTransaction("a")
        s.clear()
        s.endTransaction("b")
        assertFalse(s.canUndo)
    }

    @Test
    fun endTransactionClearsRedo() {
        val s = UndoStack<String>()
        s.record("a")
        s.undo("b")
        assertTrue(s.canRedo)
        s.beginTransaction("a")
        s.endTransaction("c")
        assertFalse(s.canRedo)
    }

    @Test
    fun limitAppliesToTransactions() {
        val s = UndoStack<Int>(limit = 2)
        s.beginTransaction(1); s.endTransaction(2)
        s.beginTransaction(2); s.endTransaction(3)
        s.beginTransaction(3); s.endTransaction(4)
        assertEquals(3, s.undo(5))
        assertEquals(2, s.undo(4))
        assertNull(s.undo(3))
    }

    @Test
    fun openChangeIsReportedOnlyWhileTransactionDiffers() {
        val s = UndoStack<String>()
        assertFalse(s.hasOpenChange("a"))
        s.beginTransaction("a")
        assertFalse(s.hasOpenChange("a"))
        assertTrue(s.hasOpenChange("b"))
        s.endTransaction("b")
        assertFalse(s.hasOpenChange("b"))
    }

    @Test
    fun redoOnEmptyReturnsNull() {
        assertNull(UndoStack<String>().redo("x"))
    }
}
