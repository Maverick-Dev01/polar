import SwiftUI

/// Las 10 animaciones de `help.json`, dibujadas con SwiftUI (sin dependencias). Cada escena es una función del avance `p` (0...1);
/// con «Reducir movimiento» se muestra el cuadro final fijo (p = 1).
struct HelpAnimationView: View {
    let id: String
    /// Para capturas y pruebas: congela la escena en este avance.
    var frozenAt: Double?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    static func loop(_ t: TimeInterval) -> Double {
        let period = 3.8
        let x = t.truncatingRemainder(dividingBy: period) / period
        let q = min(1, x / 0.72)             // el resto del ciclo se queda en el cuadro final
        return q * q * (3 - 2 * q)
    }
    var body: some View {
        Group {
            if let p = frozenAt ?? (reduceMotion ? 1 : nil) { HelpScene(id: id, p: p) }
            else { TimelineView(.animation) { HelpScene(id: id, p: Self.loop($0.date.timeIntervalSinceReferenceDate)) } }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(HelpContent.shared?.animaciones[id] ?? "Animación de ayuda")
        .accessibilityAddTraits(.isImage)
    }
}

struct HelpScene: View {
    let id: String
    let p: Double
    var body: some View {
        Canvas { ctx, size in HelpScene.draw(&ctx, size, id, p) }
            .aspectRatio(2, contentMode: .fit)
    }

    private static func lerp(_ a: CGFloat, _ b: CGFloat, _ t: Double) -> CGFloat { a + (b - a) * CGFloat(min(1, max(0, t))) }
    private static func clamp(_ t: Double) -> Double { min(1, max(0, t)) }
    private static let cardFill = Color(red: 0.99, green: 0.97, blue: 0.93)
    private static let photoA = Color(red: 0.86, green: 0.55, blue: 0.45), photoB = Color(red: 0.35, green: 0.55, blue: 0.70)
    private static let ink = Color(red: 0.55, green: 0.18, blue: 0.27)

    private static func card(_ ctx: inout GraphicsContext, _ r: CGRect, photo: Color = photoA) {
        ctx.drawLayer { l in
            l.addFilter(.shadow(color: .black.opacity(0.25), radius: 3, y: 1))
            l.fill(Path(roundedRect: r, cornerRadius: 4), with: .color(cardFill))
        }
        ctx.fill(Path(roundedRect: r.insetBy(dx: r.width * 0.08, dy: r.width * 0.08).offsetBy(dx: 0, dy: -r.height * 0.05).applying(.identity), cornerRadius: 2),
                 with: .linearGradient(Gradient(colors: [photo, photo.opacity(0.65)]), startPoint: r.origin, endPoint: CGPoint(x: r.maxX, y: r.maxY)))
    }
    private static func cursor(_ ctx: inout GraphicsContext, _ pt: CGPoint) {
        ctx.fill(Path(ellipseIn: CGRect(x: pt.x - 9, y: pt.y - 9, width: 18, height: 18)), with: .color(.black.opacity(0.55)))
        ctx.stroke(Path(ellipseIn: CGRect(x: pt.x - 9, y: pt.y - 9, width: 18, height: 18)), with: .color(.white), lineWidth: 2)
    }

