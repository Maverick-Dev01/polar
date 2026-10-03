import Foundation
import CoreGraphics

struct PhotoLook: Codable, Equatable, Sendable {
    let preset: String
    let intensity: Double
    let light: Double
    let contrast: Double
    let warmth: Double
    let grain: Double

    static let presets = ["original", "bw", "film", "sepia", "warm", "cool", "faded", "vivid"]
    static let names = ["original": "Original", "bw": "Blanco y negro", "film": "Película", "sepia": "Sepia", "warm": "Cálido", "cool": "Frío", "faded": "Desvanecido", "vivid": "Vivo"]
    init(preset: String = "original", intensity: Double = 1, light: Double = 0, contrast: Double = 0, warmth: Double = 0, grain: Double = 0) {
        self.preset = Self.presets.contains(preset) ? preset : "original"
        self.intensity = intensity; self.light = light; self.contrast = contrast; self.warmth = warmth; self.grain = grain
    }
    enum CodingKeys: String, CodingKey { case preset, intensity, light, contrast, warmth, grain }
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.init(preset: try c.decodeIfPresent(String.self, forKey: .preset) ?? "original",
                  intensity: try c.decodeIfPresent(Double.self, forKey: .intensity) ?? 1,
                  light: try c.decodeIfPresent(Double.self, forKey: .light) ?? 0,
                  contrast: try c.decodeIfPresent(Double.self, forKey: .contrast) ?? 0,
                  warmth: try c.decodeIfPresent(Double.self, forKey: .warmth) ?? 0,
                  grain: try c.decodeIfPresent(Double.self, forKey: .grain) ?? 0)
    }
    func validated() throws {
        guard intensity.isFinite, (0...1).contains(intensity), grain.isFinite, (0...1).contains(grain),
              [light, contrast, warmth].allSatisfy({ $0.isFinite && (-1...1).contains($0) })
        else { throw PolarError.invalidProject("Los ajustes de una foto no son válidos.") }
    }
    func replacing(preset: String? = nil, intensity: Double? = nil, light: Double? = nil, contrast: Double? = nil, warmth: Double? = nil, grain: Double? = nil) -> PhotoLook {
        PhotoLook(preset: preset ?? self.preset, intensity: intensity ?? self.intensity, light: light ?? self.light,
                  contrast: contrast ?? self.contrast, warmth: warmth ?? self.warmth, grain: grain ?? self.grain)
    }
    var name: String { Self.names[preset] ?? "Original" }
    var grainStrength: Double { min(1, max(0, grain + (preset == "film" ? 0.25 * intensity : 0))) }
    var isNeutral: Bool { (preset == "original" || intensity == 0) && light == 0 && contrast == 0 && warmth == 0 && grainStrength == 0 }
}

enum LookResolver {
    static func resolve(project: PolarProject, slot: Int) -> PhotoLook {
        let own = project.placements.indices.contains(slot) ? project.placements[slot]?.photoLook : nil
        return own ?? project.cardOverrides[String(slot / project.settings.style.photosPerCard)]?.photoLook ?? project.settings.photoLook ?? PhotoLook()
    }
}

