import Foundation
import CoreGraphics

@main struct ModelChecks {
    static func main() throws {
        var project = PolarProject()
        precondition(project.settings.capacity == 9, "Polaroid debe permitir 9 fotos por carta")
        precondition(project.pageCount == 1)
        project.placements = Array(repeating: nil, count: 20)
        precondition(project.pageCount == 3, "No perder las páginas adicionales")
        let restored = try JSONDecoder().decode(PolarProject.self, from: JSONEncoder().encode(project))
        precondition(restored.placements.count == 20)
        var legacySettings = try JSONSerialization.jsonObject(with: JSONEncoder().encode(PrintSettings())) as! [String: Any]
        legacySettings["paperSize"] = "a4"
        legacySettings["orientation"] = "landscape"
        let paperSettings = try JSONDecoder().decode(PrintSettings.self, from: JSONSerialization.data(withJSONObject: legacySettings))
        let encodedPaper = try JSONSerialization.jsonObject(with: JSONEncoder().encode(paperSettings)) as! [String: Any]
        if encodedPaper["paperSize"] as? String != "a4" || encodedPaper["orientation"] as? String != "landscape" {
            fputs("FAIL: paper and orientation survive project persistence\n", stderr); exit(1)
        }
        let partialLegacy = try JSONDecoder().decode(PrintSettings.self, from: Data("{}".utf8))
        precondition(partialLegacy.paperSize == .letter && partialLegacy.orientation == .portrait && partialLegacy.textStyles.isEmpty)
        let fixtureURL = URL(fileURLWithPath: "../PolarAndroid/app/src/test/resources/fixtures/mac_polaroid.polar")
        let old = try JSONDecoder().decode(PolarProject.self, from: Data(contentsOf: fixtureURL))
        try old.validated()
        precondition(old.version == 1 && old.settings.paperSize == .letter, "Proyecto v1 conserva carta predeterminada")
        let paperDimensions: [(PaperSize, Double, Double)] = [(.letter, 612, 792), (.oficio, 612.28346457, 963.77952756),
            (.legal, 612, 1008), (.a4, 595.27559055, 841.88976378), (.a3, 841.88976378, 1190.55118110),
            (.photo4x6, 288, 432), (.photo5x7, 360, 504), (.custom, 612, 792)]
        for (paper, width, height) in paperDimensions { for orientation in PaperOrientation.allCases {
            var settings = PrintSettings(); settings.paperSize = paper; settings.orientation = orientation
            let size = settings.paperSizePoints
            precondition(abs(size.width - (orientation == .portrait ? width : height)) < 0.001)
            precondition(abs(size.height - (orientation == .portrait ? height : width)) < 0.001)
            precondition(!settings.paperDescription.isEmpty)
        } }
        for format in CardFormat.allCases {
            var settings = PrintSettings(); settings.cardFormat = format
            precondition(format == .fill ? settings.cardAspect == nil : settings.cardAspect! > 0)
        }
        var typography = PolarProject()
        typography.settings.textStyles["title"] = TextAppearance(fontName: "Helvetica", size: 12, hex: "123456", bold: true,
            italic: true, alignment: .right, offsetX: 3, offsetY: -2, visible: true)
        try typography.validated()
        let fontRoundTrip = try JSONDecoder().decode(PolarProject.self, from: JSONEncoder().encode(typography))
        precondition(fontRoundTrip.settings.textStyle(.title).fontName == "Helvetica")
        precondition(fontRoundTrip.settings.textStyle(.subtitle).size == 0)
        for invalidAppearance in [TextAppearance(size: 5), TextAppearance(hex: "FFF"), TextAppearance(offsetX: 61), TextAppearance(offsetY: -.infinity), TextAppearance(fontName: String(repeating: "x", count: 129))] {
            var bad = typography; bad.settings.textStyles["title"] = invalidAppearance
            do { try bad.validated(); preconditionFailure("Rechazar tipografía inválida") } catch {}
        }
        var unknownRole = typography; unknownRole.settings.textStyles["unknown"] = TextAppearance()
        do { try unknownRole.validated(); preconditionFailure("Rechazar rol desconocido") } catch {}
        var imported = PolarProject(); imported.selectStyle(.imported)
        do { try imported.validated(); preconditionFailure("Plantilla activa necesita archivo") } catch {}
        imported.settings.importedTemplate = ImportedTemplate(path: "/private/tmp/template.png", pixelWidth: 1000, pixelHeight: 1000,
            regions: [TemplateRegion(x: 0.1, y: 0.1, width: 0.4, height: 0.8), TemplateRegion(x: 0.5, y: 0.1, width: 0.4, height: 0.8)])
        precondition(imported.settings.capacity == 2); try imported.validated()
        var badRegion = imported; badRegion.settings.importedTemplate!.regions[0].x = 0.9
        do { try badRegion.validated(); preconditionFailure("Rechazar región fuera de imagen") } catch {}
        var hugeTemplate = imported; hugeTemplate.settings.importedTemplate!.pixelWidth = Int.max
        do { try hugeTemplate.validated(); preconditionFailure("Rechazar dimensiones extremas") } catch {}
        var crowded = PolarProject(); crowded.settings.paperSize = .custom; crowded.settings.customWidthMM = 80
        crowded.settings.customHeightMM = 80; crowded.settings.rows = 6; crowded.settings.margin = 60; crowded.settings.gap = 30
        do { try crowded.validated(); preconditionFailure("Rechazar grilla sin espacio en papel") } catch {}
        var invalid = project
        invalid.settings.columns = 0
        do { try invalid.validated(); preconditionFailure("Rechazar grilla inválida") } catch {}
        var many = PolarProject()
        let asset = PhotoAsset(path: "/private/tmp/polar-example.jpg", pixelWidth: 1200, pixelHeight: 1200)
        many.photos = [asset]
        many.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 2000)
        many.normalized()
        precondition(many.placements.count == 2007)
        try many.validated()
        print("OK: capacidad, páginas, persistencia y validación del modelo")
    }
}