    static func draw(_ ctx: inout GraphicsContext, _ size: CGSize, _ id: String, _ p: Double) {
        ctx.fill(Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: 10), with: .color(.gray.opacity(0.14)))
        // Se dibuja en un lienzo virtual de 260×130 y se escala, para que se vea igual en la burbuja y en el artículo.
        let scale = min(size.width / 260, size.height / 130)
        ctx.translateBy(x: size.width / 2, y: size.height / 2); ctx.scaleBy(x: scale, y: scale); ctx.translateBy(x: -130, y: -65)
        let w: CGFloat = 260, h: CGFloat = 130
        let center = CGPoint(x: w / 2, y: h / 2)
        switch id {
        case "tap":
            let r = CGRect(x: center.x - 55, y: center.y - 36, width: 110, height: 72)
            let hit = clamp((p - 0.6) / 0.4)
            card(&ctx, r)
            if hit > 0 {
                ctx.stroke(Path(roundedRect: r.insetBy(dx: -3, dy: -3), cornerRadius: 6), with: .color(ink), lineWidth: 3)
                let rad = 10 + 22 * hit
                ctx.stroke(Path(ellipseIn: CGRect(x: center.x - rad, y: center.y - rad, width: rad * 2, height: rad * 2)), with: .color(ink.opacity(1 - hit * 0.8)), lineWidth: 2)
            }
            let q = clamp(p / 0.6)
            cursor(&ctx, CGPoint(x: lerp(w * 0.12, center.x + 20, q), y: lerp(h * 0.14, center.y + 14, q)))
        case "swipe":
            let sz = CGSize(width: 110, height: 72)
            let a = CGRect(origin: CGPoint(x: lerp(center.x - 55, -sz.width, p), y: center.y - 36), size: sz)
            let b = CGRect(origin: CGPoint(x: lerp(w + 10, center.x - 55, p), y: center.y - 36), size: sz)
            card(&ctx, a); card(&ctx, b, photo: photoB)
            cursor(&ctx, CGPoint(x: lerp(center.x + 30, 30, p), y: center.y + 10))
        case "fill":
            let sheet = CGRect(x: center.x - 52, y: 8, width: 104, height: h - 16)
            ctx.fill(Path(roundedRect: sheet, cornerRadius: 4), with: .color(cardFill))
            let colors: [Color] = [photoA, photoB, Color(red: 0.45, green: 0.65, blue: 0.45), Color(red: 0.85, green: 0.7, blue: 0.35)]
            for i in 0..<4 {
                let slot = CGRect(x: sheet.minX + 10 + CGFloat(i % 2) * 44, y: sheet.minY + 10 + CGFloat(i / 2) * ((sheet.height - 20) / 2), width: 40, height: (sheet.height - 28) / 2)
                ctx.stroke(Path(roundedRect: slot, cornerRadius: 3), with: .color(.gray.opacity(0.6)), style: StrokeStyle(lineWidth: 1, dash: [3, 2]))
                let f = clamp(p * 4.2 - Double(i) * 0.9)
                if f > 0 { ctx.fill(Path(roundedRect: slot.insetBy(dx: (1 - f) * 16, dy: (1 - f) * 12), cornerRadius: 3), with: .color(colors[i].opacity(f))) }
            }
        case "crop-ring":
            let photo = CGRect(x: center.x - 80, y: center.y - 50, width: 160, height: 100)
            ctx.fill(Path(roundedRect: photo, cornerRadius: 4), with: .linearGradient(Gradient(colors: [photoA, photoB]), startPoint: photo.origin, endPoint: CGPoint(x: photo.maxX, y: photo.maxY)))
            let ring = CGRect(x: lerp(photo.minX + 6, photo.minX + 40, p), y: lerp(photo.minY + 4, photo.minY + 12, p), width: lerp(60, 90, p), height: lerp(60, 76, p))
            var dim = Path(photo); dim.addRoundedRect(in: ring, cornerSize: CGSize(width: 6, height: 6))
            ctx.fill(dim, with: .color(.black.opacity(0.5)), style: FillStyle(eoFill: true))
            ctx.stroke(Path(roundedRect: ring, cornerRadius: 6), with: .color(.white), lineWidth: 2.5)
        case "filter-swap":
            let photo = CGRect(x: center.x - 80, y: center.y - 50, width: 160, height: 100)
            ctx.fill(Path(roundedRect: photo, cornerRadius: 4), with: .linearGradient(Gradient(colors: [photoA, photoB]), startPoint: photo.origin, endPoint: CGPoint(x: photo.maxX, y: photo.maxY)))
            let phase = p * 2, frac = phase >= 2 ? 1 : phase - floor(phase)
            let barX = photo.minX + photo.width * lerp(0, 1, frac)
            let filtered: [Color] = p < 0.5 ? [Color(white: 0.75), Color(white: 0.25)] : [Color(red: 0.80, green: 0.66, blue: 0.45), Color(red: 0.38, green: 0.27, blue: 0.16)]
            ctx.drawLayer { l in
                l.clip(to: Path(CGRect(x: photo.minX, y: photo.minY, width: barX - photo.minX, height: photo.height)))
                l.fill(Path(roundedRect: photo, cornerRadius: 4), with: .linearGradient(Gradient(colors: filtered), startPoint: photo.origin, endPoint: CGPoint(x: photo.maxX, y: photo.maxY)))
            }
            ctx.fill(Path(CGRect(x: barX - 1.5, y: photo.minY - 4, width: 3, height: photo.height + 8)), with: .color(.white))
        case "text-type":
            let photo = CGRect(x: center.x - 70, y: 10, width: 140, height: h - 50)
            ctx.fill(Path(roundedRect: photo, cornerRadius: 4), with: .linearGradient(Gradient(colors: [photoA, photoB]), startPoint: photo.origin, endPoint: CGPoint(x: photo.maxX, y: photo.maxY)))
            let line = "Feliz cumpleaños"
            let n = Int((Double(line.count) * clamp(p / 0.9)).rounded(.down))
            let typed = String(line.prefix(n)) + (p < 1 ? "▏" : "")
            ctx.draw(Text(typed).font(.custom("Georgia", size: 15)).foregroundColor(ink), at: CGPoint(x: center.x, y: h - 20), anchor: .center)
        case "print":
            let printer = CGRect(x: center.x - 60, y: 16, width: 120, height: 40)
            let sheetW: CGFloat = 54, sheetH: CGFloat = 70
            let sheet = CGRect(x: center.x - sheetW / 2, y: lerp(-70, 48, p), width: sheetW, height: sheetH)
            ctx.fill(Path(roundedRect: sheet, cornerRadius: 3), with: .color(cardFill))
            for i in 0..<3 { ctx.fill(Path(CGRect(x: sheet.minX + 8, y: sheet.minY + 10 + CGFloat(i) * 16, width: sheetW - 16, height: 10)), with: .color(photoA.opacity(0.7))) }
            ctx.fill(Path(roundedRect: printer, cornerRadius: 8), with: .color(Color(white: 0.35)))
            ctx.fill(Path(roundedRect: CGRect(x: printer.minX + 14, y: printer.maxY - 12, width: printer.width - 28, height: 5), cornerRadius: 2), with: .color(.black.opacity(0.6)))
            if p > 0.75 { ctx.draw(Text("100 %").font(.system(size: 13, weight: .bold)).foregroundColor(ink), at: CGPoint(x: center.x + 40, y: 98), anchor: .leading) }
        case "qr":
            let r = CGRect(x: center.x - 60, y: center.y - 40, width: 120, height: 80)
            card(&ctx, r, photo: Color(red: 0.4, green: 0.4, blue: 0.5))
            let cell: CGFloat = 5, origin = CGPoint(x: r.maxX - 7 * cell - 12, y: r.maxY - 7 * cell - 12)
            ctx.fill(Path(CGRect(x: origin.x - 3, y: origin.y - 3, width: 7 * cell + 6, height: 7 * cell + 6)), with: .color(.white))
            let total = 49
            for i in 0..<7 { for j in 0..<7 where (i * 7 + j * 3 + i * j) % 3 != 0 || ((i < 2 || i > 4) && (j < 2 || j > 4)) {
                if Double(i * 7 + j) < p * Double(total) { ctx.fill(Path(CGRect(x: origin.x + CGFloat(j) * cell, y: origin.y + CGFloat(i) * cell, width: cell - 0.5, height: cell - 0.5)), with: .color(.black)) }
            } }
        case "mold-detect":
            let img = CGRect(x: center.x - 80, y: center.y - 52, width: 160, height: 104)
            ctx.fill(Path(roundedRect: img, cornerRadius: 4), with: .color(Color(red: 0.88, green: 0.78, blue: 0.80)))
            for i in 0..<4 {
                let hole = CGRect(x: img.minX + 14 + CGFloat(i % 2) * 70, y: img.minY + 12 + CGFloat(i / 2) * 46, width: 58, height: 36)
                ctx.fill(Path(roundedRect: hole, cornerRadius: 3), with: .color(.white))
                let t = clamp(p * 1.4 - Double(i) * 0.15), grow = 10 * (1 - t)
                if t > 0 { ctx.stroke(Path(roundedRect: hole.insetBy(dx: -grow, dy: -grow), cornerRadius: 4), with: .color(ink.opacity(t)), style: StrokeStyle(lineWidth: 2.5, dash: t < 1 ? [4, 3] : [])) }
            }
        case "bg-remove":
            let box = CGRect(x: center.x - 70, y: center.y - 50, width: 140, height: 100)
            var checker = Path()
            for i in 0..<14 { for j in 0..<10 where (i + j) % 2 == 0 { checker.addRect(CGRect(x: box.minX + CGFloat(i) * 10, y: box.minY + CGFloat(j) * 10, width: 10, height: 10)) } }
            ctx.fill(Path(box), with: .color(.white)); ctx.fill(checker, with: .color(Color(white: 0.82)))
            ctx.fill(Path(box), with: .color(photoB.opacity(1 - p)))
            ctx.fill(Path(ellipseIn: CGRect(x: center.x - 14, y: center.y - 36, width: 28, height: 28)), with: .color(photoA))
            ctx.fill(Path(roundedRect: CGRect(x: center.x - 28, y: center.y - 4, width: 56, height: 52), cornerRadius: 22), with: .color(photoA))
        default:
            ctx.draw(Text("Ayuda").font(.title3), at: center)
        }
    }
}
