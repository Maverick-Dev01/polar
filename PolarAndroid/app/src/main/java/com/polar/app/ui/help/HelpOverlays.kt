package com.polar.app.ui.help

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.polar.app.R
import com.polar.app.help.HelpContent
import com.polar.app.help.HelpControl
import com.polar.app.help.TourFlow
import com.polar.app.help.TourStep
import com.polar.app.ui.rememberReduceMotion
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Una capa que se come todos los toques: lo que hay debajo nunca los recibe. */
private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
}

/** Convierte una ventana a las coordenadas de la capa (que no siempre empieza en 0,0). */
@Composable
private fun rememberOrigin(): Pair<MutableState<Offset>, Modifier> {
    val origin = remember { mutableStateOf(Offset.Zero) }
    return origin to Modifier.onGloballyPositioned { origin.value = it.boundsInWindow().topLeft }
}

/**
 * Burbuja colocada junto a `anchor` (arriba si el objetivo está en la mitad de abajo, abajo si no) y siempre dentro de la pantalla.
 * Con letra grande el texto se desplaza y los botones quedan visibles.
 */
@Composable
private fun AnchoredBubble(anchor: Rect?, origin: Offset, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val margin = with(density) { 16.dp.roundToPx() }
    val gap = with(density) { 12.dp.roundToPx() }
    val maxBubbleW = with(density) { 360.dp.roundToPx() }
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val w = min(constraints.maxWidth - 2 * margin, maxBubbleW).coerceAtLeast(0)
        val placeable = measurables.first().measure(Constraints(minWidth = w, maxWidth = w, maxHeight = (constraints.maxHeight - 2 * margin).coerceAtLeast(0)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val a = anchor?.translate(-origin.x, -origin.y)
            val maxY = (constraints.maxHeight - placeable.height - margin).coerceAtLeast(margin)
            val y = if (a == null) maxY else {
                val above = a.top - gap - placeable.height
                val below = a.bottom + gap
                when {
                    a.center.y > constraints.maxHeight / 2f && above >= margin -> above
                    below <= maxY -> below
                    above >= margin -> above
                    else -> maxY.toFloat() // el objetivo ocupa casi todo: la burbuja va abajo, encima de él
                }.roundToInt().coerceIn(margin, maxY)
            }
            val x = ((a?.center?.x ?: (constraints.maxWidth / 2f)) - w / 2f).roundToInt().coerceIn(margin, (constraints.maxWidth - w - margin).coerceAtLeast(margin))
            placeable.place(IntOffset(x, y))
        }
    }
}

@Composable
private fun BubbleCard(
    title: String, text: String, animation: String?, reduceMotion: Boolean, header: String? = null,
    actions: @Composable RowScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (animation != null) { HelpAnimation(animation, Modifier.padding(bottom = 8.dp), reduceMotion); }
                if (header != null) Text(header, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/**
 * Recorrido inicial: fondo atenuado con un recorte sobre el control real, y una burbuja con texto y animación.
 * Si el control de un paso no está a la vista, ese paso se salta; al acabar (o saltar) llama a `onFinish`.
 */
@Composable
fun TourOverlay(steps: List<TourStep>, targets: HelpTargets, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val visible = targets.visible()
    var index by rememberSaveable { mutableIntStateOf(-1) }
    var settled by remember { mutableStateOf(false) }
    // Los controles se registran en el primer cuadro: se espera a que el editor termine de medirse.
    LaunchedEffect(Unit) { withFrameNanos { }; withFrameNanos { }; settled = true }
    val current = when {
        !settled -> null
        index < 0 -> TourFlow.first(steps, visible)
        steps.getOrNull(index)?.objetivo in visible -> index
        else -> TourFlow.next(steps, index, visible) // el control desapareció (p. ej. giraron la pantalla): sigue o termina
    }
    LaunchedEffect(settled, current) { if (settled && current == null) onFinish() else if (current != null && current != index) index = current }
    if (current == null) { if (!settled) Box(modifier.fillMaxSize().blockTouches()); return }
    val step = steps[current]
    val target = targets.bounds[step.objetivo] ?: return
    val (origin, originModifier) = rememberOrigin()
    val (pos, count) = TourFlow.position(steps, current, visible)
    val isLast = TourFlow.next(steps, current, visible) == null
    val scrim = Color(0xCC000000)
    val ring = MaterialTheme.colorScheme.primary
    val dur = if (reduceMotion) 0 else 320
    val left by animateFloatAsState(target.left, tween(dur), label = "l")
    val top by animateFloatAsState(target.top, tween(dur), label = "t")
    val right by animateFloatAsState(target.right, tween(dur), label = "r")
    val bottom by animateFloatAsState(target.bottom, tween(dur), label = "b")
    val pad = with(LocalDensity.current) { 6.dp.toPx() }

    Box(modifier.fillMaxSize().then(originModifier).blockTouches()) {
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).clearAndSetSemantics { }) {
            drawRect(scrim)
            val tl = Offset(left - origin.value.x - pad, top - origin.value.y - pad)
            val sz = Size(right - left + 2 * pad, bottom - top + 2 * pad)
            drawRoundRect(Color.Black, tl, sz, CornerRadius(14.dp.toPx()), blendMode = BlendMode.Clear)
            drawRoundRect(ring, tl, sz, CornerRadius(14.dp.toPx()), style = Stroke(3.dp.toPx()))
        }
        AnchoredBubble(Rect(left, top, right, bottom), origin.value) {
            BubbleCard(step.titulo, step.texto, step.animacion, reduceMotion, header = stringResource(R.string.tour_step, pos, count)) {
                if (!isLast) TextButton(onClick = onFinish, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.action_skip)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { TourFlow.next(steps, current, visible)?.let { index = it } ?: onFinish() }, Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(if (isLast) R.string.tour_done else R.string.action_next))
                }
            }
        }
    }
}

