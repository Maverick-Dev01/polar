package com.polar.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.polar.app.export.ExportQuality
import com.polar.app.model.PaperSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Units { MM, INCHES }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val units: Units = Units.MM,
    val defaultPaper: PaperSize = PaperSize.LETTER,
    val onboardingSeen: Boolean = false,
    val exportQuality: ExportQuality = ExportQuality.HIGH
)

private val Context.polarDataStore by preferencesDataStore(name = "polar_settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val unitsKey = stringPreferencesKey("units")
    private val paperKey = stringPreferencesKey("default_paper")
    private val onboardingKey = booleanPreferencesKey("onboarding_seen")
    private val exportQualityKey = stringPreferencesKey("export_quality")

    val settings: Flow<AppSettings> = context.polarDataStore.data.map { p ->
        AppSettings(
            theme = p[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            units = p[unitsKey]?.let { runCatching { Units.valueOf(it) }.getOrNull() } ?: Units.MM,
            defaultPaper = p[paperKey]?.let { runCatching { PaperSize.valueOf(it) }.getOrNull() } ?: PaperSize.LETTER,
            onboardingSeen = p[onboardingKey] ?: false,
            exportQuality = p[exportQualityKey]?.let { runCatching { ExportQuality.valueOf(it) }.getOrNull() } ?: ExportQuality.HIGH
        )
    }

    suspend fun setTheme(mode: ThemeMode) { context.polarDataStore.edit { it[themeKey] = mode.name } }
    suspend fun setUnits(units: Units) { context.polarDataStore.edit { it[unitsKey] = units.name } }
    suspend fun setDefaultPaper(paper: PaperSize) { context.polarDataStore.edit { it[paperKey] = paper.name } }
    suspend fun setExportQuality(quality: ExportQuality) { context.polarDataStore.edit { it[exportQualityKey] = quality.name } }
    suspend fun setOnboardingSeen(seen: Boolean) { context.polarDataStore.edit { it[onboardingKey] = seen } }
}
