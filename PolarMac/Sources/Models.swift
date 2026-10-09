import Foundation
import CoreGraphics

enum PaperSize: String, Codable, CaseIterable, Identifiable, Sendable {
    case letter, oficio, legal, a4, a3, photo4x6, photo5x7, custom
    var id: String { rawValue }
    var name: String {
        switch self {
        case .letter: return "Carta"
        case .oficio: return "Oficio"
        case .legal: return "Legal"
        case .a4: return "A4"
        case .a3: return "A3"
        case .photo4x6: return "Foto 4 × 6 pulgadas"
        case .photo5x7: return "Foto 5 × 7 pulgadas"
        case .custom: return "Personalizado"
        }
    }
}

enum PaperOrientation: String, Codable, CaseIterable, Identifiable, Sendable {
    case portrait, landscape
    var id: String { rawValue }
    var name: String { self == .portrait ? "Vertical" : "Horizontal" }
}

enum CardFormat: String, Codable, CaseIterable, Identifiable, Sendable {
    case original, portrait, landscape, square, fill
    var id: String { rawValue }
    var name: String {
        switch self {
        case .original: return "Del diseño"
        case .portrait: return "Vertical"
        case .landscape: return "Horizontal"
        case .square: return "Cuadrado"
        case .fill: return "Llenar espacio"
        }
    }
}

enum CutStyle: String, Codable, CaseIterable, Identifiable, Sendable {
    case corners, lines
    var id: String { rawValue }
    var name: String { self == .corners ? "Marcas en esquinas" : "Líneas completas" }
}

enum TextRole: String, Codable, CaseIterable, Identifiable, Sendable {
    case title, subtitle, caption, song, artist, date
    var id: String { rawValue }
    var name: String {
        switch self {
        case .title: return "Título"
        case .subtitle: return "Subtítulo"
        case .caption: return "Pie de foto"
        case .song: return "Canción"
        case .artist: return "Artista"
        case .date: return "Fecha"
        }
    }
}

enum DateSource: String, Codable, CaseIterable, Identifiable, Sendable {
    case none, photo, chosen
    var id: String { rawValue }
    var name: String {
        switch self {
        case .none: return "Sin fecha"
        case .photo: return "De la foto"
        case .chosen: return "Elegir fecha"
        }
    }
}

enum DateStyle: String, Codable, CaseIterable, Identifiable, Sendable {
    case dayMonthYear, numeric, monthYear
    var id: String { rawValue }
    var name: String {
        switch self {
        case .dayMonthYear: return "14 feb 2026"
        case .numeric: return "14.02.26"
        case .monthYear: return "febrero 2026"
        }
    }
}

enum TextAlignment: String, Codable, CaseIterable, Identifiable, Sendable {
    case automatic, left, center, right
    var id: String { rawValue }
    var name: String {
        switch self {
        case .automatic: return "Del diseño"
        case .left: return "Izquierda"
        case .center: return "Centro"
        case .right: return "Derecha"
        }
    }
}

struct TextAppearance: Codable, Equatable, Sendable {
    var fontName = ".System"
    var size: Double = 0
    var hex = ""
    var bold = false
    var italic = false
    var alignment: TextAlignment = .automatic
    var offsetX: Double = 0
    var offsetY: Double = 0
    var visible = true
}

