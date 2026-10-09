import SwiftUI
import AppKit

// MARK: Objetivos de la guía

/// Cada control que la guía puede señalar publica su rectángulo (id → ancla). El recorrido y el modo «?» los comparten:
/// un control que no está en pantalla simplemente no aparece aquí, y el paso se salta.
struct HelpTargetKey: PreferenceKey {
    static var defaultValue: [String: Anchor<CGRect>] { [:] }
    static func reduce(value: inout [String: Anchor<CGRect>], nextValue: () -> [String: Anchor<CGRect>]) { value.merge(nextValue()) { $1 } }
}

extension View {
    func helpTarget(_ id: String) -> some View {
        anchorPreference(key: HelpTargetKey.self, value: .bounds) { [id: $0] }
    }
}

// MARK: Capa sobre el editor

/// Se coloca con `overlayPreferenceValue` sobre el editor. Sin recorrido ni modo «?» no pinta ni intercepta nada.
@MainActor struct HelpLayer: View {
    @ObservedObject var studio: Studio
    let anchors: [String: Anchor<CGRect>]
    let proxy: GeometryProxy

    static func visible(_ rects: [String: CGRect], in size: CGSize) -> [String: CGRect] {
        let window = CGRect(origin: .zero, size: size)
        return rects.filter { $0.value.width > 1 && $0.value.height > 1 && window.intersects($0.value) }
            .mapValues { $0.intersection(window) }
    }
    var body: some View {
        let rects = Self.visible(anchors.mapValues { proxy[$0] }, in: proxy.size)
        if let content = HelpContent.shared {
            if studio.tourIndex != nil { TourOverlay(studio: studio, content: content, rects: rects, size: proxy.size) }
            else if studio.helpMode { HelpModeOverlay(studio: studio, content: content, rects: rects, size: proxy.size) }
        } else if studio.tourIndex != nil || studio.helpMode {
            Color.clear.onAppear { studio.tourIndex = nil; studio.helpMode = false }
        }
    }
}

/// Coloca la burbuja junto al objetivo sin salirse de la ventana (ver HelpLayout).
struct BubbleLayout: Layout {
    var target: CGRect
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize { proposal.replacingUnspecifiedDimensions() }
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard let view = subviews.first else { return }
        let size = view.sizeThatFits(ProposedViewSize(width: HelpLayout.bubbleWidth, height: nil))
        let origin = HelpLayout.bubbleOrigin(target: target, bubble: size, container: bounds.size)
        view.place(at: CGPoint(x: bounds.minX + origin.x, y: bounds.minY + origin.y), anchor: .topLeading, proposal: ProposedViewSize(size))
    }
}

struct HelpBubble<Footer: View>: View {
    static var width: CGFloat { HelpLayout.bubbleWidth }
    let title: String
    let text: String
    var caption: String?
    var animation: String?
    @ViewBuilder var footer: () -> Footer
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            if let animation { HelpAnimationView(id: animation).frame(height: 96).frame(maxWidth: .infinity) }
            if let caption { Text(caption).font(.caption).foregroundStyle(.secondary) }
            Text(title).font(.headline).foregroundStyle(polarInk).fixedSize(horizontal: false, vertical: true)
            Text(text).font(.callout).fixedSize(horizontal: false, vertical: true)
            footer().padding(.top, Spacing.xs)
        }
        .padding(Spacing.m)
        .frame(width: Self.width, alignment: .leading)
        .background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
        .overlay(RoundedRectangle(cornerRadius: PolarRadius.card).stroke(polarInk.opacity(0.45), lineWidth: 1))
        .shadow(color: .black.opacity(0.25), radius: 14, y: 4)
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isModal)
    }
}

// MARK: Recorrido inicial

@MainActor struct TourOverlay: View {
    @ObservedObject var studio: Studio
    let content: HelpContent
    let rects: [String: CGRect]
    let size: CGSize

    private var visible: Set<String> { Set(rects.keys) }
    private var current: Int? { studio.tourIndex.flatMap { HelpTour.next(steps: content.recorrido, from: $0, visible: visible) } }

