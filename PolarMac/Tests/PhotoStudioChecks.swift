import AppKit
import Foundation

@main struct PhotoStudioChecks {
    static func check(_ value: Bool, _ label: String) { if !value { fatalError(label) } }
    static func main() throws {
        let photos = (0..<10).map { PhotoAsset(path: "original\($0).jpg", pixelWidth: 4800, pixelHeight: 3200, maskPath: "mask\($0).png") }
        var original = PolarProject(photos: photos, placements: photos.map { PhotoPlacement(assetID: $0.id) }); original.normalized()
        var mixed = original
        mixed.setPageDesign(.postcard, page: 1); mixed.setCardDesign(.heart, card: 0)
        try mixed.validated()
        check(mixed.placements == original.placements, "scopes conserve slots")
        check(mixed.settingsForCard(0).style == .heart && mixed.settingsForCard(1).style == .polaroid && mixed.settingsForCard(9).style == .postcard, "design scope")
        check(PolarRenderer.photoRects(project: mixed, page: 0)[0] != PolarRenderer.photoRects(project: original, page: 0)[0], "mixed geometry")
        mixed.placements[2]?.zoom = 2; mixed.placements[2]?.quarterTurns = 1
        mixed.placements[2]?.background = PhotoBackground(colorHex: nil)
        mixed.cardOverrides["2"] = CardOverride(texts: ["title":"Mi título"], designStyle: .heart)
        let decoded = try JSONDecoder().decode(PolarProject.self, from: JSONEncoder().encode(mixed))
        check(decoded == mixed && decoded.placements[2]?.background?.colorHex == nil, "roundtrip transparent and design")
        var copied = mixed; copied.copySlotsToNewPage([2, 5])
        check(copied.pageCount == 3 && copied.placements[18] == mixed.placements[2], "copy selection")
        check(copied.cardOverrides["18"]?.texts == mixed.cardOverrides["2"]?.texts && copied.settingsForCard(18) == mixed.settingsForCard(2) && copied.photos == mixed.photos, "copy caption and originals")
        copied.clearSlots([2, 5]); check(copied.placements[2] == nil && copied.placements[18] != nil && copied.photos == mixed.photos, "clear positions only")
        let entries = try JSONDecoder().decode([SuggestedPhrase].self, from: Data(contentsOf: URL(fileURLWithPath: "../PolarAndroid/app/src/main/assets/catalog/phrases.json")))
        check(entries.count == 120 && Set(entries.map(\.text)).count == 120, "120 unique phrases")
        let pixel = CGContext(data: nil, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4, space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        pixel.setFillColor(NSColor.red.withAlphaComponent(0.5).cgColor); pixel.fill(CGRect(x: 0, y: 0, width: 1, height: 1))
        let subject = pixel.makeImage()!
        pixel.clear(CGRect(x: 0, y: 0, width: 1, height: 1)); pixel.setFillColor(NSColor.white.cgColor); pixel.fill(CGRect(x: 0, y: 0, width: 1, height: 1))
        let composite = try BackgroundRemover.composite(subject: subject, mask: pixel.makeImage()!, options: PhotoBackground(colorHex: "000000", feather: 0, shadow: 0), background: nil)
        let color = NSBitmapImageRep(cgImage: composite).colorAt(x: 0, y: 0)!.usingColorSpace(.sRGB)!
        check(color.alphaComponent > 0.99 && color.redComponent > 0.4 && color.redComponent < 0.9, "partial-alpha subject composes over opaque background: \(color)")
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("polar-mask-recovery-\(UUID())")
        defer { try? FileManager.default.removeItem(at: directory) }
        let library = LibraryStore(root: directory)
        _ = try library.save(UUID(), project: original)
        print("OK: diseños por hoja/tarjeta, geometría, copia, originales, fondo transparente y 120 frases")
    }
}
