package com.polar.app.model

import com.polar.app.core.edit.ProjectEdits
import com.polar.app.ui.catalog.CatalogSearch
import org.junit.Assert.*
import org.junit.Test

/** El `.polar` con campos o estilos de la fase 3 se lee desde versiones anteriores y viceversa. */
class Fase3CompatTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test fun unknownStyleDegradesToPolaroid() {
        val text = fixture("mac_polaroid.polar").replace("\"style\" : \"polaroid\"", "\"style\" : \"hologramaDelFuturo\"")
        assertNotEquals(fixture("mac_polaroid.polar"), text)
        val p = PolarJson.decode(text)
        assertEquals(TemplateStyle.POLAROID, p.settings.style)
    }

    @Test fun newStylesRoundTripThroughTheFile() {
        for (style in TemplateStyle.entries.filter { it != TemplateStyle.IMPORTED }) {
            val p = ProjectEdits.selectStyle(PolarProject(), style)
            val encoded = PolarJson.encode(p)
            assertEquals(style, PolarJson.decode(encoded).settings.style)
        }
        assertTrue("\"photobooth\"" in PolarJson.encode(ProjectEdits.selectStyle(PolarProject(), TemplateStyle.PHOTOBOOTH)))
        assertTrue("\"instaxWide\"" in PolarJson.encode(ProjectEdits.selectStyle(PolarProject(), TemplateStyle.INSTAX_WIDE)))
    }

    @Test fun unknownStyleInAPageOrCardOverrideDoesNotFailTheFile() {
        val p = ProjectEdits.selectStyle(PolarProject(), TemplateStyle.POLAROID).let { ProjectEdits.setPageDesign(it, 0, TemplateStyle.MINI) }
        val text = PolarJson.encode(p).replace("\"mini\"", "\"estiloNuevo\"")
        assertEquals(TemplateStyle.POLAROID, PolarJson.decode(text).pageDesigns["0"]!!.style)
    }

    @Test fun oldRegionsWithoutShapeAreRectangles() {
        val p = PolarJson.decode(fixture("mac_imported.polar"))
        for (r in p.settings.importedTemplate!!.regions) { assertEquals(RegionShape.RECT, r.shape); assertEquals(0.0, r.radius, 0.0) }
    }

    @Test fun regionShapeAndRadiusRoundTripAndUnknownShapeFallsBackToRect() {
        val p = PolarJson.decode(fixture("mac_imported.polar"))
        val t = p.settings.importedTemplate!!
        val shaped = p.copy(settings = p.settings.copy(importedTemplate = t.copy(regions = t.regions.mapIndexed { i, r -> if (i == 0) r.copy(shape = RegionShape.ROUND, radius = 0.2) else r })))
        val text = PolarJson.encode(shaped)
        assertEquals(RegionShape.ROUND, PolarJson.decode(text).settings.importedTemplate!!.regions[0].shape)
        assertEquals(0.2, PolarJson.decode(text).settings.importedTemplate!!.regions[0].radius, 1e-9)
        val future = text.replace("\"round\"", "\"estrella\"")
        assertEquals(RegionShape.RECT, PolarJson.decode(future).settings.importedTemplate!!.regions[0].shape)
    }

    @Test fun macCanStillReadWhatWeWriteForNewStylesAndShapes() {
        val p = PolarJson.decode(fixture("mac_imported.polar"))
        MacCompat.assertReadable(PolarJson.encode(p))
        MacCompat.assertReadable(PolarJson.encode(ProjectEdits.selectStyle(PolarProject(), TemplateStyle.COLLAGE)))
    }

    @Test fun applyTemplateKeepsPhotosAndTextAndSwitchesToImported() {
        val photo = PhotoAsset(path = "a.jpg", pixelWidth = 10, pixelHeight = 10)
        val base = ProjectEdits.selectStyle(PolarProject(photos = listOf(photo), placements = listOf(PhotoPlacement(photo.id))), TemplateStyle.POLAROID)
            .let { ProjectEdits.setText(it, TextRole.TITLE, "Hola") }
        val template = ImportedTemplate("t.png", 100, 200, listOf(TemplateRegion(x = 0.1, y = 0.1, width = 0.4, height = 0.3, shape = RegionShape.ELLIPSE), TemplateRegion(x = 0.5, y = 0.5, width = 0.3, height = 0.3)))
        val next = ProjectEdits.applyTemplate(base, template)
        next.validated()
        assertEquals(TemplateStyle.IMPORTED, next.settings.style)
        assertEquals(2, next.settings.capacity)
        assertEquals("Hola", next.settings.title)
        assertEquals(photo.id, next.placements[0]!!.assetID)
        assertEquals(RegionShape.ELLIPSE, next.settings.importedTemplate!!.regions[0].shape)
    }

    @Test fun newDesignsAreFoundByNameAndDescriptionInTheCatalog() {
        val all = TemplateStyle.entries.filter { it != TemplateStyle.IMPORTED }
        assertEquals(listOf(TemplateStyle.PHOTOBOOTH), CatalogSearch.filter(all, "fotomaton"))
        assertTrue(TemplateStyle.VINYL in CatalogSearch.filter(all, "disco"))
        assertTrue(TemplateStyle.CASSETTE in CatalogSearch.filter(all, "casete"))
        assertTrue(TemplateStyle.WASHI in CatalogSearch.filter(all, "cinta"))
        assertTrue(TemplateStyle.COLLAGE in CatalogSearch.filter(all, "collage"))
        assertTrue(TemplateStyle.INSTAX_WIDE in CatalogSearch.filter(all, "ancha"))
    }

    @Test fun categoriesPutTheNewDesignsWhereTheSpecSays() {
        assertEquals(DesignCategory.CLASSIC, TemplateStyle.PHOTOBOOTH.category)
        assertEquals(DesignCategory.CLASSIC, TemplateStyle.INSTAX_WIDE.category)
        assertEquals(DesignCategory.MUSIC, TemplateStyle.VINYL.category)
        assertEquals(DesignCategory.MUSIC, TemplateStyle.CASSETTE.category)
        assertEquals(DesignCategory.FREE, TemplateStyle.COLLAGE.category)
        assertEquals(DesignCategory.OCCASIONS, TemplateStyle.WASHI.category)
    }
}
