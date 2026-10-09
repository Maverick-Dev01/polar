package com.polar.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.model.PaperSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsRepositoryTest {
    @Test
    fun savesAndReadsSettings() = runTest {
        val repo = SettingsRepository(ApplicationProvider.getApplicationContext())
        assertEquals(AppSettings(), repo.settings.first())
        repo.setTheme(ThemeMode.DARK); repo.setUnits(Units.INCHES)
        repo.setDefaultPaper(PaperSize.A4); repo.setOnboardingSeen(true)
        assertEquals(AppSettings(ThemeMode.DARK, Units.INCHES, PaperSize.A4, true), repo.settings.first())
    }

    @Test
    fun exportQualityDefaultsToHighAndPersists() = runTest {
        val repo = SettingsRepository(ApplicationProvider.getApplicationContext())
        assertEquals(com.polar.app.export.ExportQuality.HIGH, repo.settings.first().exportQuality)
        repo.setExportQuality(com.polar.app.export.ExportQuality.LIGHT)
        assertEquals(com.polar.app.export.ExportQuality.LIGHT, SettingsRepository(ApplicationProvider.getApplicationContext()).settings.first().exportQuality)
    }
}
