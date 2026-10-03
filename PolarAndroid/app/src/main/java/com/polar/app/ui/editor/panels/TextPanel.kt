package com.polar.app.ui.editor.panels

import android.content.res.AssetManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polar.app.R
import com.polar.app.data.FontCatalog
import com.polar.app.data.FontChoice
import com.polar.app.model.*
import com.polar.app.ui.PickerDates
import com.polar.app.ui.components.*
import com.polar.app.ui.editor.TextScope

class TextCallbacks(
    val onRole: (TextRole) -> Unit,
    val onScope: (TextScope) -> Unit,
    val onText: (String) -> Unit,
    val onFocus: (Boolean) -> Unit,
    val onExpand: () -> Unit = {},
    val onRevert: () -> Unit,
    val onApplyAll: () -> Unit,
    val onAppearance: ((TextAppearance) -> TextAppearance) -> Unit,
    val onReset: () -> Unit,
    val onDateSource: (DateSource) -> Unit,
    val onChosenDate: (Long) -> Unit,
    val onDateStyle: (DateStyle) -> Unit,
    val onSongUrl: (String) -> Unit,
    val onGestureStart: () -> Unit,
    val onGestureEnd: () -> Unit
)

/** Colores de texto; el nombre es una etiqueta de accesibilidad. */
private val TEXT_COLORS = listOf(
    "20242C" to R.string.color_carbon, "FFFFFF" to R.string.color_blanco, "92394A" to R.string.color_vino,
    "C34048" to R.string.color_rojo, "F28C28" to R.string.color_naranja_fechador, "BC8952" to R.string.color_dorado,
    "486855" to R.string.color_verde, "38536F" to R.string.color_azul
)

