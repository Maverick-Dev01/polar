import SwiftUI
import AppKit

// La macro State del SDK nuevo requiere Xcode; usamos el property wrapper nativo disponible desde macOS 10.15.
typealias NativeState<Value> = SwiftUI.State<Value>

@MainActor struct PolarRootView: View {
    @ObservedObject var studio: Studio
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {
        Group {
            if studio.showingLibrary { LibraryView(studio: studio) }
            else if studio.showingFinish { FinishView(studio: studio) }
            else if studio.showingCrop { CropView(studio: studio) }
            else { StudioView(studio: studio) }
        }
        .preferredColorScheme(studio.preferences.theme == .system ? nil : studio.preferences.theme == .dark ? .dark : .light)
        .sheet(isPresented: $studio.showingWelcome) { WelcomeView(studio: studio) }
        .sheet(isPresented: Binding(get: { studio.moldWizard != nil }, set: { if !$0 { studio.closeMoldWizard() } })) { MoldWizardView(studio: studio) }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.24), value: studio.showingFinish)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.24), value: studio.showingCrop)
        .alert("No pudimos completar la acción", isPresented: Binding(get: { studio.errorMessage != nil }, set: { if !$0 { studio.errorMessage = nil } })) {
            Button("Entendido") { studio.errorMessage = nil }
        } message: { Text(studio.errorMessage ?? "") }
        .onAppear { if !studio.preferences.onboardingSeen { studio.showingWelcome = true } }
    }
}

@MainActor struct LibraryView: View {
    @ObservedObject var studio: Studio
    @NativeState<String> private var search = ""
    @NativeState<Bool> private var byName = false
    private var items: [LibraryItem] {
        let filtered = studio.libraryItems.filter { search.isEmpty || $0.name.localizedCaseInsensitiveContains(search) }
        return byName ? filtered.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending } : filtered
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            HStack {
                VStack(alignment: .leading, spacing: Spacing.s) {
                    Text("Polar").font(.custom("Georgia", size: 38)).foregroundStyle(polarInk)
                    Text("Tus diseños").font(.title2.bold())
                    Text("Recuerdos listos para volver a imprimir.").foregroundStyle(.secondary)
                }
                Spacer()
                SettingsLink { Label("Ajustes", systemImage: "gearshape") }
                Button("Abrir .polar…") { studio.openProject() }
                Button { studio.newProject() } label: { Label("Nuevo diseño", systemImage: "plus") }.buttonStyle(PolarButtonStyle(primary: true))
            }
            HStack {
                TextField("Buscar tus diseños", text: $search).textFieldStyle(.roundedBorder).frame(maxWidth: 360)
                Picker("Orden", selection: $byName) { Text("Recientes").tag(false); Text("Nombre").tag(true) }.frame(width: 180)
                Spacer()
                Text("\(items.count) \(items.count == 1 ? "diseño" : "diseños")").foregroundStyle(.secondary)
            }
            if items.isEmpty {
                ContentUnavailableView(search.isEmpty ? "Tu próximo recuerdo empieza aquí" : "No encontré ese diseño", systemImage: search.isEmpty ? "photo.on.rectangle.angled" : "magnifyingglass",
                    description: Text(search.isEmpty ? "Elige Nuevo diseño, agrega tus fotos y deja que Polar las acomode." : "Prueba con otro nombre."))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 225, maximum: 280))], alignment: .leading, spacing: Spacing.m) {
                        ForEach(items) { item in
                            LibraryProjectCard(studio: studio, item: item)
                        }
                    }.padding(.vertical, 4)
                }
            }
            if studio.deletedID != nil {
                HStack {
                    Label("Diseño borrado", systemImage: "trash")
                    Button("Deshacer") { studio.restoreDeletedDesign() }
                    Spacer()
                    Button("Cerrar aviso") { studio.deletedID = nil }.buttonStyle(.borderless)
                }.padding(Spacing.m).background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
            }
        }
        .padding(Spacing.l).background(polarCream).tint(polarInk).buttonStyle(PolarButtonStyle())
    }
}

