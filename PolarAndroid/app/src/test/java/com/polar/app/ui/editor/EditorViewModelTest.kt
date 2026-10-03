package com.polar.app.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModelStore
import com.polar.app.MainDispatcherRule
import com.polar.app.R
import com.polar.app.core.text.TextResolver
import com.polar.app.data.*
import com.polar.app.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var store: ProjectStore
    private lateinit var id: String
    private var exportedPage: Int? = null
    private var optimizedPdf: Boolean? = null

    private fun vm(
        removeBackground: (suspend (PhotoAsset, File) -> String)? = null,
        thumbnail: suspend (PolarProject, Bitmap?) -> ByteArray? = { _, _ -> null }
    ): EditorViewModel {
        store = ProjectStore(tmp.root)
        id = store.create(PolarProject().normalized(), "Boda")
        val photos = object : PhotoSource {
            override suspend fun import(projectId: String, uris: List<String>) = ImportResult(
                uris.map { store.importPhoto(projectId, it.byteInputStream(), "jpg", PhotoInfo(3000, 3000, null)) }, 0
            )
        }
        val exports = object : ExportService {
            override suspend fun pdf(project: PolarProject, template: Bitmap?, optimizePhotos: Boolean): File {
                optimizedPdf = optimizePhotos
                return File(tmp.root, "a.pdf")
            }
            override suspend fun png(project: PolarProject, page: Int, template: Bitmap?) = File(tmp.root, "a.png")
            override suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?): File {
                exportedPage = page
                return File(tmp.root, "a.jpg")
            }
        }
        return EditorViewModel(id, EditorDeps(store, photos, exports, thumbnail, { null }, main.dispatcher,
            CoroutineScope(main.dispatcher), removeBackground = removeBackground))
    }

    @Test
    fun missingMaskRetriesWithoutChangingOriginalOnFailureOrAReplacementOnLateSuccess() = runTest(main.dispatcher) {
        val requested = mutableListOf<String>()
        val entered = CompletableDeferred<Unit>()
        val result = CompletableDeferred<String>()
        val v = vm(removeBackground = { photo, _ ->
            requested += photo.id
            if (requested.size == 1) throw java.io.IOException("Fallo simulado del motor")
            entered.complete(Unit)
            result.await()
        })
        advanceUntilIdle()
        val missingMask = File(tmp.root, "mascara-perdida.png")
        val original = store.importPhoto(id, "original".byteInputStream(), "jpg", PhotoInfo(800, 600, null))
            .copy(maskPath = missingMask.path)
        val replacement = store.importPhoto(id, "otra foto".byteInputStream(), "jpg", PhotoInfo(800, 600, null))
        v.addPhotosForTest(listOf(original, replacement)); advanceUntilIdle()
        v.selectSlot(0)
        val before = v.state.value.project
        assertFalse(missingMask.exists())

        v.removeBackground(); advanceUntilIdle()
        assertEquals(listOf(original.id), requested)
        assertEquals(before, v.state.value.project)
        assertEquals("original", File(original.path).readText())
        assertTrue(v.state.value.backgroundError?.contains("Fallo simulado del motor") == true)
        assertFalse(v.state.value.busy)

        v.dismissBackgroundError()
        v.removeBackground(); runCurrent()
        assertTrue(entered.isCompleted)
        assertTrue(v.state.value.busy)
        v.placePhoto(replacement.id)
        val afterReplacement = v.state.value.project
        assertEquals(replacement.id, afterReplacement.placements[0]?.assetID)
        result.complete(File(tmp.root, "resultado-tardio.png").path)
        advanceUntilIdle()
        assertEquals(listOf(original.id, original.id), requested)
        assertEquals(afterReplacement, v.state.value.project)
        assertNull(v.state.value.project.placements[0]?.background)
        assertNull(v.state.value.project.asset(v.state.value.project.placements[0])?.maskPath)
        assertNull(v.state.value.backgroundError)
        assertFalse(v.state.value.busy)
    }

    @Test fun filterScopesUndoAndComparisonNeverChangeStoredOriginals() = runTest(main.dispatcher) {
        val v=vm();advanceUntilIdle();v.addPhotos(listOf("a","b","c","d","e","f","g","h","i","j"));advanceUntilIdle()
        v.selectSlot(1);v.setTool(Tool.FILTERS)
        assertEquals(LookScope.PHOTO,v.state.value.lookScope)
        v.setLook(PhotoLook(preset="sepia"));val own=v.state.value.project
        assertEquals("sepia",com.polar.app.core.look.LookResolver.resolve(own,1).preset)
        assertEquals("original",com.polar.app.core.look.LookResolver.resolve(own,0).preset)
        v.applyLookToAll();assertEquals("sepia",v.state.value.project.settings.photoLook?.preset)
        v.undo();assertEquals(own,v.state.value.project)
        val before=v.state.value.project;v.setComparing(true);v.setComparing(false);assertEquals(before,v.state.value.project)
        v.setLookScope(LookScope.PAGES);v.toggleLookPage(1);v.setLook(PhotoLook(preset="bw"))
        assertEquals("bw",com.polar.app.core.look.LookResolver.resolve(v.state.value.project,9).preset)
        v.beginGesture();v.setLook(v.currentLook().copy(light=.1));v.setLook(v.currentLook().copy(light=.4));v.endGesture();v.undo()
        assertEquals(0.0,v.currentLook().light,0.0)
        advanceUntilIdle();assertEquals(v.state.value.project.copy(updatedAtEpochMs=store.load(id).project.updatedAtEpochMs),store.load(id).project)
    }
    @Test fun pageControlsNeverReadTheFirstPhotosOwnLook() = runTest(main.dispatcher) {
        val v=vm();advanceUntilIdle();v.addPhotos(listOf("a","b"));advanceUntilIdle()
        v.setTool(Tool.FILTERS);v.setLook(PhotoLook(preset="bw"));v.selectSlot(0);v.setLookScope(LookScope.PHOTO);v.setLook(PhotoLook(preset="sepia"))
        v.setLookScope(LookScope.PAGE);assertEquals("bw",v.currentLook().preset)
        v.setLook(v.currentLook().copy(light=.2))
        assertEquals("bw",com.polar.app.core.look.LookResolver.resolve(v.state.value.project,0).preset)
    }
    @Test fun invalidPaperEditCannotEnterHistoryOrAutosave() = runTest(main.dispatcher) {
        val v=vm();advanceUntilIdle();v.setCustomPaper(80.0,80.0);v.setGrid(3,6);v.setGap(30.0);advanceUntilIdle()
        val valid=v.state.value.project
        v.setMargin(60.0);assertEquals(valid,v.state.value.project)
        advanceUntilIdle();assertEquals(valid.copy(updatedAtEpochMs=store.load(id).project.updatedAtEpochMs),store.load(id).project)
    }

    @Test
    fun loadsProject() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        assertFalse(v.state.value.loading)
        assertEquals("Boda", v.state.value.project.name)
        assertEquals(TextRole.TITLE, v.state.value.textRole)
    }

    @Test
    fun editRecordsUndoAndRedo() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("C34048")
        assertTrue(v.state.value.canUndo)
        v.undo()
        assertEquals("92394A", v.state.value.project.settings.accentHex)
        v.redo()
        assertEquals("C34048", v.state.value.project.settings.accentHex)
    }

    @Test
    fun textTransactionIsOneStep() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val original = v.state.value.project.settings.title
        v.beginGesture(); v.setText("L"); v.setText("Lu"); v.setText("Lu y Max"); v.endGesture()
        assertEquals("Lu y Max", v.state.value.project.settings.title)
        v.undo()
        assertEquals(original, v.state.value.project.settings.title)
        assertFalse(v.state.value.canUndo)
    }

    @Test
    fun undoIsEnabledWhileTyping() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val original = v.state.value.project.settings.title
        v.beginGesture()
        v.setText("Lu")
        assertTrue(v.state.value.canUndo)
        v.undo()
        assertEquals(original, v.state.value.project.settings.title)
        assertFalse(v.state.value.canUndo)
    }

    @Test
    fun perCardTextNeedsSelection() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setTextScope(TextScope.CARD)
        assertEquals(TextScope.ALL, v.state.value.textScope)
        assertTrue(v.events.first() is EditorEvent.Message)
        v.selectSlot(1); v.setTextScope(TextScope.CARD); v.setText("El brindis")
        val p = v.state.value.project
        assertEquals("El brindis", TextResolver.text(p, 1, TextRole.TITLE))
        assertEquals(p.settings.title, TextResolver.text(p, 0, TextRole.TITLE))
    }

    @Test
    fun applyToAllWhileTextFieldIsFocusedUndoesOnlyThatAction() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("C34048")
        v.selectSlot(1); v.setTextScope(TextScope.CARD)
        v.beginGesture(); v.setText("El brindis")          // el campo sigue con el foco: transacción abierta
        v.applyTextToAll()
        assertEquals(v.state.value.project.settings.title, TextResolver.text(v.state.value.project, 1, TextRole.TITLE))
        v.undo()
        val p = v.state.value.project
        assertEquals("El brindis", TextResolver.text(p, 1, TextRole.TITLE))
        assertEquals("C34048", p.settings.accentHex)
    }

    @Test
    fun typingAfterApplyToAllIsStillOneUndoStep() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val original = v.state.value.project.settings.title
        v.selectSlot(1); v.setTextScope(TextScope.CARD)
        v.beginGesture(); v.setText("El brindis")
        v.applyTextToAll()                                   // el campo sigue enfocado
        v.setTextScope(TextScope.ALL)
        v.setText("a"); v.setText("ab")
        v.undo()                                             // un solo paso: vuelve al estado justo después de aplicar
        assertEquals(original, v.state.value.project.settings.title)
        assertTrue(v.state.value.canUndo)
        v.undo()                                             // el segundo revierte «Aplicar a todas»
        assertEquals("El brindis", TextResolver.text(v.state.value.project, 1, TextRole.TITLE))
    }

    @Test
    fun redoSurvivesReopeningTheTextField() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("C34048")
        v.beginGesture()
        v.undo()
        assertEquals("92394A", v.state.value.project.settings.accentHex)
        assertTrue(v.state.value.canRedo)
        v.redo()
        assertEquals("C34048", v.state.value.project.settings.accentHex)
    }

    @Test
    fun autosaveAfterDelay() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("486855")
        advanceTimeBy(500)
        assertEquals("92394A", store.load(id).project.settings.accentHex)
        advanceTimeBy(500); advanceUntilIdle()
        assertEquals("486855", store.load(id).project.settings.accentHex)
    }

    @Test
    fun flushSavesPendingChanges() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("38536F")
        assertTrue(v.state.value.hasUnsavedChanges)
        assertTrue(v.flush())
        assertFalse(v.state.value.hasUnsavedChanges)
        assertEquals("38536F", store.load(id).project.settings.accentHex)
    }

    @Test
    fun cancelledAutosaveCannotOverwriteLatestProjectOrThumbnail() = runTest(main.dispatcher) {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val v = vm { project, _ ->
            if (project.name == "Antiguo") {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
            }
            project.name.toByteArray()
        }
        advanceUntilIdle()
        v.rename("Antiguo")
        advanceTimeBy(800); runCurrent()
        entered.await()
        val photo = store.importPhoto(id, "photo".byteInputStream(), "jpg", PhotoInfo(800, 600, null))
        v.rename("Último"); v.setAccent("486855"); v.addPhotosForTest(listOf(photo))
        advanceTimeBy(800); runCurrent()
        val savingWhileBlocked = v.state.value.saving
        release.complete(Unit)
        advanceUntilIdle()
        assertLatestSaved(v, photo)
        assertEquals("Último", store.thumbnailFile(id)!!.readText())
        assertTrue(savingWhileBlocked)
    }

    @Test
    fun cancelledFlushAndClearingWaitForOldSaveBeforeSavingLatest() = runTest(main.dispatcher) {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var thumbnails = 0
        val v = vm { project, _ ->
            thumbnails++
            if (project.name == "Antiguo") {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
            }
            project.name.toByteArray()
        }
        val models = ViewModelStore().apply { put("editor", v) }
        advanceUntilIdle()
        v.rename("Antiguo")
        val oldFlush = launch { v.flush() }
        runCurrent(); entered.await()
        val photo = store.importPhoto(id, "photo".byteInputStream(), "jpg", PhotoInfo(800, 600, null))
        v.rename("Último"); v.setAccent("486855"); v.addPhotosForTest(listOf(photo))
        oldFlush.cancel()
        v.flushAsync(); runCurrent()
        models.clear(); runCurrent()
        release.complete(Unit)
        advanceUntilIdle()
        assertLatestSaved(v, photo)
        val savedThumbnails = thumbnails
        v.flush()
        assertEquals(savedThumbnails, thumbnails) // el último guardado quitó dirty
    }

    private fun assertLatestSaved(v: EditorViewModel, photo: PhotoAsset) {
        val loaded = store.load(id)
        assertEquals("Último", loaded.project.name)
        assertEquals("486855", loaded.project.settings.accentHex)
        assertEquals(listOf(photo.id), loaded.project.photos.map { it.id })
        assertEquals(0, loaded.missingPhotos)
        assertTrue(File(photo.path).exists())
        val meta = store.list().single()
        assertEquals("Último", meta.name)
        assertEquals(loaded.project.updatedAtEpochMs, meta.updatedAtEpochMs)
        assertFalse(v.state.value.saving)
    }

    @Test
    fun addPhotosFillsFromSelectionAndPlaceAdvances() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.selectSlot(2)
        v.addPhotos(listOf("a", "b")); advanceUntilIdle()
        val pl = v.state.value.project.placements
        assertNotNull(pl[2]); assertNotNull(pl[3]); assertNull(pl[0])
        v.selectSlot(0)
        v.placePhoto(v.state.value.project.photos[0].id)
        assertNotNull(v.state.value.project.placements[0])
        assertEquals(1, v.state.value.selectedSlot) // siguiente espacio vacío de la hoja
    }

    @Test
    fun lowResolutionIsDetected() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val small = store.importPhoto(id, "x".byteInputStream(), "jpg", PhotoInfo(120, 120, null))
        v.addPhotosForTest(listOf(small))
        assertTrue(v.lowResSlots().isNotEmpty())
    }

    @Test
    fun removeSelectedPhotoIsUndoable() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.addPhotos(listOf("a")); advanceUntilIdle()
        v.events.first() // "1 foto agregada"
        v.selectSlot(0); v.removeSelectedPhoto()
        val e = v.events.first() as EditorEvent.Message
        assertTrue(e.undoable)
        v.undo()
        assertNotNull(v.state.value.project.placements[0])
    }

    @Test
    fun saveFailureKeepsDirtyAndWarns() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        // Se reemplaza la carpeta del proyecto por un ARCHIVO con el mismo nombre: mkdirs() no
        // hace nada y escribir dentro lanza IOException. No depende de permisos del sistema
        // de archivos (en macOS/root los permisos de sólo lectura no siempre fallan).
        val dir = store.projectDir(id)
        assertTrue(dir.deleteRecursively())
        assertTrue(dir.createNewFile())
        v.setAccent("486855")
        assertFalse(v.flush())
        assertFalse(v.state.value.saving)
        assertTrue(v.state.value.hasUnsavedChanges)
        assertTrue(v.state.value.saveFailed)
        val e = v.events.first() as EditorEvent.Message
        assertEquals(R.string.editor_save_failed, e.text.id)
        // sigue pendiente: al restaurar la carpeta, el siguiente guardado se completa
        assertTrue(dir.delete()); assertTrue(File(dir, "photos").mkdirs())
        assertTrue(v.flush())
        assertFalse(v.state.value.hasUnsavedChanges)
        assertFalse(v.state.value.saveFailed)
        assertEquals("486855", store.load(id).project.settings.accentHex)
    }

    @Test
    fun thumbnailWriteFailureKeepsPhotoAndDirtyUntilRetrySucceeds() = runTest(main.dispatcher) {
        var attempts = 0
        val v = vm { _, _ -> attempts++; "thumb".toByteArray() }
        advanceUntilIdle()
        val photo = store.importPhoto(id, "photo".byteInputStream(), "jpg", PhotoInfo(800, 600, null))
        v.addPhotosForTest(listOf(photo)); v.flush()
        val thumbnail = store.thumbnailFile(id)!!
        assertTrue(thumbnail.delete()); assertTrue(thumbnail.mkdir())
        File(thumbnail, "keep").writeText("keep")
        v.setAccent("486855"); v.flush()
        assertFalse(v.state.value.saving)
        assertTrue(File(photo.path).exists())
        assertEquals(R.string.editor_save_failed, (v.events.first() as EditorEvent.Message).text.id)
        val failedAttempts = attempts
        assertTrue(thumbnail.deleteRecursively())
        v.flush()
        assertEquals(failedAttempts + 1, attempts)
        assertEquals("486855", store.load(id).project.settings.accentHex)
        assertEquals(0, store.load(id).missingPhotos)
        v.flush()
        assertEquals(failedAttempts + 1, attempts)
    }

    @Test
    fun jpegExportUsesImageMimeAndSelectedPage() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.addPhotos((0..9).map { "photo-$it" }); advanceUntilIdle()
        v.setPage(1)
        v.exportJpg(); advanceUntilIdle()
        assertEquals(1, exportedPage)
        val exported = v.events.first { it is EditorEvent.Exported } as EditorEvent.Exported
        assertEquals("image/jpeg", exported.mime)
        assertEquals("a.jpg", exported.file.name)
        assertFalse(exported.pdf)
        assertFalse(v.state.value.busy)
    }

    @Test
    fun losslessPdfDisablesPhotoJpegEncoding() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.exportPdf(optimizePhotos = false); advanceUntilIdle()
        val exported = v.events.first { it is EditorEvent.Exported } as EditorEvent.Exported
        assertEquals(false, optimizedPdf)
        assertEquals("application/pdf", exported.mime)
        assertTrue(exported.pdf)
    }

    @Test
    fun exportFailureBecomesMessage() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val failing = EditorViewModel(id, EditorDeps(store, object : PhotoSource {
            override suspend fun import(projectId: String, uris: List<String>) = ImportResult(emptyList(), 0)
        }, object : ExportService {
            override suspend fun pdf(project: PolarProject, template: Bitmap?, optimizePhotos: Boolean): File = throw PolarException("Sin espacio")
            override suspend fun png(project: PolarProject, page: Int, template: Bitmap?): File = throw IllegalStateException("x")
            override suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?): File = throw IllegalStateException("x")
        }, { _, _ -> null }, { null }, main.dispatcher, CoroutineScope(main.dispatcher)))
        advanceUntilIdle()
        failing.exportPdf(); advanceUntilIdle()
        val raw = failing.events.first() as EditorEvent.Message
        assertEquals(R.string.raw_text, raw.text.id)
        failing.exportPng(); advanceUntilIdle()
        val other = failing.events.first() as EditorEvent.Message
        assertEquals(R.string.action_failed, other.text.id)
        assertFalse(failing.state.value.busy)
    }

    @Test
    fun saveWarningIsSentOnlyOnceUntilASaveSucceeds() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val dir = store.projectDir(id)
        assertTrue(dir.deleteRecursively()); assertTrue(dir.createNewFile())
        v.setAccent("486855"); v.flush()
        v.setAccent("38536F"); v.flush()
        assertEquals(R.string.editor_save_failed, (v.events.first() as EditorEvent.Message).text.id)
        assertNull(withTimeoutOrNull(1000) { v.events.first() }) // el segundo fallo no repite el aviso
        assertTrue(dir.delete()); assertTrue(File(dir, "photos").mkdirs())
        v.flush() // guardado correcto: se rearma el aviso
        assertTrue(dir.deleteRecursively()); assertTrue(dir.createNewFile())
        v.setAccent("C34048"); v.flush()
        assertEquals(R.string.editor_save_failed, (v.events.first() as EditorEvent.Message).text.id)
    }

    private fun vmWith(projectId: String, photos: PhotoSource) = EditorViewModel(projectId, EditorDeps(store, photos, object : ExportService {
        override suspend fun pdf(project: PolarProject, template: Bitmap?, optimizePhotos: Boolean) = File(tmp.root, "a.pdf")
        override suspend fun png(project: PolarProject, page: Int, template: Bitmap?) = File(tmp.root, "a.png")
        override suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?) = File(tmp.root, "a.jpg")
    }, { _, _ -> null }, { null }, main.dispatcher, CoroutineScope(main.dispatcher)))

    @Test
    fun losingSelectionReturnsScopeToAll() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val original = v.state.value.project.settings.title
        v.selectSlot(1); v.setTextScope(TextScope.CARD)
        assertEquals(TextScope.CARD, v.state.value.textScope)
        v.selectSlot(1) // quita la selección
        assertNull(v.state.value.selectedSlot)
        assertEquals(TextScope.ALL, v.state.value.textScope)
        v.setText("x")
        val p = v.state.value.project
        assertEquals("x", p.settings.title)
        assertNotEquals(original, p.settings.title)
        assertTrue(p.cardOverrides.isEmpty())
    }

    @Test
    fun clearSelectionAlsoReturnsScopeToAll() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.selectSlot(0); v.setTextScope(TextScope.CARD)
        v.clearSelection()
        assertEquals(TextScope.ALL, v.state.value.textScope)
    }

    @Test
    fun importFailureClearsBusyAndWarns() = runTest(main.dispatcher) {
        vm(); advanceUntilIdle()
        val failing = vmWith(id, object : PhotoSource {
            override suspend fun import(projectId: String, uris: List<String>): ImportResult = throw java.io.IOException("disco")
        })
        advanceUntilIdle()
        failing.addPhotos(listOf("a")); advanceUntilIdle()
        assertFalse(failing.state.value.busy)
        val e = failing.events.first() as EditorEvent.Message
        assertEquals(R.string.action_failed, e.text.id)
    }

    @Test
    fun missingProjectSetsLoadFailed() = runTest(main.dispatcher) {
        vm(); advanceUntilIdle()
        val broken = vmWith("no-existe", object : PhotoSource {
            override suspend fun import(projectId: String, uris: List<String>) = ImportResult(emptyList(), 0)
        })
        advanceUntilIdle()
        assertTrue(broken.state.value.loadFailed)
        assertFalse(broken.state.value.loading)
        val e = broken.events.first() as EditorEvent.Message
        assertEquals(R.string.editor_load_failed, e.text.id)
    }

    @Test
    fun failedLoadNeverOverwritesStoredProject() = runTest(main.dispatcher) {
        vm(); advanceUntilIdle()
        val file = File(store.projectDir(id), "project.polar")
        file.writeText("{")
        val broken = vmWith(id, object : PhotoSource {
            override suspend fun import(projectId: String, uris: List<String>) = ImportResult(emptyList(), 0)
        })
        advanceUntilIdle()
        assertTrue(broken.state.value.loadFailed)
        broken.setAccent("C34048")
        advanceTimeBy(5000); advanceUntilIdle()
        broken.flush()
        broken.flushAsync(); advanceUntilIdle()
        ViewModelStore().apply { put("editor", broken); clear() }
        advanceUntilIdle()
        assertEquals("{", file.readText())
        assertFalse(broken.state.value.canUndo)
    }

    @Test
    fun templateLoadFailureDoesNotFailTheOpen() = runTest(main.dispatcher) {
        store = ProjectStore(tmp.root)
        val base = PolarProject().normalized()
        val withTemplate = base.copy(settings = base.settings.copy(style = TemplateStyle.IMPORTED, importedTemplate = ImportedTemplate(path = "x.png", pixelWidth = 100, pixelHeight = 100, regions = listOf(TemplateRegion(x = 0.1, y = 0.1, width = 0.5, height = 0.5)))))
        id = store.create(withTemplate, "Con plantilla")
        val v = EditorViewModel(id, EditorDeps(store, object : PhotoSource {
            override suspend fun import(projectId: String, uris: List<String>) = ImportResult(emptyList(), 0)
        }, object : ExportService {
            override suspend fun pdf(project: PolarProject, template: Bitmap?, optimizePhotos: Boolean) = File(tmp.root, "a.pdf")
            override suspend fun png(project: PolarProject, page: Int, template: Bitmap?) = File(tmp.root, "a.png")
            override suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?) = File(tmp.root, "a.jpg")
        }, { _, _ -> null }, { throw java.io.IOException("sin archivo") }, main.dispatcher, CoroutineScope(main.dispatcher)))
        advanceUntilIdle()
        assertFalse(v.state.value.loadFailed)
        assertFalse(v.state.value.loading)
        assertNull(v.templateBitmap)
        assertEquals("Con plantilla", v.state.value.project.name)
        assertEquals(R.string.editor_template_missing, (v.events.first() as EditorEvent.Message).text.id)
    }
}
