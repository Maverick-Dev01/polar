package com.polar.app

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.ThemeMode
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import com.polar.app.ui.ProvideLayout
import com.polar.app.ui.editor.*
import com.polar.app.ui.theme.PolarTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PhotoStudioDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val base get() = ApplicationProvider.getApplicationContext<Context>()
    private fun capture(name: String) {
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = File(base.cacheDir, "photo-studio-qa").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun textKeyboardPhrasesBatchAndGuidesInRealEditor() {
        val root = File(base.cacheDir, "qa-studio-${System.nanoTime()}").apply { mkdirs() }
        val context = object: ContextWrapper(base) { override fun getFilesDir() = root; override fun getCacheDir() = root }
        val container = AppContainer(context)
        val assets = List(4) { i ->
            val file = File(root, "photo-$i.png")
            val image = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply { eraseColor(listOf(Color.rgb(40,100,130),Color.rgb(140,80,90),Color.rgb(50,110,70),Color.rgb(120,100,40))[i]) }
            Canvas(image).drawCircle(600f,400f,180f,Paint().apply {color=Color.WHITE})
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
            PhotoAsset(path = file.absolutePath, pixelWidth = 1200, pixelHeight = 800)
        }
        val project = PolarProject(settings = PrintSettings(columns = 2, rows = 2), photos = assets, placements = assets.map { PhotoPlacement(it.id) }).normalized()
        val id = container.store.create(project, "QA independiente")
        lateinit var vm: EditorViewModel
        compose.runOnUiThread { vm = EditorViewModel(id, EditorDeps(container.store, container.photos, container.exports, {_,_->null}, {null})) }
        compose.waitUntil(10_000) { !vm.state.value.loading }
        val theme = if (InstrumentationRegistry.getArguments().getString("polar.theme") == "light") ThemeMode.LIGHT else ThemeMode.DARK
        compose.setContent { PolarTheme(theme) { Surface { ProvideLayout { EditorScreen(vm, container, null, {}) } } } }
        compose.runOnUiThread { vm.selectSlot(0); vm.openTextForSelected(); vm.setTrayExpanded(true) }
        compose.onNodeWithText("Editar y ver").performScrollTo().performClick()
        val field = compose.onNode(hasSetTextAction() and hasText("Texto que se imprimirá"))
        compose.waitForIdle(); Thread.sleep(250) // Esperar que la ventana nativa del diálogo reciba foco.
        field.performClick().performTextReplacement("Nuestro recuerdo favorito")
        // El IME se anima fuera del reloj de Compose; comprobar la vista una vez abierto.
        Thread.sleep(700)
        field.assertIsFocused()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        compose.waitUntil(5_000) { automation.windows.any { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD && it.root?.isVisibleToUser == true } }
        field.assertIsDisplayed()
        compose.onNodeWithText("Listo").assertIsDisplayed()
        capture("keyboard-dark-${base.resources.configuration.screenWidthDp}")
        compose.onNodeWithText("Buscar una frase").performScrollTo().performClick()
        compose.onNodeWithText("Buscar tema o palabras").performTextInput("mascotas")
        compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(isDialog())).performScrollToIndex(3)
        compose.onNodeWithText("Cuatro patas y mil maneras de alegrarnos el día.").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Tu frase")).performScrollTo().performTextReplacement("Sólo este fragmento")
        compose.onNode(hasSetTextAction() and hasText("Tu frase")).performTextInputSelection(androidx.compose.ui.text.TextRange(10,19))
        compose.onNodeWithText("Usar selección (9)").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.runOnIdle { assertEquals("fragmento", vm.state.value.project.override(0)?.texts?.get("title")) }
        compose.runOnUiThread { vm.setMultiSelecting(true) }
        compose.waitForIdle()
        Thread.sleep(700)
        capture("batch-${base.resources.configuration.screenWidthDp}")
        compose.onNodeWithText("Seleccionar todas").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(4, vm.state.value.selectedSlots.size) }
        compose.onNodeWithText("Copiar a hoja nueva").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2, vm.state.value.project.pageCount); assertEquals(8, vm.state.value.project.placedCount) }
        compose.runOnUiThread { vm.setDesignScope(DesignScope.PAGE); vm.selectStyle(TemplateStyle.POSTCARD) }
        compose.runOnIdle { assertEquals(TemplateStyle.POSTCARD, vm.state.value.project.settingsForPage(1).style); assertEquals(TemplateStyle.POSTCARD, vm.state.value.project.settingsForCard(4).style); assertEquals(TemplateStyle.POLAROID, vm.state.value.project.settingsForPage(0).style) }
        compose.runOnUiThread { vm.setMode(EditorMode.FINISH) }
        compose.waitForIdle()
        // El aviso de cambiar diseño se cierra a 1000 ms; esperar también su salida animada.
        compose.waitUntil(2_500) { compose.onAllNodesWithText("Ahora es Postal.", substring = true).fetchSemanticsNodes().isEmpty() }
        val guides = compose.onNodeWithText("Guías para recortar")
        guides.performScrollTo()
        compose.waitForIdle(); Thread.sleep(250)
        capture("guides-${base.resources.configuration.screenWidthDp}")
        guides.assertIsDisplayed().performClick()
        compose.runOnIdle { assertFalse(vm.state.value.project.settings.cutGuides) }
        capture("finish-dark-${base.resources.configuration.screenWidthDp}")
        runBlocking { vm.flush() }
        assertEquals(vm.state.value.project.placements, container.store.load(id).project.placements)
    }
    @Test fun mlKitProcessesLocalPhotoWithoutChangingTheOriginal() {
        val path = InstrumentationRegistry.getArguments().getString("polar.segmentationPhoto")
        org.junit.Assume.assumeTrue("Requiere foto local de prueba", path != null)
        val input = File(path!!); assertTrue(input.exists())
        val original = input.readBytes()
        val container = AppContainer(base)
        val image = container.bitmaps.loadForPrint(path, 1600)
        val photo = PhotoAsset(path = path, pixelWidth = image.width, pixelHeight = image.height)
        val output = runBlocking { container.backgrounds.mask(photo, File(base.cacheDir, "photo-studio-qa")) }
        val mask = container.bitmaps.loadForPrint(output, 1600)
        var foreground = 0; var background = 0
        for (y in 0 until mask.height step 8) for (x in 0 until mask.width step 8) {
            val alpha = Color.alpha(mask.getPixel(x,y)); if (alpha > 220) foreground++; if (alpha < 30) background++
        }
        assertTrue("Sujetos presentes en la máscara", foreground > 10)
        assertTrue("Fondo separado", background > 10)
        assertArrayEquals(original, input.readBytes())
        println("MLKIT_OK: máscara local con sujeto/fondo y original idéntico")
    }
    @Test fun maskCompositionPreservesSubjectAndExportsWhiteBlackAndTransparent() {
        val photo = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val mask = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
        Canvas(mask).drawRect(RectF(90f, 50f, 210f, 160f), Paint().apply { color = Color.WHITE })
        val asset = PhotoAsset(path = "source", pixelWidth = 900, pixelHeight = 600, maskPath = "mask")
        val settings = PrintSettings(style = TemplateStyle.BORDERLESS, columns = 1, rows = 1, cutGuides = false)
        fun render(hex: String?, look: PhotoLook? = null): Bitmap {
            val p = PolarProject(settings = settings.copy(photoLook = look), photos = listOf(asset), placements = listOf(PhotoPlacement(asset.id, background = PhotoBackground(colorHex = hex, feather = 0.0, shadow = 0.0))))
            val image = Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888)
            PolarRenderer.drawPage(Canvas(image), p, 0, false, 1f, { if (it.path == "mask") mask else photo })
            return image
        }
        val white = render("FFFFFF"); val black = render("000000"); val transparent = render(null)
        val rect = PolarRenderer.calculateCardRects(settings)[0]
        val x = rect.midX.toInt(); val y = rect.midY.toInt()
        assertEquals(Color.RED, white.getPixel(x, y)); assertEquals(Color.RED, black.getPixel(x, y))
        assertEquals(Color.WHITE, white.getPixel((rect.left+2).toInt(), (rect.top+2).toInt()))
        assertEquals(Color.BLACK, black.getPixel((rect.left+2).toInt(), (rect.top+2).toInt()))
        assertEquals(Color.WHITE, transparent.getPixel((rect.left+2).toInt(), (rect.top+2).toInt())) // papel blanco detrás del PNG transparente
        val filtered = render("0000FF", PhotoLook(preset = "bw"))
        val gray = filtered.getPixel((rect.left+2).toInt(), (rect.top+2).toInt())
        assertEquals(Color.red(gray), Color.green(gray)); assertEquals(Color.green(gray), Color.blue(gray))
        filtered.recycle()
        white.recycle(); black.recycle(); transparent.recycle(); photo.recycle(); mask.recycle()
    }
}
