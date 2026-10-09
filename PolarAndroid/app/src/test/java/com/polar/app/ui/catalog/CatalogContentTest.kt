package com.polar.app.ui.catalog

import android.graphics.Typeface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.engine.FontProvider
import com.polar.app.engine.Thumbnailer
import com.polar.app.ui.theme.PolarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CatalogContentTest {
    @get:Rule val compose = createComposeRule()

    private fun show() = compose.setContent {
        PolarTheme(ThemeMode.LIGHT) {
            CatalogContent("Elige un diseño", null, Thumbnailer(FontProvider { _, _, _ -> Typeface.DEFAULT }),
                showImport = false, onPick = {}, onImportTemplate = {}, onOpenPolar = {}, onBack = {})
        }
    }

    @Test fun searchWithoutResultsShowsUsefulEmptyStateAndShowAllRecovers() {
        show()
        compose.onNodeWithText("Buscar diseños").performTextInput("zzzqq")
        compose.onNodeWithText("No hay diseños con «zzzqq»").assertIsDisplayed()
        compose.onNodeWithText("Ver todos", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Polaroid").assertIsDisplayed()
    }
}
