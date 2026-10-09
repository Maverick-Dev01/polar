package com.polar.app.ui.editor.panels

import androidx.annotation.StringRes
import com.polar.app.R
import com.polar.app.core.text.DateText
import com.polar.app.core.text.TextResolver
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import java.util.TimeZone

/** Secciones del panel Texto, en el orden en que se muestran. */
enum class TextPanelSection { SONG, SCOPE, ROLES, TEXT, DATE, VISIBLE, FONT, COLOR, SIZE, MORE }

data class TextPanelState(
    val roles: List<TextRole>,
    val role: TextRole,
    val cardScope: Boolean,
    val cardNumber: Int?,
    val hasSelection: Boolean,
    val text: String,
    val generalText: String,
    val hasOwnText: Boolean,
    val ownCount: Int,
    val appearance: TextAppearance,
    val hasOwnAppearance: Boolean,
    val dateSource: DateSource,
    val dateStyle: DateStyle,
    val dateSamples: List<String>,
    val accentHex: String,
    val showSongUrl: Boolean,
    val songUrl: String,
    val isMusical: Boolean,
    val songText: String,
    val artistText: String,
    val sections: List<TextPanelSection>,
    @param:StringRes val emptyReason: Int?
)

private const val SAMPLE_DATE_MS = 1_771_070_400_000L // 14 feb 2026, para mostrar los formatos

fun textPanelState(state: EditorUiState, zone: TimeZone = TimeZone.getDefault()): TextPanelState {
    val p = state.project
    val s = state.selectedCard?.let { p.settingsForCard(it) } ?: p.settingsForPage(state.page)
    val roles = s.style.textRoles
    val role = state.textRole.takeIf { it in roles } ?: roles.firstOrNull() ?: TextRole.TITLE
    val card = state.editCard
    val musical = s.style.category == DesignCategory.MUSIC
    fun valueOf(r: TextRole) = if (card != null) TextResolver.text(p, card, r, zone) else s.text(r)
    val sections = when {
        roles.isEmpty() -> emptyList()
        else -> buildList {
            if (musical) add(TextPanelSection.SONG)
            add(TextPanelSection.SCOPE)
            add(TextPanelSection.ROLES)
            if (role == TextRole.DATE) add(TextPanelSection.DATE) else if (!musical) add(TextPanelSection.TEXT)
            addAll(listOf(TextPanelSection.VISIBLE, TextPanelSection.FONT, TextPanelSection.COLOR, TextPanelSection.SIZE, TextPanelSection.MORE))
        }
    }
    return TextPanelState(
        roles = roles,
        role = role,
        cardScope = card != null,
        cardNumber = state.selectedCardNumber,
        hasSelection = state.selectedSlot != null,
        text = if (card != null) TextResolver.text(p, card, role, zone) else s.text(role),
        generalText = s.text(role),
        hasOwnText = card != null && TextResolver.hasOwnText(p, card, role),
        ownCount = TextResolver.ownTextCount(p, role),
        appearance = if (card != null) TextResolver.appearance(p, card, role) else s.textStyle(role),
        hasOwnAppearance = card != null && TextResolver.hasOwnAppearance(p, card, role),
        dateSource = if (card != null) TextResolver.dateSource(p, card) else s.dateSource,
        dateStyle = s.dateStyle,
        dateSamples = DateStyle.entries.map { DateText.format(SAMPLE_DATE_MS, it, zone) },
        accentHex = s.accentHex,
        showSongUrl = s.style == TemplateStyle.SPOTIFY,
        songUrl = s.songURL,
        isMusical = musical,
        songText = valueOf(TextRole.SONG),
        artistText = valueOf(TextRole.ARTIST),
        sections = sections,
        emptyReason = when {
            roles.isNotEmpty() -> null
            s.style == TemplateStyle.IMPORTED -> R.string.text_empty_imported
            else -> R.string.text_empty_photo_only
        }
    )
}
