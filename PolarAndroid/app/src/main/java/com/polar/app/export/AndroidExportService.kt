package com.polar.app.export

import android.content.Context
import android.graphics.Bitmap
import com.polar.app.data.BitmapLoader
import com.polar.app.engine.FontProvider
import com.polar.app.engine.PolarRenderer
import com.polar.app.engine.PhotoFit
import com.polar.app.engine.PolarRect
import kotlin.math.ceil
import kotlin.math.max
import com.polar.app.model.PolarProject
import com.polar.app.model.PhotoAsset
import com.polar.app.model.PhotoPlacement
import com.polar.app.ui.editor.ExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidExportService(
    private val context: Context,
    private val bitmaps: BitmapLoader,
    private val fonts: FontProvider
) : ExportService {
    private fun dir() = File(context.cacheDir, "exports").apply { mkdirs() }
    private fun fileName(p: PolarProject, ext: String) =
        p.name.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifBlank { "Polar" } + ".$ext"

    private fun printTemplate(p: PolarProject, fallback: Bitmap?): Bitmap? {
        val t = p.settings.importedTemplate ?: return fallback
        val rect = PolarRenderer.templateRect(p.settings)
        val pixels = ceil(max(rect.width, rect.height) * 300 / 72).toInt().coerceIn(1, max(t.pixelWidth, t.pixelHeight))
        return bitmaps.loadForPrint(t.path, pixels)
    }

    override suspend fun pdf(project: PolarProject, template: Bitmap?, optimizePhotos: Boolean): File = withContext(Dispatchers.IO) {
        val sizes=requiredPhotoPixels(project)
        File(dir(), fileName(project, "pdf")).also {
            PolarExporter.exportPdf(project, it, { a -> bitmaps.loadForPrint(a.path, sizes[a.id] ?: BitmapLoader.EXPORT_MAX) }, printTemplate(project, template), fonts, optimizePhotos)
        }
    }

    override suspend fun png(project: PolarProject, page: Int, template: Bitmap?): File = withContext(Dispatchers.IO) {
        val sizes=requiredPhotoPixels(project,page)
        File(dir(), fileName(project, "png").replace(".png", " · hoja ${page + 1}.png")).also {
            PolarExporter.exportPng(project, page, it, 300, { a -> bitmaps.loadForPrint(a.path, sizes[a.id] ?: BitmapLoader.EXPORT_MAX) }, printTemplate(project, template), fonts)
        }
    }

    override suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?): File = withContext(Dispatchers.IO) {
        val sizes = requiredPhotoPixels(project, page)
        File(dir(), fileName(project, "jpg").replace(".jpg", " · hoja ${page + 1}.jpg")).also {
            PolarExporter.exportJpeg(project, page, it, 300, { a -> bitmaps.loadForPrint(a.path, sizes[a.id] ?: BitmapLoader.EXPORT_MAX) }, printTemplate(project, template), fonts)
        }
    }
}

/** Resolución útil a 300 ppp, con el mismo ajuste/rotación que el motor. */
internal fun requiredPhotoPixels(project: PolarProject, page: Int? = null): Map<String,Int> {
    val targets=mutableMapOf<String,Int>()
    fun include(asset: PhotoAsset, placement: PhotoPlacement, rect: PolarRect) {
        val fit=PhotoFit.compute(rect.width,rect.height,asset.pixelWidth,asset.pixelHeight,placement)
        val target=ceil(max(fit.width,fit.height)*300/72).toInt()
            .coerceIn(1,max(asset.pixelWidth,asset.pixelHeight))
        targets[asset.id]=max(targets[asset.id] ?: 0,target)
    }
    for (p in if (page == null) 0 until project.pageCount else page..page) {
        for ((local, rect) in PolarRenderer.photoRects(project, p).withIndex()) {
            val placement = project.placements.getOrNull(p * project.settings.capacity + local) ?: continue
            project.asset(placement)?.let { include(it, placement, rect) }
            placement.background?.imageID?.let { id ->
                project.photos.firstOrNull { it.id == id }?.let { include(it, PhotoPlacement(id), rect) }
            }
        }
    }
    return targets
}
