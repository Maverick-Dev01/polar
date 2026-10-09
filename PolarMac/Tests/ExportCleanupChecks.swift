import AppKit
import ImageIO
import CoreImage

@main @MainActor struct ExportCleanupChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }

    static func noisyPhoto(at url: URL) -> PhotoAsset {
        let w = 3000, h = 2000
        var bytes = [UInt8](repeating: 255, count: w * h * 4)
        var random: UInt64 = 7
        for index in stride(from: 0, to: bytes.count, by: 4) {
            for channel in 0..<3 {
                random = random &* 6364136223846793005 &+ 1442695040888963407
                bytes[index + channel] = UInt8(truncatingIfNeeded: random >> 32)
            }
        }
        let image = CGImage(width: w, height: h, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: w * 4,
                            space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
                            provider: CGDataProvider(data: Data(bytes) as CFData)!, decode: nil, shouldInterpolate: true, intent: .defaultIntent)!
        let destination = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, nil)
        check(CGImageDestinationFinalize(destination), "noisy fixture saved")
        return PhotoAsset(path: url.path, pixelWidth: w, pixelHeight: h)
    }

    static func main() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("polar-export-cleanup-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        // Task 3: resolution tiers.
        check(PhotoQuality.of(dpi: 149.9) == .low, "149.9 ppp is low")
        check(PhotoQuality.of(dpi: 150) == .fair, "150 ppp is fair")
        check(PhotoQuality.of(dpi: 219.9) == .fair, "219.9 ppp is fair")
        check(PhotoQuality.of(dpi: 220) == .good, "220 ppp is good")
        check(PhotoQuality.low.badge == "Baja" && PhotoQuality.fair.badge == "Aceptable" && PhotoQuality.good.badge == nil, "badges carry text")

        // Task 2: high-quality interpolation on every intermediate layer.
        check(PolarRenderer.bitmap(width: 4, height: 4)?.interpolationQuality == .high, "photo/filter/background layers use high interpolation")
        check(PolarRenderer.opaqueSRGBContext(width: 4, height: 4)?.interpolationQuality == .high, "PDF JPEG context uses high interpolation")

        // Task 4: quality presets.
        check(ExportQuality.light.dpi == 200 && ExportQuality.high.dpi == 300 && ExportQuality.max.dpi == 300, "preset resolutions")
        check(ExportQuality.light.jpegQuality == 0.85 && ExportQuality.high.jpegQuality == 0.94 && ExportQuality.max.jpegQuality == nil, "preset JPEG quality")
        let noisy = noisyPhoto(at: directory.appendingPathComponent("noisy.png"))
        var project = PolarProject()
        project.photos = [noisy]
        project.placements = [PhotoPlacement(assetID: noisy.id)]
        var sizes: [ExportQuality: Int] = [:]
        for quality in ExportQuality.allCases {
            let url = directory.appendingPathComponent("q-\(quality.rawValue).pdf")
            try PolarRenderer.writePDF(project: project, to: url, quality: quality)
            sizes[quality] = try Data(contentsOf: url).count
        }
        print("PDF sizes (1 noisy 3000x2000 photo, Letter, 9 cards): Ligero \(sizes[.light]!) B, Alta \(sizes[.high]!) B, Máxima \(sizes[.max]!) B")
        check(sizes[.light]! < sizes[.high]! && sizes[.high]! < sizes[.max]!, "Ligero < Alta < Máxima in PDF size")
        var jpgSizes: [ExportQuality: Int] = [:]
        for quality in ExportQuality.allCases {
            let url = directory.appendingPathComponent("q-\(quality.rawValue).jpg")
            try PolarRenderer.writeJPEG(project: project, page: 0, to: url, quality: quality)
            jpgSizes[quality] = try Data(contentsOf: url).count
            let props = CGImageSourceCopyPropertiesAtIndex(CGImageSourceCreateWithURL(url as CFURL, nil)!, 0, nil)! as NSDictionary
            let dpi = Int(quality.dpi)
            check(props[kCGImagePropertyDPIWidth] as? Int == dpi, "\(quality.title) JPG declares \(dpi) dpi")
            check(props[kCGImagePropertyPixelWidth] as? Int == Int((612 * quality.dpi / 72).rounded()), "\(quality.title) JPG pixel width matches density")
        }
        check(jpgSizes[.light]! < jpgSizes[.high]! && jpgSizes[.high]! < jpgSizes[.max]!, "JPG sizes grow Ligero < Alta < Máxima")
        let pngLight = directory.appendingPathComponent("light.png")
        try PolarRenderer.writePNG(project: project, page: 0, to: pngLight, quality: .light)
        let pngProps = CGImageSourceCopyPropertiesAtIndex(CGImageSourceCreateWithURL(pngLight as CFURL, nil)!, 0, nil)! as NSDictionary
        check(pngProps[kCGImagePropertyPixelWidth] as? Int == 2550, "PNG stays at 300 ppp even in Ligero")
        print("JPG sizes: Ligero \(jpgSizes[.light]!) B, Alta \(jpgSizes[.high]!) B, Máxima \(jpgSizes[.max]!) B")

        // Preference persists, and older settings.json files without the key still load.
        let root = directory.appendingPathComponent("library")
        let library = LibraryStore(root: root)
        check(library.preferences().exportQuality == .high, "default quality is Alta")
        var preferences = AppPreferences(); preferences.exportQuality = .light; preferences.theme = .dark
        try library.savePreferences(preferences)
        check(LibraryStore(root: root).preferences().exportQuality == .light, "export quality persists")
        try Data(#"{"theme":"dark","units":"inches","defaultPaper":"a4","onboardingSeen":true}"#.utf8).write(to: root.appendingPathComponent("settings.json"))
        let legacy = library.preferences()
        check(legacy.theme == .dark && legacy.units == .inches && legacy.exportQuality == .high, "legacy settings keep values and default quality")

        // Task 5: QR states.
        check(PolarRenderer.qrState("") == .empty, "empty link has no QR")
        check(PolarRenderer.qrState("https://example.com/" + String(repeating: "a", count: 3000)) == .tooLong, "3000-character link is too long")
        check(PolarRenderer.qrState("https://example.com/song") == .ok, "normal link is valid")
        var qrProject = PolarProject()
        qrProject.photos = [noisy]
        qrProject.placements = [PhotoPlacement(assetID: noisy.id)]
        qrProject.selectStyle(.spotify)
        qrProject.settings.songURL = "https://example.com/" + String(repeating: "a", count: 3000)
        let longURL = directory.appendingPathComponent("long.pdf"), longPNG = directory.appendingPathComponent("long.png")
        try PolarRenderer.writePDF(project: qrProject, to: longURL)
        try PolarRenderer.writePNG(project: qrProject, page: 0, to: longPNG)
        check(FileManager.default.fileExists(atPath: longURL.path) && FileManager.default.fileExists(atPath: longPNG.path), "long QR link does not abort export")
        let longImage = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(longPNG as CFURL, nil)!, 0, nil)!
        let detector = CIDetector(ofType: CIDetectorTypeQRCode, context: CIContext(), options: [CIDetectorAccuracy: CIDetectorAccuracyHigh])!
        check(detector.features(in: CIImage(cgImage: longImage)).isEmpty, "too-long link draws a marker, not a QR")
        qrProject.settings.songURL = "https://example.com/our-song"
        let okPNG = directory.appendingPathComponent("ok.png")
        try PolarRenderer.writePNG(project: qrProject, page: 0, to: okPNG)
        let okImage = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(okPNG as CFURL, nil)!, 0, nil)!
        check(detector.features(in: CIImage(cgImage: okImage)).contains { ($0 as? CIQRCodeFeature)?.messageString == "https://example.com/our-song" }, "normal link decodes")

        // Task 6: trash purge and emptying.
        let store = LibraryStore(root: directory.appendingPathComponent("trash-library"))
        var base = PolarProject(); base.name = "Uno"; base.normalized()
        let ids = (0..<4).map { _ in UUID() }
        for id in ids { try store.save(id, project: base) }
        let active = ids[3]
        try store.delete(ids[0]); try store.delete(ids[1]); try store.delete(ids[2])
        check(store.trashCount() == 3, "three designs in the trash")
        let old = Date().addingTimeInterval(-8 * 86_400), recent = Date().addingTimeInterval(-6 * 86_400)
        try FileManager.default.setAttributes([.modificationDate: old], ofItemAtPath: store.root.appendingPathComponent("trash/\(ids[0].uuidString)").path)
        try FileManager.default.setAttributes([.modificationDate: recent], ofItemAtPath: store.root.appendingPathComponent("trash/\(ids[1].uuidString)").path)
        check(store.purgeTrash() == 1 && store.trashCount() == 2, "purge removes only entries older than 7 days")
        check(!FileManager.default.fileExists(atPath: store.root.appendingPathComponent("trash/\(ids[0].uuidString)").path), "old entry is gone")
        check(store.emptyTrash(keeping: ids[2]) == 1, "empty keeps the undoable design")
        check(FileManager.default.fileExists(atPath: store.root.appendingPathComponent("trash/\(ids[2].uuidString)").path), "undoable design survives emptying")
        check((try? store.load(active)) != nil, "active project untouched by purge/empty")
        try store.restore(ids[2])
        check((try? store.load(ids[2])) != nil, "kept design can still be restored")
        try Data().write(to: store.root.appendingPathComponent("trash/.DS_Store"))
        check(store.trashCount() == 0, ".DS_Store is not counted as a trashed design")
        check(store.emptyTrash() == 0 && store.trashCount() == 0, "emptying an empty trash is a no-op")
        check(store.list().count == 2, "active projects survive")

        // Studio opens without auto-loading any personal folder.
        let studio = Studio(storageRoot: directory.appendingPathComponent("studio"))
        check(studio.project.photos.isEmpty, "a new session starts without photos")

        // No leftover development strings in sources.
        let sources = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().appendingPathComponent("Sources")
        for file in try FileManager.default.contentsOfDirectory(at: sources, includingPropertiesForKeys: nil) where file.pathExtension == "swift" {
            let text = try String(contentsOf: file, encoding: .utf8)
            check(!text.contains("Fotitos"), "\(file.lastPathComponent) has no Fotitos reference")
            check(!text.contains("#filePath"), "\(file.lastPathComponent) has no #filePath fallback")
        }
        print("Export & cleanup checks passed: quality tiers, presets, size ordering, persistence, QR states, trash purge/empty, no Fotitos.")
    }
}
