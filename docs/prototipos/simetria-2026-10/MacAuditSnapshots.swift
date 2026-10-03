import SwiftUI
import AppKit

/** Host de captura: componentes reales, datos aislados y ninguna sesión de Polar cerrada. */
@main @MainActor enum MacAuditSnapshots {
    static func main() throws {
        let root = URL(fileURLWithPath: CommandLine.arguments[1])
        let output = URL(fileURLWithPath: CommandLine.arguments[2])
        let scale = CommandLine.arguments[3]
        let app = NSApplication.shared
        app.setActivationPolicy(.prohibited)
        FontCatalog.registerFonts()
        let storage = URL(fileURLWithPath: "/private/tmp/polar-mac-audit-\(scale)")
        let studio = Studio(storageRoot: storage)
        studio.preferences.onboardingSeen = true
        studio.project.name = "Nuestro viaje de aniversario a la playa"
        studio.project.settings.columns = 2; studio.project.settings.rows = 2
        studio.project.photos = PhotoImporter.read(urls: [root.appendingPathComponent("Fotitos/Mejoradas/pareja nueva 1.jpg")]).photos
        guard let asset = studio.project.photos.first else { fatalError("Falta la foto local de auditoría") }
        studio.project.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 5)
        studio.project.normalized(); studio.isDirty = true
        guard studio.flush() else { fatalError("No se pudo preparar el proyecto aislado") }
        studio.previewImage = PolarRenderer.preview(project: studio.project, page: 0)
        studio.selectedSlot = 0
        let window = NSWindow(contentRect: NSRect(x: -10000, y: -10000, width: 1280, height: 800), styleMask: [.borderless], backing: .buffered, defer: false)
        window.isReleasedWhenClosed = false
        window.orderFront(nil)
        let screens = ["inicio", "catalogo", "editor", "fotos", "diseno", "texto", "papel", "encuadre", "terminar", "ajustes", "bienvenida"]
        for size in [(1280, 800), (1728, 1117)] {
            for dark in [false, true] {
                studio.preferences.theme = dark ? .dark : .light
                app.appearance = NSAppearance(named: dark ? .darkAqua : .aqua)
                for screen in screens {
                    studio.inspectorTab = screen == "texto" ? 1 : screen == "papel" ? 2 : ["fotos", "encuadre"].contains(screen) ? 3 : 0
                    let content: AnyView
                    switch screen {
                    case "inicio": content = AnyView(LibraryView(studio: studio))
                    case "terminar": content = AnyView(FinishView(studio: studio))
                    case "ajustes": content = AnyView(PreferencesView(studio: studio))
                    case "bienvenida": content = AnyView(WelcomeView(studio: studio))
                    default: content = AnyView(StudioView(studio: studio))
                    }
                    let width = CGFloat(size.0), height = CGFloat(size.1)
                    let font = CGFloat(Double(scale)! / 100 * 13)
                    let host = NSHostingView(rootView: content.font(.system(size: font)).frame(width: width, height: height).background(polarCream)
                        .preferredColorScheme(dark ? .dark : .light))
                    host.frame = NSRect(x: 0, y: 0, width: width, height: height)
                    window.contentView = host
                    window.setContentSize(NSSize(width: width, height: height))
                    RunLoop.main.run(until: Date().addingTimeInterval(0.35))
                    host.layoutSubtreeIfNeeded(); host.displayIfNeeded()
                    guard let rep = host.bitmapImageRepForCachingDisplay(in: host.bounds) else { fatalError("No se pudo capturar \(screen)") }
                    host.cacheDisplay(in: host.bounds, to: rep)
                    guard let png = rep.representation(using: .png, properties: [:]) else { fatalError("PNG vacío") }
                    let name = "antes-mac-\(size.0)x\(size.1)-\(dark ? "oscuro" : "claro")-\(scale)-\(screen).png"
                    try png.write(to: output.appendingPathComponent(name))
                    print(name)
                }
            }
        }
        window.orderOut(nil)
    }
}
