package com.polar.app.ui.importer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.app.R
import com.polar.app.model.PolarException
import com.polar.app.model.RegionShape
import com.polar.app.template.SavedTemplate
import com.polar.app.template.TemplateImporter
import com.polar.app.template.TemplateLibrary
import com.polar.app.template.TemplateLoad
import com.polar.app.ui.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ImportWizardEvent {
    /** El molde que se va a usar: el recién guardado o el que ya estaba en «Mis moldes». */
    data class Done(val template: SavedTemplate, val reusedExisting: Boolean) : ImportWizardEvent
    data class Error(val text: UiText) : ImportWizardEvent
    data object Exit : ImportWizardEvent
}

class ImportWizardViewModel(
    private val library: TemplateLibrary,
    /** Copia la imagen elegida (URI) a un archivo propio. */
    private val copyPicked: suspend (uri: String) -> File,
    private val analyze: (File) -> TemplateLoad = TemplateImporter::loadTemplate,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _state = MutableStateFlow(ImportWizardState())
    val state: StateFlow<ImportWizardState> = _state.asStateFlow()
    private val _events = Channel<ImportWizardEvent>(Channel.BUFFERED)
    val events: Flow<ImportWizardEvent> = _events.receiveAsFlow()

    fun pick(uri: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            var file: File? = null
            try {
                val (copied, load, match) = withContext(io) {
                    val copy = copyPicked(uri).also { file = it }
                    val result = analyze(copy)
                    Triple(copy, result, result.fingerprint?.let(library::findDuplicate))
                }
                _state.update { ImportWizard.loaded(it, copied, load, match) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                file?.delete()
                if (e !is PolarException) Log.w("Polar", "No se pudo analizar el molde", e)
                _state.update { it.copy(busy = false) }
                _events.send(ImportWizardEvent.Error(if (e is PolarException && e.message != null) UiText(R.string.raw_text, listOf(e.message!!)) else UiText(R.string.action_failed)))
            }
        }
    }

    fun useExisting() {
        val match = _state.value.match ?: return
        discardPicked()
        viewModelScope.launch { _events.send(ImportWizardEvent.Done(match.template, reusedExisting = true)) }
    }

    fun keepAsNew() = _state.update(ImportWizard::keepAsNew)
    fun next() = _state.update(ImportWizard::next)
    fun select(index: Int?) = _state.update { ImportWizard.select(it, index) }
    fun setName(name: String) = _state.update { ImportWizard.setName(it, name) }
    fun move(index: Int, dx: Double, dy: Double) = _state.update { ImportWizard.move(it, index, dx, dy) }
    fun resize(index: Int, corner: Corner, dx: Double, dy: Double) = _state.update { ImportWizard.resize(it, index, corner, dx, dy) }
    fun grow(index: Int, dw: Double, dh: Double) = _state.update { ImportWizard.grow(it, index, dw, dh) }
    fun setShape(index: Int, shape: RegionShape) = _state.update { ImportWizard.setShape(it, index, shape) }
    fun setRadius(index: Int, radius: Double) = _state.update { ImportWizard.setRadius(it, index, radius) }
    fun addRegion() = _state.update(ImportWizard::add)
    fun removeRegion(index: Int) = _state.update { ImportWizard.remove(it, index) }

    /** Atrás; en el primer paso sale del asistente. */
    fun back() {
        val previous = ImportWizard.back(_state.value)
        if (previous == null) { viewModelScope.launch { _events.send(ImportWizardEvent.Exit) }; return }
        if (previous.step == WizardStep.PICK) discardPicked()
        _state.value = previous
    }

    fun save() {
        val s = _state.value
        val file = s.file ?: return
        val load = s.load ?: return
        val fingerprint = load.fingerprint ?: return
        if (!s.canSave || s.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val saved = withContext(io) {
                    library.save(s.name, file, s.regions, fingerprint, load.template.pixelWidth, load.template.pixelHeight)
                }
                discardPicked()
                _events.send(ImportWizardEvent.Done(saved, reusedExisting = false))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Polar", "No se pudo guardar el molde", e)
                _state.update { it.copy(busy = false) }
                _events.send(ImportWizardEvent.Error(UiText(R.string.action_failed)))
            }
        }
    }

    private fun discardPicked() { _state.value.file?.delete() }

    override fun onCleared() { discardPicked() }
}
