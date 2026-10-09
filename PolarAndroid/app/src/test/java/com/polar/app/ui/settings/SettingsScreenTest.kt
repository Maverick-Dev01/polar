package com.polar.app.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.AppSettings
import com.polar.app.data.ThemeMode
import com.polar.app.model.PaperSize
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun defaultPaperOffersAllEightSizes() {
        val picked = mutableListOf<PaperSize>()
        compose.setContent {
            PolarTheme(ThemeMode.LIGHT) {
                SettingsScreen(AppSettings(), 0, {}, {}, {}, { picked += it }, {}, {})
            }
        }
        assertEquals(8, PaperSize.entries.size)
        PaperSize.entries.forEach { compose.onNodeWithText(it.displayName).performClick() }
        assertEquals(PaperSize.entries.toList(), picked)
    }
}