    var body: some View {
        ZStack(alignment: .topLeading) {
            if let index = current, let rect = rects[content.recorrido[index].objetivo] {
                let step = content.recorrido[index]
                let hole = rect.insetBy(dx: -6, dy: -6)
                let place = HelpTour.position(steps: content.recorrido, at: index, visible: visible)
                let isLast = HelpTour.next(steps: content.recorrido, from: index + 1, visible: visible) == nil
                Canvas { ctx, canvasSize in
                    var path = Path(CGRect(origin: .zero, size: canvasSize))
                    path.addRoundedRect(in: hole, cornerSize: CGSize(width: 10, height: 10))
                    ctx.fill(path, with: .color(.black.opacity(0.58)), style: FillStyle(eoFill: true))
                    ctx.stroke(Path(roundedRect: hole, cornerRadius: 10), with: .color(polarInk), lineWidth: 2.5)
                }
                .contentShape(Rectangle()).onTapGesture {}   // el fondo atenuado no deja pasar clics
                .accessibilityHidden(true)
                BubbleLayout(target: hole) {
                    HelpBubble(title: step.titulo, text: step.shownText, caption: "Paso \(place.n) de \(place.total)", animation: step.animacion) {
                        HStack {
                            Button("Saltar") { studio.endTour(seen: true) }.keyboardShortcut(.cancelAction)
                                .accessibilityHint("Cierra el recorrido; no volverá a salir solo")
                            Spacer()
                            Button(isLast ? "Terminar" : "Siguiente") {
                                if isLast { studio.endTour(seen: true) } else { studio.tourIndex = index + 1 }
                            }.buttonStyle(PolarButtonStyle(primary: true)).keyboardShortcut(.defaultAction)
                        }
                    }
                }
                .frame(width: size.width, height: size.height)
                .task(id: index) {
                    AccessibilityNotification.Announcement("Paso \(place.n) de \(place.total). \(step.titulo). \(step.shownText)").post()
                }
            } else {
                Color.clear.frame(width: 1, height: 1)
            }
        }
        .frame(width: size.width, height: size.height, alignment: .topLeading)
        // Sin pasos visibles el recorrido termina solo; se espera un instante a que el editor termine de pintarse.
        .task(id: "\(studio.tourIndex ?? -1)-\(visible.sorted())") {
            guard let i = studio.tourIndex, current == nil else { return }
            try? await Task.sleep(nanoseconds: 350_000_000)
            if !Task.isCancelled, studio.tourIndex == i, current == nil { studio.endTour(seen: i > 0) }
        }
    }
}

// MARK: Modo «?»

/// Mientras está activo, ningún clic llega a los controles: cada uno se cubre con un botón transparente que sólo explica.
@MainActor struct HelpModeOverlay: View {
    @ObservedObject var studio: Studio
    let content: HelpContent
    let rects: [String: CGRect]
    let size: CGSize
    private var selected: String? { studio.helpSelected }

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black.opacity(0.10).contentShape(Rectangle()).onTapGesture { studio.helpSelected = nil }.accessibilityHidden(true)
            // Los grandes debajo y los pequeños encima, para que un clic llegue al control más específico.
            ForEach(rects.keys.sorted { area($0) > area($1) }, id: \.self) { id in
                if let rect = rects[id], let control = content.control(id) {
                    Button { select(id) } label: {
                        RoundedRectangle(cornerRadius: 6)
                            .strokeBorder(polarInk.opacity(selected == id ? 1 : 0.6), style: StrokeStyle(lineWidth: selected == id ? 2.5 : 1.5, dash: selected == id ? [] : [5, 3]))
                            .background(selected == id ? polarInk.opacity(0.10) : .clear, in: RoundedRectangle(cornerRadius: 6))
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .frame(width: rect.width, height: rect.height)
                    .offset(x: rect.minX, y: rect.minY)
                    .accessibilityLabel(control.titulo)
                    .accessibilityHint("Explica qué hace; no lo activa")
                }
            }
            if let id = selected, let rect = rects[id], let control = content.control(id) {
                BubbleLayout(target: rect) {
                    HelpBubble(title: control.titulo, text: control.shownText) {
                        Button("Entendido") { studio.helpSelected = nil }.keyboardShortcut(.defaultAction)
                    }
                }
                .frame(width: size.width, height: size.height)
                .transition(.opacity)
            }
            VStack {
                Spacer()
                HStack(spacing: Spacing.m) {
                    Image(systemName: "questionmark.circle.fill").foregroundStyle(polarInk)
                    Text("Haz clic en un botón para saber qué hace. No se activa.").font(.callout)
                    Button("Listo") { studio.helpMode = false }.buttonStyle(PolarButtonStyle(primary: true)).keyboardShortcut(.cancelAction)
                }
                .padding(.horizontal, Spacing.m).padding(.vertical, Spacing.s)
                .background(polarSurface, in: Capsule()).overlay(Capsule().stroke(polarInk.opacity(0.45)))
                .shadow(color: .black.opacity(0.2), radius: 8, y: 2)
                .padding(.bottom, 48)
            }
            .frame(width: size.width, height: size.height)
            .accessibilityElement(children: .contain)
        }
        .frame(width: size.width, height: size.height, alignment: .topLeading)
        .onAppear { AccessibilityNotification.Announcement("Modo de ayuda. Elige un botón para saber qué hace. Escape para salir.").post() }
        .onChange(of: studio.helpSelected) { _, id in
            if let id, let control = content.control(id) { AccessibilityNotification.Announcement("\(control.titulo). \(control.shownText)").post() }
        }
    }
    private func area(_ id: String) -> CGFloat { rects[id].map { $0.width * $0.height } ?? 0 }
    private func select(_ id: String) { studio.helpSelected = (selected == id) ? nil : id }
}

// MARK: Centro de ayuda

