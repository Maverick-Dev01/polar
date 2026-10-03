package com.polar.app.core.edit

import com.polar.app.model.*

object ProjectEdits {
    const val MAX_PHOTOS = 2000
    val LAYOUT_PRESETS = listOf(1, 2, 4, 6, 8, 9, 12, 16)

    // ---------- Texto ----------

    fun setText(p: PolarProject, role: TextRole, value: String, card: Int? = null): PolarProject =
        if (card == null) p.copy(settings = p.settings.withText(role, value))
        else p.updateOverride(card) { it.copy(texts = it.texts + (role.key to value)) }

    fun clearOwnText(p: PolarProject, card: Int, role: TextRole): PolarProject =
        p.updateOverride(card) { it.copy(texts = it.texts - role.key) }

    fun applyTextToAll(p: PolarProject, role: TextRole): PolarProject =
        p.mapOverrides { it.copy(texts = it.texts - role.key) }

    fun editAppearance(
        p: PolarProject, role: TextRole, card: Int?, change: (TextAppearance) -> TextAppearance
    ): PolarProject =
        if (card == null) p.copy(settings = p.settings.withTextStyle(role, change(p.settings.textStyle(role))))
        else p.updateOverride(card) { o ->
            val base = o.styles[role.key] ?: p.settings.textStyle(role)
            o.copy(styles = o.styles + (role.key to change(base)))
        }

    fun clearOwnAppearance(p: PolarProject, card: Int, role: TextRole): PolarProject =
        p.updateOverride(card) { it.copy(styles = it.styles - role.key) }

    fun applyAppearanceToAll(p: PolarProject, role: TextRole): PolarProject =
        p.mapOverrides { it.copy(styles = it.styles - role.key) }

    fun resetAppearance(p: PolarProject, role: TextRole): PolarProject =
        p.copy(settings = p.settings.withoutTextStyle(role))

    // ---------- Filtros ----------

    fun setAllLooks(p: PolarProject, look: PhotoLook): PolarProject =
        p.copy(settings = p.settings.copy(photoLook = look.canonical()), placements = p.placements.map { it?.copy(photoLook = null) })
            .mapOverrides { it.copy(photoLook = null) }

    fun setPageLooks(p: PolarProject, look: PhotoLook, pages: Set<Int>): PolarProject {
        val valid = pages.filter { it in 0 until p.pageCount }.toSet()
        var next = p
        for (page in valid) for (card in page * p.cardsPerPage until (page + 1) * p.cardsPerPage) {
            next = next.updateOverride(card) { it.copy(photoLook = look.canonical()) }
        }
        return next.copy(placements = next.placements.mapIndexed { slot, placement ->
            if (slot / p.settings.capacity in valid) placement?.copy(photoLook = null) else placement
        })
    }

    fun setPhotoLook(p: PolarProject, look: PhotoLook, slot: Int): PolarProject =
        editPlacement(p, slot) { it.copy(photoLook = look.canonical()) }

    // ---------- Fecha ----------

    fun setDateSource(p: PolarProject, source: DateSource, card: Int? = null): PolarProject =
        if (card == null) p.copy(settings = p.settings.copy(dateSource = source))
        else p.updateOverride(card) { it.copy(dateSource = source) }

    fun setChosenDate(p: PolarProject, epochMs: Long, card: Int? = null): PolarProject {
        val swift = SwiftDate.fromEpochMs(epochMs)
        return if (card == null) p.copy(settings = p.settings.copy(chosenDate = swift, dateSource = DateSource.CHOSEN))
        else p.updateOverride(card) { it.copy(chosenDate = swift, dateSource = DateSource.CHOSEN) }
    }

    fun setDateStyle(p: PolarProject, style: DateStyle): PolarProject =
        p.copy(settings = p.settings.copy(dateStyle = style))

    // ---------- Estilos rápidos ----------

    fun applyMood(p: PolarProject, mood: MoodPreset): PolarProject {
        var s = p.settings.copy(accentHex = mood.hex)
        for (role in TextRole.entries) {
            s = s.withTextStyle(role, s.textStyle(role).copy(fontName = mood.fontName, hex = ""))
        }
        return p.copy(settings = s)
    }

