package com.polar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polar.app.R

fun hexColor(hex: String): Color = runCatching { Color(("FF" + hex.removePrefix("#")).toLong(16)) }.getOrDefault(Color.Black)

/** Colores para tocar, con nombre para el lector de pantalla. */
val ColorChoices: List<Pair<String, String>> = listOf(
    "92394A" to "Vino", "C34048" to "Rojo", "E07A5F" to "Coral", "F28C28" to "Naranja", "BC8952" to "Dorado",
    "D4A72C" to "Mostaza", "7A8450" to "Oliva", "486855" to "Verde", "7FB7A4" to "Menta", "38536F" to "Azul",
    "6C9BCF" to "Cielo", "8E7CC3" to "Lavanda", "D98CA8" to "Rosa", "7B5544" to "Café", "C8B49A" to "Arena",
    "8A8580" to "Gris", "20242C" to "Carbón", "FFFFFF" to "Blanco"
)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    steps: Int = 0
) {
    var dragging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { if(dragging) onEnd() } }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).alignByBaseline())
            Text(valueText,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.widthIn(min=64.dp).alignByBaseline())
        }
        Slider(
            value = value, valueRange = range, steps = steps,
            onValueChange = { if (!dragging) { dragging = true; onStart() }; onChange(it) },
            onValueChangeFinished = { dragging = false; onEnd() },
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

@Composable
fun MoreOptions(expanded: Boolean, onToggle: () -> Unit, summary: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.editor_more), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(if (expanded) stringResource(R.string.more_hide) else summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
        }
        if (expanded) Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun ColorSwatch(hex: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val color = hexColor(hex)
    Box(
        Modifier.size(48.dp)
            .semantics { contentDescription = name; this.selected = selected; role = Role.RadioButton }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .border(if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape)
            .padding(3.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Filled.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorChoiceDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        title = { Text(stringResource(R.string.color_choose)) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorChoices.forEach { (hex, name) -> ColorSwatch(hex, name, hex.equals(current, true)) { onPick(hex); onDismiss() } }
            }
        }
    )
}

/** Dos acciones iguales; con letra grande cada acción ocupa una fila. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymmetricActions(content: @Composable FlowRowScope.()->Unit) {
    val large=androidx.compose.ui.platform.LocalDensity.current.fontScale>=1.2f
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),maxItemsInEachRow=if(large) 1 else 2,content=content)
}

@Composable
fun PolarChip(selected: Boolean,onClick: ()->Unit,label: @Composable ()->Unit,modifier: Modifier=Modifier,leadingIcon: (@Composable ()->Unit)?=null) {
    FilterChip(selected,onClick,label,modifier.heightIn(min=48.dp),leadingIcon=leadingIcon)
}
