package com.polar.app.ui.home

import com.polar.app.ui.components.PolarChip
import com.polar.app.ui.theme.Spacing
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.app.R
import com.polar.app.data.ProjectMeta
import com.polar.app.ui.Share
import com.polar.app.ui.components.PolaroidStack
import com.polar.app.ui.theme.BrandStyle
import com.polar.app.ui.theme.HandwrittenStyle
import com.polar.app.ui.theme.PaperColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    thumbnailFile: (String) -> File?,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuFor by remember { mutableStateOf<ProjectMeta?>(null) }
    var renaming by remember { mutableStateOf<ProjectMeta?>(null) }
    val deletedLabel = stringResource(R.string.home_deleted, "%s")
    val undoLabel = stringResource(R.string.action_undo)
    val shareUnavailable = stringResource(R.string.share_unavailable)
    val newDesignLabel = stringResource(R.string.home_new)

    LifecycleResumeEffect(Unit) { vm.refresh(); onPauseOrDispose { } }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is HomeEvent.Deleted -> scope.launch {
                    val r = snackbar.showSnackbar(deletedLabel.format(e.name), undoLabel, duration = SnackbarDuration.Long)
                    if (r == SnackbarResult.ActionPerformed) vm.undoDelete(e.id)
                }
                is HomeEvent.Message -> scope.launch { snackbar.showSnackbar(e.text.resolve(context)) }
                is HomeEvent.SharePolar -> if (!Share.file(context, e.file, "application/octet-stream", e.file.name)) {
                    scope.launch { snackbar.showSnackbar(shareUnavailable) }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                modifier = Modifier.semantics { contentDescription = newDesignLabel },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(newDesignLabel) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val columns = when {
                maxWidth >= 1200.dp -> 5
                maxWidth >= 840.dp -> 4
                maxWidth >= 560.dp -> 3
                else -> 2
            }
            val side = if (maxWidth >= 840.dp) 24.dp else 16.dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = side, end = side, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Header(state, onSettings, vm::setQuery, vm::setSort)
                }
                if (state.loaded && state.all.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyLibrary(onNew) }
                } else if (state.loaded && state.visible.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.home_no_results, state.query.trim()),
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                        )
                    }
                }
                items(state.visible, key = { it.id }) { meta ->
                    ProjectPolaroid(meta,thumbnailFile(meta.id),{onOpen(meta.id)},{menuFor=meta}) {
                        DropdownMenu(expanded=menuFor?.id==meta.id,onDismissRequest={menuFor=null},modifier=Modifier.width(288.dp)) {
                            Text(meta.name,Modifier.padding(16.dp),style=MaterialTheme.typography.titleMedium,maxLines=2)
                            val actions=listOf(
                                Triple(Icons.Outlined.FolderOpen,R.string.home_open,{ menuFor=null;onOpen(meta.id) }),
                                Triple(Icons.Outlined.Edit,R.string.home_rename,{ menuFor=null;renaming=meta }),
                                Triple(Icons.Outlined.ContentCopy,R.string.home_duplicate,{ menuFor=null;vm.duplicate(meta.id) }),
                                Triple(Icons.Outlined.Share,R.string.home_share,{ menuFor=null;vm.sharePolar(meta.id) }),
                                Triple(Icons.Outlined.Delete,R.string.home_delete,{ menuFor=null;vm.delete(meta.id) })
                            )
                            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                actions.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                        pair.forEach { (icon,label,action) ->
                                            OutlinedButton(onClick=action,modifier=Modifier.weight(1f).heightIn(min=88.dp),shape=MaterialTheme.shapes.medium,contentPadding=PaddingValues(8.dp)) {
                                                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                                    Icon(icon,null,Modifier.size(24.dp))
                                                    Text(stringResource(label),style=MaterialTheme.typography.labelMedium,minLines=2,maxLines=2,textAlign=TextAlign.Center)
                                                }
                                            }
                                        }
                                        if(pair.size==1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renaming?.let { meta ->
        var text by remember(meta.id) { mutableStateOf(meta.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.home_rename)) },
            text = { OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.home_rename_label)) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.rename(meta.id, text); renaming = null }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: HomeUiState, onSettings: () -> Unit, onQuery: (String) -> Unit, onSort: (SortMode) -> Unit) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = BrandStyle, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Tune, stringResource(R.string.settings_title)) }
        }
        if (state.all.isNotEmpty()) {
            OutlinedTextField(
                value = state.query, onValueChange = onQuery, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(stringResource(R.string.home_search)) },
                shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PolarChip(state.sort == SortMode.RECENT, { onSort(SortMode.RECENT) }, { Text(stringResource(R.string.home_sort_recent)) })
                PolarChip(state.sort == SortMode.NAME, { onSort(SortMode.NAME) }, { Text(stringResource(R.string.home_sort_name)) })
                Text(pluralStringResource(R.plurals.home_count, state.visible.size, state.visible.size), maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProjectPolaroid(meta: ProjectMeta, thumb: File?, onOpen: () -> Unit, onMenu: () -> Unit, menuContent: @Composable ()->Unit) {
    val image by produceState<ImageBitmap?>(null, meta.id, meta.updatedAtEpochMs) {
        value = withContext(Dispatchers.IO) { thumb?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() } }
    }
    val tilt = remember(meta.id) { (Math.floorMod(meta.id.hashCode(), 5) - 2) * 0.8f }
    val context = LocalContext.current
    val sheets = pluralStringResource(R.plurals.home_sheets, meta.sheets, meta.sheets)
    val whenText = remember(meta.updatedAtEpochMs) { relativeTime(context.resources, System.currentTimeMillis(), meta.updatedAtEpochMs) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
        Column(
            Modifier.rotate(tilt).shadow(6.dp, RoundedCornerShape(3.dp)).background(PaperColors.Paper, RoundedCornerShape(3.dp))
                .clickable(onClick = onOpen).semantics { contentDescription = context.getString(R.string.home_open) + " " + meta.name }
                .padding(start = 8.dp, end = 8.dp, top = 8.dp)
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(PaperColors.Table)) {
                image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Text(
                meta.name, style = HandwrittenStyle, color = PaperColors.InkDark, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical=8.dp)
            )
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(meta.style.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$sheets · $whenText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines=2,maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = onMenu) { Icon(Icons.Filled.MoreVert, stringResource(R.string.home_options, meta.name)) }
                menuContent()
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onNew: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PolaroidStack(caption = stringResource(R.string.app_name))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onNew) { Text(stringResource(R.string.home_new)) }
    }
}
