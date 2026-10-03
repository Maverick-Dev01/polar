package com.polar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt

/** Anillo compartido: gesto completo, teclado y acción ajustable del lector de pantalla. */
@Composable
fun LensRing(label: String, value: Double, range: ClosedFloatingPointRange<Double>, valueText: String,
             step: Double = .1, onChange: (Double)->Unit, onStart: ()->Unit, onEnd: ()->Unit) {
    val latest by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val start by rememberUpdatedState(onStart)
    val end by rememberUpdatedState(onEnd)
    val haptic=LocalHapticFeedback.current
    val ink=MaterialTheme.colorScheme.onSurfaceVariant
    val accent=MaterialTheme.colorScheme.primary
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableDoubleStateOf(value) }
    fun discrete(v: Double) { start(); change(v.coerceIn(range)); end() }
    DisposableEffect(Unit) { onDispose { if(dragging) end() } }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, modifier=Modifier.weight(1f).alignByBaseline(), style=MaterialTheme.typography.bodyMedium)
            Text(valueText, modifier=Modifier.widthIn(min=64.dp).alignByBaseline(), color=ink, style=MaterialTheme.typography.bodyMedium)
        }
        Canvas(Modifier.fillMaxWidth().height(56.dp)
            .semantics {
                contentDescription=label; stateDescription=valueText
                progressBarRangeInfo=ProgressBarRangeInfo(value.toFloat(),range.start.toFloat()..range.endInclusive.toFloat())
                setProgress { discrete(it.toDouble()); true }
            }.onKeyEvent { event ->
                if(event.type==KeyEventType.KeyDown && (event.key==Key.DirectionLeft || event.key==Key.DirectionRight)) {
                    discrete(latest+if(event.key==Key.DirectionLeft) -step else step); true
                } else false
            }.focusable()
            .pointerInput(range) {
                try {
                detectHorizontalDragGestures(
                    onDragStart={ dragValue=latest; dragging=true; start() },
                    onDragEnd={ dragging=false; end() }, onDragCancel={ dragging=false; end() }
                ) { event, amount ->
                    event.consume()
                    val next=(dragValue+amount/size.width*(range.endInclusive-range.start)).coerceIn(range)
                    if(floor(next)!=floor(dragValue) && floor(next) in 1.0..3.0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    dragValue=next;change(next)
                }
                } finally { if(dragging) { dragging=false; end() } }
            }) {
                        for(i in -30..30) {
                val x=(size.width/2+i*12.dp.toPx()-(value/step-value.div(step).roundToInt())*12.dp.toPx()).toFloat()
                if(x in 0f..size.width) drawLine(ink.copy(alpha=.65f),Offset(x,size.height*.38f),Offset(x,size.height*(if(i%5==0) .82f else .66f)),1.dp.toPx())
            }
            drawLine(accent,Offset(size.width/2,size.height*.16f),Offset(size.width/2,size.height*.88f),3.dp.toPx())
            drawLine(ink.copy(alpha=.3f),Offset(0f,size.height-2.dp.toPx()),Offset(size.width,size.height-2.dp.toPx()))
        }
    }
}
