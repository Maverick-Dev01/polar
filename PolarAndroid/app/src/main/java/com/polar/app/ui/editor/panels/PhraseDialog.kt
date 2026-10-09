package com.polar.app.ui.editor.panels

import com.polar.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.polar.app.core.text.PhraseCatalog

@Composable
fun PhraseDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val phrases = remember { PhraseCatalog.load(context) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf<TextFieldValue?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight(.94f).windowInsetsPadding(WindowInsets.safeDrawing).imePadding(), shape = MaterialTheme.shapes.large) {
            BoxWithConstraints {
            val compact = maxHeight < 300.dp
            Column(Modifier.padding(if (compact) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp)) {
                Text(stringResource(R.string.phrase_title), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
                if (draft == null) {
                    val matches = remember(query, category) { PhraseCatalog.search(phrases, query, category) }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                    Text(stringResource(R.string.phrase_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                    OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.phrase_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                    Box {
                        OutlinedButton({ categoryMenu = true }, Modifier.heightIn(min = 48.dp)) { Text(category ?: stringResource(R.string.phrase_all_categories)) }
                        DropdownMenu(categoryMenu, { categoryMenu = false }) {
                            (listOf<String?>(null) + phrases.map { it.category }.distinct()).forEach { c ->
                                DropdownMenuItem({ Text(c ?: stringResource(R.string.phrase_all)) }, { category = c; categoryMenu = false })
                            }
                        }
                    }
                    }
                        if (matches.isEmpty()) item { Text(stringResource(R.string.phrase_none)) }
                        items(matches, key = { it.id }) { phrase ->
                            OutlinedCard(onClick = { draft = TextFieldValue(phrase.text) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(phrase.text, style = MaterialTheme.typography.bodyLarge)
                                    Text(phrase.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        item { TextButton({ draft = TextFieldValue("") }) { Text(stringResource(R.string.phrase_write_own)) } }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                    Text(stringResource(R.string.phrase_edit_hint), style = MaterialTheme.typography.bodyMedium)
                    }
                    item {
                        OutlinedTextField(draft!!, { draft = it.copy(text = it.text.take(500)) }, label = { Text(stringResource(R.string.phrase_yours)) }, minLines = 3, maxLines = 7, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Text(stringResource(R.string.phrase_count, draft!!.text.length), style = MaterialTheme.typography.bodySmall)
                    }
                    item { TextButton({ draft = null }) { Text(stringResource(R.string.phrase_back)) } }
                    }
                }
                if (draft != null) {
                    val value = draft!!
                    val selection = value.selection
                    val selected = if (selection.collapsed) value.text else value.text.substring(selection.min, selection.max)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ onPick(selected); onDismiss() }, enabled = selected.isNotBlank(), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(if (selection.collapsed) stringResource(R.string.phrase_use) else stringResource(R.string.phrase_use_selection, selected.length))
                    }
                    TextButton(onDismiss, Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.action_close)) }
                    }
                } else {
                    TextButton(onDismiss, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.action_close)) }
                }
            }
            }
        }
    }
}