enum TemplateStyle: String, Codable, CaseIterable, Identifiable, Sendable {
    case polaroid, mini, spotify, playerRed, playerGray, ticket, filmVertical, filmHorizontal, calendar, instagram, custom
    case borderless, square, postcard, botanical, celebration, pets, heart, editorial, imported
    var id: String { rawValue }
    var name: String {
        switch self {
        case .polaroid: return "Polaroid"
        case .mini: return "Instantánea mini"
        case .spotify: return "Foto + canción"
        case .playerRed: return "Reproductor rojo"
        case .playerGray: return "Reproductor horizontal"
        case .ticket: return "Boleto vintage"
        case .filmVertical: return "Película vertical"
        case .filmHorizontal: return "Película horizontal"
        case .calendar: return "Calendario"
        case .instagram: return "Instagram"
        case .custom: return "Mi diseño"
        case .borderless: return "Sin bordes"
        case .square: return "Cuadrada"
        case .postcard: return "Postal"
        case .botanical: return "Botánico"
        case .celebration: return "Celebración"
        case .pets: return "Mi mascota"
        case .heart: return "Corazón"
        case .editorial: return "Editorial"
        case .imported: return "Plantilla importada"
        }
    }
    var symbol: String {
        switch self {
        case .polaroid, .mini: return "photo.artframe"
        case .spotify: return "music.note"
        case .playerRed, .playerGray: return "play.rectangle"
        case .ticket: return "ticket"
        case .filmVertical, .filmHorizontal: return "film"
        case .calendar: return "calendar"
        case .instagram: return "heart.square"
        case .custom: return "slider.horizontal.3"
        case .borderless: return "photo.fill"
        case .square: return "square"
        case .postcard: return "envelope"
        case .botanical: return "leaf"
        case .celebration: return "sparkles"
        case .pets: return "pawprint"
        case .heart: return "heart"
        case .editorial: return "doc.richtext"
        case .imported: return "photo.badge.plus"
        }
    }
    var grid: (Int, Int) {
        switch self {
        case .polaroid, .mini, .spotify, .playerRed: return (3, 3)
        case .playerGray: return (2, 5)
        case .ticket: return (1, 4)
        case .filmVertical: return (2, 1)
        case .filmHorizontal: return (1, 6)
        case .calendar, .instagram: return (3, 4)
        case .custom, .borderless, .postcard, .botanical, .pets, .heart, .editorial: return (2, 3)
        case .square, .celebration: return (3, 3)
        case .imported: return (1, 1)
        }
    }
    var photosPerCard: Int { self == .filmVertical || self == .filmHorizontal ? 5 : 1 }
    var suggestedLook: String? { self == .filmVertical || self == .filmHorizontal ? "bw" : nil }
    var supportsDate: Bool { ![Self.filmVertical, .filmHorizontal, .calendar, .imported].contains(self) }
    var aspect: CGFloat {
        switch self {
        case .polaroid: return 0.70
        case .mini: return 0.667
        case .spotify: return 0.75
        case .playerRed: return 0.66
        case .playerGray: return 1.9
        case .ticket: return 2.27
        case .filmVertical: return 0.292
        case .filmHorizontal: return 5.3
        case .calendar: return 0.683
        case .instagram: return 0.705
        case .custom: return 0.80
        case .borderless: return 0.75
        case .square: return 1
        case .postcard: return 1.5
        case .botanical, .heart, .editorial: return 0.75
        case .celebration: return 0.70
        case .pets: return 0.80
        case .imported: return 0.75
        }
    }
}

