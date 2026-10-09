package com.polar.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToInt

/** Ids compatibles con `UUID` de Swift. */
fun newId(): String = UUID.randomUUID().toString().uppercase()

/** Fechas como las guarda la Mac: segundos desde 2001-01-01 00:00 UTC. */
object SwiftDate {
    private const val REFERENCE_EPOCH_MS = 978_307_200_000L
    fun fromEpochMs(ms: Long): Double = (ms - REFERENCE_EPOCH_MS) / 1000.0
    fun toEpochMs(value: Double): Long = Math.round(value * 1000.0) + REFERENCE_EPOCH_MS
    fun now(): Double = fromEpochMs(System.currentTimeMillis())
}

@Serializable
enum class PaperSize(val displayName: String) {
    @SerialName("letter") LETTER("Carta"),
    @SerialName("oficio") OFICIO("Oficio"),
    @SerialName("legal") LEGAL("Legal"),
    @SerialName("a4") A4("A4"),
    @SerialName("a3") A3("A3"),
    @SerialName("photo4x6") PHOTO4X6("4 × 6"),
    @SerialName("photo5x7") PHOTO5X7("5 × 7"),
    @SerialName("custom") CUSTOM("Personalizado");
}

@Serializable
enum class PaperOrientation(val displayName: String) {
    @SerialName("portrait") PORTRAIT("Vertical"),
    @SerialName("landscape") LANDSCAPE("Horizontal");
}

@Serializable
enum class CardFormat(val displayName: String) {
    @SerialName("original") ORIGINAL("Del diseño"),
    @SerialName("portrait") PORTRAIT("Vertical"),
    @SerialName("landscape") LANDSCAPE("Horizontal"),
    @SerialName("square") SQUARE("Cuadrado"),
    @SerialName("fill") FILL("Llenar espacio");
}

@Serializable
enum class CutStyle(val displayName: String) {
    @SerialName("corners") CORNERS("Marcas en esquinas"),
    @SerialName("lines") LINES("Líneas completas");
}

@Serializable
enum class TextRole(val key: String, val displayName: String) {
    @SerialName("title") TITLE("title", "Título"),
    @SerialName("subtitle") SUBTITLE("subtitle", "Subtítulo"),
    @SerialName("caption") CAPTION("caption", "Pie de foto"),
    @SerialName("song") SONG("song", "Canción"),
    @SerialName("artist") ARTIST("artist", "Artista"),
    @SerialName("date") DATE("date", "Fecha");

    /** La Mac sólo acepta estos roles dentro de `settings.textStyles`. */
    val isMacRole: Boolean get() = this != DATE

    companion object {
        fun fromKey(key: String): TextRole? = entries.firstOrNull { it.key == key }
    }
}

@Serializable
enum class TextAlignment(val displayName: String) {
    @SerialName("automatic") AUTOMATIC("Del diseño"),
    @SerialName("left") LEFT("Izquierda"),
    @SerialName("center") CENTER("Centro"),
    @SerialName("right") RIGHT("Derecha");
}

@Serializable
enum class DateSource(val displayName: String) {
    @SerialName("none") NONE("Sin fecha"),
    @SerialName("photo") PHOTO("De la foto"),
    @SerialName("chosen") CHOSEN("Elegir fecha");
}

@Serializable
enum class DateStyle {
    @SerialName("dayMonthYear") DAY_MONTH_YEAR,
    @SerialName("numeric") NUMERIC,
    @SerialName("monthYear") MONTH_YEAR;
}

@Serializable
data class TextAppearance(
    val fontName: String = ".System",
    val size: Double = 0.0,
    val hex: String = "",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val alignment: TextAlignment = TextAlignment.AUTOMATIC,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val visible: Boolean = true
)

