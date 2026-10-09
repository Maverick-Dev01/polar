import SwiftUI
import AppKit
import ImageIO
import UniformTypeIdentifiers
import ImageIO
import PDFKit

enum MoodPreset: String, CaseIterable, Identifiable {
    case couple, friends, family, pets, travel, celebration, minimal
    var id: String { rawValue }
    var name: String {
        switch self {
        case .couple: return "Parejas"
        case .friends: return "Amigos"
        case .family: return "Familia"
        case .pets: return "Mascotas"
        case .travel: return "Viajes"
        case .celebration: return "Celebraciones"
        case .minimal: return "Minimalista"
        }
    }
    var fontName: String {
        switch self {
        case .couple: return "Georgia"
        case .friends, .travel: return "AvenirNext-Medium"
        case .family: return "Baskerville"
        case .pets: return "ChalkboardSE-Regular"
        case .celebration: return "SnellRoundhand"
        case .minimal: return ".System"
        }
    }
    var hex: String {
        switch self {
        case .couple: return "92394A"
        case .friends: return "38536F"
        case .family, .travel: return "486855"
        case .pets: return "BC8952"
        case .celebration: return "C34048"
        case .minimal: return "20242C"
        }
    }
    var phrases: (String, String, String) {
        switch self {
        case .couple: return ("Nuestros momentos", "Tú y yo", "Una historia para guardar")
        case .friends: return ("Siempre juntos", "Amigos de verdad", "Los mejores recuerdos son compartidos")
        case .family: return ("Nuestra familia", "Donde empieza todo", "El cariño que nos une")
        case .pets: return ("Mi mejor compañía", "Huellas en el corazón", "Pequeñas patas, grandes aventuras")
        case .travel: return ("Nuestra aventura", "Un lugar para recordar", "Coleccionando momentos")
        case .celebration: return ("Un día especial", "Celebremos juntos", "Un recuerdo para siempre")
        case .minimal: return ("Un instante", "Para recordar", "")
        }
    }
}

/// Tamaños rápidos del panel Texto. Auto (0) deja que la hoja ajuste el texto a su zona.
enum TextSizePreset: String, CaseIterable, Identifiable {
    case auto, small, medium, large
    var id: String { rawValue }
    var name: String { switch self { case .auto: return "Auto"; case .small: return "Chica"; case .medium: return "Mediana"; case .large: return "Grande" } }
    var points: Double { switch self { case .auto: return 0; case .small: return 9; case .medium: return 12; case .large: return 18 } }
    static func matching(_ size: Double) -> TextSizePreset? { allCases.first { $0.points == size } }
}

/// Decide cuándo un gesto de dos dedos o una flecha cambia de hoja: umbral de desplazamiento acumulado,
/// un solo cambio por gesto y una pausa mínima entre cambios (debounce).
struct SheetNavigator {
    static let threshold: CGFloat = 80
    static let debounce: TimeInterval = 0.4
    private var accumulated: CGFloat = 0
    private var lastChange: TimeInterval = -.infinity
    private var consumed = false

    /// Devuelve -1 (hoja anterior), 1 (siguiente) o nil. `ended` cierra el gesto. Con `enabled == false` se descarta todo.
    mutating func scroll(dx: CGFloat, dy: CGFloat, ended: Bool, now: TimeInterval, enabled: Bool) -> Int? {
        defer { if ended { accumulated = 0; consumed = false } }
        guard enabled else { accumulated = 0; return nil }
        guard !consumed, abs(dx) > abs(dy) else { return nil }
        accumulated += dx
        guard abs(accumulated) >= Self.threshold else { return nil }
        let direction = accumulated > 0 ? -1 : 1   // deslizar a la derecha trae la hoja anterior
        accumulated = 0; consumed = true
        guard now - lastChange >= Self.debounce else { return nil }
        lastChange = now
        return direction
    }
    /// Flechas ← (-1) y → (1); respetan la misma pausa para que mantener la tecla no salte hojas.
    mutating func key(direction: Int, now: TimeInterval, enabled: Bool) -> Int? {
        guard enabled, now - lastChange >= Self.debounce / 2 else { return nil }
        lastChange = now
        return direction
    }
}

struct FontChoice: Identifiable {
    var id: String
    var name: String
}

@MainActor final class Studio: ObservableObject {
    static let shared = Studio()
    @Published var project = PolarProject()
    @Published var inspectorTab = 0
    @Published var selectedTextRole: TextRole = .title
    @Published var designSearch = ""
    @Published var fontSearch = ""
    @Published var editingTemplate = false
    @Published var mood: MoodPreset = .couple
    let fontChoices: [FontChoice]
    @Published var dropTarget = false
    @Published var page = 0
    @Published var designScope = 0
    @Published var batchSelecting = false
    @Published var selectedSlots: Set<Int> = []
    @Published var selectedSlot = 0
    @Published var previewImage: NSImage?
    @Published var status = "Elige un diseño y agrega tus fotos."
    @Published var busy = false
    @Published var errorMessage: String?
    @Published var isDirty = false
    @Published var lastExport: URL?
    @Published var projectURL: URL?
    let library: LibraryStore
    let molds: MoldLibrary
    @Published var savedMolds: [SavedMold] = []
    @Published var moldWizard: MoldWizardState?
    @Published var libraryItems: [LibraryItem] = []
    @Published var showingLibrary = true
    @Published var showingSettings = false
    @Published var showingWelcome = false
    // Guía: centro de ayuda, modo «?» y recorrido (índice del paso en help.json, nil = sin recorrido).
    @Published var showingHelp = false
    @Published var helpArticleID: String?
    @Published var helpMode = false
    @Published var helpSelected: String?
    @Published var tourIndex: Int?
    @Published var showingFinish = false
    @Published var showingCrop = false
    @Published var comparing = false
    @Published var lookScope = 3
    @Published var lookPages: Set<Int> = []
    @Published var suggestedLook: String?
    @Published var lookUndoNotice = false
    @Published var printPDF: URL?
    @Published var saveFailed = false
    @Published var textCardScope = false
    @Published var designCategory = "Todos"
    @Published var deletedID: UUID?
    @Published var preferences: AppPreferences {
        didSet {
            do { try library.savePreferences(preferences) }
            catch { errorMessage = "No se pudieron guardar los ajustes: \(error.localizedDescription)" }
        }
    }
    private(set) var projectID = UUID()
    private var autosaveTask: Task<Void, Never>?
    private var transactionBase: PolarProject?
    var thumbnails: [TemplateStyle: NSImage] = [:]
    private var previewWork: DispatchWorkItem?
    private var undoHistory: [PolarProject] = []
    private var redoHistory: [PolarProject] = []
    var isInTransaction: Bool { transactionBase != nil }
    var canUndo: Bool { !undoHistory.isEmpty || transactionBase.map { $0 != project } == true }
    var canRedo: Bool { !redoHistory.isEmpty }

