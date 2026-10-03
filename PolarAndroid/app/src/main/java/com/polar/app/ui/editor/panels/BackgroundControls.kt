package com.polar.app.ui.editor.panels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.model.PhotoBackground
import com.polar.app.ui.components.*
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.editor.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackgroundControls(state: EditorUiState, vm: EditorViewModel, container: AppContainer) {
    val options = state.selectedPlacement?.background
    var colorDialog by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.addBackgroundPhoto(it.toString()) } }
    val photo = state.project.asset(state.selectedPlacement)
    val matchedColor by produceState<String?>(null, photo?.id) {
        value = withContext(Dispatchers.IO) {
            photo?.let { container.bitmaps.load(it.path, 128) }?.let { bitmap ->
                val colors = listOf(bitmap.getPixel(0, 0), bitmap.getPixel(bitmap.width-1, 0), bitmap.getPixel(0, bitmap.height-1), bitmap.getPixel(bitmap.width-1, bitmap.height-1))
                "%02X%02X%02X".format(colors.map { android.graphics.Color.red(it) }.average().toInt(), colors.map { android.graphics.Color.green(it) }.average().toInt(), colors.map { android.graphics.Color.blue(it) }.average().toInt())
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        SectionLabel("Fondo de la foto")
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Preparando el recorte. La primera vez puede tardar mientras se descarga el modelo.", style = MaterialTheme.typography.bodySmall)
        }
        if (options == null) {
            OutlinedButton(vm::removeBackground, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy) { Text("Quitar fondo") }
            Text("La primera vez necesita conexión y Servicios de Google Play para descargar el recorte. Se conservan el original y los rasgos del rostro. Revisa el borde del cabello y los fondos complejos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Blanco" to "FFFFFF", "Negro" to "000000", "Transparente" to null).forEach { (label, hex) ->
                    PolarChip(options.imageID == null && options.colorHex == hex, { vm.editSelectedBackground { options.copy(colorHex = hex, imageID = null) } }, { Text(label) })
                }
                matchedColor?.let { hex -> PolarChip(options.colorHex == hex && options.imageID == null, { vm.editSelectedBackground { options.copy(colorHex = hex, imageID = null) } }, { Text("Color de la foto") }) }
                TextButton({ colorDialog = true }, Modifier.heightIn(min = 48.dp)) { Text("Otro color") }
            }
            OutlinedButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy) { Text("Agregar una foto de fondo") }
            LabeledSlider("Suavizar borde", options.feather.toFloat(), 0f..1f, "${(options.feather*100).toInt()} %", { v -> vm.editSelectedBackground { options.copy(feather = v.toDouble()) } }, vm::beginGesture, vm::endGesture)
            LabeledSlider("Sombra suave", options.shadow.toFloat(), 0f..1f, "${(options.shadow*100).toInt()} %", { v -> vm.editSelectedBackground { options.copy(shadow = v.toDouble()) } }, vm::beginGesture, vm::endGesture)
            TextButton({ vm.editSelectedBackground { null } }, Modifier.heightIn(min = 48.dp)) { Text("Restaurar fondo original") }
        }
    }
    if (colorDialog) ColorChoiceDialog(options?.colorHex ?: "FFFFFF", { hex -> vm.editSelectedBackground { (it ?: PhotoBackground()).copy(colorHex = hex, imageID = null) } }) { colorDialog = false }
}
