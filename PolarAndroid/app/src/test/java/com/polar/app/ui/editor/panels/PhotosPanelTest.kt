package com.polar.app.ui.editor.panels

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.model.PhotoAsset
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class PhotosPanelTest {
    @get:Rule val compose = createComposeRule()
    private val photos = List(3) { PhotoAsset(path = "p$it.jpg", pixelWidth = 10, pixelHeight = 10) }

    @Test
    fun summaryAndPlacing() {
        var placed: String? = null
        compose.setContent {
            PolarTheme(ThemeMode.LIGHT) {
                PhotosPanel(photos, used = setOf(photos[0].id), lowRes = setOf(photos[1].id), missingPhotos = 2,
                    thumbnail = { null }, onAdd = {}, onFill = {}, onPlace = { placed = it })
            }
        }
        compose.onNodeWithText("3 fotos · 1 en la hoja").assertExists()
        compose.onNodeWithText("Faltan 2 fotos de este diseño. Agrégalas y colócalas de nuevo.").assertExists()
        compose.onNodeWithContentDescription("Foto 2, poca resolución").performClick()
        assertEquals(photos[1].id, placed)
    }
}
