package com.polar.app.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.data.BitmapLoader
import com.polar.app.engine.*
import com.polar.app.ui.components.LensRing
import com.polar.app.ui.theme.CropGuide
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(state: EditorUiState,vm: EditorViewModel,container: AppContainer) {
    val slot=state.selectedSlot; val placement=state.selectedPlacement; val asset=state.project.asset(placement)
    if(slot==null || placement==null || asset==null) { LaunchedEffect(Unit) { vm.setMode(EditorMode.EDIT) };return }
    val project=state.project
    val settings=project.settings
    val card=remember(project,slot) { PolarRenderer.cardRects(project, slot/settings.capacity)[slot%settings.capacity/settings.style.photosPerCard] }
    val rect=remember(project,slot) { SheetGeometry.slotRects(project, slot/settings.capacity)[slot%settings.capacity] }
    val minimumZoom=PhotoFit.fitZoom(rect.width,rect.height,asset.pixelWidth,asset.pixelHeight,placement.quarterTurns)
    val latest by rememberUpdatedState(state.selectedPlacement!!)
    var gesturing by remember(slot) { mutableStateOf(false) }
    var fine by remember { mutableStateOf(false) }
    val image by produceState<Bitmap?>(null,state.project,slot,state.templateVersion) {
        value=withContext(Dispatchers.Default) { PolarRenderer.cardPreview(state.project,slot,800,{container.bitmaps.load(it.path,BitmapLoader.PREVIEW_MAX)},vm.templateBitmap,container.fonts) }
    }
    val overflow by produceState<Bitmap?>(null,state.project,slot) {
        value=withContext(Dispatchers.Default) { PolarRenderer.cropOverflow(state.project,slot,800,{container.bitmaps.load(it.path,BitmapLoader.PREVIEW_MAX)}) }
    }
    val slots=state.project.placements.indices.filter { state.project.placements[it]!=null }
    val position=slots.indexOf(slot)
    val quality=vm.qualityOf(slot) ?: PhotoQuality.GOOD
    fun fit()=vm.editSelectedPlacement { it.copy(zoom=PhotoFit.fitZoom(rect.width,rect.height,asset.pixelWidth,asset.pixelHeight,it.quarterTurns),offsetX=0.0,offsetY=0.0) }
    fun fill()=vm.editSelectedPlacement { it.copy(zoom=1.0,offsetX=0.0,offsetY=0.0) }
    fun move(dx: Double,dy: Double)=vm.editSelectedPlacement { it.copy(offsetX=it.offsetX+dx,offsetY=it.offsetY+dy) }
    fun zoom(delta: Double)=vm.editSelectedPlacement { it.copy(zoom=(it.zoom+delta).coerceIn(minimumZoom,4.0)) }
    val accessibilityActions=listOf(
        stringResource(R.string.crop_move_left) to {move(-.01,0.0)},stringResource(R.string.crop_move_right) to {move(.01,0.0)},
        stringResource(R.string.crop_move_up) to {move(0.0,-.01)},stringResource(R.string.crop_move_down) to {move(0.0,.01)},
        stringResource(R.string.crop_zoom_in) to {zoom(.1)},stringResource(R.string.crop_zoom_out) to {zoom(-.1)}
    )
    DisposableEffect(slot) { onDispose { vm.endGesture() } }
    Scaffold(topBar={ TopAppBar(title={Text(stringResource(R.string.crop_title,state.selectedCardNumber ?: 1),maxLines=2)},navigationIcon={IconButton(onClick={vm.setMode(EditorMode.EDIT)}){Icon(Icons.Filled.Close,stringResource(R.string.action_close))}},actions={TextButton(onClick={vm.setMode(EditorMode.EDIT)}){Text(stringResource(R.string.crop_done))}}) }) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val previewHeight=(maxHeight*.43f).coerceIn(200.dp,440.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                    IconButton(onClick={vm.adjacentPhoto(-1)},enabled=position>0){Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,stringResource(R.string.crop_previous))}
                    Text(stringResource(R.string.crop_position,position+1,slots.size),modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick={vm.adjacentPhoto(1)},enabled=position<slots.lastIndex){Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,stringResource(R.string.crop_next))}
                }
                Box(Modifier.fillMaxWidth().height(previewHeight).clipToBounds(),contentAlignment=Alignment.Center) {
                    Box(Modifier.aspectRatio((card.width/card.height).toFloat(),matchHeightConstraintsFirst=true).fillMaxHeight()
                        .semantics { contentDescription=asset.path.substringAfterLast('/'); customActions=accessibilityActions.map { (label,action)->CustomAccessibilityAction(label) { action();true } } }
                        .pointerInput(slot,card,rect) { detectTapGestures(onDoubleTap={if(latest.zoom<.999) fill() else fit()}) }
                        .pointerInput(slot,card,rect,placement.quarterTurns) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed=false)
                                vm.beginGesture(); gesturing=true
                                try {
                                    do {
                                        val event=awaitPointerEvent()
                                        val pan=event.calculatePan();val factor=event.calculateZoom()
                                        if(pan!=Offset.Zero || factor!=1f) {
                                            val scale=size.width/card.width
                                            vm.editSelectedPlacement { p ->
                                                PhotoFit.moved(p.copy(zoom=(p.zoom*factor).coerceIn(minimumZoom,4.0)),pan.x/scale,pan.y/scale,rect.width,rect.height,asset.pixelWidth,asset.pixelHeight)
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while(event.changes.any { it.pressed })
                                } finally { gesturing=false;vm.endGesture() }
                            }
                        }) {
                        image?.let { Image(it.asImageBitmap(),null,Modifier.fillMaxSize()) }
                        Canvas(Modifier.fillMaxSize()) {
                            val scale=size.width/card.width
                            val origin=Offset(((rect.left-card.left)*scale).toFloat(),((rect.top-card.top)*scale).toFloat())
                            val box=Size((rect.width*scale).toFloat(),(rect.height*scale).toFloat())
                            overflow?.let { outside ->
                                val mask=Path().apply {
                                    fillType=PathFillType.EvenOdd
                                    addRect(androidx.compose.ui.geometry.Rect(-size.width*.18f,-size.height*.08f,size.width*1.18f,size.height*1.08f))
                                    addRect(androidx.compose.ui.geometry.Rect(origin,box))
                                }
                                clipPath(mask) {
                                    drawImage(outside.asImageBitmap(),dstOffset=IntOffset((-size.width*.18f).toInt(),(-size.height*.08f).toInt()),dstSize=IntSize((size.width*1.36f).toInt(),(size.height*1.16f).toInt()),alpha=.2f)
                                }
                            }
                            drawRect(CropGuide,origin,box,style=Stroke(2.dp.toPx()))
                            if(gesturing) for(i in 1..2) {
                                drawLine(CropGuide,origin+Offset(box.width*i/3,0f),origin+Offset(box.width*i/3,box.height))
                                drawLine(CropGuide,origin+Offset(0f,box.height*i/3),origin+Offset(box.width,box.height*i/3))
                            }
                        }
                    }
                }
                val qBg=when(quality){PhotoQuality.LOW->MaterialTheme.colorScheme.errorContainer;PhotoQuality.FAIR->PolarColors.warningContainer;PhotoQuality.GOOD->PolarColors.successContainer}
                val qFg=when(quality){PhotoQuality.LOW->MaterialTheme.colorScheme.onErrorContainer;PhotoQuality.FAIR->PolarColors.onWarningContainer;PhotoQuality.GOOD->PolarColors.onSuccessContainer}
                Surface(color=qBg,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
                    Text(stringResource(when(quality){PhotoQuality.LOW->R.string.crop_quality_low;PhotoQuality.FAIR->R.string.crop_quality_fair;PhotoQuality.GOOD->R.string.crop_quality_ok}),color=qFg,modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.labelLarge)
                }
                com.polar.app.ui.editor.panels.BackgroundControls(state, vm, container)
                LensRing(stringResource(R.string.crop_zoom),placement.zoom,minimumZoom..4.0,String.format(Locale.ROOT,"%.1f×",placement.zoom),.1,{ v->vm.editSelectedPlacement { it.copy(zoom=v) } },vm::beginGesture,vm::endGesture)
                val actions=listOf(stringResource(R.string.crop_rotate) to vm::rotateSelected,stringResource(R.string.crop_fill) to ::fill,stringResource(R.string.crop_fit) to ::fit,stringResource(R.string.crop_center) to {vm.editSelectedPlacement {it.copy(offsetX=0.0,offsetY=0.0)}},stringResource(R.string.crop_reset) to vm::resetSelectedPlacement)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) { items(actions.size) { i->val (label,action)=actions[i];OutlinedButton(onClick=action,modifier=Modifier.width(104.dp).heightIn(min=56.dp),contentPadding=PaddingValues(8.dp),shape=MaterialTheme.shapes.medium){Text(label,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)} } }
                TextButton(onClick={fine=!fine},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.crop_fine))}
                if(fine) accessibilityActions.chunked(2).forEach { pair->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){pair.forEach{(label,action)->OutlinedButton(onClick=action,modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text(label)}}} }
                Text(stringResource(R.string.crop_gesture_hint),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
