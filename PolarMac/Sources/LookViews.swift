import SwiftUI
import AppKit

enum Spacing { static let xs: CGFloat = 4, s: CGFloat = 8, m: CGFloat = 16, l: CGFloat = 24, xl: CGFloat = 32 }
enum PolarRadius { static let small: CGFloat = 8, card: CGFloat = 12, surface: CGFloat = 20 }

struct PolarButtonStyle: ButtonStyle {
    var primary = false
    var expands = false
    @Environment(\.isEnabled) private var enabled
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.lineLimit(1).padding(.horizontal, Spacing.m).frame(maxWidth: expands ? .infinity : nil, minHeight: primary ? 56 : 48)
            .foregroundStyle(primary ? polarCream : polarInk)
            .background(primary ? polarInk : polarInk.opacity(configuration.isPressed ? 0.18 : 0.08), in: RoundedRectangle(cornerRadius: PolarRadius.small))
            .contentShape(RoundedRectangle(cornerRadius: PolarRadius.small)).opacity(enabled ? 1 : 0.4)
    }
}

struct ActionTile: View {
    let title: String
    let icon: String
    var selected = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: Spacing.s) {
                Image(systemName: icon).font(.system(size: 24)).frame(width: 24, height: 24)
                Text(title).font(.callout).lineLimit(1)
            }.frame(maxWidth: .infinity, minHeight: 64).padding(Spacing.s)
                .foregroundStyle(selected ? polarInk : .primary)
                .background(selected ? polarInk.opacity(0.12) : polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
                .overlay(RoundedRectangle(cornerRadius: PolarRadius.card).stroke(selected ? polarInk : Color.secondary.opacity(0.25), lineWidth: selected ? 2 : 1))
                .overlay(alignment: .topTrailing) { if selected { Image(systemName: "checkmark.circle.fill").foregroundStyle(polarInk).padding(Spacing.xs) } }
        }.buttonStyle(.plain).accessibilityLabel(title).accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct FileTabs: View {
    @Binding var selection: Int
    private let tabs: [(Int, String, String)] = [(3,"Fotos","photo.on.rectangle"), (4,"Filtros","camera.filters"), (0,"Diseño","rectangle.3.group"), (1,"Texto","textformat"), (2,"Papel","doc")]
    var body: some View {
        HStack(spacing: Spacing.xs) {
            ForEach(tabs, id: \.0) { item in
                Button { selection = item.0 } label: {
                    VStack(spacing: Spacing.xs) {
                        Image(systemName: item.2).font(.system(size: 20)).frame(height: 24)
                        Text(item.1).font(.system(size: 11, weight: .medium)).lineLimit(1)
                    }.frame(maxWidth: .infinity, minHeight: 56)
                        .foregroundStyle(selection == item.0 ? polarInk : .primary)
                        .background(selection == item.0 ? polarCream : polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.small))
                        .overlay(alignment: .bottom) { Rectangle().fill(selection == item.0 ? polarInk : .clear).frame(height: 2) }
                }.buttonStyle(.plain).accessibilityLabel(item.1).accessibilityAddTraits(selection == item.0 ? .isSelected : [])
            }
        }.padding(Spacing.s)
    }
}

