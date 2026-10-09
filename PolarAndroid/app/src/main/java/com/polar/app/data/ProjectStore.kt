package com.polar.app.data

import com.polar.app.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Serializable
data class ProjectMeta(
    val id: String,
    val name: String,
    val style: TemplateStyle,
    val sheets: Int,
    val updatedAtEpochMs: Long
)

data class LoadedProject(val project: PolarProject, val missingPhotos: Int)

/**
 * Cada proyecto vive en `projects/<id>/` con project.polar (rutas relativas), photos/, thumb.png y meta.json.
 * Borrar mueve la carpeta a `trash/` para poder deshacer.
 */
class ProjectStore(private val root: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val projects = File(root, "projects").apply { mkdirs() }
    private val trash = File(root, "trash").apply { mkdirs() }
    private companion object {
        const val TRASHED_AT = ".trashed-at"
        const val TRASH_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    }
    private val metaJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun projectDir(id: String): File = File(projects, id)
    fun thumbnailFile(id: String): File? = File(projectDir(id), "thumb.png").takeIf { it.exists() }

    @Synchronized
    fun list(): List<ProjectMeta> =
        (projects.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { dir -> runCatching { readMeta(dir.name) }.getOrNull() }
            .sortedByDescending { it.updatedAtEpochMs }

    fun create(project: PolarProject, name: String): String {
        val id = newId()
        File(projectDir(id), "photos").mkdirs()
        save(id, project.copy(name = name))
        return id
    }

    @Synchronized
    fun load(id: String): LoadedProject {
        val dir = projectDir(id)
        val stored = PolarJson.decode(File(dir, "project.polar").readText())
        val photos = stored.photos.map { it.copy(path = absolute(dir, it.path), maskPath = it.maskPath?.let { path -> absolute(dir, path) }) }
        val template = stored.settings.importedTemplate?.let { it.copy(path = absolute(dir, it.path)) }
        val project = stored.copy(photos = photos, settings = stored.settings.copy(importedTemplate = template))
        val missing = photos.count { !it.path.startsWith("content:") && !File(it.path).exists() }
        return LoadedProject(project, missing)
    }

    // ponytail: bloqueo por store; usar bloqueos por proyecto si se editan muchos diseños a la vez.
    @Synchronized
    fun save(id: String, project: PolarProject, thumbnailPng: ByteArray? = null): ProjectMeta {
        val dir = projectDir(id).apply { mkdirs() }
        val now = clock()
        val stored = project.copy(
            updatedAtEpochMs = now,
            photos = project.photos.map { it.copy(path = relative(dir, it.path), maskPath = it.maskPath?.let { path -> relative(dir, path) }) },
            settings = project.settings.copy(importedTemplate = project.settings.importedTemplate?.let { it.copy(path = relative(dir, it.path)) })
        )
        writeAtomic(File(dir, "project.polar"), PolarJson.encode(stored).toByteArray())
        val meta = ProjectMeta(id, project.name, project.settings.style, project.pageCount, now)
        writeAtomic(File(dir, "meta.json"), metaJson.encodeToString(ProjectMeta.serializer(), meta).toByteArray())
        thumbnailPng?.let { writeAtomic(File(dir, "thumb.png"), it) }
        return meta
    }

    fun importPhoto(id: String, source: InputStream, extension: String, info: PhotoInfo): PhotoAsset {
        val assetId = newId()
        val file = File(File(projectDir(id), "photos").apply { mkdirs() }, "$assetId.${extension.lowercase()}")
        try {
            source.use { input -> file.outputStream().use { input.copyTo(it) } }
        } catch (e: Throwable) {
            file.delete()
            throw e
        }
        return PhotoAsset(id = assetId, path = file.absolutePath, pixelWidth = info.width, pixelHeight = info.height, takenAtEpochMs = info.takenAtEpochMs)
    }

    fun importTemplateFile(id: String, source: InputStream, extension: String): File {
        val file = File(projectDir(id), "template-${newId()}.${extension.lowercase()}")
        source.use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    fun importPolar(text: String, name: String): String {
        val project = PolarJson.decode(text)
        return create(project, name)
    }

    fun exportPolar(id: String): String = File(projectDir(id), "project.polar").readText()

    @Synchronized
    fun duplicate(id: String, newName: String): String {
        val copyId = newId()
        projectDir(id).copyRecursively(projectDir(copyId))
        val loaded = load(copyId) // rutas relativas → apuntan a la copia
        save(copyId, loaded.project.copy(name = newName))
        return copyId
    }

    @Synchronized
    fun rename(id: String, name: String) {
        save(id, load(id).project.copy(name = name))
    }

    /** Último borrado de esta sesión: todavía se puede deshacer, así que ninguna limpieza lo toca. */
    @Volatile private var lastDeleted: String? = null

    fun delete(id: String) {
        val target = File(trash, id)
        target.deleteRecursively()
        if (!projectDir(id).renameTo(target)) throw IOException("No se pudo borrar el diseño")
        runCatching { File(target, TRASHED_AT).writeText(clock().toString()) }
        lastDeleted = id
    }

    fun restore(id: String) {
        val source = File(trash, id)
        if (source.exists() && !source.renameTo(projectDir(id))) throw IOException("No se pudo recuperar el diseño")
        File(projectDir(id), TRASHED_AT).delete()
        if (lastDeleted == id) lastDeleted = null
    }

    /** Elimina para siempre sólo este elemento de la papelera (p. ej. un diseño a medio crear). */
    fun purge(id: String) {
        File(trash, id).deleteRecursively()
        if (lastDeleted == id) lastDeleted = null
    }

    private fun trashEntries(): List<File> =
        (trash.listFiles() ?: emptyArray()).filter { it.isDirectory && !File(projects, it.name).exists() }

    /** Elementos que "Vaciar papelera" borraría: todos menos el último deshacible. */
    fun trashCount(): Int = trashEntries().count { it.name != lastDeleted }

    /** Vacía la papelera; nunca toca proyectos activos ni el último borrado que aún se puede deshacer. */
    fun emptyTrash(): Int = trashEntries().filter { it.name != lastDeleted }.count { it.deleteRecursively() }

    /** Al iniciar la app: lo borrado hace más de [maxAgeMs] se elimina; lo reciente sigue recuperable. */
    fun purgeExpiredTrash(maxAgeMs: Long = TRASH_MAX_AGE_MS): Int {
        val now = clock()
        return trashEntries().filter { it.name != lastDeleted }.count { dir ->
            val at = runCatching { File(dir, TRASHED_AT).readText().trim().toLong() }.getOrDefault(dir.lastModified())
            now - at > maxAgeMs && dir.deleteRecursively()
        }
    }

    private fun readMeta(id: String): ProjectMeta {
        val file = File(projectDir(id), "meta.json")
        if (file.exists()) {
            runCatching { return metaJson.decodeFromString(ProjectMeta.serializer(), file.readText()) }
        }
        val p = load(id).project
        return ProjectMeta(id, p.name, p.settings.style, p.pageCount, p.updatedAtEpochMs)
    }

    private fun relative(dir: File, path: String): String {
        val prefix = dir.absolutePath + File.separator
        return if (path.startsWith(prefix)) path.removePrefix(prefix) else path
    }

    private fun absolute(dir: File, path: String): String =
        if (path.startsWith("/") || path.startsWith("content:")) path else File(dir, path).absolutePath

    private fun writeAtomic(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile, ".${file.name}.${System.nanoTime()}.tmp")
        try {
            tmp.writeBytes(bytes)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            tmp.delete()
        }
    }
}
