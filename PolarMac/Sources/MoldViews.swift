import SwiftUI
import AppKit

/// Imágenes de molde ya decodificadas (la hoja del asistente se redibuja al arrastrar).
enum MoldImageCache {
    private static let cache = NSCache<NSURL, NSImage>()
    static func image(_ url: URL) -> NSImage? {
        if let cached = cache.object(forKey: url as NSURL) { return cached }
        guard let image = NSImage(contentsOf: url) else { return nil }
        cache.setObject(image, forKey: url as NSURL)
        return image
    }
}

/// Contorno de un espacio según su forma (rectángulo, redondeado u óvalo).
struct MoldShape: Shape {
    var shape: RegionShape
    var radius: Double
    func path(in rect: CGRect) -> Path {
        Path(PolarRenderer.shapePath(rect, shape: shape, shapeRadius: CGFloat(radius)))
    }
}

private struct Checkerboard: View {
    var body: some View {
        Canvas { context, size in
            let cell: CGFloat = 12
            for row in 0..<Int(ceil(size.height / cell)) { for column in 0..<Int(ceil(size.width / cell)) where (row + column) % 2 == 0 {
                context.fill(Path(CGRect(x: CGFloat(column) * cell, y: CGFloat(row) * cell, width: cell, height: cell)), with: .color(.gray.opacity(0.16)))
            } }
        }.background(Color.white.opacity(0.9))
    }
}

/// Imagen del molde con sus espacios numerados encima: arrastrar mueve, las asas de las 4 esquinas cambian el tamaño.
/// Cada espacio expone acciones de accesibilidad para mover y redimensionar sin arrastrar.
struct MoldRegionEditor: View {
    @Binding var state: MoldWizardState
    var interactive = true
    @NativeState<CGSize> private var lastMove = CGSize.zero
    @NativeState<CGSize> private var lastResize = CGSize.zero
    private let step = 0.02

    private func fit(_ ratio: CGFloat, in size: CGSize) -> CGRect {
        let width = min(size.width, size.height * ratio), height = width / ratio
        return CGRect(x: (size.width - width) / 2, y: (size.height - height) / 2, width: width, height: height)
    }
    private func handlePoint(_ rect: CGRect, _ corner: MoldWizardState.Corner) -> CGPoint {
        switch corner {
        case .topLeading: return CGPoint(x: rect.minX, y: rect.minY)
        case .topTrailing: return CGPoint(x: rect.maxX, y: rect.minY)
        case .bottomLeading: return CGPoint(x: rect.minX, y: rect.maxY)
        case .bottomTrailing: return CGPoint(x: rect.maxX, y: rect.maxY)
        }
    }

    var body: some View {
        GeometryReader { geo in
            let ratio = CGFloat(state.template?.pixelWidth ?? 3) / CGFloat(max(1, state.template?.pixelHeight ?? 4))
            let frame = fit(ratio, in: geo.size)
            ZStack(alignment: .topLeading) {
                Checkerboard().frame(width: frame.width, height: frame.height)
                if let url = state.imageURL, let image = MoldImageCache.image(url) {
                    Image(nsImage: image).resizable().interpolation(.high).frame(width: frame.width, height: frame.height)
                }
                ForEach(Array(state.regions.enumerated()), id: \.element.id) { index, region in
                    let rect = CGRect(x: region.x * frame.width, y: region.y * frame.height, width: region.width * frame.width, height: region.height * frame.height)
                    regionBody(index: index, region: region, rect: rect, frame: frame)
                }
                if interactive, state.regions.indices.contains(state.selected) {
                    let region = state.regions[state.selected]
                    let rect = CGRect(x: region.x * frame.width, y: region.y * frame.height, width: region.width * frame.width, height: region.height * frame.height)
                    ForEach(MoldWizardState.Corner.allCases, id: \.self) { corner in
                        handle(corner: corner, rect: rect, frame: frame)
                    }
                }
            }
            .frame(width: frame.width, height: frame.height)
            .clipped()
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
        }
    }

