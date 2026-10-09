import Foundation
import CoreGraphics
import ImageIO
import CryptoKit
import UniformTypeIdentifiers

/// Huella perceptual (dHash de 64 bits) y SHA-256 para reconocer un molde que ya se guardó.
enum ImageFingerprint {
    /// Un molde es duplicado si su SHA coincide o si la distancia de Hamming del dHash es de este valor o menos.
    static let duplicateThreshold = 6

    /// Componer sobre blanco, gris (0.299R+0.587G+0.114B), reducir a 9×8 por promedio de área exacto (límites
    /// fraccionarios) y comparar cada celda con la de su derecha: bit = celda[x] > celda[x+1] + 1.0.
    /// El primer bit es el más significativo; las filas van de arriba a abajo.
    static func dHash(_ image: CGImage) -> UInt64 {
        let width = image.width, height = image.height
        guard width > 0, height > 0 else { return 0 }
        var pixels = [UInt8](repeating: 255, count: width * height * 4)
        let drawn = pixels.withUnsafeMutableBytes { buffer -> Bool in
            guard let context = CGContext(data: buffer.baseAddress, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
                                          space: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB(),
                                          bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return false }
            context.setFillColor(CGColor(red: 1, green: 1, blue: 1, alpha: 1))
            context.fill(CGRect(x: 0, y: 0, width: width, height: height))
            context.interpolationQuality = .none
            context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
            return true
        }
        guard drawn else { return 0 }
        var gray = [Double](repeating: 0, count: width * height)
        for i in 0..<(width * height) {
            gray[i] = 0.299 * Double(pixels[i * 4]) + 0.587 * Double(pixels[i * 4 + 1]) + 0.114 * Double(pixels[i * 4 + 2])
        }
        let columns = 9, rows = 8
        var cells = [Double](repeating: 0, count: columns * rows)
        for cy in 0..<rows { for cx in 0..<columns {
            let x0 = Double(cx) * Double(width) / Double(columns), x1 = Double(cx + 1) * Double(width) / Double(columns)
            let y0 = Double(cy) * Double(height) / Double(rows), y1 = Double(cy + 1) * Double(height) / Double(rows)
            var sum = 0.0, weight = 0.0
            for y in Int(y0.rounded(.down))..<min(height, Int(y1.rounded(.up))) {
                let wy = min(Double(y + 1), y1) - max(Double(y), y0)
                guard wy > 0 else { continue }
                for x in Int(x0.rounded(.down))..<min(width, Int(x1.rounded(.up))) {
                    let wx = min(Double(x + 1), x1) - max(Double(x), x0)
                    guard wx > 0 else { continue }
                    sum += gray[y * width + x] * wx * wy; weight += wx * wy
                }
            }
            cells[cy * columns + cx] = weight > 0 ? sum / weight : 0
        } }
        var hash: UInt64 = 0
        for y in 0..<rows { for x in 0..<(columns - 1) {
            hash = (hash << 1) | (cells[y * columns + x] > cells[y * columns + x + 1] + 1.0 ? 1 : 0)
        } }
        return hash
    }

    static func hex(_ hash: UInt64) -> String { String(format: "%016llx", hash) }
    static func parse(_ hex: String) -> UInt64? { hex.count == 16 ? UInt64(hex, radix: 16) : nil }
    static func hamming(_ a: UInt64, _ b: UInt64) -> Int { (a ^ b).nonzeroBitCount }
    static func sha256(_ data: Data) -> String { SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined() }

    /// Imagen para la huella; las muy grandes se reducen (el promedio de área apenas cambia).
    static func decode(_ data: Data, maxPixels: Int = 2400) -> CGImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = (properties[kCGImagePropertyPixelWidth] as? NSNumber)?.intValue,
              let height = (properties[kCGImagePropertyPixelHeight] as? NSNumber)?.intValue, width > 0, height > 0 else { return nil }
        // Tamaño nativo (sin ampliar) hasta `maxPixels`; así la huella de un molde pequeño no se remuestrea.
        return CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true, kCGImageSourceThumbnailMaxPixelSize: min(maxPixels, max(width, height)),
            kCGImageSourceShouldCacheImmediately: true] as CFDictionary)
    }
}

/// Un molde guardado en «Mis moldes»: `templates/<id>/molde.png` y `meta.json`.
struct SavedMold: Codable, Equatable, Identifiable, Sendable {
    var id: String
    var nombre: String
    /// 16 dígitos hexadecimales.
    var dhash: String
    var sha256: String
    var regiones: [TemplateRegion]
    /// Segundos desde 2001 (`Date.timeIntervalSinceReferenceDate`), igual que Android.
    var creado: Double
    var archivo = "molde.png"
    var ancho = 0
    var alto = 0
}

struct MoldDuplicate: Equatable, Sendable {
    var mold: SavedMold
    var distance: Int
}

/// Biblioteca «Mis moldes» (Mac: Application Support/Polar/templates/<id>/).
struct MoldLibrary: Sendable {
    let root: URL
    private var fm: FileManager { .default }
    /// `appRoot` es la carpeta de datos de Polar; los moldes viven en `<appRoot>/templates`.
    init(appRoot: URL) { root = appRoot.appendingPathComponent("templates", isDirectory: true) }

