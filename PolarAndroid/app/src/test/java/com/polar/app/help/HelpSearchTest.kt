package com.polar.app.help

import com.polar.app.SharedFixtures
import org.junit.Assert.*
import org.junit.Test

class HelpSearchTest {
    private val content = HelpContent.parse(SharedFixtures.file("help.json").readText())
    private fun ids(q: String, cat: String? = null) = HelpSearch.filter(content.articulos, q, cat).map { it.id }

    @Test fun normalizeDropsAccentsCaseAndSpaces() {
        assertEquals("cancion con qr", HelpSearch.normalize("  CANCIÓN   con  QR "))
        assertEquals("nino", HelpSearch.normalize("Niño"))
        assertEquals("", HelpSearch.normalize("   "))
    }

    @Test fun searchIgnoresAccentsAndCase() {
        assertTrue("qr-cancion" in ids("canción"))
        assertEquals(ids("canción"), ids("CANCION"))
        assertTrue("quitar-fondo" in ids("Quitar FONDO"))
    }

    @Test fun everyWordMustMatchAnywhereInTheArticle() {
        assertTrue("importar-molde" in ids("molde huecos"))
        assertTrue(ids("molde zzzz").isEmpty())
    }

    @Test fun synonymsFromPalabrasFindTheArticle() {
        assertTrue("encuadrar" in ids("recortar foto"))
        assertTrue("imprimir-100" in ids("tamaño real"))
    }

    @Test fun titleMatchesComeBeforeBodyMatches() {
        val r = ids("filtros")
        assertEquals("filtros", r.first())
    }

    @Test fun blankQueryReturnsEverythingAndCategoryNarrows() {
        assertEquals(content.articulos.size, ids("").size)
        val texto = ids("", "texto")
        assertTrue(texto.isNotEmpty() && texto.all { id -> content.articulos.first { it.id == id }.categoria == "texto" })
        assertTrue(ids("filtros", "imprimir").none { it == "filtros" })
    }

    @Test fun noResultsGivesAnEmptyList() {
        assertTrue(ids("xyzxyz").isEmpty())
    }
}