    private func runPanel(_ panel: NSSavePanel) -> NSApplication.ModalResponse {
        NSApp.activate(ignoringOtherApps: true)
        return panel.runModal()
    }

    init(storageRoot: URL? = nil) {
        library = LibraryStore(root: storageRoot)
        molds = MoldLibrary(appRoot: library.root)
        preferences = library.preferences()
        FontCatalog.registerFonts()
        fontChoices = [FontChoice(id: ".System", name: "Sistema")] + FontCatalog.bundledChoices.map { FontChoice(id: $0.id, name: $0.name) } + NSFontManager.shared.availableFonts.sorted().compactMap { name in
            guard let font = NSFont(name: name, size: 12) else { return nil }
            return FontChoice(id: name, name: font.displayName ?? name)
        }
        library.purgeTrash()
        project.normalized()
        project.name = "Nuevo diseño"
        libraryItems = library.list()
        savedMolds = molds.list()
        previewImage = PolarRenderer.preview(project: project, page: 0)
    }

    func change(_ edit: (inout PolarProject) -> Void, remember: Bool = true) {
        let previous = project
        var next = previous
        edit(&next)
        if next.settings.columns != previous.settings.columns || next.settings.rows != previous.settings.rows { next.pageDesigns = [:] }
        next.normalized()
        guard previous != next else { return }
        do { try next.validated() }
        catch { errorMessage = error.localizedDescription; return }
        project = next
        selectedSlots = selectedSlots.filter { next.placements.indices.contains($0) }
        if remember { redoHistory.removeAll() }
        if remember && transactionBase == nil {
            undoHistory.append(previous)
            if undoHistory.count > 50 { undoHistory.removeFirst() }
            redoHistory.removeAll()
        }
        page = min(page, project.pageCount - 1)
        selectedSlot = min(max(page * project.settings.capacity, selectedSlot), (page + 1) * project.settings.capacity - 1)
        if !textRoles.contains(selectedTextRole), let first = textRoles.first { selectedTextRole = first }
        if project.settings.style != .imported { editingTemplate = false }
        isDirty = true
        scheduleSave()
        refresh()
    }

    func refresh() {
        previewWork?.cancel()
        var snapshot = project
        if comparing { snapshot.setAllLooks(PhotoLook()) }
        let selectedPage = page, original = project, comparison = comparing
        let work = DispatchWorkItem { [weak self] in
            let image = PolarRenderer.preview(project: snapshot, page: selectedPage)
            DispatchQueue.main.async {
                guard let self, self.project == original, self.page == selectedPage, self.comparing == comparison else { return }
                self.previewImage = image
            }
        }
        previewWork = work
        DispatchQueue.global(qos: .userInitiated).asyncAfter(deadline: .now() + 0.05, execute: work)
    }

    func undo() {
        endEditing()
        guard let last = undoHistory.popLast() else { return }
        redoHistory.append(project); project = last; reconcile()
    }
    func redo() {
        endEditing()
        guard let next = redoHistory.popLast() else { return }
        undoHistory.append(project); project = next; reconcile()
    }
    private func reconcile() {
        page = min(page, project.pageCount - 1)
        selectedSlot = min(max(page * project.settings.capacity, selectedSlot), (page + 1) * project.settings.capacity - 1)
        if !textRoles.contains(selectedTextRole), let first = textRoles.first { selectedTextRole = first }
        if project.settings.style != .imported { editingTemplate = false }
        isDirty = true; scheduleSave(); refresh()
    }

