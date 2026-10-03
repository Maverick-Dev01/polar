import AppKit
import PDFKit
import ImageIO

// Run from PolarMac after compiling with Models, TemplateImport and Renderer.
@main
struct ExportExamples {
    static func require(_ condition: @autoclosure () -> Bool, _ message: String) throws {
        if !condition() { throw NSError(domain: "PolarExamples", code: 1, userInfo: [NSLocalizedDescriptionKey: message]) }
    }

    static func render(_ page: PDFPage, to url: URL, dpi: CGFloat = 144) throws -> CGImage {
        let box = page.bounds(for: .mediaBox), scale = dpi / 72
        let context = CGContext(data: nil, width: Int((box.width * scale).rounded()), height: Int((box.height * scale).rounded()),
            bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.setFillColor(NSColor.white.cgColor)
        context.fill(CGRect(x: 0, y: 0, width: context.width, height: context.height))
        context.scaleBy(x: scale, y: scale)
        context.drawPDFPage(page.pageRef!)
        let image = context.makeImage()!
        let destination = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, nil)
        try require(CGImageDestinationFinalize(destination), "No se pudo generar la revisión visual.")
        return image
    }

    static func main() throws {
        let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath).deletingLastPathComponent()
        let original = root.appendingPathComponent("Mi primer diseño.polar")
        let originalData = try Data(contentsOf: original)
        let project = try JSONDecoder().decode(PolarProject.self, from: originalData)
        try project.validated()
        try require(project.placedCount == 44 && project.pageCount == 5 && project.settings.capacity == 9,
                    "El proyecto de referencia debe contener 44 fotos en 5 hojas de 9 espacios.")
        let updatedPDF = root.appendingPathComponent("Ejemplo - Polaroid actualizado.pdf")
        let examples = root.appendingPathComponent("PolarMac/Examples", isDirectory: true)
        let oficioProject = examples.appendingPathComponent("Oficio horizontal.polar")
        let oficioPDF = examples.appendingPathComponent("Oficio horizontal.pdf")
        for destination in [updatedPDF, oficioProject, oficioPDF] {
            try require(!FileManager.default.fileExists(atPath: destination.path), "El ejemplo ya existe: \(destination.lastPathComponent).")
        }
        try PolarRenderer.writePDF(project: project, to: updatedPDF)
        let pdf = PDFDocument(url: updatedPDF)!
        try require(pdf.pageCount == 5, "El PDF actualizado debe tener 5 páginas.")
        let last = pdf.page(at: 4)!, lastText = last.string ?? ""
        for text in [project.settings.title, project.settings.subtitle] {
            try require(lastText.components(separatedBy: text).count - 1 == 8, "La última hoja debe tener exactamente 8 copias de «\(text)».")
        }
        let lastImage = try render(last, to: URL(fileURLWithPath: "/private/tmp/polar-example-last-page.png"), dpi: 300)
        let empty = PolarRenderer.cardRects(settings: project.settings)[8].insetBy(dx: -5, dy: -5)
        let scale: CGFloat = 300 / 72
        let crop = lastImage.cropping(to: CGRect(x: empty.minX * scale, y: empty.minY * scale,
                                               width: empty.width * scale, height: empty.height * scale).integral)!
        let context = CGContext(data: nil, width: crop.width, height: crop.height, bitsPerComponent: 8,
            bytesPerRow: crop.width * 4, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(crop, in: CGRect(x: 0, y: 0, width: crop.width, height: crop.height))
        let bytes = context.data!.assumingMemoryBound(to: UInt8.self)
        try require((0..<(crop.width * crop.height * 4)).allSatisfy { bytes[$0] == 255 },
                    "El noveno espacio, incluidos sus márgenes de corte, debe quedar totalmente blanco.")
        var oficio = PolarProject()
        oficio.settings.paperSize = .oficio; oficio.settings.orientation = .landscape
        oficio.settings.cardFormat = .landscape; oficio.settings.columns = 2; oficio.settings.rows = 2
        oficio.settings.title = "Un recuerdo para siempre"
        oficio.settings.subtitle = "Tú y yo"
        oficio.settings.drawBorders = false; oficio.settings.cutGuides = true; oficio.settings.cutStyle = .corners
        oficio.settings.textStyles[TextRole.title.rawValue] = TextAppearance(fontName: "Georgia")
        oficio.settings.textStyles[TextRole.subtitle.rawValue] = TextAppearance(fontName: "Georgia", italic: true)
        oficio.photos = Array(project.photos.prefix(4))
        oficio.placements = oficio.photos.map { PhotoPlacement(assetID: $0.id) }
        try oficio.validated()
        let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        try encoder.encode(oficio).write(to: oficioProject, options: .atomic)
        try PolarRenderer.writePDF(project: oficio, to: oficioPDF)
        let oficioDocument = PDFDocument(url: oficioPDF)!
        try require(oficioDocument.pageCount == 1 && oficioDocument.findString(oficio.settings.title, withOptions: []).count == 4,
                    "El ejemplo oficio debe tener una página y sus cuatro títulos completos.")
        let selectedFont = oficioDocument.findString(oficio.settings.title, withOptions: []).first!.attributedString?.attribute(.font, at: 0, effectiveRange: nil) as? NSFont
        try require(selectedFont?.familyName == "Georgia", "El ejemplo oficio debe conservar la fuente Georgia en el PDF.")
        _ = try render(oficioDocument.page(at: 0)!, to: URL(fileURLWithPath: "/private/tmp/polar-example-oficio.png"))
        let preservedData = try Data(contentsOf: original)
        try require(preservedData == originalData, "El proyecto original debe conservarse intacto.")
        print("Polaroid: 5 páginas, 44 fotos, última hoja con 8 títulos/subtítulos; noveno espacio completamente blanco.")
        print("PDF actualizado: \(try Data(contentsOf: updatedPDF).count) bytes.")
        print("Oficio horizontal: 340 × 216 mm, 2 × 2 tarjetas horizontales, Georgia, 4 fotos y marcas en esquinas.")
    }
}