struct PrintSettings: Codable, Equatable, Sendable {
    var photoLook: PhotoLook?
    var style: TemplateStyle = .polaroid
    var columns = 3
    var rows = 3
    var margin: Double = 24
    var gap: Double = 12
    var title = "Nuestros momentos"
    var subtitle = "Tú y yo"
    var caption = "Una historia para guardar"
    var song = "Nuestra canción"
    var artist = "Artista"
    var songURL = ""
    var accentHex = "92394A"
    var cutGuides = true
    var calendarYear = Calendar.current.component(.year, from: Date())
    var specialDate = Date()
    var highlightDate = true
    var roundedPhotos = false
    var paperSize: PaperSize = .letter
    var orientation: PaperOrientation = .portrait
    var customWidthMM: Double = 215.9
    var customHeightMM: Double = 279.4
    var cardFormat: CardFormat = .original
    var importedTemplate: ImportedTemplate?
    var textStyles: [String: TextAppearance] = [:]
    var extraTextStyles: [String: TextAppearance] = [:]
    var dateSource: DateSource = .none
    var chosenDate: Date?
    var dateStyle: DateStyle = .dayMonthYear
    var drawBorders = false
    var cutStyle: CutStyle = .corners
    var capacity: Int {
        if style == .imported { return max(1, importedTemplate?.regions.count ?? 1) }
        guard (1...4).contains(columns), (1...6).contains(rows) else { return 1 }
        return columns * rows * style.photosPerCard
    }
    var paperSizePoints: CGSize {
        let size: CGSize
        switch paperSize {
        case .letter: size = CGSize(width: 612, height: 792)
        case .oficio: size = CGSize(width: 216 * 72 / 25.4, height: 340 * 72 / 25.4)
        case .legal: size = CGSize(width: 612, height: 1008)
        case .a4: size = CGSize(width: 210 * 72 / 25.4, height: 297 * 72 / 25.4)
        case .a3: size = CGSize(width: 297 * 72 / 25.4, height: 420 * 72 / 25.4)
        case .photo4x6: size = CGSize(width: 288, height: 432)
        case .photo5x7: size = CGSize(width: 360, height: 504)
        case .custom: size = CGSize(width: customWidthMM * 72 / 25.4, height: customHeightMM * 72 / 25.4)
        }
        return orientation == .portrait ? size : CGSize(width: size.height, height: size.width)
    }
    var paperDescription: String {
        let size = paperSizePoints
        return "\(paperSize.name) · \(Int((size.width * 25.4 / 72).rounded())) × \(Int((size.height * 25.4 / 72).rounded())) mm · \(orientation.name)"
    }
    var cardAspect: CGFloat? {
        switch cardFormat {
        case .original: return style.aspect
        case .portrait: return 0.70
        case .landscape: return 1.40
        case .square: return 1
        case .fill: return nil
        }
    }
    func text(_ role: TextRole) -> String {
        switch role {
        case .title: return title
        case .subtitle: return subtitle
        case .caption: return caption
        case .song: return song
        case .artist: return artist
        case .date: return ""
        }
    }
    func textStyle(_ role: TextRole) -> TextAppearance {
        (role == .date ? extraTextStyles[role.rawValue] : textStyles[role.rawValue]) ?? TextAppearance()
    }
    mutating func setTextStyle(_ role: TextRole, _ appearance: TextAppearance) {
        if role == .date { extraTextStyles[role.rawValue] = appearance }
        else { textStyles[role.rawValue] = appearance }
    }

    init() {}
    enum CodingKeys: String, CodingKey {
        case style, columns, rows, margin, gap, title, subtitle, caption, song, artist, songURL, accentHex,
             cutGuides, calendarYear, specialDate, highlightDate, roundedPhotos, paperSize, orientation,
             customWidthMM, customHeightMM, cardFormat, importedTemplate, textStyles, drawBorders, cutStyle,
             extraTextStyles, dateSource, chosenDate, dateStyle, photoLook
    }
    init(from decoder: Decoder) throws {
        self.init()
        let c = try decoder.container(keyedBy: CodingKeys.self)
        style = try c.decodeIfPresent(TemplateStyle.self, forKey: .style) ?? style
        columns = try c.decodeIfPresent(Int.self, forKey: .columns) ?? columns
        rows = try c.decodeIfPresent(Int.self, forKey: .rows) ?? rows
        margin = try c.decodeIfPresent(Double.self, forKey: .margin) ?? margin
        gap = try c.decodeIfPresent(Double.self, forKey: .gap) ?? gap
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? title
        subtitle = try c.decodeIfPresent(String.self, forKey: .subtitle) ?? subtitle
        caption = try c.decodeIfPresent(String.self, forKey: .caption) ?? caption
        song = try c.decodeIfPresent(String.self, forKey: .song) ?? song
        artist = try c.decodeIfPresent(String.self, forKey: .artist) ?? artist
        songURL = try c.decodeIfPresent(String.self, forKey: .songURL) ?? songURL
        accentHex = try c.decodeIfPresent(String.self, forKey: .accentHex) ?? accentHex
        cutGuides = try c.decodeIfPresent(Bool.self, forKey: .cutGuides) ?? cutGuides
        calendarYear = try c.decodeIfPresent(Int.self, forKey: .calendarYear) ?? calendarYear
        specialDate = try c.decodeIfPresent(Date.self, forKey: .specialDate) ?? specialDate
        highlightDate = try c.decodeIfPresent(Bool.self, forKey: .highlightDate) ?? highlightDate
        roundedPhotos = try c.decodeIfPresent(Bool.self, forKey: .roundedPhotos) ?? roundedPhotos
        paperSize = try c.decodeIfPresent(PaperSize.self, forKey: .paperSize) ?? paperSize
        orientation = try c.decodeIfPresent(PaperOrientation.self, forKey: .orientation) ?? orientation
        customWidthMM = try c.decodeIfPresent(Double.self, forKey: .customWidthMM) ?? customWidthMM
        customHeightMM = try c.decodeIfPresent(Double.self, forKey: .customHeightMM) ?? customHeightMM
        cardFormat = try c.decodeIfPresent(CardFormat.self, forKey: .cardFormat) ?? cardFormat
        importedTemplate = try c.decodeIfPresent(ImportedTemplate.self, forKey: .importedTemplate)
        textStyles = try c.decodeIfPresent([String: TextAppearance].self, forKey: .textStyles) ?? textStyles
        extraTextStyles = try c.decodeIfPresent([String: TextAppearance].self, forKey: .extraTextStyles) ?? extraTextStyles
        dateSource = try c.decodeIfPresent(DateSource.self, forKey: .dateSource) ?? dateSource
        chosenDate = try c.decodeIfPresent(Date.self, forKey: .chosenDate)
        dateStyle = try c.decodeIfPresent(DateStyle.self, forKey: .dateStyle) ?? dateStyle
        drawBorders = try c.decodeIfPresent(Bool.self, forKey: .drawBorders) ?? drawBorders
        cutStyle = try c.decodeIfPresent(CutStyle.self, forKey: .cutStyle) ?? cutStyle
        photoLook = try c.decodeIfPresent(PhotoLook.self, forKey: .photoLook)
    }
}

