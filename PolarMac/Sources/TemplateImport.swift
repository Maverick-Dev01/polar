import Foundation
import CoreGraphics
import ImageIO

struct TemplateRegion: Codable, Equatable, Identifiable, Sendable {
    var id = UUID()
    var x: Double
    var y: Double
    var width: Double
    var height: Double
    var isTransparent = false
    var rect: CGRect { CGRect(x: x, y: y, width: width, height: height) }

    mutating func clamp() {
        width = min(1, max(0.02, width.isFinite ? width : 0.02))
        height = min(1, max(0.02, height.isFinite ? height : 0.02))
        x = min(1 - width, max(0, x.isFinite ? x : 0))
        y = min(1 - height, max(0, y.isFinite ? y : 0))
    }
}

struct ImportedTemplate: Codable, Equatable, Sendable {
    var path: String
    var pixelWidth: Int
    var pixelHeight: Int
    var regions: [TemplateRegion]
}

enum TemplateImporter {
    static func read(url: URL) throws -> (template: ImportedTemplate, detectedCount: Int) {
        let canonical = url.standardizedFileURL.resolvingSymlinksInPath()
        guard url.isFileURL, FileManager.default.isReadableFile(atPath: canonical.path),
              let source = CGImageSourceCreateWithURL(canonical as CFURL, [kCGImageSourceShouldCache: false] as CFDictionary),
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              var pixelWidth = (properties[kCGImagePropertyPixelWidth] as? NSNumber)?.intValue,
              var pixelHeight = (properties[kCGImagePropertyPixelHeight] as? NSNumber)?.intValue,
              pixelWidth > 0, pixelHeight > 0, pixelWidth <= 30_000, pixelHeight <= 30_000,
              pixelWidth * pixelHeight <= 150_000_000,
              let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 1600,
                kCGImageSourceShouldCacheImmediately: true
              ] as CFDictionary)
        else { throw unreadable(url) }
        if (5...8).contains((properties[kCGImagePropertyOrientation] as? NSNumber)?.intValue ?? 1) {
            swap(&pixelWidth, &pixelHeight)
        }
        let width = image.width, height = image.height
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        let decoded = pixels.withUnsafeMutableBytes { buffer -> Bool in
            guard let context = CGContext(data: buffer.baseAddress, width: width, height: height, bitsPerComponent: 8,
                                          bytesPerRow: width * 4, space: CGColorSpaceCreateDeviceRGB(),
                                          bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return false }
            context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
            return true
        }
        guard decoded else { throw unreadable(url) }
        let found = regions(pixels, width: width, height: height, transparent: true)
            + regions(pixels, width: width, height: height, transparent: false)
        let ordered = rowOrder(found)
        let accepted = Array(ordered.prefix(64))
        let manual = TemplateRegion(x: 0.2, y: 0.2, width: 0.6, height: 0.6)
        return (ImportedTemplate(path: canonical.path, pixelWidth: pixelWidth, pixelHeight: pixelHeight,
                                 regions: accepted.isEmpty ? [manual] : accepted), accepted.count)
    }

    private static func unreadable(_ url: URL) -> NSError {
        NSError(domain: "PolarTemplate", code: 1, userInfo: [NSLocalizedDescriptionKey:
            "No se puede leer el diseño «\(url.lastPathComponent)». Elige una imagen PNG o JPG de hasta 150 megapíxeles."])
    }

    private static func regions(_ bytes: [UInt8], width: Int, height: Int, transparent: Bool) -> [TemplateRegion] {
        var visited = [Bool](repeating: false, count: width * height)
        func matches(_ i: Int) -> Bool {
            let p = i * 4
            if transparent { return bytes[p + 3] <= 24 }
            return bytes[p + 3] >= 245 && bytes[p] >= 235 && bytes[p + 1] >= 235 && bytes[p + 2] >= 235
        }
        var output: [TemplateRegion] = [], queue: [Int] = []
        // ponytail: enclosed rectangular light/alpha areas only; use masks if irregular apertures become necessary.
        for start in visited.indices where !visited[start] && matches(start) {
            visited[start] = true; queue.removeAll(keepingCapacity: true); queue.append(start)
            var cursor = 0, minX = width, minY = height, maxX = 0, maxY = 0, edge = false
            while cursor < queue.count {
                let i = queue[cursor], x = i % width, y = i / width
                cursor += 1
                minX = min(minX, x); minY = min(minY, y); maxX = max(maxX, x); maxY = max(maxY, y)
                edge = edge || x == 0 || y == 0 || x == width - 1 || y == height - 1
                func visit(_ n: Int) {
                    if !visited[n] && matches(n) { visited[n] = true; queue.append(n) }
                }
                if x > 0 { visit(i - 1) }; if x + 1 < width { visit(i + 1) }
                if y > 0 { visit(i - width) }; if y + 1 < height { visit(i + width) }
            }
            let w = maxX - minX + 1, h = maxY - minY + 1, area = w * h
            guard !edge, w >= max(8, width / 30), h >= max(8, height / 30),
                  area >= width * height / 200, area < width * height * 9 / 10,
                  Double(queue.count) / Double(area) >= 0.90 else { continue }
            output.append(TemplateRegion(x: Double(minX) / Double(width), y: Double(minY) / Double(height),
                                         width: Double(w) / Double(width), height: Double(h) / Double(height),
                                         isTransparent: transparent))
        }
        return output
    }

    private static func rowOrder(_ regions: [TemplateRegion]) -> [TemplateRegion] {
        let sorted = regions.sorted { $0.y == $1.y ? $0.x < $1.x : $0.y < $1.y }
        var rows: [[TemplateRegion]] = []
        for region in sorted {
            if let first = rows.last?.first, abs(region.y - first.y) <= min(region.height, first.height) * 0.25 {
                rows[rows.count - 1].append(region)
            } else { rows.append([region]) }
        }
        return rows.flatMap { $0.sorted { $0.x < $1.x } }
    }
}
