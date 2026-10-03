package com.polar.app.core.look

import com.polar.app.model.*

object LookResolver {
    fun resolve(project: PolarProject, slot: Int): PhotoLook =
        (project.placements.getOrNull(slot)?.photoLook ?: project.override(project.cardOfSlot(slot))?.photoLook ?: project.settings.photoLook ?: PhotoLook()).canonical()
}