struct PhotoAsset: Codable, Equatable, Identifiable, Sendable {
    var id = UUID()
    var path: String
    var pixelWidth: Int
    var pixelHeight: Int
    var takenAtEpochMs: Int64?
    var maskPath: String?
    var isBackground: Bool?
    var url: URL { URL(fileURLWithPath: path) }
    var name: String { url.deletingPathExtension().lastPathComponent }
}

struct PhotoBackground: Codable, Equatable, Sendable {
    var colorHex: String? = nil
    var imageID: UUID?
    var feather: Double = 0.2
    var shadow: Double = 0.15
}
struct PageDesign: Codable, Equatable, Sendable {
    var style: TemplateStyle
    var format: CardFormat = .original
}

struct PhotoPlacement: Codable, Equatable, Sendable {
    var assetID: UUID
    var zoom: Double = 1
    var offsetX: Double = 0
    var offsetY: Double = 0
    var quarterTurns: Int = 0
    var photoLook: PhotoLook?
    var background: PhotoBackground?
    enum CodingKeys: String, CodingKey { case assetID, zoom, offsetX, offsetY, quarterTurns, photoLook, background }
    init(assetID: UUID, zoom: Double = 1, offsetX: Double = 0, offsetY: Double = 0, quarterTurns: Int = 0, photoLook: PhotoLook? = nil, background: PhotoBackground? = nil) {
        self.assetID = assetID; self.zoom = zoom; self.offsetX = offsetX; self.offsetY = offsetY; self.quarterTurns = quarterTurns; self.photoLook = photoLook; self.background = background
    }
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.init(assetID: try c.decode(UUID.self, forKey: .assetID), zoom: try c.decodeIfPresent(Double.self, forKey: .zoom) ?? 1,
                  offsetX: try c.decodeIfPresent(Double.self, forKey: .offsetX) ?? 0, offsetY: try c.decodeIfPresent(Double.self, forKey: .offsetY) ?? 0,
                  quarterTurns: try c.decodeIfPresent(Int.self, forKey: .quarterTurns) ?? 0, photoLook: try c.decodeIfPresent(PhotoLook.self, forKey: .photoLook), background: try c.decodeIfPresent(PhotoBackground.self, forKey: .background))
    }
}

struct CardOverride: Codable, Equatable, Sendable {
    var texts: [String: String] = [:]
    var styles: [String: TextAppearance] = [:]
    var dateSource: DateSource?
    var chosenDate: Date?
    var photoLook: PhotoLook?
    var designStyle: TemplateStyle?
    var designFormat: CardFormat?
    var isEmpty: Bool { texts.isEmpty && styles.isEmpty && dateSource == nil && chosenDate == nil && photoLook == nil && designStyle == nil && designFormat == nil }
    enum CodingKeys: String, CodingKey { case texts, styles, dateSource, chosenDate, photoLook, designStyle, designFormat }
}

