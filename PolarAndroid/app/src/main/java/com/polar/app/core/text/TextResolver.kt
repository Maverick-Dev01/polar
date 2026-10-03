package com.polar.app.core.text

import com.polar.app.model.*
import java.util.TimeZone

/** Texto y estilo efectivos de cada tarjeta: el propio si existe, si no el general. */
object TextResolver {

    fun text(project: PolarProject, card: Int, role: TextRole, zone: TimeZone = TimeZone.getDefault()): String {
        if (role == TextRole.DATE) return dateText(project, card, zone)
        return project.override(card)?.texts?.get(role.key) ?: project.settings.text(role)
    }

    fun hasOwnText(project: PolarProject, card: Int, role: TextRole): Boolean =
        project.override(card)?.texts?.containsKey(role.key) == true

    fun appearance(project: PolarProject, card: Int, role: TextRole): TextAppearance =
        project.override(card)?.styles?.get(role.key) ?: project.settings.textStyle(role)

    fun hasOwnAppearance(project: PolarProject, card: Int, role: TextRole): Boolean =
        project.override(card)?.styles?.containsKey(role.key) == true

    fun dateSource(project: PolarProject, card: Int): DateSource =
        project.override(card)?.dateSource ?: project.settings.dateSource

    fun ownTextCount(project: PolarProject, role: TextRole): Int =
        project.cardOverrides.values.count { it.texts.containsKey(role.key) }

    private fun dateText(project: PolarProject, card: Int, zone: TimeZone): String {
        val epochMs = when (dateSource(project, card)) {
            DateSource.NONE -> return ""
            DateSource.PHOTO -> {
                val placement = project.placements.getOrNull(project.firstSlotOfCard(card)) ?: return ""
                project.asset(placement)?.takenAtEpochMs ?: return ""
            }
            DateSource.CHOSEN -> {
                val swift = project.override(card)?.chosenDate ?: project.settings.chosenDate ?: return ""
                SwiftDate.toEpochMs(swift)
            }
        }
        return DateText.format(epochMs, project.settings.dateStyle, zone)
    }
}
