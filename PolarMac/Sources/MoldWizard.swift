import Foundation
import CoreGraphics

/// Estado del asistente de 3 pasos para importar un molde: 1 elegir la imagen, 2 revisar los espacios, 3 nombre y guardar.
/// Entre el paso 1 y el 2 aparece el aviso de molde parecido cuando `findDuplicate` encuentra uno.
struct MoldWizardState: Equatable {
    enum Step: Int, CaseIterable {
        case choose = 1, review, save
        var title: String { self == .choose ? "Elige la imagen" : self == .review ? "Revisa los espacios" : "Nombre y guardar" }
    }
    enum Corner: CaseIterable { case topLeading, topTrailing, bottomLeading, bottomTrailing }
    static let minimumSize = 0.02
    static let maximumRegions = 64

    var step: Step = .choose
    var imageURL: URL?
    var template: ImportedTemplate?
    var detectedCount = 0
    var approximateShapes = 0
    var selected = 0
    var name = ""
    var duplicate: MoldDuplicate?
    var errorMessage: String?

    var regions: [TemplateRegion] { template?.regions ?? [] }
    var awaitingDuplicateDecision: Bool { step == .choose && duplicate != nil }
    var canGoNext: Bool {
        switch step {
        case .choose: return template != nil && !awaitingDuplicateDecision
        case .review: return !regions.isEmpty
        case .save: return !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }
    var hint: String? {
        guard step == .review else { return nil }
        if detectedCount == 0 { return "No encontré espacios claros. Ajusta el que aparece o agrega otros." }
        if approximateShapes > 0 { return "\(approximateShapes == 1 ? "Un espacio tiene" : "\(approximateShapes) espacios tienen") forma aproximada; revisa su forma." }
        return nil
    }

    /// Lee la imagen: detecta huecos y formas y busca un molde parecido. En error deja el mensaje y se queda en el paso 1.
    mutating func load(url: URL, library: MoldLibrary) {
        errorMessage = nil
        do {
            let result = try TemplateImporter.read(url: url)
            imageURL = url; template = result.template; detectedCount = result.detectedCount
            approximateShapes = result.approximateShapes; selected = 0
            if name.isEmpty { name = url.deletingPathExtension().lastPathComponent }
            duplicate = library.findDuplicate(imageURL: url)
            if duplicate == nil { step = .review }
        } catch {
            template = nil; duplicate = nil; errorMessage = error.localizedDescription
        }
    }

    /// «Guardar como nuevo» en el aviso de molde parecido: sigue al paso 2.
    mutating func keepAsNew() { guard awaitingDuplicateDecision else { return }; duplicate = nil; step = .review }
    mutating func next() {
        guard canGoNext, let following = Step(rawValue: step.rawValue + 1) else { return }
        step = following
    }
    mutating func back() {
        if awaitingDuplicateDecision { duplicate = nil; return }
        if let previous = Step(rawValue: step.rawValue - 1) { step = previous }
    }

    // MARK: Espacios (porcentajes como fracciones 0...1 de la imagen)
    private mutating func edit(_ index: Int, _ change: (inout TemplateRegion) -> Void) {
        guard template?.regions.indices.contains(index) == true else { return }
        var region = template!.regions[index]
        change(&region); region.clamp(); region.isTransparent = false
        template!.regions[index] = region
    }
    mutating func select(_ index: Int) { if regions.indices.contains(index) { selected = index } }
    mutating func moveRegion(_ index: Int, dx: Double, dy: Double) {
        edit(index) { $0.x += dx; $0.y += dy }
        select(index)
    }
    /// Arrastra una esquina (dx, dy en fracciones de la imagen); la esquina opuesta se queda quieta.
    mutating func resizeRegion(_ index: Int, corner: Corner, dx: Double, dy: Double) {
        guard regions.indices.contains(index) else { return }
        let r = regions[index].rect
        var left = r.minX, right = r.maxX, top = r.minY, bottom = r.maxY
        let m = Self.minimumSize
        switch corner {  // sólo se mueven los bordes de la esquina; los opuestos se quedan quietos
        case .topLeading, .bottomLeading: left = min(max(0, left + dx), right - m)
        case .topTrailing, .bottomTrailing: right = max(min(1, right + dx), left + m)
        }
        switch corner {
        case .topLeading, .topTrailing: top = min(max(0, top + dy), bottom - m)
        case .bottomLeading, .bottomTrailing: bottom = max(min(1, bottom + dy), top + m)
        }
        edit(index) { $0.x = left; $0.y = top; $0.width = right - left; $0.height = bottom - top }
        select(index)
    }
    /// Cambia el tamaño desde el centro en pasos (acción de accesibilidad «Agrandar» / «Reducir»).
    mutating func scaleRegion(_ index: Int, factor: Double) {
        guard regions.indices.contains(index) else { return }
        let r = regions[index].rect
        let w = min(1, max(Self.minimumSize, r.width * factor)), h = min(1, max(Self.minimumSize, r.height * factor))
        edit(index) { $0.x = r.midX - w / 2; $0.y = r.midY - h / 2; $0.width = w; $0.height = h }
    }
    mutating func setShape(_ index: Int, _ shape: RegionShape) {
        edit(index) { $0.shape = shape; if shape != .round { $0.radius = 0 } else if $0.radius <= 0 { $0.radius = 0.2 } }
    }
    mutating func setRadius(_ index: Int, _ radius: Double) {
        edit(index) { $0.radius = min(0.5, max(0, radius.isFinite ? radius : 0)); if $0.radius > 0 { $0.shape = .round } }
    }
    @discardableResult mutating func addRegion() -> Bool {
        guard template != nil, regions.count < Self.maximumRegions else { return false }
        template!.regions.append(TemplateRegion(x: 0.2, y: 0.2, width: 0.6, height: 0.6))
        selected = regions.count - 1
        return true
    }
    @discardableResult mutating func removeSelected() -> Bool {
        guard template != nil, regions.count > 1, regions.indices.contains(selected) else { return false }
        template!.regions.remove(at: selected)
        selected = min(selected, regions.count - 1)
        return true
    }

    /// Guarda el molde en «Mis moldes» con los espacios revisados.
    func save(in library: MoldLibrary) throws -> SavedMold {
        guard let imageURL else { throw PolarError.invalidProject("Elige primero una imagen.") }
        return try library.save(imageURL: imageURL, nombre: name, regiones: regions)
    }
}