@MainActor struct PreferencesView: View {
    @ObservedObject var studio: Studio
    @NativeState<Bool> private var confirmEmpty = false
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.l) {
            HStack { Text("Ajustes").font(.title.bold()); Spacer(); Button("Listo") { NSApp.keyWindow?.close() }.frame(minHeight: 48) }
            Form {
                FixedMenu(title: "Tema", selection: $studio.preferences.theme, options: AppTheme.allCases.map { ($0, $0.name) })
                FixedMenu(title: "Unidades", selection: $studio.preferences.units, options: AppUnits.allCases.map { ($0, $0.name) })
                FixedMenu(title: "Papel para nuevos diseños", selection: $studio.preferences.defaultPaper, options: PaperSize.allCases.map { ($0, $0.name) })
            }
            HStack {
                Text("Papelera: \(studio.trashCount) \(studio.trashCount == 1 ? "diseño" : "diseños"). Se vacía sola tras 7 días.").foregroundStyle(.secondary)
                Spacer()
                Button("Vaciar papelera (\(studio.trashCount))") { confirmEmpty = true }.disabled(studio.trashCount == 0)
            }
            .confirmationDialog("¿Vaciar la papelera?", isPresented: $confirmEmpty) {
                Button("Vaciar papelera", role: .destructive) { studio.emptyTrash() }
                Button("Cancelar", role: .cancel) {}
            } message: { Text("Los diseños borrados se eliminarán para siempre.") }
            Button("Volver a ver la bienvenida") { NSApp.keyWindow?.close(); studio.showingWelcome = true }.frame(maxWidth: .infinity, minHeight: 48)
            Text("Para imprimir, elige el mismo papel y orientación y usa Tamaño real / 100 %.").foregroundStyle(.secondary)
            Text("Funciona sin conexión. Tus fotos se copian a la biblioteca de Polar; los originales no se modifican ni se envían.").font(.callout).foregroundStyle(.secondary)
            Text("Polar \(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "?") · Mac").font(.caption).foregroundStyle(.secondary)
            Button("Mostrar licencias de las fuentes") {
                if let url = Bundle.main.resourceURL?.appendingPathComponent("licenses") { NSWorkspace.shared.open(url) }
            }
        }.padding(Spacing.l).frame(width: 520).background(polarCream).tint(polarInk).buttonStyle(PolarButtonStyle())
            .preferredColorScheme(studio.preferences.theme == .system ? nil : studio.preferences.theme == .dark ? .dark : .light)
    }
}

@MainActor struct WelcomeView: View {
    @ObservedObject var studio: Studio
    @NativeState<Int> private var step = 0
    private let titles = ["Elige un diseño", "Hazlo tuyo", "Imprime tus recuerdos"]
    private let descriptions = ["Marcos, películas, canciones y plantillas para cada ocasión.", "Agrega tus fotos. Edita las frases de todas las tarjetas o sólo de una.", "Tu trabajo se guarda automáticamente. Exporta PDF, JPG o PNG e imprime al 100 %."]
    private let icons = ["photo.on.rectangle.angled", "slider.horizontal.3", "printer"]
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: icons[step]).font(.system(size: 64)).foregroundStyle(polarInk).padding(.top, 20)
            Text(titles[step]).font(.title.bold())
            Text(descriptions[step]).multilineTextAlignment(.center).foregroundStyle(.secondary).frame(maxWidth: 370)
            HStack { ForEach(0..<3) { index in Circle().fill(index == step ? polarInk : Color.secondary.opacity(0.25)).frame(width: 8, height: 8) } }
            ViewThatFits(in: .horizontal) {
                HStack(spacing: Spacing.s) { navigation }
                VStack(spacing: Spacing.s) { navigation }
            }.buttonStyle(PolarButtonStyle(expands: true))
        }.padding(Spacing.l).frame(width: 480).background(polarCream).tint(polarInk).buttonStyle(PolarButtonStyle())
    }
    @ViewBuilder private var navigation: some View {
        Button("Saltar") { done() }.frame(maxWidth: .infinity, minHeight: 56)
        if step > 0 { Button("Anterior") { step -= 1 }.frame(maxWidth: .infinity, minHeight: 56) }
        Button(step == 2 ? "Empezar" : "Siguiente") { if step == 2 { done() } else { step += 1 } }
            .buttonStyle(PolarButtonStyle(primary: true, expands: true)).frame(maxWidth: .infinity, minHeight: 56)
    }
    private func done() { studio.preferences.onboardingSeen = true; studio.showingWelcome = false }
}

