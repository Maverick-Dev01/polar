package com.polar.app.model

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PhotoLookContractTest {
    private fun sample(): PolarProject = PolarJson.decode(javaClass.classLoader!!.getResource("fixtures/photo_looks.polar")!!.readText())
    @Test fun individualFilmPhotoWinsAndPagesLeaveOtherPagesIntact() {
        val p = sample()
        assertEquals("cool", com.polar.app.core.look.LookResolver.resolve(p,1).preset)
        assertEquals("bw", com.polar.app.core.look.LookResolver.resolve(p,2).preset)
        assertEquals("sepia", com.polar.app.core.look.LookResolver.resolve(p,5).preset)
        val next = com.polar.app.core.edit.ProjectEdits.setPageLooks(p,PhotoLook(preset="warm"),setOf(0))
        assertEquals("warm",com.polar.app.core.look.LookResolver.resolve(next,1).preset)
        assertEquals("sepia",com.polar.app.core.look.LookResolver.resolve(next,5).preset)
        assertEquals("Segunda página",next.override(1)!!.texts["title"])
        val all = com.polar.app.core.edit.ProjectEdits.setAllLooks(next,PhotoLook(preset="vivid"))
        assertTrue(all.placements.filterNotNull().all { it.photoLook == null })
        assertTrue(all.cardOverrides.values.all { it.photoLook == null })
        assertEquals("vivid",com.polar.app.core.look.LookResolver.resolve(all,1).preset)
    }
    @Test fun chosenPagesAndOnePhotoHaveDistinctScope() {
        val p = sample().copy(placements=sample().placements + List(5) { PhotoPlacement(assetID=sample().photos[0].id) })
        val pages=com.polar.app.core.edit.ProjectEdits.setPageLooks(p,PhotoLook(preset="faded"),setOf(0,2))
        assertEquals("faded",com.polar.app.core.look.LookResolver.resolve(pages,10).preset)
        assertEquals("sepia",com.polar.app.core.look.LookResolver.resolve(pages,5).preset)
        val one=com.polar.app.core.edit.ProjectEdits.setPhotoLook(pages,PhotoLook(),11)
        assertEquals("original",com.polar.app.core.look.LookResolver.resolve(one,11).preset)
        assertEquals("faded",com.polar.app.core.look.LookResolver.resolve(one,12).preset)
    }
    @Test fun legacyUnknownAndInvalidLookValuesAreHandled() {
        assertEquals(PhotoLook(),com.polar.app.core.look.LookResolver.resolve(PolarJson.decode("{}"),0))
        val future=PolarJson.decode("{\"settings\":{\"photoLook\":{\"preset\":\"future\"}}}")
        assertEquals("original",future.settings.photoLook!!.preset)
        assertThrows(PolarException::class.java) { sample().copy(settings=sample().settings.copy(photoLook=PhotoLook(light=2.0))).validated() }
        val zoomed=sample().copy(placements=listOf(sample().placements[0]!!.copy(zoom=0.5)))
        zoomed.validated()
        assertThrows(PolarException::class.java) { zoomed.copy(placements=listOf(zoomed.placements[0]!!.copy(zoom=0.0))).validated() }
    }
    @Test fun changingFilmToSingleCardsKeepsTheResolvedLooks() {
        val next=com.polar.app.core.edit.ProjectEdits.selectStyle(sample(),TemplateStyle.POLAROID)
        assertEquals("cool",com.polar.app.core.look.LookResolver.resolve(next,1).preset)
        assertEquals("sepia",com.polar.app.core.look.LookResolver.resolve(next,5).preset)
    }

    @Test fun roundtripRetainsProjectCardAndIndividualPhotoFilters() {
        val json = javaClass.classLoader!!.getResource("fixtures/photo_looks.polar")!!.readText()
        val saved = PolarJson.format.parseToJsonElement(PolarJson.encode(PolarJson.decode(json))).jsonObject
        assertTrue("El look general no debe desaparecer al guardar", saved["settings"]!!.jsonObject.containsKey("photoLook"))
        assertEquals("bw", saved["settings"]!!.jsonObject["photoLook"]!!.jsonObject["preset"]!!.jsonPrimitive.content)
        assertEquals("sepia", saved["cardOverrides"]!!.jsonObject["1"]!!.jsonObject["photoLook"]!!.jsonObject["preset"]!!.jsonPrimitive.content)
        assertEquals("cool", saved["placements"]!!.jsonArray[1].jsonObject["photoLook"]!!.jsonObject["preset"]!!.jsonPrimitive.content)
    }
}
