import Foundation
import CoreGraphics

/// Contenido único de la guía: `shared-fixtures/help.json` (Resources/help.json en la app; POLAR_HELP_JSON en pruebas y capturas).
/// Android lee el mismo archivo. Esta parte no depende de la interfaz para poder comprobarla en `check.sh`.
struct HelpCategory: Decodable, Equatable, Sendable { var id: String; var titulo: String }
struct HelpArticle: Decodable, Equatable, Sendable, Identifiable {
    var id: String, categoria: String, titulo: String, resumen: String
    var pasos: [String]
    /// Variantes por plataforma (opcionales): cada app usa la suya si existe y, si no, `pasos`.
    var pasosMac: [String]?
    var pasosAndroid: [String]?
    var destino: String?
    var animacion: String
    /// Sinónimos para la búsqueda («recortar» encuentra «Encuadrar»).
    var palabras: [String]
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id); categoria = try c.decode(String.self, forKey: .categoria)
        titulo = try c.decode(String.self, forKey: .titulo); resumen = try c.decode(String.self, forKey: .resumen)
        pasos = try c.decode([String].self, forKey: .pasos); destino = try c.decodeIfPresent(String.self, forKey: .destino)
        animacion = try c.decode(String.self, forKey: .animacion)
        pasosMac = try? c.decodeIfPresent([String].self, forKey: .pasosMac)
        pasosAndroid = try? c.decodeIfPresent([String].self, forKey: .pasosAndroid)
        palabras = try c.decodeIfPresent([String].self, forKey: .palabras) ?? []
    }
    enum CodingKeys: String, CodingKey { case id, categoria, titulo, resumen, pasos, pasosMac, pasosAndroid, destino, animacion, palabras }
    /// Los pasos que ve esta app.
    var steps: [String] { pasosMac ?? pasos }
}
struct TourStep: Decodable, Equatable, Sendable, Identifiable { var id: String, objetivo: String, animacion: String, titulo: String, texto: String }
struct HelpControl: Decodable, Equatable, Sendable, Identifiable { var id: String, titulo: String, texto: String }

struct HelpContent: Decodable, Sendable {
    static let version = 1
    var version: Int
    var categorias: [HelpCategory]
    var destinos: [String]
    var animaciones: [String: String]
    var recorrido: [TourStep]
    var controles: [HelpControl]
    var articulos: [HelpArticle]

    func control(_ id: String) -> HelpControl? { controles.first { $0.id == id } }
    func article(_ id: String) -> HelpArticle? { articulos.first { $0.id == id } }

    /// Problemas del esquema, en español; vacío = el contenido es válido.
    func problems() -> [String] {
        var out: [String] = []
        if version != Self.version { out.append("versión \(version) no soportada") }
        let cats = Set(categorias.map(\.id))
        for (id, n) in Dictionary(grouping: articulos, by: \.id).mapValues(\.count) where n > 1 { out.append("id repetido: \(id)") }
        for (id, n) in Dictionary(grouping: controles, by: \.id).mapValues(\.count) where n > 1 { out.append("id repetido (control): \(id)") }
        for a in articulos {
            if !cats.contains(a.categoria) { out.append("\(a.id): categoría desconocida «\(a.categoria)»") }
            if let d = a.destino, !destinos.contains(d) { out.append("\(a.id): destino desconocido «\(d)»") }
            if animaciones[a.animacion] == nil { out.append("\(a.id): animación desconocida «\(a.animacion)»") }
            if a.steps.isEmpty || a.pasos.isEmpty || (a.pasos + (a.pasosMac ?? []) + (a.pasosAndroid ?? [])).contains(where: { $0.trimmingCharacters(in: .whitespaces).isEmpty }) { out.append("\(a.id): pasos vacíos") }
            if a.titulo.trimmingCharacters(in: .whitespaces).isEmpty || a.resumen.trimmingCharacters(in: .whitespaces).isEmpty { out.append("\(a.id): título o resumen vacío") }
        }
        let controlIDs = Set(controles.map(\.id))
        if recorrido.count != 5 { out.append("el recorrido debe tener 5 pasos, tiene \(recorrido.count)") }
        for s in recorrido {
            if animaciones[s.animacion] == nil { out.append("recorrido \(s.id): animación desconocida «\(s.animacion)»") }
            if !controlIDs.contains(s.objetivo) { out.append("recorrido \(s.id): el objetivo «\(s.objetivo)» no es un control") }
        }
        for c in controles where c.titulo.trimmingCharacters(in: .whitespaces).isEmpty || c.texto.trimmingCharacters(in: .whitespaces).isEmpty { out.append("control \(c.id): texto vacío") }
        return out
    }

    static func parse(_ data: Data) -> HelpContent? {
        guard let content = try? JSONDecoder().decode(HelpContent.self, from: data), content.version == version else { return nil }
        return content
    }
    static func locate() -> URL? {
        let environment = ProcessInfo.processInfo.environment["POLAR_HELP_JSON"].map { URL(fileURLWithPath: $0) }
        let bundled = Bundle.main.url(forResource: "help", withExtension: "json")
        return [environment, bundled].compactMap { $0 }.first { FileManager.default.isReadableFile(atPath: $0.path) }
    }
    /// nil si falta el archivo o tiene otra versión: la ayuda se muestra vacía y la app no se cae.
    static let shared: HelpContent? = locate().flatMap { try? Data(contentsOf: $0) }.flatMap(parse)
}

