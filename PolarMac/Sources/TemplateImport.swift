import Foundation
import CoreGraphics
import ImageIO

/// Forma del hueco de una foto en un molde. Un valor desconocido (de una versión más nueva) se lee como rectángulo.
enum RegionShape: String, Codable, CaseIterable, Identifiable, Sendable {
    case rect, round, ellipse
    var id: String { rawValue }
    var name: String { self == .rect ? "Rectángulo" : self == .round ? "Redondeado" : "Óvalo" }
    init(from decoder: Decoder) throws {
        self = RegionShape(rawValue: (try? decoder.singleValueContainer().decode(String.self)) ?? "") ?? .rect
    }
}

struct TemplateRegion: Codable, Equatable, Identifiable, Sendable {
    var id = UUID()
    var x: Double
    var y: Double
    var width: Double
    var height: Double
    var isTransparent = false
    /// Campos opcionales del molde v2: un `.polar` anterior no los trae y se leen como rectángulo sin radio.
    var shape: RegionShape = .rect
    /// Fracción del lado menor del hueco (0...0.5); sólo se usa con `.round`.
    var radius: Double = 0
    enum CodingKeys: String, CodingKey { case id, x, y, width, height, isTransparent, shape, radius }
    init(id: UUID = UUID(), x: Double, y: Double, width: Double, height: Double, isTransparent: Bool = false, shape: RegionShape = .rect, radius: Double = 0) {
        self.id = id; self.x = x; self.y = y; self.width = width; self.height = height
        self.isTransparent = isTransparent; self.shape = shape; self.radius = radius
    }
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(UUID.self, forKey: .id) ?? UUID()
        x = try c.decode(Double.self, forKey: .x); y = try c.decode(Double.self, forKey: .y)
        width = try c.decode(Double.self, forKey: .width); height = try c.decode(Double.self, forKey: .height)
        isTransparent = try c.decodeIfPresent(Bool.self, forKey: .isTransparent) ?? false
        shape = (try? c.decodeIfPresent(RegionShape.self, forKey: .shape)) ?? .rect
        radius = (try? c.decodeIfPresent(Double.self, forKey: .radius)) ?? 0
    }
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
    /// `approximateShapes`: huecos cuya forma no encajó bien (IoU < 0.85) y quedaron como rectángulo: «forma aproximada».
    static func read(url: URL) throws -> (template: ImportedTemplate, detectedCount: Int, approximateShapes: Int) {
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
        let ordered = rowOrder(found.map(\.region))
        let accepted = Array(ordered.prefix(64))
        let approximate = found.filter { item in accepted.contains { $0.id == item.region.id } && item.approximate }.count
        let manual = TemplateRegion(x: 0.2, y: 0.2, width: 0.6, height: 0.6)
        return (ImportedTemplate(path: canonical.path, pixelWidth: pixelWidth, pixelHeight: pixelHeight,
                                 regions: accepted.isEmpty ? [manual] : accepted), accepted.count, approximate)
    }

    private static func unreadable(_ url: URL) -> NSError {
        NSError(domain: "PolarTemplate", code: 1, userInfo: [NSLocalizedDescriptionKey:
            "No se puede leer el diseño «\(url.lastPathComponent)». Elige una imagen PNG o JPG de hasta 150 megapíxeles."])
    }

    private static func regions(_ bytes: [UInt8], width: Int, height: Int, transparent: Bool) -> [(region: TemplateRegion, approximate: Bool)] {
        var visited = [Bool](repeating: false, count: width * height)
        func matches(_ i: Int) -> Bool {
            let p = i * 4
            if transparent { return bytes[p + 3] <= 24 }
            return bytes[p + 3] >= 245 && bytes[p] >= 235 && bytes[p + 1] >= 235 && bytes[p + 2] >= 235
        }
        var output: [(region: TemplateRegion, approximate: Bool)] = [], queue: [Int] = []
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
                  Double(queue.count) / Double(area) >= 0.70 else { continue }
            var mask = [Bool](repeating: false, count: area)
            for i in queue { mask[(i / width - minY) * w + (i % width - minX)] = true }
            let fit = classifyShape(mask: mask, width: w, height: h)
            output.append((TemplateRegion(x: Double(minX) / Double(width), y: Double(minY) / Double(height),
                                          width: Double(w) / Double(width), height: Double(h) / Double(height),
                                          isTransparent: transparent, shape: fit.shape, radius: fit.radius), fit.approximate))
        }
        return output
    }

    /// Intersección sobre unión entre la máscara del hueco y cada forma candidata, calculada sobre el rectángulo envolvente.
    /// Un rectángulo redondeado sólo gana si explica al menos 0.8 % del área que le falta al rectángulo; la elipse gana empates.
    /// Si ninguna encaja con IoU ≥ 0.85, devuelve `.rect` con `approximate = true` («forma aproximada»).
    static func classifyShape(mask: [Bool], width: Int, height: Int) -> (shape: RegionShape, radius: Double, approximate: Bool) {
        let total = width * height
        guard total > 0, mask.count == total else { return (.rect, 0, true) }
        let filled = mask.reduce(0) { $0 + ($1 ? 1 : 0) }
        let rectIoU = Double(filled) / Double(total)
        if rectIoU >= 0.995 { return (.rect, 0, false) }
        let w = Double(width), h = Double(height), minSide = min(w, h)
        func iou(_ inside: (Double, Double) -> Bool) -> Double {
            var both = 0, either = 0
            for y in 0..<height { for x in 0..<width {
                let a = mask[y * width + x], b = inside(Double(x) + 0.5, Double(y) + 0.5)
                if a && b { both += 1 }
                if a || b { either += 1 }
            } }
            return either == 0 ? 0 : Double(both) / Double(either)
        }
        let ellipse = iou { x, y in let dx = (x - w / 2) / (w / 2), dy = (y - h / 2) / (h / 2); return dx * dx + dy * dy <= 1 }
        let radiusPx = min(minSide / 2, max(0, (Double(total - filled) / (4 - Double.pi)).squareRoot()))
        let roundIoU = iou { x, y in
            let cx = min(x, w - x), cy = min(y, h - y)
            guard cx < radiusPx && cy < radiusPx else { return true }
            let dx = radiusPx - cx, dy = radiusPx - cy
            return dx * dx + dy * dy <= radiusPx * radiusPx
        }
        let fraction = radiusPx / minSide
        if ellipse >= max(roundIoU - 0.005, rectIoU + 0.008) && ellipse >= 0.85 { return (.ellipse, 0, false) }
        if roundIoU >= rectIoU + 0.008 && roundIoU >= 0.85 { return (.round, (fraction * 1000).rounded() / 1000, false) }
        if rectIoU >= 0.85 { return (.rect, 0, false) }
        return (.rect, 0, true)
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
