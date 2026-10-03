package com.polar.app.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.polar.app.model.PhotoAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(val assets: List<PhotoAsset>, val failed: Int)

interface PhotoSource {
    suspend fun import(projectId: String, uris: List<String>): ImportResult
}

/** Copia cada foto elegida al proyecto; los originales del usuario no se tocan. */
class PhotoImporter(
    private val context: Context,
    private val store: ProjectStore,
    private val loader: BitmapLoader
) : PhotoSource {
    override suspend fun import(projectId: String, uris: List<String>): ImportResult = withContext(Dispatchers.IO) {
        val assets = mutableListOf<PhotoAsset>()
        var failed = 0
        for (text in uris) {
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
