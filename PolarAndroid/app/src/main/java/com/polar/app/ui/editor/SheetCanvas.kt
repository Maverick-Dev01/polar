package com.polar.app.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.data.BitmapLoader
import com.polar.app.engine.PolarRenderer
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.PhotoLook
import com.polar.app.model.PolarProject
import com.polar.app.model.TemplateStyle
import com.polar.app.ui.theme.PaperColors
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SheetArea(state: EditorUiState, vm: EditorViewModel, container: AppContainer, modifier: Modifier = Modifier) {
    val count = state.project.pageCount
    val pager = rememberPagerState(initialPage = state.page) { count }
    var zoomed by remember { mutableStateOf(false) }
    LaunchedEffect(pager.currentPage) { if (pager.currentPage != state.page) vm.setPage(pager.currentPage) }
    LaunchedEffect(state.page) { if (pager.currentPage != state.page) pager.animateScrollToPage(state.page) }
    LaunchedEffect(state.page) { zoomed = false } // la ampliación no se hereda a otra hoja

    Column(modifier.background(PolarColors.table)) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = !zoomed && !state.editingRegions,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            SheetPage(state, page, vm, container, zoomed = zoomed && page == state.page, onToggleZoom = { zoomed = !zoomed })
        }
        PagerRow(state.page, count, onPrev = { vm.setPage(state.page - 1) }, onNext = { vm.setPage(state.page + 1) })
    }
}

@Composable
private fun PagerRow(page: Int, count: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = page > 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.editor_prev_page)) }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clearAndSetSemantics { }) {
            repeat(minOf(count, 8)) { i ->
                Box(Modifier.height(7.dp).width(if (i == page) 18.dp else 7.dp).background(
                    if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
            }
        }
        Text(stringResource(R.string.editor_page, page + 1, count), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp))
        IconButton(onClick = onNext, enabled = page < count - 1) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.editor_next_page)) }
    }
}

