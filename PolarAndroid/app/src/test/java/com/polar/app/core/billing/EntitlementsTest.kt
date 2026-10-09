package com.polar.app.core.billing

import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsTest {
    @Test fun allUnlockedIsProAndHasEveryFeature() {
        val entitlements: Entitlements = AllUnlocked
        assertTrue(entitlements.isPro())
        ProFeature.values().forEach { assertTrue("$it debe estar desbloqueada", entitlements.hasFeature(it)) }
    }

    @Test fun featureListMatchesProposal() {
        assertTrue(ProFeature.values().map { it.name }.toSet() == setOf("SEASONAL_DESIGNS", "REMOVE_BACKGROUND", "UNLIMITED_MOLDS", "MAX_QUALITY"))
    }
}
