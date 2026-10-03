package com.polar.app.data

import com.polar.app.core.edit.MoodPreset
import org.junit.Assert.*
import org.junit.Test

class FontCatalogTest {
    @Test
    fun twentyBundledFontsPlusSystem() {
        assertEquals(21, FontCatalog.all.size)
        assertEquals(FontCatalog.all.size, FontCatalog.all.map { it.id }.toSet().size)
        assertEquals(20, FontCatalog.all.count { it.res != null || it.asset != null })
    }

    @Test
    fun macNamesMapToBundledFonts() {
        assertEquals("Gelasio", FontCatalog.find("Georgia").id)
        assertEquals("Libre Baskerville", FontCatalog.find("Baskerville").id)
        assertEquals("Dancing Script", FontCatalog.find("SnellRoundhand").id)
        assertEquals(FontCatalog.SYSTEM_ID, FontCatalog.find(".System").id)
        assertEquals(FontCatalog.SYSTEM_ID, FontCatalog.find("Fuente que no existe").id)
    }

    @Test
    fun everyMoodFontExists() {
        for (mood in MoodPreset.entries) {
            assertEquals(mood.fontName, FontCatalog.find(mood.fontName).id)
        }
    }

    @Test
    fun groupsKeepCatalogOrder() {
        val groups = FontCatalog.groups()
        assertEquals(FontGroup.SYSTEM, groups.first().first)
        assertEquals(21, groups.sumOf { it.second.size })
    }

    @Test
    fun aliasTargetsExist() {
        for (name in FontCatalog.aliasNames) {
            assertNotEquals("$name cae en el sistema", FontCatalog.SYSTEM_ID, FontCatalog.find(name).id)
        }
        for (target in FontCatalog.aliasTargets) {
            assertEquals(target, FontCatalog.all.first { it.id == target }.id)
        }
    }

    @Test
    fun thinVariableFontsLoadFromAssets() {
        val expected = mapOf(
            "Josefin Sans" to "josefin_sans", "Montserrat" to "montserrat",
            "Nunito" to "nunito", "Quicksand" to "quicksand"
        )
        for ((id, file) in expected) {
            val choice = FontCatalog.find(id)
            assertEquals(id, choice.id)
            assertEquals("fonts/$file.ttf", choice.asset)
            assertNull(choice.res)
        }
    }
}