@MainActor struct HelpCenterView: View {
    @ObservedObject var studio: Studio
    @NativeState<String> private var query = ""
    @NativeState<String?> private var category: String? = nil
    private let content = HelpContent.shared
    private var results: [HelpArticle] { HelpSearch.filter(content?.articulos ?? [], query: query, categoria: category) }
    private var selected: HelpArticle? { studio.helpArticleID.flatMap { content?.article($0) } }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: Spacing.m) {
                Text("Ayuda").font(.title.bold())
                Spacer()
                Button { studio.startTour() } label: { Label("Ver el recorrido", systemImage: "play.circle") }
                Button { studio.toggleHelpMode() } label: { Label("¿Qué hace cada botón?", systemImage: "questionmark.circle") }
                Button("Cerrar") { studio.showingHelp = false }.keyboardShortcut(.cancelAction).buttonStyle(PolarButtonStyle(primary: true))
            }.padding(Spacing.m)
            Divider()
            if let content {
                HStack(spacing: 0) {
                    list(content).frame(width: 320)
                    Divider()
                    detail(content).frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            } else {
                ContentUnavailableView("La guía no está disponible", systemImage: "questionmark.circle", description: Text("No se encontró el contenido de ayuda en esta instalación."))
            }
        }
        .frame(width: 860, height: 590).background(polarCream).tint(polarInk).buttonStyle(PolarButtonStyle())
        .preferredColorScheme(studio.preferences.theme == .system ? nil : studio.preferences.theme == .dark ? .dark : .light)
    }

    private func list(_ content: HelpContent) -> some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            TextField("Buscar en la ayuda", text: $query).textFieldStyle(.roundedBorder).accessibilityLabel("Buscar en la ayuda")
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 96), spacing: 6)], alignment: .leading, spacing: 6) {
                chip("Todas", id: nil)
                ForEach(content.categorias, id: \.id) { chip($0.titulo, id: $0.id) }
            }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 4) {
                    ForEach(results) { article in
                        Button { studio.helpArticleID = article.id } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(article.titulo).font(.system(size: 13, weight: .semibold)).foregroundStyle(.primary)
                                Text(article.resumen).font(.system(size: 11)).foregroundStyle(.secondary).lineLimit(2)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading).padding(Spacing.s)
                            .background(studio.helpArticleID == article.id ? polarInk.opacity(0.14) : .clear, in: RoundedRectangle(cornerRadius: PolarRadius.small))
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(article.titulo).accessibilityHint(article.resumen)
                        .accessibilityAddTraits(studio.helpArticleID == article.id ? .isSelected : [])
                    }
                    if results.isEmpty {
                        VStack(spacing: Spacing.xs) {
                            Text("No encontré nada con esa búsqueda").font(.headline)
                            Text("Prueba con otra palabra, por ejemplo «foto», «texto» o «imprimir».").font(.callout).foregroundStyle(.secondary).multilineTextAlignment(.center)
                        }.frame(maxWidth: .infinity).padding(.top, Spacing.l)
                    }
                }
            }
        }.padding(Spacing.m)
    }

    private func chip(_ title: String, id: String?) -> some View {
        Button { category = id } label: {
            Text(title).font(.system(size: 11, weight: .medium)).lineLimit(1).minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity, minHeight: 26)
                .background(category == id ? polarInk : polarSurface, in: Capsule())
                .foregroundStyle(category == id ? polarCream : .primary)
                .overlay(Capsule().stroke(polarInk.opacity(0.35)))
        }.buttonStyle(.plain).accessibilityLabel(title).accessibilityAddTraits(category == id ? .isSelected : [])
    }

    @ViewBuilder private func detail(_ content: HelpContent) -> some View {
        if let article = selected {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.m) {
                    HelpAnimationView(id: article.animacion).frame(height: 190).frame(maxWidth: .infinity)
                    Text(article.titulo).font(.title2.bold())
                    Text(article.resumen).font(.body).foregroundStyle(.secondary)
                    VStack(alignment: .leading, spacing: Spacing.s) {
                        ForEach(Array(article.steps.enumerated()), id: \.offset) { index, paso in
                            HStack(alignment: .firstTextBaseline, spacing: Spacing.s) {
                                Text("\(index + 1)").font(.system(size: 12, weight: .bold)).foregroundStyle(polarCream)
                                    .frame(width: 22, height: 22).background(polarInk, in: Circle()).accessibilityHidden(true)
                                Text(paso).fixedSize(horizontal: false, vertical: true)
                            }.accessibilityElement(children: .combine).accessibilityLabel("Paso \(index + 1). \(paso)")
                        }
                    }
                    if let destino = article.destino {
                        Button { studio.go(to: destino) } label: { Label("Llévame ahí", systemImage: "arrow.right.circle.fill") }
                            .buttonStyle(PolarButtonStyle(primary: true)).accessibilityHint("Cierra la ayuda y abre esta función")
                    }
                }.padding(Spacing.l)
            }
        } else {
            VStack(spacing: Spacing.s) {
                Image(systemName: "text.book.closed").font(.system(size: 44)).foregroundStyle(polarInk)
                Text("Elige un tema").font(.title3.bold())
                Text("Busca a la izquierda o elige una categoría. Cada tema trae una animación y, cuando aplica, un botón «Llévame ahí».")
                    .font(.callout).foregroundStyle(.secondary).multilineTextAlignment(.center).frame(maxWidth: 360)
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
