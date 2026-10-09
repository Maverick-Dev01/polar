package com.polar.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.app.R
import com.polar.app.data.ProjectMeta
import com.polar.app.data.ProjectStore
import com.polar.app.ui.UiText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import java.text.Collator
import java.util.Locale

enum class SortMode { RECENT, NAME }

data class HomeUiState(
    val all: List<ProjectMeta> = emptyList(),
    val query: String = "",
    val sort: SortMode = SortMode.RECENT,
    val loaded: Boolean = false
) {
    val visible: List<ProjectMeta>
        get() {
            val q = query.trim()
            val filtered = if (q.isEmpty()) all else all.filter { it.name.contains(q, ignoreCase = true) }
            return if (sort == SortMode.NAME) filtered.sortedWith(compareBy(Collator.getInstance(Locale.forLanguageTag("es"))) { it.name }) else filtered
        }
}

sealed interface HomeEvent {
    data class Deleted(val id: String, val name: String) : HomeEvent
    data class Message(val text: UiText) : HomeEvent
    data class SharePolar(val file: File) : HomeEvent
    data class Opened(val id: String) : HomeEvent
}

class HomeViewModel(
    private val store: ProjectStore,
    private val cacheDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(io) { store.list() }
            _state.update { it.copy(all = list, loaded = true) }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun setSort(sort: SortMode) = _state.update { it.copy(sort = sort) }

    fun rename(id: String, name: String) {
        val clean = name.trim().take(80)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            try {
                withContext(io) { store.rename(id, clean) }
                refresh(); _events.send(HomeEvent.Message(UiText(R.string.home_renamed)))
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                failed()
            }
        }
    }

    fun duplicate(id: String) {
        val meta = _state.value.all.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            try {
                withContext(io) { store.duplicate(id, "${meta.name} (copia)") }
                refresh(); _events.send(HomeEvent.Message(UiText(R.string.home_duplicated)))
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                failed()
            }
        }
    }

    fun delete(id: String) {
        val meta = _state.value.all.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            try {
                withContext(io) { store.delete(id) }
                refresh(); _events.send(HomeEvent.Deleted(id, meta.name))
            } catch (e: IOException) {
                refresh(); failed()
            }
        }
    }

    fun undoDelete(id: String) {
        viewModelScope.launch {
            try {
                withContext(io) { store.restore(id) }
                refresh()
            } catch (e: IOException) {
                failed()
            }
        }
    }

    fun sharePolar(id: String) {
        val meta = _state.value.all.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            try {
                val file = withContext(io) {
                    val dir = File(cacheDir, "exports").apply { mkdirs() }
                    File(dir, meta.name.replace(Regex("[^\\p{L}\\p{N} _-]"), "").ifBlank { "Polar" } + ".polar")
                        .apply { writeText(store.exportPolar(id)) }
                }
                _events.send(HomeEvent.SharePolar(file))
            } catch (e: IOException) {
                failed()
            }
        }
    }

    /** «Abrir archivo .polar»: crea un proyecto nuevo con el contenido del archivo y lo abre. */
    fun openPolar(text: String, displayName: String, fallbackName: String) {
        viewModelScope.launch {
            try {
                val id = withContext(io) { store.importPolar(text, displayName.removeSuffix(".polar").ifBlank { fallbackName }) }
                _events.send(HomeEvent.Opened(id))
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _events.send(HomeEvent.Message(UiText(R.string.catalog_bad_file)))
            }
        }
    }

    private suspend fun failed() = _events.send(HomeEvent.Message(UiText(R.string.action_failed)))
}
