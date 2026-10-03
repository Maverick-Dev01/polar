package com.polar.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.AppSettings
import com.polar.app.data.ThemeMode
import com.polar.app.data.FontCatalog
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Ejercita la navegación y los componentes reales, sin tocar los diseños existentes. */
@RunWith(AndroidJUnit4::class)
class DeviceSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: PolarApplication get() = ApplicationProvider.getApplicationContext()
    private val container get() = app.container
    private lateinit var previous: AppSettings
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var id: String
    private lateinit var name: String

    @Before fun seed(): Unit = runBlocking {
        previous = container.settings.settings.first()
        container.settings.setOnboardingSeen(true)
        container.settings.setTheme(ThemeMode.LIGHT)
        name = "QA Android ${System.currentTimeMillis()}"
        id = container.store.create(PolarProject(settings = PrintSettings(columns = 2, rows = 2)), name)
        val photo = File(app.cacheDir, "qa-photo.jpg")
        val bitmap = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(80, 130, 160))
        Canvas(bitmap).drawRect(100f, 100f, 600f, 900f, Paint().apply { color = Color.YELLOW })
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        val imported = container.photos.import(id, List(5) { Uri.fromFile(photo).toString() })
        assertEquals(5, imported.assets.size)
        assertEquals(0, imported.failed)
        val project = container.store.load(id).project
        container.store.save(id, ProjectEdits.fillAll(ProjectEdits.addPhotos(project, imported.assets, 0)))
    }

    @After fun restoreSettings() = runBlocking {
        scenario?.close()
        container.settings.setTheme(previous.theme)
        container.settings.setUnits(previous.units)
        container.settings.setDefaultPaper(previous.defaultPaper)
        container.settings.setOnboardingSeen(previous.onboardingSeen)
    }

    private fun launchAndOpen() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Abrir $name").fetchSemanticsNodes().isNotEmpty() }
        val open = compose.onNodeWithContentDescription("Abrir $name").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { open(); open() } // Dos toques rápidos deben abrir un único editor.
        compose.onNodeWithText("Hoja 1 de 2").assertExists()
    }

    private fun click(text: String) {
        val node = compose.onNode(hasText(text) and hasClickAction())
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(text) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        if (!node.isDisplayed()) node.performScrollTo()
        node.performSemanticsAction(SemanticsActions.OnClick) { it() }
    }
    private fun closePanel() {
        if (compose.onAllNodesWithContentDescription("Cerrar panel").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithContentDescription("Cerrar panel").performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun screenshot(label: String) {
        val file = File(app.cacheDir, "qa-$label.png")
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun editCropPaperUndoRecreateReopenAndExport() {
        launchAndOpen()
        screenshot("editor")
        compose.onAllNodesWithContentDescription("Tarjeta 1", substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick) { it() }
        click("Encuadrar")
        click("Girar")
        click("Listo")
        compose.onAllNodes(hasText("Texto") and hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.OnClick) { it() }
        click("Más opciones")
        click("Todas las tarjetas")
        val latest = "Última edición · ñ y corazones ♥"
        compose.onNode(hasSetTextAction()).performTextReplacement(latest)
        closePanel()
        compose.onNodeWithContentDescription("Deshacer").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithContentDescription("Más opciones").performSemanticsAction(SemanticsActions.OnClick) { it() }
        click("Rehacer")
        click("Papel")
        click("Más opciones")
        click("A4")
        click("Horizontal")
        closePanel()
        scenario!!.recreate()
        compose.onNodeWithText(name).assertExists()
        compose.onNodeWithContentDescription("Volver a tus diseños").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(10_000) {
            container.store.load(id).project.let {
                it.settings.text(TextRole.TITLE) == latest && it.settings.orientation == PaperOrientation.LANDSCAPE
            }
        }
        compose.onNodeWithContentDescription("Abrir $name").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithContentDescription("Imprimir").performClick()
        compose.onNodeWithText("Terminar").assertExists()
        compose.onNodeWithText("Guardar PDF").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Guardar imagen PNG").performScrollTo().assertIsDisplayed()
        screenshot("finish")
        val project = container.store.load(id).project
        assertEquals(PaperSize.A4, project.settings.paperSize)
        assertEquals(1, project.placements.first()!!.quarterTurns)
        assertEquals(5, project.placedCount)
        val pdf = runBlocking { container.exports.pdf(project, null) }
        PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertEquals(2, renderer.pageCount)
            renderer.openPage(0).use { assertEquals(842, it.width); assertEquals(595, it.height) }
        }
        val png = runBlocking { container.exports.png(project, 1, null) }
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(png.path, bounds)
        assertEquals(3508, bounds.outWidth)
        assertEquals(2480, bounds.outHeight)
    }

    @Test fun catalogSettingsAndEveryBuiltInDesignRenderOnDevice() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Ajustes").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Ajustes").performSemanticsAction(SemanticsActions.OnClick) { it() }
        click("Oscuro")
        click("Pulgadas")
        compose.waitUntil(10_000) { runBlocking { container.settings.settings.first().units == com.polar.app.data.Units.INCHES } }
        compose.onNode(hasText("Pulgadas") and isSelected()).assertExists()
        screenshot("settings-dark")
        compose.onNodeWithContentDescription("Atrás").performSemanticsAction(SemanticsActions.OnClick) { it() }
        screenshot("home-return")
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Nuevo diseño").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Nuevo diseño").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Diseño Polaroid",substring=true).fetchSemanticsNodes().isNotEmpty() }
        screenshot("catalog")
        compose.onNodeWithText("Polaroid").assertExists()
    }

    @Test fun everyBuiltInDesignAndBundledFontRendersOnDevice() {
        for (font in FontCatalog.all.drop(1)) {
            for (bold in listOf(false, true)) for (italic in listOf(false, true)) {
                val face = container.fonts.typeface(font.id, bold, italic)
                assertNotEquals("${font.id}/$bold/$italic", android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,
                    (if (bold) 1 else 0) or (if (italic) 2 else 0)), face)
            }
        }
        val original = container.store.load(id).project
        for (style in TemplateStyle.entries.filter { it != TemplateStyle.IMPORTED }) {
            for (paper in PaperSize.entries) for (orientation in PaperOrientation.entries) {
                val project = ProjectEdits.selectStyle(original, style).let {
                    it.copy(settings = it.settings.copy(paperSize = paper, orientation = orientation)).normalized()
                }.also { it.validated() }
                val sheet = PolarRenderer.paperRect(project.settings)
                val bitmap = Bitmap.createBitmap(240, (240 * sheet.height / sheet.width).toInt(), Bitmap.Config.ARGB_8888)
                PolarRenderer.drawPage(Canvas(bitmap), project, 0, false, (240 / sheet.width).toFloat(),
                    { asset -> container.bitmaps.load(asset.path, 256) }, null, container.fonts)
                assertTrue("$style/$paper/$orientation", bitmap.getPixel(0, 0) != Color.TRANSPARENT)
                bitmap.recycle()
            }
        }
    }

    @Test fun pdfKeepsTheColoredHeartShownInThePng() {
        val project = ProjectEdits.setText(container.store.load(id).project, TextRole.TITLE, "♥")
        val file = runBlocking { container.exports.pdf(project, null) }
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            renderer.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width * 3, page.height * 3, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val red = pixels.count { Color.red(it) > 220 && Color.green(it) < 100 && Color.blue(it) < 100 }
                bitmap.recycle()
                assertTrue("El corazón en color desapareció del PDF (píxeles rojos: $red)", red > 50)
            }
        }
    }    @Test fun filtersReachPngAndPdfWithoutRecoloringTheFrame() {
        val project=ProjectEdits.selectStyle(container.store.load(id).project,TemplateStyle.TICKET).let { ProjectEdits.setAllLooks(it,PhotoLook(preset="bw")) }
        val rect=PolarRenderer.calculatePhotoRects(PolarRenderer.calculateCardRects(project.settings).first(),project.settings.style,project.settings).first()
        fun check(bitmap: Bitmap,scale: Double) {
            val color=bitmap.getPixel((rect.midX*scale).toInt(),(rect.midY*scale).toInt())
            assertTrue(kotlin.math.abs(Color.red(color)-Color.green(color))<=1)
            assertTrue(kotlin.math.abs(Color.green(color)-Color.blue(color))<=1)
            val card=PolarRenderer.calculateCardRects(project.settings).first()
            val accent=bitmap.getPixel(((card.left+card.width*.9)*scale).toInt(),((card.top+card.height*.4)*scale).toInt())
            assertTrue("El marco vino conserva su color",Color.red(accent)>Color.green(accent)+30)
            bitmap.recycle()
        }
        val png=runBlocking {container.exports.png(project,0,null)}
        check(android.graphics.BitmapFactory.decodeFile(png.path),300.0/72)
        val pdf=runBlocking {container.exports.pdf(project,null)}
        PdfRenderer(ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer->renderer.openPage(0).use {page->
            val bitmap=Bitmap.createBitmap(page.width*3,page.height*3,Bitmap.Config.ARGB_8888)
            page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);check(bitmap,3.0)
        } }
    }

}
