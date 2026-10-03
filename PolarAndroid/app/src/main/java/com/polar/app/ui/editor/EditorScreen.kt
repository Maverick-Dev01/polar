package com.polar.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.polar.app.ui.rememberReduceMotion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.export.FilePrintAdapter
import com.polar.app.export.PrintMedia
import com.polar.app.ui.LayoutKind
import com.polar.app.ui.LocalLayout
import com.polar.app.ui.Share
import com.polar.app.ui.catalog.CatalogContent
import com.polar.app.ui.theme.PolarColors
import com.polar.app.model.suggestedPhotoPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(vm: EditorViewModel, container: AppContainer, notice: String?, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(state.tool, state.mode, state.multiSelecting) {
        focus.clearFocus(); keyboard?.hide()
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.action_undo)
    val shareUnavailable = stringResource(R.string.share_unavailable)
    val actionFailed = stringResource(R.string.action_failed)
    val savedLabel = stringResource(R.string.finish_saved)
    val openLabel = stringResource(R.string.finish_open)
    // rememberSaveable: sobreviven a una rotación mientras el selector "Guardar como" está abierto.
    var pendingAction by rememberSaveable { mutableStateOf(ExportAction.SAVE) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    fun showMessage(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    fun copyTo(uri: android.net.Uri?, mime: String) {
        val file = pendingPath?.let { java.io.File(it) } ?: return
        if (uri == null) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri) ?: throw java.io.IOException("Sin flujo de salida")
                    out.use { o -> file.inputStream().use { it.copyTo(o) } }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Polar", "No se pudo guardar el archivo", e)
                showMessage(actionFailed)
                return@launch
            }
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(savedLabel, openLabel)
            if (r == SnackbarResult.ActionPerformed && !Share.open(context, uri, mime)) showMessage(shareUnavailable)
        }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { copyTo(it, "application/pdf") }
    val saveJpg = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { copyTo(it, "image/jpeg") }
    val savePng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { copyTo(it, "image/png") }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushAsync() }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it, duration = SnackbarDuration.Long) } }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is EditorEvent.Message -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val timeout = launch { delay(1000); snackbar.currentSnackbarData?.dismiss() }
                    val r = snackbar.showSnackbar(
                        e.text.resolve(context), if (e.undoable) undoLabel else null,
                        duration = SnackbarDuration.Indefinite
                    )
                    timeout.cancel()
                    if (r == SnackbarResult.ActionPerformed) vm.undo()
                }
                is EditorEvent.Exported -> {
                    pendingPath = e.file.absolutePath
                    when (pendingAction) {
                        ExportAction.PRINT -> try {
                            val pm = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                            pm.print(e.file.nameWithoutExtension, FilePrintAdapter(e.file, e.file.name), PrintMedia.attributes(vm.state.value.project.settings))
                        } catch (ex: Exception) {
                            Log.w("Polar", "No se pudo abrir la impresión", ex)
                            showMessage(actionFailed)
                        }
                        ExportAction.SAVE -> when (e.mime) {
                            "application/pdf" -> savePdf.launch(e.file.name)
                            "image/jpeg" -> saveJpg.launch(e.file.name)
                            else -> savePng.launch(e.file.name)
                        }
                        ExportAction.SHARE -> if (!Share.file(context, e.file, e.mime, e.file.name)) showMessage(shareUnavailable)
                    }
                }
            }
        }
    }

    BackHandler { onBack() }
    BackHandler(enabled = state.mode != EditorMode.EDIT) { vm.setMode(EditorMode.EDIT) }
    BackHandler(enabled = state.mode == EditorMode.EDIT && state.tool != null && LocalLayout.current == LayoutKind.COMPACT) { vm.setTool(null) }
    val expandedLayout = LocalLayout.current == LayoutKind.EXPANDED
    // En pantalla ancha el panel es fijo (tool nunca es null): Atrás quita primero la selección.
    BackHandler(enabled = state.mode == EditorMode.EDIT && state.selectedSlot != null && (state.tool == null || expandedLayout)) { vm.clearSelection() }
    BackHandler(enabled = state.mode == EditorMode.EDIT && state.editingRegions) { vm.setEditingRegions(false) }

    if (state.loadFailed) {
        LoadFailed(onBack)
        return
    }

    state.backgroundError?.let { error ->
        AlertDialog(onDismissRequest = vm::dismissBackgroundError, title = { Text("No se pudo cambiar el fondo") }, text = { Text(error) }, confirmButton = { TextButton(vm::dismissBackgroundError) { Text("Aceptar") } })
    }
    when (state.mode) {
        EditorMode.CHANGE_DESIGN -> CatalogContent(
            title = stringResource(R.string.catalog_change), current = state.selectedCard?.let { state.project.settingsForCard(it).style } ?: state.project.settingsForPage(state.page).style,
            thumbnails = container.thumbnails, showImport = false,
            onPick = vm::selectStyle, onImportTemplate = {}, onOpenPolar = {},
            onBack = { vm.setMode(EditorMode.EDIT) }
        )
        EditorMode.CROP -> CropScreen(state, vm, container)
        EditorMode.FINISH -> FinishScreen(state, vm, container, snackbar) { action, format ->
            pendingAction = action
            when (format) {
                ExportFormat.PDF, ExportFormat.PDF_LOSSLESS -> vm.exportPdf(format == ExportFormat.PDF)
                ExportFormat.PNG -> vm.exportPng()
                ExportFormat.JPEG -> vm.exportJpg()
            }
        }
        else -> EditorLayout(state, vm, container, snackbar, onBack)
    }
}

