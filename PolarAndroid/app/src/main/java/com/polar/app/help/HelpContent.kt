package com.polar.app.help

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class HelpCategory(val id: String, val titulo: String)

@Serializable
data class HelpArticle(
    val id: String, val categoria: String, val titulo: String, val resumen: String,
    val pasos: List<String>, val destino: String? = null, val animacion: String,
    /** Sinónimos para la búsqueda («recortar» encuentra «Encuadrar»). */
    val palabras: List<String> = emptyList()
)

@Serializable data class TourStep(val id: String, val objetivo: String, val animacion: String, val titulo: String, val texto: String)
@Serializable data class HelpControl(val id: String, val titulo: String, val texto: String)

/** Contenido único de la guía: `shared-fixtures/help.json`, copiado a los assets al compilar. */
@Serializable
data class HelpContent(
    val version: Int,
    val categorias: List<HelpCategory>,
    val destinos: List<String>,
    val animaciones: Map<String, String>,
    val recorrido: List<TourStep>,
    val controles: List<HelpControl>,
    val articulos: List<HelpArticle>
) {
    fun control(id: String): HelpControl? = controles.firstOrNull { it.id == id }
    fun article(id: String): HelpArticle? = articulos.firstOrNull { it.id == id }

    /** Problemas del esquema, en español; vacío = el contenido es válido. */
    fun problems(): List<String> = buildList {
        if (version != VERSION) add("versión $version no soportada")
        val cats = categorias.map { it.id }.toSet()
        articulos.groupBy { it.id }.filter { it.value.size > 1 }.keys.forEach { add("id repetido: $it") }
        controles.groupBy { it.id }.filter { it.value.size > 1 }.keys.forEach { add("id repetido (control): $it") }
        articulos.forEach { a ->
            if (a.categoria !in cats) add("${a.id}: categoría desconocida «${a.categoria}»")
            if (a.destino != null && a.destino !in destinos) add("${a.id}: destino desconocido «${a.destino}»")
            if (a.animacion !in animaciones) add("${a.id}: animación desconocida «${a.animacion}»")
            if (a.pasos.isEmpty() || a.pasos.any { it.isBlank() }) add("${a.id}: pasos vacíos")
            if (a.titulo.isBlank() || a.resumen.isBlank()) add("${a.id}: título o resumen vacío")
        }
        val controlIds = controles.map { it.id }.toSet()
        if (recorrido.size != 5) add("el recorrido debe tener 5 pasos, tiene ${recorrido.size}")
        recorrido.forEach { s ->
            if (s.animacion !in animaciones) add("recorrido ${s.id}: animación desconocida «${s.animacion}»")
            if (s.objetivo !in controlIds) add("recorrido ${s.id}: el objetivo «${s.objetivo}» no es un control")
        }
        controles.forEach { if (it.titulo.isBlank() || it.texto.isBlank()) add("control ${it.id}: texto vacío") }
    }

    companion object {
        const val VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): HelpContent = json.decodeFromString(serializer(), text)

        /** null si el archivo no se puede leer o tiene una versión desconocida (la ayuda se muestra vacía, la app no se cae). */
        fun parseOrNull(text: String): HelpContent? =
            runCatching { parse(text) }.getOrNull()?.takeIf { it.version == VERSION }

        fun load(context: Context): HelpContent? =
            runCatching { context.assets.open("help.json").bufferedReader().use { it.readText() } }.getOrNull()?.let(::parseOrNull)
    }
}