@MainActor struct FinishView: View {
    @ObservedObject var studio: Studio
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            HStack { Text("Terminar").font(.title.bold()); Spacer(); Button("Volver al editor") { studio.showingFinish = false }.frame(minHeight: 48).disabled(studio.busy) }
            Text("\(studio.project.pageCount) \(studio.project.pageCount == 1 ? "hoja" : "hojas") · \(studio.project.placedCount) \(studio.project.placedCount == 1 ? "foto" : "fotos") · \(studio.paperDescription)").foregroundStyle(.secondary)
            Text(studio.project.filterSummary).font(.callout).foregroundStyle(.secondary)
            ScrollView(.horizontal) {
                LazyHStack(alignment: .top, spacing: Spacing.m) {
                    ForEach(0..<studio.project.pageCount, id: \.self) { page in
                        VStack {
                            PageProof(project: studio.project, page: page).frame(width: 150, height: 180).background(Color.white)
                            Text("Hoja \(page + 1)").font(.caption)
                        }
                    }
                }
            }.frame(height: 205)
            if !studio.lowQualitySlots.isEmpty {
                HStack {
                    Label("\(studio.lowQualitySlots.count) \(studio.lowQualitySlots.count == 1 ? "foto con resolución baja o aceptable" : "fotos con resolución baja o aceptable")", systemImage: "exclamationmark.triangle").foregroundStyle(polarInk)
                    Button("Revisar") {
                        let slot = studio.lowQualitySlots[0]
                        studio.showingFinish = false; studio.openCrop(slot: slot)
                    }
                }
            }
            let empty = studio.project.pageCount * studio.project.settings.capacity - studio.project.placedCount
            if empty > 0 { Label("\(empty) espacios vacíos. Sus marcos y textos no se imprimen.", systemImage: "rectangle.dashed").font(.callout).foregroundStyle(.secondary) }
            Toggle("Guías para recortar fotografía y texto", isOn: Binding(get: { studio.project.settings.cutGuides }, set: { value in studio.change { $0.settings.cutGuides = value }; studio.finish() })).disabled(studio.busy)
            if studio.project.settings.cutGuides {
                Picker("Guías", selection: Binding(get: { studio.project.settings.cutStyle }, set: { value in studio.change { $0.settings.cutStyle = value }; studio.finish() })) {
                    Text("Esquinas").tag(CutStyle.corners); Text("Líneas completas").tag(CutStyle.lines)
                }.disabled(studio.busy)
            }
            VStack(alignment: .leading, spacing: Spacing.s) {
                Picker("Calidad", selection: $studio.preferences.exportQuality) {
                    ForEach(ExportQuality.allCases) { Text($0.title).tag($0) }
                }.pickerStyle(.segmented).frame(maxWidth: 480, alignment: .leading).disabled(studio.busy)
                Text(studio.preferences.exportQuality.help).font(.callout).foregroundStyle(.secondary)
            }
            if studio.busy { ProgressView("Preparando archivo…") }
            ViewThatFits(in: .horizontal) {
                HStack(spacing: Spacing.m) { finishActions }
                VStack(spacing: Spacing.m) { finishActions }
            }
            Text("Imprime al 100 %. PDF: todas las hojas con texto nítido. JPG: una hoja más ligera. PNG: sin pérdida, más peso.").font(.callout).foregroundStyle(.secondary)
            Spacer(minLength: 0)
        }.padding(Spacing.l).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading).background(polarCream).tint(polarInk)
    }
    @ViewBuilder private var finishActions: some View {
        ActionTile(title: "Imprimir", icon: "printer") { studio.printDesign() }.disabled(studio.printPDF == nil || studio.busy)
        ActionTileLabel(title: "Guardar PDF", icon: "doc").opacity(studio.busy ? 0.4 : 1).overlay {
            Menu {
                Button("PDF · todas las hojas") { studio.export(.pdf) }
            } label: { Color.clear.frame(maxWidth: .infinity, maxHeight: .infinity).contentShape(Rectangle()) }
                .menuStyle(.borderlessButton).menuIndicator(.hidden).disabled(studio.busy).accessibilityLabel("Guardar PDF")
        }
        ActionTileLabel(title: "Guardar imagen", icon: "photo").opacity(studio.busy ? 0.4 : 1).overlay {
            Menu {
                Button("JPG · esta hoja") { studio.export(.jpeg) }
                Button("PNG · sin pérdida · más peso") { studio.export(.png) }
            } label: { Color.clear.frame(maxWidth: .infinity, maxHeight: .infinity).contentShape(Rectangle()) }
                .menuStyle(.borderlessButton).menuIndicator(.hidden).disabled(studio.busy).accessibilityLabel("Guardar imagen")
        }
        ActionTile(title: "Compartir PDF", icon: "square.and.arrow.up") { if let url = studio.printPDF { studio.share(url) } }.disabled(studio.printPDF == nil || studio.busy)
    }
}

