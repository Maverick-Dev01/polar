import AppKit
import ImageIO
import UniformTypeIdentifiers

/// «Mis moldes»: dHash estable, duplicados por SHA y por huella, persistencia, asistente de 3 pasos y que borrar un molde
/// no rompe los proyectos que ya lo usan.
@main @MainActor struct MoldChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }

    static let fixtures = URL(fileURLWithPath: ProcessInfo.processInfo.environment["POLAR_FIXTURES_DIR"] ?? "../shared-fixtures").appendingPathComponent("moldes")
    static func fixture(_ name: String) -> URL { fixtures.appendingPathComponent(name) }
    static func hash(_ url: URL) -> UInt64 { ImageFingerprint.dHash(ImageFingerprint.decode(try! Data(contentsOf: url))!) }

    static func main() async throws {
        let work = FileManager.default.temporaryDirectory.appendingPathComponent("polar-molds-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: work, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: work) }
        try fingerprints(work)
        try library(work)
        try wizard(work)
        try await studioFlow(work)
        print("MoldChecks: dHash exacto y estable (50 %, 200 %, JPG 80), distintos a más de 12 bits, duplicados por SHA y por huella, persistencia y borrado, asistente de 3 pasos y proyectos con su propia copia passed")
    }

    // MARK: dHash
    static func fingerprints(_ work: URL) throws {
        let document = try JSONSerialization.jsonObject(with: Data(contentsOf: fixture("moldes.json"))) as! [String: Any]
        let values = (document["dhash"] as! [String: Any])["values"] as! [String: String]
        for (name, expected) in values {
            check(ImageFingerprint.hex(hash(fixture(name))) == expected, "\(name): dHash \(ImageFingerprint.hex(hash(fixture(name)))) debe ser \(expected)")
        }
        let base = hash(fixture("molde-rects.png"))
        check(ImageFingerprint.hamming(base, hash(fixture("molde-rects-50.png"))) == 0, "rects vs rects-50: 0 bits")
        check(ImageFingerprint.hamming(base, hash(fixture("molde-distinto-tira.png"))) == 28, "rects vs tira: 28 bits")
        check(ImageFingerprint.hamming(base, hash(fixture("molde-rounded.png"))) == 5, "rects vs redondeado: 5 bits (parecido)")
        for other in ["molde-distinto-tira.png"] {
            check(ImageFingerprint.hamming(base, hash(fixture(other))) > 12, "moldes distintos a más de 12 bits")
        }
        // Reescalado al 50 % y al 200 %, y JPG calidad 80 (con los huecos blancos, como al guardar un JPG).
        let source = CGImageSourceCreateWithURL(fixture("molde-rects.png") as CFURL, nil)!
        let image = CGImageSourceCreateImageAtIndex(source, 0, nil)!
        for factor in [0.5, 2.0] {
            let scaled = scale(image, factor: factor)
            check(ImageFingerprint.hamming(base, ImageFingerprint.dHash(scaled)) <= 3, "reescalado \(Int(factor * 100)) %: huella estable")
        }
        let jpg = work.appendingPathComponent("rects.jpg")
        let flat = scale(image, factor: 1, onWhite: true)
        let destination = CGImageDestinationCreateWithURL(jpg as CFURL, UTType.jpeg.identifier as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, flat, [kCGImageDestinationLossyCompressionQuality: 0.8] as CFDictionary)
        check(CGImageDestinationFinalize(destination), "JPG de prueba")
        check(ImageFingerprint.hamming(base, hash(jpg)) <= 3, "JPG calidad 80: huella estable (\(ImageFingerprint.hamming(base, hash(jpg))) bits)")
    }

    static func scale(_ image: CGImage, factor: Double, onWhite: Bool = false) -> CGImage {
        let w = Int(Double(image.width) * factor), h = Int(Double(image.height) * factor)
        let context = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpace(name: CGColorSpace.sRGB)!,
                                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        if onWhite { context.setFillColor(CGColor(red: 1, green: 1, blue: 1, alpha: 1)); context.fill(CGRect(x: 0, y: 0, width: w, height: h)) }
        context.interpolationQuality = .high
        context.draw(image, in: CGRect(x: 0, y: 0, width: w, height: h))
        return context.makeImage()!
    }

    // MARK: Biblioteca
    static func library(_ work: URL) throws {
        let root = work.appendingPathComponent("app-library")
        let library = MoldLibrary(appRoot: root)
        check(library.list().isEmpty && library.findDuplicate(imageURL: fixture("molde-rects.png")) == nil, "biblioteca vacía")
        let regions = try TemplateImporter.read(url: fixture("molde-rects.png")).template.regions
        let saved = try library.save(imageURL: fixture("molde-rects.png"), nombre: "  Tres recuerdos  ", regiones: regions)
        check(saved.nombre == "Tres recuerdos" && saved.dhash.count == 16 && saved.sha256.count == 64 && saved.regiones.count == 3, "metadatos del molde")
        let folder = root.appendingPathComponent("templates/\(saved.id)")
        check(FileManager.default.fileExists(atPath: folder.appendingPathComponent("molde.png").path) &&
              FileManager.default.fileExists(atPath: folder.appendingPathComponent("meta.json").path), "carpeta templates/<id>/ con molde.png y meta.json")
        let meta = try JSONSerialization.jsonObject(with: Data(contentsOf: folder.appendingPathComponent("meta.json"))) as! [String: Any]
        check(Set(meta.keys) == ["id", "nombre", "dhash", "sha256", "regiones", "creado", "archivo", "ancho", "alto"], "meta.json lleva los campos acordados con Android: \(meta.keys.sorted())")
        // Persistencia: otra instancia lee lo mismo.
        let reopened = MoldLibrary(appRoot: root).list()
        check(reopened == [saved], "el molde persiste entre instancias")
        check((try? library.template(for: saved))?.regions == regions, "la plantilla conserva los huecos y sus formas")

        // Duplicados: el mismo archivo con otro nombre (SHA), el mismo molde a la mitad (huella), un JPG y un molde parecido.
        let copy = work.appendingPathComponent("otro-nombre.png")
        try FileManager.default.copyItem(at: fixture("molde-rects.png"), to: copy)
        let bySHA = library.findDuplicate(imageURL: copy)
        check(bySHA?.mold.id == saved.id && bySHA?.distance == 0, "duplicado por SHA")
        let half = library.findDuplicate(imageURL: fixture("molde-rects-50.png"))
        check(half?.mold.id == saved.id && half?.distance == 0, "duplicado por huella tras reescalar")
        let jpg = work.appendingPathComponent("rects.jpg")
        let found = library.findDuplicate(imageURL: jpg)
        check(found?.mold.id == saved.id, "el mismo molde como JPG es duplicado")
        check(library.findDuplicate(imageURL: fixture("molde-distinto-tira.png")) == nil, "un molde distinto no es duplicado")
        let similar = library.findDuplicate(imageURL: fixture("molde-rounded.png"))
        check(similar?.mold.id == saved.id && similar?.distance == 5, "misma plantilla con huecos redondeados: parecido, el usuario decide")

        // Guardar un JPG lo convierte a molde.png; el SHA es el del archivo original.
        let second = try library.save(imageURL: jpg, nombre: "Desde JPG", regiones: regions)
        let png = try Data(contentsOf: library.imageURL(second.id))
        check(png.prefix(4) == Data([0x89, 0x50, 0x4E, 0x47]), "molde.png es un PNG aunque la fuente sea JPG")
        let jpgData = try Data(contentsOf: jpg)
        check(second.sha256 == ImageFingerprint.sha256(jpgData), "sha256 del archivo original")
        check(library.list().count == 2, "dos moldes guardados")

        // Borrado.
        try library.delete(saved.id)
        check(library.list().map(\.id) == [second.id] && !FileManager.default.fileExists(atPath: folder.path), "borrar quita la carpeta y la lista")
        try library.delete("no-existe"); try library.delete("../" + second.id)
        check(library.list().count == 1, "borrar un id inexistente o con ruta no hace nada")
        try library.delete(second.id)
        check(library.list().isEmpty, "biblioteca vacía otra vez")
    }

    // MARK: Asistente
    static func wizard(_ work: URL) throws {
        let library = MoldLibrary(appRoot: work.appendingPathComponent("wizard-library"))
        var state = MoldWizardState()
        check(state.step == .choose && !state.canGoNext, "empieza en el paso 1 sin imagen")
        state.load(url: work.appendingPathComponent("no-existe.png"), library: library)
        check(state.errorMessage != nil && state.step == .choose && state.template == nil, "una imagen ilegible deja el error y se queda en el paso 1")
        state.load(url: fixture("molde-circles.png"), library: library)
        check(state.errorMessage == nil && state.step == .review && state.regions.count == 3 && state.detectedCount == 3, "imagen válida: pasa al paso 2 con 3 espacios")
        check(state.regions.allSatisfy { $0.shape == .ellipse } && state.name == "molde-circles", "formas detectadas y nombre propuesto")
        check(state.hint == nil, "sin avisos cuando las formas encajan")

        // Mover y asas: fracciones de la imagen, siempre dentro y con tamaño mínimo.
        let first = state.regions[0].rect
        state.moveRegion(0, dx: 0.1, dy: -0.5)
        check(abs(state.regions[0].x - (first.minX + 0.1)) < 1e-9 && state.regions[0].y == 0 && state.selected == 0, "mover se limita al borde de la imagen")
        state.moveRegion(0, dx: 5, dy: 0)
        check(abs(state.regions[0].rect.maxX - 1) < 1e-9, "no se sale por la derecha")
        let before = state.regions[1].rect
        state.resizeRegion(1, corner: .bottomTrailing, dx: 0.05, dy: 0.04)
        let after = state.regions[1].rect
        check(after.minX == before.minX && after.minY == before.minY && abs(after.width - (before.width + 0.05)) < 1e-9 && abs(after.height - (before.height + 0.04)) < 1e-9, "asa inferior derecha: la esquina opuesta no se mueve")
        state.resizeRegion(1, corner: .topLeading, dx: 0.02, dy: 0.02)
        check(abs(state.regions[1].rect.maxX - after.maxX) < 1e-9 && abs(state.regions[1].rect.maxY - after.maxY) < 1e-9, "asa superior izquierda: la esquina opuesta no se mueve")
        state.resizeRegion(1, corner: .topTrailing, dx: -5, dy: 0)
        check(abs(state.regions[1].width - MoldWizardState.minimumSize) < 1e-9, "el tamaño mínimo es 2 %")
        state.scaleRegion(2, factor: 0.5)
        check(abs(state.regions[2].rect.midX - 0.7167) < 0.01, "reducir mantiene el centro")
        // Forma y radio.
        state.setShape(2, .round); check(state.regions[2].shape == .round && state.regions[2].radius == 0.2, "redondeado propone 20 %")
        state.setRadius(2, 0.35); check(state.regions[2].radius == 0.35, "radio editable")
        state.setShape(2, .rect); check(state.regions[2].radius == 0, "rectángulo sin radio")
        // Agregar y quitar.
        check(state.addRegion() && state.regions.count == 4 && state.selected == 3, "agregar espacio lo selecciona")
        check(state.removeSelected() && state.regions.count == 3 && state.selected == 2, "quitar espacio")
        while state.regions.count > 1 { state.select(0); state.removeSelected() }
        check(!state.removeSelected() && state.regions.count == 1, "siempre queda al menos un espacio")
        for _ in 0..<80 { state.addRegion() }
        check(state.regions.count == 64, "tope de 64 espacios")
        while state.regions.count > 3 { state.select(0); state.removeSelected() }

        // Navegación entre pasos.
        state.next(); check(state.step == .save, "siguiente lleva al paso 3")
        state.name = "   "; check(!state.canGoNext, "sin nombre no se guarda")
        state.name = "Mis círculos"; check(state.canGoNext, "con nombre sí")
        state.back(); check(state.step == .review, "atrás vuelve al paso 2")
        state.back(); check(state.step == .choose, "atrás vuelve al paso 1")
        state.next(); check(state.step == .review, "desde el paso 1 con imagen cargada se puede seguir")
        state.next()
        let mold = try state.save(in: library)
        check(mold.nombre == "Mis círculos" && mold.regiones == state.regions && library.list() == [mold], "guardar escribe el molde revisado")

        // Aviso de molde parecido entre los pasos 1 y 2.
        var again = MoldWizardState()
        again.load(url: fixture("molde-circles.png"), library: library)
        check(again.awaitingDuplicateDecision && again.duplicate?.mold.id == mold.id && again.duplicate?.distance == 0 && again.step == .choose, "molde parecido: el aviso aparece antes del paso 2")
        check(!again.canGoNext, "no se avanza sin decidir")
        again.next(); check(again.step == .choose, "siguiente no salta el aviso")
        again.keepAsNew(); check(again.step == .review && again.duplicate == nil, "«Guardar como nuevo» sigue al paso 2")
        var back = MoldWizardState()
        back.load(url: fixture("molde-circles.png"), library: library)
        back.back(); check(back.duplicate == nil && back.step == .choose && back.template != nil, "atrás cierra el aviso")
        // Imagen sin huecos claros.
        let plain = work.appendingPathComponent("plana.png")
        try Data(contentsOf: fixture("molde-rects.png")).write(to: plain)
        var flat = MoldWizardState()
        flat.load(url: plain, library: MoldLibrary(appRoot: work.appendingPathComponent("vacia")))
        check(flat.step == .review && flat.hint == nil, "molde con huecos no muestra aviso")
    }

    static func solidPhoto(in directory: URL) throws -> PhotoAsset {
        let width = 600, height = 450
        var bytes = [UInt8](repeating: 255, count: width * height * 4)
        for i in 0..<(width * height) { bytes[i * 4] = 200; bytes[i * 4 + 1] = 40; bytes[i * 4 + 2] = 40 }
        let image = CGImage(width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: width * 4, space: CGColorSpaceCreateDeviceRGB(),
                            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue), provider: CGDataProvider(data: Data(bytes) as CFData)!,
                            decode: nil, shouldInterpolate: false, intent: .defaultIntent)!
        let url = directory.appendingPathComponent("foto.png")
        let destination = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, nil)
        check(CGImageDestinationFinalize(destination), "foto de prueba")
        return PhotoAsset(path: url.path, pixelWidth: width, pixelHeight: height)
    }

    // MARK: Proyectos con su propia copia
    static func studioFlow(_ work: URL) async throws {
        let root = work.appendingPathComponent("studio")
        let studio = Studio(storageRoot: root)
        studio.preferences.onboardingSeen = true
        studio.project = PolarProject(); studio.project.name = "Con molde"
        let photo = try solidPhoto(in: work)
        studio.project.photos = [photo]
        // Asistente completo por el Studio: «Guardar y usar en este proyecto».
        studio.importTemplate()
        check(studio.moldWizard?.step == .choose, "importar molde abre el asistente")
        var state = studio.moldWizard!
        state.load(url: fixture("molde-rounded.png"), library: studio.molds)
        state.name = "Redondeado"
        studio.moldWizard = state
        try studio.moldFlowChecks()
    }
}

