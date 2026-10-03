package com.polar.app.ui.editor.panels

import com.polar.app.ui.theme.Spacing
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.core.edit.MoodPreset
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import com.polar.app.ui.PickerDates
import com.polar.app.ui.components.*
import com.polar.app.ui.editor.DesignScope
import com.polar.app.ui.editor.EditorUiState
import java.text.DateFormat
import java.util.Date

class DesignCallbacks(
    val onChangeDesign: () -> Unit,
    val onScope: (DesignScope) -> Unit = {},
    val onAccent: (String) -> Unit,
    val onLayout: (Int) -> Unit,
    val onMood: (MoodPreset) -> Unit,
    val onSuggested: () -> Unit,
    val onFormat: (CardFormat) -> Unit,
    val onGrid: (Int, Int) -> Unit,
    val onGap: (Float) -> Unit,
    val onRounded: (Boolean) -> Unit,
    val onYear: (Int) -> Unit,
    val onHighlight: (Boolean) -> Unit,
    val onSpecialDate: (Long) -> Unit,
    val onEditRegions: (Boolean) -> Unit,
    val onRegion: ((TemplateRegion) -> TemplateRegion) -> Unit,
    val onAddRegion: () -> Unit,
    val onRemoveRegion: () -> Unit,
    val onGestureStart: () -> Unit,
    val onGestureEnd: () -> Unit
)

