import AppKit
import ImageIO
import UniformTypeIdentifiers

@main
struct ImporterChecks {
    static func main() throws {
        let fm = FileManager.default
        let folder = fm.temporaryDirectory.appendingPathComponent("polar-importer-\(UUID().uuidString)", isDirectory: true)
        try fm.createDirectory(at: folder, withIntermediateDirectories: true)
        defer { try? fm.removeItem(at: folder) }

        // Removing EXIF handling, path normalization or immediate-folder filtering breaks these checks.
        let photo2 = folder.appendingPathComponent("Foto 2.jpg")
        let photo10 = folder.appendingPathComponent("Foto 10.jpg")
        try writeJPEG(to: photo2, orientation: 6)
        try writeJPEG(to: photo10, orientation: 1)
        let original = try Data(contentsOf: photo2)
        try Data("not an image".utf8).write(to: folder.appendingPathComponent("amor.com"))
        try Data("{}".utf8).write(to: folder.appendingPathComponent("settings.json"))
        try Data("notes".utf8).write(to: folder.appendingPathComponent("notes.txt"))
        let nested = folder.appendingPathComponent("nested", isDirectory: true)
        try fm.createDirectory(at: nested, withIntermediateDirectories: true)
        try writeJPEG(to: nested.appendingPathComponent("nested.jpg"), orientation: 1)
        let alias = folder.appendingPathComponent("alias.jpg")
        try fm.createSymbolicLink(at: alias, withDestinationURL: photo2)

        let fromFolder = PhotoImporter.read(urls: [folder])
        precondition(fromFolder.photos.count == 2 && fromFolder.skipped.isEmpty, "Import photos selected through their folder")
        let imported = PhotoImporter.read(urls: [folder, photo2, folder.appendingPathComponent("./Foto 10.jpg")])
        precondition(imported.photos.count == 2, "Import should read two immediate photos and deduplicate canonical paths")
        precondition(imported.skipped.isEmpty, "Non-images inside a folder should be ignored")
        precondition(imported.photos.map(\.name) == ["Foto 2", "Foto 10"], "Import should use natural filename order")
        precondition(imported.photos[0].path == photo2.resolvingSymlinksInPath().standardizedFileURL.path, "Store a canonical absolute path")
        precondition(imported.photos[0].pixelWidth == 300 && imported.photos[0].pixelHeight == 600, "EXIF 6 should swap displayed dimensions")
        precondition(imported.photos[1].pixelWidth == 600 && imported.photos[1].pixelHeight == 300)
        let thumb = PhotoImporter.thumbnail(for: imported.photos[0])
        precondition(thumb != nil, "A valid photo should decode a thumbnail")
        let bitmap = NSBitmapImageRep(cgImage: thumb!.cgImage(forProposedRect: nil, context: nil, hints: nil)!)
        precondition(bitmap.pixelsWide == 128 && bitmap.pixelsHigh == 256, "Thumbnail should apply EXIF rotation and respect 256 pixels")
        let top = bitmap.colorAt(x: 64, y: 16)!.usingColorSpace(.deviceRGB)!
        let bottom = bitmap.colorAt(x: 64, y: 240)!.usingColorSpace(.deviceRGB)!
        precondition(top.redComponent > 0.8 && bottom.blueComponent > 0.8, "EXIF 6 must rotate left red half to the top")

        for orientation in [5, 7, 8] {
            let url = folder.appendingPathComponent("orientation-\(orientation).jpg")
            try writeJPEG(to: url, orientation: orientation)
            let result = PhotoImporter.read(urls: [url])
            precondition(result.photos.first?.pixelWidth == 300 && result.photos.first?.pixelHeight == 600, "EXIF \(orientation) should swap dimensions")
        }

        let broken = folder.appendingPathComponent("broken.jpg")
        try Data("invalid jpg".utf8).write(to: broken)
        let missing = folder.appendingPathComponent("missing.jpg")
        let failure = PhotoImporter.read(urls: [broken, missing])
        precondition(failure.photos.isEmpty && failure.skipped.count == 2, "Selected unreadable files must be reported")
        precondition(PhotoImporter.thumbnail(for: PhotoAsset(path: missing.path, pixelWidth: 10, pixelHeight: 10)) == nil)
        let afterImport = try Data(contentsOf: photo2)
        precondition(afterImport == original, "Import must preserve originals")

        let many = folder.appendingPathComponent("many", isDirectory: true)
        try fm.createDirectory(at: many, withIntermediateDirectories: true)
        for number in 1...2001 {
            try original.write(to: many.appendingPathComponent("photo\(number).jpg"))
        }
        let limited = PhotoImporter.read(urls: [many])
        precondition(limited.photos.count == 2000 && limited.skipped.count == 1, "The photo limit must be explicit, never silent: \(limited.photos.count) photos, \(limited.skipped.count) skipped; \(limited.skipped.prefix(3))")
        print("ImporterChecks: natural order, canonical dedup, immediate folders, EXIF 5–8, oriented thumbnails, unreadable files, unchanged originals and 2000-photo limit passed")
    }

    static func writeJPEG(to url: URL, orientation: Int) throws {
        let width = 600, height = 300
        var pixels = [UInt8](repeating: 255, count: width * height * 4)
        for y in 0..<height {
            for x in 0..<width {
                let index = (y * width + x) * 4
                pixels[index] = x < width / 2 ? 255 : 0
                pixels[index + 1] = 0
                pixels[index + 2] = x < width / 2 ? 0 : 255
            }
        }
        let provider = CGDataProvider(data: Data(pixels) as CFData)!
        let cgImage = CGImage(width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: width * 4,
                              space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.last.rawValue),
                              provider: provider, decode: nil, shouldInterpolate: false, intent: .defaultIntent)!
        let destination = CGImageDestinationCreateWithURL(url as CFURL, UTType.jpeg.identifier as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, cgImage, [kCGImagePropertyOrientation: orientation] as CFDictionary)
        precondition(CGImageDestinationFinalize(destination), "Fixture should encode")
    }
}