/// Dragging the tick strip is one history transaction; VoiceOver gets the same increment/decrement operation.
struct LensRing: View {
    let title: String
    let value: Double
    let range: ClosedRange<Double>
    var step: Double = 0.1
    let change: (Double) -> Void
    let begin: () -> Void
    let end: () -> Void
    @NativeState<Double?> private var start = nil
    @NativeState<Int?> private var notch = nil
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            HStack(alignment: .firstTextBaseline) {
                Text(title).font(.callout)
                Spacer()
                Text(String(format: "%.1f", value)).monospacedDigit().font(.callout).foregroundStyle(.secondary)
            }
            GeometryReader { geometry in
                ZStack {
                    RoundedRectangle(cornerRadius: PolarRadius.small).fill(polarCream)
                    HStack(alignment: .center, spacing: 0) {
                        ForEach(0..<31) { index in
                            Rectangle().fill(Color.primary.opacity(index % 5 == 0 ? 0.55 : 0.2))
                                .frame(width: 1, height: index % 5 == 0 ? 22 : 12).frame(maxWidth: .infinity)
                        }
                    }.padding(.horizontal, Spacing.s)
                    Rectangle().fill(polarInk).frame(width: 3, height: 30)
                        .offset(x: CGFloat((value - range.lowerBound) / (range.upperBound - range.lowerBound) - 0.5) * max(1, geometry.size.width - 24))
                }.contentShape(Rectangle())
                    .gesture(DragGesture(minimumDistance: 0).onChanged { drag in
                        if start == nil { start = value; begin() }
                        let next = min(range.upperBound, max(range.lowerBound, (start ?? value) + Double(drag.translation.width / max(1, geometry.size.width)) * (range.upperBound - range.lowerBound)))
                        if range.upperBound == 4 {
                            let mark = Int(next.rounded())
                            if (1...3).contains(mark), abs(next - Double(mark)) < 0.04, notch != mark {
                                NSHapticFeedbackManager.defaultPerformer.perform(.alignment, performanceTime: .now); notch = mark
                            } else if abs(next - Double(mark)) >= 0.04 { notch = nil }
                        }
                        change(next)
                    }.onEnded { _ in start = nil; notch = nil; end() })
            }.frame(height: 48)
                .accessibilityElement(children: .ignore).accessibilityLabel(title)
                .accessibilityValue(String(format: "%.1f", value))
                .accessibilityAdjustableAction { direction in
                    begin(); change(min(range.upperBound, max(range.lowerBound, value + (direction == .increment ? step : -step)))); end()
                }
        }.onDisappear { if start != nil { end(); start = nil } }
    }
}

@MainActor struct PhotoTools: View {
    @ObservedObject var studio: Studio
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.m) {
                ActionTile(title: "Agregar", icon: "plus") { studio.addPhotos() }
                ActionTile(title: "Rellenar", icon: "sparkles") { studio.fillAll() }.disabled(studio.project.photos.isEmpty)
                ActionTile(title: "Encuadrar", icon: "crop") { studio.openCrop() }.disabled(selected == nil)
                ActionTile(title: "Filtros", icon: "camera.filters") { studio.lookScope = 3; studio.inspectorTab = 4 }.disabled(selected == nil)
            }
            if let photo = selected {
                Text(photo.name).font(.headline).lineLimit(1).accessibilityLabel(photo.name)
                RenderedCard(studio: studio).frame(height: 190)
                Text("Toca Encuadrar para mover y acercar la foto dentro de su marco.").foregroundStyle(.secondary)
                Button("Quitar foto", role: .destructive) { studio.change { $0.placements[studio.selectedSlot] = nil } }.frame(maxWidth: .infinity, minHeight: 48)
            } else {
                Text("Elige un espacio de la hoja y una foto de tu galería.").foregroundStyle(.secondary)
            }
        }
    }
    private var selected: PhotoAsset? {
        guard studio.project.placements.indices.contains(studio.selectedSlot) else { return nil }
        return studio.project.asset(for: studio.project.placements[studio.selectedSlot])
    }
}

private let presetImageCache: NSCache<NSString, NSImage> = { let cache = NSCache<NSString, NSImage>(); cache.countLimit = 64; return cache }()

@MainActor struct PresetTile: View {
    let project: PolarProject
    let slot: Int
    let preset: String
    let sampleLook: PhotoLook
    let selected: Bool
    let action: () -> Void
    @NativeState<NSImage?> private var image = nil
    private var key: String { String(data: (try? JSONEncoder().encode(project)) ?? Data(), encoding: .utf8)! + "@\(slot):\(preset):96:" + (String(data: (try? JSONEncoder().encode(sampleLook)) ?? Data(), encoding: .utf8) ?? "") }
    var body: some View {
        Button(action: action) {
            VStack(spacing: Spacing.s) {
                Group {
                    if let image { Image(nsImage: image).resizable().scaledToFill() }
                    else { Image(systemName: "photo").font(.title).foregroundStyle(.secondary) }
                }.frame(width: 84, height: 84).clipped().background(polarCream)
                Text(PhotoLook.names[preset] ?? "Original").font(.caption).lineLimit(1).frame(width: 104)
            }.padding(Spacing.s).background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
                .overlay(RoundedRectangle(cornerRadius: PolarRadius.card).stroke(selected ? polarInk : Color.secondary.opacity(0.3), lineWidth: selected ? 2 : 1))
                .overlay(alignment: .topTrailing) { if selected { Image(systemName: "checkmark.circle.fill").foregroundStyle(polarInk).padding(Spacing.xs) } }
        }.buttonStyle(.plain).accessibilityLabel(PhotoLook.names[preset] ?? "Original").accessibilityAddTraits(selected ? .isSelected : [])
            .task(id: key) {
                let id = key
                if let cached = presetImageCache.object(forKey: id as NSString) { image = cached; return }
                var sample = project
                sample.setPhotoLook(sampleLook, slot: slot)
                let rendered = await Task.detached(priority: .userInitiated) { PolarRenderer.cardPreview(project: sample, slot: slot, width: 96, photoOnly: true) }.value
                guard !Task.isCancelled else { return }
                if let rendered { presetImageCache.setObject(rendered, forKey: id as NSString) }; image = rendered
            }
    }
}

