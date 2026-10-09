package com.polar.app.engine

import com.polar.app.model.*

/**
 * Posición del QR de los diseños de música (fracciones de la tarjeta; el lado, fracción del ancho).
 * Vinilo y casete la traen en la geometría compartida. Los reproductores la ponen como una
 * etiqueta en la esquina inferior derecha de la foto. Spotify conserva su QR de siempre.
 */
object QrSlots {
    /** Margen blanco alrededor del QR, como fracción de su lado. */
    const val QUIET_ZONE = 0.08

    private val playerRed = QrSlotGeo(x = 0.62, y = 0.40, size = 0.26)
    private val playerGray = QrSlotGeo(x = 0.355, y = 0.60, size = 0.14)

    fun of(style: TemplateStyle): QrSlotGeo? = when (style) {
        TemplateStyle.PLAYER_RED -> playerRed
        TemplateStyle.PLAYER_GRAY -> playerGray
        else -> SharedGeometry.of(style)?.qrSlot
    }
}