@Serializable
enum class TemplateStyle(
    val displayName: String,
    val defaultGrid: Pair<Int, Int>,
    val photosPerCard: Int,
    val defaultAspect: Double
) {
    @SerialName("polaroid") POLAROID("Polaroid", 3 to 3, 1, 0.70),
    @SerialName("mini") MINI("Instantánea mini", 3 to 3, 1, 0.667),
    @SerialName("spotify") SPOTIFY("Foto + canción", 3 to 3, 1, 0.75),
    @SerialName("playerRed") PLAYER_RED("Reproductor rojo", 3 to 3, 1, 0.66),
    @SerialName("playerGray") PLAYER_GRAY("Reproductor horizontal", 2 to 5, 1, 1.9),
    @SerialName("ticket") TICKET("Boleto vintage", 1 to 4, 1, 2.27),
    @SerialName("filmVertical") FILM_VERTICAL("Película vertical", 2 to 1, 5, 0.292),
    @SerialName("filmHorizontal") FILM_HORIZONTAL("Película horizontal", 1 to 6, 5, 5.3),
    @SerialName("calendar") CALENDAR("Calendario", 3 to 4, 1, 0.683),
    @SerialName("instagram") INSTAGRAM("Instagram", 3 to 4, 1, 0.705),
    @SerialName("custom") CUSTOM("Mi diseño", 2 to 3, 1, 0.80),
    @SerialName("borderless") BORDERLESS("Sin bordes", 2 to 3, 1, 0.75),
    @SerialName("square") SQUARE("Cuadrada", 3 to 3, 1, 1.0),
    @SerialName("postcard") POSTCARD("Postal", 2 to 3, 1, 1.5),
    @SerialName("botanical") BOTANICAL("Botánico", 2 to 3, 1, 0.75),
    @SerialName("celebration") CELEBRATION("Celebración", 3 to 3, 1, 0.70),
    @SerialName("pets") PETS("Mi mascota", 2 to 3, 1, 0.80),
    @SerialName("heart") HEART("Corazón", 2 to 3, 1, 0.75),
    @SerialName("editorial") EDITORIAL("Editorial", 2 to 3, 1, 0.75),
    // Diseños de la fase 3: su geometría vive en shared-fixtures/estilos-geometria.json (la comparte la Mac).
    @SerialName("photobooth") PHOTOBOOTH("Fotomatón", 3 to 1, 4, 0.30),
    @SerialName("instaxWide") INSTAX_WIDE("Instantánea ancha", 2 to 3, 1, 1.256),
    @SerialName("vinyl") VINYL("Vinilo", 3 to 3, 1, 0.75),
    @SerialName("cassette") CASSETTE("Casete", 2 to 4, 1, 1.6),
    @SerialName("collage") COLLAGE("Collage", 2 to 3, 3, 0.75),
    @SerialName("washi") WASHI("Cinta washi", 2 to 3, 1, 0.80),
    @SerialName("imported") IMPORTED("Plantilla importada", 1 to 1, 1, 0.75);
}

/** Forma del hueco de una foto. Un valor desconocido de una versión futura cae a [RECT]. */
@Serializable
enum class RegionShape(val displayName: String) {
    @SerialName("rect") RECT("Rectángulo"),
    @SerialName("round") ROUND("Redondeado"),
    @SerialName("ellipse") ELLIPSE("Óvalo");
}

@Serializable
data class TemplateRegion(
    val id: String = newId(),
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val isTransparent: Boolean = false,
    /** Opcionales: un `.polar` anterior no los trae y se lee como rectángulo. */
    val shape: RegionShape = RegionShape.RECT,
    /** Fracción del lado menor del hueco (sólo [RegionShape.ROUND]). */
    val radius: Double = 0.0
) {
    fun clamped(): TemplateRegion {
        val w = width.coerceIn(0.02, 1.0)
        val h = height.coerceIn(0.02, 1.0)
        return copy(radius = if (radius.isFinite()) radius.coerceIn(0.0, 0.5) else 0.0, width = w, height = h, x = x.coerceIn(0.0, 1.0 - w), y = y.coerceIn(0.0, 1.0 - h))
    }
}

@Serializable
data class ImportedTemplate(
    val path: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val regions: List<TemplateRegion> = emptyList()
)

@Serializable
data class PhotoAsset(
    val id: String = newId(),
    val path: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val takenAtEpochMs: Long? = null,
    val maskPath: String? = null,
    val isBackground: Boolean = false
) {
    val name: String get() = path.substringAfterLast('/').substringBeforeLast('.')
}