@MainActor struct FiltersTools: View {
    @ObservedObject var studio: Studio
    @NativeState<Bool> private var advanced = false
    private var look: PhotoLook {
        if studio.lookScope == 0 { return studio.project.settings.photoLook ?? PhotoLook() }
        if studio.lookScope == 1 || studio.lookScope == 2 {
            let page = studio.lookScope == 2 ? studio.lookPages.sorted().first ?? studio.page : studio.page
            let card = page * studio.project.settings.capacity / studio.project.settings.style.photosPerCard
            return studio.project.cardOverrides[String(card)]?.photoLook ?? studio.project.settings.photoLook ?? PhotoLook()
        }
        return studio.currentLook
    }
    private var slot: Int {
        if studio.project.placements.indices.contains(studio.selectedSlot), studio.project.placements[studio.selectedSlot] != nil { return studio.selectedSlot }
        return studio.project.placements.firstIndex(where: { $0 != nil }) ?? 0
    }
    private var canApply: Bool {
        if studio.lookScope == 2 { return !studio.lookPages.isEmpty }
        if studio.lookScope == 3 { return studio.project.placements.indices.contains(studio.selectedSlot) && studio.project.placements[studio.selectedSlot] != nil }
        return true
    }
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            Text("Aplicar a").font(.headline)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.s) {
                scope("Todo proyecto", 0, "square.stack")
                scope("Esta página", 1, "doc")
                scope("Elegir páginas", 2, "doc.on.doc")
                scope("Esta foto", 3, "photo")
            }
            if studio.lookScope == 2 {
                Text("Selecciona una o varias páginas").font(.callout).foregroundStyle(.secondary)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 64))], spacing: Spacing.s) {
                    ForEach(0..<studio.project.pageCount, id: \.self) { page in
                        Button {
                            if studio.lookPages.contains(page) { studio.lookPages.remove(page) } else { studio.lookPages.insert(page) }
                        } label: {
                            Label("\(page + 1)", systemImage: studio.lookPages.contains(page) ? "checkmark.circle.fill" : "circle")
                                .frame(maxWidth: .infinity, minHeight: 48)
                        }.buttonStyle(.bordered).accessibilityLabel("Página \(page + 1)").accessibilityAddTraits(studio.lookPages.contains(page) ? .isSelected : [])
                    }
                }
            }
            Text("Filtros").font(.headline)
            ScrollView(.horizontal) {
                HStack(spacing: Spacing.m) {
                    ForEach(PhotoLook.presets, id: \.self) { preset in
                        PresetTile(project: studio.project, slot: slot, preset: preset, sampleLook: look.replacing(preset: preset), selected: look.preset == preset) { studio.applyLook(look.replacing(preset: preset)) }.disabled(!canApply)
                    }
                }.padding(Spacing.xs)
            }
            LensRing(title: "Intensidad", value: look.intensity, range: 0...1, step: 0.1,
                     change: { studio.adjustLook(look.replacing(intensity: $0)) }, begin: studio.beginEditing, end: studio.endEditing).disabled(!canApply)
            DisclosureGroup("Ajustes", isExpanded: $advanced) {
                VStack(spacing: Spacing.m) {
                    ring("Luz", look.light, -1...1) { studio.adjustLook(look.replacing(light: $0)) }
                    ring("Contraste", look.contrast, -1...1) { studio.adjustLook(look.replacing(contrast: $0)) }
                    ring("Calidez", look.warmth, -1...1) { studio.adjustLook(look.replacing(warmth: $0)) }
                    ring("Grano", look.grain, 0...1) { studio.adjustLook(look.replacing(grain: $0)) }
                }.padding(.top, Spacing.m)
            }.disabled(!canApply)
            Button("Comparar · mantener pulsado") { }.frame(maxWidth: .infinity, minHeight: 48)
                .onLongPressGesture(minimumDuration: 0.01, pressing: studio.setComparing, perform: {})
                .accessibilityHint("Mantén pulsado o mantén la tecla espacio para ver el original")
            ViewThatFits(in: .horizontal) {
                HStack { removeButton; allButton }
                VStack { removeButton; allButton }
            }
            .buttonStyle(PolarButtonStyle(expands: true)).disabled(!canApply)
            if studio.lookUndoNotice {
                HStack { Text("Filtro aplicado"); Spacer(); Button("Deshacer") { studio.undo(); studio.lookUndoNotice = false } }.padding(Spacing.m).background(polarCream, in: RoundedRectangle(cornerRadius: PolarRadius.card))
            }
        }
            .onAppear { if studio.lookPages.isEmpty { studio.lookPages = [studio.page] } }
            .onDisappear { studio.endEditing(); studio.setComparing(false) }
    }
    private func scope(_ name: String, _ value: Int, _ icon: String) -> some View {
        ActionTile(title: name, icon: icon, selected: studio.lookScope == value) { studio.endEditing(); studio.lookScope = value }
    }
    private func ring(_ title: String, _ value: Double, _ range: ClosedRange<Double>, change: @escaping (Double) -> Void) -> some View {
        LensRing(title: title, value: value, range: range, change: change, begin: studio.beginEditing, end: studio.endEditing)
    }
    private var removeButton: some View { Button("Quitar filtro") { studio.applyLook(PhotoLook()) }.frame(maxWidth: .infinity, minHeight: 48) }
    private var allButton: some View { Button("Aplicar a todas") { studio.applyLook(look, all: true) }.frame(maxWidth: .infinity, minHeight: 48) }
}

