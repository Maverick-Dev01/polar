package com.polar.app.core.edit

/** Mismos estilos y frases que la Mac; las fuentes son ids de FontCatalog. */
enum class MoodPreset(
    val displayName: String,
    val hex: String,
    val fontName: String,
    val title: String,
    val subtitle: String,
    val caption: String
) {
    COUPLE("Parejas", "92394A", "Gelasio", "Nuestros momentos", "Tú y yo", "Una historia para guardar"),
    FRIENDS("Amigos", "38536F", "Josefin Sans", "Siempre juntos", "Amigos de verdad", "Los mejores recuerdos son compartidos"),
    FAMILY("Familia", "486855", "Libre Baskerville", "Nuestra familia", "Donde empieza todo", "El cariño que nos une"),
    PETS("Mascotas", "BC8952", "Patrick Hand", "Mi mejor compañía", "Huellas en el corazón", "Pequeñas patas, grandes aventuras"),
    TRAVEL("Viajes", "486855", "Josefin Sans", "Nuestra aventura", "Un lugar para recordar", "Coleccionando momentos"),
    CELEBRATION("Celebraciones", "C34048", "Dancing Script", "Un día especial", "Celebremos juntos", "Un recuerdo para siempre"),
    MINIMAL("Minimalista", "20242C", ".System", "Un instante", "Para recordar", "")
}