/** Filtro no destructivo; cada ámbito guarda un objeto completo. */
@Serializable
data class PhotoLook(
    @Serializable(with = LookPresetSerializer::class) val preset: String = "original",
    val intensity: Double = 1.0,
    val light: Double = 0.0,
    val contrast: Double = 0.0,
    val warmth: Double = 0.0,
    val grain: Double = 0.0
) {
    val isNeutral: Boolean get() = (preset == "original" || intensity == 0.0) && light == 0.0 && contrast == 0.0 && warmth == 0.0 && grain == 0.0
    fun canonical(): PhotoLook = if (preset in PRESETS) this else copy(preset = "original")
    fun validated() {
        if (!intensity.isFinite() || intensity !in 0.0..1.0 || !grain.isFinite() || grain !in 0.0..1.0 ||
            listOf(light, contrast, warmth).any { !it.isFinite() || it !in -1.0..1.0 })
            throw PolarException("Los ajustes de una foto no son válidos.")
    }
    companion object { val PRESETS = listOf("original", "bw", "film", "sepia", "warm", "cool", "faded", "vivid") }
}

@Serializable
data class PhotoBackground(
    val colorHex: String? = null,
    val imageID: String? = null,
    val feather: Double = 0.2,
    val shadow: Double = 0.15
)

/** Sólo cambia el aspecto; conserva cuadrícula, fotos y posiciones. */
@Serializable
data class PageDesign(val style: TemplateStyle = TemplateStyle.POLAROID, val format: CardFormat = CardFormat.ORIGINAL)

@Serializable
data class PhotoPlacement(
    val assetID: String,
    val zoom: Double = 1.0,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val quarterTurns: Int = 0,
    val photoLook: PhotoLook? = null,
    val background: PhotoBackground? = null
)

data class SizeF(val width: Double, val height: Double)

@Serializable
data class PrintSettings(
    val style: TemplateStyle = TemplateStyle.POLAROID,
    val columns: Int = 3,
    val rows: Int = 3,
    val margin: Double = 24.0,
    val gap: Double = 12.0,
    val title: String = "Nuestros momentos",
    val subtitle: String = "Tú y yo",
    val caption: String = "Una historia para guardar",
    val song: String = "Nuestra canción",
    val artist: String = "Artista",
    val songURL: String = "",
    val accentHex: String = "92394A",
    val cutGuides: Boolean = true,
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val specialDate: Double = SwiftDate.now(),
    val highlightDate: Boolean = true,
    val roundedPhotos: Boolean = false,
    val paperSize: PaperSize = PaperSize.LETTER,
    val orientation: PaperOrientation = PaperOrientation.PORTRAIT,
    val customWidthMM: Double = 215.9,
    val customHeightMM: Double = 279.4,
    val cardFormat: CardFormat = CardFormat.ORIGINAL,
    val importedTemplate: ImportedTemplate? = null,
    val textStyles: Map<String, TextAppearance> = emptyMap(),
    val drawBorders: Boolean = false,
    val cutStyle: CutStyle = CutStyle.CORNERS,
    // Campos sólo de Android: la Mac los ignora.
    val extraTextStyles: Map<String, TextAppearance> = emptyMap(),
    val dateSource: DateSource = DateSource.NONE,
    val chosenDate: Double? = null,
    val dateStyle: DateStyle = DateStyle.DAY_MONTH_YEAR,
    val photoLook: PhotoLook? = null
) {
    val capacity: Int
        get() {
            if (style == TemplateStyle.IMPORTED) return (importedTemplate?.regions?.size ?: 1).coerceAtLeast(1)
            if (columns !in 1..4 || rows !in 1..6) return 1
            return columns * rows * style.photosPerCard
        }

    val paperSizePoints: SizeF
        get() {
            val base = when (paperSize) {
                PaperSize.LETTER -> SizeF(612.0, 792.0)
                PaperSize.OFICIO -> SizeF(216.0 * 72.0 / 25.4, 340.0 * 72.0 / 25.4)
                PaperSize.LEGAL -> SizeF(612.0, 1008.0)
                PaperSize.A4 -> SizeF(210.0 * 72.0 / 25.4, 297.0 * 72.0 / 25.4)
                PaperSize.A3 -> SizeF(297.0 * 72.0 / 25.4, 420.0 * 72.0 / 25.4)
                PaperSize.PHOTO4X6 -> SizeF(288.0, 432.0)
                PaperSize.PHOTO5X7 -> SizeF(360.0, 504.0)
                PaperSize.CUSTOM -> SizeF(customWidthMM * 72.0 / 25.4, customHeightMM * 72.0 / 25.4)
            }
            return if (orientation == PaperOrientation.PORTRAIT) base else SizeF(base.height, base.width)
        }

    val paperDescription: String
        get() {
            val size = paperSizePoints
            val wMM = (size.width * 25.4 / 72.0).roundToInt()
            val hMM = (size.height * 25.4 / 72.0).roundToInt()
            return "${paperSize.displayName} · $wMM × $hMM mm · ${orientation.displayName}"
        }

    val cardAspect: Double?
        get() = when (cardFormat) {
            CardFormat.ORIGINAL -> style.defaultAspect
            CardFormat.PORTRAIT -> 0.70
            CardFormat.LANDSCAPE -> 1.40
            CardFormat.SQUARE -> 1.0
            CardFormat.FILL -> null
        }

    fun text(role: TextRole): String = when (role) {
        TextRole.TITLE -> title
        TextRole.SUBTITLE -> subtitle
        TextRole.CAPTION -> caption
        TextRole.SONG -> song
        TextRole.ARTIST -> artist
        TextRole.DATE -> ""
    }

    fun withText(role: TextRole, value: String): PrintSettings = when (role) {
        TextRole.TITLE -> copy(title = value)
        TextRole.SUBTITLE -> copy(subtitle = value)
        TextRole.CAPTION -> copy(caption = value)
        TextRole.SONG -> copy(song = value)
        TextRole.ARTIST -> copy(artist = value)
        TextRole.DATE -> this
    }

    fun textStyle(role: TextRole): TextAppearance =
        (if (role.isMacRole) textStyles[role.key] else extraTextStyles[role.key]) ?: TextAppearance()

    fun withTextStyle(role: TextRole, appearance: TextAppearance): PrintSettings =
        if (role.isMacRole) copy(textStyles = textStyles + (role.key to appearance))
        else copy(extraTextStyles = extraTextStyles + (role.key to appearance))

    fun withoutTextStyle(role: TextRole): PrintSettings =
        if (role.isMacRole) copy(textStyles = textStyles - role.key)
        else copy(extraTextStyles = extraTextStyles - role.key)
}

