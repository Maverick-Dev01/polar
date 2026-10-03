package com.polar.app.data

import com.polar.app.R

enum class FontGroup(val displayName: String) {
    SYSTEM("Del sistema"), HANDWRITTEN("Manuscritas"), BOOK("De libro"),
    SANS("Palo seco"), TYPEWRITER("Máquina de escribir"), DISPLAY("Decorativas")
}

data class FontChoice(
    val id: String,
    val group: FontGroup,
    val res: Int? = null,
    val previewScale: Float = 1f,
    val asset: String? = null
) {
    val displayName: String get() = if (res == null && asset == null) "Sistema" else id
}

object FontCatalog {
    const val SYSTEM_ID = ".System"

    val all: List<FontChoice> = listOf(
        FontChoice(SYSTEM_ID, FontGroup.SYSTEM, null),
        FontChoice("Caveat", FontGroup.HANDWRITTEN, R.font.caveat, 1.2f),
        FontChoice("Kalam", FontGroup.HANDWRITTEN, R.font.kalam),
        FontChoice("Homemade Apple", FontGroup.HANDWRITTEN, R.font.homemade_apple, 0.75f),
        FontChoice("Sacramento", FontGroup.HANDWRITTEN, R.font.sacramento, 1.3f),
        FontChoice("Dancing Script", FontGroup.HANDWRITTEN, R.font.dancing_script, 1.1f),
        FontChoice("Patrick Hand", FontGroup.HANDWRITTEN, R.font.patrick_hand, 1.05f),
        FontChoice("Shadows Into Light", FontGroup.HANDWRITTEN, R.font.shadows_into_light),
        FontChoice("Amatic SC", FontGroup.HANDWRITTEN, R.font.amatic_sc, 1.25f),
        FontChoice("Gelasio", FontGroup.BOOK, R.font.gelasio),
        FontChoice("Libre Baskerville", FontGroup.BOOK, R.font.libre_baskerville, 0.9f),
        FontChoice("EB Garamond", FontGroup.BOOK, R.font.eb_garamond, 1.05f),
        FontChoice("Josefin Sans", FontGroup.SANS, asset = "fonts/josefin_sans.ttf"),
        FontChoice("Nunito", FontGroup.SANS, asset = "fonts/nunito.ttf"),
        FontChoice("Quicksand", FontGroup.SANS, asset = "fonts/quicksand.ttf"),
        FontChoice("Montserrat", FontGroup.SANS, previewScale = 0.9f, asset = "fonts/montserrat.ttf"),
        FontChoice("Special Elite", FontGroup.TYPEWRITER, R.font.special_elite, 0.9f),
        FontChoice("Courier Prime", FontGroup.TYPEWRITER, R.font.courier_prime, 0.9f),
        FontChoice("Abril Fatface", FontGroup.DISPLAY, R.font.abril_fatface, 0.9f),
        FontChoice("Pacifico", FontGroup.DISPLAY, R.font.pacifico, 0.9f),
        FontChoice("Lobster", FontGroup.DISPLAY, R.font.lobster)
    )

    /** Nombres de fuentes de la Mac y de la versión anterior de Android. */
    private val aliases = mapOf(
        "Georgia" to "Gelasio", "Baskerville" to "Libre Baskerville", "AvenirNext-Medium" to "Josefin Sans",
        "Avenir Next" to "Josefin Sans", "ChalkboardSE-Regular" to "Patrick Hand", "SnellRoundhand" to "Dancing Script",
        "Courier" to "Courier Prime", "serif" to "Gelasio", "sans-serif-medium" to "Montserrat",
        "casual" to "Patrick Hand", "cursive" to "Dancing Script"
    )

    /** Fuentes a las que apuntan los alias; las pruebas verifican que existan. */
    internal val aliasTargets: Collection<String> get() = aliases.values
    internal val aliasNames: Collection<String> get() = aliases.keys

    fun find(id: String): FontChoice =
        all.firstOrNull { it.id == id }
            ?: aliases[id]?.let { alias -> all.first { it.id == alias } }
            ?: all.first()

    fun groups(): List<Pair<FontGroup, List<FontChoice>>> =
        FontGroup.entries.map { g -> g to all.filter { it.group == g } }.filter { it.second.isNotEmpty() }
}
