package com.polar.app.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.data.BitmapLoader
import com.polar.app.core.look.LookResolver
import com.polar.app.model.CutStyle
import com.polar.app.ui.components.SwitchRow
import com.polar.app.model.PhotoLook
import com.polar.app.ui.editor.panels.lookNameResource
import com.polar.app.ui.LayoutKind
import com.polar.app.ui.LocalLayout
import com.polar.app.ui.theme.PaperColors
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishScreen(state: EditorUiState, vm: EditorViewModel, container: AppContainer, snackbar: SnackbarHostState, onAction: (ExportAction, ExportFormat) -> Unit) {
    val p = state.project
    val low = remember(p) { vm.lowResSlots() }
    val empty = remember(p) { vm.emptySlotsOnUsedPages() }
    val expanded = LocalLayout.current == LayoutKind.EXPANDED
    val sheets = pluralStringResource(R.plurals.home_sheets, p.pageCount, p.pageCount)
    val photos = pluralStringResource(R.plurals.finish_photos, p.placedCount, p.placedCount)
    val paper = p.settings.paperSize.displayName + " " + p.settings.orientation.displayName.lowercase()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.finish_title)) },
            navigationIcon = { IconButton(onClick = { vm.setMode(EditorMode.EDIT) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } }
        )
    }) { padding ->
        val summary: @Composable ColumnScope.() -> Unit = {
            LazyRow(
                Modifier.fillMaxWidth().background(PolarColors.table, MaterialTheme.shapes.large).padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items((0 until minOf(p.pageCount, 12)).toList()) { page -> PageThumb(state, vm, container, page) }
            }
            Text(stringResource(R.string.finish_summary, sheets, photos, paper), style = MaterialTheme.typography.titleMedium)
            val filtered=p.placements.indices.filter { p.placements[it]!=null && !LookResolver.resolve(p,it).isNeutral }
            val general=p.settings.photoLook
            Text(if(filtered.isEmpty()) stringResource(R.string.look_summary_none)
                else if(general!=null && filtered.all { LookResolver.resolve(p,it)==general } && filtered.size==p.placedCount) stringResource(R.string.look_summary_all,stringResource(lookNameResource(general.preset)))
                else pluralStringResource(R.plurals.look_summary_count,filtered.size,filtered.size),style=MaterialTheme.typography.bodyMedium)
            OutlinedCard {
                if (low.isNotEmpty()) CheckRow(Icons.Outlined.Warning, PolarColors.warningContainer, PolarColors.onWarningContainer,
                    pluralStringResource(R.plurals.finish_low, low.size, low.size)) {
                    TextButton(onClick = { vm.clearSelection(); vm.selectSlot(low.first()); vm.setMode(EditorMode.CROP) }) { Text(stringResource(R.string.finish_review)) }
                }
                if (state.missingPhotos > 0) CheckRow(Icons.Outlined.ImageNotSupported, PolarColors.warningContainer, PolarColors.onWarningContainer,
                    pluralStringResource(R.plurals.finish_missing, state.missingPhotos, state.missingPhotos))
                if (empty > 0) CheckRow(Icons.Outlined.Info, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant,
                    pluralStringResource(R.plurals.finish_empty, empty, empty))
                CheckRow(Icons.Outlined.Check, PolarColors.successContainer, PolarColors.onSuccessContainer, stringResource(R.string.finish_ok))
            }
        }
        val actions: @Composable ColumnScope.() -> Unit = {
            SwitchRow("Guías para recortar", p.settings.cutGuides, vm::setGuides, "Incluyen toda la tarjeta: fotografía y texto.")
            if (p.settings.cutGuides) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(p.settings.cutStyle == CutStyle.CORNERS, { vm.setCutStyle(CutStyle.CORNERS) }, { Text("Esquinas") })
                    FilterChip(p.settings.cutStyle != CutStyle.CORNERS, { vm.setCutStyle(CutStyle.LINES) }, { Text("Líneas") })
                }
            }

            Button(onClick = { onAction(ExportAction.PRINT, ExportFormat.PDF) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp)) {
                Icon(Icons.Outlined.Print, null); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.finish_print))
            }
            ActionButton(Icons.Outlined.PictureAsPdf, stringResource(R.string.finish_save_pdf), stringResource(R.string.finish_save_pdf_hint), !state.busy) { onAction(ExportAction.SAVE, ExportFormat.PDF) }
            ActionButton(Icons.Outlined.Image, stringResource(R.string.finish_save_jpg), stringResource(R.string.finish_save_jpg_hint), !state.busy) { onAction(ExportAction.SAVE, ExportFormat.JPEG) }
            ActionButton(Icons.Outlined.Image, stringResource(R.string.finish_save_png), stringResource(R.string.finish_save_png_hint), !state.busy) { onAction(ExportAction.SAVE, ExportFormat.PNG) }
            ActionButton(Icons.Outlined.PictureAsPdf, stringResource(R.string.finish_save_pdf_lossless), stringResource(R.string.finish_save_pdf_lossless_hint), !state.busy) { onAction(ExportAction.SAVE, ExportFormat.PDF_LOSSLESS) }
            ActionButton(Icons.Outlined.Share, stringResource(R.string.finish_share), stringResource(R.string.finish_share_hint), !state.busy) { onAction(ExportAction.SHARE, ExportFormat.PDF) }
            if (state.busy) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.finish_preparing))
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.finish_tip, p.settings.paperSize.displayName), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        val scroll = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(if(expanded) 24.dp else 16.dp)
        if (expanded) {
            Row(scroll, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(16.dp), content = summary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), content = actions)
            }
        } else {
            Column(scroll, verticalArrangement = Arrangement.spacedBy(16.dp)) { summary(); actions() }
        }
    }
}

@Composable
private fun PageThumb(state: EditorUiState, vm: EditorViewModel, container: AppContainer, page: Int) {
    val image by produceState<ImageBitmap?>(null, state.project, page) {
        value = withContext(Dispatchers.Default) {
            container.thumbnails.page(state.project, page, 220, { container.bitmaps.load(it.path, BitmapLoader.PREVIEW_MAX) }, vm.templateBitmap).asImageBitmap()
        }
    }
    val ratio = (state.project.settings.paperSizePoints.width / state.project.settings.paperSizePoints.height).toFloat()
    Box(Modifier.height(150.dp).aspectRatio(ratio).shadow(3.dp).background(PaperColors.Paper)) {
        image?.let { Image(it, null, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun CheckRow(icon: ImageVector, bg: Color, fg: Color, text: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(32.dp).background(bg, MaterialTheme.shapes.extraLarge), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)) }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, hint: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min=56.dp)) {
        Icon(icon, null); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(hint,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
