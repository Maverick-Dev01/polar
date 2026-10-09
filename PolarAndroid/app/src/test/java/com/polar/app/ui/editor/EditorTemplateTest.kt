package com.polar.app.ui.editor

import android.graphics.Bitmap
import com.polar.app.MainDispatcherRule
import com.polar.app.SharedFixtures
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.*
import com.polar.app.export.ExportQuality
import com.polar.app.model.*
import com.polar.app.template.TemplateImporter
import com.polar.app.template.TemplateLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorTemplateTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun setup(): Triple<EditorViewModel, ProjectStore, TemplateLibrary> {
        val store = ProjectStore(File(tmp.root, "app"))
        val library = TemplateLibrary(File(tmp.root, "app"))
        val id = store.create(PolarProject().normalized(), "Boda")
        val photos = object : PhotoSource { override suspend fun import(projectId: String, uris: List<String>) = ImportResult(emptyList(), 0) }
        val exports = object : ExportService {
            override suspend fun pdf(project: PolarProject, template: Bitmap?, quality: ExportQuality) = File("a")
            override suspend fun png(project: PolarProject, page: Int, template: Bitmap?, quality: ExportQuality) = File("a")
            override suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?, quality: ExportQuality) = File("a")
        }
        val vm = EditorViewModel(id, EditorDeps(store, photos, exports, { _, _ -> null }, { Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888) }, main.dispatcher,
            CoroutineScope(main.dispatcher), templates = library))
        return Triple(vm, store, library)
    }

    private fun saveFixture(library: TemplateLibrary, name: String): com.polar.app.template.SavedTemplate {
        val load = TemplateImporter.loadTemplate(SharedFixtures.file("moldes/$name"))
        return library.save(name, SharedFixtures.file("moldes/$name"), load.template.regions, load.fingerprint!!, load.template.pixelWidth, load.template.pixelHeight)
    }

    @Test fun useTemplateGivesTheProjectItsOwnCopySoDeletingTheMoldBreaksNothing() = runTest(main.dispatcher) {
        val (vm, store, library) = setup(); advanceUntilIdle()
        val saved = saveFixture(library, "molde-circles.png")
        vm.openMyTemplates()
        assertEquals(EditorMode.CHANGE_DESIGN, vm.state.value.mode); assertTrue(vm.state.value.catalogMine)
        vm.useTemplate(saved); advanceUntilIdle()
        val project = vm.state.value.project
        assertEquals(TemplateStyle.IMPORTED, project.settings.style)
        assertEquals(EditorMode.EDIT, vm.state.value.mode); assertFalse(vm.state.value.catalogMine)
        assertEquals(3, project.settings.capacity)
        assertTrue(project.settings.importedTemplate!!.regions.all { it.shape == RegionShape.ELLIPSE })
        assertNotNull(vm.templateBitmap)
        // Borrar el molde de «Mis moldes»: el proyecto sigue completo.
        assertTrue(library.delete(saved.id))
        assertTrue(File(project.settings.importedTemplate!!.path).exists())
        assertTrue(vm.flush())
        val reopened = store.load(store.list().single().id).project
        assertEquals(TemplateStyle.IMPORTED, reopened.settings.style)
        assertTrue(File(reopened.settings.importedTemplate!!.path).exists())
    }

    @Test fun undoRestoresThePreviousDesign() = runTest(main.dispatcher) {
        val (vm, _, library) = setup(); advanceUntilIdle()
        vm.useTemplate(saveFixture(library, "molde-rounded.png")); advanceUntilIdle()
        vm.undo(); advanceUntilIdle()
        assertEquals(TemplateStyle.POLAROID, vm.state.value.project.settings.style)
    }
}
