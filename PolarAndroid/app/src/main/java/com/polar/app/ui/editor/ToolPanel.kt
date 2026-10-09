package com.polar.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.outlined.*
import com.polar.app.model.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.app.data.AppSettings
import androidx.compose.ui.graphics.asImageBitmap
import com.polar.app.ui.editor.panels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.help.HelpIds
import com.polar.app.ui.help.helpTarget

@Composable
fun ToolPanel(tool: Tool, state: EditorUiState, vm: EditorViewModel, container: AppContainer, compact: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var choosePhrase by remember { mutableStateOf(false) }
    if (choosePhrase) PhraseDialog(vm::setText) { choosePhrase = false; vm.setTool(Tool.TEXT); vm.setTrayExpanded(true) }
    var expandedText by remember { mutableStateOf(false) }
    if (expandedText) TextEditDialog(state, container, vm::setText) { vm.endGesture(); expandedText = false }
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris->vm.addPhotos(uris.map { it.toString() }) }
    val addPhotos={ photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val folderPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri->uri?.let { vm.addFolder(it.toString()) } }
    val addFolder={ folderPicker.launch(null) }
    val title = when (tool) {
        Tool.PHOTOS -> stringResource(R.string.tool_photos)
        Tool.FILTERS -> stringResource(R.string.tool_filters)
        Tool.DESIGN -> stringResource(R.string.tool_design)
        Tool.TEXT -> if (state.editCard != null) stringResource(R.string.text_title_card, state.selectedCardNumber ?: 1) else stringResource(R.string.text_title_all)
        Tool.PAPER -> stringResource(R.string.tool_paper)
    }
    Surface(
        modifier = modifier.helpTarget(HelpIds.panel(tools.first { it.tool == tool }.helpId.removePrefix("tool."))),
        shape = RoundedCornerShape(0.dp),
        tonalElevation = 0.dp, shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp).pointerInput(Unit) {
                detectVerticalDragGestures { change, amount -> change.consume(); if(kotlin.math.abs(amount)>6) vm.setTrayExpanded(amount<0) }
            },verticalAlignment=Alignment.CenterVertically) {
                Text(title,style=MaterialTheme.typography.titleSmall,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                if(compact) {
                    TextButton(onClick={vm.setTrayExpanded(!state.trayExpanded)},modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(if(state.trayExpanded) R.string.tray_compact else R.string.tray_expand)) }
                    IconButton(onClick=onClose) { Icon(Icons.Filled.Close,stringResource(R.string.tool_close)) }
                }
            }
            if(compact && !state.trayExpanded) {
                if(tool==Tool.FILTERS) FilterPresets(state,vm,container) else CompactTools(tool,state,vm,addPhotos,addFolder)
                return@Column
            }
            Box(Modifier.weight(1f)) {
                when (tool) {
                    Tool.PHOTOS -> {
                        val lowResIds = remember(state.project) {
                            vm.lowResSlots().mapNotNull { state.project.placements.getOrNull(it)?.assetID }.toSet()
                        }
                        val fairResIds = remember(state.project) {
                            vm.reviewSlots().mapNotNull { state.project.placements.getOrNull(it)?.assetID }.toSet() - lowResIds
                        }
                        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
                            vm.addPhotos(uris.map { it.toString() })
                        }
                        if (state.multiSelecting) BatchPhotosPanel(state, vm, container) else PhotosPanel(
                            photos = state.project.photos.filter { !it.isBackground }, used = state.usedAssetIds, lowRes = lowResIds, fairRes = fairResIds,
                            missingPhotos = state.missingPhotos,
                            thumbnail = { a -> withContext(Dispatchers.IO) { container.bitmaps.load(a.path, 256)?.asImageBitmap() } },
                            onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onAddFolder = addFolder,
                            onFill = vm::fillAll, onPlace = vm::placePhoto, onSelect = { vm.setMultiSelecting(true) }
                        )
                    }
                    Tool.FILTERS -> FiltersPanel(state,vm,container)
                    Tool.DESIGN -> DesignPanel(state, DesignCallbacks(
                        onScope = vm::setDesignScope, onChangeDesign = { vm.setMode(EditorMode.CHANGE_DESIGN) }, onUseMyTemplate = vm::openMyTemplates, onAccent = vm::setAccent, onLayout = vm::applyLayout,
                        onMood = vm::applyMood, onSuggested = { val roles = state.project.settingsForPage(state.page).style.textRoles; vm.setTextRole(roles.firstOrNull { it == TextRole.CAPTION } ?: roles.firstOrNull { it == TextRole.SUBTITLE } ?: roles.firstOrNull() ?: TextRole.TITLE); choosePhrase = true }, onFormat = vm::setCardFormat,
                        onGrid = vm::setGrid, onGap = { vm.setGap(it.toDouble()) }, onRounded = vm::setRounded,
                        onYear = vm::setCalendarYear, onHighlight = vm::setHighlightDate, onSpecialDate = vm::setSpecialDate,
                        onEditRegions = vm::setEditingRegions,
                        onRegion = { change -> state.selectedSlot?.let { vm.editRegion(it % state.project.settings.capacity, change) } },
                        onAddRegion = vm::addRegion,
                        onRemoveRegion = { state.selectedSlot?.let { vm.removeRegion(it % state.project.settings.capacity) } },
                        onGestureStart = vm::beginGesture, onGestureEnd = vm::endGesture
                    ))
                    Tool.TEXT -> TextPanel(textPanelState(state), TextCallbacks(
                        onExpand = { vm.beginGesture(); expandedText = true }, onRole = vm::setTextRole, onScope = vm::setTextScope, onText = vm::setText, onRoleText = vm::setTextFor,
                        onFocus = { focused -> if (focused) vm.beginGesture() else vm.endGesture() },
                        onRevert = vm::clearOwnText, onApplyAll = vm::applyTextToAll, onAppearance = vm::editAppearance,
                        onReset = vm::resetAppearance, onDateSource = vm::setDateSource, onChosenDate = vm::setChosenDate,
                        onDateStyle = vm::setDateStyle, onSongUrl = vm::setSongUrl,
                        onGestureStart = vm::beginGesture, onGestureEnd = vm::endGesture
                    ))
                    Tool.PAPER -> {
                        val app by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
                        PaperPanel(state.project.settings, app.units, PaperCallbacks(
                            onPaper = vm::setPaper, onOrientation = vm::setOrientation, onCustom = vm::setCustomPaper,
                            onGuides = vm::setGuides, onCutStyle = vm::setCutStyle, onBorders = vm::setBorders,
                            onMargin = { vm.setMargin(it.toDouble()) }, onGestureStart = vm::beginGesture, onGestureEnd = vm::endGesture
                        ))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTools(tool: Tool,state: EditorUiState,vm: EditorViewModel,onAdd: ()->Unit,onAddFolder: ()->Unit) {
    val entries: List<Pair<String,()->Unit>> = when(tool) {
        Tool.PHOTOS -> listOf(stringResource(R.string.editor_add_photos) to onAdd,stringResource(R.string.photos_add_folder) to onAddFolder,stringResource(R.string.photos_fill) to vm::fillAll, stringResource(R.string.photos_select_short) to { vm.setMultiSelecting(true) })
        Tool.DESIGN -> listOf(1,2,4,9).map { stringResource(R.string.editor_layout_per_sheet,it) to { vm.applyLayout(it) } } + (stringResource(R.string.catalog_change) to { vm.setMode(EditorMode.CHANGE_DESIGN) })
        Tool.TEXT -> state.project.settings.style.textRoles.map { it.displayName to { vm.setTextRole(it); vm.setTrayExpanded(true) } }
        Tool.PAPER -> PaperSize.entries.map { it.displayName to { vm.setPaper(it) } }
        Tool.FILTERS -> emptyList()
    }
    LazyRow(contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        items(entries.size) { i ->
            val (label,action)=entries[i]
            OutlinedButton(onClick=action,modifier=Modifier.width(104.dp).heightIn(min=104.dp).semantics { contentDescription=label },shape=MaterialTheme.shapes.medium,contentPadding=PaddingValues(8.dp)) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(tools.first { it.tool==tool }.icon,null,Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp));Text(label,style=MaterialTheme.typography.labelSmall,minLines=2,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center,overflow=TextOverflow.Ellipsis)
                }
            }
        }
    }
}
