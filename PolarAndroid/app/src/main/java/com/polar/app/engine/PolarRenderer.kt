package com.polar.app.engine

import android.graphics.*
import android.text.TextPaint
import com.polar.app.model.*
import com.polar.app.core.look.LookResolver
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object FontNames { const val SYSTEM = ".System" }

data class PolarRect(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
) {
    val width: Double get() = max(0.0, right - left)
    val height: Double get() = max(0.0, bottom - top)
    val minX: Double get() = left
    val maxX: Double get() = right
    val minY: Double get() = top
    val maxY: Double get() = bottom
    val midX: Double get() = (left + right) / 2.0
    val midY: Double get() = (top + bottom) / 2.0

    fun insetBy(dx: Double, dy: Double): PolarRect =
        PolarRect(left + dx, top + dy, right - dx, bottom - dy)

    fun offsetBy(dx: Double, dy: Double): PolarRect =
        PolarRect(left + dx, top + dy, right + dx, bottom + dy)

    fun toAndroidRectF(scale: Float = 1f): RectF =
        RectF(
            (left * scale).toFloat(),
            (top * scale).toFloat(),
            (right * scale).toFloat(),
            (bottom * scale).toFloat()
        )
}

object PolarRenderer {

    fun paperRect(settings: PrintSettings): PolarRect {
        val size = settings.paperSizePoints
        return PolarRect(0.0, 0.0, size.width, size.height)
    }

    fun templateRect(settings: PrintSettings): PolarRect {
        val paper = paperRect(settings)
        val available = paper.insetBy(settings.margin, settings.margin)
        val template = settings.importedTemplate ?: return available
        val aspect = template.pixelWidth.toDouble() / max(1, template.pixelHeight).toDouble()
        val width = min(available.width, available.height * aspect)
        val height = width / aspect
        return PolarRect(
            available.midX - width / 2.0,
            available.midY - height / 2.0,
            available.midX + width / 2.0,
            available.midY + height / 2.0
        )
    }

    fun calculateCardRects(settings: PrintSettings): List<PolarRect> {
        val paper = paperRect(settings)
        if (settings.style == TemplateStyle.IMPORTED && settings.importedTemplate != null) {
            val frame = templateRect(settings)
            return settings.importedTemplate.regions.map { r ->
                PolarRect(
                    frame.minX + r.x * frame.width,
                    frame.minY + r.y * frame.height,
                    frame.minX + (r.x + r.width) * frame.width,
                    frame.minY + (r.y + r.height) * frame.height
                )
            }
        }

        val columns = settings.columns
        val rows = settings.rows
        val margin = settings.margin
        val gap = settings.gap
        if (columns !in 1..4 || rows !in 1..6 || !margin.isFinite() || !gap.isFinite()) return emptyList()

        val cellWidth = (paper.width - margin * 2.0 - gap * (columns - 1).toDouble()) / columns.toDouble()
        val cellHeight = (paper.height - margin * 2.0 - gap * (rows - 1).toDouble()) / rows.toDouble()

        val aspect = settings.cardAspect
        val width = if (aspect != null) min(cellWidth, cellHeight * aspect) else cellWidth
        val height = if (aspect != null) width / aspect else cellHeight
        if (width <= 0 || height <= 0) return emptyList()

        val output = mutableListOf<PolarRect>()
        for (i in 0 until (columns * rows)) {
            val col = i % columns
            val row = i / columns
            val x = margin + col.toDouble() * (cellWidth + gap) + (cellWidth - width) / 2.0
            val y = margin + row.toDouble() * (cellHeight + gap) + (cellHeight - height) / 2.0
            output.add(PolarRect(x, y, x + width, y + height))
        }
        return output
    }

    fun cardRects(project: PolarProject, page: Int): List<PolarRect> {
        val pageSettings = project.settingsForPage(page)
        if (pageSettings.style == TemplateStyle.IMPORTED) return calculateCardRects(pageSettings)
        val cells = calculateCardRects(pageSettings.copy(cardFormat = CardFormat.FILL))
        return cells.mapIndexed { index, cell ->
            val aspect = project.settingsForCard(page * project.cardsPerPage + index).cardAspect
            val w = aspect?.let { min(cell.width, cell.height * it) } ?: cell.width
            val h = aspect?.let { w / it } ?: cell.height
            PolarRect(cell.midX-w/2, cell.midY-h/2, cell.midX+w/2, cell.midY+h/2)
        }
    }
    fun photoRects(project: PolarProject, page: Int): List<PolarRect> {
        val cards = cardRects(project, page)
        if (project.settings.style == TemplateStyle.IMPORTED) return cards
        return cards.flatMapIndexed { index, rect ->
            val s = project.settingsForCard(page * project.cardsPerPage + index)
            calculatePhotoRects(rect, s.style, s)
        }
    }

