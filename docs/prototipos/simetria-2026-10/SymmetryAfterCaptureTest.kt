package com.polar.app

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.AppSettings
import com.polar.app.data.ThemeMode
import com.polar.app.model.*
import com.polar.app.ui.ProvideLayout
import com.polar.app.ui.catalog.CatalogContent
import com.polar.app.ui.editor.*
import com.polar.app.ui.home.HomeScreen
import com.polar.app.ui.home.HomeViewModel
import com.polar.app.ui.onboarding.OnboardingScreen
import com.polar.app.ui.settings.SettingsScreen
import com.polar.app.ui.theme.PolarTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Captura componentes de producción en un host aislado; no edita la biblioteca del usuario. */
class SymmetryAuditCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureMatrix() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.cacheDir, "symmetry-audit").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) {
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getCacheDir() = root
        }
        File(root,"files").deleteRecursively()
        val container = AppContainer(isolated)
        val photo = File(root, "foto.jpg")
        InstrumentationRegistry.getInstrumentation().context.assets.open("auditoria.jpg").use { input -> photo.outputStream().use(input::copyTo) }
        val id = container.store.create(PolarProject(settings = PrintSettings(columns = 2, rows = 2)), "Nuestro viaje de aniversario a la playa")
        val imported = runBlocking { container.photos.import(id, List(5) { android.net.Uri.fromFile(photo).toString() }) }
        val project = ProjectEdits.fillAll(ProjectEdits.addPhotos(container.store.load(id).project, imported.assets, 0))
        val thumb = container.thumbnails.projectPng(project, { container.bitmaps.load(it.path, 512) }, null)
        container.store.save(id, project, thumb)
        container.store.create(project, "Recuerdos")
        lateinit var editor: EditorViewModel
        lateinit var home: HomeViewModel
        compose.runOnUiThread {
            editor = EditorViewModel(id, EditorDeps(container.store, container.photos, container.exports, { _, _ -> null }, { container.bitmaps.load(it, 1024) }))
            home = HomeViewModel(container.store, root)
            home.refresh()
        }
        compose.waitUntil(20_000) { !editor.state.value.loading && home.state.value.loaded }
        val screen = mutableStateOf("inicio")
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val font = mutableStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font.value)) {
                PolarTheme(theme.value) {
                    Surface { ProvideLayout {
                        when (screen.value) {
                            "inicio", "inicio-menu" -> HomeScreen(home, container.store::thumbnailFile, {}, {}, {})
                            "catalogo" -> CatalogContent("Nuevo diseño", null, container.thumbnails, true, {}, {}, {}, {})
                            "ajustes" -> SettingsScreen(AppSettings(theme = theme.value), {}, {}, {}, {}, {})
                            "bienvenida" -> OnboardingScreen({})
                            else -> EditorScreen(editor, container, null, {})
                        }
                    } }
                }
            }
        }
        val prefix=InstrumentationRegistry.getArguments().getString("auditPhase") ?: "despues"
        val size = InstrumentationRegistry.getArguments().getString("auditSize") ?: "actual"
        val screens = listOf("inicio", "inicio-menu", "catalogo", "editor", "fotos", "fotos-amplio", "filtros", "filtros-amplio", "diseno", "diseno-amplio", "texto", "texto-amplio", "papel", "papel-amplio", "encuadre", "terminar", "ajustes", "bienvenida")
        for (dark in listOf(false, true)) for (scale in listOf(1f, 1.3f)) for (target in screens) {
            compose.runOnUiThread {
                theme.value = if (dark) ThemeMode.DARK else ThemeMode.LIGHT
                font.value = scale
                editor.setMode(when (target) { "encuadre" -> EditorMode.CROP; "terminar" -> EditorMode.FINISH; else -> EditorMode.EDIT })
                editor.setTool(when (target) { "fotos", "fotos-amplio" -> Tool.PHOTOS; "filtros", "filtros-amplio" -> Tool.FILTERS; "diseno", "diseno-amplio" -> Tool.DESIGN; "texto", "texto-amplio" -> Tool.TEXT; "papel", "papel-amplio" -> Tool.PAPER; else -> null })
                editor.clearSelection();editor.selectSlot(0)
                editor.setTrayExpanded(target.endsWith("amplio"))
                screen.value = target
            }
            compose.waitForIdle()
            if(target=="inicio-menu") {
                compose.onNodeWithContentDescription("Opciones de Nuestro viaje de aniversario a la playa").performClick()
                compose.waitForIdle()
            }
            // Las miniaturas se calculan en segundo plano: espera el siguiente frame después de su publicación.
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            Thread.sleep(150)
            // PixelCopy del componente real: no incluye barra de tareas ni diálogos del sistema.
            compose.onAllNodes(isRoot()).onFirst().assertIsDisplayed()
            val bitmap=if(target=="inicio-menu") InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!! else compose.onAllNodes(isRoot()).onFirst().captureToImage().asAndroidBitmap()
            val filename = "$prefix-android-$size-${if (dark) "oscuro" else "claro"}-${if (scale == 1f) "100" else "130"}-$target.png"
            File(root, filename).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        println("CAPTURAS: $size, ${screens.size * 4} PNG en ${root.path}")
    }
}
