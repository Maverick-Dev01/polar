package com.polar.app.ui.editor.panels

import com.polar.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.editor.EditorViewModel
import com.polar.app.ui.components.SymmetricActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BatchPhotosPanel(state: EditorUiState, vm: EditorViewModel, container: AppContainer) {
    val cap = state.project.settings.capacity
    val slots = state.project.placements.indices.filter { it / cap == state.page && state.project.placements[it] != null }
    LazyVerticalGrid(GridCells.Adaptive(88.dp), Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.batch_selected, state.selectedSlots.size, state.page + 1), style = MaterialTheme.typography.titleSmall) }
        item(span = { GridItemSpan(maxLineSpan) }) {
        SymmetricActions {
            TextButton(vm::selectAllOnPage, Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.batch_select_all)) }
            TextButton({ vm.setMultiSelecting(false) }, Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.batch_done)) }
        }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
        SymmetricActions {
            OutlinedButton(vm::removeSelectedPhotos, Modifier.weight(1f).heightIn(min = 48.dp), enabled = state.selectedSlots.isNotEmpty()) { Text(stringResource(R.string.batch_remove)) }
            OutlinedButton(vm::copySelectedPhotos, Modifier.weight(1f).heightIn(min = 48.dp), enabled = state.selectedSlots.isNotEmpty()) { Text(stringResource(R.string.batch_copy)) }
        }
        }
            items(slots, key = { it }) { slot ->
                val photo = state.project.asset(state.project.placements[slot])!!
                val image by produceState<ImageBitmap?>(null, photo.path) { value = withContext(Dispatchers.IO) { container.bitmaps.load(photo.path, 256)?.asImageBitmap() } }
                OutlinedCard(onClick = { vm.selectSlot(slot) }, modifier = Modifier.aspectRatio(1f)) {
                    Box(Modifier.fillMaxSize()) {
                        image?.let { Image(it, stringResource(R.string.batch_photo, slot % cap + 1), Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        Checkbox(slot in state.selectedSlots, { vm.selectSlot(slot) })
                    }
                }
            }
    }
}
