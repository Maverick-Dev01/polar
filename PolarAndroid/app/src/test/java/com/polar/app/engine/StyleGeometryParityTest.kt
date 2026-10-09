package com.polar.app.engine

import com.polar.app.SharedFixtures
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

/**
 * Paridad con la Mac: lee `shared-fixtures/estilos-geometria.json` desde el repositorio (no desde el recurso
 * empaquetado) y lo compara con lo que Android realmente calcula, con tolerancia de 0.5 pt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StyleGeometryParityTest {
    private val tol = 0.5
    private val file = SharedFixtures.file("estilos-geometria.json")
    private val expected = SharedGeometry.parse(file.readText())
    private val ids = Json.parseToJsonElement(file.readText()).jsonObject["styles"]!!.jsonObject.keys

    private fun styleOf(id: String) = TemplateStyle.entries.first { SharedGeometry.id(it) == id }

    @Test fun theBundledCopyIsTheSharedFile() {
        val bundled = SharedGeometry::class.java.classLoader!!.getResourceAsStream(SharedGeometry.RESOURCE)!!.readBytes()
        assertArrayEquals(file.readBytes(), bundled)
    }

    @Test fun everyStyleOfTheFixtureExistsInTheApp() {
        assertEquals(6, ids.size)
        for (id in ids) assertNotNull(id, TemplateStyle.entries.firstOrNull { SharedGeometry.id(it) == id })
    }

    @Test fun catalogMetadataMatches() {
        for ((id, geo) in expected.styles) {
            val s = styleOf(id)
            assertEquals(id, geo.displayName, s.displayName)
            assertEquals(id, geo.description, s.description)
            assertEquals(id, geo.category, s.category.name)
            assertEquals(id, geo.photosPerCard, s.photosPerCard)
            assertEquals(id, geo.cardAspect, s.defaultAspect, 1e-9)
            assertEquals(id, geo.supportsDate, s.supportsDate)
            assertEquals(id, geo.textRoles, s.textRoles.map { it.name })
            assertEquals(id, geo.qrSlot != null, s.supportsQr && s.category == DesignCategory.MUSIC)
        }
    }

    @Test fun photoAndTextRectsInPointsMatchAtSeveralCardWidths() {
        for ((id, geo) in expected.styles) for (cardWidth in listOf(180.0, 123.0, 300.0)) {
            val style = styleOf(id)
            val card = PolarRect(10.0, 20.0, 10.0 + cardWidth, 20.0 + cardWidth / geo.cardAspect)
            val settings = PrintSettings(style = style)
            val photos = PolarRenderer.calculatePhotoRects(card, style, settings)
            assertEquals(id, geo.photoSlots.size, photos.size)
            for ((i, slot) in geo.photoSlots.withIndex()) {
                assertRect("$id foto $i", card, slot.x, slot.y, slot.w, slot.h, photos[i])
            }
            val project = ProjectEdits.setDateSource(PolarProject(settings = PrintSettings(style = style)).normalized(), DateSource.CHOSEN)
                .let { ProjectEdits.setChosenDate(it, 1_771_070_400_000) }
            val items = CardTextLayout.items(project, card, 0, photos, 0xFF92394A.toInt(), TimeZone.getTimeZone("UTC"))
            assertEquals(id, geo.textSlots.map { it.role }.sorted(), items.map { it.role!!.name }.sorted())
            for (slot in geo.textSlots) {
                val item = items.single { it.role!!.name == slot.role }
                assertRect("$id texto ${slot.role}", card, slot.x, slot.y, slot.w, slot.h, item.rect)
                assertEquals(slot.defaultSizePt * cardWidth / expected.referenceCardWidthPt, item.sizePt, 1e-6)
                assertEquals(slot.bold, item.defaultBold)
                assertEquals(slot.defaultFont, item.defaultFont)
                assertEquals(android.graphics.Color.parseColor(slot.color), item.defaultColor)
                assertEquals(slot.align, item.align.name.lowercase())
            }
        }
    }

    @Test fun photoShapesMatch() {
        for ((id, geo) in expected.styles) {
            val shapes = SharedGeometry.of(styleOf(id))!!.photoSlots
            assertEquals(geo.photoSlots.map { it.shape to it.radius }, shapes.map { it.shape to it.radius })
        }
        assertEquals(RegionShape.ELLIPSE, expected.styles["vinyl"]!!.photoSlots.single().regionShape)
    }

    @Test fun qrSlotsMatchInPoints() {
        for ((id, geo) in expected.styles) {
            val slot = geo.qrSlot ?: continue
            val card = PolarRect(0.0, 0.0, 180.0, 180.0 / geo.cardAspect)
            val rect = GeometryDrawing.qrRect(card, QrSlots.of(styleOf(id))!!)
            assertEquals(id, slot.x * card.width, rect.left, tol)
            assertEquals(id, slot.y * card.height, rect.top, tol)
            assertEquals(id, slot.size * card.width, rect.width, tol)
            assertEquals(id, slot.size * card.width, rect.height, tol)
        }
    }

    @Test fun decorationsAreAllKnownPrimitivesWithinTheCard() {
        val known = setOf("rect", "roundRect", "ellipse", "ring", "stripes")
        for ((id, geo) in expected.styles) for (d in geo.decorations) {
            assertTrue("$id ${d.type}", d.type in known)
            assertTrue("$id ${d.layer}", d.layer == "below" || d.layer == "above")
        }
    }

    private fun assertRect(label: String, card: PolarRect, x: Double, y: Double, w: Double, h: Double, actual: PolarRect) {
        assertEquals("$label left", card.left + x * card.width, actual.left, tol)
        assertEquals("$label top", card.top + y * card.height, actual.top, tol)
        assertEquals("$label width", w * card.width, actual.width, tol)
        assertEquals("$label height", h * card.height, actual.height, tol)
    }
}
