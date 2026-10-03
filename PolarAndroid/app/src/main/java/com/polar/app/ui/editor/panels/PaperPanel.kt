package com.polar.app.ui.editor.panels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.data.Units
import com.polar.app.model.*
import com.polar.app.ui.components.*
import java.util.Locale

class PaperCallbacks(
    val onPaper: (PaperSize) -> Unit,
    val onOrientation: (PaperOrientation) -> Unit,
    val onCustom: (Double, Double) -> Unit,
    val onGuides: (Boolean) -> Unit,
    val onCutStyle: (CutStyle) -> Unit,
    val onBorders: (Boolean) -> Unit,
    val onMargin: (Float) -> Unit,
    val onGestureStart: () -> Unit,
    val onGestureEnd: () -> Unit
)

fun parseMeasure(text: String, units: Units): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    val mm = if (units == Units.INCHES) value * 25.4 else value
    return mm.takeIf { it in 80.0..600.0 }
}

private fun show(mm: Double, units: Units): String =
    if (units == Units.INCHES) String.format(Locale.ROOT, "%.2f", mm / 25.4) else String.format(Locale.ROOT, "%.0f", mm)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PaperPanel(settings: PrintSettings, units: Units, cb: PaperCallbacks) {
    var more by rememberSaveable { mutableStateOf(false) }
    val unitLabel = stringResource(if(units==Units.INCHES) R.string.unit_inches else R.string.unit_mm)
    var width by remember(settings.customWidthMM, units) { mutableStateOf(show(settings.customWidthMM, units)) }
    var height by remember(settings.customHeightMM, units) { mutableStateOf(show(settings.customHeightMM, units)) }
    var error by remember { mutableStateOf(false) }

    PanelColumn {
        SectionLabel(stringResource(R.string.paper_size))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaperSize.entries.forEach { p -> PolarChip(settings.paperSize == p, { cb.onPaper(p) }, { Text(p.displayName) }) }
        }
        if (settings.paperSize == PaperSize.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(width, { width = it; error = false }, label = { Text(stringResource(R.string.paper_width, unitLabel)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(height, { height = it; error = false }, label = { Text(stringResource(R.string.paper_height, unitLabel)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            if (error) Text(stringResource(if (units == Units.INCHES) R.string.paper_range_in else R.string.paper_range_mm),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                val w = parseMeasure(width, units); val h = parseMeasure(height, units)
                if (w == null || h == null) error = true else cb.onCustom(w, h)
            }) { Text(stringResource(R.string.paper_apply)) }
        }
        Text(settings.paperDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionLabel(stringResource(R.string.paper_orientation))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PaperOrientation.entries.forEachIndexed { i, o ->
                SegmentedButton(settings.orientation == o, { cb.onOrientation(o) }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(o.displayName) }
            }
        }

        SwitchRow(stringResource(R.string.paper_guides), settings.cutGuides, cb.onGuides, stringResource(R.string.paper_guides_help))
        if (settings.cutGuides) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CutStyle.entries.forEach { c -> PolarChip(settings.cutStyle == c, { cb.onCutStyle(c) }, { Text(c.displayName) }) }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Print, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.paper_tip, settings.paperSize.displayName), style = MaterialTheme.typography.bodyMedium)
            }
        }

        MoreOptions(more, { more = !more }, stringResource(R.string.paper_more_summary)) {
            LabeledSlider(stringResource(R.string.paper_margin), settings.margin.toFloat(), 0f..60f, stringResource(R.string.unit_pt, settings.margin.toInt()),
                cb.onMargin, cb.onGestureStart, cb.onGestureEnd)
            SwitchRow(stringResource(R.string.paper_borders), settings.drawBorders, cb.onBorders, stringResource(R.string.paper_borders_help))
        }
    }
}
