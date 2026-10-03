import AppKit
import CoreImage
import ImageIO
import Darwin

// All public rectangles use the sheet's top-left origin, with y increasing downward.
enum PolarRenderer {
    private static func paperRect(_ settings: PrintSettings) -> CGRect { CGRect(origin: .zero, size: settings.paperSizePoints) }

    static func templateRect(settings: PrintSettings) -> CGRect {
        let available = paperRect(settings).insetBy(dx: settings.margin, dy: settings.margin)
        guard let template = settings.importedTemplate else { return available }
        let aspect = CGFloat(template.pixelWidth) / CGFloat(max(1, template.pixelHeight))
        let width = min(available.width, available.height * aspect), height = width / aspect
        return CGRect(x: available.midX - width / 2, y: available.midY - height / 2, width: width, height: height)
    }

    static func cardRects(settings: PrintSettings) -> [CGRect] {
        let paper = paperRect(settings)
        if settings.style == .imported, let template = settings.importedTemplate {
            let frame = templateRect(settings: settings)
            return template.regions.map { CGRect(x: frame.minX + $0.x * frame.width, y: frame.minY + $0.y * frame.height,
                                                width: $0.width * frame.width, height: $0.height * frame.height) }
        }
        let columns = settings.columns, rows = settings.rows
        let margin = CGFloat(settings.margin), gap = CGFloat(settings.gap)
        guard columns > 0, rows > 0, columns <= 4, rows <= 6,
              margin.isFinite, gap.isFinite else { return [] }
        let cellWidth = (paper.width - margin * 2 - gap * CGFloat(columns - 1)) / CGFloat(columns)
        let cellHeight = (paper.height - margin * 2 - gap * CGFloat(rows - 1)) / CGFloat(rows)
        let width = settings.cardAspect.map { min(cellWidth, cellHeight * $0) } ?? cellWidth
        let height = settings.cardAspect.map { width / $0 } ?? cellHeight
        guard width > 0, height > 0 else { return [] }
        return (0..<(columns * rows)).map { index in
            CGRect(x: margin + CGFloat(index % columns) * (cellWidth + gap) + (cellWidth - width) / 2,
                   y: margin + CGFloat(index / columns) * (cellHeight + gap) + (cellHeight - height) / 2,
                   width: width, height: height)
        }
    }

