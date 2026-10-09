package com.polar.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.help.HelpIds
import com.polar.app.ui.help.helpTarget

private val MinActionWidth = 60.dp

@Composable
fun ContextBar(hasPhoto: Boolean, onChange: () -> Unit, onCrop: () -> Unit, onRemoveBackground: () -> Unit, onRotate: () -> Unit, onText: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().helpTarget(HelpIds.CARD_ACTIONS).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (hasPhoto) {
            val actions = listOf(
                Triple(Icons.Outlined.SwapHoriz, R.string.ctx_change, onChange),
                Triple(Icons.Outlined.Crop, R.string.ctx_crop, onCrop),
                Triple(Icons.Outlined.AutoFixHigh, R.string.ctx_remove_background, onRemoveBackground),
                Triple(Icons.AutoMirrored.Outlined.RotateRight, R.string.ctx_rotate, onRotate),
                Triple(Icons.Outlined.TextFields, R.string.ctx_text, onText),
                Triple(Icons.Outlined.Delete, R.string.ctx_remove, onRemove)
            )
            val largeFont = LocalDensity.current.fontScale > 1.15f
            BoxWithConstraints {
                // Caben en una fila si hay 60 dp por acción y la letra es normal; si no, dos filas de tres con el mismo ancho
                // (nada queda escondido en un desplazamiento y las etiquetas no se cortan).
                val oneRow = !largeFont && maxWidth - 8.dp >= MinActionWidth * actions.size
                if (oneRow) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        actions.forEach { (icon, label, click) -> Action(icon, stringResource(label), click, Modifier.weight(1f)) }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                        actions.chunked(3).forEach { rowActions ->
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
                                rowActions.forEach { (icon, label, click) -> Action(icon, stringResource(label), click, Modifier.weight(1f).fillMaxHeight()) }
                            }
                        }
                    }
                }
            }
        } else {
            Box(Modifier.heightIn(min=64.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = onChange,modifier=Modifier.heightIn(min=56.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ctx_place))
                }
            }
        }
    }
}

@Composable
private fun RowScope.Action(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min=64.dp), contentPadding = PaddingValues(2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null,modifier=Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,minLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