@Composable
private fun LoadFailed(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.editor_load_failed), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorLayout(state: EditorUiState, vm: EditorViewModel, container: AppContainer, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val expanded = LocalLayout.current == LayoutKind.EXPANDED
    val reduceMotion = rememberReduceMotion()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        vm.addPhotos(uris.map { it.toString() })
    }
    val pickPhotos = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    var renaming by remember { mutableStateOf(false) }
    val noPhotos = state.project.photos.isEmpty()
    val hasSelection = state.selectedSlot != null
    val selectedHasPhoto = state.selectedPlacement != null

    Scaffold(
        modifier = Modifier.imePadding(),
        // Compacto: la NavigationBar ya aplica el margen inferior del sistema; aquí sólo los laterales.
        contentWindowInsets = if (expanded) ScaffoldDefaults.contentWindowInsets
            else WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        // Compacto: aviso propio sobre la barra de herramientas (abajo); aquí sólo ancho y carga.
        snackbarHost = { if (expanded || state.loading) SnackbarHost(snackbar) },
        topBar = { EditorTopBar(state, vm, onBack, onRename = { renaming = true }) }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (expanded) {
            Row(Modifier.padding(padding).fillMaxSize()) {
                ToolRail(state.tool ?: Tool.PHOTOS) { vm.setTool(it) }
                Column(Modifier.weight(1f)) {
                    SheetArea(state, vm, container, Modifier.weight(1f))
                    FilmSuggestion(state,vm)
                    when {
                        noPhotos -> EmptyPhotosCta(pickPhotos)
                        hasSelection -> ContextBar(selectedHasPhoto, { vm.setTool(Tool.PHOTOS) }, { vm.setMode(EditorMode.CROP) }, vm::rotateSelected, vm::openTextForSelected, vm::removeSelectedPhoto)
                    }
                }
                ToolPanel(state.tool ?: Tool.PHOTOS, state, vm, container, compact = false, onClose = {}, modifier = Modifier.width(360.dp).fillMaxHeight())
            }
        } else {
            var navBarHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            var lastTool by remember { mutableStateOf(state.tool ?: Tool.PHOTOS) }
            LaunchedEffect(state.tool) { state.tool?.let { lastTool=it } }
            BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                val trayHeight by animateDpAsState(if(state.trayExpanded) maxHeight*(if (WindowInsets.ime.getBottom(density) > 0) .80f else .55f) else minOf(180.dp, maxHeight*.6f),tween(if(reduceMotion) 0 else 240,easing=FastOutSlowInEasing),label="bandeja")
                Column(Modifier.fillMaxSize()) {
                    SheetArea(state, vm, container, Modifier.weight(1f))
                    FilmSuggestion(state,vm)
                    AnimatedVisibility(state.tool!=null,
                        enter=expandVertically(tween(if(reduceMotion) 0 else 240,easing=FastOutSlowInEasing)),
                        exit=shrinkVertically(tween(if(reduceMotion) 0 else 240,easing=FastOutSlowInEasing))) {
                        val tool=state.tool ?: lastTool
                        Column {
                            ToolNavBar(tool,{ vm.setTool(if(state.tool==it) null else it) },systemInset=false)
                            ToolPanel(tool,state,vm,container,true,{vm.setTool(null)},Modifier.fillMaxWidth().height(trayHeight))
                        }
                    }
                    when {
                        state.tool != null -> Unit
                        noPhotos -> EmptyPhotosCta(pickPhotos)
                        hasSelection -> ContextBar(selectedHasPhoto, { vm.setTool(Tool.PHOTOS) }, { vm.setMode(EditorMode.CROP) }, vm::rotateSelected, vm::openTextForSelected, vm::removeSelectedPhoto)
                        else -> Hint()
                    }
                    Box(Modifier.onSizeChanged { navBarHeight = with(density) { it.height.toDp() } }) {
                        if(state.tool==null) ToolNavBar(state.tool,{ vm.setTool(it) }) else Spacer(Modifier.navigationBarsPadding())
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = navBarHeight))
            }
        }
    }

    if (renaming) {
        var text by remember { mutableStateOf(state.project.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.editor_rename)) },
            text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text(stringResource(R.string.home_rename_label)) }) },
            confirmButton = { TextButton(onClick = { vm.rename(text); renaming = false }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(state: EditorUiState, vm: EditorViewModel, onBack: () -> Unit, onRename: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    TopAppBar(
        expandedHeight=if(LocalDensity.current.fontScale>=1.2f) 104.dp else 88.dp,
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.editor_back)) } },
        title = {
            Column(Modifier.clickable(onClick = onRename)) {
                Text(state.project.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,modifier=Modifier.semantics { contentDescription=state.project.name })
                Text(
                    state.project.settings.style.displayName + " · " + stringResource(when {
                        state.saveFailed -> R.string.editor_not_saved
                        state.saving || state.hasUnsavedChanges -> R.string.editor_saving
                        else -> R.string.editor_saved
                    }),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                )
            }
        },
        actions = {
            IconButton(onClick = vm::undo, enabled = state.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.action_undo)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.editor_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.action_redo)) }, { menu = false; vm.redo() }, enabled = state.canRedo,
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, null) })
                    DropdownMenuItem({ Text("Seleccionar varias fotos") }, { menu = false; vm.setMultiSelecting(true) })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_add_page)) }, { menu = false; vm.addPage() })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_clear_page)) }, { menu = false; vm.clearPage() })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_remove_page)) }, { menu = false; vm.removePage() })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_rename)) }, { menu = false; onRename() })
                }
            }
            Button(onClick = { vm.setMode(EditorMode.FINISH) }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.padding(end = 8.dp)) {
                Icon(Icons.Outlined.Print,stringResource(R.string.editor_print),Modifier.size(20.dp))
            }
        }
    )
}

@Composable
private fun Hint() {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.editor_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyPhotosCta(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(12.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.editor_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.editor_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onAdd) { Text(stringResource(R.string.editor_add_photos)) }
        }
    }
}

@Composable private fun FilmSuggestion(state: EditorUiState,vm: EditorViewModel) {
    if(state.project.settings.style.suggestedPhotoPreset!=null && state.project.settings.photoLook?.preset!="bw") {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.look_suggestion),style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f))
            TextButton(onClick=vm::applySuggestedLook,modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.look_apply)) }
        }
    }
}