extension CardOverride {
    init(from decoder: Decoder) throws {
        self.init()
        let c = try decoder.container(keyedBy: CodingKeys.self)
        texts = try c.decodeIfPresent([String: String].self, forKey: .texts) ?? [:]
        styles = try c.decodeIfPresent([String: TextAppearance].self, forKey: .styles) ?? [:]
        dateSource = try c.decodeIfPresent(DateSource.self, forKey: .dateSource)
        chosenDate = try c.decodeIfPresent(Date.self, forKey: .chosenDate)
        photoLook = try c.decodeIfPresent(PhotoLook.self, forKey: .photoLook)
        designStyle = try c.decodeIfPresent(TemplateStyle.self, forKey: .designStyle)
        designFormat = try c.decodeIfPresent(CardFormat.self, forKey: .designFormat)
    }
}

enum PolarError: LocalizedError {
    case invalidProject(String), unreadablePhoto(String), exportFailed
    var errorDescription: String? {
        switch self {
        case .invalidProject(let reason): return "No se pudo abrir el proyecto. " + reason
        case .unreadablePhoto(let name): return "No se puede leer la foto «\(name)». Agrégala de nuevo si la moviste."
        case .exportFailed: return "No se pudo guardar el archivo. Revisa la carpeta de destino."
        }
    }
}

