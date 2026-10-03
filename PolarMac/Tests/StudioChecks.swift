import AppKit
import SwiftUI

@main @MainActor struct StudioChecks {
    static func main() async throws {
        let storage = FileManager.default.temporaryDirectory.appendingPathComponent("polar-studio-storage-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: storage) }
        let studio = Studio(storageRoot: storage)
        let asset = PhotoAsset(path: "/private/tmp/polar-example.jpg", pixelWidth: 1200, pixelHeight: 1200)
        studio.project = PolarProject()
        studio.project.photos = [asset]
        studio.project.selectStyle(.calendar)
        studio.project.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 24)
        studio.chooseStyle(.polaroid)
        studio.navigate(1)
        studio.undo()
        precondition(studio.project.settings.capacity == 12)
        precondition(studio.selectedSlot >= 12 && studio.selectedSlot < 24, "Deshacer debe seleccionar un espacio de la hoja visible")
        studio.navigate(0)
        studio.editPlacement { $0.zoom = 2 }
        studio.undo()
        precondition(studio.project.placements[0]?.zoom == 1, "Deshacer un encuadre debe recuperar el zoom")
        let before = studio.project.placements.compactMap { $0?.assetID }
        studio.setGrid(columns: 2, rows: 2)
        precondition(studio.project.settings.capacity == 4)
        precondition(studio.project.placements.compactMap { $0?.assetID } == before, "Cambiar distribución conserva las fotos y su orden")
        studio.undo()
        precondition(studio.project.settings.capacity == 12)
        studio.project.settings.title = "Mi frase personal"
        studio.applyMood(.friends)
        precondition(studio.project.settings.title == "Mi frase personal", "Los presets conservan los textos editados")
        precondition(studio.fontChoices.contains(where: { $0.id == studio.project.settings.textStyle(.title).fontName }))
        studio.project.settings.style = .imported
        studio.project.settings.importedTemplate = ImportedTemplate(path: "/private/tmp/molde.png", pixelWidth: 1000, pixelHeight: 1000, regions: [
            TemplateRegion(x: 0.1, y: 0.1, width: 0.3, height: 0.3),
            TemplateRegion(x: 0.6, y: 0.1, width: 0.3, height: 0.3, isTransparent: true)
        ])
        studio.project.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 4)
        studio.project.cardOverrides["3"] = CardOverride(texts: ["title": "Cuarta tarjeta"])
        studio.page = 0; studio.selectedSlot = 0
        studio.addTemplateRegion()
        precondition(studio.project.settings.capacity == 3 && studio.project.pageCount == 2)
        precondition(studio.project.placements[2] == nil && studio.project.placements[5] == nil)
        precondition(studio.project.placedCount == 4, "Agregar hueco mantiene fotos por página")
        precondition(studio.project.cardOverrides["4"]?.texts["title"] == "Cuarta tarjeta", "El texto propio acompaña a la tarjeta al añadir huecos")
        studio.selectedSlot = 1
        studio.removeTemplateRegion()
        precondition(studio.project.settings.capacity == 2 && studio.project.placedCount == 2)
        precondition(studio.project.cardOverrides["4"] == nil, "Quitar un hueco elimina su texto propio")
        precondition(studio.project.photos.count == 1, "Quitar hueco no borra la foto original de la galería")
        studio.undo()
        precondition(studio.project.settings.capacity == 3 && studio.project.placedCount == 4)
        studio.moveTemplateRegion(index: 1, dx: 1, dy: 0)
        let moved = studio.project.settings.importedTemplate!.regions[1]
        precondition(moved.x + moved.width <= 1 && !moved.isTransparent)

        studio.project = PolarProject()
        studio.project.photos = [asset]
        studio.project.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 44)
        studio.project.normalized()
        studio.setGrid(columns: 2, rows: 2)
        precondition(studio.project.pageCount == 11 && studio.project.placedCount == 44,
                     "Redistribuir 44 fotos de 9 a 4 por hoja debe producir 11 hojas, sin contar el relleno vacío")
        for style in [TemplateStyle.editorial, .celebration] {
            studio.chooseStyle(style)
            precondition(studio.textRoles == [.title, .subtitle, .caption, .date], "Todos los textos impresos deben poder editarse")
        }

        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("polar-studio-checks-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: folder) }
        let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 8, pixelsHigh: 8, bitsPerSample: 8,
                                      samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                                      bytesPerRow: 0, bitsPerPixel: 0)!
        let png = bitmap.representation(using: .png, properties: [:])!
        let urls = try (0..<11).map { index in
            let url = folder.appendingPathComponent("photo-\(index).png")
            try png.write(to: url)
            return url
        }
        studio.project = PolarProject()
        studio.project.placements = Array(repeating: nil, count: 1998)
        studio.page = 221; studio.selectedSlot = 1997
        try await importPhotos(urls, into: studio)
        precondition(studio.project.placedCount == 11 && studio.project.placements.count == 1998,
                     "Importar desde el final debe reutilizar los huecos anteriores sin crear hojas sobre el límite")
        precondition(studio.project.placements[1997] != nil && studio.project.placements[0] != nil)
        try studio.project.validated()

        studio.project = PolarProject()
        studio.project.photos = [asset]
        studio.project.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 2000)
        studio.project.normalized()
        studio.page = 222; studio.selectedSlot = 2000
        try await importPhotos(urls, into: studio)
        precondition(studio.project.placedCount == 2000 && studio.project.placements.count == 2007,
                     "Al llegar a 2000 fotos colocadas, las nuevas fotos deben quedarse en la galería")
        precondition(studio.project.photos.count == 12 && studio.project.placements[2000] == nil)
        try studio.project.validated()

        let large = folder.appendingPathComponent("large.json"), link = folder.appendingPathComponent("large.polar")
        try Data(repeating: 32, count: 6_000_000).write(to: large)
        try FileManager.default.createSymbolicLink(at: link, withDestinationURL: large)
        let previousTitle = studio.project.settings.title
        studio.openProject(at: link)
        precondition(studio.errorMessage?.contains("demasiado grande") == true,
                     "El límite de 5 MB debe aplicarse al destino del enlace antes de decodificar JSON")
        precondition(studio.project.settings.title == previousTitle)
        print("OK: deshacer, selección, huecos multipágina, 44 fotos/11 hojas, roles editables, importación al límite y symlink >5MB")
    }

    static func importPhotos(_ urls: [URL], into studio: Studio) async throws {
        studio.errorMessage = nil
        studio.importURLs(urls, fill: true)
        let deadline = Date().addingTimeInterval(10)
        while studio.busy && Date() < deadline { try await Task.sleep(nanoseconds: 10_000_000) }
        precondition(!studio.busy && studio.errorMessage == nil, "La importación de las fotos de prueba debe completarse")
    }
}