@MainActor struct PageProof: View {
    let project: PolarProject
    let page: Int
    @NativeState<NSImage?> private var image = nil
    private var key: String { "\(page):" + (String(data: (try? JSONEncoder().encode(project)) ?? Data(), encoding: .utf8) ?? "") }
    var body: some View {
        Group { if let image { Image(nsImage: image).resizable().scaledToFit() } else { ProgressView() } }
            .task(id: key) {
                let rendered = await Task.detached(priority: .userInitiated) { PolarRenderer.preview(project: project, page: page, scale: 0.5, printReady: true) }.value
                if !Task.isCancelled { image = rendered }
            }
    }
}

@MainActor struct RenderedCard: View {
    @ObservedObject var studio: Studio
    @NativeState<NSImage?> private var image = nil
    private var key: String { "\(studio.selectedSlot):\(studio.comparing):" + (String(data: (try? JSONEncoder().encode(studio.project)) ?? Data(), encoding: .utf8) ?? "") }
    var body: some View {
        Group { if let image { Image(nsImage: image).resizable().scaledToFit() } else { ProgressView() } }
            .task(id: key) {
                var snapshot = studio.project
                if studio.comparing { snapshot.setAllLooks(PhotoLook()) }
                let slot = studio.selectedSlot
                let rendered = await Task.detached(priority: .userInitiated) { PolarRenderer.cardPreview(project: snapshot, slot: slot) }.value
                if !Task.isCancelled { image = rendered }
            }
    }
}

@MainActor private struct CropOverflow: View {
    @ObservedObject var studio: Studio
    @NativeState<NSImage?> private var image = nil
    private var key: String { "\(studio.selectedSlot):" + (String(data: (try? JSONEncoder().encode(studio.project)) ?? Data(), encoding: .utf8) ?? "") }
    var body: some View {
        Group { if let image { Image(nsImage: image).resizable().scaledToFit() } else { Color.clear } }
            .task(id: key) {
                let snapshot = studio.project, slot = studio.selectedSlot
                let rendered = await Task.detached(priority: .userInitiated) { PolarRenderer.cropOverflow(project: snapshot, slot: slot) }.value
                if !Task.isCancelled { image = rendered }
            }
    }
}

