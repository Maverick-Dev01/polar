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
        val r = FolderImages.select(entries)
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
        assertEquals(20, r.ignored)
    }

    @Test
    fun emptyFolder() {
        val r = FolderImages.select(emptyList())
        assertEquals(0, r.uris.size)
        assertEquals(0, r.ignored)
    }
}
