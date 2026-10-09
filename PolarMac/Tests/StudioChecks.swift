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
        var nav = SheetNavigator()
        precondition(nav.scroll(dx: 30, dy: 0, ended: false, now: 10, enabled: true) == nil, "Bajo el umbral no cambia de hoja")
        precondition(nav.scroll(dx: 60, dy: 0, ended: false, now: 10.05, enabled: true) == -1, "Deslizar a la derecha va a la hoja anterior")
        precondition(nav.scroll(dx: 200, dy: 0, ended: false, now: 10.1, enabled: true) == nil, "Un gesto cambia una sola hoja")
        precondition(nav.scroll(dx: 0, dy: 0, ended: true, now: 10.2, enabled: true) == nil)
        precondition(nav.scroll(dx: -100, dy: 0, ended: false, now: 10.25, enabled: true) == nil, "El debounce frena un cambio inmediato")
        precondition(nav.scroll(dx: 0, dy: 0, ended: true, now: 10.3, enabled: true) == nil)
        precondition(nav.scroll(dx: -100, dy: 5, ended: false, now: 11, enabled: true) == 1, "Pasada la pausa, deslizar a la izquierda va a la siguiente")
        precondition(nav.scroll(dx: 0, dy: 0, ended: true, now: 11.1, enabled: true) == nil)
        precondition(nav.scroll(dx: 20, dy: 120, ended: false, now: 12, enabled: true) == nil, "El desplazamiento vertical no cambia de hoja")
        precondition(nav.scroll(dx: 200, dy: 0, ended: false, now: 13, enabled: false) == nil, "Desactivado mientras se arrastra o se editan huecos")
        precondition(nav.key(direction: 1, now: 20, enabled: false) == nil)
        precondition(nav.key(direction: 1, now: 20, enabled: true) == 1 && nav.key(direction: 1, now: 20.05, enabled: true) == nil, "Las flechas también respetan la pausa")
        precondition(TextSizePreset.matching(0) == .auto && TextSizePreset.matching(12) == .medium && TextSizePreset.matching(13) == nil)
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