struct PolarProject: Codable, Equatable, Sendable {
    var version = 1
    var settings = PrintSettings()
    var photos: [PhotoAsset] = []
    var placements: [PhotoPlacement?] = []
    var name = ""
    var updatedAtEpochMs: Int64 = 0
    var cardOverrides: [String: CardOverride] = [:]
    var pageDesigns: [String: PageDesign] = [:]
    enum CodingKeys: String, CodingKey { case version, settings, photos, placements, name, updatedAtEpochMs, cardOverrides, pageDesigns }
    var pageCount: Int { max(1, (placements.count + settings.capacity - 1) / settings.capacity) }
    var placedCount: Int { placements.compactMap { $0 }.count }
    var cardsPerPage: Int { settings.capacity / settings.style.photosPerCard }
    func compatibleStyle(_ style: TemplateStyle) -> Bool { style != .imported && settings.style != .imported && style.photosPerCard == settings.style.photosPerCard }
    func settingsForPage(_ page: Int) -> PrintSettings {
        var s = settings
        if let design = pageDesigns[String(page)] { s.style = design.style; s.cardFormat = design.format }
        return s
    }
    func settingsForCard(_ card: Int) -> PrintSettings {
        var s = settingsForPage(card / cardsPerPage)
        if let own = cardOverrides[String(card)] { s.style = own.designStyle ?? s.style; s.cardFormat = own.designFormat ?? s.cardFormat }
        return s
    }
    mutating func setPageDesign(_ style: TemplateStyle, page: Int, format: CardFormat? = nil) {
        guard (0..<pageCount).contains(page), compatibleStyle(style) else { return }
        pageDesigns[String(page)] = PageDesign(style: style, format: format ?? settingsForPage(page).cardFormat)
        for key in cardOverrides.keys where (Int(key) ?? -1) / cardsPerPage == page {
            cardOverrides[key]?.designStyle = nil; cardOverrides[key]?.designFormat = nil
        }
        cardOverrides = cardOverrides.filter { !$0.value.isEmpty }
    }
    mutating func setCardDesign(_ style: TemplateStyle, card: Int) {
        guard compatibleStyle(style), card >= 0 else { return }
        var own = cardOverrides[String(card)] ?? CardOverride(); own.designStyle = style; cardOverrides[String(card)] = own
    }
    mutating func clearSlots(_ slots: Set<Int>) { for slot in slots where placements.indices.contains(slot) { placements[slot] = nil } }
    mutating func copySlotsToNewPage(_ slots: Set<Int>) {
        let per = settings.style.photosPerCard
        let expanded = per == 1 ? slots : Set(slots.flatMap { Array(($0 / per * per)..<($0 / per * per + per)) })
        let selected = expanded.sorted().filter { placements.indices.contains($0) && (per > 1 || placements[$0] != nil) }
        guard !selected.isEmpty, placedCount + selected.filter({ placements[$0] != nil }).count <= 2000 else { return }
        normalized(); let first = placements.count
        guard first + selected.count <= 2000 + settings.capacity - 1 else { return }
        let copies = selected.map { placements[$0] }
        placements += copies
        for (i, slot) in selected.enumerated() { let source = settingsForCard(slot / per)
            var own = cardOverrides[String(slot / per)] ?? CardOverride()
            if compatibleStyle(source.style) { own.designStyle = source.style; own.designFormat = source.cardFormat }
            if !own.isEmpty { cardOverrides[String((first+i)/per)] = own } }
        normalized()
    }
    func asset(for placement: PhotoPlacement?) -> PhotoAsset? {
        guard let placement else { return nil }
        return photos.first { $0.id == placement.assetID }
    }
    mutating func normalized() {
        let total = pageCount * settings.capacity
        if placements.count < total { placements += Array(repeating: nil, count: total - placements.count) }
    }
    mutating func selectStyle(_ style: TemplateStyle) {
        let oldPer = settings.style.photosPerCard, newPer = style.photosPerCard
        if oldPer != newPer {
            materializeCardLooks()
            var remapped: [String: CardOverride] = [:]
            for (key, value) in cardOverrides.sorted(by: { (Int($0.key) ?? 0) < (Int($1.key) ?? 0) }) {
                guard let card = Int(key), card >= 0, card <= Int(Int32.max) else { continue }
                let target = String(card * oldPer / newPer)
                if var existing = remapped[target] {
                    existing.texts.merge(value.texts) { first, _ in first }
                    existing.styles.merge(value.styles) { first, _ in first }
                    existing.dateSource = existing.dateSource ?? value.dateSource
                    existing.chosenDate = existing.chosenDate ?? value.chosenDate
                    existing.photoLook = existing.photoLook ?? value.photoLook
                    remapped[target] = existing
                } else { remapped[target] = value }
            }
            cardOverrides = remapped
        }
        pageDesigns = [:]
        for key in cardOverrides.keys { cardOverrides[key]?.designStyle = nil; cardOverrides[key]?.designFormat = nil }
        settings.style = style
        (settings.columns, settings.rows) = style.grid
        while placements.last.map({ $0 == nil }) == true { placements.removeLast() }
        normalized()
    }
    func validated() throws {
        for (key, value) in pageDesigns {
            guard let page = Int(key), (0..<pageCount).contains(page), key == String(page), compatibleStyle(value.style) else { throw PolarError.invalidProject("El diseño de una hoja no es compatible.") }
        }
        guard cardOverrides.values.allSatisfy({ $0.designStyle.map(compatibleStyle) ?? true }) else { throw PolarError.invalidProject("El diseño de una tarjeta requiere otra cantidad de fotos.") }
        try settings.photoLook?.validated()
        guard version == 1 else { throw PolarError.invalidProject("La versión del archivo no es compatible.") }
        guard (1...4).contains(settings.columns), (1...6).contains(settings.rows),
              settings.margin.isFinite, (0...60).contains(settings.margin), settings.gap.isFinite, (0...30).contains(settings.gap),
              settings.customWidthMM.isFinite, (80...600).contains(settings.customWidthMM),
              settings.customHeightMM.isFinite, (80...600).contains(settings.customHeightMM),
              (1900...2100).contains(settings.calendarYear),
              placements.count <= 2000 + settings.capacity - 1, placedCount <= 2000, photos.count <= 2000
        else { throw PolarError.invalidProject("El tamaño o la distribución no son válidos.") }
        let paper = settings.paperSizePoints
        guard paper.width > CGFloat(2 * settings.margin + Double(settings.columns - 1) * settings.gap),
              paper.height > CGFloat(2 * settings.margin + Double(settings.rows - 1) * settings.gap)
        else { throw PolarError.invalidProject("El papel es demasiado pequeño para estos márgenes y espacios.") }
        guard settings.accentHex.range(of: "^[0-9A-Fa-f]{6}$", options: .regularExpression) != nil
        else { throw PolarError.invalidProject("El color no es válido.") }
        func validateAppearance(_ text: TextAppearance) throws {
            guard text.fontName.count <= 128,
                  text.hex.isEmpty || text.hex.range(of: "^[0-9A-Fa-f]{6}$", options: .regularExpression) != nil,
                  text.size.isFinite, text.size == 0 || (6...96).contains(text.size),
                  text.offsetX.isFinite, (-60...60).contains(text.offsetX), text.offsetY.isFinite, (-60...60).contains(text.offsetY)
            else { throw PolarError.invalidProject("La apariencia de un texto no es válida.") }
        }
        for (role, text) in settings.textStyles {
            guard let known = TextRole(rawValue: role), known != .date else { throw PolarError.invalidProject("El texto no es válido.") }
            try validateAppearance(text)
        }
        for (role, text) in settings.extraTextStyles {
            guard TextRole(rawValue: role) != nil else { throw PolarError.invalidProject("El texto no es válido.") }
            try validateAppearance(text)
        }
        func validateDate(_ date: Date?) throws {
            if let date, !date.timeIntervalSinceReferenceDate.isFinite { throw PolarError.invalidProject("La fecha no es válida.") }
        }
        try validateDate(settings.chosenDate)
        for (card, override) in cardOverrides {
            try override.photoLook?.validated()
            guard let index = Int(card), index >= 0, index <= Int(Int32.max), card == String(index) else { throw PolarError.invalidProject("La tarjeta no es válida.") }
            for (role, text) in override.texts {
                guard TextRole(rawValue: role) != nil, text.utf16.count <= 500 else { throw PolarError.invalidProject("El texto de una tarjeta no es válido.") }
            }
            for (role, text) in override.styles {
                guard TextRole(rawValue: role) != nil else { throw PolarError.invalidProject("El texto no es válido.") }
                try validateAppearance(text)
            }
            try validateDate(override.chosenDate)
        }
        if settings.style == .imported && settings.importedTemplate == nil {
            throw PolarError.invalidProject("Agrega una plantilla antes de usar este diseño.")
        }
        if let template = settings.importedTemplate {
            guard !template.path.isEmpty, (1...30000).contains(template.pixelWidth), (1...30000).contains(template.pixelHeight),
                  Double(template.pixelWidth) * Double(template.pixelHeight) <= 150_000_000,
                  (1...64).contains(template.regions.count), Set(template.regions.map(\.id)).count == template.regions.count
            else { throw PolarError.invalidProject("La plantilla importada no es válida.") }
            for region in template.regions {
                guard region.x.isFinite, region.y.isFinite, region.width.isFinite, region.height.isFinite,
                      region.x >= 0, region.y >= 0, region.width > 0, region.height > 0,
                      region.x + region.width <= 1.000000001, region.y + region.height <= 1.000000001
                else { throw PolarError.invalidProject("Un espacio de la plantilla queda fuera de la imagen.") }
            }
        }
        let ids = Set(photos.map(\.id))
        guard ids.count == photos.count, photos.allSatisfy({ !$0.path.isEmpty && $0.pixelWidth > 0 && $0.pixelHeight > 0 })
        else { throw PolarError.invalidProject("La lista de fotos no es válida.") }
        for slot in placements.compactMap({ $0 }) {
            try slot.photoLook?.validated()
            if let b = slot.background {
                guard b.colorHex == nil || b.colorHex!.range(of: "^[0-9A-Fa-f]{6}$", options: .regularExpression) != nil,
                      b.imageID == nil || ids.contains(b.imageID!), b.feather.isFinite, (0...1).contains(b.feather),
                      b.shadow.isFinite, (0...1).contains(b.shadow), asset(for: slot)?.maskPath?.isEmpty == false
                else { throw PolarError.invalidProject("El fondo de una foto no es válido.") }
            }
            guard ids.contains(slot.assetID), slot.zoom.isFinite, slot.zoom > 0, slot.zoom <= 4,
                  slot.offsetX.isFinite, (-1...1).contains(slot.offsetX), slot.offsetY.isFinite, (-1...1).contains(slot.offsetY),
                  (0...3).contains(slot.quarterTurns)
            else { throw PolarError.invalidProject("El encuadre de una foto no es válido.") }
        }
    }
    func effectiveDPI(slot: Int, rect: CGRect) -> Double? {
        guard placements.indices.contains(slot), let p = placements[slot], let photo = asset(for: p) else { return nil }
        let w = p.quarterTurns % 2 == 0 ? photo.pixelWidth : photo.pixelHeight
        let h = p.quarterTurns % 2 == 0 ? photo.pixelHeight : photo.pixelWidth
        return min(Double(w) / rect.width, Double(h) / rect.height) * 72 / p.zoom
    }
}

