package com.polar.app.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.polar.app.R
import com.polar.app.core.edit.MoodPreset
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.core.history.UndoStack
import com.polar.app.core.look.LookResolver
import com.polar.app.engine.LOW_RES_DPI
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import com.polar.app.ui.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

class EditorViewModel(private val projectId: String, private val deps: EditorDeps) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private val history = UndoStack<PolarProject>()
    private var saveJob: Job? = null
    // Cancelar no detiene una escritura síncrona: autosave, flush y salida usan la misma cola.
    private val saveMutex = Mutex()
    private val dirty: Boolean get() = _state.value.hasUnsavedChanges
    private var saveWarned = false
    var templateBitmap: Bitmap? = null
        private set

    init { viewModelScope.launch { load() } }

    private suspend fun load() {
        val loaded = try {
            withContext(deps.io) { deps.store.load(projectId) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Polar", "No se pudo abrir el proyecto", e)
            _state.update { it.copy(loading = false, loadFailed = true) }
            message(UiText(R.string.editor_load_failed))
            return
        }
        var templateMissing = false
        loaded.project.settings.importedTemplate?.let { t ->
            try {
                templateBitmap = withContext(deps.io) { deps.loadTemplate(t.path) }
                if (templateBitmap == null) templateMissing = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Polar", "No se pudo cargar la plantilla", e)
                templateBitmap = null
                templateMissing = true
            }
        }
        _state.update {
            it.copy(
                loading = false, project = loaded.project, missingPhotos = loaded.missingPhotos,
                textRole = loaded.project.settings.style.textRoles.firstOrNull() ?: TextRole.TITLE,
                templateVersion = it.templateVersion + 1
            )
        }
        if (templateMissing) message(UiText(R.string.editor_template_missing))
        if (loaded.missingPhotos > 0) message(UiText(R.string.editor_missing_photos, listOf(loaded.missingPhotos)))
    }

    // ---------- núcleo ----------

    /** Mientras el diseño no se haya abierto bien, el proyecto del estado es un valor por defecto: jamás se guarda. */
    private val locked: Boolean get() = _state.value.loading || _state.value.loadFailed

    private fun message(text: UiText, undoable: Boolean = false) { _events.trySend(EditorEvent.Message(text, undoable)) }

    private fun edit(message: UiText? = null, undoable: Boolean = message != null, change: (PolarProject) -> PolarProject) {
        if (locked) return
        val current = _state.value.project
        val next = try { change(current).also { it.validated() } } catch (e: PolarException) {
            message(UiText(R.string.raw_text, listOf(e.message ?: "Revisa las medidas del papel."))); return
        }
        if (next == current) return
        // Una acción puntual con aviso (p. ej. «Aplicar a todas») cierra antes el campo de texto abierto: así Deshacer sólo revierte esa acción.
        // Si el campo sigue enfocado se reabre después, para que lo que escribas luego siga siendo un solo paso.
        val reopen = message != null && history.isOpen
        if (message != null) history.endTransaction(current)
        history.record(current)
        commit(next)
        if (reopen) history.beginTransaction(next)
        message?.let { message(it, undoable) }
    }

    private fun commit(next: PolarProject) {
        if (locked) return
        _state.update { s ->
            val page = s.page.coerceIn(0, next.pageCount - 1)
            val slot = s.selectedSlot?.takeIf { it < next.placements.size }
            s.copy(project=next).withSlot(slot).copy(page = page, canUndo = history.canUndo || history.hasOpenChange(next), canRedo = history.canRedo,
                hasUnsavedChanges = true, saveFailed = false)
        }
        scheduleSave()
    }

    fun beginGesture() = history.beginTransaction(_state.value.project)

    fun endGesture() {
        history.endTransaction(_state.value.project)
        _state.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    }

    fun undo() = step(history::undo)
    fun redo() = step(history::redo)

    /** Deshacer/rehacer cierran el campo abierto; si sigue enfocado se reabre sobre el diseño restaurado. */
    private fun step(move: (PolarProject) -> PolarProject?) {
        val reopen = history.isOpen
        move(_state.value.project)?.let(::commit)
        if (reopen && !history.isOpen) history.beginTransaction(_state.value.project)
    }

    private fun scheduleSave() {
        if (locked) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch { delay(deps.autosaveDelayMs); save() }
    }

    suspend fun flush(): Boolean {
        if (locked) return true
        saveJob?.cancelAndJoin()
        if (dirty) save()
        return !dirty
    }

    fun flushAsync() { viewModelScope.launch { flush() } }

    private suspend fun save() = saveMutex.withLock {
        if (locked || !dirty) return@withLock
        val project = _state.value.project
        _state.update { it.copy(saving = true) }
        try {
            withContext(deps.io) {
                val thumb = try {
                    deps.thumbnail(project, templateBitmap)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("Polar", "No se pudo crear la miniatura", e)
                    null
                }
                currentCoroutineContext().ensureActive()
                deps.store.save(projectId, project, thumb)
            }
            _state.update { if (it.project == project) it.copy(hasUnsavedChanges = false, saveFailed = false) else it }
            saveWarned = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            _state.update { it.copy(saveFailed = true) }
            Log.w("Polar", "No se pudo guardar el proyecto", e)
            if (!saveWarned) {
                saveWarned = true
                message(UiText(R.string.editor_save_failed))
            }
        } finally {
            _state.update { it.copy(saving = false) }
        }
    }

    override fun onCleared() {
        if (dirty && !locked) {
            deps.appScope.launch(Dispatchers.Main.immediate) { flush() }
        }
    }

    // ---------- selección y navegación ----------

    fun selectSlot(slot: Int) = _state.update { s ->
        val page = slot / s.project.settings.capacity
        s.withSlot(if (s.selectedSlot == slot) null else slot).copy(page = page)
    }

    fun clearSelection() = _state.update { it.withSlot(null) }

    fun setPage(page: Int) = _state.update { s ->
        val p = page.coerceIn(0, s.project.pageCount - 1)
        val keep = s.selectedSlot?.takeIf { it / s.project.settings.capacity == p }
        s.withSlot(keep).copy(page = p)
    }

    fun setTool(tool: Tool?) { endGesture(); _state.update {
        it.copy(tool = tool, trayExpanded = false, textScope = if (tool == Tool.TEXT) TextScope.ALL else it.textScope,
            lookScope = if (tool == Tool.FILTERS && it.selectedPlacement != null) LookScope.PHOTO else it.lookScope,
            lookPages = if (it.lookPages.isEmpty()) setOf(it.page) else it.lookPages)
    } }
    fun setTrayExpanded(expanded: Boolean) = _state.update { it.copy(trayExpanded = expanded) }
    fun setComparing(comparing: Boolean) = _state.update { it.copy(comparing = comparing) }
    fun setLookScope(scope: LookScope) {
        if (scope == LookScope.PHOTO && _state.value.selectedPlacement == null) { message(UiText(R.string.look_pick_photo)); return }
        endGesture(); _state.update { it.copy(lookScope = scope) }
    }
    fun toggleLookPage(page: Int) = _state.update {
        it.copy(lookPages = if (page in it.lookPages) it.lookPages - page else it.lookPages + page)
    }
    fun currentLook(): PhotoLook {
        val s = _state.value
        return when(s.lookScope) {
            LookScope.ALL -> s.project.settings.photoLook ?: PhotoLook()
            LookScope.PHOTO -> s.selectedSlot?.let { LookResolver.resolve(s.project,it) } ?: PhotoLook()
            LookScope.PAGE,LookScope.PAGES -> {
                val page=if(s.lookScope==LookScope.PAGE) s.page else s.lookPages.minOrNull() ?: s.page
                s.project.cardOverrides[(page*s.project.cardsPerPage).toString()]?.photoLook ?: s.project.settings.photoLook ?: PhotoLook()
            }
        }
    }

    fun setLook(look: PhotoLook) {
        val s = _state.value
        if(s.lookScope==LookScope.PAGES && s.lookPages.isEmpty()) { message(UiText(R.string.look_pick_pages)); return }
        edit { p -> when(s.lookScope) {
            LookScope.ALL -> ProjectEdits.setAllLooks(p, look)
            LookScope.PAGE -> ProjectEdits.setPageLooks(p, look, setOf(s.page))
            LookScope.PAGES -> ProjectEdits.setPageLooks(p, look, s.lookPages)
            LookScope.PHOTO -> s.selectedSlot?.let { ProjectEdits.setPhotoLook(p, look, it) } ?: p
        } }
    }
    fun applySuggestedLook() {
        val preset=_state.value.project.settings.style.suggestedPhotoPreset ?: return
        edit(UiText(R.string.look_applied_all)) { ProjectEdits.setAllLooks(it,PhotoLook(preset=preset)) }
    }
    fun applyLookToAll() {
        val look = currentLook()
        edit(UiText(R.string.look_applied_all)) { ProjectEdits.setAllLooks(it, look) }
        _state.update { it.copy(lookScope = LookScope.ALL) }
    }
    fun adjacentPhoto(direction: Int) {
        val s = _state.value; val slots = s.project.placements.indices.filter { s.project.placements[it] != null }
        val i = slots.indexOf(s.selectedSlot)
        slots.getOrNull(i+direction)?.let { slot -> endGesture(); _state.update { it.withSlot(slot).copy(page=slot/it.project.settings.capacity) } }
    }

    fun openTextForSelected() = _state.update { it.copy(tool = Tool.TEXT, textScope = if (it.selectedSlot != null) TextScope.CARD else TextScope.ALL) }

    fun setTextRole(role: TextRole) = _state.update { it.copy(textRole = role) }

    fun setTextScope(scope: TextScope) {
        if (scope == TextScope.CARD && _state.value.selectedSlot == null) {
            message(UiText(R.string.editor_pick_card_for_text)); return
        }
        _state.update { it.copy(textScope = scope) }
    }

    fun setMode(mode: EditorMode) { endGesture(); _state.update { it.copy(mode = mode, comparing = false) } }
    fun setEditingRegions(on: Boolean) = _state.update { it.copy(editingRegions = on) }

    // ---------- texto ----------

    fun setText(value: String) {
        val s = _state.value
        edit { ProjectEdits.setText(it, s.textRole, value.take(500), s.editCard) }
    }

    fun clearOwnText() {
        val s = _state.value
        val card = s.selectedCard ?: return
        edit(UiText(R.string.editor_card_back_to_general, listOf(s.selectedCardNumber ?: 0))) { ProjectEdits.clearOwnText(it, card, s.textRole) }
    }

    fun applyTextToAll() {
        val role = _state.value.textRole
        edit(UiText(R.string.editor_text_same_for_all, listOf(role.displayName))) { ProjectEdits.applyTextToAll(it, role) }
    }

    fun editAppearance(change: (TextAppearance) -> TextAppearance) {
        val s = _state.value
        edit { ProjectEdits.editAppearance(it, s.textRole, s.editCard, change) }
    }

    fun resetAppearance() {
        val s = _state.value
        val card = s.editCard
        edit(UiText(R.string.editor_style_reset)) {
            if (card != null) ProjectEdits.clearOwnAppearance(it, card, s.textRole) else ProjectEdits.resetAppearance(it, s.textRole)
        }
    }

    fun setDateSource(source: DateSource) { val c = _state.value.editCard; edit { ProjectEdits.setDateSource(it, source, c) } }
    fun setChosenDate(epochMs: Long) { val c = _state.value.editCard; edit { ProjectEdits.setChosenDate(it, epochMs, c) } }
    fun setDateStyle(style: DateStyle) = edit { ProjectEdits.setDateStyle(it, style) }
    fun setSongUrl(url: String) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(songURL = url.trim().take(500)) } }

    // ---------- diseño ----------

    fun selectStyle(style: TemplateStyle) {
        edit(UiText(R.string.editor_style_now, listOf(style.displayName))) { ProjectEdits.selectStyle(it, style) }
        _state.update { it.withSlot(null).copy(mode = EditorMode.EDIT, textRole = style.textRoles.firstOrNull() ?: TextRole.TITLE) }
    }

    fun applyMood(mood: MoodPreset) {
        _state.update { it.copy(lastMood = mood) }
        edit(UiText(R.string.editor_mood_applied, listOf(mood.displayName))) { ProjectEdits.applyMood(it, mood) }
    }

    fun applySuggestedPhrases() {
        val mood = _state.value.lastMood
        edit(UiText(R.string.editor_phrases_applied)) { ProjectEdits.applySuggestedPhrases(it, mood) }
    }

    fun setAccent(hex: String) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(accentHex = hex) } }

    fun applyLayout(count: Int) {
        edit(UiText(R.string.editor_layout_per_sheet, listOf(count))) { ProjectEdits.applyLayoutPreset(it, count) }
        _state.update { it.withSlot(null).copy(page = 0) }
    }

    fun setGrid(columns: Int, rows: Int) = edit { ProjectEdits.setGrid(it, columns, rows) }
    fun setCardFormat(format: CardFormat) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(cardFormat = format) } }
    fun setGap(value: Double) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(gap = value.coerceIn(0.0, 30.0)) } }
    fun setRounded(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(roundedPhotos = on) } }
    fun setCalendarYear(year: Int) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(calendarYear = year.coerceIn(1900, 2100)) } }
    fun setHighlightDate(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(highlightDate = on) } }
    fun setSpecialDate(epochMs: Long) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(specialDate = SwiftDate.fromEpochMs(epochMs)) } }

    fun editRegion(index: Int, change: (TemplateRegion) -> TemplateRegion) = edit { ProjectEdits.editTemplateRegion(it, index, change) }
    fun moveRegion(index: Int, dx: Double, dy: Double) = editRegion(index) { r -> r.copy(x = r.x + dx, y = r.y + dy) }
    fun addRegion() = edit(UiText(R.string.editor_region_added)) { ProjectEdits.addTemplateRegion(it) }
    fun removeRegion(index: Int) = edit(UiText(R.string.editor_region_removed)) { ProjectEdits.removeTemplateRegion(it, index) }

    // ---------- papel ----------

    fun setPaper(size: PaperSize) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(paperSize = size) } }
    fun setOrientation(o: PaperOrientation) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(orientation = o) } }
    fun setCustomPaper(widthMM: Double, heightMM: Double) = edit { ProjectEdits.setCustomPaper(it, widthMM, heightMM) }
    fun setMargin(value: Double) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(margin = value.coerceIn(0.0, 60.0)) } }
    fun setGuides(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(cutGuides = on) } }
    fun setCutStyle(style: CutStyle) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(cutStyle = style) } }
    fun setBorders(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(drawBorders = on) } }

    // ---------- fotos ----------

    fun addPhotos(uris: List<String>) {
        if (uris.isEmpty() || locked) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = try {
                deps.photos.import(projectId, uris)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Polar", "Falló la importación de fotos", e)
                message(UiText(R.string.action_failed))
                return@launch
            } finally {
                _state.update { it.copy(busy = false) }
            }
            addAssets(result.assets)
            val added = result.assets.size
            val failed = result.failed
            val text = when {
                added == 0 && failed == 0 -> return@launch
                added == 0 && failed == 1 -> UiText(R.string.editor_photo_unreadable_one)
                added == 0 -> UiText(R.string.editor_photos_unreadable_many, listOf(failed))
                added == 1 && failed == 0 -> UiText(R.string.editor_photo_added_one)
                added == 1 && failed == 1 -> UiText(R.string.editor_photo_added_one_failed_one)
                added == 1 -> UiText(R.string.editor_photo_added_one_failed, listOf(failed))
                failed == 0 -> UiText(R.string.editor_photos_added_many, listOf(added))
                failed == 1 -> UiText(R.string.editor_photos_added_many_failed_one, listOf(added))
                else -> UiText(R.string.editor_photos_added_many_failed, listOf(added, failed))
            }
            message(text, undoable = added > 0)
        }
    }

    /** Sólo para pruebas: agrega fotos ya copiadas al proyecto. */
    internal fun addPhotosForTest(assets: List<PhotoAsset>) = addAssets(assets)

    private fun addAssets(assets: List<PhotoAsset>) {
        val s = _state.value
        val from = s.selectedSlot ?: s.project.placements.indexOfFirst { it == null }.takeIf { it >= 0 } ?: s.project.placements.size
        edit { ProjectEdits.addPhotos(it, assets, from) }
    }

    fun fillAll() {
        val p = _state.value.project
        if (p.photos.isEmpty()) { message(UiText(R.string.editor_add_photos_first)); return }
        val pages = ProjectEdits.fillAll(p).pageCount
        val text = when {
            p.photos.size == 1 -> UiText(R.string.editor_arranged_single)
            pages == 1 -> UiText(R.string.editor_arranged_one_page, listOf(p.photos.size))
            else -> UiText(R.string.editor_arranged_pages, listOf(p.photos.size, pages))
        }
        edit(text) { ProjectEdits.fillAll(it) }
        _state.update { it.withSlot(null).copy(page = 0) }
    }

    fun placePhoto(assetId: String) {
        val s = _state.value
        val slot = s.selectedSlot ?: run { message(UiText(R.string.editor_pick_card_for_photo)); return }
        edit { ProjectEdits.assign(it, slot, assetId) }
        val p = _state.value.project
        val cap = p.settings.capacity
        val pageEnd = (slot / cap + 1) * cap
        val next = (slot + 1 until pageEnd).firstOrNull { p.placements.getOrNull(it) == null }
        _state.update { it.copy(selectedSlot = next ?: slot) }
    }

    fun removeSelectedPhoto() {
        val slot = _state.value.selectedSlot ?: return
        edit(UiText(R.string.editor_photo_removed), undoable = true) { ProjectEdits.clearSlot(it, slot) }
    }

    fun rotateSelected() {
        val slot = _state.value.selectedSlot ?: return
        edit { ProjectEdits.editPlacement(it, slot) { p -> p.copy(quarterTurns = p.quarterTurns + 1) } }
    }

    fun editSelectedPlacement(change: (PhotoPlacement) -> PhotoPlacement) {
        val slot = _state.value.selectedSlot ?: return
        edit { ProjectEdits.editPlacement(it, slot, change) }
    }

    fun resetSelectedPlacement() = editSelectedPlacement { it.copy(zoom = 1.0, offsetX = 0.0, offsetY = 0.0, quarterTurns = 0) }

    // ---------- hojas ----------

    fun addPage() {
        edit(UiText(R.string.editor_page_added)) { ProjectEdits.addPage(it) }
        _state.update { it.withSlot(null).copy(page = it.project.pageCount - 1) }
    }

    fun clearPage() { val page = _state.value.page; edit(UiText(R.string.editor_page_cleared, listOf(page + 1)), undoable = true) { ProjectEdits.clearPage(it, page) } }
    fun removePage() { val page = _state.value.page; edit(UiText(R.string.editor_page_removed), undoable = true) { ProjectEdits.removePage(it, page) } }

    fun rename(name: String) {
        val clean = name.trim().take(80)
        if (clean.isNotEmpty()) edit { it.copy(name = clean) }
    }

    // ---------- calidad ----------

    fun dpiOf(slot: Int): Double? {
        val p = _state.value.project
        val s = p.settings
        val local = slot % s.capacity
        val cards = PolarRenderer.calculateCardRects(s)
        val rect = if (s.style == TemplateStyle.IMPORTED) cards.getOrNull(local) ?: return null
        else {
            val card = cards.getOrNull(local / s.style.photosPerCard) ?: return null
            PolarRenderer.calculatePhotoRects(card, s.style, s).getOrNull(local % s.style.photosPerCard) ?: return null
        }
        return p.effectiveDPI(slot, rect.width, rect.height)
    }

    fun lowResSlots(): List<Int> =
        _state.value.project.placements.indices.filter { slot -> dpiOf(slot)?.let { it < LOW_RES_DPI } == true }

    fun emptySlotsOnUsedPages(): Int {
        val p = _state.value.project
        val cap = p.settings.capacity
        return (0 until p.pageCount).sumOf { page ->
            val slice = p.placements.subList(page * cap, minOf(p.placements.size, (page + 1) * cap))
            if (slice.any { it != null }) slice.count { it == null } else 0
        }
    }

    // ---------- exportar ----------

    fun exportPdf(optimizePhotos: Boolean = true) = export(if (optimizePhotos) ExportFormat.PDF else ExportFormat.PDF_LOSSLESS)
    fun exportPng() = export(ExportFormat.PNG)
    fun exportJpg() = export(ExportFormat.JPEG)

    private fun export(format: ExportFormat) {
        if (locked || _state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val s = _state.value
                val file = when (format) {
                    ExportFormat.PDF, ExportFormat.PDF_LOSSLESS -> deps.exports.pdf(s.project, templateBitmap, format == ExportFormat.PDF)
                    ExportFormat.PNG -> deps.exports.png(s.project, s.page, templateBitmap)
                    ExportFormat.JPEG -> deps.exports.jpg(s.project, s.page, templateBitmap)
                }
                _events.send(EditorEvent.Exported(file, format.mime, format.pdf))
            } catch (e: CancellationException) {
                throw e
            } catch (e: PolarException) {
                message(UiText(if (e.message != null) R.string.raw_text else R.string.editor_export_failed, listOfNotNull(e.message)))
            } catch (e: OutOfMemoryError) {
                message(UiText(R.string.editor_export_memory))
            } catch (e: Exception) {
                Log.w("Polar", "Falló la exportación", e)
                message(UiText(R.string.action_failed))
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}

/** Si ya no hay tarjeta seleccionada, el texto vuelve a ser el general: así nunca se edita "a ciegas". */
private fun EditorUiState.withSlot(slot: Int?): EditorUiState =
    copy(selectedSlot = slot, textScope = if (slot == null) TextScope.ALL else textScope,
        lookScope = if((slot == null || project.placements.getOrNull(slot)==null) && lookScope == LookScope.PHOTO) LookScope.ALL else lookScope)
