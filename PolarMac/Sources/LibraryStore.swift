import AppKit

enum AppTheme: String, Codable, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }
    var name: String { self == .system ? "Sistema" : self == .light ? "Claro" : "Oscuro" }
}

enum AppUnits: String, Codable, CaseIterable, Identifiable {
    case mm, inches
    var id: String { rawValue }
    var name: String { self == .mm ? "Milímetros" : "Pulgadas" }
    var abbreviation: String { self == .mm ? "mm" : "pulg." }
    var pointsPerUnit: Double { self == .mm ? 72 / 25.4 : 72 }
}

struct AppPreferences: Codable {
    var theme: AppTheme = .system
    var units: AppUnits = .mm
    var defaultPaper: PaperSize = .letter
    var onboardingSeen = false
    var exportQuality: ExportQuality = .high
    /// El recorrido inicial ya se completó o se saltó. Un valor ausente o dañado cuenta como «no visto».
    var tourSeen = false

    init() {}
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        theme = try c.decodeIfPresent(AppTheme.self, forKey: .theme) ?? theme
        units = try c.decodeIfPresent(AppUnits.self, forKey: .units) ?? units
        defaultPaper = try c.decodeIfPresent(PaperSize.self, forKey: .defaultPaper) ?? defaultPaper
        onboardingSeen = try c.decodeIfPresent(Bool.self, forKey: .onboardingSeen) ?? onboardingSeen
        exportQuality = (try? c.decodeIfPresent(ExportQuality.self, forKey: .exportQuality)) ?? .high
        tourSeen = (try? c.decodeIfPresent(Bool.self, forKey: .tourSeen)) ?? false
    }
}

struct LibraryItem: Identifiable {
    var id: UUID
    var name: String
    var style: TemplateStyle
    var pages: Int
    var updatedAt: Date
    var thumbnail: URL
}

/// MainActor serializa las escrituras: cancelar el debounce no inicia otra escritura en paralelo.
@MainActor final class LibraryStore {
    let root: URL
    private let fm = FileManager.default
    init(root: URL? = nil) {
        self.root = root ?? fm.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("Polar")
    }
    func directory(_ id: UUID) -> URL { root.appendingPathComponent("projects/\(id.uuidString)") }
    func projectFile(_ id: UUID) -> URL { directory(id).appendingPathComponent("project.polar") }

    func load(_ id: UUID) throws -> PolarProject {
        try read(projectFile(id))
    }
    func read(_ url: URL) throws -> PolarProject {
        let source = url.resolvingSymlinksInPath()
        let size = try source.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? Int.max
        guard size <= 5_000_000 else { throw PolarError.invalidProject("El archivo es demasiado grande.") }
        let data = try Data(contentsOf: source, options: .mappedIfSafe)
        guard data.count <= 5_000_000 else { throw PolarError.invalidProject("El archivo es demasiado grande.") }
        var project = try JSONDecoder().decode(PolarProject.self, from: data)
        try project.validated()
        for index in project.photos.indices {
            project.photos[index].path = absolute(project.photos[index].path, beside: source)
            if let mask = project.photos[index].maskPath { project.photos[index].maskPath = absolute(mask, beside: source) }
        }
        if let template = project.settings.importedTemplate {
            project.settings.importedTemplate?.path = absolute(template.path, beside: source)
        }
        project.normalized()
        return project
    }
    private func absolute(_ path: String, beside file: URL) -> String {
        path.hasPrefix("/") || path.hasPrefix("content:") ? path : file.deletingLastPathComponent().appendingPathComponent(path).standardizedFileURL.path
    }

