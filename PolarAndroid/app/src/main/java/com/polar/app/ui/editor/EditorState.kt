package com.polar.app.ui.editor

import com.polar.app.export.ExportQuality
import android.graphics.Bitmap
import com.polar.app.core.edit.MoodPreset
import com.polar.app.data.PhotoSource
import com.polar.app.data.ProjectStore
import com.polar.app.model.*
import com.polar.app.ui.UiText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

enum class Tool { PHOTOS, FILTERS, DESIGN, TEXT, PAPER }
enum class EditorMode { EDIT, CROP, FINISH, CHANGE_DESIGN }
enum class LookScope { ALL, PAGE, PAGES, PHOTO }
enum class DesignScope { ALL, PAGE, CARD }
enum class TextScope { ALL, CARD }
enum class ExportAction { PRINT, SAVE, SHARE }
enum class ExportFormat(val mime: String, val pdf: Boolean) {
    PDF("application/pdf", true),
    PNG("image/png", false), JPEG("image/jpeg", false)
}

data class EditorUiState(
    val loading: Boolean = true,
    val project: PolarProject = PolarProject().normalized(),
    val page: Int = 0,
    val selectedSlot: Int? = null,
    val multiSelecting: Boolean = false,
    val selectedSlots: Set<Int> = emptySet(),
    val designScope: DesignScope = DesignScope.ALL,
    val tool: Tool? = null,
    val trayExpanded: Boolean = false,
    val lookScope: LookScope = LookScope.ALL,
    val lookPages: Set<Int> = emptySet(),
    val comparing: Boolean = false,
    val textRole: TextRole = TextRole.TITLE,
    val textScope: TextScope = TextScope.ALL,
    val mode: EditorMode = EditorMode.EDIT,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val saveFailed: Boolean = false,
    val missingPhotos: Int = 0,
    val editingRegions: Boolean = false,
    val templateVersion: Int = 0,
    val busy: Boolean = false,
    val backgroundError: UiText? = null,
    val lastMood: MoodPreset = MoodPreset.COUPLE,
    val loadFailed: Boolean = false,
    val focusBackground: Boolean = false
) {
    val selectedCard: Int? get() = selectedSlot?.let { project.cardOfSlot(it) }
    val selectedPlacement: PhotoPlacement? get() = selectedSlot?.let { project.placements.getOrNull(it) }
    val editCard: Int? get() = if (textScope == TextScope.CARD) selectedCard else null
    val usedAssetIds: Set<String> get() = project.placements.mapNotNull { it?.assetID }.toSet()
    val selectedCardNumber: Int? get() = selectedCard?.let { it % project.cardsPerPage + 1 }
}

sealed interface EditorEvent {
    data class Message(val text: UiText, val undoable: Boolean = false) : EditorEvent
    data class Exported(val file: File, val mime: String, val pdf: Boolean) : EditorEvent
}

interface ExportService {
    suspend fun pdf(project: PolarProject, template: Bitmap?, quality: ExportQuality = ExportQuality.HIGH): File
    suspend fun png(project: PolarProject, page: Int, template: Bitmap?, quality: ExportQuality = ExportQuality.HIGH): File
    suspend fun jpg(project: PolarProject, page: Int, template: Bitmap?, quality: ExportQuality = ExportQuality.HIGH): File
}

class EditorDeps(
    val store: ProjectStore,
    val photos: PhotoSource,
    val exports: ExportService,
    val thumbnail: suspend (PolarProject, Bitmap?) -> ByteArray?,
    val loadTemplate: (String) -> Bitmap?,
    val io: CoroutineDispatcher = Dispatchers.IO,
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    val autosaveDelayMs: Long = 800,
    val removeBackground: (suspend (PhotoAsset, File) -> String)? = null
)
