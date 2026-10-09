package com.polar.app

import android.app.Application
import android.content.Context
import com.polar.app.core.billing.AllUnlocked
import com.polar.app.core.billing.Entitlements
import com.polar.app.data.AndroidFontProvider
import com.polar.app.data.BitmapLoader
import com.polar.app.data.PhotoImporter
import com.polar.app.data.ProjectStore
import com.polar.app.data.SettingsRepository
import com.polar.app.engine.Thumbnailer
import com.polar.app.export.AndroidExportService
import com.polar.app.template.TemplateLibrary

class AppContainer(context: Context) {
    val store = ProjectStore(context.filesDir)
    val templates = TemplateLibrary(context.filesDir)
    val bitmaps = BitmapLoader(context)
    val fonts = AndroidFontProvider(context)
    val exports = AndroidExportService(context, bitmaps, fonts)
    val backgrounds = com.polar.app.data.BackgroundRemover(context.applicationContext, bitmaps)
    val photos = PhotoImporter(context, store, bitmaps)
    val settings = SettingsRepository(context)
    val thumbnails = Thumbnailer(fonts)
    /** Hoy todo está desbloqueado; no se usa para bloquear nada (docs/monetizacion.md). */
    val entitlements: Entitlements = AllUnlocked
}

class PolarApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.polar.app.engine.PolarRenderer.qrTooLongLabel = getString(R.string.qr_too_long_marker)
        container.store.purgeExpiredTrash() // lo borrado hace más de 7 días ya no se puede recuperar
    }
}
