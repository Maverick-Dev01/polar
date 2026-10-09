package com.polar.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderImagesTest {
    private fun file(name: String, mime: String? = null) = FolderEntry(name, "content://t/$name", mime, false)

    @Test
    fun ordersByNameIgnoringCase() {
        val r = FolderImages.select(listOf(file("b.jpg"), file("A.png"), file("c.WEBP")))
        assertEquals(listOf("content://t/A.png", "content://t/b.jpg", "content://t/c.WEBP"), r.uris)
        assertEquals(0, r.ignored)
    }

    @Test
    fun acceptsKnownExtensionsAndImageMimeAndCountsTheRest() {
        val entries = listOf(
            file("a.jpg"), file("b.jpeg"), file("c.png"), file("d.heic"), file("e.webp"),
            file("notas.txt"), file("clip.mp4", "video/mp4"), file("sin_extension", "image/jpeg"),
            FolderEntry("Subcarpeta", "content://t/sub", null, true)
        )
        val r = FolderImages.select(entries, sdk = 34)
        assertEquals(6, r.uris.size)
        // txt y mp4 se ignoran; la subcarpeta no cuenta como archivo.
        assertEquals(2, r.ignored)
    }

    @Test
    fun capsAtFiveHundredAndCountsTheExcessAsIgnored() {
        val entries = (1..520).map { file("img%04d.jpg".format(it)) }
        val r = FolderImages.select(entries)
        assertEquals(500, r.uris.size)
        assertEquals("content://t/img0001.jpg", r.uris.first())
        assertEquals("content://t/img0500.jpg", r.uris.last())
        assertEquals(0, r.ignored)
        assertEquals(20, r.omitted)
    }

    @Test
    fun emptyFolder() {
        val r = FolderImages.select(emptyList())
        assertEquals(0, r.uris.size)
        assertEquals(0, r.ignored)
    }

    @Test
    fun heicIsExcludedBelowApi28AndCountedSeparately() {
        val entries = listOf(file("a.jpg"), file("b.HEIC"), file("c.heif"), file("d.png", "image/png"), file("x.txt"))
        val old = FolderImages.select(entries, sdk = 27)
        assertEquals(listOf("content://t/a.jpg", "content://t/d.png"), old.uris)
        assertEquals(2, old.unsupported)
        assertEquals(1, old.ignored)
        val modern = FolderImages.select(entries, sdk = 28)
        assertEquals(4, modern.uris.size)
        assertEquals(0, modern.unsupported)
    }

    @Test
    fun heicDetectedByMimeWithoutExtension() {
        val old = FolderImages.select(listOf(file("IMG_0001", "image/heic")), sdk = 26)
        assertEquals(0, old.uris.size)
        assertEquals(1, old.unsupported)
    }
}
