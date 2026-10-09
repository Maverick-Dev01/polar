package com.polar.app.template

import com.polar.app.model.SwiftDate
import com.polar.app.model.TemplateRegion
import com.polar.app.model.newId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** `meta.json` de un molde guardado. Los nombres de los campos son los del plan. */
@Serializable
data class SavedTemplate(
    val id: String,
    @SerialName("nombre") val name: String,
    val dhash: String,
    val sha256: String,
    @SerialName("regiones") val regions: List<TemplateRegion>,
    @SerialName("creado") val createdAt: Double,
    @SerialName("archivo") val fileName: String = "molde.png",
    @SerialName("ancho") val pixelWidth: Int = 0,
    @SerialName("alto") val pixelHeight: Int = 0
)

/** Un molde guardado que se parece al que se quiere guardar. */
data class TemplateMatch(val template: SavedTemplate, val distance: Int, val sameFile: Boolean)

/**
 * «Mis moldes»: `templates/<id>/molde.png` + `meta.json` bajo [root] (`filesDir`).
 * Cada proyecto lleva su propia copia del molde, así que borrar uno de aquí no rompe ningún proyecto.
 */
class TemplateLibrary(root: File, private val clock: () -> Double = SwiftDate::now) {
    private val dir = File(root, "templates")
    private companion object { val ALLOWED_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp") }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun imageFile(t: SavedTemplate): File = File(File(dir, t.id), t.fileName)

    @Synchronized
    fun list(): List<SavedTemplate> =
        (dir.listFiles() ?: emptyArray()).filter { it.isDirectory }
            .mapNotNull { runCatching { json.decodeFromString(SavedTemplate.serializer(), File(it, "meta.json").readText()) }.getOrNull() }
            .filter { imageFile(it).exists() }
            .sortedByDescending { it.createdAt }

    @Synchronized
    fun get(id: String): SavedTemplate? = list().firstOrNull { it.id == id }

    /** Copia la imagen y escribe `meta.json`; deja la carpeta completa o nada. */
    @Synchronized
    fun save(name: String, source: File, regions: List<TemplateRegion>, fingerprint: TemplateFingerprint, pixelWidth: Int, pixelHeight: Int): SavedTemplate {
        val id = newId()
        val target = File(dir, id)
        val staging = File(dir, ".$id.tmp")
        try {
            staging.mkdirs()
            val ext = source.extension.lowercase().takeIf { it in ALLOWED_EXTENSIONS } ?: "png" // la imagen se reconoce por su contenido
            val fileName = "molde.$ext"
            source.copyTo(File(staging, fileName), overwrite = true)
            val meta = SavedTemplate(id, name.trim(), fingerprint.dhashHex, fingerprint.sha256, regions, clock(), fileName, pixelWidth, pixelHeight)
            writeMeta(File(staging, "meta.json"), meta)
            if (!staging.renameTo(target)) throw IOException("No se pudo guardar el molde")
            return meta
        } finally {
            staging.deleteRecursively()
        }
    }

    @Synchronized
    fun rename(id: String, name: String) {
        val t = get(id) ?: return
        writeMeta(File(File(dir, id), "meta.json"), t.copy(name = name.trim()))
    }

    /** Escribe a un temporal y lo renombra: un corte a medias nunca deja un meta.json truncado. */
    private fun writeMeta(target: File, meta: SavedTemplate) {
        val tmp = File(target.parentFile, ".${target.name}.${System.nanoTime()}.tmp")
        try {
            tmp.writeText(json.encodeToString(SavedTemplate.serializer(), meta))
            java.nio.file.Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally { tmp.delete() }
    }

    @Synchronized
    fun delete(id: String): Boolean = File(dir, id).takeIf { it.isDirectory }?.deleteRecursively() ?: false

    /** El molde guardado más parecido, si es el mismo archivo (SHA) o su dHash dista [TemplateFingerprint.DUPLICATE_DISTANCE] o menos. */
    fun findDuplicate(fingerprint: TemplateFingerprint): TemplateMatch? {
        val candidates = list().mapNotNull { t ->
            val sameFile = t.sha256 == fingerprint.sha256
            val distance = runCatching { TemplateFingerprint.distance(TemplateFingerprint.parse(t.dhash), fingerprint.dhash) }.getOrNull() ?: return@mapNotNull null
            if (sameFile || distance <= TemplateFingerprint.DUPLICATE_DISTANCE) TemplateMatch(t, if (sameFile) 0 else distance, sameFile) else null
        }
        return candidates.minWithOrNull(compareBy({ it.distance }, { -it.template.createdAt }))
    }
}
