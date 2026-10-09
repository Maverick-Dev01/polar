import AppKit

/// Guía de uso (subproyecto 4): esquema de help.json, búsqueda, destinos «Llévame ahí», salto de pasos del recorrido,
/// «recorrido ya visto» en settings.json, burbujas dentro de la ventana y etiquetas citadas que existen en la app.
@main @MainActor struct HelpChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }

    static func main() async throws {
        guard let url = HelpContent.locate(), let data = try? Data(contentsOf: url), let content = HelpContent.parse(data) else {
            check(false, "POLAR_HELP_JSON debe apuntar a shared-fixtures/help.json válido"); return
        }
        schema(content, data)
        search(content)
        destinations(content)
        controls(content)
        tour(content)
        try persistence()
        bubbles()
        try labels(content)
        print("HelpChecks: esquema y variantes por plataforma, búsqueda sin acentos y con sinónimos, \(content.destinos.count) destinos con pantalla en la Mac, controles, salto de pasos del recorrido, tourSeen tolerante, burbujas dentro de 1120×740 y etiquetas citadas passed")
    }

    static func schema(_ c: HelpContent, _ data: Data) {
        check(c.problems().isEmpty, "esquema: \(c.problems())")
        check((18...26).contains(c.articulos.count), "unos 20 artículos, hay \(c.articulos.count)")
        check(c.categorias.map(\.id) == ["empezar", "fotos", "texto", "diseno", "imprimir"], "categorías")
        for cat in c.categorias { check(c.articulos.contains { $0.categoria == cat.id }, "categoría vacía \(cat.id)") }
        check(c.recorrido.map(\.objetivo) == ["tool.fotos", "hoja", "tool.texto", "tool.filtros", "top.imprimir"], "orden del recorrido")
        check(HelpContent.parse(Data("{ no es json".utf8)) == nil, "JSON roto no se cae")
        check(HelpContent.parse(Data(#"{"version":99,"categorias":[],"destinos":[],"animaciones":{},"recorrido":[],"controles":[],"articulos":[]}"#.utf8)) == nil, "versión desconocida")
        // Un artículo roto se detecta.
        var broken = c
        broken.articulos.append(HelpArticle.sample(id: c.articulos[0].id, categoria: "nada", destino: "inventado", animacion: "zzz", pasos: []))
        broken.recorrido.removeFirst()
        let text = broken.problems().joined(separator: "\n")
        for needle in ["categoría", "destino", "animación", "pasos", "id repetido", "recorrido"] { check(text.contains(needle), "no detectó «\(needle)»") }
        // Variante por plataforma: pasosMac gana, pasosAndroid se ignora, sin ninguna se usa pasos.
        let a = HelpArticle.sample(id: "x", categoria: "fotos", destino: nil, animacion: "tap", pasos: ["común"], mac: ["sólo Mac"], android: ["sólo Android"])
        check(a.steps == ["sólo Mac"], "pasosMac")
        check(HelpArticle.sample(id: "y", categoria: "fotos", destino: nil, animacion: "tap", pasos: ["común"], android: ["sólo Android"]).steps == ["común"], "sin pasosMac se usa pasos")
        let json = #"{"id":"z","categoria":"fotos","titulo":"T","resumen":"R","pasos":["a"],"pasosMac":["m"],"pasosAndroid":"roto","animacion":"tap"}"#
        let decoded = try? JSONDecoder().decode(HelpArticle.self, from: Data(json.utf8))
        check(decoded?.steps == ["m"] && decoded?.pasosAndroid == nil && decoded?.palabras == [], "decodificación tolerante de variantes")
    }

    static func search(_ c: HelpContent) {
        let all = c.articulos
        check(HelpSearch.filter(all, query: "", categoria: nil).count == all.count, "sin búsqueda: todos")
        check(HelpSearch.filter(all, query: "   ", categoria: nil).count == all.count, "espacios: todos")
        let a = HelpSearch.filter(all, query: "canción", categoria: nil).map(\.id), b = HelpSearch.filter(all, query: "CANCION", categoria: nil).map(\.id)
        check(!a.isEmpty && a == b, "«canción» y «CANCION» dan lo mismo")
        check(HelpSearch.filter(all, query: "recortar", categoria: nil).contains { $0.id.contains("encuadr") || $0.titulo.lowercased().contains("encuadr") }, "«recortar» encuentra Encuadrar por sinónimo")
        check(HelpSearch.filter(all, query: "undo", categoria: nil).first?.id == "deshacer", "«undo» encuentra Deshacer")
        check(HelpSearch.filter(all, query: "zzzxq", categoria: nil).isEmpty, "sin resultados")
        let texto = HelpSearch.filter(all, query: "", categoria: "texto")
        check(!texto.isEmpty && texto.allSatisfy { $0.categoria == "texto" }, "filtro por categoría")
        // Los de título primero.
        let first = HelpSearch.filter(all, query: "filtros", categoria: nil).first
        check(first.map { HelpSearch.normalize($0.titulo).contains("filtro") } == true, "el título coincide primero")
        check(HelpSearch.normalize("  Árbol   Ñandú ") == "arbol nandu", "normalización")
    }

    static func destinations(_ c: HelpContent) {
        let used = Set(c.articulos.compactMap(\.destino))
        check(Set(c.destinos) == Set(HelpDestination.allCases.map(\.rawValue)), "los destinos del JSON y los de la Mac coinciden")
        for id in c.destinos.filter({ $0 != "ajustes" }) {
            check(HelpDestination(rawValue: id) != nil, "destino sin pantalla en Mac: \(id)")
        }
        check(used.isSubset(of: Set(c.destinos)), "un artículo usa un destino no declarado")
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("polar-help-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: root) }
        let studio = Studio(storageRoot: root)
        check(studio.showingLibrary, "se parte de Tus diseños")
        for id in c.destinos where id != "ajustes" {   // «ajustes» abre la ventana de la app; la prueba no tiene ventanas
            let studio = Studio(storageRoot: root.appendingPathComponent(UUID().uuidString))
            check(studio.go(to: id), "«Llévame ahí» no hizo nada: \(id)")
            let target = HelpDestination(rawValue: id)!
            switch target {
            case .inicio: check(studio.showingLibrary, "inicio abre Tus diseños")
            case .moldesAsistente: check(studio.moldWizard != nil, "abre el asistente de moldes")
            case .misMoldes: check(!studio.showingLibrary && studio.designCategory == "Mis moldes", "abre Mis moldes")
            case .catalogo: check(!studio.showingLibrary && studio.designCategory == "Todos", "abre el catálogo")
            case .terminar, .editorEncuadrar: check(!studio.showingLibrary, "\(id) abre el editor (sin fotos, deja un aviso en vez de fallar)"); check(studio.inspectorTab == 3, "\(id) sin fotos lleva a Fotos")
            default: check(!studio.showingLibrary && studio.inspectorTab == target.inspectorTab, "\(id) abre el panel")
            }
        }
        check(studio.go(to: "recorrido") && studio.tourIndex == 0 && !studio.showingLibrary, "«recorrido» repite el recorrido")
        check(!studio.go(to: "no-existe"), "destino desconocido no hace nada")
        // Con un diseño ya guardado se abre el más reciente en vez de crear otro.
        let existing = Studio(storageRoot: root.appendingPathComponent("conDiseno"))
        existing.change { $0.name = "Reciente" }; existing.showLibrary()
        let count = existing.library.list().count
        check(count == 1 && existing.showingLibrary, "hay un diseño guardado")
        check(existing.go(to: "editor.filtros") && existing.project.name == "Reciente" && existing.library.list().count == 1, "abre el diseño más reciente")
    }

    static func controls(_ c: HelpContent) {
        let ids = Set(c.controles.map(\.id))
        for id in HelpIds.all { check(ids.contains(id), "falta la explicación de \(id)") }
        for id in ids { check(HelpIds.all.contains(id), "el control \(id) no está conectado a la Mac") }
        check(Set(HelpIds.all).count == HelpIds.all.count, "ids repetidos")
        for tab in 0...4 { check(ids.contains(HelpIds.panel(forTab: tab)), "panel de la pestaña \(tab)") }
    }

    static func tour(_ c: HelpContent) {
        let steps = c.recorrido
        let all = Set(steps.map(\.objetivo))
        check(HelpTour.next(steps: steps, from: 0, visible: all) == 0, "todo visible: empieza en el paso 1")
        check(HelpTour.position(steps: steps, at: 2, visible: all) == (3, 5), "posición 3 de 5")
        let noTexto = all.subtracting(["tool.texto"])
        check(HelpTour.next(steps: steps, from: 2, visible: noTexto) == 3, "salta el paso cuyo objetivo no se ve")
        check(HelpTour.position(steps: steps, at: 3, visible: noTexto) == (3, 4), "la numeración sólo cuenta los pasos que se muestran")
        check(HelpTour.next(steps: steps, from: 0, visible: ["hoja"]) == 1, "sin el primero, arranca en el segundo")
        check(HelpTour.next(steps: steps, from: 2, visible: ["tool.fotos", "hoja"]) == nil, "sin pasos restantes termina")
        check(HelpTour.next(steps: steps, from: 0, visible: []) == nil, "sin ningún objetivo visible termina solo")
        check(HelpTour.next(steps: steps, from: 5, visible: all) == nil, "después del último termina")
        check(HelpTour.next(steps: steps, from: -1, visible: all) == nil, "índice inválido")
    }

    static func persistence() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("polar-tour-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: root) }
        let studio = Studio(storageRoot: root)
        studio.showingLibrary = false
        studio.showingWelcome = false
        check(!studio.preferences.tourSeen && studio.shouldAutoStartTour, "primera vez: el recorrido sale solo")
        studio.startTour()
        check(studio.tourIndex == 0 && !studio.shouldAutoStartTour, "mientras corre no se vuelve a lanzar")
        studio.endTour(seen: false)
        check(!studio.preferences.tourSeen, "terminar sin mostrar pasos no lo marca como visto")
        studio.startTour(); studio.endTour(seen: true)
        check(studio.preferences.tourSeen && studio.tourIndex == nil, "completar o saltar lo marca como visto")
        check(Studio(storageRoot: root).preferences.tourSeen, "persiste en settings.json")
        let again = Studio(storageRoot: root); again.showingLibrary = false; again.showingWelcome = false
        check(!again.shouldAutoStartTour, "no reaparece solo")
        again.startTour(); check(again.tourIndex == 0, "desde Ayuda se puede repetir")
        // Lectura tolerante.
        let file = root.appendingPathComponent("settings.json")
        try Data(#"{"theme":"dark","onboardingSeen":true}"#.utf8).write(to: file)
        var prefs = LibraryStore(root: root).preferences()
        check(!prefs.tourSeen && prefs.onboardingSeen && prefs.theme == .dark, "sin la clave: no visto y el resto se conserva")
        try Data(#"{"theme":"dark","tourSeen":"quizá"}"#.utf8).write(to: file)
        prefs = LibraryStore(root: root).preferences()
        check(!prefs.tourSeen && prefs.theme == .dark, "valor dañado: no visto y el resto se conserva")
        try Data(#"{"tourSeen":true,"exportQuality":"x"}"#.utf8).write(to: file)
        check(LibraryStore(root: root).preferences().tourSeen, "otro campo dañado no pierde tourSeen")
    }

    static func bubbles() {
        let window = CGSize(width: 1120, height: 740)
        let bounds = CGRect(origin: .zero, size: window).insetBy(dx: HelpLayout.margin - 0.001, dy: HelpLayout.margin - 0.001)
        var targets: [CGRect] = [CGRect(origin: .zero, size: window), CGRect(x: 0, y: 0, width: 80, height: 48), CGRect(x: 1040, y: 0, width: 80, height: 48),
                                 CGRect(x: 0, y: 692, width: 120, height: 48), CGRect(x: 1000, y: 692, width: 120, height: 48), CGRect(x: 760, y: 90, width: 360, height: 560),
                                 CGRect(x: 248, y: 120, width: 512, height: 380), CGRect(x: 440, y: 6, width: 60, height: 40)]
        for x in stride(from: 0.0, through: 1060, by: 140) { for y in stride(from: 0.0, through: 700, by: 120) { targets.append(CGRect(x: x, y: y, width: 60, height: 40)) } }
        for height in [180.0, 300, 420] {
            for target in targets {
                let size = CGSize(width: HelpLayout.bubbleWidth, height: height)
                let origin = HelpLayout.bubbleOrigin(target: target, bubble: size, container: window)
                check(bounds.contains(CGRect(origin: origin, size: size)), "burbuja fuera de la ventana: objetivo \(target) alto \(height) → \(origin)")
            }
        }
        // Prefiere debajo del objetivo si cabe.
        let o = HelpLayout.bubbleOrigin(target: CGRect(x: 500, y: 20, width: 100, height: 40), bubble: CGSize(width: 320, height: 200), container: window)
        check(o.y >= 60, "debajo del objetivo")
    }

    /// Toda «etiqueta» de los pasos que ve la Mac (pasosMac si existe, si no pasos) debe existir como literal en Sources/*.swift.
    static func labels(_ c: HelpContent) throws {
        let dir = URL(fileURLWithPath: ProcessInfo.processInfo.environment["POLAR_MAC_SOURCES"] ?? "Sources")
        let files = try FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil).filter { $0.pathExtension == "swift" }
        let source = try files.filter { $0.lastPathComponent != "HelpContent.swift" }.map { try String(contentsOf: $0, encoding: .utf8) }.joined(separator: "\n")
        // Una «N» suelta en la etiqueta vale cualquier número: en el código es una interpolación \(…).
        let interpolation = try NSRegularExpression(pattern: #"\\\([^)]*\)"#)
        let flat = interpolation.stringByReplacingMatches(in: source, range: NSRange(source.startIndex..., in: source), withTemplate: "N")
        let haystack = HelpSearch.normalize(flat.replacingOccurrences(of: "\u{2026}", with: "").replacingOccurrences(of: "...", with: ""))
        var missing: [String] = []
        let pattern = try NSRegularExpression(pattern: "«([^»]+)»")
        let texts: [(String, String)] = c.articulos.flatMap { a in a.steps.map { (a.id, $0) } }
            + c.recorrido.map { ("recorrido \($0.id)", $0.shownText) } + c.controles.map { ("control \($0.id)", $0.shownText) }
        for (owner, step) in texts {
            do {
                for match in pattern.matches(in: step, range: NSRange(step.startIndex..., in: step)) {
                    guard let range = Range(match.range(at: 1), in: step) else { continue }
                    let label = HelpSearch.normalize(String(step[range]).replacingOccurrences(of: "\u{2026}", with: "").replacingOccurrences(of: "...", with: ""))
                    if !label.isEmpty, !haystack.contains(label) { missing.append("\(owner): «\(step[range])»") }
                }
            }
        }
        if ProcessInfo.processInfo.environment["POLAR_HELP_LABELS"] == "warn" {
            if !missing.isEmpty { print("AVISO (POLAR_HELP_LABELS=warn): etiquetas de los pasos que aún no existen en la Mac:\n  " + missing.joined(separator: "\n  ")) }
            return
        }
        check(missing.isEmpty, "etiquetas de los pasos que no existen en la Mac:\n  " + missing.joined(separator: "\n  "))
    }
}

extension HelpArticle {
    static func sample(id: String, categoria: String, destino: String?, animacion: String, pasos: [String], mac: [String]? = nil, android: [String]? = nil) -> HelpArticle {
        var object: [String: Any] = ["id": id, "categoria": categoria, "titulo": "T", "resumen": "R", "pasos": pasos, "animacion": animacion]
        if let destino { object["destino"] = destino }
        if let mac { object["pasosMac"] = mac }
        if let android { object["pasosAndroid"] = android }
        return try! JSONDecoder().decode(HelpArticle.self, from: JSONSerialization.data(withJSONObject: object))
    }
}