@Composable
private fun SheetPage(state: EditorUiState, page: Int, vm: EditorViewModel, container: AppContainer, zoomed: Boolean, onToggleZoom: () -> Unit) {
    val project = remember(state.project,state.comparing) { if(state.comparing) ProjectEdits.setAllLooks(state.project,PhotoLook()) else state.project }
    val settings = project.settings
    val paper = settings.paperSizePoints
    val ratio = (paper.width / paper.height).toFloat()
    val density = LocalDensity.current
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val warning = PolarColors.onWarningContainer
    val rects = remember(project, page) { SheetGeometry.slotRects(project, page) }
    val lowRes = remember(project) { vm.lowResSlots().toSet() }
    val fairRes = remember(project) { vm.reviewSlots().toSet() - lowRes }
    val errorColor = MaterialTheme.colorScheme.error
    val cap = settings.capacity
    val currentSettings by rememberUpdatedState(settings)

    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val (w, h) = fit(maxWidth, maxHeight, ratio)
        val widthPx = with(density) { w.toPx() }
        val ptToPx = widthPx / paper.width.toFloat()
        val image = rememberPageImage(project, page, widthPx.roundToInt(), vm.templateBitmap, state.templateVersion, container)
        var pan by remember(zoomed) { mutableStateOf(Offset.Zero) }

        Box(
            Modifier.size(w, h).testTag("print-sheet-$page")
                .graphicsLayer {
                    val s = if (zoomed) 2f else 1f
                    scaleX = s; scaleY = s; translationX = pan.x; translationY = pan.y
                }
                .shadow(6.dp, RoundedCornerShape(2.dp))
                .background(PaperColors.Paper)
                .pointerInput(rects, ptToPx, page, state.editingRegions) {
                    detectTapGestures(
                        onDoubleTap = { onToggleZoom() },
                        onLongPress = { },
                        onPress = {
                            try {
                                val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { tryAwaitRelease() }
                                if(released==null) { vm.setComparing(true); tryAwaitRelease() }
                            } finally { vm.setComparing(false) }
                        },
                        onTap = { pos ->
                            val xPt = (pos.x / ptToPx).toDouble()
                            val yPt = (pos.y / ptToPx).toDouble()
                            val hit = SheetGeometry.slotAt(rects, xPt, yPt) ?: SheetGeometry.cardSlotAt(project, page, xPt, yPt)
                            if (hit != null) vm.selectSlot(page * cap + hit) else vm.clearSelection()
                        }
                    )
                }
                .pointerInput(zoomed, state.editingRegions, state.selectedSlot, ptToPx) {
                    if (zoomed) detectDragGestures { change, drag -> change.consume(); pan += drag }
                    else if (state.editingRegions && currentSettings.style == TemplateStyle.IMPORTED) {
                        val index = (state.selectedSlot ?: return@pointerInput) % cap
                        // Cada paso del arrastre cambia `settings`; por eso no es llave y se lee el valor vigente.
                        var dragging = false
                        try {
                            detectDragGestures(
                                onDragStart = { dragging = true; vm.beginGesture() },
                                onDragEnd = { dragging = false; vm.endGesture() },
                                onDragCancel = { dragging = false; vm.endGesture() }
                            ) { change, drag ->
                                change.consume()
                                val frame = PolarRenderer.templateRect(currentSettings)
                                vm.moveRegion(index, drag.x / ptToPx / frame.width, drag.y / ptToPx / frame.height)
                            }
                        } finally {
                            // Si el bloque se cancela a media pasada, onDragCancel no se llama: cierra la transacción aquí.
                            if (dragging) vm.endGesture()
                        }
                    }
                }
        ) {
            image?.let { Image(it, null, Modifier.fillMaxSize()) }
            val selectedLocal = state.selectedSlot?.takeIf { it / cap == page }?.rem(cap)
            Canvas(Modifier.fillMaxSize()) {
                selectedLocal?.let { rects.getOrNull(it) }?.let { r ->
                    val inset = 3.dp.toPx()
                    drawRect(
                        primary, topLeft = Offset(r.left.toFloat() * ptToPx - inset, r.top.toFloat() * ptToPx - inset),
                        size = Size(r.width.toFloat() * ptToPx + 2 * inset, r.height.toFloat() * ptToPx + 2 * inset),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                rects.forEachIndexed { i, r ->
                    val dot = if (page * cap + i in lowRes) errorColor else if (page * cap + i in fairRes) warning else null
                    if (dot != null) drawCircle(dot, radius = 7.dp.toPx(),
                        center = Offset(r.right.toFloat() * ptToPx - 9.dp.toPx(), r.bottom.toFloat() * ptToPx - 9.dp.toPx()))
                }
            }
            selectedLocal?.let { rects.getOrNull(it) }?.let { r ->
                Box(
                    Modifier.offset(with(density) { (r.left.toFloat() * ptToPx).toDp() } - 10.dp, with(density) { (r.top.toFloat() * ptToPx).toDp() } - 10.dp)
                        .background(primary, MaterialTheme.shapes.small).padding(horizontal = 6.dp, vertical = 2.dp)
                        .clearAndSetSemantics { }
                ) { Text("${state.selectedCardNumber ?: 1}", color = onPrimary, style = MaterialTheme.typography.labelSmall) }
            }
            // Nodos sólo para TalkBack: no reciben toques, el toque lo maneja la hoja.
            rects.forEachIndexed { i, r ->
                val slot = page * cap + i
                val empty = project.placements.getOrNull(slot) == null
                val number = project.cardOfSlot(slot) % project.cardsPerPage + 1
                val label = (if (empty) context.getString(R.string.editor_card_empty, number) else context.getString(R.string.editor_card, number)) +
                    (if (slot in lowRes) context.getString(R.string.editor_card_low) else if (slot in fairRes) context.getString(R.string.editor_card_fair) else "")
                Box(
                    Modifier.offset(with(density) { (r.left.toFloat() * ptToPx).toDp() }, with(density) { (r.top.toFloat() * ptToPx).toDp() })
                        .size(with(density) { (r.width.toFloat() * ptToPx).toDp() }, with(density) { (r.height.toFloat() * ptToPx).toDp() })
                        .semantics {
                            contentDescription = label
                            role = Role.Button
                            selected = state.selectedSlot == slot
                            onClick { vm.selectSlot(slot); true }
                        }
                )
            }
        }
    }
}

private fun fit(maxW: Dp, maxH: Dp, ratio: Float): Pair<Dp, Dp> =
    if (maxW / maxH > ratio) (maxH * ratio) to maxH else maxW to (maxW / ratio)

@Composable
private fun rememberPageImage(project: PolarProject, page: Int, widthPx: Int, template: Bitmap?, version: Int, container: AppContainer): ImageBitmap? {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(project, page, widthPx, version) {
        delay(16) // junta cambios muy seguidos (sliders, escritura)
        val next = try {
            withContext(Dispatchers.Default) {
                val paper = project.settings.paperSizePoints
                val scale = widthPx / paper.width.toFloat()
                val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), (paper.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                PolarRenderer.drawPage(
                    AndroidCanvas(bitmap), project, page, isPreview = true, scale = scale,
                    bitmapProvider = { container.bitmaps.load(it.path, BitmapLoader.PREVIEW_MAX) },
                    templateBitmap = template, fonts = container.fonts
                )
                bitmap.asImageBitmap()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Polar", "No se pudo dibujar la hoja", e); null
        } catch (e: OutOfMemoryError) {
            Log.w("Polar", "Sin memoria al dibujar la hoja", e); null
        }
        if (next != null) image = next // si falla, se queda la imagen anterior
    }
    return image
}
