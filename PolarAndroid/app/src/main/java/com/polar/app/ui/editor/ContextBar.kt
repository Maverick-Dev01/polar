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

@Composable
fun ContextBar(hasPhoto: Boolean, onChange: () -> Unit, onCrop: () -> Unit, onRotate: () -> Unit, onText: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (hasPhoto) {
            Row(Modifier.heightIn(min=64.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Action(Icons.Outlined.SwapHoriz, stringResource(R.string.ctx_change), onChange)
                Action(Icons.Outlined.Crop, stringResource(R.string.ctx_crop), onCrop)
                Action(Icons.AutoMirrored.Outlined.RotateRight, stringResource(R.string.ctx_rotate), onRotate)
                Action(Icons.Outlined.TextFields, stringResource(R.string.ctx_text), onText)
                Action(Icons.Outlined.Delete, stringResource(R.string.ctx_remove), onRemove)
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
private fun RowScope.Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.width(88.dp).heightIn(min=64.dp), contentPadding = PaddingValues(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null,modifier=Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,minLines=2,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