    private func regionVisual(index: Int, region: TemplateRegion, selected: Bool) -> some View {
        let fillOpacity: Double = selected ? 0.30 : 0.16
        let lineWidth: CGFloat = selected ? 2.5 : 1.5
        let dash: [CGFloat] = selected ? [] : [5, 3]
        let outline = StrokeStyle(lineWidth: lineWidth, dash: dash)
        return ZStack {
            MoldShape(shape: region.shape, radius: region.radius).fill(polarInk.opacity(fillOpacity))
            MoldShape(shape: region.shape, radius: region.radius).stroke(polarInk, style: outline)
            Text("\(index + 1)").font(.system(size: 13, weight: .bold)).monospacedDigit()
                .padding(.horizontal, 7).padding(.vertical, 2).foregroundStyle(polarCream).background(polarInk, in: Capsule())
        }
    }

    private func moveGesture(_ index: Int, _ frame: CGRect) -> some Gesture {
        DragGesture(minimumDistance: 2).onChanged { value in
            let dx = (value.translation.width - lastMove.width) / frame.width
            let dy = (value.translation.height - lastMove.height) / frame.height
            state.select(index)
            state.moveRegion(index, dx: Double(dx), dy: Double(dy))
            lastMove = value.translation
        }.onEnded { _ in lastMove = .zero }
    }

    private func percent(_ value: Double) -> Int { Int((value * 100).rounded()) }

    private func regionBody(index: Int, region: TemplateRegion, rect: CGRect, frame: CGRect) -> some View {
        let selected = index == state.selected
        let position = "Izquierda \(percent(region.x)) %, arriba \(percent(region.y)) %, ancho \(percent(region.width)) %, alto \(percent(region.height)) %"
        return regionVisual(index: index, region: region, selected: selected)
        .frame(width: max(8, rect.width), height: max(8, rect.height))
        .contentShape(Rectangle())
        .offset(x: rect.minX, y: rect.minY)
        .gesture(moveGesture(index, frame), including: interactive ? .all : .subviews)
        .onTapGesture { if interactive { state.select(index) } }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Espacio \(index + 1) de \(state.regions.count), \(region.shape.name)")
        .accessibilityValue(position)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
        .accessibilityAction(.default) { state.select(index) }
        .accessibilityAction(named: "Mover a la izquierda") { state.moveRegion(index, dx: -step, dy: 0) }
        .accessibilityAction(named: "Mover a la derecha") { state.moveRegion(index, dx: step, dy: 0) }
        .accessibilityAction(named: "Mover hacia arriba") { state.moveRegion(index, dx: 0, dy: -step) }
        .accessibilityAction(named: "Mover hacia abajo") { state.moveRegion(index, dx: 0, dy: step) }
        .accessibilityAction(named: "Agrandar") { state.scaleRegion(index, factor: 1.1) }
        .accessibilityAction(named: "Reducir") { state.scaleRegion(index, factor: 0.9) }
        .accessibilityAction(named: "Quitar espacio") { state.select(index); state.removeSelected() }
    }

    private func handle(corner: MoldWizardState.Corner, rect: CGRect, frame: CGRect) -> some View {
        let point = handlePoint(rect, corner)
        return Circle().fill(polarInk).frame(width: 14, height: 14)
            .overlay(Circle().stroke(Color.white, lineWidth: 2))
            .frame(width: 44, height: 44).contentShape(Circle())
            .position(x: point.x, y: point.y)
            .gesture(DragGesture(minimumDistance: 1).onChanged { value in
                state.resizeRegion(state.selected, corner: corner, dx: Double((value.translation.width - lastResize.width) / frame.width),
                                   dy: Double((value.translation.height - lastResize.height) / frame.height))
                lastResize = value.translation
            }.onEnded { _ in lastResize = .zero })
            .accessibilityHidden(true)  // el espacio ya ofrece «Agrandar» y «Reducir»
    }
}


