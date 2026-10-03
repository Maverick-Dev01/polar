package com.polar.app.export

import android.content.Context
import android.graphics.Bitmap
import com.polar.app.data.BitmapLoader
import com.polar.app.engine.FontProvider
import com.polar.app.engine.PolarRenderer
import com.polar.app.engine.PhotoFit
import kotlin.math.ceil
import kotlin.math.max
import com.polar.app.model.PolarProject
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

    override suspend fun pdf(project: PolarProject, template: Bitmap?, optimizePhotos: Boolean): File = withContext(Dispatchers.IO) {
        val sizes=requiredPhotoPixels(project)
        File(dir(), fileName(project, "pdf")).also {
            PolarExporter.exportPdf(project, it, { a -> bitmaps.loadForPrint(a.path, sizes[a.id] ?: BitmapLoader.EXPORT_MAX) }, template, fonts, optimizePhotos)
        }
    }

    override suspend fun png(project: PolarProject, page: Int, template: Bitmap?): File = withContext(Dispatchers.IO) {
        val sizes=requiredPhotoPixels(project,page)
        File(dir(), fileName(project, "png").replace(".png", " · hoja ${page + 1}.png")).also {
            PolarExporter.exportPng(project, page, it, 300, { a -> bitmaps.loadForPrint(a.path, sizes[a.id] ?: BitmapLoader.EXPORT_MAX) }, template, fonts)
        }
    }

    override suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?): File = withContext(Dispatchers.IO) {
        val sizes = requiredPhotoPixels(project, page)
        File(dir(), fileName(project, "jpg").replace(".jpg", " · hoja ${page + 1}.jpg")).also {
            PolarExporter.exportJpeg(project, page, it, 300, { a -> bitmaps.loadForPrint(a.path, sizes[a.id] ?: BitmapLoader.EXPORT_MAX) }, template, fonts)
        }
    }
}

/** Resolución útil a 300 ppp, con el mismo ajuste/rotación que el motor. */
internal fun requiredPhotoPixels(project: PolarProject, page: Int? = null): Map<String,Int> {
    val s=project.settings; val cards=PolarRenderer.calculateCardRects(s)
    val targets=mutableMapOf<String,Int>()
    for(slot in project.placements.indices) {
        if(page!=null && slot/s.capacity!=page) continue
        val placement=project.placements[slot] ?: continue
        val asset=project.asset(placement) ?: continue
        val local=slot%s.capacity
        val card=cards.getOrNull(local/s.style.photosPerCard) ?: continue
        val rect=PolarRenderer.calculatePhotoRects(card,s.style,s).getOrNull(local%s.style.photosPerCard) ?: continue
        val fit=PhotoFit.compute(rect.width,rect.height,asset.pixelWidth,asset.pixelHeight,placement)
        val target=ceil(max(fit.width,fit.height)*300/72).toInt().coerceIn(1,BitmapLoader.EXPORT_MAX)
        targets[asset.id]=max(targets[asset.id] ?: 0,target)
    }
    return targets
}
