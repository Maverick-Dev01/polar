package com.polar.app.help

import com.polar.app.SharedFixtures
import org.junit.Assert.*
import org.junit.Test

/** Valida el `help.json` real del repositorio (el mismo que copia Gradle a los assets y que usa la Mac). */
class HelpContentTest {
    private val text = SharedFixtures.file("help.json").readText()
    private val content = HelpContent.parse(text)

    @Test fun theSharedFileHasNoSchemaProblems() {
        assertEquals(emptyList<String>(), content.problems())
    }

    @Test fun hasAboutTwentyArticlesInTheFiveCategories() {
        assertTrue("Debe haber unos 20 artículos", content.articulos.size in 18..26)
        assertEquals(listOf("empezar", "fotos", "texto", "diseno", "imprimir"), content.categorias.map { it.id })
        content.categorias.forEach { c -> assertTrue("Categoría vacía: ${c.id}", content.articulos.any { it.categoria == c.id }) }
    }

    @Test fun theTourHasFiveStepsInTheAgreedOrder() {
        assertEquals(listOf("tool.fotos", "hoja", "tool.texto", "tool.filtros", "top.imprimir"), content.recorrido.map { it.objetivo })
    }

    @Test fun everyDestinoOfTheJsonMapsToANavigableTarget() {
        val used = content.articulos.mapNotNull { it.destino }.toSet()
        assertTrue(used.isNotEmpty())
        (content.destinos + used).toSet().forEach { id ->
            assertNotNull("El destino «$id» no tiene pantalla en Android", HelpDestination.fromId(id))
            // Con y sin un diseño abierto, siempre hay a dónde ir.
            assertNotNull(HelpNav.resolve(id, projectId = "p1"))
            assertNotNull(HelpNav.resolve(id, projectId = null))
        }
        // Y al revés: Android no promete destinos que el JSON no declare.
        assertEquals(content.destinos.toSet(), HelpDestination.entries.map { it.id }.toSet())
    }

    @Test fun everyEditorDestinoHasALandingAction() {
        content.destinos.filter { HelpDestination.fromId(it)!!.needsEditor }.forEach { id ->
            assertNotEquals("«$id» no abre nada en el editor", EditorLanding.NONE, EditorLanding.of(id))
        }
        assertEquals(EditorLanding.TOUR, EditorLanding.of(HelpDestination.TOUR_ID))
        assertEquals(EditorLanding.NONE, EditorLanding.of("no-existe"))
    }

    @Test fun controlIdsUsedByTheScreensAllHaveAnExplanation() {
        val ids = content.controles.map { it.id }.toSet()
        HelpIds.all.forEach { assertTrue("Falta la explicación de $it", it in ids) }
    }

    @Test fun validationCatchesBrokenContent() {
        val broken = content.copy(
            articulos = content.articulos + content.articulos.first().copy(categoria = "nada", destino = "inventado", animacion = "zzz", pasos = emptyList()),
            recorrido = content.recorrido.drop(1)
        )
        val problems = broken.problems().joinToString("\n")
        assertTrue(problems, "categoría" in problems && "destino" in problems && "animación" in problems && "pasos" in problems && "id repetido" in problems && "recorrido" in problems)
    }

    @Test fun malformedJsonIsReportedNotCrashed() {
        assertNull(HelpContent.parseOrNull("{ no es json"))
        assertNull(HelpContent.parseOrNull("""{"version":99,"categorias":[],"destinos":[],"animaciones":{},"recorrido":[],"controles":[],"articulos":[]}"""))
    }
}
