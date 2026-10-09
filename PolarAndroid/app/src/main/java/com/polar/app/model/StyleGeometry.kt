package com.polar.app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Geometría de los diseños de la fase 3, leída de `shared-fixtures/estilos-geometria.json`
 * (el build la copia como recurso). La Mac lee el mismo archivo, así que ambas apps dibujan igual.
 * Toda x, y, w, h es fracción del ancho (x, w) y del alto (y, h) de la tarjeta.
 */
@Serializable
data class PhotoSlotGeo(
    val x: Double, val y: Double, val w: Double, val h: Double,
    val shape: String = "rect", val radius: Double = 0.0
) {
    val regionShape: RegionShape get() = when (shape) { "round" -> RegionShape.ROUND; "ellipse" -> RegionShape.ELLIPSE; else -> RegionShape.RECT }
}

@Serializable
data class TextSlotGeo(
    val role: String, val x: Double, val y: Double, val w: Double, val h: Double,
    val align: String = "center", val defaultFont: String = ".System", val defaultSizePt: Double = 10.0,
    val color: String = "#000000", val bold: Boolean = false
) {
    val textRole: TextRole? get() = TextRole.entries.firstOrNull { it.name == role }
}

@Serializable
data class QrSlotGeo(val x: Double, val y: Double, val size: Double, val sizeBasis: String = "width")

@Serializable
data class DecorationGeo(
    val type: String, val x: Double, val y: Double, val w: Double, val h: Double,
    val color: String, val opacity: Double = 1.0, val rotationDeg: Double = 0.0, val layer: String = "below",
    val strokeW: Double = 0.0, val radius: Double = 0.0, val count: Int = 0
)

@Serializable
data class StyleGeometry(
    val displayName: String, val description: String, val category: String,
    val photosPerCard: Int, val cardAspect: Double, val defaultBackground: String, val supportsDate: Boolean,
    val textRoles: List<String>, val photoSlots: List<PhotoSlotGeo>, val textSlots: List<TextSlotGeo>,
    val qrSlot: QrSlotGeo? = null, val decorations: List<DecorationGeo> = emptyList()
)

@Serializable
data class GeometryFile(val version: Int, val referenceCardWidthPt: Double, val styles: Map<String, StyleGeometry>)

object SharedGeometry {
    const val RESOURCE = "estilos-geometria.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): GeometryFile = json.decodeFromString(GeometryFile.serializer(), text)

    val file: GeometryFile by lazy {
        val stream = SharedGeometry::class.java.classLoader?.getResourceAsStream(RESOURCE)
            ?: error("Falta el recurso $RESOURCE (lo copia el build desde shared-fixtures).")
        parse(stream.use { it.readBytes().toString(Charsets.UTF_8) })
    }

    val referenceCardWidthPt: Double get() = file.referenceCardWidthPt

    /** Los ids de la geometría son los mismos `SerialName` del estilo. */
    fun id(style: TemplateStyle): String = when (style) {
        TemplateStyle.INSTAX_WIDE -> "instaxWide"
        else -> style.name.lowercase()
    }

    fun of(style: TemplateStyle): StyleGeometry? = file.styles[id(style)]

    /** Estilos con geometría compartida. */
    val styles: List<TemplateStyle> by lazy { TemplateStyle.entries.filter { of(it) != null } }
}
