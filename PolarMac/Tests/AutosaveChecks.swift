import AppKit
import SwiftUI

@main @MainActor struct AutosaveChecks {
    static func main() async throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("polar-autosave-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: root) }
        let studio = Studio(storageRoot: root)
        for index in 0..<100 { studio.change { $0.settings.title = "Edición \(index)" } }
        try await Task.sleep(nanoseconds: 1_300_000_000)
        guard !studio.isDirty else {
            fputs("FAIL: la última edición no se guardó automáticamente\n", stderr)
            exit(1)
        }
        let latest = try studio.library.load(studio.projectID)
        precondition(latest.settings.title == "Edición 99", "Una edición vieja no sobrescribe la última")
        studio.beginEditing()
        for index in 0..<20 { studio.change { $0.settings.title = "Frase \(index)" } }
        studio.endEditing(); studio.undo()
        precondition(studio.project.settings.title == "Edición 99", "Todo el campo de texto se deshace en un paso")
        studio.redo(); precondition(studio.project.settings.title == "Frase 19")
        studio.change { $0.settings.title = "Al salir" }
        studio.showLibrary()
        let atExit = try studio.library.load(studio.projectID)
        precondition(studio.showingLibrary && !studio.isDirty && atExit.settings.title == "Al salir", "La salida guarda sin esperar al debounce")
        let reopened = Studio(storageRoot: root)
        reopened.openDesign(studio.projectID)
        precondition(reopened.project.settings.title == "Al salir", "El diseño se recupera en otra sesión")
        reopened.textCardScope = true; reopened.selectedSlot = 1; reopened.selectedTextRole = .title
        reopened.setTextValue("Sólo la segunda")
        precondition(TextResolver.text(project: reopened.project, card: 0, role: .title) == "Al salir")
        precondition(TextResolver.text(project: reopened.project, card: 1, role: .title) == "Sólo la segunda")
        reopened.returnToGeneralText()
        precondition(TextResolver.text(project: reopened.project, card: 1, role: .title) == "Al salir")
        reopened.preferences.theme = .dark; reopened.preferences.units = .inches
        let configured = Studio(storageRoot: root)
        precondition(configured.preferences.theme == .dark && configured.preferences.units == .inches)
        let duplicate = try studio.library.duplicate(studio.projectID)
        try studio.library.rename(duplicate, to: "Otro pedido")
        precondition(studio.library.list().count == 2)
        try studio.library.delete(duplicate); precondition(studio.library.list().count == 1)
        try studio.library.restore(duplicate)
        let restored = try studio.library.load(duplicate)
        precondition(restored.name == "Otro pedido" && studio.library.list().count == 2)
        let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 8, pixelsHigh: 8, bitsPerSample: 8,
                                      samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                                      bytesPerRow: 0, bitsPerPixel: 0)!
        let originalPhoto = root.appendingPathComponent("original.png")
        let bytes = bitmap.representation(using: .png, properties: [:])!
        try bytes.write(to: originalPhoto)
        let asset = PhotoImporter.read(urls: [originalPhoto]).photos[0]
        reopened.change { $0.photos = [asset]; $0.placements = [PhotoPlacement(assetID: asset.id)] }
        precondition(reopened.flush())
        let copyPath = reopened.project.photos[0].path
        precondition(copyPath != originalPhoto.path)
        let untouched = try Data(contentsOf: originalPhoto)
        precondition(untouched == bytes, "La biblioteca no cambia los originales")
        try FileManager.default.removeItem(at: originalPhoto)
        let copied = try Data(contentsOf: URL(fileURLWithPath: copyPath))
        precondition(copied == bytes, "Mover el original no pierde la foto de la biblioteca")
        reopened.addPage()
        reopened.change { $0.cardOverrides["10"] = CardOverride(texts: ["title": "Hoja siguiente"]) }
        reopened.navigate(0); reopened.removePage()
        precondition(reopened.project.cardOverrides["1"]?.texts["title"] == "Hoja siguiente", "El texto se reindexa al quitar una hoja")
        reopened.undo()
        precondition(reopened.project.cardOverrides["10"]?.texts["title"] == "Hoja siguiente")
        let blocked = root.appendingPathComponent("blocked")
        let original = Data("Conservar".utf8); try original.write(to: blocked)
        let failing = Studio(storageRoot: blocked)
        failing.showingLibrary = false
        failing.change { $0.settings.title = "Pendiente" }
        precondition(!failing.flush() && failing.isDirty && failing.saveFailed)
        failing.showLibrary()
        precondition(!failing.showingLibrary && failing.project.settings.title == "Pendiente", "Un fallo de E/S mantiene abierto el editor")
        let preserved = try Data(contentsOf: blocked)
        precondition(preserved == original)
        print("OK: 100 ediciones/autoguardado, salida, reapertura, texto por tarjeta, historial agrupado, ajustes, duplicar/borrar/deshacer, copias intactas, quitar hojas y fallo de E/S")
    }
}