    fun calculatePhotoRects(card: PolarRect, style: TemplateStyle, settings: PrintSettings): List<PolarRect> {
        fun r(x: Double, y: Double, w: Double, h: Double): PolarRect =
            PolarRect(
                card.minX + x * card.width,
                card.minY + y * card.height,
                card.minX + (x + w) * card.width,
                card.minY + (y + h) * card.height
            )

        return when (style) {
            TemplateStyle.POLAROID -> listOf(r(0.065, 0.045, 0.87, 0.735))
            TemplateStyle.MINI -> listOf(r(0.075, 0.045, 0.85, 0.75))
            TemplateStyle.SPOTIFY -> listOf(r(0.065, 0.045, 0.87, 0.61))
            TemplateStyle.PLAYER_RED -> listOf(r(0.065, 0.04, 0.87, 0.56))
            TemplateStyle.PLAYER_GRAY -> listOf(r(0.045, 0.085, 0.47, 0.81))
            TemplateStyle.TICKET -> listOf(r(0.055, 0.09, 0.65, 0.65))
            TemplateStyle.FILM_VERTICAL -> (0 until 5).map { r(0.14, 0.02 + it * 0.196, 0.72, 0.18) }
            TemplateStyle.FILM_HORIZONTAL -> (0 until 5).map { r(0.014 + it * 0.195, 0.16, 0.18, 0.68) }
            TemplateStyle.CALENDAR -> listOf(r(0.06, 0.04, 0.88, 0.585))
            TemplateStyle.INSTAGRAM -> listOf(r(0.045, 0.19, 0.91, 0.59))
            TemplateStyle.CUSTOM -> listOf(r(0.055, 0.055, 0.89, 0.70))
            TemplateStyle.BORDERLESS, TemplateStyle.IMPORTED -> listOf(card)
            TemplateStyle.SQUARE -> listOf(r(0.06, 0.06, 0.88, 0.76))
            TemplateStyle.POSTCARD -> listOf(r(0.035, 0.055, 0.61, 0.89))
            TemplateStyle.BOTANICAL -> listOf(r(0.09, 0.07, 0.82, 0.65))
            TemplateStyle.CELEBRATION -> listOf(r(0.07, 0.13, 0.86, 0.56))
            TemplateStyle.PETS -> listOf(r(0.065, 0.07, 0.87, 0.65))
            TemplateStyle.HEART -> listOf(r(0.065, 0.08, 0.87, 0.65))
            TemplateStyle.EDITORIAL -> listOf(r(0.065, 0.19, 0.87, 0.55))
        }
    }

    fun drawPage(
        canvas: Canvas,
        project: PolarProject,
        page: Int,
        isPreview: Boolean,
        scale: Float = 1f,
        bitmapProvider: (PhotoAsset) -> Bitmap? = { null },
        templateBitmap: Bitmap? = null,
        fonts: FontProvider = SystemFontProvider,
        pdfPhoto: ((Bitmap) -> Unit)? = null
    ) {
        // Una transición puede medir temporalmente la hoja a cero: no rasterizar texto/emoji a escala infinita.
        if (!scale.isFinite() || scale <= 0f) return
        val s = project.settingsForPage(page)
        val paper = paperRect(s)

        // Draw paper background
        val whitePaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(paper.toAndroidRectF(scale), whitePaint)

        if (s.style == TemplateStyle.IMPORTED && s.importedTemplate != null) {
            drawImported(canvas, project, page, isPreview, scale, bitmapProvider, templateBitmap, fonts, pdfPhoto)
            return
        }

        val cards = cardRects(project, page)
        for ((index, card) in cards.withIndex()) {
            val firstSlot = page * s.capacity + index * s.style.photosPerCard
            val cardIndex = page * project.cardsPerPage + index
            val hasAssigned = (firstSlot until min(project.placements.size, firstSlot + s.style.photosPerCard))
                .any { project.placements[it] != null }

            if (!isPreview && !hasAssigned) continue

            drawCard(canvas, card, project, firstSlot, cardIndex, isPreview, scale, bitmapProvider, fonts, pdfPhoto)
            if (s.cutGuides) {
                drawGuides(canvas, card, s.cutStyle, paper, cards, scale)
            }
        }
    }

    /** Una sola tarjeta con marcadores de posición, para las miniaturas del catálogo. */
    fun drawCardPreview(canvas: Canvas, project: PolarProject, card: PolarRect, scale: Float, fonts: FontProvider = SystemFontProvider) {
        if (!scale.isFinite() || scale <= 0f) return
        drawCard(canvas, card, project, firstSlot = 0, cardIndex = 0, isPreview = true, scale = scale, bitmapProvider = { null }, fonts = fonts)
    }