    func directory(_ id: String) -> URL { root.appendingPathComponent(id, isDirectory: true) }
    func imageURL(_ id: String) -> URL { directory(id).appendingPathComponent("molde.png") }

    func list() -> [SavedMold] {
        let folders = (try? fm.contentsOfDirectory(at: root, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles])) ?? []
        return folders.compactMap { folder in
            guard let data = try? Data(contentsOf: folder.appendingPathComponent("meta.json")),
                  let mold = try? JSONDecoder().decode(SavedMold.self, from: data), mold.id == folder.lastPathComponent,
                  fm.fileExists(atPath: folder.appendingPathComponent("molde.png").path) else { return nil }
            return mold
        }.sorted { $0.creado == $1.creado ? $0.nombre < $1.nombre : $0.creado > $1.creado }
    }

    /// Guarda la imagen (como PNG) con sus huecos. La carpeta aparece completa o no aparece.
    @discardableResult func save(imageURL source: URL, nombre: String, regiones: [TemplateRegion], id: String = UUID().uuidString,
                                 now: Date = Date()) throws -> SavedMold {
        let data = try Data(contentsOf: source)
        guard let image = ImageFingerprint.decode(data) else { throw PolarError.invalidProject("No se puede leer la imagen del molde.") }
        let trimmed = nombre.trimmingCharacters(in: .whitespacesAndNewlines)
        let mold = SavedMold(id: id, nombre: trimmed.isEmpty ? "Mi molde" : String(trimmed.prefix(80)), dhash: ImageFingerprint.hex(ImageFingerprint.dHash(image)),
                             sha256: ImageFingerprint.sha256(data), regiones: regiones, creado: now.timeIntervalSinceReferenceDate, archivo: "molde.png", ancho: image.width, alto: image.height)
        let staging = root.appendingPathComponent(".\(id).\(UUID().uuidString).tmp", isDirectory: true)
        try fm.createDirectory(at: staging, withIntermediateDirectories: true)
        defer { try? fm.removeItem(at: staging) }
        if source.pathExtension.lowercased() == "png" { try data.write(to: staging.appendingPathComponent("molde.png")) }
        else { try writePNG(of: data, to: staging.appendingPathComponent("molde.png")) }
        let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        try encoder.encode(mold).write(to: staging.appendingPathComponent("meta.json"), options: .atomic)
        try? fm.removeItem(at: directory(id))
        try fm.moveItem(at: staging, to: directory(id))
        return mold
    }

    /// Vuelve a escribir los huecos de un molde que el usuario ajustó (la imagen no cambia).
    func update(_ mold: SavedMold) throws {
        let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        try encoder.encode(mold).write(to: directory(mold.id).appendingPathComponent("meta.json"), options: .atomic)
    }

    func delete(_ id: String) throws {
        guard !id.isEmpty, !id.contains("/"), fm.fileExists(atPath: directory(id).path) else { return }
        try fm.removeItem(at: directory(id))
    }

    /// El molde guardado más parecido a `imageURL`: el mismo archivo (SHA) o una huella a 6 bits o menos.
    func findDuplicate(imageURL url: URL) -> MoldDuplicate? {
        guard let data = try? Data(contentsOf: url), let image = ImageFingerprint.decode(data) else { return nil }
        return findDuplicate(image: image, sha256: ImageFingerprint.sha256(data))
    }

    func findDuplicate(image: CGImage, sha256 sha: String? = nil) -> MoldDuplicate? {
        let hash = ImageFingerprint.dHash(image)
        var best: MoldDuplicate?
        for mold in list() {
            let distance = ImageFingerprint.parse(mold.dhash).map { ImageFingerprint.hamming(hash, $0) } ?? 64
            let sameFile = sha != nil && mold.sha256 == sha
            guard sameFile || distance <= ImageFingerprint.duplicateThreshold else { continue }
            let candidate = MoldDuplicate(mold: mold, distance: sameFile ? 0 : distance)
            if best == nil || candidate.distance < best!.distance { best = candidate }
        }
        return best
    }

    /// Plantilla lista para un proyecto, apuntando a la imagen del molde guardado.
    func template(for mold: SavedMold, path: URL? = nil) throws -> ImportedTemplate {
        let file = path ?? imageURL(mold.id)
        guard let source = CGImageSourceCreateWithURL(file as CFURL, nil),
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = (properties[kCGImagePropertyPixelWidth] as? NSNumber)?.intValue,
              let height = (properties[kCGImagePropertyPixelHeight] as? NSNumber)?.intValue
        else { throw PolarError.invalidProject("No se puede leer el molde «\(mold.nombre)».") }
        return ImportedTemplate(path: file.path, pixelWidth: width, pixelHeight: height, regions: mold.regiones)
    }

    private func writePNG(of data: Data, to url: URL) throws {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil), let image = CGImageSourceCreateImageAtIndex(source, 0, [kCGImageSourceCreateThumbnailWithTransform: true] as CFDictionary),
              let destination = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) else { throw PolarError.exportFailed }
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else { throw PolarError.exportFailed }
    }
}
