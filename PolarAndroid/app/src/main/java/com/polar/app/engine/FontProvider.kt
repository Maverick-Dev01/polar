package com.polar.app.engine

import android.graphics.Typeface

/** Da la tipografía de un texto; el motor no sabe de recursos de Android. */
fun interface FontProvider {
    fun typeface(fontName: String, bold: Boolean, italic: Boolean): Typeface
}

object SystemFontProvider : FontProvider {
    override fun typeface(fontName: String, bold: Boolean, italic: Boolean): Typeface =
        Typeface.create(Typeface.DEFAULT, styleOf(bold, italic))
}

fun styleOf(bold: Boolean, italic: Boolean): Int = when {
    bold && italic -> Typeface.BOLD_ITALIC
    bold -> Typeface.BOLD
    italic -> Typeface.ITALIC
    else -> Typeface.NORMAL
}