/// Asistente de 3 pasos: elegir la imagen, revisar los espacios, nombre y guardar.
@MainActor struct MoldWizardView: View {
    @ObservedObject var studio: Studio
    private var state: Binding<MoldWizardState> {
        Binding(get: { studio.moldWizard ?? MoldWizardState() }, set: { studio.moldWizard = $0 })
    }
    private var current: MoldWizardState { studio.moldWizard ?? MoldWizardState() }

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            Group {
                switch current.step {
                case .choose: chooseStep
                case .review: reviewStep
                case .save: saveStep
                }
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
            Divider()
            footer
        }
        .frame(width: 780, height: 640)
        .background(polarCream)
        .tint(polarInk).buttonStyle(PolarButtonStyle())
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            Text("Importar molde").font(.system(size: 18, weight: .semibold))
            HStack(spacing: Spacing.s) {
                ForEach(MoldWizardState.Step.allCases, id: \.rawValue) { step in
                    let done = step.rawValue < current.step.rawValue, active = step == current.step
                    HStack(spacing: Spacing.xs) {
                        Text("\(step.rawValue)").font(.system(size: 12, weight: .bold)).frame(width: 24, height: 24)
                            .foregroundStyle(active || done ? polarCream : .secondary)
                            .background(active || done ? polarInk : Color.secondary.opacity(0.18), in: Circle())
                        Text(step.title).font(.system(size: 13, weight: active ? .semibold : .regular)).foregroundStyle(active ? Color.primary : .secondary)
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("Paso \(step.rawValue) de 3: \(step.title)")
                    .accessibilityValue(active ? "Paso actual" : done ? "Completado" : "Pendiente")
                    if step != .save { Rectangle().fill(Color.secondary.opacity(0.3)).frame(height: 1).frame(maxWidth: 40) }
                }
                Spacer()
            }
        }.padding(.horizontal, Spacing.l).padding(.vertical, Spacing.m).background(polarSurface)
    }

    // MARK: Paso 1
    private var chooseStep: some View {
        VStack(spacing: Spacing.m) {
            if current.awaitingDuplicateDecision, let duplicate = current.duplicate {
                duplicatePrompt(duplicate)
            } else {
                illustration.frame(height: 190)
                Text("Los espacios **transparentes** o **blancos** de tu imagen serán los lugares de las fotos. El resto (texto, adornos) se imprime tal cual.")
                    .font(.system(size: 14)).multilineTextAlignment(.center).frame(maxWidth: 480).fixedSize(horizontal: false, vertical: true)
                Button { studio.pickMoldImage() } label: { Label("Elegir imagen…", systemImage: "photo") }
                    .buttonStyle(PolarButtonStyle(primary: true)).disabled(studio.busy)
                if studio.busy { ProgressView("Buscando espacios…").controlSize(.small) }
                if let message = current.errorMessage {
                    Label(message, systemImage: "exclamationmark.triangle").font(.system(size: 12)).foregroundStyle(.orange)
                        .frame(maxWidth: 480).fixedSize(horizontal: false, vertical: true)
                }
                Text("PNG con transparencia o JPG/PNG con espacios blancos.").font(.system(size: 11)).foregroundStyle(.secondary)
            }
        }.padding(Spacing.l)
    }

    /// Ilustración sencilla: una tarjeta con tres espacios que se vuelven fotos.
    private var illustration: some View {
        HStack(spacing: Spacing.l) {
            mockCard(holes: false).accessibilityLabel("Tu imagen con espacios vacíos")
            Image(systemName: "arrow.right").font(.system(size: 22, weight: .semibold)).foregroundStyle(.secondary).accessibilityHidden(true)
            mockCard(holes: true).accessibilityLabel("Los espacios se llenan con tus fotos")
        }.accessibilityElement(children: .contain)
    }
    private func mockCard(holes filled: Bool) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10).fill(polarInk.opacity(0.85))
            VStack(spacing: 8) {
                hole(filled: filled, hue: 0.05).frame(height: 64)
                HStack(spacing: 8) { hole(filled: filled, hue: 0.45); hole(filled: filled, hue: 0.62) }.frame(height: 48)
                Capsule().fill(polarCream.opacity(0.9)).frame(width: 70, height: 6)
            }.padding(14)
        }.frame(width: 170, height: 190)
    }
    private func hole(filled: Bool, hue: Double) -> some View {
        RoundedRectangle(cornerRadius: 6)
            .fill(filled ? AnyShapeStyle(LinearGradient(colors: [Color(hue: hue, saturation: 0.45, brightness: 0.95), Color(hue: hue + 0.06, saturation: 0.6, brightness: 0.6)], startPoint: .top, endPoint: .bottom))
                         : AnyShapeStyle(Color.white))
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.white.opacity(0.7), style: StrokeStyle(lineWidth: 1, dash: filled ? [] : [4, 3])))
    }

    private func duplicatePrompt(_ duplicate: MoldDuplicate) -> some View {
        VStack(spacing: Spacing.m) {
            Image(systemName: "square.on.square").font(.system(size: 30)).foregroundStyle(polarInk).accessibilityHidden(true)
            Text("Este molde se parece a «\(duplicate.mold.nombre)» que ya guardaste").font(.system(size: 16, weight: .semibold)).multilineTextAlignment(.center)
            HStack(spacing: Spacing.l) {
                VStack(spacing: Spacing.xs) {
                    thumbnail(current.imageURL).frame(width: 150, height: 190)
                    Text("La imagen que elegiste").font(.system(size: 11)).foregroundStyle(.secondary)
                }
                VStack(spacing: Spacing.xs) {
                    thumbnail(studio.molds.imageURL(duplicate.mold.id)).frame(width: 150, height: 190)
                    Text(duplicate.mold.nombre).font(.system(size: 11)).foregroundStyle(.secondary).lineLimit(1)
                }
            }
            Text(duplicate.distance == 0 ? "Es la misma imagen." : "Se ven casi iguales; tú decides.").font(.system(size: 12)).foregroundStyle(.secondary)
            HStack(spacing: Spacing.s) {
                Button("Usar el existente") { studio.useExistingMold(duplicate.mold) }.buttonStyle(PolarButtonStyle(primary: true))
                Button("Guardar como nuevo") { state.wrappedValue.keepAsNew() }
            }
        }
    }
    private func thumbnail(_ url: URL?) -> some View {
        ZStack {
            Checkerboard()
            if let url, let image = MoldImageCache.image(url) { Image(nsImage: image).resizable().scaledToFit() }
        }.clipShape(RoundedRectangle(cornerRadius: PolarRadius.small))
            .overlay(RoundedRectangle(cornerRadius: PolarRadius.small).stroke(Color.secondary.opacity(0.3)))
    }

    // MARK: Paso 2
    private var reviewStep: some View {
        HStack(alignment: .top, spacing: Spacing.m) {
            MoldRegionEditor(state: state)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(nsColor: .unemphasizedSelectedContentBackgroundColor), in: RoundedRectangle(cornerRadius: PolarRadius.card))
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.m) {
                    Text("Espacios para fotos").font(.system(size: 14, weight: .semibold))
                    Text("Arrastra un espacio para moverlo y las asas de las esquinas para cambiar su tamaño.").font(.system(size: 11)).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                    if let hint = current.hint {
                        Label(hint, systemImage: current.detectedCount == 0 ? "info.circle" : "exclamationmark.triangle").font(.system(size: 11)).foregroundStyle(.orange)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    regionList
                    if current.regions.indices.contains(current.selected) { shapePicker(index: current.selected) }
                    HStack(spacing: Spacing.s) {
                        Button { state.wrappedValue.addRegion() } label: { Text("＋ Agregar").frame(maxWidth: .infinity) }.disabled(current.regions.count >= MoldWizardState.maximumRegions)
                        Button { state.wrappedValue.removeSelected() } label: { Text("Quitar").frame(maxWidth: .infinity) }.disabled(current.regions.count <= 1)
                    }.buttonStyle(PolarButtonStyle(expands: true)).font(.system(size: 13))
                }
            }.frame(width: 250)
        }.padding(Spacing.m)
    }

    private var regionList: some View {
        VStack(spacing: Spacing.xs) {
            ForEach(Array(current.regions.enumerated()), id: \.element.id) { index, region in
                let selected = index == current.selected
                Button { state.wrappedValue.select(index) } label: {
                    HStack {
                        Text("Espacio \(index + 1)").font(.system(size: 13, weight: selected ? .semibold : .regular))
                        Spacer()
                        Text(region.shape.name).font(.system(size: 11)).foregroundStyle(.secondary)
                    }.padding(.horizontal, Spacing.s).frame(minHeight: 36)
                        .background(selected ? polarInk.opacity(0.14) : polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.small))
                        .overlay(RoundedRectangle(cornerRadius: PolarRadius.small).stroke(selected ? polarInk : Color.secondary.opacity(0.25), lineWidth: selected ? 2 : 1))
                        .contentShape(RoundedRectangle(cornerRadius: PolarRadius.small))
                }.buttonStyle(.plain).accessibilityLabel("Espacio \(index + 1), \(region.shape.name)").accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
    }

    private func shapePicker(index: Int) -> some View {
        let region = current.regions[index]
        return VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Forma del espacio \(index + 1)").font(.system(size: 12)).foregroundStyle(.secondary)
            EqualChoice(title: "Forma del espacio \(index + 1)", options: RegionShape.allCases.map { ($0, $0.name) },
                        selection: Binding(get: { region.shape }, set: { if let value = $0 { state.wrappedValue.setShape(index, value) } }))
            if region.shape == .round {
                HStack {
                    Text("Redondeo").font(.system(size: 12)).foregroundStyle(.secondary)
                    Spacer()
                    Text("\(Int((region.radius * 100).rounded())) %").font(.system(size: 12)).monospacedDigit().foregroundStyle(.secondary)
                }
                Slider(value: Binding(get: { region.radius }, set: { state.wrappedValue.setRadius(index, max(0.01, $0)) }), in: 0.01...0.5)
                    .controlSize(.small).accessibilityLabel("Redondeo del espacio \(index + 1)")
            }
        }
    }

    // MARK: Paso 3
    private var saveStep: some View {
        HStack(alignment: .top, spacing: Spacing.l) {
            MoldRegionEditor(state: state, interactive: false).frame(width: 260, height: 360)
                .background(Color(nsColor: .unemphasizedSelectedContentBackgroundColor), in: RoundedRectangle(cornerRadius: PolarRadius.card))
                .accessibilityLabel("Vista previa del molde con \(current.regions.count) espacios")
            VStack(alignment: .leading, spacing: Spacing.m) {
                Text("Ponle un nombre").font(.system(size: 16, weight: .semibold))
                TextField("Nombre del molde", text: Binding(get: { current.name }, set: { state.wrappedValue.name = String($0.prefix(80)) }))
                    .textFieldStyle(.roundedBorder).font(.system(size: 14))
                Text("\(current.regions.count) \(current.regions.count == 1 ? "espacio" : "espacios") para fotos. Se guarda en Mis moldes para usarlo en otros proyectos.")
                    .font(.system(size: 12)).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                Divider()
                Button { studio.finishMoldWizard(.currentProject) } label: { Text("Guardar y usar en este proyecto").frame(maxWidth: .infinity) }
                    .buttonStyle(PolarButtonStyle(primary: true, expands: true)).disabled(!current.canGoNext)
                Button { studio.finishMoldWizard(.newProject) } label: { Text("Guardar y usar en un proyecto nuevo").frame(maxWidth: .infinity) }
                    .buttonStyle(PolarButtonStyle(expands: true)).disabled(!current.canGoNext)
                Button { studio.finishMoldWizard(.saveOnly) } label: { Text("Sólo guardar en Mis moldes").frame(maxWidth: .infinity) }
                    .buttonStyle(PolarButtonStyle(expands: true)).disabled(!current.canGoNext)
                Spacer()
            }.frame(maxWidth: 380)
        }.padding(Spacing.l)
    }

    private var footer: some View {
        HStack(spacing: Spacing.s) {
            Button("Cancelar") { studio.closeMoldWizard() }.keyboardShortcut(.cancelAction)
            Spacer()
            if current.step != .choose || current.awaitingDuplicateDecision {
                Button("Atrás") { state.wrappedValue.back() }
            }
            if current.step == .review {
                Button("Siguiente") { state.wrappedValue.next() }.buttonStyle(PolarButtonStyle(primary: true)).disabled(!current.canGoNext)
                    .keyboardShortcut(.defaultAction)
            }
        }.padding(.horizontal, Spacing.l).padding(.vertical, Spacing.s).background(polarSurface)
    }
}