    fun applySuggestedPhrases(p: PolarProject, mood: MoodPreset): PolarProject {
        val s = p.settings.copy(title = mood.title, subtitle = mood.subtitle, caption = mood.caption)
        val keys = setOf(TextRole.TITLE.key, TextRole.SUBTITLE.key, TextRole.CAPTION.key)
        return p.copy(settings = s).mapOverrides { it.copy(texts = it.texts - keys) }
    }

    // ---------- Diseño y distribución ----------

    fun selectStyle(p: PolarProject, style: TemplateStyle): PolarProject {
        val oldPer = p.settings.style.photosPerCard
        val base = if (oldPer != style.photosPerCard && p.cardOverrides.values.any { it.photoLook != null }) {
            p.copy(placements = p.placements.mapIndexed { slot, placement ->
                placement?.copy(photoLook = com.polar.app.core.look.LookResolver.resolve(p, slot))
            })
        } else p
        val next = base.withStyle(style).copy(pageDesigns = emptyMap(), cardOverrides = base.cardOverrides.mapValues { it.value.copy(designStyle = null, designFormat = null) })
        val newPer = style.photosPerCard
        if (oldPer == newPer || p.cardOverrides.isEmpty()) return next
        val remapped = LinkedHashMap<String, CardOverride>()
        p.cardOverrides.entries
            .sortedBy { it.key.toInt() }
            .forEach { (key, value) ->
                val target = ((key.toInt() * oldPer) / newPer).toString()
                val existing = remapped[target]
                remapped[target] = (if (existing == null) value else existing.mergedWith(value)).copy(designStyle = null, designFormat = null)
            }
        return next.copy(cardOverrides = remapped)
    }

    fun setPageDesign(p: PolarProject, page: Int, style: TemplateStyle, format: CardFormat = p.settingsForPage(page).cardFormat): PolarProject {
        if (page !in 0 until p.pageCount || !p.compatibleStyle(style)) return p
        return p.copy(pageDesigns = p.pageDesigns + (page.toString() to PageDesign(style, format)),
            cardOverrides = p.cardOverrides.mapValues { (key, value) ->
                if (key.toInt() / p.cardsPerPage == page) value.copy(designStyle = null, designFormat = null) else value
            }.filterValues { !it.isEmpty })
    }
    fun setCardDesign(p: PolarProject, card: Int, style: TemplateStyle): PolarProject =
        if (!p.compatibleStyle(style)) p else p.updateOverride(card) { it.copy(designStyle = style) }

    fun setCardFormat(p: PolarProject, card: Int, format: CardFormat): PolarProject = p.updateOverride(card) { it.copy(designFormat = format) }

    /** Sólo vacía posiciones, nunca borra archivos originales. */
    fun clearSlots(p: PolarProject, slots: Set<Int>): PolarProject =
        p.copy(placements = p.placements.mapIndexed { i, value -> if (i in slots) null else value })

    fun copySlotsToNewPage(p: PolarProject, slots: Set<Int>): PolarProject {
        val expanded = if (p.settings.style.photosPerCard > 1) slots.flatMap { slot ->
            val first = p.firstSlotOfCard(p.cardOfSlot(slot)); (first until first + p.settings.style.photosPerCard).toList()
        }.toSet() else slots
        val selected = expanded.sorted().filter { it in p.placements.indices && (p.settings.style.photosPerCard > 1 || p.placements[it] != null) }
        if (selected.isEmpty() || p.normalized().placements.size + selected.size > MAX_PHOTOS + p.settings.capacity - 1 || p.placedCount + selected.count { p.placements[it] != null } > MAX_PHOTOS) return p
        val base = p.normalized()
        val first = base.placements.size
        val cap = p.settings.capacity
        val copies = selected.map { p.placements[it] }
        val padding = (cap - copies.size % cap) % cap
        var next = base.copy(placements = base.placements + copies + List(padding) { null })
        // Cada grupo conserva sus textos/filtros; en película se copia la tarjeta completa desde la UI.
        selected.forEachIndexed { i, slot ->
            val source = p.settingsForCard(p.cardOfSlot(slot))
            val own = (p.override(p.cardOfSlot(slot)) ?: CardOverride()).let {
                if (p.compatibleStyle(source.style)) it.copy(designStyle = source.style, designFormat = source.cardFormat) else it
            }
            if (!own.isEmpty) next = next.copy(cardOverrides = next.cardOverrides + (next.cardOfSlot(first + i).toString() to own))
        }
        return next
    }

