package com.polar.app.ui.catalog

import com.polar.app.MainDispatcherRule
import com.polar.app.R
import com.polar.app.data.ProjectStore
import com.polar.app.model.PaperSize
import com.polar.app.model.TemplateStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class CatalogViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun vm(store: ProjectStore, text: String = "", read: (suspend (String) -> String)? = null) = CatalogViewModel(
        store = store, readText = read ?: { text }, copyTemplate = { _, _ -> File("x") },
        defaultPaper = { PaperSize.A4 },
        newName = "Nuevo diseño", templateName = "Mi plantilla", openedName = "Diseño abierto",
        io = main.dispatcher
    )

    @Test
    fun createFromStyleUsesDefaultPaperAndStyleGrid() = runTest(main.dispatcher) {
        val store = ProjectStore(tmp.root)
        val v = vm(store)
        v.createFromStyle(TemplateStyle.FILM_VERTICAL); advanceUntilIdle()
        val created = v.events.first() as CatalogEvent.Created
        assertNull(created.notice)
        val id = created.id
        val p = store.load(id).project
        assertEquals(TemplateStyle.FILM_VERTICAL, p.settings.style)
        assertEquals(PaperSize.A4, p.settings.paperSize)
        assertEquals(2, p.settings.columns)
        assertEquals("Nuevo diseño", p.name)
    }

    @Test
    fun openMacPolarCreatesProjectAndReportsMissingPhotos() = runTest(main.dispatcher) {
        val store = ProjectStore(tmp.root)
        // Las rutas absolutas de la Mac pueden existir en la máquina que corre la prueba; se redirigen a una ruta inexistente.
        val text = javaClass.classLoader!!.getResource("fixtures/mac_polaroid.polar")!!.readText()
            .replace("\\/Users\\/", "\\/nonexistent-polar-test\\/")
        val v = vm(store, text)
        v.openPolar("content://x", "Mi primer diseño.polar"); advanceUntilIdle()
        val e = v.events.first() as CatalogEvent.Created
        assertEquals("Mi primer diseño", store.load(e.id).project.name)
        assertEquals(R.string.catalog_missing_photos, e.notice!!.id)
        assertEquals(listOf(store.load(e.id).missingPhotos), e.notice!!.args)
    }

    @Test
    fun invalidPolarShowsError() = runTest(main.dispatcher) {
        val v = vm(ProjectStore(tmp.root), "no es un diseño")
        v.openPolar("content://x", "roto.polar"); advanceUntilIdle()
        assertTrue(v.events.first() is CatalogEvent.Error)
    }

    @Test
    fun openPolarWithIoFailureShowsError() = runTest(main.dispatcher) {
        val store = ProjectStore(tmp.root)
        val v = vm(store, read = { throw IOException("disco lleno") })
        v.openPolar("content://x", "a.polar"); advanceUntilIdle()
        val e = v.events.first() as CatalogEvent.Error
        assertEquals(R.string.action_failed, e.text.id)
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun doubleCreateIsIgnoredWhileBusy() = runTest(main.dispatcher) {
        val store = ProjectStore(tmp.root)
        val v = vm(store)
        v.createFromStyle(TemplateStyle.POLAROID)
        v.createFromStyle(TemplateStyle.MINI)
        advanceUntilIdle()
        assertEquals(1, store.list().size)
    }

    @Test
    fun createFromSavedTemplateCopiesTheImageIntoTheNewProject() = runTest(main.dispatcher) {
        val store = ProjectStore(tmp.root)
        val library = com.polar.app.template.TemplateLibrary(tmp.root)
        val source = File(tmp.root, "src.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val regions = listOf(com.polar.app.model.TemplateRegion(x = 0.1, y = 0.1, width = 0.5, height = 0.5, shape = com.polar.app.model.RegionShape.ELLIPSE))
        val saved = library.save("Mi molde", source, regions, com.polar.app.template.TemplateFingerprint(5L, "sha"), 100, 200)
        val v = CatalogViewModel(store, { "" }, { _, _ -> File("x") }, { PaperSize.A4 }, "Nuevo", "Plantilla", "Abierto", main.dispatcher, library)
        v.createFromSavedTemplate(saved); advanceUntilIdle()
        val created = v.events.first() as CatalogEvent.Created
        val p = store.load(created.id).project
        assertEquals("Mi molde", p.name)
        assertEquals(TemplateStyle.IMPORTED, p.settings.style)
        val template = p.settings.importedTemplate!!
        assertTrue(File(template.path).readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(template.path.startsWith(store.projectDir(created.id).absolutePath))
        assertEquals(com.polar.app.model.RegionShape.ELLIPSE, template.regions[0].shape)
        library.delete(saved.id) // el proyecto conserva su copia
        assertTrue(File(store.load(created.id).project.settings.importedTemplate!!.path).exists())
    }
}
