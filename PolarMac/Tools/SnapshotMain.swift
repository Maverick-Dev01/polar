// Arnés de capturas fuera de pantalla: renderiza las pantallas de Polar con datos demo generados en código.
// No abre la app real, no usa el teclado ni el ratón y no toca ninguna biblioteca existente.
// Uso: PolarMac/Tools/snapshots.sh <carpeta de salida> [filtro de nombre]
import AppKit
import SwiftUI

let outputDirectory = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "/private/tmp/polar-snapshots")
let nameFilter = CommandLine.arguments.count > 2 ? CommandLine.arguments[2] : ""
try? FileManager.default.createDirectory(at: outputDirectory, withIntermediateDirectories: true)

_ = NSApplication.shared
NSApp.setActivationPolicy(.prohibited)

func demoPhoto(_ directory: URL, index: Int, landscape: Bool) -> PhotoAsset {
    let size = landscape ? CGSize(width: 1200, height: 800) : CGSize(width: 800, height: 1100)
    let hues: [CGFloat] = [0.02, 0.08, 0.14, 0.33, 0.45, 0.55, 0.62, 0.75, 0.88]
    let hue = hues[index % hues.count]
    let image = NSImage(size: size, flipped: false) { rect in
        NSGradient(colors: [NSColor(hue: hue, saturation: 0.55, brightness: 0.95, alpha: 1), NSColor(hue: (hue + 0.08).truncatingRemainder(dividingBy: 1), saturation: 0.7, brightness: 0.45, alpha: 1)])!.draw(in: rect, angle: 62)
        NSColor.white.withAlphaComponent(0.85).setFill()
        NSBezierPath(ovalIn: CGRect(x: rect.width * 0.32, y: rect.height * 0.42, width: rect.width * 0.36, height: rect.width * 0.36)).fill()
        NSColor(hue: hue, saturation: 0.5, brightness: 0.35, alpha: 0.9).setFill()
        NSBezierPath(roundedRect: CGRect(x: rect.width * 0.2, y: rect.height * 0.1, width: rect.width * 0.6, height: rect.height * 0.28), xRadius: 90, yRadius: 90).fill()
        return true
    }
    let url = directory.appendingPathComponent("demo-\(index).jpg")
    let rep = NSBitmapImageRep(data: image.tiffRepresentation!)!
    try! rep.representation(using: .jpeg, properties: [.compressionFactor: 0.7])!.write(to: url)
    return PhotoAsset(path: url.path, pixelWidth: Int(size.width), pixelHeight: Int(size.height))
}

let sharedPhotoDirectory = FileManager.default.temporaryDirectory.appendingPathComponent("polar-snapshots-photos")
@MainActor let sharedPhotos: [PhotoAsset] = {
    try? FileManager.default.createDirectory(at: sharedPhotoDirectory, withIntermediateDirectories: true)
    return (0..<9).map { demoPhoto(sharedPhotoDirectory, index: $0, landscape: $0 % 3 == 2) }
}()

@MainActor func makeStudio(root: URL, style: TemplateStyle = .polaroid, photos: Int = 7, seenWelcome: Bool = true) -> Studio {
    lastStudioRoot = root
    let studio = Studio(storageRoot: root)
    studio.preferences.onboardingSeen = seenWelcome
    studio.project = PolarProject()
    studio.project.photos = Array(sharedPhotos.prefix(photos))
    studio.project.name = "Aniversario"
    studio.project.selectStyle(style)
    studio.fillAll()
    studio.showingLibrary = false
    studio.status = "Guardado"
    studio.selectedSlot = 0
    studio.refresh()
    return studio
}