@MainActor struct CropView: View {
    @ObservedObject var studio: Studio
    @NativeState<PhotoPlacement?> private var dragBase = nil
    @NativeState<Double?> private var zoomBase = nil
    @NativeState<Bool> private var fine = false
    @NativeState<Bool> private var manipulating = false
    private var placement: PhotoPlacement? { studio.project.placements.indices.contains(studio.selectedSlot) ? studio.project.placements[studio.selectedSlot] : nil }
    private var asset: PhotoAsset? { studio.project.asset(for: placement) }
    private var geometry: (card: CGRect, photo: CGRect)? { PolarRenderer.cropGeometry(project: studio.project, slot: studio.selectedSlot) }
    private var slots: [Int] { studio.project.placements.indices.filter { studio.project.placements[$0] != nil } }
    private var minimumZoom: Double {
        guard let asset, let placement, let geometry else { return 0.1 }
        return PhotoFit.fitZoom(box: geometry.photo.size, source: CGSize(width: asset.pixelWidth, height: asset.pixelHeight), quarterTurns: placement.quarterTurns)
    }
    var body: some View {
        VStack(spacing: Spacing.m) {
            HStack {
                Button("Volver al editor") { studio.endEditing(); studio.showingCrop = false }.frame(minHeight: 48)
                Spacer()
                Text("Encuadrar").font(.custom("Georgia", size: 24)).foregroundStyle(polarInk)
                Spacer()
                Button("Listo") { studio.endEditing(); studio.showingCrop = false }.buttonStyle(PolarButtonStyle(primary: true)).frame(minHeight: 56)
            }
            GeometryReader { proxy in frameArea(proxy.size) }.frame(maxHeight: .infinity)
            HStack {
                Button { studio.nextPhoto(-1) } label: { Label("Anterior", systemImage: "chevron.left") }.disabled(slots.first == studio.selectedSlot)
                Spacer()
                Text("\((slots.firstIndex(of: studio.selectedSlot) ?? 0) + 1) de \(slots.count)").monospacedDigit()
                Spacer()
                Button { studio.nextPhoto(1) } label: { Label("Siguiente", systemImage: "chevron.right") }.disabled(slots.last == studio.selectedSlot)
            }.frame(maxWidth: 700, minHeight: 48)
            if let placement {
                LensRing(title: "Zoom · anillo de lente", value: placement.zoom, range: minimumZoom...4,
                         change: { value in studio.editPlacement { $0.zoom = value } }, begin: studio.beginEditing, end: studio.endEditing).frame(maxWidth: 700)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: Spacing.m) {
                ActionTile(title: "Girar 90°", icon: "rotate.right") { action { $0.quarterTurns = ($0.quarterTurns + 1) % 4 } }
                ActionTile(title: "Llenar", icon: "arrow.up.left.and.arrow.down.right") { action { $0.zoom = 1; $0.offsetX = 0; $0.offsetY = 0 } }
                ActionTile(title: "Ajustar", icon: "arrow.down.right.and.arrow.up.left") { fit() }
                ActionTile(title: "Centrar", icon: "scope") { action { $0.offsetX = 0; $0.offsetY = 0 } }
                ActionTile(title: "Restablecer", icon: "arrow.counterclockwise") { action { $0.zoom = 1; $0.offsetX = 0; $0.offsetY = 0; $0.quarterTurns = 0 } }
            }.frame(maxWidth: 800)
            DisclosureGroup("Ajuste fino", isExpanded: $fine) {
                HStack(spacing: Spacing.m) {
                    ForEach([( "←", -0.01, 0.0), ("↑", 0.0, -0.01), ("↓", 0.0, 0.01), ("→", 0.01, 0.0)], id: \.0) { item in
                        Button(item.0) { nudge(item.1, item.2) }.frame(maxWidth: .infinity, minHeight: 48).accessibilityLabel(nudgeName(item.1, item.2))
                    }
                    Button("−0.1") { zoom(-0.1) }.frame(maxWidth: .infinity, minHeight: 48).accessibilityLabel("Alejar")
                    Button("+0.1") { zoom(0.1) }.frame(maxWidth: .infinity, minHeight: 48).accessibilityLabel("Acercar")
                }
            }.frame(maxWidth: 800)
            if let geometry, let dpi = studio.project.effectiveDPI(slot: studio.selectedSlot, rect: geometry.photo) {
                Label(dpi < 150 ? "Poca resolución para este tamaño" : "Resolución suficiente · \(Int(dpi)) ppp", systemImage: dpi < 150 ? "exclamationmark.triangle" : "checkmark.circle")
                    .foregroundStyle(dpi < 150 ? polarInk : .secondary)
            }
        }.padding(Spacing.l).background(polarCream).tint(polarInk).buttonStyle(PolarButtonStyle())
            .background(PhotoKeys(compare: studio.setComparing, zoom: zoom, nudge: nudge, begin: studio.beginEditing, end: studio.endEditing).frame(width: 0, height: 0))
            .onDisappear { studio.endEditing(); studio.setComparing(false) }
    }
    @ViewBuilder private func frameArea(_ size: CGSize) -> some View {
        if let geometry, let asset, let placement {
            let scale = max(0.05, min((size.width - 80) / geometry.card.width, (size.height - 32) / geometry.card.height))
            let cardSize = CGSize(width: geometry.card.width * scale, height: geometry.card.height * scale)
            let photo = CGRect(x: (geometry.photo.minX - geometry.card.minX) * scale, y: (geometry.photo.minY - geometry.card.minY) * scale, width: geometry.photo.width * scale, height: geometry.photo.height * scale)
            ZStack {
                polarSurface
                RenderedCard(studio: studio).frame(width: cardSize.width, height: cardSize.height).shadow(radius: 8)
                CropOverflow(studio: studio).frame(width: cardSize.width * 1.36, height: cardSize.height * 1.16)
                    .opacity(0.2).mask {
                        Path { path in
                            path.addRect(CGRect(x: 0, y: 0, width: cardSize.width * 1.36, height: cardSize.height * 1.16))
                            path.addRect(photo.offsetBy(dx: cardSize.width * 0.18, dy: cardSize.height * 0.08))
                        }.fill(style: FillStyle(eoFill: true))
                    }.allowsHitTesting(false)
                if manipulating {
                    Path { path in
                        for index in 1...2 {
                            let x = photo.minX + photo.width * CGFloat(index) / 3, y = photo.minY + photo.height * CGFloat(index) / 3
                            path.move(to: CGPoint(x: x, y: photo.minY)); path.addLine(to: CGPoint(x: x, y: photo.maxY))
                            path.move(to: CGPoint(x: photo.minX, y: y)); path.addLine(to: CGPoint(x: photo.maxX, y: y))
                        }
                    }.stroke(Color.white.opacity(0.8), lineWidth: 1).frame(width: cardSize.width, height: cardSize.height).allowsHitTesting(false)
                }
            }.frame(width: cardSize.width, height: cardSize.height)
                .contentShape(Rectangle())
                .gesture(DragGesture(minimumDistance: 2).onChanged { value in
                    if dragBase == nil { dragBase = placement; manipulating = true; studio.beginEditing() }
                    let moved = PhotoFit.moved(dragBase ?? placement, translation: CGSize(width: value.translation.width / scale, height: value.translation.height / scale), box: geometry.photo.size, source: CGSize(width: asset.pixelWidth, height: asset.pixelHeight))
                    studio.editPlacement { $0 = moved }
                }.onEnded { _ in dragBase = nil; manipulating = false; studio.endEditing() })
                .simultaneousGesture(MagnifyGesture().onChanged { value in
                    if zoomBase == nil { zoomBase = placement.zoom; manipulating = true; studio.beginEditing() }
                    studio.editPlacement { $0.zoom = min(4, max(minimumZoom, (zoomBase ?? placement.zoom) * value.magnification)) }
                }.onEnded { _ in zoomBase = nil; manipulating = false; studio.endEditing() })
                .onTapGesture(count: 2) { if abs(placement.zoom - minimumZoom) < 0.01 { action { $0.zoom = 1; $0.offsetX = 0; $0.offsetY = 0 } } else { fit() } }
                .accessibilityElement(children: .ignore).accessibilityLabel("Foto \((slots.firstIndex(of: studio.selectedSlot) ?? 0) + 1), encuadre dentro de su marco")
                .accessibilityAction(named: "Mover a la izquierda") { nudge(-0.01, 0) }
                .accessibilityAction(named: "Mover a la derecha") { nudge(0.01, 0) }
                .accessibilityAction(named: "Mover arriba") { nudge(0, -0.01) }
                .accessibilityAction(named: "Mover abajo") { nudge(0, 0.01) }
                .accessibilityAction(named: "Acercar") { zoom(0.1) }.accessibilityAction(named: "Alejar") { zoom(-0.1) }
                .position(x: size.width / 2, y: size.height / 2)
        } else { Text("Elige una foto para encuadrar").frame(maxWidth: .infinity, maxHeight: .infinity) }
    }
    private func action(_ edit: (inout PhotoPlacement) -> Void) { studio.endEditing(); studio.editPlacement(edit) }
    private func fit() { action { $0.zoom = minimumZoom; $0.offsetX = 0; $0.offsetY = 0 } }
    private func zoom(_ delta: Double) { studio.editPlacement { $0.zoom = min(4, max(minimumZoom, $0.zoom + delta)) } }
    private func nudge(_ x: Double, _ y: Double) { action { $0.offsetX = min(1, max(-1, $0.offsetX + x)); $0.offsetY = min(1, max(-1, $0.offsetY + y)) } }
    private func nudgeName(_ x: Double, _ y: Double) -> String { x < 0 ? "Mover a la izquierda" : x > 0 ? "Mover a la derecha" : y < 0 ? "Mover arriba" : "Mover abajo" }
}

