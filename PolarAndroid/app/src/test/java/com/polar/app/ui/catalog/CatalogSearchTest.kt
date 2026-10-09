package com.polar.app.ui.catalog

import com.polar.app.model.TemplateStyle
import com.polar.app.model.description
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSearchTest {
    private val all = TemplateStyle.entries.filter { it != TemplateStyle.IMPORTED }

    @Test fun blankQueryKeepsEverythingInOrder() {
        assertEquals(all, CatalogSearch.filter(all, ""))
        assertEquals(all, CatalogSearch.filter(all, "   "))
    }

    @Test fun ignoresCaseAndAccents() {
        assertEquals(listOf(TemplateStyle.BOTANICAL), CatalogSearch.filter(all, "botanico").filter { it == TemplateStyle.BOTANICAL })
        assertTrue(TemplateStyle.CELEBRATION in CatalogSearch.filter(all, "CELEBRACION"))
        assertTrue(TemplateStyle.MINI in CatalogSearch.filter(all, "instantanea"))
    }

    @Test fun matchesDescriptionAsWellAsName() {
        val byDescription = all.first { it.description.isNotBlank() }
        val word = CatalogSearch.normalize(byDescription.description).split(" ").first { it.length > 4 }
        assertTrue(byDescription in CatalogSearch.filter(all, word))
    }

    @Test fun everyWordMustMatch() {
        assertTrue(CatalogSearch.filter(all, "polaroid zzzzzz").isEmpty())
        assertTrue(TemplateStyle.POLAROID in CatalogSearch.filter(all, "  polaroid  "))
    }

    @Test fun noResultsIsEmptyNotAnError() {
        assertTrue(CatalogSearch.filter(all, "xyzxyz").isEmpty())
    }
}