@MainActor struct LibraryProjectCard: View {
    @ObservedObject var studio: Studio
    let item: LibraryItem
    @NativeState<Bool> private var menu = false
    @NativeState<Bool> private var renaming = false
    @NativeState<String> private var name = ""
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            Button { studio.openDesign(item.id) } label: {
                Color.white.aspectRatio(1.5, contentMode: .fit).overlay {
                    if let image = NSImage(contentsOf: item.thumbnail) { Image(nsImage: image).resizable().scaledToFit().padding(Spacing.xs) }
                    else { Image(systemName: item.style.symbol).font(.system(size: 54)).foregroundStyle(polarInk) }
                }.clipped()
            }.buttonStyle(.plain).accessibilityLabel("Abrir \(item.name)")
            HStack(alignment: .firstTextBaseline, spacing: Spacing.s) {
                if renaming {
                    TextField("Nombre del diseño", text: $name).textFieldStyle(.roundedBorder).onSubmit(save)
                    Button(action: save) { Image(systemName: "checkmark") }.accessibilityLabel("Guardar nombre").frame(minWidth: 48, minHeight: 48)
                    Button { renaming = false } label: { Image(systemName: "xmark") }.accessibilityLabel("Cancelar nombre").frame(minWidth: 48, minHeight: 48)
                } else {
                    Text(item.name).font(.headline).lineLimit(1, reservesSpace: true).frame(maxWidth: .infinity, alignment: .leading).accessibilityLabel(item.name)
                    Button { menu.toggle() } label: { Image(systemName: "ellipsis").frame(width: 48, height: 48) }
                        .buttonStyle(.plain).accessibilityLabel("Opciones de \(item.name)")
                        .popover(isPresented: $menu, arrowEdge: .bottom) { actions.padding(Spacing.m).frame(width: 340).background(polarCream) }
                }
            }.frame(minHeight: 48)
            Text("\(item.style.name) · \(item.pages) \(item.pages == 1 ? "hoja" : "hojas")\n\(Date().timeIntervalSince(item.updatedAt) < 60 ? "Hace un momento" : RelativeDateTimeFormatter().localizedString(for: item.updatedAt, relativeTo: Date()))")
                .font(.caption).foregroundStyle(.secondary).lineLimit(2, reservesSpace: true)
        }.padding(Spacing.m).frame(maxWidth: .infinity).background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
            .scaleEffect(menu ? 1.015 : 1).shadow(color: .black.opacity(menu ? 0.12 : 0), radius: 8)
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.24), value: menu)
    }
    private var actions: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.m) {
            ActionTile(title: "Abrir", icon: "folder") { menu = false; studio.openDesign(item.id) }
            ActionTile(title: "Renombrar", icon: "pencil") { menu = false; name = item.name; renaming = true }
            ActionTile(title: "Duplicar", icon: "doc.on.doc") { menu = false; studio.duplicateDesign(item.id) }
            ActionTile(title: "Compartir", icon: "square.and.arrow.up") { menu = false; studio.share(studio.library.projectFile(item.id)) }
            ActionTile(title: "Borrar", icon: "trash") { menu = false; studio.deleteDesign(item.id) }
        }
    }
    private func save() { studio.renameDesign(item.id, to: name); renaming = false }
}

/// Selector en menú con el mismo ancho en todas las filas de Ajustes.
struct FixedMenu<Value: Hashable>: View {
    let title: String
    @Binding var selection: Value
    let options: [(Value, String)]
    var body: some View {
        LabeledContent(title) {
            HStack { Text(options.first { $0.0 == selection }?.1 ?? ""); Spacer(); Image(systemName: "chevron.up.chevron.down").font(.system(size: 10)) }
                .padding(.horizontal, Spacing.m).frame(width: 220, height: 40)
                .background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.small))
                .overlay {
                    Menu {
                        ForEach(Array(options.enumerated()), id: \.offset) { _, option in Button(option.1) { selection = option.0 } }
                    } label: { Color.clear.frame(maxWidth: .infinity, maxHeight: .infinity).contentShape(Rectangle()) }
                        .menuStyle(.borderlessButton).menuIndicator(.hidden).accessibilityLabel(title)
                }
        }
    }
}
