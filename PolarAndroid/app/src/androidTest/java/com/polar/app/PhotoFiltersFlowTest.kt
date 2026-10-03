package com.polar.app

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.core.look.LookResolver
import com.polar.app.data.ThemeMode
import com.polar.app.model.*
import com.polar.app.ui.ProvideLayout
import com.polar.app.ui.editor.*
import com.polar.app.ui.theme.PolarTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PhotoFiltersFlowTest {
    @get:Rule val compose=createComposeRule()
    @Test fun scopesApplyAllUndoAndHeldComparisonUseTheRealEditor() {
        val base=ApplicationProvider.getApplicationContext<Context>()
        val root=File(base.cacheDir,"qa-filter-${System.nanoTime()}").apply {mkdirs()}
        val context=object: ContextWrapper(base) {override fun getFilesDir()=root;override fun getCacheDir()=root}
        val container=AppContainer(context)
        val original=File(root,"original.png")
        val bitmap=Bitmap.createBitmap(600,400,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(60,130,200))}
        original.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        val bytes=original.readBytes()
        val id=container.store.create(PolarProject().normalized(),"Prueba filtros")
        val imported=runBlocking {container.photos.import(id,List(10){Uri.fromFile(original).toString()})}
        container.store.save(id,ProjectEdits.fillAll(ProjectEdits.addPhotos(container.store.load(id).project,imported.assets,0)))
        lateinit var vm: EditorViewModel
        compose.runOnUiThread {vm=EditorViewModel(id,EditorDeps(container.store,container.photos,container.exports,{_,_->null},{null}))}
        compose.waitUntil(10_000){!vm.state.value.loading}
        compose.setContent {PolarTheme(ThemeMode.DARK){Surface {ProvideLayout {EditorScreen(vm,container,null,{})}}}}
        compose.runOnUiThread {vm.selectSlot(0)}
        compose.onNodeWithContentDescription("Filtros").performClick()
        compose.onNodeWithTag("filter-presets").performScrollToIndex(3)
        compose.onNodeWithContentDescription("Sepia").performClick()
        compose.runOnIdle {
            assertEquals("sepia",LookResolver.resolve(vm.state.value.project,0).preset)
            assertEquals("original",LookResolver.resolve(vm.state.value.project,1).preset)
        }
        compose.onNode(hasText("Más opciones") and hasClickAction()).performClick()
        compose.onNodeWithText("Esta página").assertIsDisplayed().performClick()
        compose.runOnIdle {assertEquals(LookScope.PAGE,vm.state.value.lookScope)}
        compose.onNodeWithTag("filter-presets").performScrollToIndex(1)
        compose.onNodeWithTag("filters-panel-scroll").performSemanticsAction(SemanticsActions.ScrollBy) {it(0f,140f)}
        compose.onNodeWithContentDescription("Blanco y negro").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("bw",LookResolver.resolve(vm.state.value.project,8).preset)
            assertEquals("original",LookResolver.resolve(vm.state.value.project,9).preset)
        }
        compose.onNodeWithText("Varias páginas").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Página 2").performClick()
        compose.onNodeWithTag("filter-presets").performScrollToIndex(7)
        compose.onNodeWithTag("filters-panel-scroll").performSemanticsAction(SemanticsActions.ScrollBy) {it(0f,180f)}
        compose.onNodeWithContentDescription("Vivo").assertIsDisplayed().performClick()
        compose.runOnIdle {assertEquals("vivid",LookResolver.resolve(vm.state.value.project,9).preset)}
        val before=vm.state.value.project
        compose.onNodeWithText("Aplicar a todas").performScrollTo().performClick()
        compose.onNode(hasText("Deshacer") and hasClickAction()).performClick()
        compose.runOnIdle {assertEquals(before,vm.state.value.project)}
        compose.onNodeWithContentDescription("Cerrar panel").performClick()
        val sheet=compose.onNodeWithTag("print-sheet-0")
        sheet.performTouchInput {down(center)}
        compose.waitUntil(5_000) {vm.state.value.comparing}
        assertEquals(before,vm.state.value.project)
        sheet.performTouchInput {up()}
        compose.waitUntil(5_000) {!vm.state.value.comparing}
        assertArrayEquals(bytes,original.readBytes())
    }
}
