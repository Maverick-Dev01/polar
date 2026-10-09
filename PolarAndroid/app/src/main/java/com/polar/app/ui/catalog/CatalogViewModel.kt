package com.polar.app.ui.catalog

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.app.R
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.ProjectStore
import com.polar.app.model.*
import com.polar.app.template.SavedTemplate
import com.polar.app.template.TemplateImporter
import com.polar.app.template.TemplateLibrary
import com.polar.app.ui.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface CatalogEvent {
    data class Created(val id: String, val notice: UiText? = null) : CatalogEvent
    data class Error(val text: UiText) : CatalogEvent
}

private fun Exception.toUiText(): UiText {
    if (this is PolarException) message?.let { return UiText(R.string.raw_text, listOf(it)) }
    return UiText(R.string.action_failed)
}

class CatalogViewModel(
    private val store: ProjectStore,
    private val readText: suspend (String) -> String,
    private val copyTemplate: suspend (projectId: String, uri: String) -> File,
    private val defaultPaper: suspend () -> PaperSize,
    private val newName: String,
    private val templateName: String,
    private val openedName: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val templates: TemplateLibrary? = null
) : ViewModel() {
    private val _events = Channel<CatalogEvent>(Channel.BUFFERED)
    val events: Flow<CatalogEvent> = _events.receiveAsFlow()
    private var busy = false

    /** Ignora toques repetidos mientras una creación sigue en curso. */
    private fun guarded(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                busy = false
            }
        }
    }

    private suspend fun fail(e: Exception) {
        if (e !is PolarException) Log.w("Polar", "Falló el catálogo", e)
        _events.send(CatalogEvent.Error(e.toUiText()))
    }

    fun createFromStyle(style: TemplateStyle) = guarded {
        try {
            val id = withContext(io) {
                val base = PolarProject(settings = PrintSettings(paperSize = defaultPaper()))
                store.create(ProjectEdits.selectStyle(base, style), newName)
            }
            _events.send(CatalogEvent.Created(id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(e)
        }
    }

    fun createFromTemplate(uri: String) = guarded {
        var id: String? = null
        try {
            val (newId, detected) = withContext(io) {
                val created = store.create(PolarProject(), templateName)
                id = created
                val load = TemplateImporter.loadTemplate(copyTemplate(created, uri))
                val project = PolarProject(
                    settings = PrintSettings(style = TemplateStyle.IMPORTED, columns = 1, rows = 1, paperSize = defaultPaper(), importedTemplate = load.template),
                    name = templateName
                ).normalized()
                store.save(created, project)
                created to load.detectedCount
            }
            val notice = if (detected == 0) UiText(R.string.catalog_no_holes) else UiText(R.string.catalog_holes_found, listOf(detected))
            _events.send(CatalogEvent.Created(newId, notice))
        } catch (e: CancellationException) {
            id?.let { discard(it) }
            throw e
        } catch (e: Exception) {
            id?.let { discard(it) }
            fail(e)
        }
    }

    /** Un diseño nuevo con un molde de «Mis moldes»: el proyecto lleva su propia copia de la imagen. */
    fun createFromSavedTemplate(saved: SavedTemplate) = guarded {
        var id: String? = null
        try {
            val newId = withContext(io) {
                val library = templates ?: throw PolarException("Mis moldes no está disponible.")
                val created = store.create(PolarProject(), saved.name)
                id = created
                val source = library.imageFile(saved)
                val copy = source.inputStream().use { store.importTemplateFile(created, it, source.extension) }
                val imported = ImportedTemplate(path = copy.absolutePath, pixelWidth = saved.pixelWidth, pixelHeight = saved.pixelHeight, regions = saved.regions)
                store.save(created, PolarProject(
                    settings = PrintSettings(style = TemplateStyle.IMPORTED, columns = 1, rows = 1, paperSize = defaultPaper(), importedTemplate = imported),
                    name = saved.name
                ).normalized())
                created
            }
            _events.send(CatalogEvent.Created(newId))
        } catch (e: CancellationException) {
            id?.let { discard(it) }
            throw e
        } catch (e: Exception) {
            id?.let { discard(it) }
            fail(e)
        }
    }

    /** Limpia el proyecto a medio crear sin tapar el error real si la limpieza también falla. */
    private suspend fun discard(id: String) = withContext(NonCancellable + io) {
        runCatching { store.delete(id); store.purge(id) }
            .onFailure { Log.w("Polar", "No se pudo limpiar el proyecto $id", it) }
    }

    fun openPolar(uri: String, displayName: String) = guarded {
        try {
            val (id, missing) = withContext(io) {
                val created = store.importPolar(readText(uri), displayName.removeSuffix(".polar").ifBlank { openedName })
                created to store.load(created).missingPhotos
            }
            val notice = if (missing > 0) UiText(R.string.catalog_missing_photos, listOf(missing)) else null
            _events.send(CatalogEvent.Created(id, notice))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(e)
        }
    }
}
