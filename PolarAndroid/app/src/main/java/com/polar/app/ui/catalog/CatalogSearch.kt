package com.polar.app.ui.catalog

import com.polar.app.model.TemplateStyle
import com.polar.app.model.description
import java.text.Normalizer

/** Búsqueda del catálogo: sin acentos ni mayúsculas, en nombre y descripción; cada palabra debe aparecer. */
object CatalogSearch {
    private val marks = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(marks, "").lowercase().trim()

    fun filter(styles: List<TemplateStyle>, query: String): List<TemplateStyle> {
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return styles
        return styles.filter { s ->
            val haystack = normalize(s.displayName + " " + s.description)
            words.all { it in haystack }
        }
    }
}
