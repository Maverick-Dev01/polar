import AppKit
import ImageIO
import PDFKit

/// Los 6 diseños nuevos de la fase 3: paridad con `shared-fixtures/estilos-geometria.json`, dibujo en 3 papeles y 2 orientaciones,
/// formas de foto, QR en todos los diseños musicales y compatibilidad del .polar con estilos y formas desconocidos.
@main @MainActor struct StyleGeometryChecks {
    static let newStyles: [TemplateStyle] = [.photobooth, .instaxWide, .vinyl, .cassette, .collage, .washi]

    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }

    static func main() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("polar-geometry-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        try parity()
        let photos = try [("rojo", (230, 30, 30)), ("verde", (30, 200, 60)), ("azul", (30, 60, 230)), ("amarillo", (240, 220, 30))].map { try solidPhoto($0.1, name: $0.0, in: directory) }
        try rendersEverywhere(photos[0], directory: directory)
        shapes(photos)
        qr(photos[0])
        compatibility()
        photosPerCard()
        print("StyleGeometryChecks: paridad con el fixture compartido, 6 diseños × 3 papeles × 2 orientaciones, formas de foto, QR en 5 diseños musicales, estilos y formas desconocidos y fotos por tarjeta passed")
    }

    // MARK: Paridad con el fixture compartido (0.5 pt)
    static func parity() throws {
        guard let url = StyleGeometryTable.locate() else { check(false, "POLAR_GEOMETRY_JSON debe apuntar a shared-fixtures/estilos-geometria.json"); return }
        let root = try JSONSerialization.jsonObject(with: Data(contentsOf: url)) as! [String: Any]
        let styles = root["styles"] as! [String: [String: Any]]
        check(Set(styles.keys) == Set(newStyles.map(\.rawValue)), "el fixture define exactamente los 6 diseños nuevos")
        let categories = ["CLASSIC": "Clásicos", "MUSIC": "Música", "FREE": "Libre", "OCCASIONS": "Ocasiones"]
        let studio = Studio(storageRoot: FileManager.default.temporaryDirectory.appendingPathComponent("polar-geometry-studio-\(UUID().uuidString)"))
        defer { try? FileManager.default.removeItem(at: studio.library.root) }
        for style in newStyles {
            let entry = styles[style.rawValue]!
            check(style.isDataDriven && style.name == entry["displayName"] as? String, "\(style.rawValue): nombre del fixture")
            check(style.photosPerCard == entry["photosPerCard"] as? Int, "\(style.rawValue): fotos por tarjeta")
            check(abs(Double(style.aspect) - (entry["cardAspect"] as! Double)) < 0.0005, "\(style.rawValue): proporción de la tarjeta")
            check(style.supportsDate == entry["supportsDate"] as? Bool, "\(style.rawValue): admite fecha")
            check(style.category == categories[entry["category"] as! String], "\(style.rawValue): categoría")
            check(style.isMusic == (entry["category"] as? String == "MUSIC"), "\(style.rawValue): es musical sólo si el fixture lo dice")
            studio.project.selectStyle(style); studio.selectedSlot = 0
            check(studio.textRoles.map { $0.rawValue.uppercased() } == (entry["textRoles"] as! [String]), "\(style.rawValue): roles de texto")

            // Los marcos de foto del código coinciden con los del fixture a 180 pt (referencia) y a otro ancho.
            let slots = entry["photoSlots"] as! [[String: Any]]
            for width in [180.0, 263.0] {
                let card = CGRect(x: 17, y: 29, width: width, height: width / Double(style.aspect))
                let rects = PolarRenderer.photoRects(in: card, style: style, settings: PrintSettings())
                check(rects.count == slots.count, "\(style.rawValue): cantidad de marcos de foto")
                for (rect, slot) in zip(rects, slots) {
                    let expected = CGRect(x: card.minX + (slot["x"] as! Double) * card.width, y: card.minY + (slot["y"] as! Double) * card.height,
                                          width: (slot["w"] as! Double) * card.width, height: (slot["h"] as! Double) * card.height)
                    check(abs(rect.minX - expected.minX) <= 0.5 && abs(rect.minY - expected.minY) <= 0.5 &&
                          abs(rect.width - expected.width) <= 0.5 && abs(rect.height - expected.height) <= 0.5, "\(style.rawValue): marco de foto fuera de 0.5 pt \(rect) vs \(expected)")
                }
                for (index, slot) in slots.enumerated() {
                    let (shape, radius) = PolarRenderer.photoShape(style: style, index: index)
                    check(shape.rawValue == slot["shape"] as? String && abs(Double(radius) - (slot["radius"] as! Double)) < 0.0001, "\(style.rawValue): forma de la foto \(index)")
                }
            }
            // Los huecos de texto de la tabla cargada son los del fixture (el dibujo los lee de ahí).
            let text = entry["textSlots"] as! [[String: Any]]
            let loaded = StyleGeometryTable.geometry(style)!
            check(loaded.textSlots.count == text.count && zip(loaded.textSlots, text).allSatisfy { slot, raw in
                slot.role == raw["role"] as? String && abs(slot.x - (raw["x"] as! Double)) < 0.0005 && abs(slot.y - (raw["y"] as! Double)) < 0.0005 &&
                abs(slot.w - (raw["w"] as! Double)) < 0.0005 && abs(slot.h - (raw["h"] as! Double)) < 0.0005 && slot.defaultFont == raw["defaultFont"] as? String
            }, "\(style.rawValue): huecos de texto")
            check(loaded.decorations.count == (entry["decorations"] as! [Any]).count, "\(style.rawValue): adornos")
            check((loaded.qrSlot != nil) == (entry["qrSlot"] is [String: Any]), "\(style.rawValue): QR")
        }
        check(StyleGeometryTable.referenceCardWidthPt == CGFloat(root["referenceCardWidthPt"] as! Double), "ancho de referencia")
    }

    // MARK: Fotos de prueba y muestreo
    static func solidPhoto(_ rgb: (Int, Int, Int), name: String, in directory: URL) throws -> PhotoAsset {
        let width = 600, height = 450
        var bytes = [UInt8](repeating: 255, count: width * height * 4)
        for i in 0..<(width * height) { bytes[i * 4] = UInt8(rgb.0); bytes[i * 4 + 1] = UInt8(rgb.1); bytes[i * 4 + 2] = UInt8(rgb.2) }
        let image = CGImage(width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: width * 4, space: CGColorSpaceCreateDeviceRGB(),
                            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue), provider: CGDataProvider(data: Data(bytes) as CFData)!,
                            decode: nil, shouldInterpolate: false, intent: .defaultIntent)!
        let url = directory.appendingPathComponent("\(name).png")
        let destination = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, nil)
        check(CGImageDestinationFinalize(destination), "foto de prueba")
        return PhotoAsset(path: url.path, pixelWidth: width, pixelHeight: height)
    }

    static func project(_ style: TemplateStyle, photos: [PhotoAsset], paper: PaperSize = .letter, orientation: PaperOrientation = .portrait, url: String = "") -> PolarProject {
        var project = PolarProject()
        project.photos = photos
        project.selectStyle(style)
        project.settings.paperSize = paper; project.settings.orientation = orientation
        project.settings.songURL = url
        project.settings.cutGuides = false
        project.placements = (0..<style.photosPerCard).map { PhotoPlacement(assetID: photos[$0 % photos.count].id) }
        project.normalized()
        return project
    }

    static func bitmap(_ project: PolarProject) -> (CGImage, [UInt8]) {
        let image = PolarRenderer.preview(project: project, page: 0, scale: 2, printReady: true).cgImage(forProposedRect: nil, context: nil, hints: nil)!
        var bytes = [UInt8](repeating: 0, count: image.width * image.height * 4)
        let context = CGContext(data: &bytes, width: image.width, height: image.height, bitsPerComponent: 8, bytesPerRow: image.width * 4,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        return (image, bytes)
    }
    /// Color en el punto (x, y) de la hoja, en puntos, con origen arriba a la izquierda.
    static func pixel(_ bitmap: (CGImage, [UInt8]), _ point: CGPoint, paper: CGSize) -> (Int, Int, Int) {
        let x = min(bitmap.0.width - 1, Int(point.x / paper.width * CGFloat(bitmap.0.width))), y = min(bitmap.0.height - 1, Int(point.y / paper.height * CGFloat(bitmap.0.height)))
        let i = (y * bitmap.0.width + x) * 4
        return (Int(bitmap.1[i]), Int(bitmap.1[i + 1]), Int(bitmap.1[i + 2]))
    }
    static func isRed(_ c: (Int, Int, Int)) -> Bool { c.0 > 190 && c.1 < 90 && c.2 < 90 }
    /// Las 4 fotos de prueba, en orden: rojo, verde, azul, amarillo.
    static func matches(_ index: Int, _ c: (Int, Int, Int)) -> Bool {
        switch index {
        case 0: return isRed(c)
        case 1: return c.1 > 150 && c.0 < 90
        case 2: return c.2 > 190 && c.0 < 90
        default: return c.0 > 190 && c.1 > 170 && c.2 < 90
        }
    }

    // MARK: 6 diseños × 3 papeles × 2 orientaciones
    static func rendersEverywhere(_ photo: PhotoAsset, directory: URL) throws {
        for style in newStyles { for paper in [PaperSize.letter, .a4, .photo4x6] { for orientation in PaperOrientation.allCases {
            var p = project(style, photos: [photo], paper: paper, orientation: orientation)
            p.settings.dateSource = .chosen; p.settings.chosenDate = Date(timeIntervalSince1970: 1_790_000_000)
            let cards = PolarRenderer.cardRects(project: p, page: 0)
            check(!cards.isEmpty && cards.allSatisfy { $0.width > 20 && $0.height > 20 }, "\(style.rawValue) \(paper) \(orientation): tarjetas con tamaño útil")
            let preview = PolarRenderer.preview(project: p, page: 0)
            check(preview.size.width > 0, "\(style.rawValue) vista previa")
            let pdf = directory.appendingPathComponent("\(style.rawValue)-\(paper.rawValue)-\(orientation.rawValue).pdf")
            try PolarRenderer.writePDF(project: p, to: pdf)
            check(PDFDocument(url: pdf)?.pageCount == 1, "\(style.rawValue) \(paper) \(orientation): PDF de una hoja")
            try? FileManager.default.removeItem(at: pdf)
        } } }
    }

    // MARK: Formas de la foto y adornos
    static func shapes(_ photos: [PhotoAsset]) {
        let paper = PrintSettings().paperSizePoints
        // Vinilo: la foto es un círculo; la esquina de su marco queda fuera de la foto y el centro es la foto.
        var vinyl = project(.vinyl, photos: photos)
        var bitmapImage = bitmap(vinyl)
        var card = PolarRenderer.cardRects(project: vinyl, page: 0)[0]
        var slot = PolarRenderer.photoRects(in: card, style: .vinyl, settings: vinyl.settings)[0]
        check(isRed(pixel(bitmapImage, CGPoint(x: slot.midX, y: slot.midY - slot.height * 0.3), paper: paper)), "vinilo: el centro muestra la foto")
        check(!isRed(pixel(bitmapImage, CGPoint(x: slot.minX + 0.5, y: slot.minY + 0.5), paper: paper)), "vinilo: la esquina del marco queda fuera del círculo")
        check(isRed(pixel(bitmapImage, CGPoint(x: slot.midX + slot.width * 0.3, y: slot.midY), paper: paper)), "vinilo: dentro del círculo")
        let hole = pixel(bitmapImage, CGPoint(x: card.minX + card.width * 0.5, y: card.minY + card.height * 0.38), paper: paper)
        check(hole.0 > 235 && hole.1 > 225, "vinilo: el agujero central se dibuja encima de la foto")
        vinyl.settings.title = "x"

        // Casete: esquinas redondeadas del 8 % del lado menor.
        let cassette = project(.cassette, photos: photos)
        bitmapImage = bitmap(cassette)
        card = PolarRenderer.cardRects(project: cassette, page: 0)[0]
        slot = PolarRenderer.photoRects(in: card, style: .cassette, settings: cassette.settings)[0]
        check(isRed(pixel(bitmapImage, CGPoint(x: slot.midX, y: slot.midY), paper: paper)), "casete: el centro muestra la foto")
        check(!isRed(pixel(bitmapImage, CGPoint(x: slot.minX + 0.6, y: slot.minY + 0.6), paper: paper)), "casete: la esquina del marco queda recortada")
        check(isRed(pixel(bitmapImage, CGPoint(x: slot.minX + slot.width * 0.5, y: slot.minY + 2), paper: paper)), "casete: el borde recto conserva la foto")

        // Fotomatón y collage: cada foto en su hueco.
        let booth = project(.photobooth, photos: photos)
        bitmapImage = bitmap(booth)
        card = PolarRenderer.cardRects(project: booth, page: 0)[0]
        let boothSlots = PolarRenderer.photoRects(in: card, style: .photobooth, settings: booth.settings)
        for (index, rect) in boothSlots.enumerated() {
            let c = pixel(bitmapImage, CGPoint(x: rect.midX, y: rect.midY), paper: paper)
            check(matches(index, c), "fotomatón: la foto \(index + 1) en su hueco (\(c))")
        }
        let collage = project(.collage, photos: photos)
        bitmapImage = bitmap(collage)
        card = PolarRenderer.cardRects(project: collage, page: 0)[0]
        for (index, rect) in PolarRenderer.photoRects(in: card, style: .collage, settings: collage.settings).enumerated() {
            let c = pixel(bitmapImage, CGPoint(x: rect.midX, y: rect.midY), paper: paper)
            check(matches(index, c), "collage: la foto \(index + 1) en su hueco (\(c))")
        }

        // Washi: la cinta se dibuja encima de la esquina de la foto (capa «above»).
        let washi = project(.washi, photos: photos)
        bitmapImage = bitmap(washi)
        card = PolarRenderer.cardRects(project: washi, page: 0)[0]
        let tape = pixel(bitmapImage, CGPoint(x: card.minX + card.width * 0.12, y: card.minY + card.height * 0.085), paper: paper)
        check(!isRed(tape) && tape.0 > 180, "washi: la cinta cubre la esquina de la foto (\(tape))")
        let body = pixel(bitmapImage, CGPoint(x: card.minX + card.width * 0.5, y: card.minY + card.height * 0.4), paper: paper)
        check(isRed(body), "washi: la foto se ve en el centro")
        // Instantánea ancha: proporción horizontal y pie de texto sobre fondo blanco.
        let wide = project(.instaxWide, photos: photos)
        card = PolarRenderer.cardRects(project: wide, page: 0)[0]
        check(card.width > card.height, "instantánea ancha: tarjeta horizontal")
    }

    // MARK: QR en todos los diseños musicales
    static func qr(_ photo: PhotoAsset) {
        let paper = PrintSettings().paperSizePoints
        for style in [TemplateStyle.spotify, .playerRed, .playerGray, .vinyl, .cassette] {
            var counts: [Int] = []
            for link in ["", "https://open.spotify.com/track/ejemplo"] {
                let p = project(style, photos: [photo], url: link)
                let image = bitmap(p)
                let card = PolarRenderer.cardRects(project: p, page: 0)[0]
                // Zona del QR: el fixture para vinilo y casete; para los demás, su posición propia (misma que usa el dibujo).
                let slot = StyleGeometryTable.geometry(style)?.qrSlot ?? LegacyQRSlot.slot(style, aspect: style.aspect) ?? QRSlotGeometry(x: 0.76, y: 0.84, size: 0.2, sizeBasis: "width")
                let side = slot.size * card.width
                var dark = 0
                for ix in 0..<24 { for iy in 0..<24 {
                    let point = CGPoint(x: card.minX + slot.x * card.width + side * (Double(ix) + 0.5) / 24, y: card.minY + slot.y * card.height + side * (Double(iy) + 0.5) / 24)
                    let c = pixel(image, point, paper: paper)
                    if c.0 < 60 && c.1 < 60 && c.2 < 60 { dark += 1 }
                } }
                counts.append(dark)
            }
            check(counts[0] == 0 && counts[1] > 20, "\(style.rawValue): el QR sólo se dibuja con enlace (sin enlace \(counts[0]), con enlace \(counts[1]))")
        }
    }

    // MARK: Compatibilidad del .polar
    static func compatibility() {
        func decode(_ json: String) -> PolarProject? { try? JSONDecoder().decode(PolarProject.self, from: Data(json.utf8)) }
        // Un id de diseño de una versión más nueva degrada a Polaroid sin fallar.
        let future = decode("{\"settings\":{\"style\":\"disenoDelFuturo\"},\"pageDesigns\":{\"0\":{\"style\":\"disenoDelFuturo\",\"format\":\"original\"}},\"cardOverrides\":{\"0\":{\"designStyle\":\"disenoDelFuturo\",\"texts\":{\"title\":\"Hola\"}}}}")
        check(future != nil && future!.settings.style == .polaroid, "un estilo desconocido degrada a Polaroid")
        check(future?.pageDesigns.isEmpty == true && future?.cardOverrides["0"]?.designStyle == nil && future?.cardOverrides["0"]?.texts["title"] == "Hola", "diseños por hoja o tarjeta desconocidos se descartan y el texto se conserva")
        check((try? future?.validated()) != nil, "el proyecto degradado pasa la validación")
        // Un estilo nuevo guardado hoy se lee igual, con el diseño por hoja intacto si la cantidad de fotos coincide.
        var mine = PolarProject(); mine.selectStyle(.photobooth)
        mine.placements = Array(repeating: nil, count: 4); mine.pageDesigns["0"] = PageDesign(style: .collage, format: .original)
        let restoredBooth = decode(String(data: try! JSONEncoder().encode(mine), encoding: .utf8)!)
        check(restoredBooth?.settings.style == .photobooth, "el estilo nuevo sobrevive guardar y abrir")
        check(restoredBooth?.pageDesigns.isEmpty == true, "un diseño por hoja de otra cantidad de fotos se descarta al abrir en vez de fallar")
        for style in newStyles {
            var project = PolarProject(); project.selectStyle(style)
            let again = decode(String(data: try! JSONEncoder().encode(project), encoding: .utf8)!)
            check(again?.settings.style == style, "\(style.rawValue) sobrevive guardar y abrir")
        }
        // Huecos de molde sin forma ni radio (proyecto de una versión anterior) son rectángulos.
        let legacy = try? JSONDecoder().decode(TemplateRegion.self, from: Data("{\"id\":\"\(UUID().uuidString)\",\"x\":0.1,\"y\":0.1,\"width\":0.3,\"height\":0.3,\"isTransparent\":true}".utf8))
        check(legacy?.shape == .rect && legacy?.radius == 0 && legacy?.isTransparent == true, "forma y radio opcionales: rectángulo por defecto")
        let unknown = try? JSONDecoder().decode(TemplateRegion.self, from: Data("{\"x\":0.1,\"y\":0.1,\"width\":0.3,\"height\":0.3,\"shape\":\"hexagono\",\"radius\":0.3}".utf8))
        check(unknown?.shape == .rect, "una forma desconocida se lee como rectángulo")
        let round = TemplateRegion(x: 0.1, y: 0.1, width: 0.3, height: 0.3, shape: .round, radius: 0.2)
        let back = try? JSONDecoder().decode(TemplateRegion.self, from: JSONEncoder().encode(round))
        check(back == round, "forma y radio sobreviven guardar y abrir")
        // Prueba cruzada: un .polar con estilo nuevo y un molde de huecos redondeados, como lo escribe Android, se abre y se dibuja.
        let mold = (ProcessInfo.processInfo.environment["POLAR_FIXTURES_DIR"] ?? "../shared-fixtures") + "/moldes/molde-rounded.png"
        func regionJSON(_ x: Double, _ y: Double, _ w: Double, _ h: Double) -> String {
            "{\"id\":\"\(UUID().uuidString)\",\"x\":\(x),\"y\":\(y),\"width\":\(w),\"height\":\(h),\"isTransparent\":true,\"shape\":\"round\",\"radius\":0.2}"
        }
        let template = "{\"path\":\"\(mold)\",\"pixelWidth\":1200,\"pixelHeight\":1600,\"regions\":[\(regionJSON(0.08, 0.06, 0.84, 0.375)),\(regionJSON(0.08, 0.475, 0.4, 0.3125))]}"
        let photoID = UUID().uuidString
        let photoJSON = "\"photos\":[{\"id\":\"\(photoID)\",\"path\":\"\(mold)\",\"pixelWidth\":1200,\"pixelHeight\":1600}]"
        for style in ["cassette", "imported"] {
            let polar = "{\"version\":1,\"name\":\"Desde Android\",\"settings\":{\"style\":\"\(style)\",\"songURL\":\"https://ejemplo.mx\",\"importedTemplate\":\(template)},\(photoJSON),\"placements\":[{\"assetID\":\"\(photoID)\"}]}"
            let cross = decode(polar)
            check(cross != nil && (try? cross?.validated()) != nil, "\(style): el .polar de Android se abre y valida")
            check(cross?.settings.importedTemplate?.regions.allSatisfy { $0.shape == .round && $0.radius == 0.2 } == true, "\(style): formas redondas leídas")
            if let cross { check(PolarRenderer.preview(project: cross, page: 0, scale: 1, printReady: true).size.width > 0, "\(style): se dibuja") }
        }
        var invalid = PolarProject(); invalid.settings.style = .imported
        invalid.settings.importedTemplate = ImportedTemplate(path: "/tmp/m.png", pixelWidth: 10, pixelHeight: 10, regions: [TemplateRegion(x: 0, y: 0, width: 0.5, height: 0.5, shape: .round, radius: 0.9)])
        check((try? invalid.validated()) == nil, "un radio fuera de 0...0.5 se rechaza")
    }

    static func photosPerCard() {
        var project = PolarProject(); project.selectStyle(.photobooth)
        check(project.settings.capacity == 12 && project.cardsPerPage == 3, "fotomatón: 3 tiras de 4 fotos por hoja")
        check(!project.compatibleStyle(.polaroid) && project.compatibleStyle(.photobooth) && !project.compatibleStyle(.collage), "sólo se combinan diseños con la misma cantidad de fotos")
        project.selectStyle(.collage)
        check(project.settings.capacity == 18 && project.cardsPerPage == 6, "collage: 6 tarjetas de 3 fotos por hoja")
    }
}
