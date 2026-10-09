import SwiftUI
import AppKit

// Capturas fuera de pantalla de la guía. Se compila junto con Sources/ (sin @main) como main.swift; ver snapshots.sh.
// Dibuja ventanas que nunca se muestran ni reciben el teclado o el ratón.
let outDir = URL(fileURLWithPath: CommandLine.arguments.dropFirst().first ?? ".")
let app = NSApplication.shared
app.setActivationPolicy(.prohibited)

@MainActor func spin(_ seconds: Double) { RunLoop.main.run(until: Date().addingTimeInterval(seconds)) }

@MainActor func solidPNG(_ url: URL, _ color: NSColor, size: Int = 900) throws {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size * 2 / 3, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    let gradient = NSGradient(starting: color, ending: color.blended(withFraction: 0.5, of: .white) ?? color)!
    gradient.draw(in: NSRect(x: 0, y: 0, width: rep.pixelsWide, height: rep.pixelsHigh), angle: 40)
    NSGraphicsContext.restoreGraphicsState()
    try rep.representation(using: .png, properties: [:])!.write(to: url)
}

@MainActor func makeStudio(dark: Bool) throws -> Studio {
    let root = FileManager.default.temporaryDirectory.appendingPathComponent("polar-snap-\(UUID().uuidString)")
    let photos = root.appendingPathComponent("fotos"); try FileManager.default.createDirectory(at: photos, withIntermediateDirectories: true)
    let colors: [NSColor] = [.systemOrange, .systemTeal, .systemPink, .systemGreen]
    var urls: [URL] = []
    for (i, c) in colors.enumerated() { let u = photos.appendingPathComponent("foto\(i + 1).png"); try solidPNG(u, c); urls.append(u) }
    let studio = Studio(storageRoot: root.appendingPathComponent("lib"))
    studio.preferences.theme = dark ? .dark : .light
    studio.preferences.onboardingSeen = true
    studio.showingLibrary = false
    studio.importURLs(urls, fill: true)
    spin(2.5)
    return studio
}

@MainActor func snapshot<V: View>(_ view: V, size: CGSize, dark: Bool, to url: URL) {
    let window = NSWindow(contentRect: NSRect(origin: .zero, size: size), styleMask: [.borderless], backing: .buffered, defer: false)
    window.appearance = NSAppearance(named: dark ? .darkAqua : .aqua)
    let host = NSHostingView(rootView: view.frame(width: size.width, height: size.height))
    host.frame = NSRect(origin: .zero, size: size)
    window.contentView = host
    window.setFrameOrigin(NSPoint(x: -20000, y: -20000))
    window.orderBack(nil)   // fuera de cualquier pantalla y detrás de todo; sin activar la app
    spin(1.2)
    host.layoutSubtreeIfNeeded()
    guard let rep = host.bitmapImageRepForCachingDisplay(in: host.bounds) else { fputs("sin bitmap\n", stderr); return }
    host.cacheDisplay(in: host.bounds, to: rep)
    try? rep.representation(using: .png, properties: [:])?.write(to: url)
    window.orderOut(nil)
}

@MainActor func run() throws {
    let size = CGSize(width: 1120, height: 740)
    for dark in [false, true] {
        let tag = dark ? "oscuro" : "claro"
        // Paso del recorrido (el de «Texto», para ver una burbuja con animación sobre el panel).
        var studio = try makeStudio(dark: dark)
        studio.tourIndex = 2
        snapshot(PolarRootView(studio: studio), size: size, dark: dark, to: outDir.appendingPathComponent("recorrido-paso3-\(tag).png"))
        studio.tourIndex = 4
        snapshot(PolarRootView(studio: studio), size: size, dark: dark, to: outDir.appendingPathComponent("recorrido-paso5-imprimir-\(tag).png"))
        // Modo «?» con una burbuja abierta.
        studio = try makeStudio(dark: dark)
        studio.helpMode = true; studio.helpSelected = "top.imprimir"
        snapshot(PolarRootView(studio: studio), size: size, dark: dark, to: outDir.appendingPathComponent("modo-interrogacion-imprimir-\(tag).png"))
        studio.helpSelected = "tool.texto"
        snapshot(PolarRootView(studio: studio), size: size, dark: dark, to: outDir.appendingPathComponent("modo-interrogacion-texto-\(tag).png"))
        // Centro de ayuda: lista y artículo.
        studio = try makeStudio(dark: dark)
        snapshot(HelpCenterView(studio: studio), size: CGSize(width: 860, height: 590), dark: dark, to: outDir.appendingPathComponent("ayuda-lista-\(tag).png"))
        studio.helpArticleID = "encuadrar"
        if HelpContent.shared?.article("encuadrar") == nil { studio.helpArticleID = HelpContent.shared?.articulos.first?.id }
        snapshot(HelpCenterView(studio: studio), size: CGSize(width: 860, height: 590), dark: dark, to: outDir.appendingPathComponent("ayuda-articulo-\(tag).png"))
    }
    // Todas las animaciones en su cuadro final y a mitad (para revisarlas).
    let ids = ["tap", "swipe", "fill", "crop-ring", "filter-swap", "text-type", "print", "qr", "mold-detect", "bg-remove"]
    for dark in [false, true] {
        let grid = VStack(spacing: 8) {
            ForEach(0..<5, id: \.self) { row in
                HStack(spacing: 8) {
                    ForEach(0..<2, id: \.self) { col in
                        let id = ids[row * 2 + col]
                        HStack(spacing: 4) { HelpAnimationView(id: id, frozenAt: 0.5); HelpAnimationView(id: id, frozenAt: 1) }
                    }
                }
            }
        }.padding(12).background(polarCream)
        snapshot(grid, size: CGSize(width: 1000, height: 560), dark: dark, to: outDir.appendingPathComponent("animaciones-\(dark ? "oscuro" : "claro").png"))
    }
}
try MainActor.assumeIsolated { try run() }
