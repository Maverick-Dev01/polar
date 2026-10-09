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

    // ----- Etiquetas «entre comillas» = textos reales de la pantalla -----

    private val stringsXml = java.io.File(SharedFixtures.root.parentFile, "PolarAndroid/app/src/main/res/values/strings.xml").readText()
    private fun norm(t: String) = t.replace("…", "").replace("...", "").replace(Regex("^[^\\p{L}\\p{N}]+"), "").trim()
    /** Cada texto de strings.xml como expresión regular (los %1$d y %1$s valen cualquier cosa). */
    private val screenTexts: List<Regex> = Regex("<string name=\"[^\"]+\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL).findAll(stringsXml).map { m ->
        val raw = m.groupValues[1].replace("&amp;", "&").replace("\\'", "'").replace("&lt;", "<").replace("&gt;", ">")
        val parts = raw.split(Regex("%\\d\\$[ds]|%%"))
        Regex(parts.joinToString(".+") { Regex.escape(norm(it)) }.let { if (parts.sumOf { norm(it).length } < 4) "(?!)" else it })
    }.toList()
    private fun labelsIn(text: String) = Regex("«([^»]+)»").findAll(text).map { it.groupValues[1] }.toList()
    private fun existsOnScreen(label: String): Boolean {
        val l = norm(label).replace(Regex("\\bN\\b"), "1")
        return screenTexts.any { it.matches(l) || it.matches(l.replace("1", "1")) } || stringsXml.contains(">" + label + "<") || screenTexts.any { it.pattern.contains(Regex.escape(l)) && it.matches(l) }
    }

    @Test fun everyQuotedLabelOfWhatAndroidShowsExistsInStringsXml() {
        val missing = mutableListOf<String>()
        fun check(where: String, text: String) = labelsIn(text).filterNot(::existsOnScreen).forEach { missing += "$where: «$it»" }
        content.articulos.forEach { a ->
            // Lo que ve Android: pasosAndroid si existe, si no pasos. (pasosMac no se prueba aquí: lo prueba la Mac.)
            a.pasosAqui.forEach { check(a.id, it) }
            check(a.id + " resumen", a.resumen)
        }
        content.recorrido.forEach { check("recorrido ${it.id}", it.textoAqui) }
        content.controles.forEach { check("control ${it.id}", it.textoAqui) }
        assertEquals("Etiquetas que no existen en strings.xml:\n" + missing.joinToString("\n"), emptyList<String>(), missing)
    }

    @Test fun theLabelCheckItselfCatchesInventedLabels() {
        assertTrue(existsOnScreen("Guardar y usar")); assertTrue(existsOnScreen("Sólo tarjeta N")); assertTrue(existsOnScreen("Agregar espacio"))
        assertFalse(existsOnScreen("Importar plantilla")); assertFalse(existsOnScreen("Estilo rápido")); assertFalse(existsOnScreen("Comparar"))
    }

    @Test fun platformVariantsAreUsedWhenPresentAndFallBackToSharedSteps() {
        val withVariant = content.articulos.first { it.pasosAndroid != null }
        assertEquals(withVariant.pasosAndroid, withVariant.pasosAqui)
        val shared = content.articulos.first { it.pasosAndroid == null }
        assertEquals(shared.pasos, shared.pasosAqui)
        assertTrue(content.articulos.any { it.pasosMac != null })
    }
}
