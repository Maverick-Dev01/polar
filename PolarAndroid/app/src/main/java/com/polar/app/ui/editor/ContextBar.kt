package com.polar.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.R

private val ActionWidth = 88.dp

@Composable
fun ContextBar(hasPhoto: Boolean, onChange: () -> Unit, onCrop: () -> Unit, onRemoveBackground: () -> Unit, onRotate: () -> Unit, onText: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
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
            BoxWithConstraints {
                // Si caben, las seis acciones se reparten igual; si no, cada una conserva su ancho y la fila se desplaza.
                val fits = maxWidth - 8.dp >= ActionWidth * actions.size
                val rowModifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 4.dp)
                Row(if (fits) rowModifier.fillMaxWidth() else rowModifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    actions.forEach { (icon, label, click) -> Action(icon, stringResource(label), click, if (fits) Modifier.weight(1f) else Modifier.width(ActionWidth)) }
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
    TextButton(onClick = onClick, modifier = modifier.heightIn(min=64.dp), contentPadding = PaddingValues(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null,modifier=Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,minLines=2,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
