package com.polar.app.data

import android.os.Build

/** Un elemento del nivel superior de una carpeta elegida con el selector de carpetas. */
data class FolderEntry(val name: String, val uri: String, val mime: String?, val isDirectory: Boolean)

/**
 * [uris] ya ordenadas y recortadas al tope.
 * [ignored]: archivos que no son imagen; [unsupported]: HEIC/HEIF en un Android que no los decodifica;
 * [omitted]: imágenes que pasan del tope.
 */
data class FolderListing(val uris: List<String>, val ignored: Int, val unsupported: Int = 0, val omitted: Int = 0)

object FolderImages {
    const val MAX = 500
    private val extensions = setOf("jpg", "jpeg", "png", "heic", "heif", "webp")
    private val mimes = setOf("image/jpeg", "image/png", "image/heic", "image/heif", "image/webp")

    fun isImage(entry: FolderEntry): Boolean {
        if (entry.isDirectory) return false
        val ext = entry.name.substringAfterLast('.', "").lowercase()
        return ext in extensions || entry.mime?.lowercase() in mimes
    }

    /** Android decodifica HEIC/HEIF desde la API 28. */
    fun isHeif(entry: FolderEntry): Boolean {
        val ext = entry.name.substringAfterLast('.', "").lowercase()
        return ext == "heic" || ext == "heif" || entry.mime?.lowercase().let { it == "image/heic" || it == "image/heif" }
    }

    fun select(entries: List<FolderEntry>, sdk: Int = Build.VERSION.SDK_INT): FolderListing {
        val files = entries.filter { !it.isDirectory }
        val images = files.filter(::isImage)
        val decodable = if (sdk >= 28) images else images.filterNot(::isHeif)
        val kept = decodable.sortedBy { it.name.lowercase() }.take(MAX)
        return FolderListing(
            uris = kept.map { it.uri },
            ignored = files.size - images.size,
            unsupported = images.size - decodable.size,
            omitted = decodable.size - kept.size
        )
    }
}
