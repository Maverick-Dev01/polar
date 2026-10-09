package com.polar.app.help

/** Los destinos de `help.json` que Android sabe abrir. Debe coincidir con la lista «destinos» del JSON (lo verifica una prueba). */
enum class HelpDestination(val id: String, val needsEditor: Boolean) {
    INICIO("inicio", false),
    CATALOGO("catalogo", false),
    EDITOR_FOTOS("editor.fotos", true),
    EDITOR_FILTROS("editor.filtros", true),
    EDITOR_DISENO("editor.diseno", true),
    EDITOR_TEXTO("editor.texto", true),
    EDITOR_PAPEL("editor.papel", true),
    EDITOR_ENCUADRAR("editor.encuadrar", true),
    TERMINAR("terminar", true),
    AJUSTES("ajustes", false),
    MOLDES_ASISTENTE("moldes.asistente", false),
    MIS_MOLDES("mis-moldes", false);

    companion object {
        /** Destino interno (no está en help.json): repetir el recorrido desde Ayuda. */
        const val TOUR_ID = "recorrido"
        fun fromId(id: String): HelpDestination? = entries.firstOrNull { it.id == id }
    }
}

/** Qué hace el editor al llegar desde Ayuda. */
enum class EditorLanding {
    TOOL_PHOTOS, TOOL_FILTERS, TOOL_DESIGN, TOOL_TEXT, TOOL_PAPER, CROP, FINISH, IMPORT, MY_TEMPLATES, TOUR,
    /** Salir del editor (guardando) hacia Inicio. */
    HOME,
    NONE;

    companion object {
        fun of(destino: String): EditorLanding = when (destino) {
            "editor.fotos" -> TOOL_PHOTOS
            "editor.filtros" -> TOOL_FILTERS
            "editor.diseno" -> TOOL_DESIGN
            "editor.texto" -> TOOL_TEXT
            "editor.papel" -> TOOL_PAPER
            "editor.encuadrar" -> CROP
            "terminar" -> FINISH
            "moldes.asistente" -> IMPORT
            "mis-moldes" -> MY_TEMPLATES
            HelpDestination.TOUR_ID -> TOUR
            "inicio" -> HOME
            else -> NONE
        }
    }
}

/** Adónde navega «Llévame ahí» (resuelto sin tocar Compose, para probarlo). */
sealed interface HelpTarget {
    data object Home : HelpTarget
    data object Settings : HelpTarget
    /** Catálogo; `landing` = «moldes.asistente» o «mis-moldes» abre eso directo. */
    data class Catalog(val landing: String? = null) : HelpTarget
    /** Volver al editor abierto (o al más reciente) y aplicar `landing`. */
    data class Editor(val landing: String) : HelpTarget
}

object HelpNav {
    fun resolve(destino: String, projectId: String?): HelpTarget {
        if (destino == HelpDestination.TOUR_ID) return HelpTarget.Editor(destino)
        return when (HelpDestination.fromId(destino)) {
            HelpDestination.INICIO -> HelpTarget.Home
            HelpDestination.AJUSTES -> HelpTarget.Settings
            HelpDestination.CATALOGO, null -> HelpTarget.Catalog()
            HelpDestination.MOLDES_ASISTENTE, HelpDestination.MIS_MOLDES ->
                if (projectId != null) HelpTarget.Editor(destino) else HelpTarget.Catalog(destino)
            else -> HelpTarget.Editor(destino)
        }
    }
}

/** Ids de los controles que registra la interfaz; cada uno tiene su explicación en `controles` de help.json. */
object HelpIds {
    const val TOP_BAR = "top.bar"
    const val UNDO = "top.deshacer"; const val REDO = "top.rehacer"; const val BACK = "top.back"
    const val NAME = "top.nombre"; const val MORE = "top.mas"; const val PRINT = "top.imprimir"
    const val SHEET = "hoja"; const val PAGES = "hoja.paginas"; const val CARD_ACTIONS = "ctx"; const val ADD_PHOTOS = "cta.fotos"
    fun tool(name: String) = "tool.$name"
    fun panel(name: String) = "panel.$name"
    val tools = listOf("fotos", "filtros", "diseno", "texto", "papel")
    /** El contenedor de la barra solo sirve para ubicar el aviso del modo «?»; no se explica. */
    val all: List<String> = listOf(UNDO, REDO, BACK, NAME, MORE, PRINT, SHEET, PAGES, CARD_ACTIONS, ADD_PHOTOS) +
        tools.map(::tool) + tools.map(::panel)
}

/** Lógica del recorrido: qué paso sigue según los controles que de verdad se ven. */
object TourFlow {
    fun first(steps: List<TourStep>, visible: Set<String>): Int? = steps.indexOfFirst { it.objetivo in visible }.takeIf { it >= 0 }
    fun next(steps: List<TourStep>, from: Int, visible: Set<String>): Int? =
        (from + 1 until steps.size).firstOrNull { steps[it].objetivo in visible }
    /** «Paso n de m» contando solo los pasos que se pueden mostrar. */
    fun position(steps: List<TourStep>, index: Int, visible: Set<String>): Pair<Int, Int> {
        val shown = steps.indices.filter { steps[it].objetivo in visible }
        return (shown.count { it <= index }.coerceAtLeast(1)) to shown.size.coerceAtLeast(1)
    }
    /** Se lanza solo la primera vez, cuando el editor ya cargó; `seen == null` es «aún leyendo Ajustes». */
    fun shouldAutoStart(seen: Boolean?, loading: Boolean, editing: Boolean): Boolean = seen == false && !loading && editing
}