extension PolarProject {
    init(from decoder: Decoder) throws {
        self.init()
        let c = try decoder.container(keyedBy: CodingKeys.self)
        version = try c.decodeIfPresent(Int.self, forKey: .version) ?? version
        settings = try c.decodeIfPresent(PrintSettings.self, forKey: .settings) ?? settings
        photos = try c.decodeIfPresent([PhotoAsset].self, forKey: .photos) ?? photos
        placements = try c.decodeIfPresent([PhotoPlacement?].self, forKey: .placements) ?? placements
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? name
        updatedAtEpochMs = try c.decodeIfPresent(Int64.self, forKey: .updatedAtEpochMs) ?? updatedAtEpochMs
        cardOverrides = try c.decodeIfPresent([String: CardOverride].self, forKey: .cardOverrides) ?? cardOverrides
        pageDesigns = try c.decodeIfPresent([String: PageDesign].self, forKey: .pageDesigns) ?? pageDesigns
    }
}

enum TextResolver {
    static func text(project: PolarProject, card: Int, role: TextRole, zone: TimeZone = .current) -> String {
        guard role == .date else { return project.cardOverrides[String(card)]?.texts[role.rawValue] ?? project.settings.text(role) }
        let date: Date
        switch dateSource(project: project, card: card) {
        case .none: return ""
        case .photo:
            let slot = card * project.settings.style.photosPerCard
            guard project.placements.indices.contains(slot),
                  let ms = project.asset(for: project.placements[slot])?.takenAtEpochMs else { return "" }
            date = Date(timeIntervalSince1970: Double(ms) / 1000)
        case .chosen:
            guard let chosen = project.cardOverrides[String(card)]?.chosenDate ?? project.settings.chosenDate else { return "" }
            date = chosen
        }
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = zone
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        guard let day = parts.day, let month = parts.month, let year = parts.year, (1...12).contains(month) else { return "" }
        let short = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]
        let long = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"]
        switch project.settings.dateStyle {
        case .dayMonthYear: return "\(day) \(short[month - 1]) \(year)"
        case .numeric: return String(format: "%02d.%02d.%02d", day, month, year % 100)
        case .monthYear: return "\(long[month - 1]) \(year)"
        }
    }
    static func appearance(project: PolarProject, card: Int, role: TextRole) -> TextAppearance {
        project.cardOverrides[String(card)]?.styles[role.rawValue] ?? project.settings.textStyle(role)
    }
    static func dateSource(project: PolarProject, card: Int) -> DateSource {
        project.cardOverrides[String(card)]?.dateSource ?? project.settings.dateSource
    }
}

