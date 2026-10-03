package com.polar.app.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Replica las reglas de decodificación y validación de PolarMac (Models.swift). */
object MacCompat {
    private val uuid = Regex("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$")
    private val macRoles = setOf("title", "subtitle", "caption", "song", "artist")
    private val photoKeys = setOf("id", "path", "pixelWidth", "pixelHeight")
    private val placementKeys = setOf("assetID", "zoom", "offsetX", "offsetY", "quarterTurns")
    private val appearanceKeys = setOf("fontName", "size", "hex", "bold", "italic", "alignment", "offsetX", "offsetY", "visible")
    private val regionKeys = setOf("id", "x", "y", "width", "height", "isTransparent")

    fun assertReadable(text: String) {
        val root = Json.parseToJsonElement(text).jsonObject
        assertEquals("version", 1, root["version"]!!.jsonPrimitive.intOrNull)
        val photos = root["photos"] as JsonArray
        for (p in photos) {
            val o = p.jsonObject
            assertTrue("foto sin claves: ${photoKeys - o.keys}", o.keys.containsAll(photoKeys))
            assertTrue("id de foto no es UUID", uuid.matches(o["id"]!!.jsonPrimitive.content))
        }
        for (p in root["placements"] as JsonArray) {
            if (p is JsonNull) continue
            val o = p.jsonObject
            assertTrue("colocación sin claves: ${placementKeys - o.keys}", o.keys.containsAll(placementKeys))
            assertTrue("assetID no es UUID", uuid.matches(o["assetID"]!!.jsonPrimitive.content))
        }
        val settings = root["settings"]!!.jsonObject
        assertTrue("specialDate debe ser número", settings["specialDate"]?.jsonPrimitive?.content?.toDoubleOrNull() != null)
        val styles = settings["textStyles"] as? JsonObject ?: JsonObject(emptyMap())
        assertTrue("rol desconocido para Mac: ${styles.keys - macRoles}", macRoles.containsAll(styles.keys))
        for ((_, a) in styles) assertTrue("apariencia sin claves", a.jsonObject.keys.containsAll(appearanceKeys))
        (settings["importedTemplate"] as? JsonObject)?.let { t ->
            for (r in t["regions"] as JsonArray) {
                val o = r.jsonObject
                assertTrue("región sin claves", o.keys.containsAll(regionKeys))
                assertTrue("id de región no es UUID", uuid.matches(o["id"]!!.jsonPrimitive.content))
            }
        }
    }
}