@OptIn(ExperimentalTextApi::class)
fun FontChoice.family(assets: AssetManager): FontFamily = when {
    res != null -> FontFamily(Font(res))
    asset != null -> FontFamily(Font(asset, assets, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))))
    else -> FontFamily.Default
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TextPanel(s: TextPanelState, cb: TextCallbacks) {
    var more by rememberSaveable { mutableStateOf(false) }
    var otherColor by remember { mutableStateOf(false) }
    var phrases by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val a = s.appearance
    val font = FontCatalog.find(a.fontName)
    val assets = LocalContext.current.assets
    val fontFamily = remember(font.id) { font.family(assets) }

    PanelColumn {
        if (s.emptyReason != null) {
            Text(stringResource(s.emptyReason), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@PanelColumn
        }

        SymmetricActions {
            OutlinedButton(onClick={cb.onScope(TextScope.ALL)},modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {selected=!s.cardScope}) {
                Text(stringResource(R.string.text_scope_all),maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            }
            OutlinedButton(onClick={cb.onScope(TextScope.CARD)},modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {selected=s.cardScope}) {
                Text(if(s.cardNumber!=null) stringResource(R.string.text_scope_card,s.cardNumber) else stringResource(R.string.text_scope_one),maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.roles.forEach { r -> PolarChip(selected = r == s.role, onClick = { cb.onRole(r) }, label = { Text(r.displayName) }) }
        }

        if (s.role != TextRole.DATE) {
            SymmetricActions {
                OutlinedButton(cb.onExpand, Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Editar y ver") }
                OutlinedButton({ phrases = true }, Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Frases sugeridas") }
            }
            DisposableEffect(Unit) { onDispose { cb.onFocus(false) } }
            OutlinedTextField(
                value = s.text, onValueChange = cb.onText, maxLines = 4,
                label = {
                    Text(if (s.cardScope) stringResource(R.string.text_for_card, s.role.displayName, s.cardNumber ?: 1)
                    else stringResource(R.string.text_for_all, s.role.displayName))
                },
                textStyle = TextStyle(fontFamily = fontFamily, fontSize = (18 * font.previewScale).sp),
                modifier = Modifier.fillMaxWidth().onFocusChanged { cb.onFocus(it.isFocused) }
            )
            if (s.cardScope && s.hasOwnText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.text_others_keep, s.generalText), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = cb.onRevert) { Text(stringResource(R.string.text_revert)) }
                }
            }
            if (!s.cardScope && s.ownCount > 0) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(pluralStringResource(R.plurals.text_own_count, s.ownCount, s.ownCount), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = cb.onApplyAll) { Text(stringResource(R.string.text_apply_all)) }
                    }
                }
            }
            Text(stringResource(R.string.text_fit_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            SectionLabel(stringResource(R.string.text_which_date))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PolarChip(s.dateSource == DateSource.PHOTO, { cb.onDateSource(DateSource.PHOTO) }, { Text(stringResource(R.string.text_date_photo)) })
                PolarChip(s.dateSource == DateSource.CHOSEN, { pickDate = true }, { Text(stringResource(R.string.text_date_choose)) })
                PolarChip(s.dateSource == DateSource.NONE, { cb.onDateSource(DateSource.NONE) }, { Text(stringResource(R.string.text_date_none)) })
            }
            SectionLabel(stringResource(R.string.text_date_format))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateStyle.entries.forEachIndexed { i, st -> PolarChip(s.dateStyle == st, { cb.onDateStyle(st) }, { Text(s.dateSamples[i]) }) }
            }
        }

        SectionLabel(stringResource(R.string.text_font))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FontCatalog.all, key = { it.id }) { choice ->
                val family = remember(choice.id) { choice.family(assets) }
                PolarChip(
                    selected = font.id == choice.id,
                    onClick = { cb.onAppearance { it.copy(fontName = choice.id) } },
                    label = { Text(choice.displayName, fontFamily = family, fontSize = (15 * choice.previewScale).sp) }
                )
            }
        }

        SectionLabel(stringResource(R.string.text_color))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PolarChip(a.hex.isEmpty(), { cb.onAppearance { it.copy(hex = "") } }, { Text(stringResource(R.string.text_color_design)) }, modifier = Modifier.heightIn(min=48.dp))
            TEXT_COLORS.forEach { (hex, name) -> ColorSwatch(hex, stringResource(name), a.hex.equals(hex, true)) { cb.onAppearance { it.copy(hex = hex) } } }
            TextButton(onClick = { otherColor = true }, modifier = Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.color_other)) }
        }

        SectionLabel(stringResource(R.string.text_size))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.0 to R.string.text_size_auto, 8.0 to R.string.text_size_s, 12.0 to R.string.text_size_m, 18.0 to R.string.text_size_l).forEach { (size, label) ->
                PolarChip(a.size == size, { cb.onAppearance { it.copy(size = size) } }, { Text(stringResource(label)) })
            }
            FilledIconToggleButton(checked = a.bold, onCheckedChange = { v -> cb.onAppearance { it.copy(bold = v) } }) {
                Icon(Icons.Filled.FormatBold, stringResource(R.string.text_bold))
            }
            FilledIconToggleButton(checked = a.italic, onCheckedChange = { v -> cb.onAppearance { it.copy(italic = v) } }) {
                Icon(Icons.Filled.FormatItalic, stringResource(R.string.text_italic))
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val options = listOf(TextAlignment.AUTOMATIC, TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT)
            options.forEachIndexed { i, al ->
                SegmentedButton(selected = a.alignment == al, onClick = { cb.onAppearance { it.copy(alignment = al) } }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) {
                    when (al) {
                        TextAlignment.AUTOMATIC -> Text(stringResource(R.string.text_align_auto))
                        TextAlignment.LEFT -> Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, stringResource(R.string.text_align_left))
                        TextAlignment.CENTER -> Icon(Icons.Filled.FormatAlignCenter, stringResource(R.string.text_align_center))
                        TextAlignment.RIGHT -> Icon(Icons.AutoMirrored.Filled.FormatAlignRight, stringResource(R.string.text_align_right))
                    }
                }
            }
        }

        if (s.cardScope && s.hasOwnAppearance) Text(stringResource(R.string.text_own_style), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        MoreOptions(more, { more = !more }, stringResource(R.string.text_more_summary)) {
            SwitchRow(stringResource(R.string.text_visible), a.visible, { v -> cb.onAppearance { it.copy(visible = v) } })
            LabeledSlider(stringResource(R.string.text_exact_size), if (a.size == 0.0) 6f else a.size.toFloat(), 6f..96f,
                if (a.size == 0.0) stringResource(R.string.text_size_auto) else stringResource(R.string.unit_pt, a.size.toInt()),
                onChange = { v -> cb.onAppearance { it.copy(size = v.toDouble().let(Math::round).toDouble()) } }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
            LabeledSlider(stringResource(R.string.text_move_x), a.offsetX.toFloat(), -60f..60f, stringResource(R.string.unit_pt, a.offsetX.toInt()),
                onChange = { v -> cb.onAppearance { it.copy(offsetX = v.toDouble()) } }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
            LabeledSlider(stringResource(R.string.text_move_y), a.offsetY.toFloat(), -60f..60f, stringResource(R.string.unit_pt, a.offsetY.toInt()),
                onChange = { v -> cb.onAppearance { it.copy(offsetY = v.toDouble()) } }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
            TextButton(onClick = cb.onReset) { Text(stringResource(R.string.text_reset)) }
        }

        if (s.showSongUrl) {
            DisposableEffect(Unit) { onDispose { cb.onFocus(false) } }
            OutlinedTextField(
                value = s.songUrl, onValueChange = cb.onSongUrl, singleLine = true,
                label = { Text(stringResource(R.string.text_song_url)) },
                supportingText = { Text(stringResource(R.string.text_song_url_help)) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { cb.onFocus(it.isFocused) }
            )
        }
    }

    if (phrases) PhraseDialog(cb.onText) { phrases = false }
    if (otherColor) ColorChoiceDialog(a.hex.ifEmpty { s.accentHex }, { hex -> cb.onAppearance { it.copy(hex = hex) } }) { otherColor = false }
    if (pickDate) {
        val picker = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { cb.onChosenDate(PickerDates.toStored(it)) }; pickDate = false }) { Text(stringResource(R.string.action_accept)) } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(picker) }
    }
}