/// Resolution tiers for a photo at its printed size. LOW < 150 ppp, FAIR < 220 ppp, GOOD otherwise.
enum PhotoQuality: Equatable {
    case low, fair, good
    static let lowDPI = 150.0, fairDPI = 220.0
    static func of(dpi: Double) -> PhotoQuality { dpi < lowDPI ? .low : (dpi < fairDPI ? .fair : .good) }
    var badge: String? { self == .low ? "Baja" : (self == .fair ? "Aceptable" : nil) }
}

/// Export quality preference (app setting; never stored in the .polar file).
enum ExportQuality: String, Codable, CaseIterable, Identifiable {
    case light, high, max
    var id: String { rawValue }
    var title: String { self == .light ? "Ligero" : (self == .high ? "Alta" : "Máxima") }
    var dpi: CGFloat { self == .light ? 200 : 300 }
    /// JPEG quality applied to photos embedded in the PDF; nil keeps rendered pixels untouched.
    var jpegQuality: Double? { self == .light ? 0.85 : (self == .high ? 0.94 : nil) }
    var imageJPEGQuality: Double { self == .light ? 0.85 : 0.94 }
    var help: String {
        switch self {
        case .light: return "Fotos a 200 ppp con compresión ligera. Archivo pequeño, ideal para enviar."
        case .high: return "Fotos a 300 ppp con compresión de alta calidad. Equilibrio recomendado para imprimir."
        case .max: return "Fotos a 300 ppp sin recomprimir. El archivo pesa más."
        }
    }
}