/** Lo que una tarjeta cambia respecto al texto general. Claves = `TextRole.key`. */
@Serializable
data class CardOverride(
    val texts: Map<String, String> = emptyMap(),
    val styles: Map<String, TextAppearance> = emptyMap(),
    val dateSource: DateSource? = null,
    val chosenDate: Double? = null,
    val photoLook: PhotoLook? = null,
    val designStyle: TemplateStyle? = null,
    val designFormat: CardFormat? = null
) {
    val isEmpty: Boolean get() = texts.isEmpty() && styles.isEmpty() && dateSource == null && chosenDate == null && photoLook == null && designStyle == null && designFormat == null
}

class PolarException(message: String) : Exception(message)

@Serializable
data class PolarProject(
    val version: Int = 1,
    val settings: PrintSettings = PrintSettings(),
    val photos: List<PhotoAsset> = emptyList(),
    val placements: List<PhotoPlacement?> = emptyList(),
    // Campos sólo de Android: la Mac los ignora.
    val name: String = "",
    val updatedAtEpochMs: Long = 0,
    val cardOverrides: Map<String, CardOverride> = emptyMap(),
    val pageDesigns: Map<String, PageDesign> = emptyMap()
) {
    val pageCount: Int
        get() {
            val cap = settings.capacity.coerceAtLeast(1)
            return ((placements.size + cap - 1) / cap).coerceAtLeast(1)
        }

    val placedCount: Int get() = placements.count { it != null }

    /** Tarjetas por hoja (en película, cada tarjeta reúne 5 fotos). */
    val cardsPerPage: Int
        get() = if (settings.style == TemplateStyle.IMPORTED) settings.capacity
        else (settings.capacity / settings.style.photosPerCard).coerceAtLeast(1)

    fun cardOfSlot(slot: Int): Int = slot / settings.style.photosPerCard
    fun firstSlotOfCard(card: Int): Int = card * settings.style.photosPerCard
    fun override(card: Int): CardOverride? = cardOverrides[card.toString()]

    fun compatibleStyle(style: TemplateStyle): Boolean = style != TemplateStyle.IMPORTED && settings.style != TemplateStyle.IMPORTED && style.photosPerCard == settings.style.photosPerCard
    fun settingsForPage(page: Int): PrintSettings = pageDesigns[page.toString()]?.let {
        settings.copy(style = it.style, cardFormat = it.format)
    } ?: settings
    fun settingsForCard(card: Int): PrintSettings {
        val s = settingsForPage(card / cardsPerPage)
        val o = override(card)
        return s.copy(style = o?.designStyle ?: s.style, cardFormat = o?.designFormat ?: s.cardFormat)
    }

    fun asset(forPlacement: PhotoPlacement?): PhotoAsset? {
        if (forPlacement == null) return null
        return photos.firstOrNull { it.id == forPlacement.assetID }
    }

    fun normalized(): PolarProject {
        val total = pageCount * settings.capacity.coerceAtLeast(1)
        return if (placements.size >= total) this else copy(placements = placements + List(total - placements.size) { null })
    }

    /** Cambia de diseño como la Mac: cuadrícula por defecto y sin huecos vacíos al final. */
    fun withStyle(style: TemplateStyle): PolarProject {
        val (cols, rows) = style.defaultGrid
        val trimmed = placements.dropLastWhile { it == null }
        return copy(settings = settings.copy(style = style, columns = cols, rows = rows), placements = trimmed).normalized()
    }

    fun validated() {
        for ((page, design) in pageDesigns) {
            if (page.toIntOrNull()?.let { it in 0 until pageCount && page == it.toString() } != true || !compatibleStyle(design.style))
                throw PolarException("El diseño de una hoja no es compatible con sus posiciones.")
        }
        if (cardOverrides.values.any { it.designStyle?.let { style -> !compatibleStyle(style) } == true })
            throw PolarException("Este diseño requiere otra cantidad de fotos por tarjeta.")
        settings.photoLook?.validated()
        placements.filterNotNull().forEach { it.photoLook?.validated() }
        cardOverrides.values.forEach { it.photoLook?.validated() }
        if (version != 1) throw PolarException("La versión del archivo no es compatible.")
        if (settings.columns !in 1..4 || settings.rows !in 1..6) throw PolarException("El tamaño o la distribución no son válidos.")
        if (!settings.margin.isFinite() || settings.margin !in 0.0..60.0) throw PolarException("El margen no es válido.")
        if (!settings.gap.isFinite() || settings.gap !in 0.0..30.0) throw PolarException("La separación no es válida.")
        if (!settings.customWidthMM.isFinite() || settings.customWidthMM !in 80.0..600.0) throw PolarException("El ancho personalizado no es válido.")
        if (!settings.customHeightMM.isFinite() || settings.customHeightMM !in 80.0..600.0) throw PolarException("El alto personalizado no es válido.")
        if (settings.calendarYear !in 1900..2100) throw PolarException("El año del calendario no es válido.")
        if (placements.size > 2000 + settings.capacity - 1 || placedCount > 2000 || photos.size > 2000) {
            throw PolarException("Este diseño tiene más de 2000 fotos.")
        }
        val paper = settings.paperSizePoints
        if (paper.width <= 2 * settings.margin + (settings.columns - 1) * settings.gap ||
            paper.height <= 2 * settings.margin + (settings.rows - 1) * settings.gap
        ) throw PolarException("El papel es demasiado pequeño para estos márgenes y espacios.")
        val hex = Regex("^[0-9A-Fa-f]{6}$")
        if (!hex.matches(settings.accentHex)) throw PolarException("El color no es válido.")

        fun checkAppearance(a: TextAppearance) {
            if (a.fontName.length > 128) throw PolarException("El nombre de una fuente es demasiado largo.")
            if (a.hex.isNotEmpty() && !hex.matches(a.hex)) throw PolarException("El color de un texto no es válido.")
            if (!a.size.isFinite() || (a.size != 0.0 && a.size !in 6.0..96.0)) throw PolarException("El tamaño de un texto no es válido.")
            if (!a.offsetX.isFinite() || a.offsetX !in -60.0..60.0 || !a.offsetY.isFinite() || a.offsetY !in -60.0..60.0) {
                throw PolarException("La posición de un texto no es válida.")
            }
        }
        for ((role, a) in settings.textStyles) {
            if (TextRole.fromKey(role)?.isMacRole != true) throw PolarException("Hay un texto desconocido: $role")
            checkAppearance(a)
        }
        for ((role, a) in settings.extraTextStyles) {
            if (TextRole.fromKey(role) == null) throw PolarException("Hay un texto desconocido: $role")
            checkAppearance(a)
        }
        for ((card, o) in cardOverrides) {
            if (card.toIntOrNull()?.let { it >= 0 && card == it.toString() } != true) throw PolarException("Una tarjeta no es válida.")
            for ((role, value) in o.texts) {
                if (TextRole.fromKey(role) == null) throw PolarException("Hay un texto desconocido: $role")
                if (value.length > 500) throw PolarException("Un texto es demasiado largo.")
            }
            for ((role, a) in o.styles) {
                if (TextRole.fromKey(role) == null) throw PolarException("Hay un texto desconocido: $role")
                checkAppearance(a)
            }
        }
        if (settings.style == TemplateStyle.IMPORTED && settings.importedTemplate == null) {
            throw PolarException("Agrega una plantilla antes de usar este diseño.")
        }
        settings.importedTemplate?.let { t ->
            if (t.path.isEmpty() || t.pixelWidth !in 1..30000 || t.pixelHeight !in 1..30000 ||
                t.pixelWidth.toDouble() * t.pixelHeight.toDouble() > 150_000_000
            ) throw PolarException("La plantilla importada no es válida.")
            if (t.regions.size !in 1..64 || t.regions.map { it.id }.toSet().size != t.regions.size) {
                throw PolarException("La plantilla importada no es válida.")
            }
            for (r in t.regions) {
                if (!r.x.isFinite() || !r.y.isFinite() || !r.width.isFinite() || !r.height.isFinite() ||
                    r.x < 0.0 || r.y < 0.0 || r.width <= 0.0 || r.height <= 0.0 ||
                    r.x + r.width > 1.000001 || r.y + r.height > 1.000001
                ) throw PolarException("Un espacio de la plantilla queda fuera de la imagen.")
            }
        }
        val ids = photos.map { it.id }.toSet()
        if (ids.size != photos.size || photos.any { it.path.isEmpty() || it.pixelWidth <= 0 || it.pixelHeight <= 0 }) {
            throw PolarException("La lista de fotos no es válida.")
        }
        for (slot in placements.filterNotNull()) {
            slot.background?.let { b ->
                if ((b.colorHex != null && !hex.matches(b.colorHex)) || (b.imageID != null && b.imageID !in ids) ||
                    !b.feather.isFinite() || b.feather !in 0.0..1.0 || !b.shadow.isFinite() || b.shadow !in 0.0..1.0 ||
                    photos.firstOrNull { it.id == slot.assetID }?.maskPath.isNullOrBlank())
                    throw PolarException("El fondo de una foto no es válido.")
            }
            if (slot.assetID !in ids) throw PolarException("Falta una foto que usa el diseño.")
            if (!slot.zoom.isFinite() || (slot.zoom <= 0.0 || slot.zoom > 4.0) ||
                !slot.offsetX.isFinite() || slot.offsetX !in -1.0..1.0 ||
                !slot.offsetY.isFinite() || slot.offsetY !in -1.0..1.0 || slot.quarterTurns !in 0..3
            ) throw PolarException("El encuadre de una foto no es válido.")
        }
    }

    fun effectiveDPI(slot: Int, rectWidthPoints: Double, rectHeightPoints: Double): Double? {
        val p = placements.getOrNull(slot) ?: return null
        val photo = asset(p) ?: return null
        val w = if (p.quarterTurns % 2 == 0) photo.pixelWidth else photo.pixelHeight
        val h = if (p.quarterTurns % 2 == 0) photo.pixelHeight else photo.pixelWidth
        return minOf(w / rectWidthPoints, h / rectHeightPoints) * 72.0 / p.zoom
    }
}
