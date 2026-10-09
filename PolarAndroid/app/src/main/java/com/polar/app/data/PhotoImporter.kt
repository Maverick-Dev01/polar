package com.polar.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.polar.app.model.PhotoAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class ImportResult(val assets: List<PhotoAsset>, val failed: Int)

interface PhotoSource {
    suspend fun import(projectId: String, uris: List<String>): ImportResult

    /** Lista las imágenes del nivel superior de una carpeta elegida con el selector de carpetas. */
    suspend fun listFolder(treeUri: String): FolderListing = FolderListing(emptyList(), 0)
}

/** Copia cada foto elegida al proyecto; los originales del usuario no se tocan. */
class PhotoImporter(
    private val context: Context,
    private val store: ProjectStore,
    private val loader: BitmapLoader
) : PhotoSource {
    override suspend fun listFolder(treeUri: String): FolderListing = withContext(Dispatchers.IO) {
        val tree = Uri.parse(treeUri)
        val parent = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val entries = mutableListOf<FolderEntry>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        // Los errores (permiso revocado, carpeta ya no disponible) se propagan: no equivalen a una carpeta vacía.
        val cursor = context.contentResolver.query(parent, projection, null, null, null) ?: throw java.io.IOException("Sin acceso a la carpeta")
        cursor.use { c ->
            while (c.moveToNext()) {
                ensureActive()
                val mime = c.getString(2)
                entries += FolderEntry(
                    c.getString(1) ?: "", DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)).toString(),
                    mime, mime == DocumentsContract.Document.MIME_TYPE_DIR
                )
            }
        }
        FolderImages.select(entries)
    }

    override suspend fun import(projectId: String, uris: List<String>): ImportResult = withContext(Dispatchers.IO) {
        val assets = mutableListOf<PhotoAsset>()
        var failed = 0
        for (text in uris) {
            ensureActive()
            val uri = Uri.parse(text)
            val info = loader.readInfo(uri)
            val stream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            if (info == null || stream == null) { failed++; stream?.close(); continue }
            val mime = context.contentResolver.getType(uri)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            try {
                assets += store.importPhoto(projectId, stream, ext, info)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed++
            }
        }
        ImportResult(assets, failed)
    }
}
