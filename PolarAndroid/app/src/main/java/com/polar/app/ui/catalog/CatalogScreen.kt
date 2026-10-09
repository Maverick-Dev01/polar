package com.polar.app.ui.catalog

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.polar.app.ui.importer.ImportWizardScreen
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
import com.polar.app.template.SavedTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CatalogScreen(container: AppContainer, onCreated: (id: String, notice: String?) -> Unit, onBack: () -> Unit, landing: String? = null) {
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
                openedName = openedName,
                templates = container.templates
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
    var importing by rememberSaveable { mutableStateOf(landing == "moldes.asistente") }
    val library = rememberMyTemplates(container)
    if (importing) {
        ImportWizardScreen(container, onDone = { saved -> importing = false; library.refresh(); vm.createFromSavedTemplate(saved) }, onExit = { importing = false })
        return
    }
    CatalogContent(
        title = stringResource(R.string.catalog_new),
        current = null,
        thumbnails = container.thumbnails,
        showImport = true,
        initialMine = landing == "mis-moldes",
        onPick = vm::createFromStyle,
        onImportTemplate = { importing = true },
        templates = library.templates,
        templateThumbnail = library.thumbnail,
        onPickTemplate = vm::createFromSavedTemplate,
        onDeleteTemplate = library.delete,
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

/** «Mis moldes» listo para usar desde una pantalla: lista, miniaturas y borrado. */
class MyTemplatesState(val templates: List<SavedTemplate>, val thumbnail: suspend (SavedTemplate) -> android.graphics.Bitmap?, val refresh: () -> Unit, val delete: (SavedTemplate) -> Unit)

@Composable
fun rememberMyTemplates(container: AppContainer): MyTemplatesState {
    var templates by remember { mutableStateOf(emptyList<SavedTemplate>()) }
    val scope = rememberCoroutineScope()
    val refresh: () -> Unit = { scope.launch { templates = withContext(Dispatchers.IO) { container.templates.list() } } }
    LaunchedEffect(Unit) { refresh() }
    return MyTemplatesState(
        templates,
        thumbnail = { t -> container.bitmaps.load(container.templates.imageFile(t).absolutePath, 360) },
        refresh = refresh,
        delete = { t -> scope.launch { withContext(Dispatchers.IO) { container.templates.delete(t.id) }; refresh() } }
    )
}
