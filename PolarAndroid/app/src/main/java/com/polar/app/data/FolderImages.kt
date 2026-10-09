package com.polar.app.data

/** Un elemento del nivel superior de una carpeta elegida con el selector de carpetas. */
data class FolderEntry(val name: String, val uri: String, val mime: String?, val isDirectory: Boolean)

/** [uris] ya ordenadas y recortadas al tope; [ignored] cuenta archivos que no son imagen y los que pasan del tope. */
data class FolderListing(val uris: List<String>, val ignored: Int)

object FolderImages {
    const val MAX = 500
    private val extensions = setOf("jpg", "jpeg", "png", "heic", "webp")
    private val mimes = setOf("image/jpeg", "image/png", "image/heic", "image/heif", "image/webp")

    fun isImage(entry: FolderEntry): Boolean {
        if (entry.isDirectory) return false
        val ext = entry.name.substringAfterLast('.', "").lowercase()
        return ext in extensions || entry.mime?.lowercase() in mimes
    }

    fun select(entries: List<FolderEntry>): FolderListing {
        val files = entries.filter { !it.isDirectory }
        val images = files.filter(::isImage).sortedBy { it.name.lowercase() }
        val kept = images.take(MAX)
        return FolderListing(kept.map { it.uri }, (files.size - kept.size))
    }
}
