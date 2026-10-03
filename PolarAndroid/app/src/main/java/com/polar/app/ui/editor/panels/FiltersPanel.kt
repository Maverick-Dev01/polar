package com.polar.app.ui.editor.panels

import com.polar.app.ui.components.PolarChip
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.core.look.LookResolver
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import com.polar.app.ui.components.LensRing
import com.polar.app.ui.editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

fun lookNameResource(preset: String): Int = when(preset) {
    "bw"->R.string.look_bw; "film"->R.string.look_film; "sepia"->R.string.look_sepia; "warm"->R.string.look_warm
    "cool"->R.string.look_cool; "faded"->R.string.look_faded; "vivid"->R.string.look_vivid; else->R.string.look_original
}
private val thumbnails=LruCache<String,Bitmap>(32)
@Composable
fun FilterPresets(state: EditorUiState,vm: EditorViewModel,container: AppContainer) {
    val look=vm.currentLook()
    val slot=state.selectedSlot?.takeIf { state.project.placements.getOrNull(it)!=null } ?: state.project.placements.indexOfFirst { it!=null }.takeIf { it>=0 }
    LazyRow(modifier=Modifier.testTag("filter-presets"),horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(horizontal=16.dp)) {
        items(PhotoLook.PRESETS) { preset ->
            val selected=look.preset==preset; val name=stringResource(lookNameResource(preset))
            val sample=look.copy(preset=preset)
            val key="${state.project.hashCode()}/$slot/$sample/${state.templateVersion}"
            val bitmap by produceState<Bitmap?>(thumbnails[key],key) {
                value=withContext(Dispatchers.Default) {
                    thumbnails[key] ?: slot?.let {
                        PolarRenderer.photoPreview(ProjectEdits.setPhotoLook(state.project,sample,it),it,120,{ a->container.bitmaps.load(a.path,512) },vm.templateBitmap,container.fonts)
                            .also { thumbnails.put(key,it) }
                    }
                }
            }
            Surface(onClick={vm.setLook(sample)},modifier=Modifier.width(104.dp).semantics { role=Role.RadioButton; this.selected=selected;contentDescription=name },
                shape=MaterialTheme.shapes.medium,border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(64.dp),contentAlignment=Alignment.Center) {
                        bitmap?.let { Image(it.asImageBitmap(),null,Modifier.fillMaxSize()) }
                        if(selected) Icon(Icons.Filled.Check,null,Modifier.align(Alignment.TopEnd).size(20.dp),tint=MaterialTheme.colorScheme.primary)
                    }
                    Text(name,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersPanel(state: EditorUiState,vm: EditorViewModel,container: AppContainer) {
    val look=vm.currentLook()
    var adjustments by remember { mutableStateOf(false) }
    Column(Modifier.testTag("filters-panel-scroll").fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        FlowRow(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            LookScope.entries.forEach { scope ->
                PolarChip(selected=scope==state.lookScope,onClick={vm.setLookScope(scope)},label={Text(stringResource(when(scope){LookScope.ALL->R.string.look_all;LookScope.PAGE->R.string.look_page;LookScope.PAGES->R.string.look_pages;LookScope.PHOTO->R.string.look_photo}))},modifier=Modifier.heightIn(min=48.dp))
            }
        }
        if(state.lookScope==LookScope.PHOTO) Text(stringResource(R.string.look_scope_photo,(state.selectedSlot ?: 0)+1,(state.selectedCard ?: 0)+1),Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall)
        if(state.lookScope==LookScope.PAGES) LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(state.project.pageCount) { page -> PolarChip(selected=page in state.lookPages,onClick={vm.toggleLookPage(page)},label={Text(stringResource(R.string.look_page_number,page+1))},modifier=Modifier.heightIn(min=48.dp)) }
        }
        val targetSlots=state.project.placements.indices.filter { slot->state.project.placements[slot]!=null && when(state.lookScope) {
            LookScope.ALL->true;LookScope.PAGE->slot/state.project.settings.capacity==state.page
            LookScope.PAGES->slot/state.project.settings.capacity in state.lookPages;LookScope.PHOTO->slot==state.selectedSlot
        } }
        if(targetSlots.map {LookResolver.resolve(state.project,it)}.distinct().size>1) Text(stringResource(R.string.look_mixed),Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        FilterPresets(state,vm,container)
        Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            LensRing(stringResource(R.string.look_intensity),look.intensity,0.0..1.0,"${(look.intensity*100).roundToInt()} %",.05,{vm.setLook(look.copy(intensity=it))},vm::beginGesture,vm::endGesture)
            TextButton(onClick={adjustments=!adjustments},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(stringResource(R.string.look_adjustments)) }
            if(adjustments) {
                listOf(Triple(R.string.look_light,look.light,-1.0..1.0),Triple(R.string.look_contrast,look.contrast,-1.0..1.0),Triple(R.string.look_warmth,look.warmth,-1.0..1.0),Triple(R.string.look_grain,look.grain,0.0..1.0)).forEach { (label,value,range) ->
                    LensRing(stringResource(label),value,range,"${(value*100).roundToInt()} %",.05,{ v->vm.setLook(when(label) {R.string.look_light->look.copy(light=v);R.string.look_contrast->look.copy(contrast=v);R.string.look_warmth->look.copy(warmth=v);else->look.copy(grain=v)}) },vm::beginGesture,vm::endGesture)
                }
            }
            OutlinedButton(onClick={vm.setLook(PhotoLook())},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(stringResource(R.string.look_remove)) }
            if(state.lookScope!=LookScope.ALL) Button(onClick=vm::applyLookToAll,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text(stringResource(R.string.look_apply_all)) }
            if(state.project.settings.style.suggestedPhotoPreset!=null) {
                Text(stringResource(R.string.look_suggestion),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={vm.setLook(look.copy(preset="bw"))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(stringResource(R.string.look_apply)) }
            }
            Text(stringResource(R.string.look_compare),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