/// Categoría «Mis moldes» del catálogo: los moldes guardados como tarjetas.
@MainActor struct MyMoldsCatalog: View {
    @ObservedObject var studio: Studio
    private let grid = [GridItem(.flexible()), GridItem(.flexible())]
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            if studio.savedMolds.isEmpty {
                VStack(spacing: Spacing.s) {
                    Image(systemName: "photo.badge.plus").font(.system(size: 26)).foregroundStyle(polarInk).accessibilityHidden(true)
                    Text("Aún no tienes moldes").font(.system(size: 13, weight: .semibold))
                    Text("Importa una imagen con espacios transparentes o blancos y la guardamos aquí para usarla cuando quieras.")
                        .font(.system(size: 11)).foregroundStyle(.secondary).multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
                }.frame(maxWidth: .infinity).padding(Spacing.m)
                    .background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
            } else {
                LazyVGrid(columns: grid, spacing: Spacing.s) {
                    ForEach(studio.savedMolds) { mold in MoldCatalogTile(studio: studio, mold: mold) }
                }
            }
            Button { studio.importTemplate() } label: { Label("Importar molde…", systemImage: "square.and.arrow.down") }.font(.system(size: 12))
        }
    }
}

@MainActor struct MoldCatalogTile: View {
    @ObservedObject var studio: Studio
    let mold: SavedMold
    @NativeState<Bool> private var confirmDelete = false
    var body: some View {
        Button { studio.useMold(mold) } label: {
            VStack(alignment: .leading, spacing: Spacing.s) {
                Color.clear.aspectRatio(1, contentMode: .fit).overlay {
                    if let image = MoldImageCache.image(studio.molds.imageURL(mold.id)) { Image(nsImage: image).resizable().scaledToFit() }
                    else { Image(systemName: "photo").font(.title).foregroundStyle(polarInk) }
                }.background(polarCream, in: RoundedRectangle(cornerRadius: PolarRadius.small)).clipped()
                Text(mold.nombre).font(.caption.bold()).lineLimit(2, reservesSpace: true).fixedSize(horizontal: false, vertical: true)
                Text("\(mold.regiones.count) \(mold.regiones.count == 1 ? "espacio" : "espacios")").font(.system(size: 10)).foregroundStyle(.secondary)
            }.padding(Spacing.s).frame(maxWidth: .infinity)
                .background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
                .overlay(RoundedRectangle(cornerRadius: PolarRadius.card).stroke(Color.secondary.opacity(0.25), lineWidth: 1))
        }.buttonStyle(.plain)
            .contextMenu { Button("Usar este molde") { studio.useMold(mold) }; Button("Quitar de Mis moldes", role: .destructive) { confirmDelete = true } }
            .overlay(alignment: .topTrailing) {
                Menu {
                    Button("Usar este molde") { studio.useMold(mold) }
                    Button("Quitar de Mis moldes", role: .destructive) { confirmDelete = true }
                } label: { Image(systemName: "ellipsis.circle.fill").font(.system(size: 16)).foregroundStyle(polarInk) }
                    .menuStyle(.borderlessButton).menuIndicator(.hidden).frame(width: 44, height: 44).accessibilityLabel("Opciones de \(mold.nombre)")
            }
            .accessibilityLabel("\(mold.nombre), \(mold.regiones.count) espacios")
            .accessibilityAction(named: "Quitar de Mis moldes") { confirmDelete = true }
            .confirmationDialog("¿Quitar «\(mold.nombre)» de Mis moldes?", isPresented: $confirmDelete, titleVisibility: .visible) {
                Button("Quitar", role: .destructive) { studio.deleteMold(mold) }
                Button("Cancelar", role: .cancel) {}
            } message: { Text("Tus proyectos que lo usan conservan su propia copia.") }
    }
}
