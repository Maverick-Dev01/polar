package com.polar.app.ui.home

import com.polar.app.MainDispatcherRule
import com.polar.app.R
import com.polar.app.data.ProjectStore
import com.polar.app.model.PolarProject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun setup(): Pair<ProjectStore, HomeViewModel> {
        var t = 0L
        val store = ProjectStore(tmp.newFolder("files")) { ++t }
        store.create(PolarProject(), "Viaje a Oaxaca")
        store.create(PolarProject(), "Boda Lu y Max")
        store.create(PolarProject(), "Álbum de Rocky")
        return store to HomeViewModel(store, tmp.newFolder("cache"), main.dispatcher)
    }

    @Test
    fun listsNewestFirstAndSortsByNameInSpanish() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        assertEquals(listOf("Álbum de Rocky", "Boda Lu y Max", "Viaje a Oaxaca"), vm.state.value.visible.map { it.name })
        vm.setSort(SortMode.NAME)
        assertEquals("Álbum de Rocky", vm.state.value.visible.first().name)
    }

    @Test
    fun searchIgnoresCaseAndSpaces() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        vm.setQuery("  boda ")
        assertEquals(listOf("Boda Lu y Max"), vm.state.value.visible.map { it.name })
    }

    @Test
    fun deleteThenUndo() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        val target = vm.state.value.visible.first()
        vm.delete(target.id); advanceUntilIdle()
        val event = vm.events.first() as HomeEvent.Deleted
        assertEquals(target.name, event.name)
        assertEquals(2, vm.state.value.visible.size)
        vm.undoDelete(target.id); advanceUntilIdle()
        assertEquals(3, vm.state.value.visible.size)
    }

    @Test
    fun duplicateAddsCopy() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        val target = vm.state.value.visible.first { it.name == "Boda Lu y Max" }
        vm.duplicate(target.id); advanceUntilIdle()
        assertTrue(vm.state.value.visible.any { it.name == "Boda Lu y Max (copia)" })
        assertEquals(R.string.home_duplicated, (vm.events.first() as HomeEvent.Message).text.id)
    }

    @Test
    fun renameIgnoresBlank() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        val id = vm.state.value.visible.first().id
        vm.rename(id, "   "); advanceUntilIdle()
        assertEquals("Álbum de Rocky", vm.state.value.visible.first { it.id == id }.name)
        vm.rename(id, "Rocky 2026"); advanceUntilIdle()
        assertEquals("Rocky 2026", vm.state.value.visible.first { it.id == id }.name)
        assertEquals(R.string.home_renamed, (vm.events.first() as HomeEvent.Message).text.id)
    }

    @Test
    fun deleteOfMissingProjectShowsMessage() = runTest(main.dispatcher) {
        val (store, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        val target = vm.state.value.visible.first()
        store.projectDir(target.id).deleteRecursively() // desaparece fuera de la app tras el refresh
        vm.delete(target.id); advanceUntilIdle()
        val event = vm.events.first() as HomeEvent.Message
        assertEquals(R.string.action_failed, event.text.id)
    }
}