/// Native events are limited to this window and leave text editing untouched.
struct PhotoKeys: NSViewRepresentable {
    let compare: (Bool) -> Void
    var zoom: ((Double) -> Void)?
    var nudge: ((Double, Double) -> Void)?
    var begin: (() -> Void)?
    var end: (() -> Void)?
    func makeNSView(context: Context) -> KeyView { KeyView() }
    func updateNSView(_ view: KeyView, context: Context) { view.handlers = self }
    static func dismantleNSView(_ view: KeyView, coordinator: ()) { view.stop() }
    final class KeyView: NSView {
        var handlers: PhotoKeys?
        private var monitor: Any?
        private var focusObserver: NSObjectProtocol?
        private var wheelEnd: DispatchWorkItem?
        private var wheelActive = false
        override func viewDidMoveToWindow() {
            super.viewDidMoveToWindow()
            if window == nil { stop(); return }
            guard monitor == nil else { return }
            focusObserver = NotificationCenter.default.addObserver(forName: NSWindow.didResignKeyNotification, object: window, queue: .main) { [weak self] _ in self?.finishInteraction() }
            monitor = NSEvent.addLocalMonitorForEvents(matching: [.keyDown, .keyUp, .scrollWheel]) { [weak self] event in
                guard let self, event.window == self.window, !(self.window?.firstResponder is NSTextView), let handlers = self.handlers else { return event }
                if event.type == .scrollWheel, event.modifierFlags.contains(.command), let zoom = handlers.zoom {
                    if !self.wheelActive { self.wheelActive = true; handlers.begin?() }
                    zoom(Double(event.scrollingDeltaY) * 0.015)
                    self.wheelEnd?.cancel()
                    let work = DispatchWorkItem { [weak self] in self?.wheelActive = false; self?.handlers?.end?() }
                    self.wheelEnd = work; DispatchQueue.main.asyncAfter(deadline: .now() + 0.2, execute: work)
                    return nil
                }
                if event.keyCode == 49, event.modifierFlags.intersection([.command, .option, .control]).isEmpty { handlers.compare(event.type == .keyDown); return nil }
                guard event.type == .keyDown else { return event }
                if event.modifierFlags.contains(.command), let zoom = handlers.zoom {
                    if ["+", "="].contains(event.charactersIgnoringModifiers ?? "") { handlers.begin?(); zoom(0.1); handlers.end?(); return nil }
                    if event.charactersIgnoringModifiers == "-" { handlers.begin?(); zoom(-0.1); handlers.end?(); return nil }
                }
                if let nudge = handlers.nudge {
                    switch event.keyCode {
                    case 123: nudge(-0.01, 0)
                    case 124: nudge(0.01, 0)
                    case 125: nudge(0, 0.01)
                    case 126: nudge(0, -0.01)
                    default: return event
                    }; return nil
                }
                return event
            }
        }
        func stop() {
            if let monitor { NSEvent.removeMonitor(monitor); self.monitor = nil }
            if let focusObserver { NotificationCenter.default.removeObserver(focusObserver); self.focusObserver = nil }
            finishInteraction()
        }
        private func finishInteraction() {
            wheelEnd?.cancel(); wheelEnd = nil
            if wheelActive { handlers?.end?(); wheelActive = false }
            handlers?.compare(false)
        }
    }
}