extension Studio {
    /// Sólo para la prueba: termina el asistente como el botón de guardar y recorre lo que pasa con el proyecto.
    func moldFlowChecks() throws {
        guard var state = moldWizard else { return }
        state.next(); moldWizard = state
        precondition(state.step == .save, "el paso 2 lleva al paso 3")
        finishMoldWizard(.currentProject)
        precondition(project.settings.style == .imported, "usar el molde cambia el diseño a «molde importado»")
        precondition(savedMolds.count == 1 && savedMolds[0].nombre == "Redondeado" && moldWizard == nil, "el molde queda en Mis moldes y el asistente se cierra")
        let template = project.settings.importedTemplate!
        precondition(template.regions.allSatisfy { $0.shape == .round && abs($0.radius - 0.2) <= 0.05 }, "el proyecto usa los huecos redondeados")
        let projectFolder = library.directory(projectID).path + "/"
        precondition(template.path.hasPrefix(projectFolder) && FileManager.default.fileExists(atPath: template.path), "el proyecto lleva su propia copia de la imagen: \(template.path)")
        // Colocar una foto en el primer hueco y exportar; después borrar el molde: el proyecto sigue exportando.
        change { $0.placements = [PhotoPlacement(assetID: $0.photos[0].id)]; $0.normalized() }
        let pdf = FileManager.default.temporaryDirectory.appendingPathComponent("polar-molde-\(UUID().uuidString).pdf")
        defer { try? FileManager.default.removeItem(at: pdf) }
        do { try PolarRenderer.writePDF(project: project, to: pdf) } catch { preconditionFailure("exportar con molde: \(error)") }
        // Recorte por forma: la esquina del hueco redondeado no lleva foto; el centro sí.
        let card = PolarRenderer.cardRects(settings: project.settings)[0]
        let paper = project.settings.paperSizePoints
        let image = PolarRenderer.preview(project: project, page: 0, scale: 2, printReady: true).cgImage(forProposedRect: nil, context: nil, hints: nil)!
        var bytes = [UInt8](repeating: 0, count: image.width * image.height * 4)
        let context = CGContext(data: &bytes, width: image.width, height: image.height, bitsPerComponent: 8, bytesPerRow: image.width * 4,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        func at(_ p: CGPoint) -> (Int, Int, Int) {
            let x = Int(p.x / paper.width * CGFloat(image.width)), y = Int(p.y / paper.height * CGFloat(image.height)), i = (y * image.width + x) * 4
            return (Int(bytes[i]), Int(bytes[i + 1]), Int(bytes[i + 2]))
        }
        let center = at(CGPoint(x: card.midX, y: card.midY)), corner = at(CGPoint(x: card.minX + 1, y: card.minY + 1))
        precondition(center.0 > 150 && center.1 < 100, "el centro del hueco muestra la foto (\(center))")
        precondition(!(corner.0 > 150 && corner.1 < 100), "la esquina del hueco redondeado queda recortada (\(corner))")
        let mold = savedMolds[0]
        deleteMold(mold)
        precondition(savedMolds.isEmpty && !FileManager.default.fileExists(atPath: molds.directory(mold.id).path), "el molde se quita de Mis moldes")
        precondition(FileManager.default.fileExists(atPath: project.settings.importedTemplate!.path), "borrar el molde no toca la copia del proyecto")
        do { try PolarRenderer.writePDF(project: project, to: pdf) } catch { preconditionFailure("exportar después de borrar el molde: \(error)") }
        // Reabrir el proyecto guardado: también funciona.
        flush()
        let reopened = try! library.load(projectID)
        precondition(reopened.settings.importedTemplate?.regions == project.settings.importedTemplate?.regions && FileManager.default.fileExists(atPath: reopened.settings.importedTemplate!.path), "el proyecto guardado conserva sus huecos y su imagen")
        // «Usar el existente» desde Mis moldes en otro molde.
        let regions = reopened.settings.importedTemplate!.regions
        let other = try! molds.save(imageURL: URL(fileURLWithPath: ProcessInfo.processInfo.environment["POLAR_FIXTURES_DIR"]! + "/moldes/molde-distinto-tira.png"), nombre: "Tira", regiones: regions)
        refreshMolds(); useMold(other)
        precondition(project.settings.importedTemplate?.path.hasPrefix(projectFolder) == true && savedMolds.count == 1, "usar un molde guardado copia la imagen al proyecto")
        try? FileManager.default.removeItem(at: library.root)
    }
}