enum HelpSearch {
    /// Minúsculas, sin acentos y con espacios simples. «Canción» y «cancion» son lo mismo.
    static func normalize(_ text: String) -> String {
        text.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "es"))
            .split(whereSeparator: { $0.isWhitespace }).joined(separator: " ")
    }
    /// Cada palabra de la búsqueda debe aparecer en el artículo; primero los que la tienen en el título, luego en sinónimos o resumen.
    static func filter(_ articles: [HelpArticle], query: String, categoria: String?) -> [HelpArticle] {
        let words = normalize(query).split(separator: " ").map(String.init)
        let inCategory = articles.filter { categoria == nil || $0.categoria == categoria }
        if words.isEmpty { return inCategory }
        let ranked: [(Int, Int, HelpArticle)] = inCategory.enumerated().compactMap { index, a in
            let title = normalize(a.titulo)
            let head = title + " " + normalize(a.palabras.joined(separator: " ")) + " " + normalize(a.resumen)
            let all = head + " " + normalize(a.steps.joined(separator: " "))
            guard words.allSatisfy({ all.contains($0) }) else { return nil }
            return (words.allSatisfy { title.contains($0) } ? 0 : words.allSatisfy { head.contains($0) } ? 1 : 2, index, a)
        }
        return ranked.sorted { ($0.0, $0.1) < ($1.0, $1.1) }.map { $0.2 }
    }
}

/// Los controles de la Mac que la guía puede señalar. Cada uno debe tener su explicación en help.json.
enum HelpIds {
    static let all: [String] = [
        "top.back", "top.nombre", "top.deshacer", "top.rehacer", "top.mas", "top.imprimir",
        "tool.fotos", "tool.filtros", "tool.diseno", "tool.texto", "tool.papel",
        "hoja", "hoja.paginas", "ctx", "cta.fotos",
        "panel.fotos", "panel.filtros", "panel.diseno", "panel.texto", "panel.papel"
    ]
    static func panel(forTab tab: Int) -> String {
        switch tab { case 0: return "panel.diseno"; case 1: return "panel.texto"; case 2: return "panel.papel"; case 3: return "panel.fotos"; default: return "panel.filtros" }
    }
}

/// A dónde lleva «Llévame ahí». Los ids son los de `destinos` en help.json; la prueba exige que coincidan.
enum HelpDestination: String, CaseIterable {
    case inicio, catalogo
    case editorFotos = "editor.fotos", editorFiltros = "editor.filtros", editorDiseno = "editor.diseno"
    case editorTexto = "editor.texto", editorPapel = "editor.papel", editorEncuadrar = "editor.encuadrar"
    case terminar, ajustes
    case moldesAsistente = "moldes.asistente", misMoldes = "mis-moldes"
    static let tourID = "recorrido"
    /// Pestaña del inspector del editor que se abre, si corresponde.
    var inspectorTab: Int? {
        switch self {
        case .editorFotos: return 3
        case .editorFiltros: return 4
        case .editorDiseno: return 0
        case .editorTexto: return 1
        case .editorPapel: return 2
        default: return nil
        }
    }
    var needsEditor: Bool { inspectorTab != nil || self == .editorEncuadrar || self == .terminar || self == .catalogo || self == .misMoldes }
}

/// Recorrido inicial: salta los pasos cuyo objetivo no se ve (ventana pequeña, vista distinta) y termina solo si no queda ninguno.
enum HelpTour {
    static func next(steps: [TourStep], from index: Int, visible: Set<String>) -> Int? {
        guard index >= 0 else { return nil }
        return steps.indices.dropFirst(index).first { visible.contains(steps[$0].objetivo) }
    }
    /// Posición («2 de 4») entre los pasos que de verdad se muestran.
    static func position(steps: [TourStep], at index: Int, visible: Set<String>) -> (n: Int, total: Int) {
        let shown = steps.indices.filter { visible.contains(steps[$0].objetivo) || $0 == index }
        return ((shown.firstIndex(of: index) ?? 0) + 1, shown.count)
    }
}

enum HelpLayout {
    static let margin: CGFloat = 12
    static let bubbleWidth: CGFloat = 320
    /// Esquina superior izquierda de la burbuja: debajo del objetivo si cabe, si no arriba, a la derecha, a la izquierda;
    /// al final se ajusta para que nunca salga de la ventana.
    static func bubbleOrigin(target: CGRect, bubble: CGSize, container: CGSize, gap: CGFloat = 12) -> CGPoint {
        let m = margin
        let maxX = max(m, container.width - bubble.width - m), maxY = max(m, container.height - bubble.height - m)
        func clamp(_ p: CGPoint) -> CGPoint { CGPoint(x: min(max(p.x, m), maxX), y: min(max(p.y, m), maxY)) }
        let centeredX = target.midX - bubble.width / 2, centeredY = target.midY - bubble.height / 2
        let below = CGPoint(x: centeredX, y: target.maxY + gap)
        if below.y <= maxY { return clamp(below) }
        let above = CGPoint(x: centeredX, y: target.minY - gap - bubble.height)
        if above.y >= m { return clamp(above) }
        let right = CGPoint(x: target.maxX + gap, y: centeredY)
        if right.x <= maxX { return clamp(right) }
        let left = CGPoint(x: target.minX - gap - bubble.width, y: centeredY)
        if left.x >= m { return clamp(left) }
        return clamp(CGPoint(x: centeredX, y: maxY))   // el objetivo ocupa casi toda la ventana: sobre él, abajo
    }
}
