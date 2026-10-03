package com.polar.app.ui.editor.panels

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.polar.app.AppContainer
import com.polar.app.engine.PolarRenderer
import com.polar.app.ui.editor.EditorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TextEditDialog(state: EditorUiState, container: AppContainer, onText: (String) -> Unit, onDismiss: () -> Unit) {
    val panel = textPanelState(state)
    val slot = state.selectedSlot ?: state.project.placements.indexOfFirst { it != null }.coerceAtLeast(0)
    val image by produceState<ImageBitmap?>(null, state.project, slot) {
        value = withContext(Dispatchers.Default) {
            PolarRenderer.cardPreview(state.project, slot, 1200, { container.bitmaps.load(it.path, 1600) }).asImageBitmap()
        }
    }
    var showPhrase by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val dismiss = { focus.clearFocus(); keyboard?.hide(); onDismiss() }
    Dialog(dismiss, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            BoxWithConstraints {
            val compact = maxHeight < 300.dp || WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
            // Mantener el campo en la misma rama cuando el IME cambia la altura evita perder el foco.
            val horizontal = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val showHeading = !horizontal || maxHeight >= 180.dp
            val preview: @Composable (Modifier) -> Unit = { modifier ->
                Box(modifier.clipToBounds().pointerInput(Unit) {
                    detectTransformGestures { _, movement, factor, _ ->
                        zoom = (zoom * factor).coerceIn(1f,4f)
                        val next = pan + movement
                        pan = Offset(next.x.coerceIn(-size.width*(zoom-1)/2,size.width*(zoom-1)/2), next.y.coerceIn(-size.height*(zoom-1)/2,size.height*(zoom-1)/2))
                    }
                }) {
                    image?.let { Image(it, "Vista ampliada de la tarjeta. Pellizca para ampliar.", Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = pan.x, translationY = pan.y)) }
                    TextButton({ zoom = 1f; pan = Offset.Zero }, Modifier.align(androidx.compose.ui.Alignment.TopEnd)) { Text("1×") }
                }
            }
            val input: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(panel.text, onText, label = { Text("Texto que se imprimirá") }, minLines = if (compact) 1 else 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                    Text(if (compact) "${panel.text.length} / 500 caracteres" else "${panel.text.length} / 500 caracteres. Revisa el espacio en la vista ampliada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton({ showPhrase = true }, Modifier.heightIn(min = 48.dp)) { Text("Buscar una frase") }
                }
            }
            Column(Modifier.padding(if (compact) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp)) {
                if (showHeading) Text("Editar ${panel.role.displayName.lowercase()}", style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
                if (horizontal) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    preview(Modifier.weight(1f).fillMaxHeight()); input(Modifier.weight(1f).fillMaxHeight())
                } else {
                    preview(Modifier.weight(1f).fillMaxWidth()); input(Modifier.weight(1f).fillMaxWidth())
                }
                Button(dismiss, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Listo") }
            }
            }
        }
    }
    if (showPhrase) PhraseDialog(onText) { showPhrase = false }
}
