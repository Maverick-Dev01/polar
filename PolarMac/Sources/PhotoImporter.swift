import AppKit
import ImageIO
import UniformTypeIdentifiers

enum PhotoImporter {
    // These aliases also work when LaunchServices has not registered image filename tags yet.
    private static let commonImageExtensions: Set<String> = ["jpg", "jpeg", "jpe", "png", "heic", "heif", "tif", "tiff", "gif", "bmp", "webp", "avif", "ico", "icns"]
    private static let thumbnails: NSCache<NSString, NSImage> = {
        let cache = NSCache<NSString, NSImage>()
        cache.countLimit = 128
        return cache
    }()

    static func read(urls: [URL]) -> (photos: [PhotoAsset], skipped: [String]) {
        let fm = FileManager.default
        var candidates: [URL] = []
        var seen = Set<String>()
        var skipped: [String] = []
        for selected in urls {
            guard selected.isFileURL else {
                skipped.append("No se puede leer «\(selected.lastPathComponent)».")
                continue
            }
            let url = selected.standardizedFileURL.resolvingSymlinksInPath()
            var directory: ObjCBool = false
            if fm.fileExists(atPath: url.path, isDirectory: &directory), directory.boolValue {
                guard seen.insert(url.path).inserted else { continue }
                do {
                    let children = try fm.contentsOfDirectory(at: url, includingPropertiesForKeys: [.isDirectoryKey], options: .skipsHiddenFiles)
                    for child in children {
                        let suffix = child.pathExtension.lowercased()
                        guard commonImageExtensions.contains(suffix) || UTType(filenameExtension: suffix)?.conforms(to: .image) == true,
                              (try? child.resourceValues(forKeys: [.isDirectoryKey]).isDirectory) != true else { continue }
                        let canonical = child.standardizedFileURL.resolvingSymlinksInPath()
                        if seen.insert(canonical.path).inserted { candidates.append(canonical) }
                    }
                } catch {
                    skipped.append("No se puede leer la carpeta «\(url.lastPathComponent)».")
                }
            } else if seen.insert(url.path).inserted {
                candidates.append(url)
            }
        }
        candidates.sort {
            let order = $0.lastPathComponent.localizedStandardCompare($1.lastPathComponent)
            return order == .orderedSame ? $0.path < $1.path : order == .orderedAscending
        }
        var photos: [PhotoAsset] = []
        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale(identifier: "en_US_POSIX")
        dateFormatter.calendar = Calendar(identifier: .gregorian)
        dateFormatter.dateFormat = "yyyy:MM:dd HH:mm:ss"
        dateFormatter.isLenient = false
        var overLimit = 0
        for url in candidates {
            guard fm.isReadableFile(atPath: url.path),
                  let source = CGImageSourceCreateWithURL(url as CFURL, [kCGImageSourceShouldCache: false] as CFDictionary),
                  let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
                  let width = (properties[kCGImagePropertyPixelWidth] as? NSNumber)?.intValue,
                  let height = (properties[kCGImagePropertyPixelHeight] as? NSNumber)?.intValue,
                  width > 0, height > 0 else {
                skipped.append("No se puede leer la foto «\(url.lastPathComponent)».")
                continue
            }
            guard photos.count < 2000 else { overLimit += 1; continue }
            let orientation = (properties[kCGImagePropertyOrientation] as? NSNumber)?.intValue ?? 1
            let sideways = (5...8).contains(orientation)
            let exif = properties[kCGImagePropertyExifDictionary] as? [CFString: Any]
            let tiff = properties[kCGImagePropertyTIFFDictionary] as? [CFString: Any]
            let dateText = (exif?[kCGImagePropertyExifDateTimeOriginal] as? String) ?? (tiff?[kCGImagePropertyTIFFDateTime] as? String)
            let takenAt = dateText.flatMap { $0.count == 19 ? dateFormatter.date(from: $0) : nil }
                .map { Int64(($0.timeIntervalSince1970 * 1000).rounded()) }
            photos.append(PhotoAsset(path: url.path, pixelWidth: sideways ? height : width, pixelHeight: sideways ? width : height,
                                     takenAtEpochMs: takenAt))
        }
        if overLimit > 0 {
            skipped.append("El límite es de 2000 fotos por selección. No se añadieron \(overLimit) fotos; puedes agregarlas en otro proyecto.")
        }
        return (photos, skipped)
    }

    static func thumbnail(for photo: PhotoAsset) -> NSImage? {
        let key = photo.path as NSString
        if let image = thumbnails.object(forKey: key) { return image }
        guard let source = CGImageSourceCreateWithURL(photo.url as CFURL, [kCGImageSourceShouldCache: false] as CFDictionary),
              let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 256
              ] as CFDictionary) else { return nil }
        let image = NSImage(cgImage: cgImage, size: NSSize(width: cgImage.width, height: cgImage.height))
        thumbnails.setObject(image, forKey: key)
        return image
    }

    static let folderLimit = 500
    /// «Agregar carpeta»: imágenes del primer nivel (sin subcarpetas), por nombre natural, hasta `limit`.
    /// `ignored` cuenta lo que no es imagen; `omitted`, las imágenes que no entran por el tope.
    static func folderListing(_ folder: URL, limit: Int = folderLimit) -> (urls: [URL], omitted: Int, ignored: Int) {
        let children = (try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: [.isDirectoryKey], options: .skipsHiddenFiles)) ?? []
        var images: [URL] = [], ignored = 0
        for url in children {
            if (try? url.resourceValues(forKeys: [.isDirectoryKey]).isDirectory) == true { continue }
            if UTType(filenameExtension: url.pathExtension.lowercased())?.conforms(to: .image) == true { images.append(url) } else { ignored += 1 }
        }
        images.sort { $0.lastPathComponent.localizedStandardCompare($1.lastPathComponent) == .orderedAscending }
        return (Array(images.prefix(limit)), max(0, images.count - limit), ignored)
    }
    static func folderStatus(added: Int, unreadable: Int, omitted: Int, limit: Int = folderLimit) -> String {
        if omitted > 0 { return "Se agregaron las primeras \(limit); el resto se omitió" }
        return "\(added) \(added == 1 ? "foto agregada" : "fotos agregadas")" + (unreadable > 0 ? " · \(unreadable) no se pudieron leer" : "")
    }
}
