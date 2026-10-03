import AppKit
import ImageIO
import PDFKit
import CoreImage

@main
struct RendererChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }

    static func sample(_ image: CGImage, x: Int, y: Int) -> (Int, Int, Int) {
        let context = CGContext(data: nil, width: image.width, height: image.height, bitsPerComponent: 8,
                                bytesPerRow: image.width * 4, space: CGColorSpaceCreateDeviceRGB(),
                                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        let bytes = context.data!.assumingMemoryBound(to: UInt8.self)
        let index = y * image.width * 4 + x * 4
        return (Int(bytes[index]), Int(bytes[index + 1]), Int(bytes[index + 2]))
    }

    static func fixture(at url: URL) throws -> PhotoAsset {
        // EXIF orientation 6 turns a top-red / bottom-blue image clockwise.
        var bytes = [UInt8](repeating: 255, count: 80 * 120 * 4)
        for y in 0..<120 {
            for x in 0..<80 {
                let i = (y * 80 + x) * 4
                bytes[i] = y < 60 ? 255 : 0
                bytes[i + 1] = 0
                bytes[i + 2] = y < 60 ? 0 : 255
            }
        }
        let image = CGImage(width: 80, height: 120, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: 320,
                            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
                            provider: CGDataProvider(data: Data(bytes) as CFData)!, decode: nil, shouldInterpolate: true, intent: .defaultIntent)!
        let destination = CGImageDestinationCreateWithURL(url as CFURL, "public.jpeg" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, [kCGImagePropertyOrientation: 6, kCGImageDestinationLossyCompressionQuality: 1] as CFDictionary)
        check(CGImageDestinationFinalize(destination), "EXIF fixture saved")
        return PhotoAsset(path: url.path, pixelWidth: 120, pixelHeight: 80)
    }

    static func noisyFixture(at url: URL) -> PhotoAsset {
        var bytes = [UInt8](repeating: 255, count: 2000 * 1500 * 4)
        var random: UInt64 = 42
        for index in stride(from: 0, to: bytes.count, by: 4) {
            for channel in 0..<3 {
                random = random &* 6364136223846793005 &+ 1442695040888963407
                bytes[index + channel] = UInt8(truncatingIfNeeded: random >> 32)
            }
        }
        let image = CGImage(width: 2000, height: 1500, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: 8000,
                            space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
                            provider: CGDataProvider(data: Data(bytes) as CFData)!, decode: nil, shouldInterpolate: true, intent: .defaultIntent)!
        let destination = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, nil)
        check(CGImageDestinationFinalize(destination), "large noisy fixture saved")
        return PhotoAsset(path: url.path, pixelWidth: 2000, pixelHeight: 1500)
    }

    static func pdfImages(at url: URL) -> [[String: Any]] {
        let document = CGPDFDocument(url as CFURL)!
        var resources: CGPDFDictionaryRef?, objects: CGPDFDictionaryRef?
        check(CGPDFDictionaryGetDictionary(document.page(at: 1)!.dictionary!, "Resources", &resources), "PDF resources exist")
        check(CGPDFDictionaryGetDictionary(resources!, "XObject", &objects), "PDF image objects exist")
        let records = NSMutableArray()
        CGPDFDictionaryApplyFunction(objects!, { _, object, info in
            var stream: CGPDFStreamRef?
            guard CGPDFObjectGetValue(object, .stream, &stream), let stream else { return }
            guard let dictionary = CGPDFStreamGetDictionary(stream) else { return }
            var subtype: UnsafePointer<CChar>?, filter: UnsafePointer<CChar>?
            guard CGPDFDictionaryGetName(dictionary, "Subtype", &subtype), String(cString: subtype!) == "Image" else { return }
            var width: CGPDFInteger = 0, height: CGPDFInteger = 0
            CGPDFDictionaryGetInteger(dictionary, "Width", &width)
            CGPDFDictionaryGetInteger(dictionary, "Height", &height)
            CGPDFDictionaryGetName(dictionary, "Filter", &filter)
            Unmanaged<NSMutableArray>.fromOpaque(info!).takeUnretainedValue().add([
                "width": width, "height": height, "filter": filter.map { String(cString: $0) } ?? ""])
        }, Unmanaged.passUnretained(records).toOpaque())
        return records as! [[String: Any]]
    }

    static func rasterPDF(at url: URL) -> CGImage {
        let context = CGContext(data: nil, width: 2550, height: 3300, bitsPerComponent: 8, bytesPerRow: 0,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.setFillColor(NSColor.white.cgColor)
        context.fill(CGRect(x: 0, y: 0, width: 2550, height: 3300))
        context.scaleBy(x: 300 / 72, y: 300 / 72)
        context.drawPDFPage(CGPDFDocument(url as CFURL)!.page(at: 1)!)
        return context.makeImage()!
    }

    static func greenPixels(_ image: CGImage, in points: CGRect) -> Int {
        let pixels = CGRect(x: points.minX * 300 / 72, y: points.minY * 300 / 72, width: points.width * 300 / 72, height: points.height * 300 / 72).integral
        guard let cropped = image.cropping(to: pixels) else { return 0 }
        let context = CGContext(data: nil, width: cropped.width, height: cropped.height, bitsPerComponent: 8,
            bytesPerRow: cropped.width * 4, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(cropped, in: CGRect(x: 0, y: 0, width: cropped.width, height: cropped.height))
        let bytes = context.data!.assumingMemoryBound(to: UInt8.self)
        return stride(from: 0, to: cropped.width * cropped.height * 4, by: 4).filter {
            Int(bytes[$0 + 1]) > Int(bytes[$0]) + 50 && Int(bytes[$0 + 1]) > Int(bytes[$0 + 2]) + 50
        }.count
    }

    static func main() throws {
        let directory = URL(fileURLWithPath: "/private/tmp/polar-renderer-checks-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let paper = CGRect(x: 0, y: 0, width: 612, height: 792)
        let oriented = try fixture(at: directory.appendingPathComponent("oriented.jpg"))
        var a4Project = PolarProject()
        a4Project.settings.paperSize = .a4
        a4Project.settings.orientation = .landscape
        let a4URL = directory.appendingPathComponent("a4.pdf")
        try PolarRenderer.writePDF(project: a4Project, to: a4URL)
        let a4Box = PDFDocument(url: a4URL)!.page(at: 0)!.bounds(for: .mediaBox)
        check(abs(a4Box.width - 297 * 72 / 25.4) < 0.01 && abs(a4Box.height - 210 * 72 / 25.4) < 0.01,
              "PDF respects A4 landscape paper")
        for size in PaperSize.allCases { for orientation in PaperOrientation.allCases {
            a4Project.settings.paperSize = size; a4Project.settings.orientation = orientation
            try PolarRenderer.writePDF(project: a4Project, to: a4URL)
            let box = PDFDocument(url: a4URL)!.page(at: 0)!.bounds(for: .mediaBox)
            check(abs(box.width - a4Project.settings.paperSizePoints.width) < 0.01 && abs(box.height - a4Project.settings.paperSizePoints.height) < 0.01,
                  "\(size)/\(orientation): exported paper matches chosen size")
        } }
        for style in TemplateStyle.allCases {
            var settings = PrintSettings()
            settings.style = style
            (settings.columns, settings.rows) = style.grid
            if style == .imported {
                settings.importedTemplate = ImportedTemplate(path: oriented.path, pixelWidth: 120, pixelHeight: 80,
                    regions: [TemplateRegion(x: 0.1, y: 0.1, width: 0.35, height: 0.8), TemplateRegion(x: 0.55, y: 0.1, width: 0.35, height: 0.8)])
            }
            let cards = PolarRenderer.cardRects(settings: settings)
            check(cards.count == settings.capacity / style.photosPerCard, "\(style): all cards exist")
            for card in cards {
                check(paper.contains(card) && card.width > 0 && card.height > 0, "\(style): card stays on letter paper")
                let photos = PolarRenderer.photoRects(in: card, style: style, settings: settings)
                check(photos.count == style.photosPerCard, "\(style): correct photo slot count")
                check(photos.allSatisfy { card.contains($0) && $0.width > 0 && $0.height > 0 }, "\(style): photos stay inside cards")
            }
        }
        var project = PolarProject()
        project.photos = [oriented]
        project.placements = Array(repeating: nil, count: 10)
        project.placements[0] = PhotoPlacement(assetID: oriented.id)
        project.placements[9] = PhotoPlacement(assetID: oriented.id)
        let pdfURL = directory.appendingPathComponent("two-pages.pdf")
        try PolarRenderer.writePDF(project: project, to: pdfURL)
        let pdf = PDFDocument(url: pdfURL)
        check(pdf?.pageCount == 2, "PDF keeps every page")
        for index in 0..<2 {
            check(pdf?.page(at: index)?.bounds(for: .mediaBox) == paper, "PDF has exact letter media box")
        }
        check(pdf?.string?.contains("Nuestros momentos") == true, "PDF text remains selectable and upright")
        let pngURL = directory.appendingPathComponent("page.png")
        try PolarRenderer.writePNG(project: project, page: 0, to: pngURL)
        let source = CGImageSourceCreateWithURL(pngURL as CFURL, nil)!
        let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil)! as NSDictionary
        check(properties[kCGImagePropertyPixelWidth] as? Int == 2550, "PNG is 2550 pixels wide")
        check(properties[kCGImagePropertyPixelHeight] as? Int == 3300, "PNG is 3300 pixels tall")
        check(abs((properties[kCGImagePropertyDPIWidth] as? Double ?? 0) - 300) < 0.1, "PNG records 300 dpi")
        check(PolarRenderer.preview(project: project, page: 0).size == NSSize(width: 612, height: 792), "preview uses letter point size")
        var single = PolarProject(); single.photos = [oriented]; single.placements = [PhotoPlacement(assetID: oriented.id)]
        let singleURL = directory.appendingPathComponent("one-of-nine.pdf")
        try PolarRenderer.writePDF(project: single, to: singleURL)
        let singleText = PDFDocument(url: singleURL)!.string ?? ""
        check(singleText.components(separatedBy: "Nuestros momentos").count - 1 == 1, "eight empty cards emit no repeated text")
        let singleRaster = rasterPDF(at: singleURL)
        let cards = PolarRenderer.cardRects(settings: single.settings)
        let emptyFrame = sample(singleRaster, x: Int(cards[1].minX * 300 / 72), y: Int(cards[1].midY * 300 / 72))
        let emptyGuide = sample(singleRaster, x: Int(cards[1].minX * 300 / 72), y: Int((cards[1].minY - 3) * 300 / 72))
        let filledFrame = sample(singleRaster, x: Int(cards[0].minX * 300 / 72), y: Int(cards[0].midY * 300 / 72))
        check(emptyFrame.0 > 250 && emptyFrame.1 > 250 && emptyFrame.2 > 250 && emptyGuide.0 > 250, "empty cards emit no frame or cut guide")
        check(filledFrame.0 > 250 && filledFrame.1 > 250 && filledFrame.2 > 250, "gray outline stays out of print by default")
        let blank = PolarProject(); try PolarRenderer.writePDF(project: blank, to: singleURL)
        check((PDFDocument(url: singleURL)!.string ?? "").isEmpty, "fully empty sheet contains no user text")
        for columns in [1, 2, 4] { for format in CardFormat.allCases {
            var settings = PrintSettings(); settings.columns = columns; settings.rows = 2; settings.cardFormat = format
            let grid = PolarRenderer.cardRects(settings: settings)
            check(grid.count == columns * 2 && grid.allSatisfy { paper.contains($0) }, "\(columns) columns / \(format) stays on paper")
            if let aspect = settings.cardAspect { check(grid.allSatisfy { abs($0.width / $0.height - aspect) < 0.001 }, "card format controls aspect") }
        } }
        single.settings.paperSize = .photo4x6; single.settings.orientation = .landscape
        let dynamicPNG = directory.appendingPathComponent("photo-paper.png")
        try PolarRenderer.writePNG(project: single, page: 0, to: dynamicPNG)
        let dynamicProperties = CGImageSourceCopyPropertiesAtIndex(CGImageSourceCreateWithURL(dynamicPNG as CFURL, nil)!, 0, nil)! as NSDictionary
        check(dynamicProperties[kCGImagePropertyPixelWidth] as? Int == 1800 && dynamicProperties[kCGImagePropertyPixelHeight] as? Int == 1200,
              "landscape 4×6 PNG has dynamic 300 dpi pixels")
        single.settings.paperSize = .letter; single.settings.orientation = .portrait
        single.settings.title = "Tipografía elegida"
        single.settings.textStyles["title"] = TextAppearance(fontName: "Helvetica", size: 14, hex: "00AA00", bold: true,
            italic: true, alignment: .left, offsetX: 0, offsetY: 0, visible: true)
        let typographyURL = directory.appendingPathComponent("typography.pdf")
        try PolarRenderer.writePDF(project: single, to: typographyURL)
        let typographyPDF = PDFDocument(url: typographyURL)!
        let selection = typographyPDF.findString("Tipografía elegida", withOptions: []).first!
        let selectedFont = selection.attributedString?.attribute(.font, at: 0, effectiveRange: nil) as? NSFont
        check(selectedFont?.familyName?.contains("Helvetica") == true, "chosen installed font survives vector PDF")
        let originalTextBounds = selection.bounds(for: typographyPDF.page(at: 0)!)
        let titleArea = CGRect(x: cards[0].minX, y: cards[0].minY + cards[0].height * 0.79, width: cards[0].width, height: cards[0].height * 0.15)
        check(greenPixels(rasterPDF(at: typographyURL), in: titleArea) > 30, "title color is applied in print")
        single.settings.textStyles["title"]!.offsetX = 3; single.settings.textStyles["title"]!.offsetY = 2
        try PolarRenderer.writePDF(project: single, to: typographyURL)
        let movedPDF = PDFDocument(url: typographyURL)!, moved = movedPDF.findString("Tipografía elegida", withOptions: []).first!
        let movedBounds = moved.bounds(for: movedPDF.page(at: 0)!)
        check(abs(movedBounds.minX - originalTextBounds.minX - 3) < 0.5 && abs(movedBounds.minY - originalTextBounds.minY + 2) < 0.5,
              "text offsets use actual print points")
        single.settings.textStyles["title"]!.offsetY = -60
        try PolarRenderer.writePNG(project: single, page: 0, to: dynamicPNG)
        let clipped = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(dynamicPNG as CFURL, nil)!, 0, nil)!
        check(greenPixels(clipped, in: PolarRenderer.photoRects(in: cards[0], style: .polaroid, settings: single.settings)[0]) == 0,
              "moving text never paints over photo")
        single.settings.textStyles["title"]!.visible = false
        try PolarRenderer.writePDF(project: single, to: typographyURL)
        check(!(PDFDocument(url: typographyURL)!.string ?? "").contains("Tipografía elegida"), "hidden user text is omitted")
        var wrapped = PolarProject(); wrapped.photos = [oriented]
        wrapped.placements = [PhotoPlacement(assetID: oriented.id)]
        wrapped.settings.title = "Sofía & Mateo"
        wrapped.settings.subtitle = "Siempre contigo"
        wrapped.settings.caption = "Los pequeños momentos hacen grandes historias."
        wrapped.selectStyle(.postcard)
        try PolarRenderer.writePDF(project: wrapped, to: typographyURL)
        check(PDFDocument(url: typographyURL)!.findString("Mateo", withOptions: []).count == 1,
              "postcard wraps the complete title into its narrow text column")
        wrapped.selectStyle(.celebration)
        try PolarRenderer.writePDF(project: wrapped, to: typographyURL)
        check(PDFDocument(url: typographyURL)!.findString("historias.", withOptions: []).count == 1,
              "celebration wraps the complete caption without clipping its last word")
        var touching = PolarProject(); touching.selectStyle(.borderless)
        touching.settings.columns = 2; touching.settings.rows = 1; touching.settings.gap = 0
        touching.photos = [oriented]; touching.placements = Array(repeating: PhotoPlacement(assetID: oriented.id), count: 2)
        let touchingCards = PolarRenderer.cardRects(settings: touching.settings)
        touching.settings.cutGuides = false
        try PolarRenderer.writePNG(project: touching, page: 0, to: dynamicPNG)
        let cleanPhoto = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(dynamicPNG as CFURL, nil)!, 0, nil)!
        let cornerX = Int((touchingCards[0].maxX - 3) * 300 / 72), cornerY = Int(ceil(touchingCards[0].minY * 300 / 72))
        let cleanCorner = sample(cleanPhoto, x: cornerX, y: cornerY)
        touching.settings.cutGuides = true
        try PolarRenderer.writePNG(project: touching, page: 0, to: dynamicPNG)
        let guidedPhoto = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(dynamicPNG as CFURL, nil)!, 0, nil)!
        let guidedCorner = sample(guidedPhoto, x: cornerX, y: cornerY)
        check(cleanCorner == guidedCorner, "external corner marks never paint over touching neighbour photos")
        var photoProject = PolarProject()
        photoProject.photos = [oriented]
        photoProject.placements = [PhotoPlacement(assetID: oriented.id)]
        let orientedURL = directory.appendingPathComponent("oriented.png")
        try PolarRenderer.writePNG(project: photoProject, page: 0, to: orientedURL)
        let orientedImage = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(orientedURL as CFURL, nil)!, 0, nil)!
        let firstCard = PolarRenderer.cardRects(settings: photoProject.settings)[0]
        let firstPhoto = PolarRenderer.photoRects(in: firstCard, style: .polaroid, settings: photoProject.settings)[0]
        let left = sample(orientedImage, x: Int((firstPhoto.minX + firstPhoto.width * 0.2) * 300 / 72), y: Int(firstPhoto.midY * 300 / 72))
        let right = sample(orientedImage, x: Int((firstPhoto.minX + firstPhoto.width * 0.8) * 300 / 72), y: Int(firstPhoto.midY * 300 / 72))
        check(left.2 > 220 && right.0 > 220, "EXIF clockwise orientation is preserved")
        photoProject.placements[0]?.quarterTurns = 1
        try PolarRenderer.writePNG(project: photoProject, page: 0, to: orientedURL)
        let rotatedImage = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(orientedURL as CFURL, nil)!, 0, nil)!
        let top = sample(rotatedImage, x: Int(firstPhoto.midX * 300 / 72), y: Int((firstPhoto.minY + firstPhoto.height * 0.2) * 300 / 72))
        let bottom = sample(rotatedImage, x: Int(firstPhoto.midX * 300 / 72), y: Int((firstPhoto.minY + firstPhoto.height * 0.8) * 300 / 72))
        check(top.2 > 220 && bottom.0 > 220, "quarter-turn rotates clockwise without upside-down image draws")
        photoProject.selectStyle(.spotify)
        photoProject.settings.songURL = "https://example.com/our-song"
        let qrURL = directory.appendingPathComponent("qr.png")
        try PolarRenderer.writePNG(project: photoProject, page: 0, to: qrURL)
        let qrImage = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(qrURL as CFURL, nil)!, 0, nil)!
        let detector = CIDetector(ofType: CIDetectorTypeQRCode, context: CIContext(), options: [CIDetectorAccuracy: CIDetectorAccuracyHigh])!
        let links = detector.features(in: CIImage(cgImage: qrImage)).compactMap { ($0 as? CIQRCodeFeature)?.messageString }
        check(links.contains("https://example.com/our-song"), "song QR encodes the real supplied URL")
        let qrJPG = directory.appendingPathComponent("qr.jpg")
        try PolarRenderer.writeJPEG(project: photoProject, page: 0, to: qrJPG)
        let jpgImage = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(qrJPG as CFURL, nil)!, 0, nil)!
        check(detector.features(in: CIImage(cgImage: jpgImage)).contains { ($0 as? CIQRCodeFeature)?.messageString == "https://example.com/our-song" },
              "JPG retains a scannable song QR")
        var calendarProject = PolarProject()
        calendarProject.photos = [oriented]
        calendarProject.selectStyle(.calendar)
        calendarProject.settings.columns = 1
        calendarProject.settings.rows = 1
        calendarProject.settings.calendarYear = 2024
        calendarProject.placements = Array(repeating: PhotoPlacement(assetID: oriented.id), count: 2)
        let calendarURL = directory.appendingPathComponent("calendar.pdf")
        try PolarRenderer.writePDF(project: calendarProject, to: calendarURL)
        let february = PDFDocument(url: calendarURL)!.page(at: 1)!.string!
        check(february.contains("FEB") && february.range(of: "(?:^|\\s)29(?:\\s|$)", options: .regularExpression) != nil,
              "second calendar card is real leap-year February")
        calendarProject.settings.calendarYear = 2023
        try PolarRenderer.writePDF(project: calendarProject, to: calendarURL)
        let regularFebruary = PDFDocument(url: calendarURL)!.page(at: 1)!.string!
        check(regularFebruary.range(of: "(?:^|\\s)29(?:\\s|$)", options: .regularExpression) == nil,
              "non-leap February omits day 29")
        calendarProject.settings.columns = 3
        calendarProject.settings.rows = 4
        calendarProject.placements = Array(repeating: PhotoPlacement(assetID: oriented.id), count: 12)
        try PolarRenderer.writePDF(project: calendarProject, to: calendarURL)
        check(PDFDocument(url: calendarURL)!.string!.contains("MAR"), "default calendar keeps the whole March label")
        let originalPhotoData = try Data(contentsOf: pngURL)
        let originalPhoto = PhotoAsset(path: pngURL.path, pixelWidth: 2550, pixelHeight: 3300)
        var protectedProject = PolarProject()
        protectedProject.photos = [originalPhoto]
        protectedProject.placements = [PhotoPlacement(assetID: originalPhoto.id)]
        var originalProtected = false
        do { try PolarRenderer.writePNG(project: protectedProject, page: 0, to: pngURL) }
        catch { originalProtected = true }
        check(originalProtected, "export cannot replace an imported original photo")
        let afterProtectedExport = try Data(contentsOf: pngURL)
        check(afterProtectedExport == originalPhotoData, "original photo bytes remain intact")
        let previous = Data("keep existing export".utf8)
        try previous.write(to: pngURL)
        let missing = PhotoAsset(path: directory.appendingPathComponent("missing.jpg").path, pixelWidth: 100, pixelHeight: 100)
        project.photos = [missing]
        project.placements[0] = PhotoPlacement(assetID: missing.id)
        project.placements[9] = nil
        do {
            try PolarRenderer.writePNG(project: project, page: 0, to: pngURL)
            check(false, "assigned missing photo must fail export")
        } catch PolarError.unreadablePhoto { }
        let preserved = try Data(contentsOf: pngURL)
        check(preserved == previous, "failed export preserves existing destination")
        do {
            try PolarRenderer.writePDF(project: project, to: pdfURL)
            check(false, "PDF rejects assigned missing photo")
        } catch PolarError.unreadablePhoto { }
        var currentPageProject = PolarProject()
        currentPageProject.photos = [missing]
        currentPageProject.placements = Array(repeating: nil, count: 10)
        currentPageProject.placements[9] = PhotoPlacement(assetID: missing.id)
        try PolarRenderer.writePNG(project: currentPageProject, page: 0, to: pngURL)
        check(CGImageSourceCreateWithURL(pngURL as CFURL, nil) != nil, "PNG current page succeeds when only another page has a missing photo")
        let noisy = noisyFixture(at: directory.appendingPathComponent("large-noisy.png"))
        var noisyProject = PolarProject()
        noisyProject.photos = [noisy]
        noisyProject.placements = [PhotoPlacement(assetID: noisy.id)]
        let compactURL = directory.appendingPathComponent("compact.pdf")
        try PolarRenderer.writePDF(project: noisyProject, to: compactURL)
        let compactData = try Data(contentsOf: compactURL)
        check(compactData.count < 1_000_000, "one 300 dpi photo in a nine-card PDF stays under 1 MB (got \(compactData.count) bytes)")
        check(PDFDocument(url: compactURL)!.string!.contains("Nuestros momentos"), "compact PDF retains vector selectable text")
        let embedded = pdfImages(at: compactURL)
        let visible = PolarRenderer.photoRects(in: PolarRenderer.cardRects(settings: noisyProject.settings)[0], style: .polaroid, settings: noisyProject.settings)[0]
        let density: CGFloat = 300 / 72
        let visibleWidth = Int(ceil(visible.width * density)), visibleHeight = Int(ceil(visible.height * density))
        check(embedded.contains { ($0["filter"] as? String) == "DCTDecode" && $0["width"] as? Int == visibleWidth
            && $0["height"] as? Int == visibleHeight }, "PDF only embeds the visible 300 dpi photo, not unused cropped pixels")
        let losslessURL = directory.appendingPathComponent("lossless.pdf")
        try PolarRenderer.writePDF(project: noisyProject, to: losslessURL, optimizePhotos: false)
        check(!pdfImages(at: losslessURL).contains { $0["filter"] as? String == "DCTDecode" }, "lossless PDF does not introduce JPEG encoding")
        check(PDFDocument(url: losslessURL)!.string!.contains("Nuestros momentos"), "lossless PDF retains vector text")
        let jpegURL = directory.appendingPathComponent("compact.jpg"), losslessPNG = directory.appendingPathComponent("lossless.png")
        try PolarRenderer.writeJPEG(project: noisyProject, page: 0, to: jpegURL)
        try PolarRenderer.writePNG(project: noisyProject, page: 0, to: losslessPNG)
        let jpegProperties = CGImageSourceCopyPropertiesAtIndex(CGImageSourceCreateWithURL(jpegURL as CFURL, nil)!, 0, nil)! as NSDictionary
        check(jpegProperties[kCGImagePropertyPixelWidth] as? Int == 2550 && jpegProperties[kCGImagePropertyPixelHeight] as? Int == 3300,
              "letter JPG retains full 300 dpi sheet dimensions")
        check(jpegProperties[kCGImagePropertyDPIWidth] as? Int == 300 && jpegProperties[kCGImagePropertyDPIHeight] as? Int == 300,
              "JPG includes physical 300 dpi metadata")
        let jpegBytes = try Data(contentsOf: jpegURL).count, pngBytes = try Data(contentsOf: losslessPNG).count
        check(jpegBytes < pngBytes, "JPG is lighter than PNG for noisy photos")
        noisyProject.placements[0]?.zoom = 4
        try PolarRenderer.writePDF(project: noisyProject, to: compactURL)
        check(pdfImages(at: compactURL).contains { $0["width"] as? Int == visibleWidth && $0["height"] as? Int == visibleHeight }, "zoom retains 300 dpi visible coverage without embedding the entire source")
        var alphaBytes = [UInt8](repeating: 0, count: 80 * 120 * 4)
        for y in 0..<120 { for x in 40..<80 {
            alphaBytes[(y * 80 + x) * 4] = 255
            alphaBytes[(y * 80 + x) * 4 + 3] = 255
        } }
        let alphaImage = CGImage(width: 80, height: 120, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: 320,
                                 space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
                                 provider: CGDataProvider(data: Data(alphaBytes) as CFData)!, decode: nil, shouldInterpolate: true, intent: .defaultIntent)!
        let alphaURL = directory.appendingPathComponent("transparent.png")
        let alphaDestination = CGImageDestinationCreateWithURL(alphaURL as CFURL, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(alphaDestination, alphaImage, nil)
        check(CGImageDestinationFinalize(alphaDestination), "transparent fixture saved")
        let alphaPhoto = PhotoAsset(path: alphaURL.path, pixelWidth: 80, pixelHeight: 120)
        var alphaProject = PolarProject()
        alphaProject.selectStyle(.playerRed)
        alphaProject.photos = [alphaPhoto]
        alphaProject.placements = [PhotoPlacement(assetID: alphaPhoto.id)]
        let alphaPDFURL = directory.appendingPathComponent("transparent.pdf")
        try PolarRenderer.writePDF(project: alphaProject, to: alphaPDFURL)
        let alphaPDF = rasterPDF(at: alphaPDFURL)
        let alphaCard = PolarRenderer.cardRects(settings: alphaProject.settings)[0]
        let alphaRect = PolarRenderer.photoRects(in: alphaCard, style: .playerRed, settings: alphaProject.settings)[0]
        let translucent = sample(alphaPDF, x: Int((alphaRect.minX + alphaRect.width * 0.25) * 300 / 72), y: Int(alphaRect.midY * 300 / 72))
        let cardColor = sample(alphaPDF, x: Int((alphaRect.minX - 3) * 300 / 72), y: Int(alphaRect.midY * 300 / 72))
        check(abs(translucent.0 - cardColor.0) < 12 && abs(translucent.1 - cardColor.1) < 12 && abs(translucent.2 - cardColor.2) < 12,
              "PDF composites transparent PNG onto the actual card color")
        var importedProject = PolarProject(); importedProject.selectStyle(.imported)
        importedProject.settings.importedTemplate = ImportedTemplate(path: alphaURL.path, pixelWidth: 80, pixelHeight: 120,
            regions: [TemplateRegion(x: 0, y: 0, width: 0.5, height: 1, isTransparent: true), TemplateRegion(x: 0.5, y: 0, width: 0.5, height: 1)])
        importedProject.photos = [oriented]
        importedProject.placements = Array(repeating: PhotoPlacement(assetID: oriented.id), count: 2)
        let importedPNG = directory.appendingPathComponent("imported.png")
        try PolarRenderer.writePNG(project: importedProject, page: 0, to: importedPNG)
        let importedImage = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(importedPNG as CFURL, nil)!, 0, nil)!
        let importedCards = PolarRenderer.cardRects(settings: importedProject.settings)
        for card in importedCards {
            let pixel = sample(importedImage, x: Int((card.minX + card.width * 0.2) * 300 / 72), y: Int(card.midY * 300 / 72))
            check(pixel.2 > 220 && pixel.0 < 20, "mixed alpha and opaque template regions both keep assigned photos")
        }
        let originalTemplateData = try Data(contentsOf: alphaURL)
        var templateProtected = false
        do { try PolarRenderer.writePNG(project: importedProject, page: 0, to: alphaURL) } catch { templateProtected = true }
        check(templateProtected, "export cannot replace active imported template")
        let preservedTemplateData = try Data(contentsOf: alphaURL)
        check(preservedTemplateData == originalTemplateData, "imported template original is unchanged")
        print("Noisy fixture PDF: \(compactData.count) bytes; JPG: \(jpegBytes), PNG: \(pngBytes).")
        print("Renderer checks passed: 20 layouts, 8 papers × 2 orientations, empty cards omitted, typography, mixed template, PDF/PNG300, EXIF, QR, calendar, original protection and atomic output.")
    }
}
