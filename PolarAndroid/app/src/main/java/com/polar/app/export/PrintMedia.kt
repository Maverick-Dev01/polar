package com.polar.app.export

import android.print.PrintAttributes
import com.polar.app.model.PaperOrientation
import com.polar.app.model.PaperSize
import com.polar.app.model.PrintSettings

/** Papel de Polar → papel de Android, para que la impresora proponga el tamaño correcto. */
object PrintMedia {
    fun mediaFor(settings: PrintSettings): PrintAttributes.MediaSize? {
        val base = when (settings.paperSize) {
            PaperSize.LETTER -> PrintAttributes.MediaSize.NA_LETTER
            PaperSize.LEGAL -> PrintAttributes.MediaSize.NA_LEGAL
            PaperSize.A4 -> PrintAttributes.MediaSize.ISO_A4
            PaperSize.A3 -> PrintAttributes.MediaSize.ISO_A3
            PaperSize.PHOTO4X6 -> PrintAttributes.MediaSize.NA_INDEX_4X6
            PaperSize.OFICIO, PaperSize.PHOTO5X7, PaperSize.CUSTOM -> return null
        }
        return if (settings.orientation == PaperOrientation.LANDSCAPE) base.asLandscape() else base.asPortrait()
    }

    fun attributes(settings: PrintSettings): PrintAttributes = PrintAttributes.Builder().apply {
        mediaFor(settings)?.let { setMediaSize(it) }
        setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        setMinMargins(PrintAttributes.Margins.NO_MARGINS)
    }.build()
}