    fun cardPreview(project: PolarProject, slot: Int, maxSide: Int, bitmapProvider: (PhotoAsset) -> Bitmap?, template: Bitmap? = null, fonts: FontProvider = SystemFontProvider): Bitmap {
        val card = cardRects(project, slot / project.settings.capacity)[(slot % project.settings.capacity) / project.settings.style.photosPerCard]
        val scale = (maxSide / max(card.width, card.height)).toFloat()
        val result = Bitmap.createBitmap(ceil(card.width*scale).toInt().coerceAtLeast(1), ceil(card.height*scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.translate((-card.left*scale).toFloat(), (-card.top*scale).toFloat())
        drawPage(canvas, project, slot/project.settings.capacity, false, scale, bitmapProvider, template, fonts)
        return result
    }

    fun photoPreview(project: PolarProject, slot: Int, maxSide: Int, bitmapProvider: (PhotoAsset) -> Bitmap?, template: Bitmap? = null, fonts: FontProvider = SystemFontProvider): Bitmap {
        val photo = photoRects(project, slot / project.settings.capacity)[slot % project.settings.capacity]
        val scale = (maxSide / max(photo.width, photo.height)).toFloat()
        val result = Bitmap.createBitmap(ceil(photo.width*scale).toInt().coerceAtLeast(1), ceil(photo.height*scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.translate((-photo.left*scale).toFloat(), (-photo.top*scale).toFloat())
        drawPage(canvas, project, slot/project.settings.capacity, false, scale, bitmapProvider, template, fonts)
        return result
    }

    /** Foto fuera del hueco para el encuadre: comparte transformación y matriz con la impresión. */
    fun cropOverflow(project: PolarProject, slot: Int, maxSide: Int, provider: (PhotoAsset)->Bitmap?): Bitmap {
        val s=project.settingsForCard(project.cardOfSlot(slot))
        val card=cardRects(project, slot / project.settings.capacity)[slot%s.capacity/s.style.photosPerCard]
        val rect=calculatePhotoRects(card,s.style,s)[slot%s.style.photosPerCard]
        val scale=(maxSide/max(card.width,card.height)).toFloat()
        val result=Bitmap.createBitmap(ceil(card.width*1.36*scale).toInt().coerceAtLeast(1),ceil(card.height*1.16*scale).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(result)
        canvas.translate(((-card.left+card.width*.18)*scale).toFloat(),((-card.top+card.height*.08)*scale).toFloat())
        val placement=project.placements.getOrNull(slot)
        drawPhoto(canvas,project.asset(placement),placement,rect,parseColor(s.accentHex),Color.WHITE,true,scale,provider,LookResolver.resolve(project,slot),project.cardOfSlot(slot),card,clipPhoto=false)
        return result
    }

    private fun drawCard(
        canvas: Canvas,
        card: PolarRect,
        project: PolarProject,
        firstSlot: Int,
        cardIndex: Int,
        isPreview: Boolean,
        scale: Float,
        bitmapProvider: (PhotoAsset) -> Bitmap?,
        fonts: FontProvider,
        pdfPhoto: ((Bitmap) -> Unit)? = null
    ) {
        val s = project.settingsForCard(cardIndex)
        val style = s.style
        val accent = parseColor(s.accentHex)

        val bgColor = when (style) {
            TemplateStyle.FILM_VERTICAL, TemplateStyle.FILM_HORIZONTAL -> Color.rgb(14, 14, 14)
            TemplateStyle.PLAYER_RED -> accent
            TemplateStyle.PLAYER_GRAY -> Color.rgb(133, 133, 133)
            TemplateStyle.TICKET -> Color.rgb(227, 201, 166)
            TemplateStyle.BOTANICAL, TemplateStyle.PETS, TemplateStyle.EDITORIAL -> Color.rgb(250, 247, 237)
            TemplateStyle.CELEBRATION, TemplateStyle.HEART -> blendColor(accent, Color.WHITE, 0.94f)
            else -> Color.WHITE
        }

        val radius = if (style == TemplateStyle.PLAYER_GRAY || style == TemplateStyle.PLAYER_RED) {
            (min(card.width, card.height) * 0.08 * scale).toFloat()
        } else 0f

        val bgPaint = Paint().apply {
            color = bgColor
            isAntiAlias = true
        }
        val cardRectF = card.toAndroidRectF(scale)
        if (radius > 0) {
            canvas.drawRoundRect(cardRectF, radius, radius, bgPaint)
        } else {
            canvas.drawRect(cardRectF, bgPaint)
        }

        // Draw card border
        if (bgColor == Color.WHITE && (isPreview || s.drawBorders)) {
            val borderPaint = Paint().apply {
                color = Color.rgb(199, 199, 199)
                this.style = Paint.Style.STROKE
                strokeWidth = max(1f, (0.45 * scale).toFloat())
                isAntiAlias = true
            }
            canvas.drawRect(cardRectF, borderPaint)
        }

        if (style == TemplateStyle.TICKET) {
            val stub = PolarRect(card.minX + 0.745 * card.width, card.minY, card.maxX, card.maxY)
            val stubPaint = Paint().apply { color = accent; isAntiAlias = true }
            canvas.drawRect(stub.toAndroidRectF(scale), stubPaint)
        }

        if (style == TemplateStyle.CUSTOM) {
            val strokePaint = Paint().apply {
                color = accent
                this.style = Paint.Style.STROKE
                strokeWidth = (3.0 * scale).toFloat()
                isAntiAlias = true
            }
            val insetRect = card.insetBy(2.0, 2.0).toAndroidRectF(scale)
            canvas.drawRect(insetRect, strokePaint)
        }

        // Draw photo slots
        val photoSlots = calculatePhotoRects(card, style, s)
        for ((subslot, rect) in photoSlots.withIndex()) {
            val slot = firstSlot + subslot
            val placement = project.placements.getOrNull(slot)
            val photo = project.asset(placement)
            val rounded = s.roundedPhotos || style == TemplateStyle.PLAYER_GRAY ||
                    style == TemplateStyle.PLAYER_RED || style == TemplateStyle.PETS
            val cornerRad = if (rounded) (min(rect.width, rect.height) * 0.045 * scale).toFloat() else 0f

            canvas.save()
            if (style == TemplateStyle.HEART) {
                val path = heartPath(rect, scale)
                canvas.clipPath(path)
            } else if (cornerRad > 0) {
                val path = Path().apply {
                    addRoundRect(rect.toAndroidRectF(scale), cornerRad, cornerRad, Path.Direction.CW)
                }
                canvas.clipPath(path)
            } else {
                canvas.clipRect(rect.toAndroidRectF(scale))
            }

            drawPhoto(canvas, photo, placement, rect, accent, bgColor, isPreview, scale, bitmapProvider, LookResolver.resolve(project, slot), cardIndex, card, pdfPhoto = pdfPhoto, backgroundPhoto = placement?.background?.imageID?.let { id -> project.photos.firstOrNull { it.id == id } })
            canvas.restore()
        }

        // Draw Card Texts & Details
        drawCardElements(canvas, card, project, firstSlot, cardIndex, photoSlots, scale, accent, fonts)
    }

    private fun drawCardElements(
        canvas: Canvas,
        card: PolarRect,
        project: PolarProject,
        firstSlot: Int,
        cardIndex: Int,
        photoSlots: List<PolarRect>,
        scale: Float,
        accent: Int,
        fonts: FontProvider
    ) {
        val s = project.settings
        val style = s.style
        val fontSize = card.width * 0.06

        fun r(x: Double, y: Double, w: Double, h: Double): PolarRect =
            PolarRect(
                card.minX + x * card.width,
                card.minY + y * card.height,
                card.minX + (x + w) * card.width,
                card.minY + (y + h) * card.height
            )

        when (style) {
            TemplateStyle.SPOTIFY -> {
                val hasQR = s.songURL.isNotBlank()
                drawPlayer(canvas, r(0.07, 0.826, if (hasQR) 0.65 else 0.86, 0.145), Color.BLACK, scale, false)
                if (hasQR) {
                    val side = min(card.width * 0.21, card.height * 0.15)
                    val qrRect = PolarRect(card.maxX - card.width * 0.035 - side, card.maxY - card.height * 0.03 - side, card.maxX - card.width * 0.035, card.maxY - card.height * 0.03)
                    drawQR(canvas, s.songURL, qrRect, scale)
                }
            }
            TemplateStyle.PLAYER_RED -> {
                drawPlayer(canvas, r(0.075, 0.76, 0.85, 0.195), Color.WHITE, scale, true)
            }
            TemplateStyle.PLAYER_GRAY -> {
                drawPlayer(canvas, r(0.56, 0.44, 0.38, 0.42), Color.WHITE, scale, true)
            }
            TemplateStyle.TICKET -> {
                drawTicketNotches(canvas, card, scale)
            }
            TemplateStyle.FILM_VERTICAL, TemplateStyle.FILM_HORIZONTAL -> {
                drawPerforations(canvas, card, style == TemplateStyle.FILM_VERTICAL, scale)
            }
            TemplateStyle.CALENDAR -> {
                drawCalendar(canvas, card, (firstSlot % 12) + 1, s, accent, scale)
            }
            TemplateStyle.POSTCARD -> {
                val linePaint = Paint().apply { color = accent; strokeWidth = (0.6 * scale).toFloat(); isAntiAlias = true }
                canvas.drawLine(
                    (card.minX + card.width * 0.675).toFloat() * scale,
                    (card.minY + card.height * 0.12).toFloat() * scale,
                    (card.minX + card.width * 0.675).toFloat() * scale,
                    (card.minY + card.height * 0.88).toFloat() * scale,
                    linePaint
                )
            }
            TemplateStyle.HEART -> {
                val strokePaint = Paint().apply {
                    color = accent
                    this.style = Paint.Style.STROKE
                    strokeWidth = (0.8 * scale).toFloat()
                    isAntiAlias = true
                }
                canvas.drawPath(heartPath(photoSlots[0], scale), strokePaint)
            }
            else -> {}
        }

        for (item in CardTextLayout.items(project, card, cardIndex, photoSlots, accent)) {
            drawText(
                canvas = canvas, value = item.text, rect = item.rect, sizePt = item.sizePt,
                defaultColor = item.defaultColor, defaultBold = item.defaultBold,
                align = when (item.align) { TextAlign.LEFT -> Paint.Align.LEFT; TextAlign.CENTER -> Paint.Align.CENTER; TextAlign.RIGHT -> Paint.Align.RIGHT },
                appearance = item.appearance, scale = scale, fonts = fonts
            )
        }
    }

    private fun drawPhoto(
        canvas: Canvas,
        photo: PhotoAsset?,
        placement: PhotoPlacement?,
        rect: PolarRect,
        accent: Int,
        bg: Int,
        isPreview: Boolean,
        scale: Float,
        bitmapProvider: (PhotoAsset) -> Bitmap?,
        look: PhotoLook,
        cardIndex: Int,
        card: PolarRect,
        clipPhoto: Boolean = true,
        pdfPhoto: ((Bitmap) -> Unit)? = null,
        backgroundPhoto: PhotoAsset? = null
    ) {
        if (photo == null || placement == null) {
            if (isPreview) {
                drawPlaceholder(canvas, rect, accent, scale)
            } else {
                val p = Paint().apply { color = Color.WHITE }
                canvas.drawRect(rect.toAndroidRectF(scale), p)
            }
            return
        }

        val bitmap = bitmapProvider(photo)
        if (bitmap == null) {
            if (isPreview) drawPlaceholder(canvas, rect, accent, scale)
            return
        }

        val fit = PhotoFit.compute(rect.width, rect.height, bitmap.width, bitmap.height, placement)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(PhotoFilters.matrix(look).map { it.toFloat() }.toFloatArray()))
        }
        val options = placement.background
        val mask = options?.let { photo.maskPath?.let { path -> bitmapProvider(photo.copy(path = path)) } }
        if (options != null && mask == null && !isPreview) throw java.io.IOException("No se pudo leer la máscara del fondo. Vuelve a quitar el fondo de esta foto.")
        val background = backgroundPhoto?.let(bitmapProvider)
        fun draw(target: Canvas, originX: Double, originY: Double, drawScale: Float = scale) {
            if (options != null && mask != null) {
                PhotoCompositor.draw(target, RectF((originX*drawScale).toFloat(), (originY*drawScale).toFloat(), ((originX+rect.width)*drawScale).toFloat(), ((originY+rect.height)*drawScale).toFloat()), bitmap, mask, background, options, fit, drawScale, paint)
                return
            }
            target.save()
            target.translate(((originX + fit.centerX) * drawScale).toFloat(), ((originY + fit.centerY) * drawScale).toFloat())
            target.rotate(fit.degrees)
            target.drawBitmap(bitmap, null, RectF((-fit.width*drawScale/2).toFloat(),(-fit.height*drawScale/2).toFloat(),(fit.width*drawScale/2).toFloat(),(fit.height*drawScale/2).toFloat()), paint)
            target.restore()
        }
        canvas.save()
        if(clipPhoto) canvas.clipRect(rect.toAndroidRectF(scale)) // También los moldes importados recortan exactamente su hueco.
        if (clipPhoto && (options != null || pdfPhoto != null || PhotoFilters.grainStrength(look) > 0 || (!isPreview && scale <= 1.001f && !look.isNeutral))) {
            val rasterScale=if(!isPreview && scale<=1.001f) 300f/72f else scale
            val layer = Bitmap.createBitmap(ceil(rect.width*rasterScale).toInt().coerceAtLeast(1), ceil(rect.height*rasterScale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            try {
                val layerCanvas = Canvas(layer)
                if (pdfPhoto != null) layerCanvas.scale((layer.width / (rect.width * rasterScale)).toFloat(), (layer.height / (rect.height * rasterScale)).toFloat())
                draw(layerCanvas, 0.0, 0.0,rasterScale)
                PhotoFilters.grain(layer, look, PhotoFilters.seed(photo.id, cardIndex), rect.left-card.left, rect.top-card.top, rect.width/layer.width, rect.height/layer.height)
                pdfPhoto?.invoke(layer)
                canvas.drawBitmap(layer,null,rect.toAndroidRectF(scale),Paint(Paint.FILTER_BITMAP_FLAG))
            } finally { layer.recycle() }
        } else draw(canvas, rect.left, rect.top)
        canvas.restore()
    }

    private fun drawPlaceholder(canvas: Canvas, rect: PolarRect, accent: Int, scale: Float) {
        val r = rect.toAndroidRectF(scale)
        val gradient = LinearGradient(
            r.centerX(), r.top, r.centerX(), r.bottom,
            Color.rgb(232, 224, 209),
            blendColor(accent, Color.WHITE, 0.82f),
            Shader.TileMode.CLAMP
        )
        val gradPaint = Paint().apply {
            shader = gradient
            isAntiAlias = true
        }
        canvas.drawRect(r, gradPaint)

        // Sun / Mountain icon
        val sunSize = (min(rect.width, rect.height) * 0.17 * scale).toFloat()
        val sunPaint = Paint().apply {
            color = Color.argb(200, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawCircle(
            (rect.minX + rect.width * 0.66).toFloat() * scale,
            (rect.minY + rect.height * 0.17).toFloat() * scale,
            sunSize / 2f,
            sunPaint
        )

        val mountPaint = Paint().apply {
            color = blendColor(accent, Color.WHITE, 0.50f)
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(r.left, r.bottom)
            lineTo(r.left, (rect.minY + rect.height * 0.71).toFloat() * scale)
            quadTo(
                (rect.minX + rect.width * 0.40).toFloat() * scale,
                (rect.minY + rect.height * 0.55).toFloat() * scale,
                r.right,
                (rect.minY + rect.height * 0.77).toFloat() * scale
            )
            lineTo(r.right, r.bottom)
            close()
        }
        canvas.drawPath(path, mountPaint)
    }

    private fun drawText(
        canvas: Canvas,
        value: String,
        rect: PolarRect,
        sizePt: Double,
        defaultColor: Int = Color.BLACK,
        defaultBold: Boolean = false,
        align: Paint.Align = Paint.Align.CENTER,
        appearance: TextAppearance? = null,
        scale: Float,
        fonts: FontProvider = SystemFontProvider
    ) {
        if (value.isBlank()) return
        if (appearance != null && !appearance.visible) return
        val target = if (appearance != null) rect.offsetBy(appearance.offsetX, appearance.offsetY) else rect
        val color = if (appearance != null && appearance.hex.isNotBlank()) parseColor(appearance.hex) else defaultColor
        val bold = defaultBold || appearance?.bold == true
        val italic = appearance?.italic == true
        val finalAlign = when (appearance?.alignment) {
            TextAlignment.LEFT -> Paint.Align.LEFT
            TextAlignment.CENTER -> Paint.Align.CENTER
            TextAlignment.RIGHT -> Paint.Align.RIGHT
            else -> align
        }
        var pointSize = if (appearance != null && appearance.size > 0) appearance.size else max(6.0, sizePt)
        val paint = TextPaint().apply {
            this.color = color
            isAntiAlias = true
            typeface = fonts.typeface(appearance?.fontName ?: FontNames.SYSTEM, bold, italic)
        }
        for (i in 0 until 16) {
            paint.textSize = (pointSize * scale).toFloat()
            val fm = paint.fontMetrics
            if ((paint.measureText(value) <= target.width * scale && fm.descent - fm.ascent <= target.height * scale) || pointSize <= 6.0) break
            pointSize = max(6.0, pointSize * 0.88)
        }
        val width = paint.measureText(value)
        val fm = paint.fontMetrics
        val x = when (finalAlign) {
            Paint.Align.LEFT -> (target.left * scale).toFloat()
            Paint.Align.RIGHT -> (target.right * scale).toFloat() - width
            else -> (target.midX * scale).toFloat() - width / 2f
        }
        val y = (target.midY * scale).toFloat() - (fm.descent + fm.ascent) / 2f
        canvas.save()
        canvas.clipRect(target.toAndroidRectF(scale))
        // PDF de Android conserva fuentes de emoji que algunos visores no pintan.
        // Sólo estas líneas van como imagen a 300 ppp; el texto normal sigue siendo vectorial.
        if (value.codePoints().anyMatch { it >= 0x1F000 || it in 0x2300..0x27FF || it == 0xFE0F || it == 0xA9 || it == 0xAE }) {
            val bounds = Rect()
            paint.getTextBounds(value, 0, value.length, bounds)
            val left = min(0f, bounds.left.toFloat()) - 1f
            val top = min(fm.top, bounds.top.toFloat()) - 1f
            val right = max(width, bounds.right.toFloat()) + 1f
            val bottom = max(fm.bottom, bounds.bottom.toFloat()) + 1f
            val density = max(1f, 300f / 72f / scale)
            val raster = Bitmap.createBitmap(ceil((right - left) * density).toInt(), ceil((bottom - top) * density).toInt(), Bitmap.Config.ARGB_8888)
            try {
                Canvas(raster).apply { scale(density, density); drawText(value, -left, -top, paint) }
                canvas.drawBitmap(raster, null, RectF(x + left, y + top, x + right, y + bottom), Paint(Paint.FILTER_BITMAP_FLAG))
            } finally { raster.recycle() }
        } else canvas.drawText(value, x, y, paint)
        canvas.restore()
    }

    private fun drawPlayer(canvas: Canvas, rect: PolarRect, color: Int, scale: Float, volume: Boolean) {
        val r = rect.toAndroidRectF(scale)
        val linePaint = Paint().apply {
            this.color = color
            alpha = 140
            strokeWidth = (0.8 * scale).toFloat()
            isAntiAlias = true
        }
        canvas.drawLine(r.left, r.top, r.right, r.top, linePaint)

        // Scrub dot
        val dotPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }
        val scrubX = (rect.minX + rect.width * 0.58).toFloat() * scale
        val dotR = (1.4 * scale).toFloat()
        canvas.drawCircle(scrubX, r.top, dotR, dotPaint)

        // Timers
        val timerPaint = TextPaint().apply {
            this.color = color
            textSize = (rect.height * 0.14 * scale).toFloat()
            isAntiAlias = true
        }
        canvas.drawText("1:03", r.left, r.top + (8 * scale).toFloat(), timerPaint)
        val t2 = "2:22"
        canvas.drawText(t2, r.right - timerPaint.measureText(t2), r.top + (8 * scale).toFloat(), timerPaint)

        // Center transport buttons (Previous, Play/Pause, Next)
        val btnPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val midY = r.top + (r.height() * 0.55f)
        val btnSize = (min(rect.width * 0.12, rect.height * 0.30) * scale).toFloat()

        // Play triangle
        val playPath = Path().apply {
            moveTo(r.centerX() - btnSize * 0.35f, midY - btnSize * 0.45f)
            lineTo(r.centerX() + btnSize * 0.45f, midY)
            lineTo(r.centerX() - btnSize * 0.35f, midY + btnSize * 0.45f)
            close()
        }
        canvas.drawPath(playPath, btnPaint)
    }

    private fun drawQR(canvas: Canvas, url: String, rect: PolarRect, scale: Float) {
        val sizePx = (min(rect.width, rect.height) * scale).toInt()
        val bitmap = QrGenerator.generateQrBitmap(url, sizePx) ?: return
        val r = rect.toAndroidRectF(scale)
        canvas.drawBitmap(bitmap, null, r, null)
    }

    private fun drawCalendar(
        canvas: Canvas,
        card: PolarRect,
        month: Int,
        settings: PrintSettings,
        accent: Int,
        scale: Float
    ) {
        val monthNames = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
        val monthStr = monthNames.getOrElse(month - 1) { "ENE" }

        drawText(canvas, monthStr, PolarRect(card.minX + card.width * 0.055, card.minY + card.height * 0.67, card.minX + card.width * 0.29, card.minY + card.height * 0.79), card.width * 0.08, Color.BLACK, true, scale = scale)
        drawText(canvas, settings.calendarYear.toString(), PolarRect(card.minX + card.width * 0.055, card.minY + card.height * 0.80, card.minX + card.width * 0.29, card.minY + card.height * 0.855), card.width * 0.062, accent, true, scale = scale)

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, settings.calendarYear)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0

        val specCal = Calendar.getInstance().apply { timeInMillis = SwiftDate.toEpochMs(settings.specialDate) }
        val specYear = specCal.get(Calendar.YEAR)
        val specMonth = specCal.get(Calendar.MONTH) + 1
        val specDay = specCal.get(Calendar.DAY_OF_MONTH)

        val grid = PolarRect(card.minX + card.width * 0.325, card.minY + card.height * 0.672, card.minX + card.width * 0.945, card.minY + card.height * 0.957)
        val cellW = grid.width / 7.0
        val cellH = grid.height / 7.0

        val daysHeader = listOf("L", "M", "M", "J", "V", "S", "D")
        for ((col, day) in daysHeader.withIndex()) {
            val cell = PolarRect(grid.minX + col * cellW, grid.minY, grid.minX + (col + 1) * cellW, grid.minY + cellH)
            drawText(canvas, day, cell, cellH * 0.68, if (col == 6) accent else Color.GRAY, true, scale = scale)
        }

        for (day in 1..daysInMonth) {
            val idx = offset + day - 1
            val col = idx % 7
            val row = (idx / 7) + 1
            val cell = PolarRect(grid.minX + col * cellW, grid.minY + row * cellH, grid.minX + (col + 1) * cellW, grid.minY + (row + 1) * cellH)
            val isSpecial = settings.highlightDate && specYear == settings.calendarYear && specMonth == month && specDay == day

            if (isSpecial) {
                val circlePaint = Paint().apply { color = accent; isAntiAlias = true }
                val cr = cell.insetBy(cellW * 0.13, 0.0).toAndroidRectF(scale)
                canvas.drawRoundRect(cr, (cellH * 0.5 * scale).toFloat(), (cellH * 0.5 * scale).toFloat(), circlePaint)
            }

            drawText(canvas, day.toString(), cell, cellH * 0.66, if (isSpecial) Color.WHITE else if (col == 6) accent else Color.BLACK, isSpecial, scale = scale)
        }
    }

    private fun drawPerforations(canvas: Canvas, card: PolarRect, vertical: Boolean, scale: Float) {
        val count = if (vertical) 30 else 35
        val p = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        for (i in 0 until count) {
            if (vertical) {
                val h = (card.height / count.toDouble()) * 0.68
                val y = card.minY + (i.toDouble() + 0.15) * (card.height / count.toDouble())
                for (x in listOf(card.minX + card.width * 0.03, card.maxX - card.width * 0.10)) {
                    val rect = PolarRect(x, y, x + card.width * 0.07, y + h).toAndroidRectF(scale)
                    canvas.drawRect(rect, p)
                }
            } else {
                val w = (card.width / count.toDouble()) * 0.57
                val x = card.minX + (i.toDouble() + 0.22) * (card.width / count.toDouble())
                for (y in listOf(card.minY + card.height * 0.04, card.maxY - card.height * 0.115)) {
                    val rect = PolarRect(x, y, x + w, y + card.height * 0.075).toAndroidRectF(scale)
                    canvas.drawRect(rect, p)
                }
            }
        }
    }

    private fun drawTicketNotches(canvas: Canvas, card: PolarRect, scale: Float) {
        val hole = card.height * 0.018
        val p = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        for (i in 1 until 15) {
            val y = card.minY + i.toDouble() * card.height / 15.0
            for (x in listOf(card.minX, card.maxX, card.minX + card.width * 0.745)) {
                val rect = RectF(
                    ((x - hole) * scale).toFloat(),
                    ((y - hole) * scale).toFloat(),
                    ((x + hole) * scale).toFloat(),
                    ((y + hole) * scale).toFloat()
                )
                canvas.drawRoundRect(rect, (hole * scale).toFloat(), (hole * scale).toFloat(), p)
            }
        }
    }

    private fun drawGuides(
        canvas: Canvas,
        card: PolarRect,
        style: CutStyle,
        paper: PolarRect,
        excludingCards: List<PolarRect>,
        scale: Float
    ) {
        val grayPaint = Paint().apply {
            color = Color.rgb(163, 163, 163)
            strokeWidth = max(1f, (0.35 * scale).toFloat())
            isAntiAlias = true
        }

        if (style == CutStyle.LINES) {
            grayPaint.pathEffect = DashPathEffect(floatArrayOf(3f * scale, 3f * scale), 0f)
            grayPaint.style = Paint.Style.STROKE
            canvas.drawRect(card.toAndroidRectF(scale), grayPaint)
            return
        }

        // Corner tick marks
        for (x in listOf(card.minX, card.maxX)) {
            canvas.drawLine((x * scale).toFloat(), ((card.minY - 5.0) * scale).toFloat(), (x * scale).toFloat(), ((card.minY - 1.5) * scale).toFloat(), grayPaint)
            canvas.drawLine((x * scale).toFloat(), ((card.maxY + 1.5) * scale).toFloat(), (x * scale).toFloat(), ((card.maxY + 5.0) * scale).toFloat(), grayPaint)
        }
        for (y in listOf(card.minY, card.maxY)) {
            canvas.drawLine(((card.minX - 5.0) * scale).toFloat(), (y * scale).toFloat(), ((card.minX - 1.5) * scale).toFloat(), (y * scale).toFloat(), grayPaint)
            canvas.drawLine(((card.maxX + 1.5) * scale).toFloat(), (y * scale).toFloat(), ((card.maxX + 5.0) * scale).toFloat(), (y * scale).toFloat(), grayPaint)
        }
    }

    private fun drawImported(
        canvas: Canvas,
        project: PolarProject,
        page: Int,
        isPreview: Boolean,
        scale: Float,
        bitmapProvider: (PhotoAsset) -> Bitmap?,
        templateBitmap: Bitmap?,
        @Suppress("UNUSED_PARAMETER") fonts: FontProvider,
        pdfPhoto: ((Bitmap) -> Unit)? = null
    ) {
        val s = project.settings
        val template = s.importedTemplate ?: return
        val frame = templateRect(s)
        val cards = calculateCardRects(s)

        // Draw transparent holes photos first
        for ((idx, region) in template.regions.withIndex()) {
            if (region.isTransparent) {
                val slot = page * s.capacity + idx
                val placement = project.placements.getOrNull(slot)
                if (isPreview || placement != null) {
                    drawPhoto(canvas, project.asset(placement), placement, cards[idx], parseColor(s.accentHex), Color.WHITE, isPreview, scale, bitmapProvider, LookResolver.resolve(project, slot), page * project.cardsPerPage + idx, cards[idx], pdfPhoto = pdfPhoto, backgroundPhoto = placement?.background?.imageID?.let { id -> project.photos.firstOrNull { it.id == id } })
                }
            }
        }

        // Draw template bitmap
        if (templateBitmap != null) {
            canvas.drawBitmap(templateBitmap, null, frame.toAndroidRectF(scale), null)
        }

        // Draw opaque holes photos on top of template
        for ((idx, region) in template.regions.withIndex()) {
            if (!region.isTransparent) {
                val slot = page * s.capacity + idx
                val placement = project.placements.getOrNull(slot)
                if (isPreview || placement != null) {
                    drawPhoto(canvas, project.asset(placement), placement, cards[idx], parseColor(s.accentHex), Color.WHITE, isPreview, scale, bitmapProvider, LookResolver.resolve(project, slot), page * project.cardsPerPage + idx, cards[idx], pdfPhoto = pdfPhoto, backgroundPhoto = placement?.background?.imageID?.let { id -> project.photos.firstOrNull { it.id == id } })
                }
            }
        }
    }

    private fun heartPath(rect: PolarRect, scale: Float): Path {
        val p = Path()
        fun px(x: Double): Float = ((rect.minX + rect.width * x) * scale).toFloat()
        fun py(y: Double): Float = ((rect.minY + rect.height * y) * scale).toFloat()

        p.moveTo(px(0.5), py(0.98))
        p.cubicTo(px(0.30), py(0.79), px(-0.04), py(0.50), px(0.02), py(0.30))
        p.cubicTo(px(0.06), py(-0.03), px(0.36), py(-0.03), px(0.5), py(0.19))
        p.cubicTo(px(0.64), py(-0.03), px(0.94), py(-0.03), px(0.98), py(0.30))
        p.cubicTo(px(1.04), py(0.50), px(0.70), py(0.79), px(0.5), py(0.98))
        p.close()
        return p
    }

    private fun parseColor(hex: String): Int {
        val clean = hex.removePrefix("#")
        val num = clean.toLongOrNull(16) ?: 0x92394A
        val r = ((num shr 16) and 0xFF).toInt()
        val g = ((num shr 8) and 0xFF).toInt()
        val b = (num and 0xFF).toInt()
        return Color.rgb(r, g, b)
    }

    private fun blendColor(c1: Int, c2: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(c1) * (1f - f) + Color.red(c2) * f).toInt()
        val g = (Color.green(c1) * (1f - f) + Color.green(c2) * f).toInt()
        val b = (Color.blue(c1) * (1f - f) + Color.blue(c2) * f).toInt()
        return Color.rgb(r, g, b)
    }
}
