package com.polar.app.ui.importer

import com.polar.app.model.RegionShape
import com.polar.app.model.TemplateRegion
import com.polar.app.template.TemplateLoad
import com.polar.app.template.TemplateMatch
import java.io.File

/** Pasos del asistente. [DUPLICATE] sólo aparece entre PICK y REVIEW si hay un molde parecido. */
enum class WizardStep { PICK, DUPLICATE, REVIEW, NAME }

enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

data class ImportWizardState(
    val step: WizardStep = WizardStep.PICK,
    val file: File? = null,
    val load: TemplateLoad? = null,
    val regions: List<TemplateRegion> = emptyList(),
    val match: TemplateMatch? = null,
    val selected: Int? = null,
    val name: String = "",
    val busy: Boolean = false
) {
    /** 1 a 3 para mostrar «Paso n de 3»; el aviso de duplicado cuenta como el paso 1. */
    val stepNumber: Int get() = when (step) { WizardStep.PICK, WizardStep.DUPLICATE -> 1; WizardStep.REVIEW -> 2; WizardStep.NAME -> 3 }
    val approximateShape: Boolean get() = load?.approximateShape == true
    val detectedCount: Int get() = load?.detectedCount ?: 0
    val canSave: Boolean get() = name.isNotBlank() && regions.isNotEmpty()
    val maxRegions: Int get() = 64
}

/** Reglas del asistente, sin interfaz: se prueban en la JVM. */
object ImportWizard {
    private const val MIN_SIZE = 0.02

    /** Termina de analizar la imagen elegida: con un molde parecido, avisa antes de revisar los espacios. */
    fun loaded(state: ImportWizardState, file: File, load: TemplateLoad, match: TemplateMatch?): ImportWizardState =
        state.copy(
            step = if (match != null) WizardStep.DUPLICATE else WizardStep.REVIEW,
            file = file, load = load, regions = load.template.regions, match = match,
            selected = if (load.template.regions.isNotEmpty()) 0 else null, busy = false
        )

    /** «Guardar como nuevo» en el aviso de duplicado. */
    fun keepAsNew(state: ImportWizardState): ImportWizardState =
        if (state.step == WizardStep.DUPLICATE) state.copy(step = WizardStep.REVIEW) else state

    fun next(state: ImportWizardState): ImportWizardState = when (state.step) {
        WizardStep.REVIEW -> if (state.regions.isEmpty()) state else state.copy(step = WizardStep.NAME)
        else -> state
    }

    /** Atrás: null significa salir del asistente. */
    fun back(state: ImportWizardState): ImportWizardState? = when (state.step) {
        WizardStep.PICK -> null
        WizardStep.DUPLICATE -> ImportWizardState()
        WizardStep.REVIEW -> if (state.match != null) state.copy(step = WizardStep.DUPLICATE) else ImportWizardState()
        WizardStep.NAME -> state.copy(step = WizardStep.REVIEW)
    }

    fun select(state: ImportWizardState, index: Int?): ImportWizardState =
        state.copy(selected = index?.takeIf { it in state.regions.indices })

    fun setName(state: ImportWizardState, name: String): ImportWizardState = state.copy(name = name.take(60))

    private fun edit(state: ImportWizardState, index: Int, change: (TemplateRegion) -> TemplateRegion): ImportWizardState {
        if (index !in state.regions.indices) return state
        val regions = state.regions.toMutableList().also { it[index] = change(it[index]).clamped() }
        return state.copy(regions = regions)
    }

    /** Mueve el espacio por fracciones de la imagen; nunca sale de ella. */
    fun move(state: ImportWizardState, index: Int, dx: Double, dy: Double) =
        edit(state, index) { it.copy(x = it.x + dx, y = it.y + dy) }

    /** Arrastra una esquina: la opuesta queda fija. */
    fun resize(state: ImportWizardState, index: Int, corner: Corner, dx: Double, dy: Double) = edit(state, index) { r ->
        var left = r.x; var top = r.y; var right = r.x + r.width; var bottom = r.y + r.height
        when (corner) {
            Corner.TOP_LEFT -> { left += dx; top += dy }
            Corner.TOP_RIGHT -> { right += dx; top += dy }
            Corner.BOTTOM_LEFT -> { left += dx; bottom += dy }
            Corner.BOTTOM_RIGHT -> { right += dx; bottom += dy }
        }
        left = left.coerceIn(0.0, 1.0); right = right.coerceIn(0.0, 1.0)
        top = top.coerceIn(0.0, 1.0); bottom = bottom.coerceIn(0.0, 1.0)
        if (right - left < MIN_SIZE) { if (corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT) left = right - MIN_SIZE else right = left + MIN_SIZE }
        if (bottom - top < MIN_SIZE) { if (corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT) top = bottom - MIN_SIZE else bottom = top + MIN_SIZE }
        r.copy(x = left, y = top, width = right - left, height = bottom - top)
    }

    /** Cambia el tamaño desde el centro (acciones de accesibilidad). */
    fun grow(state: ImportWizardState, index: Int, dw: Double, dh: Double) = edit(state, index) { r ->
        r.copy(x = r.x - dw / 2, y = r.y - dh / 2, width = r.width + dw, height = r.height + dh)
    }

    fun setShape(state: ImportWizardState, index: Int, shape: RegionShape) = edit(state, index) {
        it.copy(shape = shape, radius = if (shape == RegionShape.ROUND && it.radius == 0.0) 0.2 else if (shape == RegionShape.ROUND) it.radius else 0.0)
    }

    fun setRadius(state: ImportWizardState, index: Int, radius: Double) = edit(state, index) { it.copy(radius = radius) }

    fun add(state: ImportWizardState): ImportWizardState {
        if (state.regions.size >= state.maxRegions) return state
        val regions = state.regions + TemplateRegion(x = 0.3, y = 0.3, width = 0.4, height = 0.3)
        return state.copy(regions = regions, selected = regions.lastIndex)
    }

    fun remove(state: ImportWizardState, index: Int): ImportWizardState {
        if (state.regions.size <= 1 || index !in state.regions.indices) return state
        val regions = state.regions.filterIndexed { i, _ -> i != index }
        return state.copy(regions = regions, selected = (index - 1).coerceAtLeast(0).coerceAtMost(regions.lastIndex))
    }

    /** Qué esquina de [r] está cerca del punto (en fracciones) dentro de [reach], si alguna. */
    fun cornerAt(r: TemplateRegion, x: Double, y: Double, reachX: Double, reachY: Double): Corner? =
        Corner.entries.firstOrNull { c ->
            val cx = if (c == Corner.TOP_LEFT || c == Corner.BOTTOM_LEFT) r.x else r.x + r.width
            val cy = if (c == Corner.TOP_LEFT || c == Corner.TOP_RIGHT) r.y else r.y + r.height
            kotlin.math.abs(x - cx) <= reachX && kotlin.math.abs(y - cy) <= reachY
        }

    /** Espacio bajo el punto; el más pequeño gana si se traslapan. */
    fun regionAt(regions: List<TemplateRegion>, x: Double, y: Double): Int? =
        regions.withIndex().filter { (_, r) -> x >= r.x && x <= r.x + r.width && y >= r.y && y <= r.y + r.height }
            .minByOrNull { (_, r) -> r.width * r.height }?.index
}
