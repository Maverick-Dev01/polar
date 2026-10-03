package com.polar.app.ui.catalog

import com.polar.app.ui.components.PolarChip
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextOverflow
import com.polar.app.ui.theme.Spacing
import com.polar.app.ui.components.SymmetricActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.engine.Thumbnailer
import com.polar.app.model.*
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogContent(
    title: String,
    current: TemplateStyle?,
    thumbnails: Thumbnailer,
    showImport: Boolean,
    onPick: (TemplateStyle) -> Unit,
    onImportTemplate: (Uri) -> Unit,
    onOpenPolar: (Uri) -> Unit,
    onBack: () -> Unit
) {
    var category by rememberSaveable { mutableStateOf<DesignCategory?>(null) }
    val styles = remember(category) {
        TemplateStyle.entries.filter { it != TemplateStyle.IMPORTED && (category == null || it.category == category) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(onImportTemplate) }
    val pickPolar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onOpenPolar) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title,maxLines=2,modifier=Modifier.semantics { contentDescription=title }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } }
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val columns = when { maxWidth >= 1000.dp -> 5; maxWidth >= 700.dp -> 4; maxWidth >= 540.dp -> 3; else -> 2 }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = if(maxWidth>=840.dp) Spacing.l else Spacing.m, end = if(maxWidth>=840.dp) Spacing.l else Spacing.m, bottom = Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.grid),
                verticalArrangement = Arrangement.spacedBy(Spacing.grid)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        item { PolarChip(category == null, { category = null }, { Text(stringResource(R.string.catalog_all)) },modifier=Modifier.heightIn(min=48.dp)) }
                        items(DesignCategory.entries) { c -> PolarChip(category == c, { category = c }, { Text(c.displayName) },modifier=Modifier.heightIn(min=48.dp)) }
                    }
                }
                items(styles, key = { it.name }) { style ->
                    DesignCard(style, selected = style == current, thumbnails = thumbnails, onClick = { onPick(style) })
                }
                if (showImport) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.padding(top = 8.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.catalog_own_title), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.catalog_own_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SymmetricActions {
                                    OutlinedButton(onClick = { pickImage.launch("image/*") },modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                                        Icon(Icons.Outlined.Upload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.catalog_import))
                                    }
                                    OutlinedButton(onClick = { pickPolar.launch(arrayOf("*/*")) },modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                                        Icon(Icons.Outlined.FileOpen, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.catalog_open_polar))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignCard(style: TemplateStyle, selected: Boolean, thumbnails: Thumbnailer, onClick: () -> Unit) {
    val px = with(LocalDensity.current) { 120.dp.roundToPx() }
    val image by produceState<ImageBitmap?>(null, style, px) {
        value = withContext(Dispatchers.Default) { thumbnails.styleCard(style, px).asImageBitmap() }
    }
    val label = stringResource(R.string.catalog_design, style.displayName)
    OutlinedCard(
        onClick = onClick,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label. ${style.description}"; this.selected = selected }
    ) {
        Column(Modifier.padding(Spacing.grid), verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.2f).background(PolarColors.table, MaterialTheme.shapes.small).padding(Spacing.grid),
                contentAlignment = Alignment.Center
            ) {
                image?.let { Image(it,null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize()) }
                if(selected) Icon(Icons.Filled.Check,null,Modifier.align(Alignment.TopEnd).size(20.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(style.displayName, style = MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(style.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
