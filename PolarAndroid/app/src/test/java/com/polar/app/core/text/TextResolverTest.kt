package com.polar.app.core.text

import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class TextResolverTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val photo = PhotoAsset(path = "a.jpg", pixelWidth = 10, pixelHeight = 10, takenAtEpochMs = 1_771_070_400_000) // 14 feb 2026 12:00 UTC
    private val base = PolarProject(
        settings = PrintSettings(title = "Lu y Max"),
        photos = listOf(photo),
        placements = listOf(PhotoPlacement(photo.id), PhotoPlacement(photo.id), null)
    ).normalized()

    @Test
    fun generalTextWhenNoOverride() {
        assertEquals("Lu y Max", TextResolver.text(base, 0, TextRole.TITLE))
        assertFalse(TextResolver.hasOwnText(base, 0, TextRole.TITLE))
    }

    @Test
    fun ownTextOnlyForThatCard() {
        val p = base.copy(cardOverrides = mapOf("1" to CardOverride(texts = mapOf("title" to "El brindis"))))
        assertEquals("Lu y Max", TextResolver.text(p, 0, TextRole.TITLE))
        assertEquals("El brindis", TextResolver.text(p, 1, TextRole.TITLE))
        assertTrue(TextResolver.hasOwnText(p, 1, TextRole.TITLE))
        assertEquals(1, TextResolver.ownTextCount(p, TextRole.TITLE))
    }

    @Test
    fun emptyOwnTextIsStillOwn() {
        val p = base.copy(cardOverrides = mapOf("1" to CardOverride(texts = mapOf("title" to ""))))
        assertEquals("", TextResolver.text(p, 1, TextRole.TITLE))
        assertTrue(TextResolver.hasOwnText(p, 1, TextRole.TITLE))
    }

    @Test
    fun appearanceFallsBackToGeneral() {
        val general = TextAppearance(hex = "112233")
        val p = base.copy(
            settings = base.settings.withTextStyle(TextRole.TITLE, general),
            cardOverrides = mapOf("1" to CardOverride(styles = mapOf("title" to general.copy(hex = "AABBCC"))))
        )
        assertEquals("112233", TextResolver.appearance(p, 0, TextRole.TITLE).hex)
        assertEquals("AABBCC", TextResolver.appearance(p, 1, TextRole.TITLE).hex)
        assertTrue(TextResolver.hasOwnAppearance(p, 1, TextRole.TITLE))
    }

    @Test
    fun dateFromPhotoChosenOrNone() {
        val fromPhoto = base.copy(settings = base.settings.copy(dateSource = DateSource.PHOTO))
        assertEquals("14 feb 2026", TextResolver.text(fromPhoto, 0, TextRole.DATE, utc))
        assertEquals("", TextResolver.text(fromPhoto, 2, TextRole.DATE, utc)) // tarjeta vacía
        assertEquals("", TextResolver.text(base, 0, TextRole.DATE, utc))      // NONE por defecto

        val chosen = SwiftDate.fromEpochMs(1_791_806_400_000) // 12 oct 2026 12:00 UTC
        val perCard = base.copy(cardOverrides = mapOf("1" to CardOverride(dateSource = DateSource.CHOSEN, chosenDate = chosen)))
        assertEquals("12 oct 2026", TextResolver.text(perCard, 1, TextRole.DATE, utc))
        assertEquals(DateSource.NONE, TextResolver.dateSource(perCard, 0))
    }

    @Test
    fun photoWithoutExifDateShowsNothing() {
        val noDate = photo.copy(id = newId(), takenAtEpochMs = null)
        val p = base.copy(photos = listOf(noDate), placements = listOf(PhotoPlacement(noDate.id)), settings = base.settings.copy(dateSource = DateSource.PHOTO))
        assertEquals("", TextResolver.text(p, 0, TextRole.DATE, utc))
    }
}
