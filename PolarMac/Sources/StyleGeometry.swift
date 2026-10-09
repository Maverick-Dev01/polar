import Foundation
import CoreGraphics

/// Geometría de los diseños nuevos de la fase 3. La fuente única es `shared-fixtures/estilos-geometria.json`
/// (la app la lleva en Resources; las pruebas y las capturas la encuentran con POLAR_GEOMETRY_JSON).
/// Android la lee del mismo archivo, así que la salida coincide. Todas las x,y,w,h son fracciones de la tarjeta.
struct PhotoSlotGeometry: Decodable, Equatable, Sendable {
    var x: Double, y: Double, w: Double, h: Double
    var shape: RegionShape
    var radius: Double
}

struct TextSlotGeometry: Decodable, Equatable, Sendable {
    var role: String
    var x: Double, y: Double, w: Double, h: Double
    var align: String
    var defaultFont: String
    var defaultSizePt: Double
    var color: String
    var bold: Bool
    var textRole: TextRole? { TextRole(rawValue: role.lowercased()) }
}

struct DecorationGeometry: Decodable, Equatable, Sendable {
    var type: String
    var x: Double, y: Double, w: Double, h: Double
    var color: String
    var opacity: Double
    var rotationDeg: Double
    var layer: String
    var strokeW: Double?
    var count: Int?
    var radius: Double?
}

struct QRSlotGeometry: Decodable, Equatable, Sendable {
    var x: Double, y: Double, size: Double
    var sizeBasis: String
}

struct StyleGeometry: Decodable, Equatable, Sendable {
    var displayName: String
    var description: String
    var category: String
    var photosPerCard: Int
    var cardAspect: Double
    var defaultBackground: String
    var supportsDate: Bool
    var textRoles: [String]
    var photoSlots: [PhotoSlotGeometry]
    var textSlots: [TextSlotGeometry]
    var decorations: [DecorationGeometry]
    var qrSlot: QRSlotGeometry?
}

struct StyleGeometryDocument: Decodable, Sendable {
    var version: Int
    var referenceCardWidthPt: Double
    var styles: [String: StyleGeometry]
}

enum StyleGeometryTable {
    /// Se busca en POLAR_GEOMETRY_JSON y, si no, en Resources/estilos-geometria.json de la app.
    static func locate() -> URL? {
        let environment = ProcessInfo.processInfo.environment["POLAR_GEOMETRY_JSON"].map { URL(fileURLWithPath: $0) }
        let bundled = Bundle.main.url(forResource: "estilos-geometria", withExtension: "json")
        return [environment, bundled].compactMap { $0 }.first { FileManager.default.isReadableFile(atPath: $0.path) }
    }
    static let document: StyleGeometryDocument? = {
        guard let url = locate(), let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(StyleGeometryDocument.self, from: data)
    }()
    static var referenceCardWidthPt: CGFloat { CGFloat(document?.referenceCardWidthPt ?? 180) }
    static func geometry(_ style: TemplateStyle) -> StyleGeometry? { document?.styles[style.rawValue] }
    /// Marcos de foto en puntos para una tarjeta; sin la tabla (instalación incompleta) cada foto ocupa la tarjeta entera.
    static func photoRects(in card: CGRect, style: TemplateStyle) -> [CGRect] {
        guard let slots = geometry(style)?.photoSlots else { return Array(repeating: card, count: style.photosPerCard) }
        return slots.map { CGRect(x: card.minX + $0.x * card.width, y: card.minY + $0.y * card.height, width: $0.w * card.width, height: $0.h * card.height) }
    }
    static func textSlot(_ style: TemplateStyle, _ role: TextRole) -> TextSlotGeometry? {
        geometry(style)?.textSlots.first { $0.textRole == role }
    }
}

/// Posición del QR en los diseños musicales previos a la fase 3 (la tabla compartida sólo define vinilo y casete).
/// Fracciones de la tarjeta: x, y de la esquina superior izquierda; `size` = lado como fracción del ancho.
enum LegacyQRSlot {
    static func slot(_ style: TemplateStyle, aspect: CGFloat) -> QRSlotGeometry? {
        switch style {
        case .playerRed: return QRSlotGeometry(x: 0.62, y: 0.40, size: 0.26, sizeBasis: "width")
        case .playerGray: return QRSlotGeometry(x: 0.355, y: 0.60, size: 0.14, sizeBasis: "width")
        default: return nil
        }
    }
}