/**
 * Modo «?»: encima del editor, tocar un control muestra qué hace y NO lo ejecuta (la capa se come el toque).
 * Cada control registrado es también un elemento de accesibilidad con su nombre, para TalkBack.
 */
@Composable
fun HelpModeOverlay(content: HelpContent, targets: HelpTargets, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val (origin, originModifier) = rememberOrigin()
    val density = LocalDensity.current
    val ring = MaterialTheme.colorScheme.primary
    val entries = targets.bounds.entries
        .filter { it.value.width > 1f && it.value.height > 1f && it.key != com.polar.app.help.HelpIds.TOP_BAR }
        .mapNotNull { e -> content.control(e.key)?.let { Triple(e.key, e.value, it) } }
        // Los grandes (hoja, panel) debajo; los chicos encima: se elige siempre el control más específico bajo el dedo.
        .sortedByDescending { (_, r, _) -> r.width * r.height }
    val bannerTop = targets.bounds[com.polar.app.help.HelpIds.TOP_BAR]?.bottom ?: 0f

    Box(modifier.fillMaxSize().then(originModifier).blockTouches()
        .pointerInput(Unit) { detectTapGestures { selected = null } }) {
        val sel = entries.firstOrNull { it.first == selected }
        if (sel != null) Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val r = sel.second; val pad = 3.dp.toPx()
            drawRoundRect(ring, Offset(r.left - origin.value.x - pad, r.top - origin.value.y - pad), Size(r.width + 2 * pad, r.height + 2 * pad), CornerRadius(10.dp.toPx()), style = Stroke(3.dp.toPx()))
        }
        val openHint = stringResource(R.string.help_mode_open_hint)
        entries.forEachIndexed { i, (id, r, control) ->
            Box(
                Modifier.zIndex(i.toFloat())
                    .offset { IntOffset((r.left - origin.value.x).roundToInt(), (r.top - origin.value.y).roundToInt()) }
                    .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                    .semantics(mergeDescendants = true) { contentDescription = control.titulo; role = Role.Button }
                    .clickable(onClickLabel = openHint, interactionSource = remember { MutableInteractionSource() }, indication = null) { selected = id }
            )
        }
        Surface(
            Modifier.zIndex(1000f).offset { IntOffset(0, (bannerTop - origin.value.y).roundToInt().coerceAtLeast(0)) }
                .padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 4.dp, shadowElevation = 4.dp
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.help_mode_banner), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                TextButton(onClick = onDone, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.help_mode_done)) }
            }
        }
        if (sel != null) Box(Modifier.zIndex(1001f)) {
            AnchoredBubble(sel.second, origin.value) {
                BubbleCard(sel.third.titulo, sel.third.texto, null, reduceMotion) {
                    TextButton(onClick = { selected = null }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.help_bubble_close)) }
                }
            }
        }
    }
}
