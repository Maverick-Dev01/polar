package com.polar.app.template

import com.polar.app.SharedFixtures
import com.polar.app.model.RegionShape
import com.polar.app.model.TemplateRegion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TemplateLibraryTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1000.0
    private val library by lazy { TemplateLibrary(tmp.root) { now++ } }

    private fun analyze(name: String) = TemplateImporter.loadTemplate(SharedFixtures.file("moldes/$name"))
    private fun save(name: String, title: String = name): SavedTemplate {
        val load = analyze(name)
        return library.save(title, SharedFixtures.file("moldes/$name"), load.template.regions, load.fingerprint!!, load.template.pixelWidth, load.template.pixelHeight)
    }

    @Test fun savesAndListsNewestFirstWithTheMetaOfThePlan() {
        val a = save("molde-rects.png", "Rectas"); val b = save("molde-circles.png", "Círculos")
        assertEquals(listOf(b.id, a.id), library.list().map { it.id })
        assertTrue(File(tmp.root, "templates/${a.id}/molde.png").exists())
        val meta = File(tmp.root, "templates/${a.id}/meta.json").readText()
        for (key in listOf("\"id\"", "\"nombre\"", "\"dhash\"", "\"sha256\"", "\"regiones\"", "\"creado\"")) assertTrue(key, key in meta)
        assertEquals(16, a.dhash.length)
        assertEquals("0101011111111180", a.dhash)
        assertEquals(3, library.list().last().regions.size)
    }

    @Test fun persistsAcrossInstancesIncludingShapes() {
        save("molde-rounded.png")
        val reopened = TemplateLibrary(tmp.root).list().single()
        assertTrue(reopened.regions.all { it.shape == RegionShape.ROUND && Math.abs(it.radius - 0.2) < 0.01 })
    }

    @Test fun deleteRemovesTheFolderAndNothingElse() {
        val a = save("molde-rects.png"); val b = save("molde-circles.png")
        assertTrue(library.delete(a.id))
        assertEquals(listOf(b.id), library.list().map { it.id })
        assertFalse(File(tmp.root, "templates/${a.id}").exists())
        assertFalse(library.delete(a.id))
    }

    @Test fun sameFileIsADuplicateBySha() {
        save("molde-rects.png", "Rectas")
        val match = library.findDuplicate(analyze("molde-rects.png").fingerprint!!)!!
        assertTrue(match.sameFile); assertEquals("Rectas", match.template.name)
    }

    @Test fun rescaledCopyIsADuplicateByDhash() {
        save("molde-rects.png", "Rectas")
        val match = library.findDuplicate(analyze("molde-rects-50.png").fingerprint!!)!!
        assertFalse(match.sameFile); assertEquals(0, match.distance); assertEquals("Rectas", match.template.name)
    }

    @Test fun similarRoundedVersionIsOfferedAsSimilar() {
        save("molde-rects.png", "Rectas")
        val match = library.findDuplicate(analyze("molde-rounded.png").fingerprint!!)
        assertNotNull(match); assertTrue(match!!.distance in 1..TemplateFingerprint.DUPLICATE_DISTANCE)
    }

    @Test fun aDifferentTemplateIsNotADuplicate() {
        save("molde-rects.png")
        assertNull(library.findDuplicate(analyze("molde-distinto-tira.png").fingerprint!!))
    }

    @Test fun emptyLibraryFindsNothingAndSkipsBrokenFolders() {
        assertNull(library.findDuplicate(analyze("molde-rects.png").fingerprint!!))
        File(tmp.root, "templates/roto").mkdirs(); File(tmp.root, "templates/roto/meta.json").writeText("{")
        assertTrue(library.list().isEmpty())
    }

    @Test fun renameKeepsTheRest() {
        val a = save("molde-rects.png", "Viejo")
        library.rename(a.id, "  Nuevo ")
        assertEquals("Nuevo", library.get(a.id)!!.name)
        assertEquals(a.sha256, library.get(a.id)!!.sha256)
    }
}