@MainActor func snapshot<V: View>(_ view: V, width: CGFloat, height: CGFloat, dark: Bool, name: String, fitContent: Bool = false) {
    guard nameFilter.isEmpty || name.contains(nameFilter) else { return }
    let host = NSHostingView(rootView: view)
    var size = NSSize(width: width, height: height)
    if fitContent { host.frame = NSRect(x: 0, y: 0, width: width, height: 10); size = host.fittingSize; size.width = max(size.width, width) }
    let window = NSWindow(contentRect: NSRect(origin: .zero, size: size), styleMask: [.borderless], backing: .buffered, defer: false)
    window.appearance = NSAppearance(named: dark ? .darkAqua : .aqua)
    window.contentView = host
    host.frame = NSRect(origin: .zero, size: size)
    // Deja correr tareas asíncronas (miniaturas, vistas previas) antes de capturar.
    for _ in 0..<30 { RunLoop.current.run(until: Date().addingTimeInterval(0.1)); host.layoutSubtreeIfNeeded() }
    host.displayIfNeeded()
    guard let rep = host.bitmapImageRepForCachingDisplay(in: host.bounds) else { return }
    host.cacheDisplay(in: host.bounds, to: rep)
    let file = outputDirectory.appendingPathComponent("\(name)-\(Int(width))x\(Int(height))-\(dark ? "oscuro" : "claro").png")
    try? rep.representation(using: .png, properties: [:])!.write(to: file)
    print("capturada", file.lastPathComponent)
    window.contentView = nil
    // Libera el disco de cada caso: la biblioteca de prueba copia las fotos demo.
    if let studio = lastStudioRoot { try? FileManager.default.removeItem(at: studio) }
}
var lastStudioRoot: URL?

@MainActor func run() {
    let root = FileManager.default.temporaryDirectory.appendingPathComponent("polar-snapshots-\(UUID().uuidString)")
    defer { try? FileManager.default.removeItem(at: root); try? FileManager.default.removeItem(at: sharedPhotoDirectory) }
    for (width, height) in [(CGFloat(1100), CGFloat(720)), (1600, 1000)] {
        for dark in [false, true] {
            // Biblioteca vacía y con diseños.
            let empty = Studio(storageRoot: root.appendingPathComponent("vacia-\(Int(width))-\(dark)"))
            empty.showingLibrary = true
            snapshot(PolarRootView(studio: empty), width: width, height: height, dark: dark, name: "01-biblioteca-vacia")

            let filled = makeStudio(root: root.appendingPathComponent("llena-\(Int(width))-\(dark)"), style: .polaroid)
            for (index, style) in [TemplateStyle.polaroid, .spotify, .filmVertical, .calendar, .instagram].enumerated() {
                filled.project.selectStyle(style); filled.project.name = ["Aniversario", "Playlist de verano", "Viaje a Oaxaca", "Calendario 2027", "Fotos de Sofía"][index]
                _ = try? filled.library.save(UUID(), project: filled.project, thumbnail: PolarRenderer.preview(project: filled.project, page: 0))
            }
            filled.libraryItems = filled.library.list(); filled.showingLibrary = true
            snapshot(PolarRootView(studio: filled), width: width, height: height, dark: dark, name: "02-biblioteca-con-disenos")

            // Editor: una pestaña por captura, con diseño de foto + canción para ver la sección Canción.
            let tabs = [(3, "fotos"), (4, "filtros"), (0, "diseno"), (1, "texto"), (2, "papel")]
            for (tab, label) in tabs {
                let studio = makeStudio(root: root.appendingPathComponent("editor-\(label)-\(Int(width))-\(dark)"), style: tab == 1 ? .spotify : .polaroid)
                studio.inspectorTab = tab
                snapshot(PolarRootView(studio: studio), width: width, height: height, dark: dark, name: "03-editor-\(label)")
            }
            let polaroidText = makeStudio(root: root.appendingPathComponent("texto-polaroid-\(Int(width))-\(dark)"), style: .polaroid)
            polaroidText.inspectorTab = 1; polaroidText.selectedTextRole = .title
            snapshot(PolarRootView(studio: polaroidText), width: width, height: height, dark: dark, name: "03-editor-texto-polaroid")

            let crop = makeStudio(root: root.appendingPathComponent("encuadrar-\(Int(width))-\(dark)"))
            crop.showingCrop = true
            snapshot(PolarRootView(studio: crop), width: width, height: height, dark: dark, name: "04-encuadrar")

            let finish = makeStudio(root: root.appendingPathComponent("terminar-\(Int(width))-\(dark)"))
            finish.showingFinish = true
            snapshot(PolarRootView(studio: finish), width: width, height: height, dark: dark, name: "05-terminar")
        }
    }
    for dark in [false, true] {
        let studio = makeStudio(root: root.appendingPathComponent("ajustes-\(dark)"))
        snapshot(PreferencesView(studio: studio), width: 520, height: 600, dark: dark, name: "06-ajustes", fitContent: true)
        snapshot(WelcomeView(studio: studio), width: 480, height: 500, dark: dark, name: "07-bienvenida", fitContent: true)
    }
}

MainActor.assumeIsolated { run() }
