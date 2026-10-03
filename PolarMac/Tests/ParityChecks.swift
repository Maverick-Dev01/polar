import AppKit
import ImageIO
import PDFKit

@main struct ParityChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }

    static func main() throws {
        var document = try JSONSerialization.jsonObject(with: JSONEncoder().encode(PolarProject())) as! [String: Any]
        document["name"] = "Desde Android"
        document["updatedAtEpochMs"] = Int64(1_771_070_400_000)
        document["cardOverrides"] = ["1": ["texts": ["title": "Sólo esta foto"], "styles": [:], "dateSource": "chosen", "chosenDate": 793_195_200.0]]
        var settings = document["settings"] as! [String: Any]
        settings["dateSource"] = "photo"
        settings["dateStyle"] = "numeric"
        settings["extraTextStyles"] = ["date": try JSONSerialization.jsonObject(with: JSONEncoder().encode(TextAppearance(fontName: "Caveat"))) ]
        document["settings"] = settings
        let loaded = try JSONDecoder().decode(PolarProject.self, from: JSONSerialization.data(withJSONObject: document))
        let saved = try JSONSerialization.jsonObject(with: JSONEncoder().encode(loaded)) as! [String: Any]
        check(saved["name"] as? String == "Desde Android", "Android project metadata survives Mac save")
        check(saved["cardOverrides"] != nil, "per-card text survives Mac save")
        check((saved["settings"] as? [String: Any])?["extraTextStyles"] != nil, "date appearance survives Mac save")
        let empty = try JSONDecoder().decode(PolarProject.self, from: Data("{}".utf8))
        check(empty.settings.dateSource == .none && empty.cardOverrides.isEmpty && empty.name.isEmpty, "legacy project defaults")
        let restored = try JSONDecoder().decode(PolarProject.self, from: JSONEncoder().encode(loaded))
        check(restored == loaded, "all Android fields roundtrip")
        let partial = try JSONDecoder().decode(CardOverride.self, from: Data("{\"texts\":{\"title\":\"\"}}".utf8))
        check(partial.styles.isEmpty && partial.texts["title"] == "", "partial override defaults and explicit empty text")

        for invalid in [CardOverride(texts: ["unknown": "x"]), CardOverride(texts: ["title": String(repeating: "x", count: 501)]),
                        CardOverride(styles: ["date": TextAppearance(offsetX: 61)])] {
            var bad = loaded; bad.cardOverrides = ["0": invalid]
            do { try bad.validated(); check(false, "reject malformed card overrides") } catch {}
        }
        var bad = loaded; bad.cardOverrides = ["-1": CardOverride(texts: ["title": "x"])]
        do { try bad.validated(); check(false, "reject negative card indices") } catch {}
        bad = loaded; bad.settings.chosenDate = Date(timeIntervalSinceReferenceDate: .infinity)
        do { try bad.validated(); check(false, "reject non-finite dates") } catch {}

        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("polar-parity-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let photoURL = directory.appendingPathComponent("dated.jpg")
        try fixture(to: photoURL)
        guard let photo = PhotoImporter.read(urls: [photoURL]).photos.first else { fatalError("synthetic photo must import") }
        check(photo.pixelWidth == 120 && photo.pixelHeight == 80, "EXIF orientation retained")
        check(photo.takenAtEpochMs != nil, "photo import retains EXIF capture date")
        var project = PolarProject()
        project.photos = [photo]
        project.placements = [PhotoPlacement(assetID: photo.id), nil, PhotoPlacement(assetID: photo.id)]
        project.settings.dateSource = .photo
        check(TextResolver.text(project: project, card: 0, role: .date) == "14 feb 2026", "photo capture date")
        check(TextResolver.text(project: project, card: 1, role: .date).isEmpty, "missing photo produces no date")
        project.settings.dateStyle = .numeric
        check(TextResolver.text(project: project, card: 0, role: .date) == "14.02.26", "numeric photo date")
        project.settings.dateStyle = .monthYear
        check(TextResolver.text(project: project, card: 0, role: .date) == "febrero 2026", "Spanish month and year")
        project.settings.dateStyle = .dayMonthYear
        project.settings.chosenDate = Date(timeIntervalSince1970: 1_791_806_400)
        project.cardOverrides["2"] = CardOverride(texts: ["title": "La segunda foto"], styles: ["title": TextAppearance(hex: "0000FF")], dateSource: .chosen)
        check(TextResolver.text(project: project, card: 2, role: .date, zone: TimeZone(secondsFromGMT: 0)!) == "12 oct 2026", "chosen date inherits general date")
        check(TextResolver.appearance(project: project, card: 2, role: .title).hex == "0000FF", "card appearance overrides general")
        project.cardOverrides["1"] = partial
        check(TextResolver.text(project: project, card: 1, role: .title).isEmpty, "empty own text does not fall back")
        project.settings.setTextStyle(.date, TextAppearance(fontName: "Caveat"))
        check(project.settings.extraTextStyles["date"] != nil && project.settings.textStyles["date"] == nil, "date style keeps Android JSON keys")

        var film = project
        film.cardOverrides["4"] = CardOverride(texts: ["title": "Last title", "caption": "Last caption"])
        film.selectStyle(.filmVertical)
        check(film.placements[1] == nil && film.placements[2]?.assetID == photo.id, "switching layouts preserves holes")
        check(film.cardOverrides["0"]?.texts["title"] == "" && film.cardOverrides["0"]?.texts["caption"] == "Last caption", "first override per role wins when cards merge")
        film.selectStyle(.polaroid)
        check(film.cardOverrides["0"]?.texts["caption"] == "Last caption", "film override maps to first photo")

        for choice in FontCatalog.bundledChoices {
            check(FontCatalog.font(choice.id, size: 12) != nil, "bundled font loads: \(choice.id)")
        }
        project.settings.setTextStyle(.title, TextAppearance(fontName: "Caveat"))

        project.settings.dateSource = .none
        let pdfURL = directory.appendingPathComponent("cards.pdf")
        try PolarRenderer.writePDF(project: project, to: pdfURL)
        let pdfText = PDFDocument(url: pdfURL)?.string ?? ""
        check(pdfText.contains("La segunda foto") && pdfText.contains("12 oct"), "PDF renders per-card text and date")
        check(pdfText.components(separatedBy: "Nuestros momentos").count == 2, "empty middle card omitted in export")
        let titleSelection = PDFDocument(url: pdfURL)?.findString("Nuestros momentos", withOptions: []).first
        let titleFont = titleSelection?.attributedString?.attribute(.font, at: 0, effectiveRange: nil) as? NSFont
        check(titleFont?.familyName?.contains("Caveat") == true, "Android font ID renders with bundled font in vector PDF")
        check(FontCatalog.font("Avenir Next", size: 12)?.familyName?.contains("Josefin") == true, "legacy font alias resolves to the Android font")

        project.cardOverrides = [:]
        project.settings.dateSource = .none
        project.settings.title = "Peso original"
        project.settings.setTextStyle(.title, TextAppearance(fontName: "Helvetica", hex: "00AA00"))
        try PolarRenderer.writePDF(project: project, to: pdfURL)
        let recoloredFont = PDFDocument(url: pdfURL)?.findString("Peso original", withOptions: []).first?.attributedString?.attribute(.font, at: 0, effectiveRange: nil) as? NSFont
        check(recoloredFont?.fontDescriptor.symbolicTraits.contains(.bold) == true, "editing font and color preserves the design's default bold text")
        project.settings.columns = 1; project.settings.rows = 1
        project.placements = [PhotoPlacement(assetID: photo.id)]
        project.settings.dateSource = .chosen
        project.settings.chosenDate = Date(timeIntervalSince1970: 1_771_070_400)
        project.settings.extraTextStyles = [:]
        for style in TemplateStyle.allCases where style != .imported {
            project.settings.style = style
            try PolarRenderer.writePDF(project: project, to: pdfURL)
            let text = PDFDocument(url: pdfURL)?.string ?? ""
            check(text.contains("14 feb 2026") == style.supportsDate, "date appears only in supported layout: \(style)")
        }

        project.settings.style = .polaroid
        project.settings.dateSource = .none
        project.settings.title = "💛 🌈 ❤️ 👨‍👩‍👧"
        project.settings.subtitle = ""
        project.settings.textStyles = [:]
        try PolarRenderer.writePDF(project: project, to: pdfURL)
        let pngURL = directory.appendingPathComponent("emoji.png")
        try PolarRenderer.writePNG(project: project, page: 0, to: pngURL)
        let png = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(pngURL as CFURL, nil)!, 0, nil)!
        let card = PolarRenderer.cardRects(settings: project.settings)[0]
        let titleRect = CGRect(x: card.minX + card.width * 0.07, y: card.minY + card.height * 0.82,
                               width: card.width * 0.86, height: card.height * 0.075)
        let pngColor = coloredPixels(png, in: titleRect), pdfColor = coloredPixels(rasterPDF(pdfURL), in: titleRect)
        check(pngColor > 500 && pdfColor > pngColor / 2, "native color emoji survive PNG and PDF without custom rasterization (PNG \(pngColor), PDF \(pdfColor))")
        print("Parity checks passed: legacy and Android JSON, override validation/remap, dates/EXIF, 20 fonts, per-card PDF, empty cards, date layouts and native emoji")
    }

    static func fixture(to url: URL) throws {
        let context = CGContext(data: nil, width: 80, height: 120, bitsPerComponent: 8, bytesPerRow: 0,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
        context.setFillColor(NSColor.gray.cgColor); context.fill(CGRect(x: 0, y: 0, width: 80, height: 120))
        let destination = CGImageDestinationCreateWithURL(url as CFURL, "public.jpeg" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, context.makeImage()!, [kCGImagePropertyOrientation: 6,
            kCGImagePropertyExifDictionary: [kCGImagePropertyExifDateTimeOriginal: "2026:02:14 12:00:00"]] as CFDictionary)
        check(CGImageDestinationFinalize(destination), "EXIF fixture written")
    }

    static func rasterPDF(_ url: URL) -> CGImage {
        let context = CGContext(data: nil, width: 2550, height: 3300, bitsPerComponent: 8, bytesPerRow: 0,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.setFillColor(NSColor.white.cgColor); context.fill(CGRect(x: 0, y: 0, width: 2550, height: 3300))
        context.scaleBy(x: 300 / 72, y: 300 / 72)
        context.drawPDFPage(CGPDFDocument(url as CFURL)!.page(at: 1)!)
        return context.makeImage()!
    }

    static func coloredPixels(_ image: CGImage, in rect: CGRect) -> Int {
        let pixels = CGRect(x: rect.minX * 300 / 72, y: rect.minY * 300 / 72, width: rect.width * 300 / 72, height: rect.height * 300 / 72).integral
        guard let crop = image.cropping(to: pixels) else { return 0 }
        let context = CGContext(data: nil, width: crop.width, height: crop.height, bitsPerComponent: 8, bytesPerRow: crop.width * 4,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(crop, in: CGRect(x: 0, y: 0, width: crop.width, height: crop.height))
        let bytes = context.data!.assumingMemoryBound(to: UInt8.self)
        return stride(from: 0, to: crop.width * crop.height * 4, by: 4).filter {
            let values = [Int(bytes[$0]), Int(bytes[$0 + 1]), Int(bytes[$0 + 2])]
            return values.max()! - values.min()! > 50
        }.count
    }
}
