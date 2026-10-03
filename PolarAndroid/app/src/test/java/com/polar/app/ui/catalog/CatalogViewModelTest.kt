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
}