    @discardableResult func save(_ id: UUID, project: PolarProject, thumbnail: NSImage? = nil) throws -> PolarProject {
        try project.validated()
        let dir = directory(id)
        try fm.createDirectory(at: dir, withIntermediateDirectories: true)
        var saved = project
        for index in saved.photos.indices {
            let photo = saved.photos[index]
            if let mask = photo.maskPath, fm.fileExists(atPath: mask) {
                let source = URL(fileURLWithPath: mask)
                let destination = dir.appendingPathComponent("photos/\(photo.id.uuidString)/\(source.lastPathComponent)")
                try copy(source, to: destination)
                saved.photos[index].maskPath = destination.path
            }
            let destination = dir.appendingPathComponent("photos/\(photo.id.uuidString)/\(photo.url.lastPathComponent)")
            if fm.fileExists(atPath: photo.path) || fm.fileExists(atPath: destination.path) {
                try copy(photo.url, to: destination)
                saved.photos[index].path = destination.path
            }
        }
        if let template = saved.settings.importedTemplate, fm.fileExists(atPath: template.path) {
            let source = URL(fileURLWithPath: template.path)
            let destination = source.path.hasPrefix(dir.path + "/") ? source : dir.appendingPathComponent("templates/\(UUID().uuidString)/\(source.lastPathComponent)")
            try copy(source, to: destination)
            saved.settings.importedTemplate?.path = destination.path
        }
        saved.updatedAtEpochMs = Int64(Date().timeIntervalSince1970 * 1000)
        let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let data = try encoder.encode(saved)
        guard data.count <= 5_000_000 else { throw PolarError.invalidProject("El archivo es demasiado grande. Divide el trabajo en varios diseños.") }
        try data.write(to: projectFile(id), options: .atomic)
        if let cg = thumbnail?.cgImage(forProposedRect: nil, context: nil, hints: nil) {
            let small = NSImage(size: NSSize(width: 220, height: 220 * CGFloat(cg.height) / CGFloat(cg.width)))
            small.lockFocus()
            NSImage(cgImage: cg, size: .zero).draw(in: NSRect(origin: .zero, size: small.size))
            small.unlockFocus()
            if let tiff = small.tiffRepresentation, let png = NSBitmapImageRep(data: tiff)?.representation(using: .png, properties: [:]) {
                try? png.write(to: dir.appendingPathComponent("thumb.png"), options: .atomic)
            }
        }
        return saved
    }
    private func copy(_ source: URL, to destination: URL) throws {
        guard source.standardizedFileURL != destination.standardizedFileURL,
              !fm.fileExists(atPath: destination.path) else { return }
        try fm.createDirectory(at: destination.deletingLastPathComponent(), withIntermediateDirectories: true)
        let temporary = destination.deletingLastPathComponent().appendingPathComponent(".\(UUID().uuidString).tmp")
        defer { try? fm.removeItem(at: temporary) }
        try fm.copyItem(at: source, to: temporary)
        try fm.moveItem(at: temporary, to: destination)
    }
    func list() -> [LibraryItem] {
        // ponytail: lee los proyectos completos; añadir metadatos separados si la biblioteca llega a miles de diseños.
        let folders = (try? fm.contentsOfDirectory(at: root.appendingPathComponent("projects"), includingPropertiesForKeys: nil)) ?? []
        return folders.compactMap { folder in
            guard let id = UUID(uuidString: folder.lastPathComponent), let project = try? load(id) else { return nil }
            return LibraryItem(id: id, name: project.name.isEmpty ? "Sin nombre" : project.name, style: project.settings.style,
                               pages: project.pageCount, updatedAt: Date(timeIntervalSince1970: Double(project.updatedAtEpochMs) / 1000),
                               thumbnail: folder.appendingPathComponent("thumb.png"))
        }.sorted { $0.updatedAt > $1.updatedAt }
    }
    func rename(_ id: UUID, to name: String) throws {
        var project = try load(id); project.name = String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(120))
        if project.name.isEmpty { project.name = "Sin nombre" }
        try save(id, project: project)
    }
    func duplicate(_ id: UUID) throws -> UUID {
        var project = try load(id); project.name += " · copia"
        let copyID = UUID(); try save(copyID, project: project)
        if fm.fileExists(atPath: directory(id).appendingPathComponent("thumb.png").path) {
            try copy(directory(id).appendingPathComponent("thumb.png"), to: directory(copyID).appendingPathComponent("thumb.png"))
        }
        return copyID
    }
    func delete(_ id: UUID) throws {
        let trash = root.appendingPathComponent("trash")
        try fm.createDirectory(at: trash, withIntermediateDirectories: true)
        let destination = trash.appendingPathComponent(id.uuidString)
        try? fm.removeItem(at: destination)
        try fm.moveItem(at: directory(id), to: destination)
        // The folder keeps its old mtime when moved, so stamp the deletion time for the 7-day purge.
        try? fm.setAttributes([.modificationDate: Date()], ofItemAtPath: destination.path)
    }
    static let trashRetentionDays = 7
    private var trash: URL { root.appendingPathComponent("trash") }
    private func trashEntries() -> [URL] {
        (try? fm.contentsOfDirectory(at: trash, includingPropertiesForKeys: [.contentModificationDateKey], options: [.skipsHiddenFiles])) ?? []
    }
    /// Number of deleted designs still in the trash.
    func trashCount() -> Int { trashEntries().count }
    /// Removes trashed designs deleted more than 7 days before `now`. Never touches `projects/`.
    @discardableResult func purgeTrash(now: Date = Date()) -> Int {
        let limit = now.addingTimeInterval(-Double(Self.trashRetentionDays) * 86_400)
        var removed = 0
        for entry in trashEntries() {
            let date = (try? entry.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast
            if date < limit, (try? fm.removeItem(at: entry)) != nil { removed += 1 }
        }
        return removed
    }
    /// Permanently empties the trash, except the design that can still be undone in this session.
    @discardableResult func emptyTrash(keeping kept: UUID? = nil) -> Int {
        var removed = 0
        for entry in trashEntries() where entry.lastPathComponent != kept?.uuidString {
            if (try? fm.removeItem(at: entry)) != nil { removed += 1 }
        }
        return removed
    }
    func restore(_ id: UUID) throws {
        try fm.createDirectory(at: directory(id).deletingLastPathComponent(), withIntermediateDirectories: true)
        try fm.moveItem(at: root.appendingPathComponent("trash/\(id.uuidString)"), to: directory(id))
    }
    func preferences() -> AppPreferences {
        (try? JSONDecoder().decode(AppPreferences.self, from: Data(contentsOf: root.appendingPathComponent("settings.json")))) ?? AppPreferences()
    }
    func savePreferences(_ value: AppPreferences) throws {
        try fm.createDirectory(at: root, withIntermediateDirectories: true)
        try JSONEncoder().encode(value).write(to: root.appendingPathComponent("settings.json"), options: .atomic)
    }
}
