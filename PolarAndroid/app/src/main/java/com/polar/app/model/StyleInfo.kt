package com.polar.app.model

import com.polar.app.model.TemplateStyle.*

enum class DesignCategory(val displayName: String) {
    CLASSIC("Clásicos"), MUSIC("Música"), FILM("Cine"), DATES("Fechas"), OCCASIONS("Ocasiones"), FREE("Libre")
}

val TemplateStyle.category: DesignCategory
    get() = when (this) {
        POLAROID, MINI, SQUARE, BORDERLESS, INSTAGRAM -> DesignCategory.CLASSIC
        PHOTOBOOTH, INSTAX_WIDE -> DesignCategory.CLASSIC
        SPOTIFY, PLAYER_RED, PLAYER_GRAY, VINYL, CASSETTE -> DesignCategory.MUSIC
        FILM_VERTICAL, FILM_HORIZONTAL -> DesignCategory.FILM
        CALENDAR -> DesignCategory.DATES
        TICKET, CELEBRATION, HEART, PETS, BOTANICAL, WASHI -> DesignCategory.OCCASIONS
        CUSTOM, POSTCARD, EDITORIAL, COLLAGE, IMPORTED -> DesignCategory.FREE
    }

/** Fecha disponible salvo donde no hay lugar para ella o ya trae sus propias fechas. */
val TemplateStyle.supportsDate: Boolean
    get() = this !in setOf(FILM_VERTICAL, FILM_HORIZONTAL, CALENDAR, IMPORTED)

/** Roles editables, en el mismo orden que la Mac, más la fecha. */
val TemplateStyle.textRoles: List<TextRole>
    get() {
        val base = when (this) {
            FILM_VERTICAL, FILM_HORIZONTAL, CALENDAR, BORDERLESS, IMPORTED -> emptyList()
            SPOTIFY, PLAYER_RED, PLAYER_GRAY, VINYL, CASSETTE -> listOf(TextRole.SONG, TextRole.ARTIST)
            PHOTOBOOTH, COLLAGE -> listOf(TextRole.TITLE, TextRole.CAPTION)
            INSTAX_WIDE -> listOf(TextRole.TITLE, TextRole.SUBTITLE)
            WASHI -> listOf(TextRole.CAPTION)
            INSTAGRAM -> listOf(TextRole.TITLE, TextRole.CAPTION)
            POSTCARD, EDITORIAL, CELEBRATION -> listOf(TextRole.TITLE, TextRole.SUBTITLE, TextRole.CAPTION)
            else -> listOf(TextRole.TITLE, TextRole.SUBTITLE)
        }
        return if (supportsDate) base + TextRole.DATE else base
    }

val TemplateStyle.description: String
    get() = when (this) {
        POLAROID -> "Marco blanco con pie de foto"
        MINI -> "Más alta, como una instantánea mini"
        SPOTIFY -> "Reproductor y QR a tu canción"
        PLAYER_RED -> "Reproductor a color"
        PLAYER_GRAY -> "Reproductor ancho"
        TICKET -> "Para viajes y conciertos"
        FILM_VERTICAL -> "Tira de cinco fotos"
        FILM_HORIZONTAL -> "Tira horizontal de cinco fotos"
        CALENDAR -> "El mes con su fecha especial"
        INSTAGRAM -> "Como una publicación"
        CUSTOM -> "Marco de color"
        BORDERLESS -> "Sólo la foto, de orilla a orilla"
        SQUARE -> "Polaroid cuadrada"
        POSTCARD -> "Foto grande y un mensaje"
        BOTANICAL -> "Papel crema y hojas"
        CELEBRATION -> "Para cumpleaños y fiestas"
        PETS -> "Para tu mejor compañía"
        HEART -> "Tu foto recortada en corazón"
        EDITORIAL -> "Como una revista"
        IMPORTED -> "Tu propio molde"
        PHOTOBOOTH -> "Tira vertical de 4 fotos, como cabina"
        INSTAX_WIDE -> "Formato ancho con pie grueso"
        VINYL -> "Disco con tu foto de etiqueta"
        CASSETTE -> "Casete con tu foto en la etiqueta"
        COLLAGE -> "Una foto grande y dos pequeñas"
        WASHI -> "Foto con cinta y pie manuscrito"
    }

val TemplateStyle.suggestedPhotoPreset: String? get() = if(this == TemplateStyle.FILM_VERTICAL || this == TemplateStyle.FILM_HORIZONTAL) "bw" else null

/** Los diseños de música admiten un enlace y dibujan su QR cuando existe. */
val TemplateStyle.supportsQr: Boolean get() = category == DesignCategory.MUSIC
