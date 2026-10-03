package com.polar.app.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.exifinterface.media.ExifInterface
import com.polar.app.engine.FontProvider
import com.polar.app.engine.PolarRenderer
import com.polar.app.engine.SystemFontProvider
import com.polar.app.model.PhotoAsset
import com.polar.app.model.PolarException
import com.polar.app.model.PolarProject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object PolarExporter {

    fun renderPageToBitmap(
        project: PolarProject,
        page: Int,
        dpi: Int = 300,
        bitmapProvider: (PhotoAsset) -> Bitmap? = { null },
        templateBitmap: Bitmap? = null,
        fonts: FontProvider = SystemFontProvider
    ): Bitmap {
        project.validated()
        if (page !in 0 until project.pageCount) {
            throw PolarException("La página $page no existe.")
        }

        val paper = PolarRenderer.paperRect(project.settings)
        val scale = dpi.toFloat() / 72.0f
        val widthPx = (paper.width * scale).roundToInt().coerceAtLeast(1)
        val heightPx = (paper.height * scale).roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        PolarRenderer.drawPage(
            canvas = canvas,
            project = project,
            page = page,
            isPreview = false,
            scale = scale,
            bitmapProvider = bitmapProvider,
            templateBitmap = templateBitmap,
            fonts = fonts
        )
        return bitmap
    }

    fun exportPng(
        project: PolarProject,
        page: Int,
        outputFile: File,
        dpi: Int = 300,
        bitmapProvider: (PhotoAsset) -> Bitmap? = { null },
        templateBitmap: Bitmap? = null,
        fonts: FontProvider = SystemFontProvider
    ) = exportImage(project, page, outputFile, dpi, bitmapProvider, templateBitmap, fonts, Bitmap.CompressFormat.PNG, 100)

    fun exportJpeg(
        project: PolarProject,
        page: Int,
        outputFile: File,
        dpi: Int = 300,
        bitmapProvider: (PhotoAsset) -> Bitmap? = { null },
        templateBitmap: Bitmap? = null,
        fonts: FontProvider = SystemFontProvider
    ) = exportImage(project, page, outputFile, dpi, bitmapProvider, templateBitmap, fonts, Bitmap.CompressFormat.JPEG, 94)

    private fun exportImage(
        project: PolarProject, page: Int, outputFile: File, dpi: Int,
        bitmapProvider: (PhotoAsset) -> Bitmap?, templateBitmap: Bitmap?, fonts: FontProvider,
        format: Bitmap.CompressFormat, quality: Int
    ) {
        val bitmap = renderPageToBitmap(project, page, dpi, bitmapProvider, templateBitmap, fonts)
        val tempFile = File(outputFile.parentFile, ".${outputFile.name}.${System.currentTimeMillis()}.tmp")
        try {
            FileOutputStream(tempFile).use { out ->
                val success = bitmap.compress(format, quality, out)
                if (!success) throw PolarException("No se pudo comprimir la imagen.")
            }
            if (format == Bitmap.CompressFormat.JPEG) ExifInterface(tempFile).apply {
                setAttribute(ExifInterface.TAG_X_RESOLUTION, "$dpi/1")
                setAttribute(ExifInterface.TAG_Y_RESOLUTION, "$dpi/1")
                setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2")
                saveAttributes()
            }
            if (outputFile.exists()) outputFile.delete()
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
        } finally {
            bitmap.recycle()
            if (tempFile.exists()) tempFile.delete()
        }
    }

    fun exportPdf(
        project: PolarProject,
        outputFile: File,
        bitmapProvider: (PhotoAsset) -> Bitmap? = { null },
        templateBitmap: Bitmap? = null,
        fonts: FontProvider = SystemFontProvider,
        optimizePhotos: Boolean = true
    ) {
        project.validated()
        val paper = PolarRenderer.paperRect(project.settings)
        val widthPt = paper.width.roundToInt().coerceAtLeast(1)
        val heightPt = paper.height.roundToInt().coerceAtLeast(1)

        val pdfDocument = PdfDocument()
        val tempFile = File(outputFile.parentFile, ".${outputFile.name}.${System.currentTimeMillis()}.tmp")
        val compactFile = File(tempFile.parentFile, "${tempFile.name}.compact")
        val photos = mutableSetOf<PdfPhotoFingerprint>()

        try {
            for (p in 0 until project.pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, p + 1).create()
                val page = pdfDocument.startPage(pageInfo)

                PolarRenderer.drawPage(
                    canvas = page.canvas,
                    project = project,
                    page = p,
                    isPreview = false,
                    scale = 1.0f,
                    bitmapProvider = bitmapProvider,
                    templateBitmap = templateBitmap,
                    fonts = fonts,
                    pdfPhoto = { bitmap ->
                        PdfPhotoFingerprint.fromBitmap(bitmap)?.let { if (optimizePhotos) photos.add(it) }
                    }
                )

                pdfDocument.finishPage(page)
            }

            FileOutputStream(tempFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            if (optimizePhotos && PdfPhotoOptimizer.optimize(tempFile, compactFile, photos)) {
                compactFile.copyTo(tempFile, overwrite = true)
            }

            if (outputFile.exists()) outputFile.delete()
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
        } finally {
            pdfDocument.close()
            if (tempFile.exists()) tempFile.delete()
            if (compactFile.exists()) compactFile.delete()
        }
    }
}
