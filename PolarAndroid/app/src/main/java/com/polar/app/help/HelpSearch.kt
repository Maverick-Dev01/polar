package com.polar.app.help

import java.text.Normalizer

object HelpSearch {
    private val marks = Regex("\\p{Mn}+")

    /** Minúsculas, sin acentos y con espacios simples. «Canción» y «cancion» son lo mismo. */
    fun normalize(text: String): String =
        marks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase().trim().replace(Regex("\\s+"), " ")

    /** Cada palabra de la búsqueda debe aparecer en algún lugar del artículo; los de título van primero. */
    fun filter(articles: List<HelpArticle>, query: String, categoria: String?): List<HelpArticle> {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        val inCategory = articles.filter { categoria == null || it.categoria == categoria }
        if (words.isEmpty()) return inCategory
        return inCategory.mapNotNull { a ->
            val title = normalize(a.titulo)
            val head = title + " " + normalize(a.palabras.joinToString(" ")) + " " + normalize(a.resumen)
            val all = head + " " + normalize(a.pasos.joinToString(" "))
            if (words.all { it in all }) (if (words.all { it in title }) 0 else if (words.all { it in head }) 1 else 2) to a else null
        }.sortedBy { it.first }.map { it.second }
    }
}
