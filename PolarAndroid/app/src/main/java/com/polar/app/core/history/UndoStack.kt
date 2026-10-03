package com.polar.app.core.history

/** Historial con transacciones: un campo de texto o un slider cuentan como un solo paso. */
class UndoStack<T>(private val limit: Int = 50) {
    init {
        require(limit >= 0)
    }

    private val undoList = ArrayDeque<T>()
    private val redoList = ArrayDeque<T>()
    private var transactionBase: T? = null
    private var inTransaction = false

    val canUndo: Boolean get() = undoList.isNotEmpty()
    val canRedo: Boolean get() = redoList.isNotEmpty()

    /** Hay una transacción abierta (el campo de texto o el control sigue activo). */
    val isOpen: Boolean get() = inTransaction

    /** Hay una transacción abierta con cambios que aún no están en la pila (se pueden deshacer ya). */
    fun hasOpenChange(current: T): Boolean = inTransaction && transactionBase != current

    fun record(before: T) {
        if (inTransaction) { redoList.clear(); return }
        push(before)
    }

    fun beginTransaction(before: T) {
        if (inTransaction) return
        inTransaction = true
        transactionBase = before
    }

    fun endTransaction(current: T) {
        if (!inTransaction) return
        inTransaction = false
        val base = transactionBase
        transactionBase = null
        if (base != null && base != current) push(base)
    }

    fun undo(current: T): T? {
        if (inTransaction) endTransaction(current)
        val previous = undoList.removeLastOrNull() ?: return null
        redoList.addLast(current)
        return previous
    }

    fun redo(current: T): T? {
        if (inTransaction) endTransaction(current)
        val next = redoList.removeLastOrNull() ?: return null
        undoList.addLast(current)
        return next
    }

    fun clear() {
        undoList.clear(); redoList.clear(); inTransaction = false; transactionBase = null
    }

    private fun push(before: T) {
        undoList.addLast(before)
        while (undoList.size > limit) undoList.removeFirst()
        redoList.clear()
    }
}