extension PolarProject {
    mutating func setAllLooks(_ look: PhotoLook) {
        settings.photoLook = look
        for key in Array(cardOverrides.keys) {
            cardOverrides[key]?.photoLook = nil
            if cardOverrides[key]?.isEmpty == true { cardOverrides.removeValue(forKey: key) }
        }
        for slot in placements.indices { placements[slot]?.photoLook = nil }
    }
    mutating func setPageLooks(_ look: PhotoLook, pages: Set<Int>) {
        let per = settings.style.photosPerCard, cards = settings.capacity / per
        for page in pages where (0..<pageCount).contains(page) {
            for card in (page * cards)..<((page + 1) * cards) {
                var own = cardOverrides[String(card)] ?? CardOverride()
                own.photoLook = look; cardOverrides[String(card)] = own
            }
            for slot in (page * settings.capacity)..<min((page + 1) * settings.capacity, placements.count) { placements[slot]?.photoLook = nil }
        }
    }
    mutating func setPhotoLook(_ look: PhotoLook, slot: Int) {
        guard placements.indices.contains(slot), placements[slot] != nil else { return }
        placements[slot]?.photoLook = look
    }
    mutating func materializeCardLooks() {
        guard cardOverrides.values.contains(where: { $0.photoLook != nil }) else { return }
        let snapshot = self
        for slot in placements.indices where placements[slot] != nil && placements[slot]?.photoLook == nil {
            placements[slot]?.photoLook = LookResolver.resolve(project: snapshot, slot: slot)
        }
    }
    var filterSummary: String {
        let filtered = placements.indices.filter { placements[$0] != nil && !LookResolver.resolve(project: self, slot: $0).isNeutral }
        if filtered.isEmpty { return "Sin filtros" }
        if let general = settings.photoLook, !general.isNeutral,
           placements.indices.allSatisfy({ placements[$0] == nil || LookResolver.resolve(project: self, slot: $0) == general }) {
            return "Filtro: \(general.name) (todas)"
        }
        return "Filtros en \(filtered.count) \(filtered.count == 1 ? "foto" : "fotos")"
    }
}

enum PhotoFilters {
    static let identity: [Double] = [1,0,0,0,0, 0,1,0,0,0, 0,0,1,0,0, 0,0,0,1,0]
    static let matrices: [String: [Double]] = [
        "original": identity,
        "bw": [0.2126,0.7152,0.0722,0,0, 0.2126,0.7152,0.0722,0,0, 0.2126,0.7152,0.0722,0,0, 0,0,0,1,0],
        "film": [0.24449,0.82248,0.08303,0,-19.125, 0.24449,0.82248,0.08303,0,-19.125, 0.24449,0.82248,0.08303,0,-19.125, 0,0,0,1,0],
        "sepia": [0.393,0.769,0.189,0,0, 0.349,0.686,0.168,0,0, 0.272,0.534,0.131,0,0, 0,0,0,1,0],
        "warm": [1.03937,-0.03576,-0.00361,0,12, -0.01063,1.01424,-0.00361,0,0, -0.01063,-0.03576,1.04639,0,-12, 0,0,0,1,0],
        "cool": [1,0,0,0,-12, 0,1,0,0,0, 0,0,1,0,12, 0,0,0,1,0],
        "faded": [0.687402,0.193104,0.019494,0,16, 0.057402,0.823104,0.019494,0,16, 0.057402,0.193104,0.649494,0,16, 0,0,0,1,0],
        "vivid": [1.316535,-0.19668,-0.019855,0,-12.75, -0.058465,1.17832,-0.019855,0,-12.75, -0.058465,-0.19668,1.355145,0,-12.75, 0,0,0,1,0]
    ]
    static func matrix(_ look: PhotoLook) -> [Double] {
        let preset = matrices[look.preset] ?? identity
        var m = zip(identity, preset).map { $0 + look.intensity * ($1 - $0) }
        // W × C × L × preset: only RGB diagonals and biases change in these adjustments.
        let f = 1 + 0.5 * look.contrast
        for row in 0..<3 {
            for col in 0..<4 { m[row * 5 + col] *= f }
            m[row * 5 + 4] = f * (m[row * 5 + 4] + 64 * look.light) + 127.5 * (1 - f) + (row == 0 ? 20 * look.warmth : row == 2 ? -20 * look.warmth : 0)
        }
        return m
    }
    static func rgba(_ channels: [Int], matrix m: [Double], delta: Int = 0) -> [Int] {
        (0..<4).map { row in
            let v = (0..<4).reduce(m[row * 5 + 4]) { $0 + m[row * 5 + $1] * Double(channels[$1]) }
            let filtered = min(255, max(0, Int(floor(v + 0.5))))
            return min(255, max(0, filtered + (row < 3 ? delta : 0)))
        }
    }
    static func seed(assetID: String, card: Int) -> UInt32 {
        (assetID.uppercased() + ":" + String(card)).utf8.reduce(UInt32(2166136261)) { ($0 ^ UInt32($1)) &* 16777619 }
    }
    static func noise(seed: UInt32, x: Int, y: Int, strength: Double) -> Int {
        var h = seed ^ (UInt32(truncatingIfNeeded: x) &* 0x9E3779B9) ^ (UInt32(truncatingIfNeeded: y) &* 0x85EBCA6B)
        h ^= h >> 16; h = h &* 0x7FEB352D; h ^= h >> 15; h = h &* 0x846CA68B; h ^= h >> 16
        return Int(floor((2 * Double(h >> 24) / 255 - 1) * 8 * strength + 0.5))
    }
    static func apply(_ image: CGImage, look: PhotoLook, seed: UInt32 = 0, origin: CGPoint = .zero, pointsPerPixel: CGSize = CGSize(width: 1, height: 1)) throws -> CGImage {
        if look.isNeutral { return image }
        let w = image.width, h = image.height, row = w * 4
        var bytes = [UInt8](repeating: 0, count: row * h)
        let m = matrix(look)
        let result: CGImage? = bytes.withUnsafeMutableBytes { buffer in
            let pixels = buffer.bindMemory(to: UInt8.self)
            guard let context = CGContext(data: buffer.baseAddress, width: w, height: h, bitsPerComponent: 8, bytesPerRow: row,
                                          space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue) else { return nil }
            context.draw(image, in: CGRect(x: 0, y: 0, width: w, height: h))
            for y in 0..<h { for x in 0..<w {
                let i = y * row + x * 4, alpha = Int(pixels[i + 3])
                if alpha == 0 { continue }
                let a = Double(alpha) / 255
                let r = Double(pixels[i]) / a, g = Double(pixels[i + 1]) / a, b = Double(pixels[i + 2]) / a
                let delta = look.grainStrength == 0 ? 0 : noise(seed: seed, x: Int(floor(origin.x + (CGFloat(x) + 0.5) * pointsPerPixel.width)), y: Int(floor(origin.y + (CGFloat(y) + 0.5) * pointsPerPixel.height)), strength: look.grainStrength)
                for c in 0..<3 {
                    let j = c * 5
                    let filtered = min(255, max(0, floor(m[j] * r + m[j + 1] * g + m[j + 2] * b + m[j + 4] + 0.5)))
                    let v = min(255, max(0, filtered + Double(delta)))
                    pixels[i + c] = UInt8(min(Double(alpha), floor(v * a + 0.5)))
                }
            } }
            return context.makeImage()
        }
        guard let result else { throw PolarError.exportFailed }; return result
    }
}