    static func photoRects(in card: CGRect, style: TemplateStyle, settings: PrintSettings) -> [CGRect] {
        func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
            CGRect(x: card.minX + x * card.width, y: card.minY + y * card.height,
                   width: w * card.width, height: h * card.height)
        }
        switch style {
        case .polaroid: return [r(0.065, 0.045, 0.87, 0.735)]
        case .mini: return [r(0.075, 0.045, 0.85, 0.75)]
        case .spotify: return [r(0.065, 0.045, 0.87, 0.61)]
        case .playerRed: return [r(0.065, 0.04, 0.87, 0.56)]
        case .playerGray: return [r(0.045, 0.085, 0.47, 0.81)]
        case .ticket: return [r(0.055, 0.09, 0.65, 0.65)]
        case .filmVertical: return (0..<5).map { r(0.14, 0.02 + CGFloat($0) * 0.196, 0.72, 0.18) }
        case .filmHorizontal: return (0..<5).map { r(0.014 + CGFloat($0) * 0.195, 0.16, 0.18, 0.68) }
        case .calendar: return [r(0.06, 0.04, 0.88, 0.585)]
        case .instagram: return [r(0.045, 0.19, 0.91, 0.59)]
        case .custom: return [r(0.055, 0.055, 0.89, 0.70)]
        case .borderless, .imported: return [card]
        case .square: return [r(0.06, 0.06, 0.88, 0.76)]
        case .postcard: return [r(0.035, 0.055, 0.61, 0.89)]
        case .botanical: return [r(0.09, 0.07, 0.82, 0.65)]
        case .celebration: return [r(0.07, 0.13, 0.86, 0.56)]
        case .pets: return [r(0.065, 0.07, 0.87, 0.65)]
        case .heart: return [r(0.065, 0.08, 0.87, 0.65)]
        case .editorial: return [r(0.065, 0.19, 0.87, 0.55)]
        }
    }

    static func preview(project: PolarProject, page: Int, scale: CGFloat = 2, printReady: Bool = false) -> NSImage {
        let paper = paperRect(project.settings)
        let width = max(1, Int((paper.width * scale).rounded())), height = max(1, Int((paper.height * scale).rounded()))
        guard let context = bitmap(width: width, height: height) else { return NSImage(size: paper.size) }
        context.translateBy(x: 0, y: CGFloat(height))
        context.scaleBy(x: CGFloat(width) / paper.width, y: -CGFloat(height) / paper.height)
        try? drawPage(project: project, page: max(0, min(page, project.pageCount - 1)), context: context, isPreview: !printReady)
        guard let image = context.makeImage() else { return NSImage(size: paper.size) }
        return NSImage(cgImage: image, size: paper.size)
    }

    static func cropGeometry(project: PolarProject, slot: Int) -> (card: CGRect, photo: CGRect)? {
        let per = project.settings.style.photosPerCard, local = slot % project.settings.capacity
        let cards = cardRects(settings: project.settings)
        guard cards.indices.contains(local / per) else { return nil }
        let card = cards[local / per], areas = photoRects(in: card, style: project.settings.style, settings: project.settings)
        guard areas.indices.contains(local % per) else { return nil }
        return (card, areas[local % per])
    }

    static func cardPreview(project: PolarProject, slot: Int, width: CGFloat = 440, photoOnly: Bool = false) -> NSImage? {
        guard let geometry = cropGeometry(project: project, slot: slot) else { return nil }
        let rect = photoOnly ? geometry.photo : geometry.card, scale = min(2, width / rect.width)
        let w = max(1, Int(ceil(rect.width * scale))), h = max(1, Int(ceil(rect.height * scale)))
        guard let context = bitmap(width: w, height: h) else { return nil }
        context.translateBy(x: 0, y: CGFloat(h)); context.scaleBy(x: CGFloat(w) / rect.width, y: -CGFloat(h) / rect.height)
        context.translateBy(x: -rect.minX, y: -rect.minY)
        let previous = NSGraphicsContext.current
        NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: true)
        defer { NSGraphicsContext.current = previous }
        if project.settings.style == .imported {
            try? drawImported(project: project, page: slot / project.settings.capacity, context: context, isPreview: true, isPDF: false)
        } else {
            let per = project.settings.style.photosPerCard
            try? drawCard(geometry.card, project: project, firstSlot: slot / per * per, context: context, isPreview: true, isPDF: false)
        }
        guard let image = context.makeImage() else { return nil }
        return NSImage(cgImage: image, size: rect.size)
    }

    // Only the lost area is shown by CropView; the frame itself remains the real card render.
    static func cropOverflow(project: PolarProject, slot: Int) -> NSImage? {
        guard let geometry = cropGeometry(project: project, slot: slot), project.placements.indices.contains(slot),
              let placement = project.placements[slot], let asset = project.asset(for: placement) else { return nil }
        let viewport = geometry.card.insetBy(dx: -geometry.card.width * 0.18, dy: -geometry.card.height * 0.08)
        let scale: CGFloat = 2
        guard let context = bitmap(width: Int(ceil(viewport.width * scale)), height: Int(ceil(viewport.height * scale))) else { return nil }
        context.translateBy(x: 0, y: CGFloat(context.height)); context.scaleBy(x: scale, y: -scale)
        context.translateBy(x: -viewport.minX, y: -viewport.minY)
        try? drawPhoto(asset, placement: placement, in: geometry.photo, radius: 0, accent: .black, background: .white,
                       context: context, isPreview: true, isPDF: false, look: PhotoLook(), card: geometry.card,
                       cardIndex: slot / project.settings.style.photosPerCard, clipPhoto: false)
        guard let image = context.makeImage() else { return nil }
        return NSImage(cgImage: image, size: viewport.size)
    }

    static func writePDF(project: PolarProject, to url: URL, optimizePhotos: Bool = true) throws {
        try validateExport(project, to: url)
        let paper = paperRect(project.settings)
        try atomicWrite(to: url) { temporary in
            var mediaBox = paper
            guard let consumer = CGDataConsumer(url: temporary as CFURL),
                  let context = CGContext(consumer: consumer, mediaBox: &mediaBox,
                                          [kCGPDFContextCreator: "Polar"] as CFDictionary)
            else { throw PolarError.exportFailed }
            defer { context.closePDF() }
            for page in 0..<project.pageCount {
                context.beginPDFPage(nil)
                context.saveGState()
                context.translateBy(x: 0, y: paper.height)
                context.scaleBy(x: 1, y: -1)
                do {
                    try drawPage(project: project, page: page, context: context, isPreview: false, isPDF: true, optimizePhotos: optimizePhotos)
                    context.restoreGState()
                    context.endPDFPage()
                } catch {
                    context.restoreGState()
                    context.endPDFPage()
                    throw error
                }
            }
        }
    }

    static func writePNG(project: PolarProject, page: Int, to url: URL) throws {
        try writeImage(project: project, page: page, to: url, jpeg: false)
    }

    static func writeJPEG(project: PolarProject, page: Int, to url: URL) throws {
        try writeImage(project: project, page: page, to: url, jpeg: true)
    }

    private static func writeImage(project: PolarProject, page: Int, to url: URL, jpeg: Bool) throws {
        try validateExport(project, to: url, page: page)
        let paper = paperRect(project.settings)
        let width = max(1, Int((paper.width * 300 / 72).rounded())), height = max(1, Int((paper.height * 300 / 72).rounded()))
        try atomicWrite(to: url) { temporary in
            guard let context = bitmap(width: width, height: height) else { throw PolarError.exportFailed }
            context.translateBy(x: 0, y: CGFloat(height))
            context.scaleBy(x: CGFloat(width) / paper.width, y: -CGFloat(height) / paper.height)
            try drawPage(project: project, page: page, context: context, isPreview: false)
            guard let image = context.makeImage(),
                  let destination = CGImageDestinationCreateWithURL(temporary as CFURL, (jpeg ? "public.jpeg" : "public.png") as CFString, 1, nil)
            else { throw PolarError.exportFailed }
            var properties: [CFString: Any] = [kCGImagePropertyDPIWidth: 300, kCGImagePropertyDPIHeight: 300]
            if jpeg { properties[kCGImageDestinationLossyCompressionQuality] = 0.94 }
            CGImageDestinationAddImage(destination, image, properties as CFDictionary)
            guard CGImageDestinationFinalize(destination) else { throw PolarError.exportFailed }
        }
    }

    private static func bitmap(width: Int, height: Int) -> CGContext? {
        CGContext(data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
                  space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)
    }

    private static func validateExport(_ project: PolarProject, to destination: URL, page: Int? = nil) throws {
        try project.validated()
        if let page, !(0..<project.pageCount).contains(page) { throw PolarError.invalidProject("La página no existe.") }
        let canonicalDestination = destination.standardizedFileURL.resolvingSymlinksInPath()
        let templatePath = project.settings.style == .imported ? project.settings.importedTemplate?.path : nil
        let protected = project.photos.map(\.url) + (templatePath.map { [URL(fileURLWithPath: $0)] } ?? [])
        guard !protected.contains(where: { $0.standardizedFileURL.resolvingSymlinksInPath() == canonicalDestination }) else {
            throw NSError(domain: "PolarExport", code: 1, userInfo: [NSLocalizedDescriptionKey:
                "Elige otro nombre o carpeta para exportar y conservar los archivos originales."])
        }
        if project.settings.style == .imported {
            guard let templatePath, FileManager.default.isReadableFile(atPath: templatePath),
                  CGImageSourceCreateWithURL(URL(fileURLWithPath: templatePath) as CFURL, nil) != nil
            else { throw PolarError.invalidProject("Agrega de nuevo la plantilla importada; no se puede leer su archivo.") }
        }
        let start = (page ?? 0) * project.settings.capacity
        let end = page == nil ? project.placements.count : min(project.placements.count, start + project.settings.capacity)
        let assigned = Set(project.placements[start..<end].compactMap { $0?.assetID })
        for photo in project.photos where assigned.contains(photo.id) {
            guard FileManager.default.isReadableFile(atPath: photo.path),
                  let source = CGImageSourceCreateWithURL(photo.url as CFURL, nil), CGImageSourceGetCount(source) > 0
            else { throw PolarError.unreadablePhoto(photo.name) }
        }
    }

    private static func atomicWrite(to url: URL, write: (URL) throws -> Void) throws {
        let temporary = url.deletingLastPathComponent().appendingPathComponent(".\(url.lastPathComponent).\(UUID().uuidString).tmp")
        defer { try? FileManager.default.removeItem(at: temporary) }
        try write(temporary)
        let result = temporary.path.withCString { source in url.path.withCString { destination in rename(source, destination) } }
        guard result == 0 else { throw PolarError.exportFailed }
    }

    private static func drawPage(project: PolarProject, page: Int, context: CGContext, isPreview: Bool, isPDF: Bool = false, optimizePhotos: Bool = true) throws {
        let paper = paperRect(project.settings)
        let previous = NSGraphicsContext.current
        NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: true)
        defer { NSGraphicsContext.current = previous }
        context.setFillColor(NSColor.white.cgColor)
        context.fill(paper)
        context.setShouldAntialias(true)
        context.interpolationQuality = .high
        if project.settings.style == .imported, project.settings.importedTemplate != nil {
            try drawImported(project: project, page: page, context: context, isPreview: isPreview, isPDF: isPDF, optimizePhotos: optimizePhotos)
            return
        }
        let cards = cardRects(settings: project.settings)
        for (index, card) in cards.enumerated() {
            context.saveGState()
            defer { context.restoreGState() }
            let firstSlot = page * project.settings.capacity + index * project.settings.style.photosPerCard
            if !isPreview && (firstSlot >= project.placements.count || project.placements[firstSlot..<min(project.placements.count, firstSlot + project.settings.style.photosPerCard)].allSatisfy({ $0 == nil })) { continue }
            try drawCard(card, project: project, firstSlot: firstSlot, context: context, isPreview: isPreview, isPDF: isPDF, optimizePhotos: optimizePhotos)
            if project.settings.cutGuides { drawGuides(card, style: project.settings.cutStyle, paper: paper, excluding: cards, context: context) }
        }
    }

    private static func drawCard(_ card: CGRect, project: PolarProject, firstSlot: Int, context: CGContext, isPreview: Bool, isPDF: Bool, optimizePhotos: Bool = true) throws {
        let s = project.settings, style = s.style, accent = color(s.accentHex)
        func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
            CGRect(x: card.minX + x * card.width, y: card.minY + y * card.height, width: w * card.width, height: h * card.height)
        }
        let background: NSColor
        switch style {
        case .filmVertical, .filmHorizontal: background = NSColor(white: 0.055, alpha: 1)
        case .playerRed: background = accent
        case .playerGray: background = NSColor(white: 0.52, alpha: 1)
        case .ticket: background = NSColor(red: 0.89, green: 0.79, blue: 0.65, alpha: 1)
        case .botanical, .pets, .editorial: background = NSColor(red: 0.98, green: 0.97, blue: 0.93, alpha: 1)
        case .celebration, .heart: background = accent.blended(withFraction: 0.94, of: .white) ?? .white
        default: background = .white
        }
        let radius = style == .playerGray || style == .playerRed ? min(card.width, card.height) * 0.08 : 0
        fill(card, color: background, radius: radius, context: context)
        if background == .white && (isPreview || s.drawBorders) { stroke(card, color: NSColor(white: 0.78, alpha: 1), width: 0.45, context: context) }
        if style == .ticket {
            fill(r(0.745, 0, 0.255, 1), color: accent, context: context)
        }
        if style == .custom {
            stroke(card.insetBy(dx: 2, dy: 2), color: accent, width: 3, context: context)
        }
        let slots = photoRects(in: card, style: style, settings: s)
        for (subslot, rect) in slots.enumerated() {
            context.saveGState()
            defer { context.restoreGState() }
            let slot = firstSlot + subslot
            let placement = project.placements.indices.contains(slot) ? project.placements[slot] : nil
            let photo = project.asset(for: placement)
            let rounded = s.roundedPhotos || style == .playerGray || style == .playerRed || style == .pets
            if style == .heart { context.addPath(heartPath(in: rect)); context.clip() }
            try drawPhoto(photo, placement: placement, in: rect, radius: rounded ? min(rect.width, rect.height) * 0.045 : 0,
                          accent: accent, background: background, context: context, isPreview: isPreview, isPDF: isPDF, optimizePhotos: optimizePhotos,
                          look: LookResolver.resolve(project: project, slot: slot), card: card, cardIndex: firstSlot / style.photosPerCard)
        }
        func userText(_ role: TextRole, in rect: CGRect, size: CGFloat, color: NSColor = .black,
                      weight: NSFont.Weight = .regular, alignment: NSTextAlignment = .center) {
            let cardIndex = firstSlot / style.photosPerCard
            text(TextResolver.text(project: project, card: cardIndex, role: role), in: rect, size: size, color: color, weight: weight,
                 alignment: alignment, appearance: TextResolver.appearance(project: project, card: cardIndex, role: role),
                 card: card, photos: role == .date && style == .borderless ? [] : slots)
        }
        let fontSize = card.width * 0.06
        switch style {
        case .polaroid, .mini:
            userText(.title, in: r(0.07, 0.82, 0.86, 0.075), size: fontSize, color: accent, weight: .medium)
            userText(.subtitle, in: r(0.07, 0.905, 0.86, 0.05), size: fontSize * 0.66, color: .darkGray)
        case .spotify:
            userText(.song, in: r(0.065, 0.686, 0.78, 0.065), size: fontSize * 1.12, weight: .bold, alignment: .left)
            userText(.artist, in: r(0.065, 0.757, 0.78, 0.045), size: fontSize * 0.78, color: .gray, alignment: .left)
            let hasQR = !s.songURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            drawPlayer(in: r(0.07, 0.826, hasQR ? 0.65 : 0.86, 0.145), color: .black, context: context, volume: false)
            if hasQR {
                let side = min(card.width * 0.21, card.height * 0.15)
                let qrRect = CGRect(x: card.maxX - card.width * 0.035 - side, y: card.maxY - card.height * 0.03 - side,
                                    width: side, height: side)
                try drawQR(s.songURL, in: qrRect, context: context, isPreview: isPreview)
            }
        case .playerRed:
            userText(.song, in: r(0.07, 0.634, 0.80, 0.055), size: fontSize * 0.83, color: .white, weight: .semibold, alignment: .left)
            userText(.artist, in: r(0.07, 0.693, 0.80, 0.04), size: fontSize * 0.62, color: .white, alignment: .left)
            drawPlayer(in: r(0.075, 0.76, 0.85, 0.195), color: .white, context: context, volume: true)
        case .playerGray:
            userText(.song, in: r(0.56, 0.16, 0.38, 0.11), size: card.height * 0.064, color: .white, weight: .semibold, alignment: .left)
            userText(.artist, in: r(0.56, 0.30, 0.38, 0.09), size: card.height * 0.05, color: .white, alignment: .left)
            drawPlayer(in: r(0.56, 0.44, 0.38, 0.42), color: .white, context: context, volume: true)
            line(from: CGPoint(x: card.midX - card.width * 0.07, y: card.maxY - card.height * 0.05),
                 to: CGPoint(x: card.midX + card.width * 0.07, y: card.maxY - card.height * 0.05), color: .white, width: 1, context: context)
        case .ticket:
            userText(.title, in: r(0.06, 0.79, 0.64, 0.095), size: card.height * 0.075, color: accent, weight: .bold)
            userText(.subtitle, in: r(0.06, 0.895, 0.64, 0.07), size: card.height * 0.05, color: .darkGray)
            text("POLAR", in: r(0.76, 0.14, 0.22, 0.12), size: card.height * 0.10, color: .white, weight: .bold)
            text("Nº \(String(format: "%03d", firstSlot + 1))", in: r(0.76, 0.65, 0.22, 0.12), size: card.height * 0.075, color: .white, weight: .semibold)
            symbol("sparkle", in: r(0.80, 0.33, 0.14, 0.20), color: .white)
            let hole = card.height * 0.018
            for i in 1..<15 {
                let y = card.minY + CGFloat(i) * card.height / 15
                for x in [card.minX, card.maxX, card.minX + card.width * 0.745] {
                    fill(CGRect(x: x - hole, y: y - hole, width: hole * 2, height: hole * 2), color: .white, radius: hole, context: context)
                }
            }
        case .filmVertical, .filmHorizontal:
            drawPerforations(card, vertical: style == .filmVertical, context: context)
        case .calendar:
            drawCalendar(in: card, month: (firstSlot % 12) + 1, settings: s, accent: accent, context: context)
        case .instagram:
            symbol("camera", in: r(0.065, 0.027, 0.065, 0.042), color: .black)
            text("Instagram", in: r(0.16, 0.026, 0.53, 0.06), size: fontSize * 0.88, weight: .semibold, alignment: .left)
            symbol("paperplane", in: r(0.88, 0.026, 0.065, 0.045), color: .black)
            stroke(r(0.064, 0.112, 0.074, 0.052), color: accent, width: 1, radius: card.width * 0.037, context: context)
            userText(.title, in: r(0.17, 0.116, 0.69, 0.044), size: fontSize * 0.7, weight: .semibold, alignment: .left)
            text("⋮", in: r(0.89, 0.106, 0.06, 0.065), size: fontSize, weight: .bold)
            for (i, name) in ["heart.fill", "bubble", "paperplane"].enumerated() {
                symbol(name, in: r(0.06 + CGFloat(i) * 0.12, 0.8, 0.075, 0.05), color: i == 0 ? accent : .black)
            }
            symbol("bookmark.fill", in: r(0.865, 0.8, 0.07, 0.05), color: .black)
            userText(.caption, in: r(0.065, 0.863, 0.87, 0.04), size: fontSize * 0.63, alignment: .left)
            for (i, name) in ["house.fill", "magnifyingglass", "plus.app", "heart", "person.crop.circle"].enumerated() {
                symbol(name, in: r(0.065 + CGFloat(i) * 0.197, 0.93, 0.06, 0.045), color: .black)
            }
        case .custom:
            userText(.title, in: r(0.06, 0.81, 0.88, 0.08), size: fontSize * 1.15, color: accent, weight: .semibold)
            userText(.subtitle, in: r(0.06, 0.91, 0.88, 0.045), size: fontSize * 0.75, color: .darkGray)
        case .square:
            userText(.title, in: r(0.06, 0.845, 0.88, 0.075), size: fontSize, color: accent, weight: .semibold)
            userText(.subtitle, in: r(0.06, 0.93, 0.88, 0.045), size: fontSize * 0.66, color: .darkGray)
        case .postcard:
            line(from: CGPoint(x: card.minX + card.width * 0.675, y: card.minY + card.height * 0.12),
                 to: CGPoint(x: card.minX + card.width * 0.675, y: card.minY + card.height * 0.88), color: accent, width: 0.6, context: context)
            symbol("envelope", in: r(0.86, 0.06, 0.10, 0.10), color: accent)
            userText(.title, in: r(0.70, 0.23, 0.26, 0.19), size: card.height * 0.09, color: accent, weight: .bold)
            userText(.subtitle, in: r(0.70, 0.46, 0.26, 0.16), size: card.height * 0.055, color: .darkGray)
            userText(.caption, in: r(0.70, 0.69, 0.26, 0.15), size: card.height * 0.043, color: .darkGray)
        case .botanical:
            symbol("leaf.fill", in: r(0.02, 0.72, 0.12, 0.08), color: accent)
            symbol("leaf", in: r(0.86, 0.04, 0.12, 0.08), color: accent)
            userText(.title, in: r(0.08, 0.80, 0.84, 0.08), size: fontSize * 1.10, color: accent, weight: .medium)
            userText(.subtitle, in: r(0.08, 0.90, 0.84, 0.055), size: fontSize * 0.70, color: .darkGray)
        case .celebration:
            symbol("sparkles", in: r(0.06, 0.035, 0.15, 0.065), color: accent)
            symbol("party.popper", in: r(0.78, 0.035, 0.15, 0.065), color: accent)
            for x in [CGFloat(0.12), 0.36, 0.64, 0.88] { fill(r(x, 0.735, 0.012, 0.012), color: accent, radius: 1, context: context) }
            userText(.title, in: r(0.07, 0.78, 0.86, 0.07), size: fontSize * 1.1, color: accent, weight: .bold)
            userText(.caption, in: r(0.07, 0.85, 0.86, 0.08), size: fontSize * 0.70, color: .darkGray)
            userText(.subtitle, in: r(0.07, 0.94, 0.86, 0.035), size: fontSize * 0.55, color: accent)
        case .pets:
            symbol("pawprint.fill", in: r(0.04, 0.79, 0.08, 0.06), color: accent)
            symbol("pawprint", in: r(0.88, 0.79, 0.08, 0.06), color: accent)
            userText(.title, in: r(0.14, 0.79, 0.72, 0.08), size: fontSize * 1.15, color: accent, weight: .bold)
            userText(.subtitle, in: r(0.07, 0.90, 0.86, 0.055), size: fontSize * 0.75, color: .darkGray)
        case .heart:
            context.setStrokeColor(accent.cgColor); context.setLineWidth(0.8)
            context.addPath(heartPath(in: slots[0])); context.strokePath()
            userText(.title, in: r(0.07, 0.80, 0.86, 0.075), size: fontSize * 1.05, color: accent, weight: .semibold)
            userText(.subtitle, in: r(0.07, 0.905, 0.86, 0.05), size: fontSize * 0.72, color: .darkGray)
        case .editorial:
            userText(.title, in: r(0.065, 0.045, 0.87, 0.08), size: fontSize * 1.5, color: accent, weight: .heavy)
            userText(.subtitle, in: r(0.065, 0.135, 0.87, 0.04), size: fontSize * 0.55, alignment: .left)
            userText(.caption, in: r(0.065, 0.79, 0.87, 0.12), size: fontSize * 0.80, color: .darkGray, alignment: .left)
            text("POLAR / \(String(format: "%03d", firstSlot + 1))", in: r(0.065, 0.95, 0.87, 0.025), size: fontSize * 0.42, color: accent, alignment: .right)
        case .borderless, .imported: break
        }
        if style.supportsDate && TextResolver.dateSource(project: project, card: firstSlot / style.photosPerCard) != .none {
            let layout: (CGRect, CGFloat, NSColor, NSTextAlignment)?
            switch style {
            case .polaroid, .mini, .pets, .heart: layout = (r(0.07, 0.955, 0.86, 0.035), fontSize * 0.5, .gray, .center)
            case .spotify: layout = (r(0.55, 0.757, 0.30, 0.045), fontSize * 0.6, .gray, .right)
            case .playerRed: layout = (r(0.55, 0.693, 0.32, 0.04), fontSize * 0.55, .white, .right)
            case .playerGray: layout = (r(0.56, 0.86, 0.38, 0.08), card.height * 0.045, .white, .left)
            case .ticket: layout = (r(0.76, 0.40, 0.22, 0.10), card.height * 0.06, .white, .center)
            case .instagram: layout = (r(0.065, 0.91, 0.87, 0.035), fontSize * 0.5, .gray, .left)
            case .custom: layout = (r(0.06, 0.955, 0.88, 0.035), fontSize * 0.5, .gray, .center)
            case .square: layout = (r(0.06, 0.975, 0.88, 0.022), fontSize * 0.42, .gray, .center)
            case .postcard: layout = (r(0.70, 0.86, 0.26, 0.07), card.height * 0.04, .gray, .center)
            case .botanical: layout = (r(0.08, 0.955, 0.84, 0.035), fontSize * 0.5, .gray, .center)
            case .celebration: layout = (r(0.07, 0.07, 0.86, 0.05), fontSize * 0.55, accent, .center)
            case .editorial: layout = (r(0.065, 0.95, 0.40, 0.025), fontSize * 0.42, accent, .left)
            case .borderless: layout = (r(0.40, 0.90, 0.56, 0.06), card.height * 0.045, color("FF8C2E"), .right)
            default: layout = nil
            }
            if let (rect, size, color, alignment) = layout { userText(.date, in: rect, size: size, color: color, alignment: alignment) }
        }
    }

    private static func heartPath(in rect: CGRect) -> CGPath {
        let path = CGMutablePath()
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: rect.minX + rect.width * x, y: rect.minY + rect.height * y) }
        path.move(to: p(0.5, 0.98))
        path.addCurve(to: p(0.02, 0.30), control1: p(0.30, 0.79), control2: p(-0.04, 0.50))
        path.addCurve(to: p(0.5, 0.19), control1: p(0.06, -0.03), control2: p(0.36, -0.03))
        path.addCurve(to: p(0.98, 0.30), control1: p(0.64, -0.03), control2: p(0.94, -0.03))
        path.addCurve(to: p(0.5, 0.98), control1: p(1.04, 0.50), control2: p(0.70, 0.79))
        path.closeSubpath()
        return path
    }

    private static func drawImported(project: PolarProject, page: Int, context: CGContext, isPreview: Bool, isPDF: Bool, optimizePhotos: Bool = true) throws {
        guard let template = project.settings.importedTemplate else { return }
        let frame = templateRect(settings: project.settings), cards = cardRects(settings: project.settings)
        func photo(_ index: Int) throws {
            let slot = page * project.settings.capacity + index
            let placement = project.placements.indices.contains(slot) ? project.placements[slot] : nil
            if !isPreview && placement == nil { return }
            try drawPhoto(project.asset(for: placement), placement: placement, in: cards[index], radius: 0,
                          accent: color(project.settings.accentHex), background: .white, context: context, isPreview: isPreview, isPDF: isPDF, optimizePhotos: optimizePhotos,
                          look: LookResolver.resolve(project: project, slot: slot), card: cards[index], cardIndex: slot)
        }
        for index in template.regions.indices where template.regions[index].isTransparent { try photo(index) }
        let maximum = max(1, Int((max(frame.width, frame.height) * (isPreview ? 2 : 300 / 72)).rounded(.up)))
        if let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: template.path) as CFURL, nil),
           var image = CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true, kCGImageSourceThumbnailMaxPixelSize: maximum] as CFDictionary) {
            if isPDF && optimizePhotos && !template.regions.contains(where: \.isTransparent) { image = try jpegForPDF(image, background: .white) }
            context.saveGState()
            context.translateBy(x: frame.minX, y: frame.maxY); context.scaleBy(x: 1, y: -1)
            context.draw(image, in: CGRect(origin: .zero, size: frame.size)); context.restoreGState()
        } else if !isPreview { throw PolarError.invalidProject("No se puede leer la plantilla importada.") }
        for index in template.regions.indices where !template.regions[index].isTransparent { try photo(index) }
        for (index, card) in cards.enumerated() {
            let slot = page * project.settings.capacity + index
            let assigned = project.placements.indices.contains(slot) && project.placements[slot] != nil
            if isPreview { stroke(card, color: NSColor(white: 0.68, alpha: 0.8), width: 0.5, context: context) }
            if project.settings.cutGuides && (isPreview || assigned) { drawGuides(card, style: project.settings.cutStyle, paper: paperRect(project.settings), excluding: cards, context: context) }
        }
    }

    private static func drawPhoto(_ photo: PhotoAsset?, placement: PhotoPlacement?, in rect: CGRect, radius: CGFloat,
                                  accent: NSColor, background: NSColor, context: CGContext, isPreview: Bool, isPDF: Bool, optimizePhotos: Bool = true,
                                  look: PhotoLook, card: CGRect, cardIndex: Int, clipPhoto: Bool = true) throws {
        context.saveGState()
        defer { context.restoreGState() }
        guard context.boundingBoxOfClipPath.intersects(rect) else { return }
        if clipPhoto {
            context.addPath(CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil))
            context.clip()
        }
        guard let photo, let placement else {
            if isPreview { placeholder(rect, accent: accent, context: context) }
            else { fill(rect, color: .white, context: context) }
            return
        }
        guard let source = CGImageSourceCreateWithURL(photo.url as CFURL, nil) else {
            if isPreview { placeholder(rect, accent: accent, context: context); return }
            throw PolarError.unreadablePhoto(photo.name)
        }
        let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as NSDictionary?
        var sourceWidth = CGFloat((properties?[kCGImagePropertyPixelWidth] as? NSNumber)?.doubleValue ?? Double(photo.pixelWidth))
        var sourceHeight = CGFloat((properties?[kCGImagePropertyPixelHeight] as? NSNumber)?.doubleValue ?? Double(photo.pixelHeight))
        if (5...8).contains(properties?[kCGImagePropertyOrientation] as? Int ?? 1) { swap(&sourceWidth, &sourceHeight) }
        let rotated = placement.quarterTurns % 2 != 0
        let density: CGFloat = isPreview ? 2 : 300 / 72
        let requiredScale = max(rect.width / (rotated ? sourceHeight : sourceWidth),
                                rect.height / (rotated ? sourceWidth : sourceHeight)) * CGFloat(placement.zoom) * density
        let longest = max(sourceWidth, sourceHeight)
        let maxPixels = max(1, Int(ceil(min(longest, longest * requiredScale, isPreview ? 1600 : longest))))
        guard var image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: maxPixels,
                kCGImageSourceShouldCacheImmediately: true
              ] as CFDictionary)
        else {
            if isPreview { placeholder(rect, accent: accent, context: context); return }
            throw PolarError.unreadablePhoto(photo.name)
        }
        let width = CGFloat(image.width), height = CGFloat(image.height)
        let fit = PhotoFit.compute(box: rect.size, source: CGSize(width: width, height: height), placement: placement)
        if !look.isNeutral || isPDF {
            // Render in card coordinates before adding grain, so zoom and output resolution never change its seed/grid.
            let density: CGFloat = isPreview ? 2 : 300 / 72
            let w = max(1, Int(ceil(rect.width * density))), h = max(1, Int(ceil(rect.height * density)))
            guard let filtered = bitmap(width: w, height: h) else { throw PolarError.exportFailed }
            filtered.translateBy(x: 0, y: CGFloat(h)); filtered.scaleBy(x: CGFloat(w) / rect.width, y: -CGFloat(h) / rect.height)
            filtered.translateBy(x: fit.center.x, y: fit.center.y)
            filtered.rotate(by: CGFloat(placement.quarterTurns) * .pi / 2)
            filtered.scaleBy(x: fit.scale, y: -fit.scale)
            filtered.draw(image, in: CGRect(x: -width / 2, y: -height / 2, width: width, height: height))
            guard let cropped = filtered.makeImage() else { throw PolarError.exportFailed }
            image = try PhotoFilters.apply(cropped, look: look, seed: PhotoFilters.seed(assetID: photo.id.uuidString, card: cardIndex),
                                           origin: CGPoint(x: rect.minX - card.minX, y: rect.minY - card.minY), pointsPerPixel: CGSize(width: rect.width / CGFloat(w), height: rect.height / CGFloat(h)))
            if isPDF && optimizePhotos { image = try jpegForPDF(image, background: background) }
            context.translateBy(x: rect.minX, y: rect.maxY); context.scaleBy(x: 1, y: -1)
            context.draw(image, in: CGRect(origin: .zero, size: rect.size))
            return
        }
        context.translateBy(x: rect.minX + fit.center.x, y: rect.minY + fit.center.y)
        context.rotate(by: CGFloat(placement.quarterTurns) * .pi / 2)
        context.scaleBy(x: fit.scale, y: -fit.scale)
        context.draw(image, in: CGRect(x: -width / 2, y: -height / 2, width: width, height: height))
    }

    private static func jpegForPDF(_ image: CGImage, background: NSColor) throws -> CGImage {
        // Composite alpha against the card and convert to sRGB before JPEG's opaque encoding.
        guard let context = CGContext(data: nil, width: image.width, height: image.height, bitsPerComponent: 8,
                                      bytesPerRow: 0, space: CGColorSpace(name: CGColorSpace.sRGB)!,
                                      bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else { throw PolarError.exportFailed }
        let bounds = CGRect(x: 0, y: 0, width: image.width, height: image.height)
        context.setFillColor(background.cgColor)
        context.fill(bounds)
        context.draw(image, in: bounds)
        let data = NSMutableData()
        guard let opaque = context.makeImage(),
              let destination = CGImageDestinationCreateWithData(data, "public.jpeg" as CFString, 1, nil)
        else { throw PolarError.exportFailed }
        CGImageDestinationAddImage(destination, opaque, [kCGImageDestinationLossyCompressionQuality: 0.94] as CFDictionary)
        guard CGImageDestinationFinalize(destination), let provider = CGDataProvider(data: data),
              let compressed = CGImage(jpegDataProviderSource: provider, decode: nil, shouldInterpolate: true, intent: .defaultIntent)
        else { throw PolarError.exportFailed }
        return compressed
    }

    private static func placeholder(_ rect: CGRect, accent: NSColor, context: CGContext) {
        let top = NSColor(red: 0.91, green: 0.88, blue: 0.82, alpha: 1)
        let bottom = accent.blended(withFraction: 0.82, of: .white) ?? top
        if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: [top.cgColor, bottom.cgColor] as CFArray, locations: [0, 1]) {
            context.drawLinearGradient(gradient, start: CGPoint(x: rect.midX, y: rect.minY), end: CGPoint(x: rect.midX, y: rect.maxY), options: [])
        }
        let sun = min(rect.width, rect.height) * 0.17
        fill(CGRect(x: rect.minX + rect.width * 0.66, y: rect.minY + rect.height * 0.17, width: sun, height: sun), color: NSColor(white: 1, alpha: 0.8), radius: sun / 2, context: context)
        context.setFillColor((accent.blended(withFraction: 0.50, of: .white) ?? accent).cgColor)
        context.move(to: CGPoint(x: rect.minX, y: rect.maxY))
        context.addLine(to: CGPoint(x: rect.minX, y: rect.minY + rect.height * 0.71))
        context.addCurve(to: CGPoint(x: rect.maxX, y: rect.minY + rect.height * 0.77), control1: CGPoint(x: rect.minX + rect.width * 0.28, y: rect.minY + rect.height * 0.48), control2: CGPoint(x: rect.minX + rect.width * 0.6, y: rect.maxY))
        context.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        context.closePath()
        context.fillPath()
        symbol("photo", in: CGRect(x: rect.midX - sun * 0.4, y: rect.midY - sun * 0.4, width: sun * 0.8, height: sun * 0.8), color: NSColor(white: 1, alpha: 0.78))
    }

    private static func drawCalendar(in card: CGRect, month: Int, settings: PrintSettings, accent: NSColor, context: CGContext) {
        let monthNames = ["ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"]
        text(monthNames[month - 1], in: CGRect(x: card.minX + card.width * 0.055, y: card.minY + card.height * 0.67, width: card.width * 0.235, height: card.height * 0.12), size: card.width * 0.08, weight: .heavy)
        text(String(settings.calendarYear), in: CGRect(x: card.minX + card.width * 0.055, y: card.minY + card.height * 0.80, width: card.width * 0.235, height: card.height * 0.055), size: card.width * 0.062, color: accent, weight: .medium)
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        guard let first = calendar.date(from: DateComponents(year: settings.calendarYear, month: month, day: 1)),
              let days = calendar.range(of: .day, in: .month, for: first) else { return }
        let offset = (calendar.component(.weekday, from: first) + 5) % 7
        let special = calendar.dateComponents([.year, .month, .day], from: settings.specialDate)
        let grid = CGRect(x: card.minX + card.width * 0.325, y: card.minY + card.height * 0.672, width: card.width * 0.62, height: card.height * 0.285)
        let cellWidth = grid.width / 7, cellHeight = grid.height / 7
        for (column, day) in ["L", "M", "M", "J", "V", "S", "D"].enumerated() {
            text(day, in: CGRect(x: grid.minX + CGFloat(column) * cellWidth, y: grid.minY, width: cellWidth, height: cellHeight), size: cellHeight * 0.68, color: column == 6 ? accent : .gray, weight: .bold)
        }
        for day in days {
            let index = offset + day - 1, column = index % 7, row = index / 7 + 1
            let cell = CGRect(x: grid.minX + CGFloat(column) * cellWidth, y: grid.minY + CGFloat(row) * cellHeight, width: cellWidth, height: cellHeight)
            let highlight = settings.highlightDate && special.year == settings.calendarYear && special.month == month && special.day == day
            if highlight { fill(cell.insetBy(dx: cellWidth * 0.13, dy: 0), color: accent, radius: cellHeight / 2, context: context) }
            text(String(day), in: cell, size: cellHeight * 0.66, color: highlight ? .white : (column == 6 ? accent : .black), weight: highlight ? .bold : .regular)
        }
    }

    private static func drawPlayer(in rect: CGRect, color: NSColor, context: CGContext, volume: Bool) {
        line(from: CGPoint(x: rect.minX, y: rect.minY), to: CGPoint(x: rect.maxX, y: rect.minY), color: color.withAlphaComponent(0.55), width: 0.8, context: context)
        fill(CGRect(x: rect.minX + rect.width * 0.58, y: rect.minY - 1.4, width: 2.8, height: 2.8), color: color, radius: 1.4, context: context)
        text("1:03", in: CGRect(x: rect.minX, y: rect.minY + 2, width: rect.width * 0.2, height: rect.height * 0.2), size: rect.height * 0.12, color: color, alignment: .left)
        text("2:22", in: CGRect(x: rect.maxX - rect.width * 0.2, y: rect.minY + 2, width: rect.width * 0.2, height: rect.height * 0.2), size: rect.height * 0.12, color: color, alignment: .right)
        let size = min(rect.width * 0.12, rect.height * 0.30)
        for (x, name) in [(CGFloat(0.22), "backward.fill"), (CGFloat(0.5), "pause.fill"), (CGFloat(0.78), "forward.fill")] {
            symbol(name, in: CGRect(x: rect.minX + rect.width * x - size / 2, y: rect.minY + rect.height * 0.40, width: size, height: size), color: color)
        }
        if volume {
            line(from: CGPoint(x: rect.minX + rect.width * 0.1, y: rect.maxY), to: CGPoint(x: rect.maxX - rect.width * 0.1, y: rect.maxY), color: color.withAlphaComponent(0.7), width: 0.8, context: context)
            fill(CGRect(x: rect.minX + rect.width * 0.80, y: rect.maxY - 2.5, width: 5, height: 5), color: color, radius: 2.5, context: context)
            symbol("speaker.fill", in: CGRect(x: rect.minX, y: rect.maxY - 3, width: 5, height: 6), color: color)
            symbol("speaker.wave.2.fill", in: CGRect(x: rect.maxX - 5, y: rect.maxY - 3, width: 5, height: 6), color: color)
        }
    }

    private static func drawQR(_ value: String, in rect: CGRect, context: CGContext, isPreview: Bool) throws {
        guard let filter = CIFilter(name: "CIQRCodeGenerator") else { throw PolarError.exportFailed }
        filter.setValue(Data(value.utf8), forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        guard let output = filter.outputImage else {
            if isPreview { return }
            throw PolarError.invalidProject("El enlace de la canción es demasiado largo para el QR.")
        }
        guard let image = CIContext().createCGImage(output, from: output.extent) else { throw PolarError.exportFailed }
        fill(rect, color: .white, context: context)
        let inset = rect.width * 0.10
        let target = rect.insetBy(dx: inset, dy: inset)
        context.saveGState()
        context.interpolationQuality = .none
        context.translateBy(x: target.minX, y: target.maxY)
        context.scaleBy(x: 1, y: -1)
        context.draw(image, in: CGRect(origin: .zero, size: target.size))
        context.restoreGState()
    }

    private static func drawPerforations(_ rect: CGRect, vertical: Bool, context: CGContext) {
        let count = vertical ? 30 : 35
        for i in 0..<count {
            if vertical {
                let height = rect.height / CGFloat(count) * 0.68
                let y = rect.minY + (CGFloat(i) + 0.15) * rect.height / CGFloat(count)
                for x in [rect.minX + rect.width * 0.03, rect.maxX - rect.width * 0.10] {
                    fill(CGRect(x: x, y: y, width: rect.width * 0.07, height: height), color: .white, context: context)
                }
            } else {
                let width = rect.width / CGFloat(count) * 0.57
                let x = rect.minX + (CGFloat(i) + 0.22) * rect.width / CGFloat(count)
                for y in [rect.minY + rect.height * 0.04, rect.maxY - rect.height * 0.115] {
                    fill(CGRect(x: x, y: y, width: width, height: rect.height * 0.075), color: .white, context: context)
                }
            }
        }
    }

    private static func drawGuides(_ rect: CGRect, style: CutStyle, paper: CGRect, excluding cards: [CGRect], context: CGContext) {
        let gray = NSColor(white: 0.64, alpha: 1)
        if style == .lines {
            context.saveGState()
            context.setLineDash(phase: 0, lengths: [3, 3])
            stroke(rect, color: gray, width: 0.35, context: context)
            context.restoreGState()
            return
        }
        context.saveGState()
        defer { context.restoreGState() }
        // ponytail: at most 64 regions; use a union mask if larger templates become necessary.
        for card in cards { context.addRect(paper); context.addRect(card); context.clip(using: .evenOdd) }
        for x in [rect.minX, rect.maxX] {
            line(from: CGPoint(x: x, y: rect.minY - 5), to: CGPoint(x: x, y: rect.minY - 1.5), color: gray, width: 0.35, context: context)
            line(from: CGPoint(x: x, y: rect.maxY + 1.5), to: CGPoint(x: x, y: rect.maxY + 5), color: gray, width: 0.35, context: context)
        }
        for y in [rect.minY, rect.maxY] {
            line(from: CGPoint(x: rect.minX - 5, y: y), to: CGPoint(x: rect.minX - 1.5, y: y), color: gray, width: 0.35, context: context)
            line(from: CGPoint(x: rect.maxX + 1.5, y: y), to: CGPoint(x: rect.maxX + 5, y: y), color: gray, width: 0.35, context: context)
        }
    }

    private static func color(_ hex: String) -> NSColor {
        let value = UInt32(hex, radix: 16) ?? 0x92394A
        return NSColor(red: CGFloat((value >> 16) & 255) / 255, green: CGFloat((value >> 8) & 255) / 255, blue: CGFloat(value & 255) / 255, alpha: 1)
    }

    private static func fill(_ rect: CGRect, color: NSColor, radius: CGFloat = 0, context: CGContext) {
        context.setFillColor(color.cgColor)
        context.addPath(CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil))
        context.fillPath()
    }

    private static func stroke(_ rect: CGRect, color: NSColor, width: CGFloat, radius: CGFloat = 0, context: CGContext) {
        context.setStrokeColor(color.cgColor)
        context.setLineWidth(width)
        context.addPath(CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil))
        context.strokePath()
    }

    private static func line(from start: CGPoint, to end: CGPoint, color: NSColor, width: CGFloat, context: CGContext) {
        context.setStrokeColor(color.cgColor)
        context.setLineWidth(width)
        context.move(to: start)
        context.addLine(to: end)
        context.strokePath()
    }

    private static func text(_ value: String, in rect: CGRect, size: CGFloat, color: NSColor = .black,
                             weight: NSFont.Weight = .regular, alignment: NSTextAlignment = .center,
                             appearance: TextAppearance? = nil, card: CGRect? = nil, photos: [CGRect] = []) {
        guard appearance?.visible != false, !value.isEmpty else { return }
        let target = rect.offsetBy(dx: appearance?.offsetX ?? 0, dy: appearance?.offsetY ?? 0)
        let paragraph = NSMutableParagraphStyle()
        switch appearance?.alignment ?? .automatic {
        case .automatic: paragraph.alignment = alignment
        case .left: paragraph.alignment = .left
        case .center: paragraph.alignment = .center
        case .right: paragraph.alignment = .right
        }
        paragraph.lineBreakMode = appearance == nil ? .byTruncatingTail : .byWordWrapping
        var pointSize = appearance.map { $0.size > 0 ? CGFloat($0.size) : max(6, size) } ?? size
        let chosenColor = appearance.flatMap { $0.hex.isEmpty ? nil : self.color($0.hex) } ?? color
        func font(_ pointSize: CGFloat) -> NSFont {
            var result = appearance.flatMap { FontCatalog.font($0.fontName, size: pointSize) }
                ?? NSFont.systemFont(ofSize: pointSize, weight: weight)
            if appearance?.bold == true || (appearance?.fontName != ".System" && weight != .regular) { result = NSFontManager.shared.convert(result, toHaveTrait: .boldFontMask) }
            if appearance?.italic == true { result = NSFontManager.shared.convert(result, toHaveTrait: .italicFontMask) }
            return result
        }
        func attributed(_ pointSize: CGFloat) -> NSAttributedString {
            NSAttributedString(string: value, attributes: [.font: font(pointSize), .foregroundColor: chosenColor, .paragraphStyle: paragraph])
        }
        if appearance != nil {
            for _ in 0..<16 {
                let measured = attributed(pointSize).boundingRect(with: CGSize(width: max(1, target.width), height: .greatestFiniteMagnitude),
                                                                  options: [.usesLineFragmentOrigin, .usesFontLeading])
                if measured.height <= target.height && measured.width <= target.width || pointSize <= 6 { break }
                pointSize = max(6, pointSize * 0.88)
            }
        }
        guard let context = NSGraphicsContext.current?.cgContext else { return }
        context.saveGState()
        defer { context.restoreGState() }
        if let card {
            context.addRect(card)
            for photo in photos { context.addRect(photo) }
            context.clip(using: .evenOdd)
        }
        context.clip(to: target)
        attributed(pointSize).draw(with: target, options: [.usesLineFragmentOrigin, .usesFontLeading])
    }

    private static func symbol(_ name: String, in rect: CGRect, color: NSColor) {
        guard let image = NSImage(systemSymbolName: name, accessibilityDescription: nil)?.withSymbolConfiguration(
            NSImage.SymbolConfiguration(paletteColors: [color])) else { return }
        image.draw(in: rect, from: .zero, operation: .sourceOver, fraction: 1, respectFlipped: true, hints: nil)
    }
}
