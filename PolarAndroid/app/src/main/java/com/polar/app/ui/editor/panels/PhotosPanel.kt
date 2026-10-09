package com.polar.app.ui.editor.panels

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.ui.components.SymmetricActions
import com.polar.app.model.PhotoAsset
import com.polar.app.ui.theme.PolarColors

@Composable
fun PhotosPanel(
    photos: List<PhotoAsset>,
    used: Set<String>,
    lowRes: Set<String>,
    fairRes: Set<String> = emptySet(),
    missingPhotos: Int,
    thumbnail: suspend (PhotoAsset) -> ImageBitmap?,
    onAdd: () -> Unit,
    onFill: () -> Unit,
    onPlace: (String) -> Unit,
    onSelect: () -> Unit = {}
) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = (maxWidth / 84.dp).toInt().coerceIn(3, 6)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    if (missingPhotos > 0) {
                        Surface(color = PolarColors.warningContainer, shape = MaterialTheme.shapes.medium) {
                            Text(stringResource(R.string.photos_missing, missingPhotos), color = PolarColors.onWarningContainer,
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                        }
                    }
                    OutlinedButton(onSelect, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Seleccionar fotos de esta hoja") }
                    Text(stringResource(R.string.photos_summary,pluralStringResource(R.plurals.photos_count,photos.size,photos.size),used.size),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    SymmetricActions {
                        FilledTonalButton(onClick=onAdd,modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Icon(Icons.Filled.Add,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(stringResource(R.string.photos_add)) }
                        if(photos.isNotEmpty()) FilledTonalButton(onClick=onFill,modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Icon(Icons.Filled.AutoAwesome,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(stringResource(R.string.photos_fill)) }
                    }
                }
            }
            itemsIndexed(photos, key = { _, p -> p.id }) { i, photo ->
                val label = context.getString(R.string.photos_item, i + 1) +
                    (if (photo.id in used) context.getString(R.string.photos_in_use) else "") +
                    (if (photo.id in lowRes) context.getString(R.string.photos_low) else if (photo.id in fairRes) context.getString(R.string.photos_fair) else "")
                val image by produceState<ImageBitmap?>(null, photo.id) { value = thumbnail(photo) }
                Box(
                    Modifier.aspectRatio(1f).clip(MaterialTheme.shapes.small).background(PolarColors.table)
                        .clickable { onPlace(photo.id) }.semantics { contentDescription = label }
                ) {
                    image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    if (photo.id in used) Box(
                        Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) }
                    if (photo.id in lowRes) Surface(
                        color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    ) { Text(stringResource(R.string.photos_low_badge), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp)) }
                    else if (photo.id in fairRes) Surface(
                        color = PolarColors.warningContainer, shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    ) { Text(stringResource(R.string.photos_fair_badge), color = PolarColors.onWarningContainer, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp)) }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.photos_tip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
