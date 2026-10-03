#!/usr/bin/env python3
"""Repite la recaptura final de 96 PNG de las vistas reales de PolarMac.

Desde la raíz del repositorio:
    python3 docs/prototipos/simetria-2026-10/capturar-mac-despues.py

Requiere macOS, xcrun/swiftc y la foto local (no incluida):
    Fotitos/Mejoradas/pareja nueva 1.jpg

Escribe docs/capturas/simetria/despues. Compila fuentes y biblioteca aisladas
bajo un directorio temporal, que elimina al terminar. El 130 % escala sólo
las fuentes de las copias de UI; renderer y contenedores conservan su tamaño.
Es el host de la recaptura final, incluida window.appearance para los pickers.
"""
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
HOST_SOURCE = r'''
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
        let storage = URL(fileURLWithPath: CommandLine.arguments[4])
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
        let screens = ["inicio", "catalogo", "editor", "fotos", "filtros", "diseno", "texto", "papel", "encuadre", "terminar", "ajustes", "bienvenida"]
        for size in [(1280, 800), (1728, 1117)] {
            for dark in [false, true] {
                studio.preferences.theme = dark ? .dark : .light
                app.appearance = NSAppearance(named: dark ? .darkAqua : .aqua)
                window.appearance = app.appearance
                for screen in screens {
                    studio.inspectorTab = screen == "texto" ? 1 : screen == "papel" ? 2 : screen == "filtros" ? 4 : screen == "fotos" ? 3 : 0
                    let content: AnyView
                    switch screen {
                    case "inicio": content = AnyView(LibraryView(studio: studio))
                    case "encuadre": content = AnyView(CropView(studio: studio))
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
                    // Las pruebas de impresión se renderizan en segundo plano, con caché temporal fría.
                    RunLoop.main.run(until: Date().addingTimeInterval(screen == "terminar" ? 3.0 : 0.8))
                    host.layoutSubtreeIfNeeded(); host.displayIfNeeded()
                    guard let rep = host.bitmapImageRepForCachingDisplay(in: host.bounds) else { fatalError("No se pudo capturar \(screen)") }
                    host.cacheDisplay(in: host.bounds, to: rep)
                    guard let png = rep.representation(using: .png, properties: [:]) else { fatalError("PNG vacío") }
                    let name = "despues-mac-\(size.0)x\(size.1)-\(dark ? "oscuro" : "claro")-\(scale)-\(screen).png"
                    try png.write(to: output.appendingPathComponent(name))
                    print(name)
                }
            }
        }
        window.orderOut(nil)
    }
}
'''

with tempfile.TemporaryDirectory(prefix="polar-mac-after-") as directory:
    HERE = Path(directory)
    (HERE / "MacAuditSnapshots.swift").write_text(HOST_SOURCE)
    OUTPUT = ROOT / "docs/capturas/simetria/despues"
    OUTPUT.mkdir(parents=True, exist_ok=True)
    SOURCES = ROOT / "PolarMac/Sources"
    common = [SOURCES / (name + ".swift") for name in ["Models", "PhotoLook", "PhotoImporter", "TemplateImport", "Renderer", "Studio", "LibraryStore", "FontCatalog"]]
    named = {"largeTitle": 26, "title": 22, "title2": 17, "title3": 15, "headline": 13, "body": 13, "callout": 12, "subheadline": 11, "footnote": 10, "caption": 10, "caption2": 10}
    for percent in [100, 130]:
        temp = HERE / f"source-{percent}"
        temp.mkdir(exist_ok=True)
        views = []
        for filename in ["PolarApp.swift", "LibraryViews.swift", "LookViews.swift", "DesignCatalogTile.swift"]:
            text = (SOURCES / filename).read_text()
            if filename == "PolarApp.swift":
                text = text[:text.index("@main @MainActor struct PolarApp")] + text[text.index("@MainActor struct StudioView"):]
            if percent == 130:
                # Sólo las copias del host: escala tamaños de UI explícitos y estilos semánticos.
                text = re.sub(r"(size:\s*)([0-9]+(?:\.[0-9]+)?)", lambda m: m[1] + str(float(m[2]) * 1.3), text)
                text = re.sub(r"\.font\(\.(largeTitle|title2|title3|title|headline|body|callout|subheadline|footnote|caption2|caption)(\.bold\(\))?\)", lambda m: ".font(.system(size: " + str(named[m[1]] * 1.3) + (", weight: .bold" if m[2] or m[1] == "headline" else "") + "))", text)
            dest = temp / filename
            dest.write_text(text)
            views.append(dest)
        executable = temp / "capture"
        subprocess.run(["xcrun", "swiftc", "-swift-version", "5", "-module-cache-path", str(HERE / "module-cache"), *map(str, common + views), str(HERE / "MacAuditSnapshots.swift"), "-o", str(executable)], check=True)
        subprocess.run([str(executable), str(ROOT), str(OUTPUT), str(percent), str(HERE / f"library-{percent}")], check=True)
