package com.polar.app.core.text

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SuggestedPhrase(val id: String, val category: String, val text: String, val tags: String)

object PhraseCatalog {
    fun load(context: Context): List<SuggestedPhrase> = context.assets.open("catalog/phrases.json").bufferedReader().use {
        Json.decodeFromString<List<SuggestedPhrase>>(it.readText())
    }
    fun search(entries: List<SuggestedPhrase>, query: String, category: String?): List<SuggestedPhrase> =
        entries.filter { (category == null || it.category == category) && (query.isBlank() ||
            "$it".contains(query.trim(), ignoreCase = true)) }
}
