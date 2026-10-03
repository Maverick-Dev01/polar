package com.polar.app.model

import org.junit.Assert.*
import org.junit.Test

class PolarJsonTest {
    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test
    fun decodesMacPolaroidProject() {
        val p = PolarJson.decode(fixture("mac_polaroid.polar"))
        assertEquals(TemplateStyle.POLAROID, p.settings.style)
        assertTrue(p.photos.isNotEmpty())
        assertEquals(p.photos[0].id, p.placements[0]!!.assetID)
        assertEquals(812592909.24548, p.settings.specialDate, 0.0001)
        assertEquals(PaperSize.LETTER, p.settings.paperSize)
    }

    @Test
    fun decodesMacTextStylesAndPaper() {
        val p = PolarJson.decode(fixture("mac_oficio_textstyles.polar"))
        assertEquals(PaperSize.OFICIO, p.settings.paperSize)
        assertEquals(PaperOrientation.LANDSCAPE, p.settings.orientation)
        assertEquals("Recuerdos para siempre", p.settings.title)
        val title = p.settings.textStyle(TextRole.TITLE)
        assertTrue(title.bold)
        assertEquals(13.0, title.size, 0.0)
        assertEquals(12.0, title.offsetX, 0.0)
        assertEquals("Georgia", title.fontName)
    }

    @Test
    fun decodesMacImportedTemplate() {
        val p = PolarJson.decode(fixture("mac_imported.polar"))
        assertEquals(TemplateStyle.IMPORTED, p.settings.style)
        assertTrue(p.settings.importedTemplate!!.regions.isNotEmpty())
    }

    @Test
    fun roundTripStaysMacReadable() {
        for (name in listOf("mac_polaroid.polar", "mac_oficio_textstyles.polar", "mac_imported.polar", "mac_calendar.polar")) {
            val p = PolarJson.decode(fixture(name))
            val text = PolarJson.encode(p)
            MacCompat.assertReadable(text)
            assertEquals(p, PolarJson.decode(text))
        }
    }

    @Test
    fun newFieldsNeverBreakMac() {
        val photo = PhotoAsset(path = "photos/a.jpg", pixelWidth = 100, pixelHeight = 100, takenAtEpochMs = 1_771_070_400_000)
        val dateStyle = TextAppearance(fontName = "Caveat", hex = "FF8C2E")
        val p = PolarProject(
            settings = PrintSettings(dateSource = DateSource.PHOTO).withTextStyle(TextRole.DATE, dateStyle),
            photos = listOf(photo),
            placements = listOf(PhotoPlacement(assetID = photo.id)),
            name = "Boda",
            cardOverrides = mapOf("0" to CardOverride(texts = mapOf("caption" to "El brindis")))
        )
        val text = PolarJson.encode(p)
        MacCompat.assertReadable(text) // falla si "date" aparece en settings.textStyles
        val back = PolarJson.decode(text)
        assertEquals("FF8C2E", back.settings.textStyle(TextRole.DATE).hex)
        assertEquals("El brindis", back.cardOverrides["0"]!!.texts["caption"])
    }

    @Test
    fun rejectsInvalidFileWithSpanishMessage() {
        val e = assertThrows(PolarException::class.java) { PolarJson.decode("{\"version\": 7}") }
        assertTrue(e.message!!.contains("versión"))
        assertThrows(PolarException::class.java) { PolarJson.decode("no es json") }
    }
}