struct PhotoFitResult {
    let center: CGPoint
    let scale: CGFloat
    let drawnSize: CGSize
}

enum PhotoFit {
    static func compute(box: CGSize, source: CGSize, placement: PhotoPlacement) -> PhotoFitResult {
        let rotated = placement.quarterTurns % 2 != 0
        let w = rotated ? source.height : source.width, h = rotated ? source.width : source.height
        let scale = max(box.width / w, box.height / h) * placement.zoom
        let drawn = CGSize(width: w * scale, height: h * scale)
        return PhotoFitResult(center: CGPoint(x: box.width / 2 + placement.offsetX * (drawn.width - box.width) / 2,
                                             y: box.height / 2 + placement.offsetY * (drawn.height - box.height) / 2), scale: scale, drawnSize: drawn)
    }
    static func fitZoom(box: CGSize, source: CGSize, quarterTurns: Int) -> Double {
        let rotated = quarterTurns % 2 != 0
        let w = rotated ? source.height : source.width, h = rotated ? source.width : source.height
        return min(box.width / w, box.height / h) / max(box.width / w, box.height / h)
    }
    static func moved(_ placement: PhotoPlacement, translation: CGSize, box: CGSize, source: CGSize) -> PhotoPlacement {
        let fit = compute(box: box, source: source, placement: placement)
        var result = placement
        if abs(fit.drawnSize.width - box.width) > 0.001 { result.offsetX = min(1, max(-1, placement.offsetX + translation.width * 2 / (fit.drawnSize.width - box.width))) }
        if abs(fit.drawnSize.height - box.height) > 0.001 { result.offsetY = min(1, max(-1, placement.offsetY + translation.height * 2 / (fit.drawnSize.height - box.height))) }
        return result
    }
}