private val DESIGN_PALETTE = listOf(
    "92394A" to R.string.color_vino, "C34048" to R.string.color_rojo, "486855" to R.string.color_verde,
    "38536F" to R.string.color_azul, "20242C" to R.string.color_carbon, "BC8952" to R.string.color_dorado
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DesignPanel(state: EditorUiState, cb: DesignCallbacks) {
    val s = if (state.designScope == DesignScope.CARD && state.selectedCard != null) state.project.settingsForCard(state.selectedCard!!) else if (state.designScope == DesignScope.PAGE) state.project.settingsForPage(state.page) else state.project.settings
    val imported = s.style == TemplateStyle.IMPORTED
    var more by rememberSaveable { mutableStateOf(false) }
    var otherColor by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    PanelColumn {
        SectionLabel("Aplicar diseño a")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DesignScope.ALL to "Colección", DesignScope.PAGE to "Hoja ${state.page + 1}", DesignScope.CARD to "Tarjeta seleccionada").forEach { (scope, label) ->
                PolarChip(state.designScope == scope, { cb.onScope(scope) }, { Text(label) })
            }
        }
        Text("Los diseños compatibles conservan las posiciones. La distribución de fotos por hoja se aplica a la colección.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.design_current), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.style.displayName, style = MaterialTheme.typography.titleMedium)
                }
                FilledTonalButton(onClick = cb.onChangeDesign) { Text(stringResource(R.string.design_change)) }
            }
        }

        SectionLabel(stringResource(R.string.design_color))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DESIGN_PALETTE.forEach { (hex, nameRes) -> ColorSwatch(hex, stringResource(nameRes), s.accentHex.equals(hex, true)) { cb.onAccent(hex) } }
            TextButton(onClick = { otherColor = true }, modifier = Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.color_other)) }
        }

        if (!imported) {
            SectionLabel(stringResource(R.string.design_per_sheet))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectEdits.LAYOUT_PRESETS.forEach { n ->
                    PolarChip(selected = s.columns * s.rows == n, onClick = { cb.onLayout(n) }, label = { Text("$n") })
                }
            }
            if (s.style.photosPerCard > 1) Text(stringResource(R.string.design_per_sheet_film), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionLabel(stringResource(R.string.design_moods))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoodPreset.entries.forEach { m ->
                PolarChip(selected = s.accentHex.equals(m.hex, true) && s.textStyle(TextRole.TITLE).fontName == m.fontName, onClick = { cb.onMood(m) }, label = { Text(m.displayName) })
            }
        }
        Text(stringResource(R.string.design_moods_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = cb.onSuggested, enabled = s.style.textRoles.isNotEmpty()) { Text(stringResource(R.string.design_suggested)) }

        if (s.style == TemplateStyle.CALENDAR) {
            SectionLabel(stringResource(R.string.design_calendar))
            Stepper(stringResource(R.string.design_year), s.calendarYear, 1900..2100) { cb.onYear(it) }
            SwitchRow(stringResource(R.string.design_highlight), s.highlightDate, cb.onHighlight)
            if (s.highlightDate) {
                OutlinedButton(onClick = { pickDate = true }) {
                    Text(stringResource(R.string.design_pick_date) + " · " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(SwiftDate.toEpochMs(s.specialDate))))
                }
            }
        }

        if (imported) {
            SectionLabel(stringResource(R.string.design_template))
            SwitchRow(stringResource(R.string.design_edit_holes), state.editingRegions, cb.onEditRegions, stringResource(R.string.design_holes_help))
            val regions = s.importedTemplate?.regions.orEmpty()
            val index = state.selectedSlot?.rem(s.capacity)
            if (state.editingRegions && index != null && index in regions.indices) {
                val r = regions[index]
                Text(stringResource(R.string.design_hole_n, index + 1, regions.size), style = MaterialTheme.typography.titleSmall)
                RegionSlider(stringResource(R.string.design_left), r.x, cb) { v -> { it.copy(x = v) } }
                RegionSlider(stringResource(R.string.design_top), r.y, cb) { v -> { it.copy(y = v) } }
                RegionSlider(stringResource(R.string.design_width), r.width, cb) { v -> { it.copy(width = v) } }
                RegionSlider(stringResource(R.string.design_height), r.height, cb) { v -> { it.copy(height = v) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
                OutlinedButton(onClick = cb.onAddRegion, enabled = regions.size < 64) { Text(stringResource(R.string.design_add_hole)) }
                OutlinedButton(onClick = cb.onRemoveRegion, enabled = regions.size > 1 && state.selectedSlot != null) { Text(stringResource(R.string.design_remove_hole)) }
            }
        } else {
            MoreOptions(more, { more = !more }, stringResource(R.string.design_more_summary)) {
                SectionLabel(stringResource(R.string.design_format))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardFormat.entries.forEach { f -> PolarChip(s.cardFormat == f, { cb.onFormat(f) }, { Text(f.displayName) }) }
                }
                Stepper(stringResource(R.string.design_columns), s.columns, 1..4) { cb.onGrid(it, s.rows) }
                Stepper(stringResource(R.string.design_rows), s.rows, 1..6) { cb.onGrid(s.columns, it) }
                LabeledSlider(stringResource(R.string.design_gap), s.gap.toFloat(), 0f..30f, "${s.gap.toInt()} pt", cb.onGap, cb.onGestureStart, cb.onGestureEnd)
                SwitchRow(stringResource(R.string.design_rounded), s.roundedPhotos, cb.onRounded)
            }
        }
    }

    if (otherColor) ColorChoiceDialog(s.accentHex, cb.onAccent) { otherColor = false }
    if (pickDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = PickerDates.toPicker(SwiftDate.toEpochMs(s.specialDate)))
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { cb.onSpecialDate(PickerDates.toStored(it)) }; pickDate = false }) { Text(stringResource(R.string.action_accept)) } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(picker) }
    }
}

@Composable
private fun RegionSlider(label: String, value: Double, cb: DesignCallbacks, change: (Double) -> (TemplateRegion) -> TemplateRegion) {
    LabeledSlider(label, (value * 100).toFloat(), 0f..100f, "${(value * 100).toInt()} %",
        onChange = { cb.onRegion(change(it / 100.0)) }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) { Icon(Icons.Filled.Remove, stringResource(R.string.less) + " " + label) }
        Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 48.dp))
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) { Icon(Icons.Filled.Add, stringResource(R.string.more) + " " + label) }
    }
}