    fun setGrid(p: PolarProject, columns: Int, rows: Int): PolarProject {
        if (p.settings.style == TemplateStyle.IMPORTED || columns !in 1..4 || rows !in 1..6) return p
        return p.copy(
            settings = p.settings.copy(columns = columns, rows = rows),
            pageDesigns = emptyMap(),
            placements = p.placements.dropLastWhile { it == null }
        ).normalized()
    }

    fun applyLayoutPreset(p: PolarProject, count: Int): PolarProject {
        val landscape = p.settings.orientation == PaperOrientation.LANDSCAPE
        val grid = when (count) {
            1 -> 1 to 1
            2 -> if (landscape) 2 to 1 else 1 to 2
            4 -> 2 to 2
            6 -> if (landscape) 3 to 2 else 2 to 3
            8 -> if (landscape) 4 to 2 else 2 to 4
            9 -> 3 to 3
            12 -> if (landscape) 4 to 3 else 3 to 4
            16 -> 4 to 4
            else -> return p
        }
        return setGrid(p, grid.first, grid.second)
    }

    fun updateSettings(p: PolarProject, change: (PrintSettings) -> PrintSettings): PolarProject =
        p.copy(settings = change(p.settings)).normalized()

    fun setCustomPaper(p: PolarProject, widthMM: Double, heightMM: Double): PolarProject {
        fun clamp(v: Double, fallback: Double) = if (v.isFinite()) v.coerceIn(80.0, 600.0) else fallback
        return updateSettings(p) {
            it.copy(paperSize = PaperSize.CUSTOM, customWidthMM = clamp(widthMM, 215.9), customHeightMM = clamp(heightMM, 279.4))
        }
    }

    // ---------- Fotos ----------

    fun addPhotos(p: PolarProject, assets: List<PhotoAsset>, fillFrom: Int?): PolarProject {
        val known = p.photos.map { it.path }.toSet()
        val room = (MAX_PHOTOS - p.photos.size).coerceAtLeast(0)
        val added = assets.filter { it.path !in known }.distinctBy { it.path }.take(room)
        if (added.isEmpty()) return p
        val placements = p.placements.toMutableList()
        if (fillFrom != null) {
            var slot = fillFrom.coerceAtLeast(0)
            for (asset in added) {
                while (slot < placements.size && placements[slot] != null) slot++
                while (placements.size <= slot) placements.add(null)
                placements[slot] = PhotoPlacement(assetID = asset.id)
                slot++
            }
        }
        return p.copy(photos = p.photos + added, placements = placements).normalized()
    }

    fun fillAll(p: PolarProject): PolarProject =
        if (p.photos.isEmpty()) p
        else p.copy(placements = p.photos.filter { !it.isBackground }.map { PhotoPlacement(assetID = it.id) }).normalized().let { next ->
            next.copy(pageDesigns = next.pageDesigns.filterKeys { key -> key.toInt() < next.pageCount })
        }

    fun assign(p: PolarProject, slot: Int, assetId: String): PolarProject {
        if (slot < 0 || p.photos.none { it.id == assetId }) return p
        val list = p.placements.toMutableList()
        while (list.size <= slot) list.add(null)
        list[slot] = PhotoPlacement(assetID = assetId)
        return p.copy(placements = list).normalized()
    }

    fun clearSlot(p: PolarProject, slot: Int): PolarProject {
        if (slot !in p.placements.indices) return p
        return p.copy(placements = p.placements.toMutableList().also { it[slot] = null })
    }

    fun editPlacement(p: PolarProject, slot: Int, change: (PhotoPlacement) -> PhotoPlacement): PolarProject {
        val current = p.placements.getOrNull(slot) ?: return p
        val next = change(current).let {
            it.copy(
                zoom = if (it.zoom.isFinite() && it.zoom > 0) it.zoom.coerceAtMost(4.0) else 1.0,
                offsetX = it.offsetX.coerceIn(-1.0, 1.0),
                offsetY = it.offsetY.coerceIn(-1.0, 1.0),
                quarterTurns = ((it.quarterTurns % 4) + 4) % 4
            )
        }
        return p.copy(placements = p.placements.toMutableList().also { it[slot] = next })
    }

