package com.polar.app.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object PolarJson {
    /** Mismo formato que la Mac: todas las claves, nulos omitidos, legible. */
    val format: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
        coerceInputValues = true
    }

    fun encode(project: PolarProject): String = format.encodeToString(PolarProject.serializer(), project)

    fun decode(text: String): PolarProject {
        if (text.length > 5_000_000) throw PolarException("El archivo es demasiado grande.")
        val project = try {
            format.decodeFromString(PolarProject.serializer(), text)
        } catch (e: SerializationException) {
            throw PolarException("El archivo no es un diseño de Polar.")
        } catch (e: IllegalArgumentException) {
            throw PolarException("El archivo no es un diseño de Polar.")
        }
        project.validated()
        return project.normalized()
    }
}


/** Un preset futuro no vuelve ilegible un proyecto. */
object LookPresetSerializer : kotlinx.serialization.KSerializer<String> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LookPreset", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): String = decoder.decodeString().let { if (it in PhotoLook.PRESETS) it else "original" }
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: String) = encoder.encodeString(if (value in PhotoLook.PRESETS) value else "original")
}
