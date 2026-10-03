package com.polar.app.data

import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ProjectStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1_000L
    private fun store() = ProjectStore(tmp.root) { now }
    private val info = PhotoInfo(800, 600, 1_771_070_400_000)

    @Test
    fun createSaveLoadKeepsPhotosRelativeOnDisk() {
        val s = store()
        val id = s.create(PolarProject().normalized(), "Boda")
        val asset = s.importPhoto(id, "jpegbytes".byteInputStream(), "jpg", info)
        assertTrue(File(asset.path).exists())
        val p = PolarProject(name = "Boda", photos = listOf(asset), placements = listOf(PhotoPlacement(asset.id))).normalized()
        s.save(id, p)
        val onDisk = File(s.projectDir(id), "project.polar").readText()
        assertTrue(onDisk.contains("\"photos/${asset.id}.jpg\""))
        val loaded = s.load(id)
        assertEquals(0, loaded.missingPhotos)
        assertEquals(asset.path, loaded.project.photos[0].path)
        assertEquals("Boda", loaded.project.name)
        assertEquals(1_771_070_400_000, loaded.project.photos[0].takenAtEpochMs)
    }

    @Test
    fun listIsNewestFirstWithMeta() {
        val s = store()
        val a = s.create(PolarProject(), "A"); now = 2_000
        val b = s.create(PolarProject(settings = PrintSettings(style = TemplateStyle.CALENDAR)), "B")
        val list = s.list()
        assertEquals(listOf(b, a), list.map { it.id })
        assertEquals(TemplateStyle.CALENDAR, list[0].style)
        assertEquals("B", list[0].name)
    }

    @Test
    fun duplicateCopiesPhotosAndRenames() {
        val s = store()
        val id = s.create(PolarProject(), "Pedido Ana")
        val asset = s.importPhoto(id, "x".byteInputStream(), "png", info)
        s.save(id, PolarProject(photos = listOf(asset), placements = listOf(PhotoPlacement(asset.id))))
        val copy = s.duplicate(id, "Pedido Ana (copia)")
        val loaded = s.load(copy)
        assertEquals("Pedido Ana (copia)", loaded.project.name)
        assertTrue(loaded.project.photos[0].path.startsWith(s.projectDir(copy).absolutePath))
        assertEquals(0, loaded.missingPhotos)
    }

    @Test
    fun deleteGoesToTrashAndCanBeRestored() {
        val s = store()
        val id = s.create(PolarProject(), "Borrar")
        s.delete(id)
        assertTrue(s.list().isEmpty())
        s.restore(id)
        assertEquals(listOf(id), s.list().map { it.id })
        s.delete(id); s.emptyTrash()
        s.restore(id)
        assertTrue(s.list().isEmpty())
    }

    @Test
    fun openMacProjectWithMissingPhotos() {
        val s = store()
        // Las rutas absolutas de la Mac pueden existir en la máquina que corre la prueba; se redirigen a una ruta inexistente.
        val text = javaClass.classLoader!!.getResource("fixtures/mac_polaroid.polar")!!.readText()
            .replace("\\/Users\\/", "\\/nonexistent-polar-test\\/")
        val id = s.importPolar(text, "Mi primer diseño")
        val loaded = s.load(id)
        assertEquals(loaded.project.photos.size, loaded.missingPhotos)
        assertTrue(loaded.project.photos.isNotEmpty())
        assertEquals("Mi primer diseño", loaded.project.name)
    }

    @Test
    fun exportPolarIsMacReadable() {
        val s = store()
        val id = s.create(PolarProject().normalized(), "Exportar")
        com.polar.app.model.MacCompat.assertReadable(s.exportPolar(id))
    }

    @Test
    fun renameUpdatesMeta() {
        val s = store()
        val id = s.create(PolarProject(), "Viejo")
        s.rename(id, "Nuevo")
        assertEquals("Nuevo", s.list().single().name)
        assertEquals("Nuevo", s.load(id).project.name)
    }

    @Test
    fun concurrentSavesKeepLatestProjectMetaAndThumbnailTogether() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val latestFinished = CountDownLatch(1)
        val workers = Executors.newFixedThreadPool(2)
        val s = ProjectStore(tmp.root) {
            if (Thread.currentThread().name == "old-save") {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                2_000L
            } else 3_000L
        }
        val id = s.create(PolarProject(), "Inicial")
        val old = workers.submit {
            Thread.currentThread().name = "old-save"
            s.save(id, PolarProject(name = "Antiguo"), "Antiguo".toByteArray())
        }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val latest = workers.submit {
                s.save(id, PolarProject(name = "Último"), "Último".toByteArray())
                latestFinished.countDown()
            }
            // Una espera acotada por la finalización, sin sleeps: si cruza el guardado viejo, éste lo pisará.
            latestFinished.await(1, TimeUnit.SECONDS)
            release.countDown()
            old.get(5, TimeUnit.SECONDS); latest.get(5, TimeUnit.SECONDS)
            val project = s.load(id).project
            val meta = s.list().single()
            assertEquals("Último", project.name)
            assertEquals("Último", meta.name)
            assertEquals(project.updatedAtEpochMs, meta.updatedAtEpochMs)
            assertEquals("Último", s.thumbnailFile(id)!!.readText())
        } finally {
            release.countDown()
            workers.shutdownNow()
        }
    }

    @Test
    fun corruptProjectIsSkippedInList() {
        val s = store()
        s.create(PolarProject(), "Bien")
        File(tmp.root, "projects/ROTO").mkdirs()
        File(tmp.root, "projects/ROTO/project.polar").writeText("{")
        assertEquals(listOf("Bien"), s.list().map { it.name })
    }

    @Test
    fun restoreOfUnknownIdDoesNothing() {
        val s = store()
        s.restore("NOEXISTE")
        assertTrue(s.list().isEmpty())
    }

    @Test
    fun corruptMetaFallsBackToProject() {
        val s = store()
        val id = s.create(PolarProject(), "Bien")
        File(s.projectDir(id), "meta.json").writeText("{")
        assertEquals(listOf("Bien"), s.list().map { it.name })
    }

    @Test
    fun failedPhotoCopyLeavesNoPartialFile() {
        val s = store()
        val id = s.create(PolarProject(), "Foto")
        val broken = object : java.io.InputStream() {
            override fun read(): Int = throw java.io.IOException("boom")
        }
        assertThrows(java.io.IOException::class.java) { s.importPhoto(id, broken, "jpg", info) }
        assertEquals(0, File(s.projectDir(id), "photos").listFiles()!!.size)
    }

    @Test
    fun failedAtomicReplacementKeepsPhotosAndCleansTemporaryFiles() {
        val s = store()
        val id = s.create(PolarProject(), "Foto")
        val photo = s.importPhoto(id, "photo".byteInputStream(), "jpg", info)
        val project = PolarProject(name = "Foto", photos = listOf(photo)).normalized()
        s.save(id, project)
        val meta = File(s.projectDir(id), "meta.json")
        assertTrue(meta.delete()); assertTrue(meta.mkdir())
        File(meta, "keep").writeText("keep")
        assertThrows(java.io.IOException::class.java) { s.save(id, project.copy(name = "Último")) }
        assertEquals("keep", File(meta, "keep").readText())
        assertTrue(File(photo.path).exists())
        assertFalse(s.projectDir(id).listFiles()!!.any { it.name.endsWith(".tmp") })
    }
}