    func chooseStyle(_ style: TemplateStyle) {
        endEditing()
        if designScope != 0 && !project.compatibleStyle(style) {
            errorMessage = "Este diseño agrupa otra cantidad de fotos. Elige Colección para reorganizar todas las hojas; tus fotos se conservarán."
            return
        }
        if style == .imported, project.settings.importedTemplate == nil { importTemplate(); return }
        change { p in
            if designScope == 0 { p.selectStyle(style) }
            else if designScope == 1 { p.setPageDesign(style, page: page) }
            else { p.setCardDesign(style, card: selectedCard) }
        }
        refresh(); status = "Diseño: \(style.name)"; suggestedLook = style.suggestedLook
    }
    var designSettings: PrintSettings { designScope == 0 ? project.settings : designScope == 1 ? project.settingsForPage(page) : project.settingsForCard(selectedCard) }
    func setDesignFormat(_ format: CardFormat) {
        change { p in
            if designScope == 0 { p.settings.cardFormat = format }
            else if designScope == 1 { p.setPageDesign(p.settingsForPage(page).style, page: page, format: format) }
            else { var own = p.cardOverrides[String(selectedCard)] ?? CardOverride(); own.designFormat = format; p.cardOverrides[String(selectedCard)] = own }
        }
    }
    func selectPhotoSlot(_ slot: Int) {
        if batchSelecting { if selectedSlots.contains(slot) { selectedSlots.remove(slot) } else { selectedSlots.insert(slot) } }
        else { selectedSlot = slot; inspectorTab = editingTemplate ? 0 : 3 }
    }
    func selectAllOnPage() { selectedSlots = Set(project.placements.indices.filter { $0 / project.settings.capacity == page && project.placements[$0] != nil }) }
    func removeSelectedPhotos() { endEditing(); change { $0.clearSlots(selectedSlots) }; selectedSlots = []; status = "Fotos quitadas de la hoja. Puedes deshacer desde la barra." }
    func copySelectedPhotos() {
        endEditing(); let first = project.pageCount
        change { $0.copySlotsToNewPage(selectedSlots) }
        if project.pageCount == first { errorMessage = "No se pudieron copiar más fotos. El límite es 2000."; return }
        selectedSlots = []; batchSelecting = false; navigate(first)
    }
    func removeBackground() {
        guard !busy, project.placements.indices.contains(selectedSlot), let photo = project.asset(for: project.placements[selectedSlot]) else { return }
        if let path = photo.maskPath, let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: path) as CFURL, nil), CGImageSourceCreateImageAtIndex(source, 0, nil) != nil {
            editBackground { _ in PhotoBackground(colorHex: "FFFFFF") }; return
        }
        endEditing(); let slot = selectedSlot, id = projectID
        let directory = library.directory(id).appendingPathComponent("photos")
        busy = true
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let path = try BackgroundRemover.mask(photo: photo, directory: directory)
                DispatchQueue.main.async {
                    guard self.projectID == id else { return }
                    self.busy = false
                    self.change { p in
                        guard p.placements.indices.contains(slot), p.placements[slot]?.assetID == photo.id, let index = p.photos.firstIndex(where: { $0.id == photo.id }) else { return }
                        p.photos[index].maskPath = path; p.placements[slot]?.background = PhotoBackground(colorHex: "FFFFFF")
                    }
                }
            } catch { DispatchQueue.main.async { if self.projectID == id { self.busy = false; self.errorMessage = "No se pudo quitar el fondo. Se conserva tu original. " + error.localizedDescription } } }
        }
    }
    func editBackground(_ edit: (PhotoBackground?) -> PhotoBackground?) { editPlacement { $0.background = edit($0.background) } }
    func addBackgroundPhoto() {
        guard !busy else { return }
        let panel = NSOpenPanel(); panel.allowedContentTypes = [.image]; panel.canChooseDirectories = false
        guard runPanel(panel) == .OK, let url = panel.url else { return }
        let result = PhotoImporter.read(urls: [url])
        guard let photo = result.photos.first, project.photos.count < 2000 else { errorMessage = "No se pudo leer el fondo o alcanzaste el límite de fotos."; return }
        change { p in
            guard p.placements.indices.contains(selectedSlot), p.placements[selectedSlot]?.background != nil else { return }
            var backdrop = photo; backdrop.isBackground = true; p.photos.append(backdrop); p.placements[selectedSlot]?.background?.imageID = photo.id
        }
    }

    func setGrid(columns: Int, rows: Int) {
        guard (1...4).contains(columns), (1...6).contains(rows), project.settings.style != .imported else { return }
        change {
            $0.settings.columns = columns; $0.settings.rows = rows
            while $0.placements.last.map({ $0 == nil }) == true { $0.placements.removeLast() }
        }
        page = 0; selectedSlot = 0; refresh()
        status = "Distribución de \(project.settings.capacity) \(project.settings.capacity == 1 ? "foto" : "fotos") por hoja."
    }

    func applyLayout(_ count: Int) {
        let landscape = project.settings.orientation == .landscape
        let grid: (Int, Int)
        switch count {
        case 1: grid = (1, 1)
        case 2: grid = landscape ? (2, 1) : (1, 2)
        case 4: grid = (2, 2)
        case 6: grid = landscape ? (3, 2) : (2, 3)
        case 8: grid = landscape ? (4, 2) : (2, 4)
        case 9: grid = (3, 3)
        case 12: grid = landscape ? (4, 3) : (3, 4)
        case 16: grid = (4, 4)
        default: return
        }
        setGrid(columns: grid.0, rows: grid.1)
    }

    var textRoles: [TextRole] {
        let base: [TextRole]
        switch project.settingsForCard(selectedCard).style {
        case .filmVertical, .filmHorizontal, .calendar, .borderless, .imported: base = []
        case .spotify, .playerRed, .playerGray, .vinyl, .cassette: base = [.song, .artist]
        case .photobooth, .collage: base = [.title, .caption]
        case .washi: base = [.caption]
        case .instagram: base = [.title, .caption]
        case .postcard, .editorial, .celebration: base = [.title, .subtitle, .caption]
        default: base = [.title, .subtitle]
        }
        return project.settingsForCard(selectedCard).style.supportsDate ? base + [.date] : base
    }

    var selectedCard: Int { selectedSlot / project.settings.style.photosPerCard }
    func textValue(_ role: TextRole) -> String {
        TextResolver.text(project: project, card: textCardScope ? selectedCard : -1, role: role)
    }
    func textAppearance(_ role: TextRole) -> TextAppearance {
        textCardScope ? TextResolver.appearance(project: project, card: selectedCard, role: role) : project.settings.textStyle(role)
    }
    func setTextValue(_ value: String, role explicitRole: TextRole? = nil) {
        let role = explicitRole ?? selectedTextRole
        let card = textCardScope ? selectedCard : nil
        change {
            let value = String(value.prefix(500))
            if let card {
                var own = $0.cardOverrides[String(card)] ?? CardOverride()
                own.texts[role.rawValue] = value
                $0.cardOverrides[String(card)] = own
                return
            }
            switch role {
            case .title: $0.settings.title = value
            case .subtitle: $0.settings.subtitle = value
            case .caption: $0.settings.caption = value
            case .song: $0.settings.song = value
            case .artist: $0.settings.artist = value
            case .date: break
            }
        }
    }
    func editText(_ edit: (inout TextAppearance) -> Void) {
        let role = selectedTextRole
        let card = textCardScope ? selectedCard : nil
        var appearance = textAppearance(role)
        edit(&appearance)
        change {
            if let card {
                var own = $0.cardOverrides[String(card)] ?? CardOverride()
                own.styles[role.rawValue] = appearance
                $0.cardOverrides[String(card)] = own
            } else { $0.settings.setTextStyle(role, appearance) }
        }
    }
    func resetTextStyle() {
        let card = textCardScope ? selectedCard : nil, role = selectedTextRole
        change {
            if let card { $0.cardOverrides[String(card)]?.styles.removeValue(forKey: role.rawValue) }
            else if role == .date { $0.settings.extraTextStyles.removeValue(forKey: "date") }
            else { $0.settings.textStyles.removeValue(forKey: role.rawValue) }
        }
    }
    func applyMood(_ value: MoodPreset) {
        mood = value
        change {
            $0.settings.accentHex = value.hex
            for role in TextRole.allCases {
                var appearance = $0.settings.textStyle(role)
                appearance.fontName = NSFont(name: value.fontName, size: 12) == nil ? ".System" : value.fontName
                appearance.hex = ""
                $0.settings.setTextStyle(role, appearance)
            }
        }
        status = "Estilo para \(value.name.lowercased()). Tus frases se conservaron."
    }
    func applySuggestedPhrases() {
        let phrase = mood.phrases
        change { $0.settings.title = phrase.0; $0.settings.subtitle = phrase.1; $0.settings.caption = phrase.2 }
        status = "Frases para \(mood.name.lowercased()) aplicadas. Puedes editarlas en Texto."
    }
    func clampPaperSize() {
        change {
            $0.settings.customWidthMM = min(600, max(80, $0.settings.customWidthMM.isFinite ? $0.settings.customWidthMM : 215.9))
            $0.settings.customHeightMM = min(600, max(80, $0.settings.customHeightMM.isFinite ? $0.settings.customHeightMM : 279.4))
        }
    }

    /// «Importar molde…» abre el asistente de 3 pasos (elegir imagen, revisar espacios, nombre y guardar).
    func importTemplate() {
        guard !busy else { return }
        endEditing()
        moldWizard = MoldWizardState()
    }
    func closeMoldWizard() { moldWizard = nil }
    func refreshMolds() { savedMolds = molds.list() }

    /// Paso 1: abre el selector de archivos y lee la imagen (huecos, formas y moldes parecidos).
    func pickMoldImage() {
        guard !busy, moldWizard != nil else { return }
        let panel = NSOpenPanel()
        panel.title = "Elige la imagen de tu molde"
        panel.prompt = "Elegir"
        panel.allowedContentTypes = [.image]
        guard runPanel(panel) == .OK, let url = panel.url else { return }
        loadMoldImage(url)
    }
    func loadMoldImage(_ url: URL) {
        guard var state = moldWizard else { return }
        busy = true; status = "Buscando espacios en el molde…"
        let library = molds
        DispatchQueue.global(qos: .userInitiated).async {
            state.load(url: url, library: library)
            DispatchQueue.main.async {
                self.busy = false
                if self.moldWizard != nil { self.moldWizard = state }
                self.status = state.errorMessage == nil ? "Revisa los espacios del molde." : "No se leyó el molde."
            }
        }
    }

    enum MoldUse { case saveOnly, currentProject, newProject }
    /// Paso 3: guarda el molde en «Mis moldes» y, según se elija, lo usa en este proyecto o en uno nuevo.
    func finishMoldWizard(_ use: MoldUse) {
        guard let state = moldWizard, state.canGoNext || state.step == .save else { return }
        do {
            let mold = try state.save(in: molds)
            refreshMolds(); moldWizard = nil
            if use == .saveOnly { status = "Molde «\(mold.nombre)» guardado en Mis moldes."; designCategory = "Mis moldes" }
            else { useMold(mold, inNewProject: use == .newProject) }
        } catch { errorMessage = error.localizedDescription }
    }
    /// «Usar el existente» del aviso de molde parecido.
    func useExistingMold(_ mold: SavedMold) { moldWizard = nil; useMold(mold, inNewProject: false) }

    /// Aplica un molde guardado. El proyecto se guarda enseguida con su propia copia de la imagen, así que borrar
    /// el molde de «Mis moldes» después no rompe este proyecto.
    func useMold(_ mold: SavedMold, inNewProject: Bool = false) {
        guard !busy else { return }
        if inNewProject, !newProject() { return }
        do {
            let template = try molds.template(for: mold)
            change {
                $0.settings.importedTemplate = template
                $0.selectStyle(.imported)
            }
            designScope = 0; page = 0; selectedSlot = 0; inspectorTab = 0; editingTemplate = false
            thumbnails.removeAll(); flush(); refresh()
            status = "Molde «\(mold.nombre)» listo. Coloca tus fotos."
        } catch { errorMessage = error.localizedDescription }
    }
    func deleteMold(_ mold: SavedMold) {
        do {
            try molds.delete(mold.id); refreshMolds()
            status = "Molde «\(mold.nombre)» quitado de Mis moldes. Tus proyectos conservan su copia."
        } catch { errorMessage = error.localizedDescription }
    }

    func editTemplateRegion(_ edit: (inout TemplateRegion) -> Void) {
        guard project.settings.style == .imported, let template = project.settings.importedTemplate else { return }
        let index = selectedSlot % project.settings.capacity
        guard template.regions.indices.contains(index) else { return }
        change {
            var region = $0.settings.importedTemplate!.regions[index]
            edit(&region); region.clamp(); region.isTransparent = false
            $0.settings.importedTemplate!.regions[index] = region
        }
    }
    func moveTemplateRegion(index: Int, dx: Double, dy: Double) {
        guard project.settings.style == .imported, let template = project.settings.importedTemplate, template.regions.indices.contains(index) else { return }
        selectedSlot = page * project.settings.capacity + index
        editTemplateRegion { $0.x += dx; $0.y += dy }
    }
    func addTemplateRegion() {
        guard let template = project.settings.importedTemplate, template.regions.count < 64 else { return }
        let oldCapacity = project.settings.capacity
        let newCapacity = oldCapacity + 1
        guard project.pageCount * newCapacity <= 2000 + newCapacity - 1 else {
            errorMessage = "No caben más huecos en este proyecto. Guarda otro diseño para continuar."
            return
        }
        change {
            var placements: [PhotoPlacement?] = []
            for start in stride(from: 0, to: $0.placements.count, by: oldCapacity) {
                placements += $0.placements[start..<min(start + oldCapacity, $0.placements.count)]
                placements.append(nil)
            }
            $0.settings.importedTemplate!.regions.append(TemplateRegion(x: 0.2, y: 0.2, width: 0.6, height: 0.6))
            $0.placements = placements
            $0.cardOverrides = Dictionary(uniqueKeysWithValues: $0.cardOverrides.compactMap { key, value in
                guard let index = Int(key) else { return nil }
                return (String((index / oldCapacity) * newCapacity + index % oldCapacity), value)
            })
        }
        selectedSlot = page * newCapacity + newCapacity - 1
        editingTemplate = true
        status = "Hueco añadido. Ajusta su posición y tamaño."
    }
    func removeTemplateRegion() {
        guard let template = project.settings.importedTemplate, template.regions.count > 1 else { return }
        let oldCapacity = project.settings.capacity, removed = selectedSlot % oldCapacity
        change {
            $0.placements = $0.placements.enumerated().filter { $0.offset % oldCapacity != removed }.map(\.element)
            $0.settings.importedTemplate!.regions.remove(at: removed)
            $0.cardOverrides = Dictionary(uniqueKeysWithValues: $0.cardOverrides.compactMap { key, value in
                guard let index = Int(key), index % oldCapacity != removed else { return nil }
                let local = index % oldCapacity
                return (String((index / oldCapacity) * (oldCapacity - 1) + local - (local > removed ? 1 : 0)), value)
            })
        }
        selectedSlot = page * project.settings.capacity
        status = "Hueco quitado. Las fotos siguen en tu galería."
    }

    func navigate(_ value: Int) {
        page = min(max(0, value), project.pageCount - 1)
        selectedSlot = page * project.settings.capacity
        refresh()
    }

    func put(_ photo: PhotoAsset) {
        guard project.placements[selectedSlot] != nil || project.placedCount < 2000 else {
            errorMessage = "Este proyecto ya contiene 2000 fotos colocadas. Crea otro diseño para agregar más."
            return
        }
        change { $0.placements[selectedSlot] = PhotoPlacement(assetID: photo.id) }
        if let next = project.placements.indices.first(where: { $0 > selectedSlot && $0 < (page + 1) * project.settings.capacity && project.placements[$0] == nil }) {
            selectedSlot = next
        }
        status = "Foto colocada. Selecciona un espacio para cambiar su encuadre."
    }

    func editPlacement(_ edit: (inout PhotoPlacement) -> Void) {
        guard project.placements.indices.contains(selectedSlot), var value = project.placements[selectedSlot] else { return }
        edit(&value)
        change { $0.placements[selectedSlot] = value }
    }

    func fillAll() {
        guard !project.photos.isEmpty else { return }
        change { p in p.placements = p.photos.filter { $0.isBackground != true }.map { PhotoPlacement(assetID: $0.id) }
            p.pageDesigns = p.pageDesigns.filter { (Int($0.key) ?? 0) < p.pageCount } }
        page = 0; selectedSlot = 0; refresh()
        status = "\(project.placedCount) \(project.placedCount == 1 ? "foto distribuida" : "fotos distribuidas") en \(project.pageCount) \(project.pageCount == 1 ? "hoja" : "hojas")."
    }

    func clearPage() {
        change {
            for index in page * $0.settings.capacity..<(page + 1) * $0.settings.capacity { $0.placements[index] = nil }
        }
        status = "Hoja vacía. Tus fotos siguen en la galería."
    }

    func addPage() {
        guard project.placements.count + project.settings.capacity <= 2000 + project.settings.capacity - 1 else {
            errorMessage = "Este proyecto alcanzó su límite de hojas. Crea otro diseño para continuar."
            return
        }
        change { $0.placements += Array(repeating: nil, count: $0.settings.capacity) }
        navigate(project.pageCount - 1)
    }

    func removePage() {
        guard project.pageCount > 1 else { clearPage(); return }
        let count = project.settings.capacity / project.settings.style.photosPerCard, first = page * count
        change { project in
            project.pageDesigns = Dictionary(uniqueKeysWithValues: project.pageDesigns.compactMap { key, value in
                guard let n = Int(key), n != page else { return nil }; return (String(n > page ? n-1 : n), value)
            })
            project.placements.removeSubrange(page * project.settings.capacity..<(page + 1) * project.settings.capacity)
            project.cardOverrides = Dictionary(uniqueKeysWithValues: project.cardOverrides.compactMap { key, value in
                guard let index = Int(key), index < first || index >= first + count else { return nil }
                return (String(index >= first + count ? index - count : index), value)
            })
        }
        navigate(page)
    }

    func addPhotos() {
        let panel = NSOpenPanel()
        panel.title = "Agrega fotos o una carpeta"
        panel.prompt = "Agregar"
        panel.allowsMultipleSelection = true
        panel.canChooseDirectories = true
        panel.allowedContentTypes = [.image]
        if runPanel(panel) == .OK { importURLs(panel.urls, fill: true) }
    }

        /// «Agregar carpeta…»: sólo carpetas; las imágenes del primer nivel, por nombre, hasta 500.
    func addFolder() {
        let panel = NSOpenPanel()
        panel.title = "Elige una carpeta con fotos"
        panel.prompt = "Agregar carpeta"
        panel.canChooseFiles = false; panel.canChooseDirectories = true; panel.allowsMultipleSelection = false
        guard runPanel(panel) == .OK, let folder = panel.url else { return }
        importFolder(folder)
    }
    func importFolder(_ folder: URL) {
        let listing = PhotoImporter.folderListing(folder)
        importURLs(listing.urls, fill: true, omitted: listing.omitted > 0, folder: true)
    }

    func importURLs(_ urls: [URL], fill: Bool, omitted: Bool = false, folder: Bool = false) {
        guard !busy else { return }
        busy = true; status = "Leyendo fotos…"
        DispatchQueue.global(qos: .userInitiated).async {
            let result = PhotoImporter.read(urls: urls)
            DispatchQueue.main.async {
                self.busy = false
                let existing = Set(self.project.photos.map(\.path))
                let remaining = 2000 - self.project.photos.count
                let incoming = result.photos.filter { !existing.contains($0.path) }
                let added = Array(incoming.prefix(max(0, remaining)))
                if !added.isEmpty {
                    self.change { project in
                        project.photos += added
                        if fill {
                            var next = self.selectedSlot
                            var placed = project.placedCount
                            for photo in added {
                                if placed >= 2000 { break }
                                if let empty = project.placements.indices.first(where: { $0 >= next && project.placements[$0] == nil }) ?? project.placements.firstIndex(where: { $0 == nil }) { next = empty }
                                else {
                                    guard project.placements.count < 2000 + project.settings.capacity - 1 else { break }
                                    next = project.placements.count; project.placements.append(nil)
                                }
                                project.placements[next] = PhotoPlacement(assetID: photo.id)
                                next += 1
                                placed += 1
                            }
                        }
                    }
                    self.thumbnails.removeAll()
                }
                self.status = "\(added.count) \(added.count == 1 ? "foto agregada" : "fotos agregadas"). \(self.project.photos.count) en tu galería."
                if folder {
                    self.status = PhotoImporter.folderStatus(added: added.count, unreadable: result.skipped.count, omitted: omitted ? 1 : 0)
                    if incoming.count <= added.count { return }
                }
                if incoming.count > added.count { self.errorMessage = "La galería admite hasta 2000 fotos. No se agregaron las restantes." }
                else if !result.skipped.isEmpty { self.errorMessage = "No se pudieron leer: " + result.skipped.prefix(5).joined(separator: ", ") }
            }
        }
    }

    @discardableResult func saveProject(asNew: Bool = false) -> Bool {
        guard flush() else { return false }
        var target = projectURL
        if asNew || target == nil {
            let panel = NSSavePanel()
            panel.title = "Guardar tu diseño"
            panel.nameFieldStringValue = project.name + ".polar"
            panel.allowedContentTypes = [UTType(exportedAs: "local.polar.project")]
            guard runPanel(panel) == .OK, let url = panel.url else { return false }
            target = url
        }
        do {
            try project.validated()
            let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
            try encoder.encode(project).write(to: target!, options: .atomic)
            projectURL = target; isDirty = false; status = "Proyecto guardado: \(target!.lastPathComponent)"
            return true
        } catch { errorMessage = error.localizedDescription; return false }
    }

    func openProject() {
        let panel = NSOpenPanel()
        panel.title = "Abrir un diseño Polar"
        panel.allowedContentTypes = [UTType(exportedAs: "local.polar.project")]
        guard runPanel(panel) == .OK, let url = panel.url else { return }
        openProject(at: url)
    }

    func openProject(at url: URL) {
        guard !busy else { return }
        do {
            var loaded = try library.read(url)
            guard confirmDiscard() else { return }
            if loaded.name.isEmpty { loaded.name = url.deletingPathExtension().lastPathComponent }
            projectID = UUID()
            project = loaded; page = 0; selectedSlot = 0; projectURL = url; isDirty = true
            designScope = 0; batchSelecting = false; selectedSlots = []
            undoHistory.removeAll(); redoHistory.removeAll(); thumbnails.removeAll(); editingTemplate = false
            textCardScope = false; showingLibrary = false; showingCrop = false; showingFinish = false; comparing = false; suggestedLook = nil; lookPages.removeAll(); flush()
            selectedTextRole = textRoles.first ?? .title; refresh()
            let missing = loaded.photos.filter { !FileManager.default.fileExists(atPath: $0.path) }.count
            status = missing == 0 ? "Diseño abierto: \(url.lastPathComponent)" : "\(missing) fotos se movieron. Agrégalas y reemplaza sus espacios."
        } catch { errorMessage = error.localizedDescription }
    }

    @discardableResult func newProject() -> Bool {
        guard !busy else { return false }
        guard confirmDiscard() else { return false }
        let photos = project.photos
        project = PolarProject(); project.photos = photos; project.normalized()
        project.name = "Nuevo diseño"; project.settings.paperSize = preferences.defaultPaper
        projectID = UUID()
        page = 0; selectedSlot = 0; projectURL = nil; isDirty = true
        designScope = 0; batchSelecting = false; selectedSlots = []
        undoHistory.removeAll(); redoHistory.removeAll(); editingTemplate = false; selectedTextRole = .title; refresh()
        textCardScope = false; showingLibrary = false; showingCrop = false; showingFinish = false; comparing = false; suggestedLook = nil; lookPages.removeAll(); flush()
        status = "Nuevo diseño. Elige un molde y coloca tus fotos."
        return true
    }

    func confirmDiscard() -> Bool {
        guard !busy else { status = "Espera a que termine la operación antes de cerrar."; return false }
        endEditing()
        if flush() { return true }
        let alert = NSAlert()
        alert.messageText = "No se guardaron los últimos cambios"
        alert.informativeText = "Revisa el espacio y los permisos. Puedes reintentar o conservar el editor abierto."
        alert.addButton(withTitle: "Reintentar"); alert.addButton(withTitle: "Descartar cambios"); alert.addButton(withTitle: "Cancelar")
        switch alert.runModal() {
        case .alertFirstButtonReturn: return flush()
        case .alertSecondButtonReturn: isDirty = false; return true
        default: return false
        }
    }

    enum ExportFormat { case pdf, png, jpeg }

    func export(_ format: ExportFormat) {
        guard !busy, project.placedCount > 0 else { return }
        let image = format == .png || format == .jpeg
        let panel = NSSavePanel()
        panel.title = image ? "Guardar esta hoja como imagen" : "Guardar todas las hojas para imprimir"
        panel.allowedContentTypes = [image ? (format == .jpeg ? .jpeg : .png) : .pdf]
        panel.nameFieldStringValue = image ? "Polar hoja \(page + 1).\(format == .jpeg ? "jpg" : "png")" : "Polar para imprimir.pdf"
        guard runPanel(panel) == .OK, let url = panel.url else { return }
        let snapshot = project, selectedPage = page, quality = preferences.exportQuality
        busy = true; status = "Preparando \(image ? "imagen" : "PDF")…"
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                switch format {
                case .png: try PolarRenderer.writePNG(project: snapshot, page: selectedPage, to: url, quality: quality)
                case .jpeg: try PolarRenderer.writeJPEG(project: snapshot, page: selectedPage, to: url, quality: quality)
                case .pdf: try PolarRenderer.writePDF(project: snapshot, to: url, quality: quality)
                }
                DispatchQueue.main.async {
                    self.busy = false; self.lastExport = url
                    self.status = image ? "Imagen lista: \(snapshot.settings.paperSize.name) a \(Int(quality.dpi)) ppp." : "PDF listo (\(quality.title)): \(snapshot.pageCount) hojas \(snapshot.settings.paperSize.name). Imprime al 100 %."
                }
            } catch {
                DispatchQueue.main.async { self.busy = false; self.errorMessage = error.localizedDescription; self.status = "No se guardó la exportación." }
            }
        }
    }

    func beginEditing() { if transactionBase == nil { transactionBase = project } }
    func endEditing() {
        guard let base = transactionBase else { return }
        transactionBase = nil
        if base != project {
            undoHistory.append(base)
            if undoHistory.count > 50 { undoHistory.removeFirst() }
            redoHistory.removeAll()
        }
    }
    private func scheduleSave() {
        autosaveTask?.cancel()
        saveFailed = false
        autosaveTask = Task { @MainActor [weak self] in
            do { try await Task.sleep(nanoseconds: 1_000_000_000) }
            catch { return }
            self?.flush()
        }
    }
    @discardableResult func flush() -> Bool {
        autosaveTask?.cancel(); autosaveTask = nil
        guard isDirty else { return true }
        do {
            // ponytail: escritura síncrona en MainActor (JSON limitado a 5 MB); una cola serial si la E/S llega a bloquear la interfaz.
            let thumbnail = PolarRenderer.preview(project: project, page: 0, scale: 220 / project.settings.paperSizePoints.width)
            project = try library.save(projectID, project: project, thumbnail: thumbnail)
            isDirty = false; saveFailed = false
            libraryItems = library.list()
            return true
        } catch {
            saveFailed = true
            status = "Sin guardar. Revisa el espacio y los permisos."
            errorMessage = error.localizedDescription
            return false
        }
    }
    func showLibrary() {
        guard !busy else { return }
        endEditing()
        guard flush() else { return }
        libraryItems = library.list(); showingLibrary = true
    }
    func openDesign(_ id: UUID) {
        guard !busy, confirmDiscard() else { return }
        do {
            project = try library.load(id); projectID = id
            designScope = 0; batchSelecting = false; selectedSlots = []
            page = 0; selectedSlot = 0; projectURL = nil; isDirty = false
            undoHistory.removeAll(); redoHistory.removeAll(); transactionBase = nil
            thumbnails.removeAll(); editingTemplate = false; textCardScope = false
            selectedTextRole = textRoles.first ?? .title; showingLibrary = false; showingCrop = false; showingFinish = false; comparing = false; suggestedLook = nil; lookPages.removeAll(); refresh()
            let missing = project.photos.filter { !FileManager.default.fileExists(atPath: $0.path) }.count
            status = missing == 0 ? "Diseño abierto: \(project.name)" : "\(missing) fotos faltan. Agrégalas y reemplaza sus espacios."
        } catch { errorMessage = error.localizedDescription }
    }
    func renameDesign(_ id: UUID, to name: String) {
        do {
            let clean = String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(120))
            if id == projectID {
                change { $0.name = clean.isEmpty ? "Sin nombre" : clean }; guard flush() else { return }
            } else { try library.rename(id, to: clean) }
            libraryItems = library.list()
        } catch { errorMessage = error.localizedDescription }
    }
    func duplicateDesign(_ id: UUID) {
        do { _ = try library.duplicate(id); libraryItems = library.list() }
        catch { errorMessage = error.localizedDescription }
    }
    func deleteDesign(_ id: UUID) {
        do {
            try library.delete(id); deletedID = id
            if id == projectID {
                autosaveTask?.cancel(); projectID = UUID(); project = PolarProject(); project.normalized(); isDirty = false
                undoHistory.removeAll(); redoHistory.removeAll(); transactionBase = nil
            }
            libraryItems = library.list()
        } catch { errorMessage = error.localizedDescription }
    }
    /// Resolution tier of a gallery photo at its first placed slot; nil when it is not placed.
    func placedQuality(of photo: PhotoAsset) -> PhotoQuality? {
        guard let slot = project.placements.firstIndex(where: { $0?.assetID == photo.id }),
              let geometry = PolarRenderer.cropGeometry(project: project, slot: slot),
              let dpi = project.effectiveDPI(slot: slot, rect: geometry.photo) else { return nil }
        return PhotoQuality.of(dpi: dpi)
    }
    var trashCount: Int { library.trashCount() }
    /// Permanently removes the trash, keeping the design that can still be undone.
    func emptyTrash() {
        library.emptyTrash(keeping: deletedID)
        objectWillChange.send()
    }
    func restoreDeletedDesign() {
        guard let id = deletedID else { return }
        do { try library.restore(id); deletedID = nil; libraryItems = library.list() }
        catch { errorMessage = error.localizedDescription }
    }
    func returnToGeneralText() {
        let card = String(selectedCard), role = selectedTextRole
        change {
            $0.cardOverrides[card]?.texts.removeValue(forKey: role.rawValue)
            $0.cardOverrides[card]?.styles.removeValue(forKey: role.rawValue)
            if role == .date { $0.cardOverrides[card]?.dateSource = nil; $0.cardOverrides[card]?.chosenDate = nil }
        }
    }
    func applyGeneralTextToAll() {
        let role = selectedTextRole
        change {
            for key in Array($0.cardOverrides.keys) {
                $0.cardOverrides[key]?.texts.removeValue(forKey: role.rawValue)
                $0.cardOverrides[key]?.styles.removeValue(forKey: role.rawValue)
                if role == .date { $0.cardOverrides[key]?.dateSource = nil; $0.cardOverrides[key]?.chosenDate = nil }
            }
        }
    }
    var dateSource: DateSource { textCardScope ? TextResolver.dateSource(project: project, card: selectedCard) : project.settings.dateSource }
    var chosenDate: Date { (textCardScope ? project.cardOverrides[String(selectedCard)]?.chosenDate : nil) ?? project.settings.chosenDate ?? Date() }
    func setDateSource(_ value: DateSource) {
        let card = textCardScope ? String(selectedCard) : nil
        change {
            if let card {
                var own = $0.cardOverrides[card] ?? CardOverride(); own.dateSource = value
                if value == .chosen && own.chosenDate == nil { own.chosenDate = $0.settings.chosenDate ?? Date() }
                $0.cardOverrides[card] = own
            } else {
                $0.settings.dateSource = value
                if value == .chosen && $0.settings.chosenDate == nil { $0.settings.chosenDate = Date() }
            }
        }
    }
    func setChosenDate(_ value: Date) {
        let card = textCardScope ? String(selectedCard) : nil
        change {
            if let card {
                var own = $0.cardOverrides[card] ?? CardOverride(); own.chosenDate = value; own.dateSource = .chosen
                $0.cardOverrides[card] = own
            } else { $0.settings.chosenDate = value; $0.settings.dateSource = .chosen }
        }
    }
    var paperDescription: String {
        let size = project.settings.paperSizePoints, unit = preferences.units
        return String(format: "%@ · %.2f × %.2f %@ · %@", project.settings.paperSize.name,
                      size.width / unit.pointsPerUnit, size.height / unit.pointsPerUnit, unit.abbreviation, project.settings.orientation.name)
    }
    /// Slots whose photo resolution is low (< 150 ppp) or only fair (< 220 ppp) at the printed size.
    var lowQualitySlots: [Int] {
        project.placements.indices.filter { slot in
            guard let geometry = PolarRenderer.cropGeometry(project: project, slot: slot),
                  let dpi = project.effectiveDPI(slot: slot, rect: geometry.photo) else { return false }
            return PhotoQuality.of(dpi: dpi) != .good
        }
    }
    var currentLook: PhotoLook { LookResolver.resolve(project: project, slot: selectedSlot) }
    func applyLook(_ look: PhotoLook, all: Bool = false) {
        endEditing()
        change { project in
            if all || lookScope == 0 { project.setAllLooks(look) }
            else if lookScope == 1 { project.setPageLooks(look, pages: [page]) }
            else if lookScope == 2 { project.setPageLooks(look, pages: lookPages) }
            else { project.setPhotoLook(look, slot: selectedSlot) }
        }
        lookUndoNotice = true
        DispatchQueue.main.asyncAfter(deadline: .now()+1) { self.lookUndoNotice = false }
    }
    func adjustLook(_ look: PhotoLook) {
        change { project in
            if lookScope == 0 { project.setAllLooks(look) }
            else if lookScope == 1 { project.setPageLooks(look, pages: [page]) }
            else if lookScope == 2 { project.setPageLooks(look, pages: lookPages) }
            else { project.setPhotoLook(look, slot: selectedSlot) }
        }
    }
    func setComparing(_ value: Bool) { guard value != comparing else { return }; comparing = value; refresh() }
    func openCrop(slot: Int? = nil) {
        if let slot { selectedSlot = slot; page = slot / project.settings.capacity }
        guard project.placements.indices.contains(selectedSlot), project.placements[selectedSlot] != nil else { return }
        endEditing(); showingCrop = true
    }
    func nextPhoto(_ delta: Int) {
        endEditing()
        let slots = project.placements.indices.filter { project.placements[$0] != nil }
        guard let index = slots.firstIndex(of: selectedSlot), slots.indices.contains(index + delta) else { return }
        selectedSlot = slots[index + delta]; page = selectedSlot / project.settings.capacity; refresh()
    }
    func finish() {
        guard !busy, project.placedCount > 0, flush() else { return }
        printPDF = nil; showingFinish = true; busy = true
        let snapshot = project
        let file = FileManager.default.temporaryDirectory.appendingPathComponent("Polar-\(UUID().uuidString).pdf")
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                try PolarRenderer.writePDF(project: snapshot, to: file)
                DispatchQueue.main.async { self.busy = false; self.printPDF = file }
            } catch {
                DispatchQueue.main.async { self.busy = false; self.errorMessage = error.localizedDescription }
            }
        }
    }
    func printDesign() {
        guard let file = printPDF, let document = PDFDocument(url: file) else { return }
        let info = NSPrintInfo.shared.copy() as! NSPrintInfo
        info.orientation = project.settings.orientation == .landscape ? .landscape : .portrait
        info.paperSize = project.settings.paperSizePoints
        info.topMargin = 0; info.bottomMargin = 0; info.leftMargin = 0; info.rightMargin = 0
        document.printOperation(for: info, scalingMode: .pageScaleNone, autoRotate: false)?.run()
    }
    func share(_ url: URL) {
        guard let view = NSApp.keyWindow?.contentView else { return }
        NSSharingServicePicker(items: [url]).show(relativeTo: CGRect(x: view.bounds.midX, y: view.bounds.midY, width: 1, height: 1), of: view, preferredEdge: .minY)
    }
    func thumbnail(_ style: TemplateStyle) -> NSImage? {
        if let image = thumbnails[style] { return image }
        var sample = PolarProject()
        if style == .imported {
            guard let template = project.settings.importedTemplate else { return nil }
            sample.settings.importedTemplate = template
        }
        sample.selectStyle(style)
        sample.settings.columns = 1; sample.settings.rows = 1
        if let photo = project.photos.first(where: { $0.name.hasSuffix(" 2") }) ?? project.photos.first {
            sample.photos = [photo]
            sample.placements = Array(repeating: PhotoPlacement(assetID: photo.id), count: style == .imported ? sample.settings.capacity : style.photosPerCard)
        }
        let rendered = PolarRenderer.preview(project: sample, page: 0)
        if style == .imported { thumbnails[style] = rendered; return rendered }
        guard let cg = rendered.cgImage(forProposedRect: nil, context: nil, hints: nil),
              let rect = PolarRenderer.cardRects(settings: sample.settings).first else { return rendered }
        let scale = CGFloat(cg.width) / sample.settings.paperSizePoints.width
        guard let cropped = cg.cropping(to: CGRect(x: rect.minX * scale, y: rect.minY * scale, width: rect.width * scale, height: rect.height * scale).integral) else { return rendered }
        let image = NSImage(cgImage: cropped, size: NSSize(width: rect.width, height: rect.height))
        thumbnails[style] = image
        return image
    }
}
