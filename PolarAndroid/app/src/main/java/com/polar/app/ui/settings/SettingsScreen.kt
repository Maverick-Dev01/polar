package com.polar.app.ui.settings

import com.polar.app.ui.components.PolarChip
import com.polar.app.ui.theme.Spacing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.BuildConfig
import com.polar.app.data.AppSettings
import com.polar.app.data.ThemeMode
import com.polar.app.data.Units
import com.polar.app.model.PaperSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    trashCount: Int,
    onEmptyTrash: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onUnits: (Units) -> Unit,
    onPaper: (PaperSize) -> Unit,
    onShowOnboarding: () -> Unit,
    onBack: () -> Unit,
    updates: (@Composable () -> Unit)? = null,
    onHelp: () -> Unit = {}
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    val licenses = remember { runCatching { context.assets.list("licenses")?.sorted().orEmpty() }.getOrDefault(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.m, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Labeled(stringResource(R.string.settings_appearance)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(ThemeMode.SYSTEM to R.string.settings_theme_system, ThemeMode.LIGHT to R.string.settings_theme_light, ThemeMode.DARK to R.string.settings_theme_dark)
                    options.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(selected = settings.theme == mode, onClick = { onTheme(mode) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) { Text(stringResource(label)) }
                    }
                }
            }
            Labeled(stringResource(R.string.settings_units)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(Units.MM to R.string.settings_units_mm, Units.INCHES to R.string.settings_units_in)
                    options.forEachIndexed { i, (u, label) ->
                        SegmentedButton(selected = settings.units == u, onClick = { onUnits(u) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) { Text(stringResource(label)) }
                    }
                }
            }
            Labeled(stringResource(R.string.settings_default_paper)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaperSize.entries.forEach { p ->
                        PolarChip(selected = settings.defaultPaper == p, onClick = { onPaper(p) }, label = { Text(p.displayName) })
                    }
                }
            }
            if (updates != null) updates()
            else Text(stringResource(R.string.updates_play), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedCard {
                RowItem(stringResource(R.string.settings_help), onHelp)
                HorizontalDivider()
                RowItem(stringResource(R.string.settings_show_onboarding), onShowOnboarding)
                HorizontalDivider()
                RowItem(stringResource(R.string.settings_how_print)) { dialog = "print" }
                HorizontalDivider()
                RowItem(stringResource(R.string.settings_licenses)) { dialog = "licenses" }
                if (trashCount > 0) {
                    HorizontalDivider()
                    RowItem(stringResource(R.string.settings_empty_trash, trashCount)) { dialog = "trash" }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                stringResource(R.string.settings_version, version), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                stringResource(if (BuildConfig.GITHUB_UPDATES_ENABLED) R.string.distribution_github else R.string.distribution_play),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        }
    }

    when (dialog) {
        "print" -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_ok)) } },
            title = { Text(stringResource(R.string.settings_how_print)) },
            text = { Text(stringResource(R.string.settings_how_print_body)) }
        )
        "trash" -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null; onEmptyTrash() }) { Text(stringResource(R.string.settings_empty_trash_confirm)) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
            title = { Text(stringResource(R.string.settings_empty_trash, trashCount)) },
            text = { Text(stringResource(R.string.settings_empty_trash_body)) }
        )
        "licenses" -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_close)) } },
            title = { Text(stringResource(R.string.settings_licenses)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    licenses.forEach { name ->
                        Text(name.removeSuffix(".txt").replace('_', ' '), style = MaterialTheme.typography.titleSmall)
                        Text(
                            remember(name) { context.assets.open("licenses/$name").bufferedReader().use { it.readText() }.take(600) + "…" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        )
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun RowItem(text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        modifier = Modifier.heightIn(min = 56.dp).clickable(onClick = onClick)
    )
}
