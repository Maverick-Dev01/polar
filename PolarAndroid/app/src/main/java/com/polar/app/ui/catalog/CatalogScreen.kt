package com.polar.app.ui.catalog

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.model.PolarException
import com.polar.app.ui.UiText
import com.polar.app.ui.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Composable
fun CatalogScreen(container: AppContainer, onCreated: (id: String, notice: String?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val newName = stringResource(R.string.catalog_new)
    val templateName = stringResource(R.string.catalog_template_name)
    val openedName = stringResource(R.string.catalog_opened_name)
    val vm: CatalogViewModel = viewModel(factory = viewModelFactory {
        initializer {
            CatalogViewModel(
                store = container.store,
                readText = { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.bufferedReader().readText() } ?: ""
                    }
                },
                copyTemplate = { id, uri ->
                    withContext(Dispatchers.IO) {
                        val parsed = Uri.parse(uri)
                        val ext = context.contentResolver.getType(parsed)?.substringAfter('/') ?: "png"
                        container.store.importTemplateFile(
                            id, context.contentResolver.openInputStream(parsed)
                                ?: throw PolarException(context.getString(R.string.catalog_bad_image)), ext
                        )
                    }
                },
                defaultPaper = { container.settings.settings.first().defaultPaper },
                newName = newName,
                templateName = templateName,
                openedName = openedName
            )
        }
    })
    var error by remember { mutableStateOf<UiText?>(null) }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is CatalogEvent.Created -> onCreated(e.id, e.notice?.resolve(context))
                is CatalogEvent.Error -> error = e.text
            }
        }
    }
    CatalogContent(
        title = stringResource(R.string.catalog_new),
        current = null,
        thumbnails = container.thumbnails,
        showImport = true,
        onPick = vm::createFromStyle,
        onImportTemplate = { vm.createFromTemplate(it.toString()) },
        onOpenPolar = { uri ->
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: openedName
            vm.openPolar(uri.toString(), name)
        },
        onBack = onBack
    )
    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.action_ok)) } },
            text = { Text(it.resolve()) }
        )
    }
}
