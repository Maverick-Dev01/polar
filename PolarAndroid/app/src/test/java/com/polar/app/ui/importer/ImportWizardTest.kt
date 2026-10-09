package com.polar.app.ui.importer

import com.polar.app.MainDispatcherRule
import com.polar.app.SharedFixtures
import com.polar.app.model.ImportedTemplate
import com.polar.app.model.RegionShape
import com.polar.app.model.TemplateRegion
import com.polar.app.template.*
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImportWizardTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private val regions = listOf(TemplateRegion(x = 0.1, y = 0.1, width = 0.3, height = 0.3), TemplateRegion(x = 0.5, y = 0.5, width = 0.3, height = 0.3))
    private fun load(approx: Boolean = false) = TemplateLoad(ImportedTemplate("x.png", 100, 100, regions), 2, approx, TemplateFingerprint(1L, "sha"))
    private fun saved(name: String = "Viejo") = SavedTemplate("ID", name, "0000000000000001", "sha", regions, 1.0)
    private val file = File("x.png")
    private val start = ImportWizardState()

    // ---- navegación ----

    @Test fun startsOnPickAndGoesStraightToReviewWithoutDuplicate() {
        assertEquals(WizardStep.PICK, start.step); assertEquals(1, start.stepNumber)
        val s = ImportWizard.loaded(start, file, load(), null)
        assertEquals(WizardStep.REVIEW, s.step); assertEquals(2, s.stepNumber); assertEquals(0, s.selected)
    }

    @Test fun duplicateWarningSitsBetweenStepsOneAndTwo() {
        val s = ImportWizard.loaded(start, file, load(), TemplateMatch(saved(), 2, false))
        assertEquals(WizardStep.DUPLICATE, s.step); assertEquals(1, s.stepNumber)
        assertEquals(WizardStep.REVIEW, ImportWizard.keepAsNew(s).step)
    }

    @Test fun reviewThenNameThenBackWalksTheSteps() {
        val review = ImportWizard.loaded(start, file, load(), null)
        val name = ImportWizard.next(review)
        assertEquals(WizardStep.NAME, name.step); assertEquals(3, name.stepNumber)
        assertEquals(WizardStep.REVIEW, ImportWizard.back(name)!!.step)
        assertEquals(WizardStep.PICK, ImportWizard.back(review)!!.step)
        assertNull(ImportWizard.back(start))
    }

    @Test fun backFromReviewReturnsToTheDuplicateWarningWhenThereWasOne() {
        val review = ImportWizard.keepAsNew(ImportWizard.loaded(start, file, load(), TemplateMatch(saved(), 0, true)))
        assertEquals(WizardStep.DUPLICATE, ImportWizard.back(review)!!.step)
    }

    @Test fun cannotAdvanceWithoutRegionsOrSaveWithoutName() {
        val empty = ImportWizard.loaded(start, file, load(), null).copy(regions = emptyList())
        assertEquals(WizardStep.REVIEW, ImportWizard.next(empty).step)
        val named = ImportWizard.setName(ImportWizard.loaded(start, file, load(), null), "  ")
        assertFalse(named.canSave)
        assertTrue(ImportWizard.setName(named, "Mi molde").canSave)
    }

    // ---- edición de espacios ----

    private val review = ImportWizard.loaded(start, file, load(), null)

    @Test fun moveStaysInsideTheImage() {
        val m = ImportWizard.move(review, 0, -5.0, 5.0).regions[0]
        assertEquals(0.0, m.x, 1e-9); assertEquals(1.0 - 0.3, m.y, 1e-9)
    }

    @Test fun editingDetectedTransparentSpaceKeepsPhotoVisibleOverOpaqueArtwork() {
        val state = review.copy(regions = listOf(TemplateRegion(x = 0.1, y = 0.1,
            width = 0.2, height = 0.2, isTransparent = true)))
        val edited = listOf(
            ImportWizard.move(state, 0, 0.5, 0.0),
            ImportWizard.resize(state, 0, Corner.BOTTOM_RIGHT, 0.1, 0.1),
            ImportWizard.grow(state, 0, 0.1, 0.1),
            ImportWizard.setShape(state, 0, RegionShape.ELLIPSE),
            ImportWizard.setRadius(state, 0, 0.2)
        )
        edited.forEach { assertFalse(it.regions[0].isTransparent) }
        assertTrue(state.regions[0].isTransparent)
    }

    @Test fun resizingACornerKeepsTheOppositeOneFixed() {
        val r = ImportWizard.resize(review, 0, Corner.BOTTOM_RIGHT, 0.1, 0.05).regions[0]
        assertEquals(0.1, r.x, 1e-9); assertEquals(0.1, r.y, 1e-9); assertEquals(0.4, r.width, 1e-9); assertEquals(0.35, r.height, 1e-9)
        val t = ImportWizard.resize(review, 0, Corner.TOP_LEFT, 0.05, 0.05).regions[0]
        assertEquals(0.15, t.x, 1e-9); assertEquals(0.4, t.x + t.width, 1e-9)
    }

    @Test fun resizeNeverCollapsesBelowTheMinimum() {
        val r = ImportWizard.resize(review, 0, Corner.BOTTOM_RIGHT, -5.0, -5.0).regions[0]
        assertEquals(0.02, r.width, 1e-9); assertEquals(0.02, r.height, 1e-9)
    }

    @Test fun shapePickerSetsDefaultRadiusForRoundAndClearsItOtherwise() {
        val round = ImportWizard.setShape(review, 0, RegionShape.ROUND).regions[0]
        assertEquals(RegionShape.ROUND, round.shape); assertEquals(0.2, round.radius, 1e-9)
        val tuned = ImportWizard.setRadius(ImportWizard.setShape(review, 0, RegionShape.ROUND), 0, 0.35).regions[0]
        assertEquals(0.35, tuned.radius, 1e-9)
        val oval = ImportWizard.setShape(ImportWizard.setShape(review, 0, RegionShape.ROUND), 0, RegionShape.ELLIPSE).regions[0]
        assertEquals(0.0, oval.radius, 1e-9)
    }

    @Test fun addAndRemoveRegionsRespectTheLimits() {
        val added = ImportWizard.add(review)
        assertEquals(3, added.regions.size); assertEquals(2, added.selected)
        assertEquals(added.regions.map { it.id }.toSet().size, 3)
        val removed = ImportWizard.remove(added, 2)
        assertEquals(2, removed.regions.size); assertEquals(1, removed.selected)
        var only = ImportWizard.remove(ImportWizard.remove(removed, 0), 0)
        assertEquals(1, only.regions.size) // nunca se queda sin espacios
        val full = review.copy(regions = List(64) { TemplateRegion(x = 0.1, y = 0.1, width = 0.2, height = 0.2) })
        assertEquals(64, ImportWizard.add(full).regions.size)
    }

    @Test fun accessibilityGrowKeepsTheCenter() {
        val r = ImportWizard.grow(review, 0, 0.02, 0.02).regions[0]
        assertEquals(0.25, r.x + r.width / 2, 1e-9); assertEquals(0.25, r.y + r.height / 2, 1e-9); assertEquals(0.32, r.width, 1e-9)
    }

    @Test fun hitTestingPrefersCornersOfTheSelectionThenTheSmallestRegion() {
        assertEquals(Corner.BOTTOM_RIGHT, ImportWizard.cornerAt(regions[0], 0.405, 0.395, 0.03, 0.03))
        assertNull(ImportWizard.cornerAt(regions[0], 0.25, 0.25, 0.03, 0.03))
        val nested = listOf(TemplateRegion(x = 0.0, y = 0.0, width = 1.0, height = 1.0), TemplateRegion(x = 0.4, y = 0.4, width = 0.2, height = 0.2))
        assertEquals(1, ImportWizard.regionAt(nested, 0.5, 0.5)); assertEquals(0, ImportWizard.regionAt(nested, 0.1, 0.1)); assertNull(ImportWizard.regionAt(regions, 0.9, 0.9))
    }

    // ---- con el ViewModel y las imágenes reales ----

    private fun vm(library: TemplateLibrary) = ImportWizardViewModel(
        library, copyPicked = { uri -> File(tmp.root, "cache-${System.nanoTime()}.png").also { SharedFixtures.file("moldes/$uri").copyTo(it) } }, io = main.dispatcher
    )

    @Test fun fullFlowSavesThenWarnsAboutADuplicateAndReusesTheExisting() = runTest(main.dispatcher) {
        val library = TemplateLibrary(tmp.root)
        val first = vm(library)
        first.pick("molde-rects.png"); advanceUntilIdle()
        assertEquals(WizardStep.REVIEW, first.state.value.step)
        assertEquals(3, first.state.value.regions.size)
        first.next(); first.setName("Mis rectas"); first.save(); advanceUntilIdle()
        val done = first.events.first() as ImportWizardEvent.Done
        assertFalse(done.reusedExisting); assertEquals("Mis rectas", done.template.name)
        assertEquals(1, library.list().size)

        // El mismo molde reescalado al 50 %: aviso entre los pasos 1 y 2.
        val second = vm(library)
        second.pick("molde-rects-50.png"); advanceUntilIdle()
        assertEquals(WizardStep.DUPLICATE, second.state.value.step)
        assertEquals("Mis rectas", second.state.value.match!!.template.name)
        second.useExisting(); advanceUntilIdle()
        val reused = second.events.first() as ImportWizardEvent.Done
        assertTrue(reused.reusedExisting); assertEquals(done.template.id, reused.template.id)
        assertEquals(1, library.list().size)

        // «Guardar como nuevo» sigue al paso 2 y guarda otro.
        val third = vm(library)
        third.pick("molde-rects-50.png"); advanceUntilIdle()
        third.keepAsNew(); assertEquals(WizardStep.REVIEW, third.state.value.step)
        third.next(); third.setName("Otra copia"); third.save(); advanceUntilIdle()
        assertEquals(2, library.list().size)
    }

    @Test fun circlesAreDetectedAsOvalsInTheWizard() = runTest(main.dispatcher) {
        val v = vm(TemplateLibrary(tmp.root))
        v.pick("molde-circles.png"); advanceUntilIdle()
        assertTrue(v.state.value.regions.all { it.shape == RegionShape.ELLIPSE })
    }

    @Test fun backFromTheDuplicateWarningDropsThePickedCopy() = runTest(main.dispatcher) {
        val library = TemplateLibrary(tmp.root)
        val a = vm(library); a.pick("molde-rects.png"); advanceUntilIdle(); a.next(); a.setName("x"); a.save(); advanceUntilIdle()
        val b = vm(library); b.pick("molde-rects.png"); advanceUntilIdle()
        val copy = b.state.value.file!!
        assertTrue(copy.exists())
        b.back()
        assertEquals(WizardStep.PICK, b.state.value.step); assertFalse(copy.exists())
    }

    @Test fun unreadableImageReportsAnErrorAndStaysOnStepOne() = runTest(main.dispatcher) {
        val v = ImportWizardViewModel(TemplateLibrary(tmp.root), copyPicked = { File(tmp.root, "malo.png").also { it.writeText("no soy una imagen") } }, io = main.dispatcher)
        v.pick("x"); advanceUntilIdle()
        assertTrue(v.events.first() is ImportWizardEvent.Error)
        assertEquals(WizardStep.PICK, v.state.value.step); assertFalse(v.state.value.busy)
    }
}
