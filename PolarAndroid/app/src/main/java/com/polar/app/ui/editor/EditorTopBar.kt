package com.polar.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polar.app.R

/** En anchos menores de 400 dp o con letra mayor a 1.15, Rehacer pasa al menú para que nada se encime. */
fun redoInMenu(widthDp: Float, fontScale: Float): Boolean = widthDp < 400f || fontScale > 1.15f

class EditorTopBarActions(
    val onBack: () -> Unit, val onRename: () -> Unit, val onUndo: () -> Unit, val onRedo: () -> Unit, val onPrint: () -> Unit,
    val onSelectMany: () -> Unit, val onAddPage: () -> Unit, val onClearPage: () -> Unit, val onRemovePage: () -> Unit
)

/** Atrás · nombre y estado · Deshacer · Rehacer · ⋮ · «Imprimir» (ícono y texto). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(name: String, styleName: String, status: String, canUndo: Boolean, canRedo: Boolean, actions: EditorTopBarActions) {
    var menu by remember { mutableStateOf(false) }
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints {
        val redoInMenu = redoInMenu(maxWidth.value, fontScale)
        TopAppBar(
            expandedHeight = if (fontScale >= 1.2f) 104.dp else 88.dp,
            navigationIcon = { IconButton(onClick = actions.onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.editor_back)) } },
            title = {
                Column(Modifier.clickable(onClick = actions.onRename)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { contentDescription = name })
                    Text(
                        "$styleName · $status", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                IconButton(onClick = actions.onUndo, enabled = canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.action_undo)) }
                if (!redoInMenu) IconButton(onClick = actions.onRedo, enabled = canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.action_redo)) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.editor_more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (redoInMenu) DropdownMenuItem({ Text(stringResource(R.string.action_redo)) }, { menu = false; actions.onRedo() }, enabled = canRedo,
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, null) })
                        DropdownMenuItem({ Text(stringResource(R.string.editor_select_many)) }, { menu = false; actions.onSelectMany() })
                        DropdownMenuItem({ Text(stringResource(R.string.editor_add_page)) }, { menu = false; actions.onAddPage() })
                        DropdownMenuItem({ Text(stringResource(R.string.editor_clear_page)) }, { menu = false; actions.onClearPage() })
                        DropdownMenuItem({ Text(stringResource(R.string.editor_remove_page)) }, { menu = false; actions.onRemovePage() })
                        DropdownMenuItem({ Text(stringResource(R.string.editor_rename)) }, { menu = false; actions.onRename() })
                    }
                }
                Button(onClick = actions.onPrint, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.heightIn(min = 48.dp).padding(end = 8.dp)) {
                    Icon(Icons.Outlined.Print, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.editor_print), maxLines = 1)
                }
            }
        )
    }
}
