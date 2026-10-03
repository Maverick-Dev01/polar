package com.polar.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.app.BuildConfig
import com.polar.app.R
import com.polar.app.ui.resolve
import com.polar.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun UpdateSection(vm: UpdateViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requestingInstall by rememberSaveable { mutableStateOf(false) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    fun install() {
        scope.launch {
            val intent = vm.installerIntent() ?: return@launch
            runCatching { installer.launch(intent) }.onFailure { vm.installationUnavailable() }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (requestingInstall && context.packageManager.canRequestPackageInstalls()) install()
        else if (requestingInstall) vm.permissionDenied()
        requestingInstall = false
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
            Text(stringResource(R.string.updates_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.updates_installed, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.updates_description), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.info?.let { info ->
                Text(stringResource(R.string.updates_available, info.versionName), style = MaterialTheme.typography.labelLarge)
                if (info.notes.isNotBlank()) Text(info.notes, style = MaterialTheme.typography.bodyMedium)
            }
            state.message?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when (state.phase) {
                UpdatePhase.CHECKING, UpdatePhase.VERIFYING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(if (state.phase == UpdatePhase.CHECKING) R.string.updates_checking else R.string.updates_verifying))
                }
                UpdatePhase.DOWNLOADING -> {
                    LinearProgressIndicator(progress = { state.progress ?: 0f }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.updates_progress, ((state.progress ?: 0f) * 100).toInt()))
                    OutlinedButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.action_cancel)) }
                }
                UpdatePhase.AVAILABLE -> {
                    if (vm.canUpdate) Button(onClick = vm::download, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.updates_download))
                    } else Text(stringResource(R.string.updates_debug))
                    TextButton(onClick = vm::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.updates_check)) }
                }
                UpdatePhase.READY -> {
                    Text(stringResource(R.string.updates_ready), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        if (context.packageManager.canRequestPackageInstalls()) install()
                        else {
                            requestingInstall = true
                            runCatching { permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))) }
                                .onFailure { requestingInstall = false; vm.installationUnavailable() }
                        }
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.updates_install)) }
                    TextButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.updates_discard)) }
                }
                else -> OutlinedButton(onClick = vm::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.updates_check)) }
            }
        }
    }
}
