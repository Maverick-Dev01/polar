import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

@main struct TemplateChecks {
    static func main() throws {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("polar-templates-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: folder) }

        // Missing alpha/white analysis, border exclusion, EXIF transform, or source preservation breaks these checks.
        let transparentURL = folder.appendingPathComponent("transparent.png")
        try image(to: transparentURL, transparent: true)
        let original = try Data(contentsOf: transparentURL)
        let alpha = try TemplateImporter.read(url: transparentURL)
        precondition(alpha.detectedCount == 2, "Detect the two enclosed transparent openings")
        precondition(alpha.template.regions.allSatisfy(\.isTransparent), "Preserve the transparent overlay flag")
        check(alpha.template.regions[0].rect, CGRect(x: 0.1, y: 0.1, width: 0.3, height: 0.3))
        check(alpha.template.regions[1].rect, CGRect(x: 0.6, y: 0.55, width: 0.25, height: 0.35))
        let afterImport = try Data(contentsOf: transparentURL)
        precondition(afterImport == original, "Import never changes the original template")

        let whiteURL = folder.appendingPathComponent("white.jpg")
        try image(to: whiteURL, transparent: false)
        let white = try TemplateImporter.read(url: whiteURL)
        precondition(white.detectedCount == 2 && white.template.regions.allSatisfy { !$0.isTransparent }, "Detect opaque light frames separately from alpha")
        check(white.template.regions[0].rect, CGRect(x: 0.1, y: 0.1, width: 0.3, height: 0.3))

        let largeURL = folder.appendingPathComponent("large.png")
        try image(to: largeURL, transparent: true, scale: 6)
        let large = try TemplateImporter.read(url: largeURL)
        precondition(large.template.pixelWidth == 1800 && large.template.pixelHeight == 1200 && large.detectedCount == 2, "Keep original dimensions when detection downsamples")
        check(large.template.regions[0].rect, CGRect(x: 0.1, y: 0.1, width: 0.3, height: 0.3))

        let rotatedURL = folder.appendingPathComponent("rotated.jpg")
        try image(to: rotatedURL, transparent: false, orientation: 6)
        let rotated = try TemplateImporter.read(url: rotatedURL)
        precondition(rotated.template.pixelWidth == 200 && rotated.template.pixelHeight == 300, "Use EXIF-oriented template dimensions")
        precondition(rotated.detectedCount == 2)
        check(rotated.template.regions[0].rect, CGRect(x: 0.6, y: 0.1, width: 0.3, height: 0.3))

        let plainURL = folder.appendingPathComponent("blank.png")
        try image(to: plainURL, transparent: false, blank: true)
        let plain = try TemplateImporter.read(url: plainURL)
        precondition(plain.detectedCount == 0 && plain.template.regions.count == 1, "Do not mistake the whole white page for a detected opening")
        check(plain.template.regions[0].rect, CGRect(x: 0.2, y: 0.2, width: 0.6, height: 0.6))

        let manyURL = folder.appendingPathComponent("many.png")
        try image(to: manyURL, transparent: true, many: true)
        let many = try TemplateImporter.read(url: manyURL)
        precondition(many.template.regions.count == 64 && many.detectedCount == 64, "Bound detection to 64 usable regions")
        precondition(many.template.regions[0].y < many.template.regions[9].y, "Region order follows visual rows")
        precondition(abs(many.template.regions[0].x - 0.0167) < 0.002 && abs(many.template.regions[1].x - 0.1256) < 0.002, "Small vertical offsets do not reverse the left-to-right row order")

        let missing = folder.appendingPathComponent("missing.png")
        let broken = folder.appendingPathComponent("broken.png")
        try Data("invalid PNG".utf8).write(to: broken)
        for url in [missing, broken] {
            do { _ = try TemplateImporter.read(url: url); preconditionFailure("Reject unreadable templates") } catch {}
        }
        let restored = try JSONDecoder().decode(ImportedTemplate.self, from: JSONEncoder().encode(alpha.template))
        precondition(restored.regions.count == 2 && restored.regions[0].isTransparent, "Imported layout persists without changing region IDs")
        precondition(restored.regions.map(\.id) == alpha.template.regions.map(\.id))
        var region = TemplateRegion(x: 0.95, y: -0.1, width: 0.4, height: 0)
        region.clamp()
        precondition(region.rect.minX >= 0 && region.rect.minY >= 0 && region.rect.maxX <= 1 && region.rect.maxY <= 1 && region.height >= 0.02)
        try shapeChecks()
        print("TemplateChecks: transparent and white holes, borders, EXIF orientation, downsampling, fallback, row order, 64-region cap, unchanged originals, unreadable files, persistence, and rect/round/ellipse shape detection (synthetic masks and shared fixtures) passed")
    }

    /// Máscara sintética de un hueco: un cuadrado, un rectángulo redondeado (radio = fracción del lado menor) o un óvalo.
    static func mask(_ shape: RegionShape, width: Int, height: Int, radius: Double = 0) -> [Bool] {
        let r = radius * Double(min(width, height))
        var result = [Bool](repeating: false, count: width * height)
        for y in 0..<height { for x in 0..<width {
            let px = Double(x) + 0.5, py = Double(y) + 0.5
            switch shape {
            case .rect: result[y * width + x] = true
            case .ellipse:
                let dx = (px - Double(width) / 2) / (Double(width) / 2), dy = (py - Double(height) / 2) / (Double(height) / 2)
                result[y * width + x] = dx * dx + dy * dy <= 1
            case .round:
                let cx = min(px, Double(width) - px), cy = min(py, Double(height) - py)
                if cx >= r || cy >= r { result[y * width + x] = true }
                else { let dx = r - cx, dy = r - cy; result[y * width + x] = dx * dx + dy * dy <= r * r }
            }
        } }
        return result
    }

    static func shapeChecks() throws {
        let square = TemplateImporter.classifyShape(mask: mask(.rect, width: 120, height: 120), width: 120, height: 120)
        precondition(square.shape == .rect && square.radius == 0 && !square.approximate, "Un cuadrado lleno es un rectángulo")
        for (w, h) in [(200, 200), (300, 180), (160, 240)] {
            let rounded = TemplateImporter.classifyShape(mask: mask(.round, width: w, height: h, radius: 0.2), width: w, height: h)
            precondition(rounded.shape == .round && abs(rounded.radius - 0.2) <= 0.05 && !rounded.approximate,
                         "Rectángulo redondeado con radio de 20 % (\(w)×\(h)): \(rounded)")
        }
        let pill = TemplateImporter.classifyShape(mask: mask(.round, width: 300, height: 120, radius: 0.35), width: 300, height: 120)
        precondition(pill.shape == .round && abs(pill.radius - 0.35) <= 0.05, "Radio de 35 %: \(pill)")
        for (w, h) in [(150, 150), (240, 150)] {
            let circle = TemplateImporter.classifyShape(mask: mask(.ellipse, width: w, height: h), width: w, height: h)
            precondition(circle.shape == .ellipse && circle.radius == 0 && !circle.approximate, "Círculo u óvalo \(w)×\(h): \(circle)")
        }
        // Una forma que no se parece a ninguna (triángulo) cae a rectángulo y avisa «forma aproximada».
        var triangle = [Bool](repeating: false, count: 160 * 160)
        for y in 0..<160 { for x in 0..<160 where Double(x) <= Double(y) { triangle[y * 160 + x] = true } }
        let approx = TemplateImporter.classifyShape(mask: triangle, width: 160, height: 160)
        precondition(approx.shape == .rect && approx.approximate, "Un triángulo queda como rectángulo con aviso de forma aproximada")

        // Los moldes compartidos de shared-fixtures/moldes: mismas regiones, orden y forma que espera moldes.json (tolerancia 0.01; radio ±0.05).
        guard let fixtures = ProcessInfo.processInfo.environment["POLAR_FIXTURES_DIR"].map({ URL(fileURLWithPath: $0).appendingPathComponent("moldes") }) else { return }
        let document = try JSONSerialization.jsonObject(with: Data(contentsOf: fixtures.appendingPathComponent("moldes.json"))) as! [String: Any]
        let templates = document["templates"] as! [String: [String: Any]]
        for (file, entry) in templates.sorted(by: { $0.key < $1.key }) {
            guard let expected = (entry["regions"] ?? (entry["sameAs"] as? String).flatMap { templates[$0]?["regions"] }) as? [[String: Any]] else { continue }
            let result = try TemplateImporter.read(url: fixtures.appendingPathComponent(file))
            precondition(result.detectedCount == expected.count && result.approximateShapes == 0, "\(file): \(result.detectedCount) huecos, esperaba \(expected.count)")
            for (region, raw) in zip(result.template.regions, expected) {
                let rect = (raw["rect"] as! [Double])
                precondition(abs(region.x - rect[0]) < 0.01 && abs(region.y - rect[1]) < 0.01 && abs(region.width - rect[2]) < 0.01 && abs(region.height - rect[3]) < 0.01,
                             "\(file): región \(region.rect) vs \(rect)")
                precondition(region.shape.rawValue == raw["shape"] as? String, "\(file): forma \(region.shape) vs \(raw["shape"] ?? "")")
                precondition(abs(region.radius - (raw["radius"] as! Double)) <= 0.05, "\(file): radio \(region.radius) vs \(raw["radius"] ?? "")")
            }
        }
    }

    static func check(_ actual: CGRect, _ expected: CGRect) {
        precondition(abs(actual.minX - expected.minX) < 0.02 && abs(actual.minY - expected.minY) < 0.02 &&
                     abs(actual.width - expected.width) < 0.02 && abs(actual.height - expected.height) < 0.02,
                     "Expected normalized \(expected), got \(actual)")
    }

    static func image(to url: URL, transparent: Bool, orientation: Int = 1, blank: Bool = false, many: Bool = false, scale: Int = 1) throws {
        let width = (many ? 900 : 300) * scale, height = (many ? 900 : 200) * scale
        var pixels = [UInt8](repeating: 255, count: width * height * 4)
        if !blank {
            for i in 0..<(width * height) { pixels[i * 4] = 90; pixels[i * 4 + 1] = 40; pixels[i * 4 + 2] = 60 }
        }
        func paint(_ rect: CGRect) {
            let scaled = rect.applying(CGAffineTransform(scaleX: CGFloat(scale), y: CGFloat(scale)))
            for y in Int(scaled.minY)..<Int(scaled.maxY) { for x in Int(scaled.minX)..<Int(scaled.maxX) {
                let i = (y * width + x) * 4
                for c in 0..<4 { pixels[i + c] = transparent ? 0 : 255 }
            } }
        }
        if many {
            for y in 0..<9 { for x in 0..<9 { paint(CGRect(x: 15 + x * 98, y: 15 + y * 98 + (x % 3 - 1) * 2, width: 70, height: 70)) } }
        } else if !blank {
            paint(CGRect(x: 30, y: 20, width: 90, height: 60))
            paint(CGRect(x: 180, y: 110, width: 75, height: 70))
            paint(CGRect(x: 0, y: 0, width: 10, height: height / scale))
        }
        let provider = CGDataProvider(data: Data(pixels) as CFData)!
        let cgImage = CGImage(width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: width * 4,
                              space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.last.rawValue),
                              provider: provider, decode: nil, shouldInterpolate: false, intent: .defaultIntent)!
        let kind = url.pathExtension == "jpg" ? UTType.jpeg : UTType.png
        let destination = CGImageDestinationCreateWithURL(url as CFURL, kind.identifier as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, cgImage, [kCGImagePropertyOrientation: orientation,
                                                       kCGImageDestinationLossyCompressionQuality: 1] as CFDictionary)
        precondition(CGImageDestinationFinalize(destination), "Fixture should encode")
    }
}
