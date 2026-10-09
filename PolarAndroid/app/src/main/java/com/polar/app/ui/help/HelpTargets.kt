package com.polar.app.ui.help

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Registro de los controles visibles del editor (id → rectángulo en la ventana). Lo comparten el recorrido
 * y el modo «?»: un control que no está en pantalla (panel cerrado, ventana estrecha) simplemente no aparece aquí.
 */
@Stable
class HelpTargets {
    private val owners = HashMap<String, LinkedHashMap<Any, Rect>>()
    val bounds = mutableStateMapOf<String, Rect>()

    fun update(id: String, owner: Any, rect: Rect) {
        owners.getOrPut(id) { LinkedHashMap() }[owner] = rect
        if (bounds[id] != rect) bounds[id] = rect
    }

    /** Dos copias del mismo control pueden convivir un instante (animación de la bandeja): sólo se borra la última. */
    fun remove(id: String, owner: Any) {
        val map = owners[id] ?: return
        map.remove(owner)
        if (map.isEmpty()) { owners.remove(id); bounds.remove(id) } else bounds[id] = map.values.last()
    }

    /** Los ids que de verdad se ven (con tamaño). */
    fun visible(): Set<String> = bounds.filterValues { it.width > 1f && it.height > 1f }.keys
}

val LocalHelpTargets = compositionLocalOf<HelpTargets?> { null }

/** Marca este control como objetivo de la guía. Sin un `HelpTargets` arriba no hace nada. */
fun Modifier.helpTarget(id: String): Modifier = composed {
    val targets = LocalHelpTargets.current
    if (targets == null) this else {
        val owner = remember { Any() }
        DisposableEffect(id, targets) { onDispose { targets.remove(id, owner) } }
        onGloballyPositioned { targets.update(id, owner, it.boundsInWindow()) }
    }
}