    // ---------- Hojas ----------

    fun clearPage(p: PolarProject, page: Int): PolarProject {
        if (page !in 0 until p.normalized().pageCount) return p
        val cap = p.settings.capacity
        val list = p.normalized().placements.toMutableList()
        for (i in page * cap until minOf(list.size, (page + 1) * cap)) list[i] = null
        return p.copy(placements = list)
    }

    fun addPage(p: PolarProject): PolarProject {
        val cap = p.settings.capacity
        val base = p.normalized()
        if (base.placements.size + cap > MAX_PHOTOS + cap - 1) return p
        return base.copy(placements = base.placements + List(cap) { null })
    }

    fun removePage(p: PolarProject, page: Int): PolarProject {
        val base = p.normalized()
        if (page !in 0 until base.pageCount) return p
        if (base.pageCount <= 1) return clearPage(base, 0)
        val cap = base.settings.capacity
        val list = base.placements.toMutableList()
        list.subList(page * cap, minOf(list.size, (page + 1) * cap)).clear()
        val perPage = base.cardsPerPage
        val first = page * perPage
        val shifted = base.cardOverrides.mapNotNull { (key, value) ->
            val card = key.toInt()
            when {
                card < first -> key to value
                card < first + perPage -> null
                else -> (card - perPage).toString() to value
            }
        }.toMap()
        val designs = base.pageDesigns.mapNotNull { (key, value) ->
            val n = key.toInt()
            when { n == page -> null; n > page -> (n - 1).toString() to value; else -> key to value }
        }.toMap()
        return base.copy(placements = list, cardOverrides = shifted, pageDesigns = designs).normalized()
    }

    // ---------- Plantilla importada ----------

    fun editTemplateRegion(p: PolarProject, index: Int, change: (TemplateRegion) -> TemplateRegion): PolarProject {
        val t = p.settings.importedTemplate ?: return p
        if (index !in t.regions.indices) return p
        val regions = t.regions.toMutableList().also { it[index] = change(it[index]).clamped() }
        return p.copy(settings = p.settings.copy(importedTemplate = t.copy(regions = regions)))
    }

    fun addTemplateRegion(p: PolarProject): PolarProject {
        val t = p.settings.importedTemplate ?: return p
        if (t.regions.size >= 64) return p
        val region = TemplateRegion(x = 0.3, y = 0.3, width = 0.4, height = 0.3)
        return p.copy(settings = p.settings.copy(importedTemplate = t.copy(regions = t.regions + region))).normalized()
    }

    fun removeTemplateRegion(p: PolarProject, index: Int): PolarProject {
        val t = p.settings.importedTemplate ?: return p
        if (t.regions.size <= 1 || index !in t.regions.indices) return p
        return p.copy(settings = p.settings.copy(importedTemplate = t.copy(regions = t.regions.filterIndexed { i, _ -> i != index })))
    }
}

private fun PolarProject.updateOverride(card: Int, change: (CardOverride) -> CardOverride): PolarProject {
    val key = card.toString()
    val next = change(cardOverrides[key] ?: CardOverride())
    val map = cardOverrides.toMutableMap()
    if (next.isEmpty) map.remove(key) else map[key] = next
    return copy(cardOverrides = map)
}

/** Primero gana por rol; fecha sólo si aún no hay una. */
private fun CardOverride.mergedWith(other: CardOverride): CardOverride = CardOverride(
    texts = texts + other.texts.filterKeys { it !in texts },
    styles = styles + other.styles.filterKeys { it !in styles },
    dateSource = dateSource ?: other.dateSource,
    chosenDate = chosenDate ?: other.chosenDate,
    photoLook = photoLook ?: other.photoLook
)

private fun PolarProject.mapOverrides(change: (CardOverride) -> CardOverride): PolarProject =
    copy(cardOverrides = cardOverrides.mapValues { change(it.value) }.filterValues { !it.isEmpty })
