import SwiftUI
import AppKit

let polarInk = Color(nsColor: NSColor(name: nil) { appearance in
    appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
        ? NSColor(srgbRed: 1, green: 0.70, blue: 0.74, alpha: 1)
        : NSColor(srgbRed: 0.48, green: 0.16, blue: 0.23, alpha: 1)
})
let polarCream = Color(nsColor: NSColor(name: nil) { appearance in
    appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
        ? NSColor(srgbRed: 0.11, green: 0.10, blue: 0.10, alpha: 1)
        : NSColor(srgbRed: 0.97, green: 0.95, blue: 0.92, alpha: 1)
})
let polarSurface = Color(nsColor: .controlBackgroundColor)
private let ink = polarInk
private let cream = polarCream

@MainActor final class PolarDelegate: NSObject, NSApplicationDelegate, NSWindowDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)
        DispatchQueue.main.async {
            NSApp.windows.first?.delegate = self
        }
    }
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }
    func applicationShouldTerminate(_ sender: NSApplication) -> NSApplication.TerminateReply {
        Studio.shared.confirmDiscard() ? .terminateNow : .terminateCancel
    }
    func windowShouldClose(_ sender: NSWindow) -> Bool { Studio.shared.confirmDiscard() }
    func application(_ sender: NSApplication, openFiles filenames: [String]) {
        if let path = filenames.first { Studio.shared.openProject(at: URL(fileURLWithPath: path)) }
        sender.reply(toOpenOrPrint: .success)
    }
}

@main @MainActor struct PolarApp: App {
    @NSApplicationDelegateAdaptor(PolarDelegate.self) var delegate
    @StateObject private var studio = Studio.shared
    var body: some Scene {
        Window("Polar", id: "studio") {
            PolarRootView(studio: studio)
                .frame(minWidth: 1120, minHeight: 740)
        }
        .defaultSize(width: 1320, height: 900)
        .commands {
            CommandGroup(replacing: .newItem) {
                Button("Nuevo diseño") { studio.newProject() }.keyboardShortcut("n")
                Button("Abrir diseño…") { studio.openProject() }.keyboardShortcut("o")
                Button("Importar plantilla…") { studio.importTemplate() }
                Button("Guardar diseño…") { studio.saveProject() }.keyboardShortcut("s")
                Button("Guardar diseño como…") { studio.saveProject(asNew: true) }.keyboardShortcut("s", modifiers: [.command, .shift])
            }
            CommandGroup(replacing: .undoRedo) {
                Button("Deshacer") { studio.undo() }.keyboardShortcut("z").disabled(!studio.canUndo)
                Button("Rehacer") { studio.redo() }.keyboardShortcut("z", modifiers: [.command, .shift]).disabled(!studio.canRedo)
            }
            CommandGroup(replacing: .appSettings) {
                SettingsLink { Text("Ajustes…") }.keyboardShortcut(",")
            }
            CommandMenu("Fotos") {
                Button("Agregar fotos…") { studio.addPhotos() }.keyboardShortcut("i")
                Button("Rellenar todas las hojas") { studio.fillAll() }
                Button("Vaciar esta hoja") { studio.clearPage() }
            }
            CommandMenu("Imprimir") {
                Button("Preparar impresión…") { studio.finish() }.keyboardShortcut("p")
                Button("Exportar PDF…") { studio.export(.pdf) }.keyboardShortcut("e")
                Button("Exportar JPG de esta hoja…") { studio.export(.jpeg) }
                Button("Exportar PNG sin pérdida…") { studio.export(.png) }
            }
            CommandGroup(replacing: .help) {
                Button("Descargar última versión…") {
                    NSWorkspace.shared.open(URL(string: "https://github.com/Maverick-Dev01/polar/releases/latest")!)
                }
                Button("Cómo usar Polar") {
                    let alert = NSAlert()
                    alert.messageText = "Fotos que se quedan"
                    alert.informativeText = "1. Elige un diseño y agrega tus fotos.\n2. Selecciona una tarjeta para cambiar su foto o encuadre.\n3. En Texto, edita todas las tarjetas o sólo la seleccionada, con fuentes y fechas.\n4. Usa Imprimir para revisar resolución y hojas y guardar PDF, JPG o PNG.\n\nTu trabajo se guarda automáticamente en Tus diseños. Los originales se conservan; Polar guarda una copia para trabajar. Puedes exportar un .polar editable; para abrirlo en otro equipo necesitarás también sus fotos.\n\nElige el mismo papel y orientación en la impresora y usa Tamaño real / 100 %."
                    alert.runModal()
                }
            }
        }
        Settings { PreferencesView(studio: studio) }
    }
}

@MainActor struct StudioView: View {
    @ObservedObject var studio: Studio
    @NativeState<Bool> private var phrases = false
    @NativeState<Bool> private var expandedText = false
    @FocusState private var textFocused: Bool
    private let grid = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        VStack(spacing: 0) {
            topbar
            Divider()
            HStack(spacing: 0) {
                designs.frame(width: 216)
                Divider()
                VStack(spacing: 0) {
                    pageBar
                    canvas
                    Divider()
                    gallery.frame(height: 216)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                Divider()
                inspector.frame(width: 360)
            }
            Divider()
            HStack(spacing: 8) {
                if studio.busy { ProgressView().controlSize(.mini) }
                else { Image(systemName: "checkmark.circle").foregroundStyle(ink) }
                Text(studio.status).font(.system(size: 11)).lineLimit(1)
                Spacer()
                if let url = studio.lastExport {
                    Button("Mostrar archivo") { NSWorkspace.shared.activateFileViewerSelecting([url]) }
                        .buttonStyle(.borderless).font(.system(size: 11))
                }
                Text(studio.paperDescription).font(.system(size: 11)).foregroundStyle(.secondary)
            }.padding(.horizontal, Spacing.m).padding(.vertical, Spacing.s).background(polarSurface)
        }
        .tint(ink)
        .buttonStyle(PolarButtonStyle())
        .background(cream)
        .background(PhotoKeys(compare: studio.setComparing).frame(width: 0, height: 0))
        .onChange(of: textFocused) { _, focused in if focused { studio.beginEditing() } else { studio.endEditing() } }
        .onChange(of: studio.selectedTextRole) { _, _ in studio.endEditing() }
        .onChange(of: studio.textCardScope) { _, _ in studio.endEditing() }
        .sheet(isPresented: $phrases) { PhrasePicker(onPick: studio.setTextValue) { phrases = false } }
        .sheet(isPresented: $expandedText) { TextPreviewEditor(studio: studio) { studio.endEditing(); expandedText = false } }
        .onChange(of: studio.selectedSlot) { _, _ in
            if !studio.editingTemplate, studio.project.placements.indices.contains(studio.selectedSlot), studio.project.placements[studio.selectedSlot] != nil { studio.inspectorTab = 3 }
        }
    }

    private var topbar: some View {
        VStack(spacing: Spacing.s) {
            HStack(spacing: Spacing.m) {
                Image(systemName: "photo.on.rectangle.angled").font(.system(size: 23)).foregroundStyle(ink)
                Text("Polar").font(.custom("Georgia", size: 29)).foregroundStyle(ink).fixedSize()
                TextField("Nombre del diseño", text: Binding(get: { studio.project.name }, set: { value in studio.change { $0.name = String(value.prefix(120)) } }))
                    .textFieldStyle(.plain).font(.system(size: 13, weight: .medium)).frame(maxWidth: .infinity, minHeight: 48).accessibilityLabel("Nombre completo: \(studio.project.name)")
                Spacer()
                Text(studio.saveFailed ? "Sin guardar" : studio.isDirty ? "Guardando…" : "Guardado").font(.system(size: 11)).foregroundStyle(.secondary).fixedSize()
                Button { studio.showLibrary() } label: { Label("Tus diseños", systemImage: "square.grid.2x2") }.disabled(studio.busy)
                SettingsLink { Image(systemName: "gearshape") }.accessibilityLabel("Ajustes")
            }
            HStack(spacing: Spacing.s) {
                Button { studio.undo() } label: { Image(systemName: "arrow.uturn.backward") }.disabled(!studio.canUndo).accessibilityLabel("Deshacer")
                Button { studio.redo() } label: { Image(systemName: "arrow.uturn.forward") }.disabled(!studio.canRedo).accessibilityLabel("Rehacer")
                Menu {
                    Button("Nuevo diseño") { studio.newProject() }
                    Button("Abrir diseño…") { studio.openProject() }
                    Button("Guardar diseño") { studio.saveProject() }
                    Button("Guardar como…") { studio.saveProject(asNew: true) }
                    Divider()
                    Button("Importar plantilla…") { studio.importTemplate() }
                } label: { Label("Proyecto", systemImage: "folder") }
                Button { studio.addPhotos() } label: { Label("Agregar fotos", systemImage: "plus") }
                Spacer()
                Menu {
                    Button("PDF · todas las hojas") { studio.export(.pdf) }
                    Button("JPG · esta hoja · más ligero") { studio.export(.jpeg) }
                    Button("PNG · esta hoja · sin pérdida") { studio.export(.png) }
                } label: { Label("Exportar", systemImage: "square.and.arrow.up") }
                    .menuStyle(.borderedButton).disabled(studio.busy || studio.project.placedCount == 0)
                Button { studio.finish() } label: { Label("Imprimir", systemImage: "printer") }.buttonStyle(PolarButtonStyle(primary: true)).disabled(studio.busy || studio.project.placedCount == 0)
            }
        }
        .buttonStyle(PolarButtonStyle()).controlSize(.regular)
        .padding(.horizontal, Spacing.l).padding(.vertical, Spacing.m)
        .background(polarSurface)
    }

    private var designs: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.m) {
                Text("Elige un diseño").font(.system(size: 16, weight: .semibold))
                Text("Un molde para cada recuerdo.")
                    .font(.system(size: 11)).foregroundStyle(.secondary)
                Picker("Aplicar diseño a", selection: $studio.designScope) {
                    Text("Colección").tag(0)
                    Text("Esta hoja").tag(1)
                    Text("Esta tarjeta").tag(2)
                }.disabled(studio.project.settings.style == .imported)
                Text("La cuadrícula es común. Puedes combinar estilos con la misma cantidad de fotos por tarjeta.").font(.caption).foregroundStyle(.secondary)
                TextField("Buscar diseño", text: $studio.designSearch).textFieldStyle(.roundedBorder).font(.system(size: 12))
                Picker("Categoría", selection: $studio.designCategory) {
                    ForEach(["Todos", "Clásicos", "Música", "Cine", "Fechas", "Ocasiones", "Libre"], id: \.self) { Text($0).tag($0) }
                }.font(.system(size: 12)).foregroundStyle(Color(nsColor: .labelColor))
                LazyVGrid(columns: grid, spacing: Spacing.s) {
                    ForEach(TemplateStyle.allCases.filter { (studio.designSearch.isEmpty || $0.name.localizedCaseInsensitiveContains(studio.designSearch)) && (studio.designCategory == "Todos" || $0.category == studio.designCategory) }) { style in
                        DesignCatalogTile(studio: studio, style: style)
                    }
                }
                Button { studio.importTemplate() } label: { Label("Importar plantilla…", systemImage: "square.and.arrow.down") }.font(.system(size: 12))
                Text("Personaliza texto, color y distribución. Guarda cada diseño para volver a usarlo.")
                    .font(.system(size: 10)).foregroundStyle(.secondary).lineSpacing(3).padding(.top, 4)
            }.padding(16)
        }
        .background(cream)
    }

    private var pageBar: some View {
        HStack(spacing: Spacing.s) {
            Text(studio.project.settingsForPage(studio.page).style.name).font(.system(size: 13, weight: .semibold))
            Text("\(studio.project.settings.capacity) \(studio.project.settings.capacity == 1 ? "foto" : "fotos") por hoja").font(.system(size: 11)).foregroundStyle(.secondary)
            Spacer()
            Button { studio.navigate(studio.page - 1) } label: { Image(systemName: "chevron.left") }
                .disabled(studio.page == 0).accessibilityLabel("Hoja anterior")
            Text("\(studio.page + 1) / \(studio.project.pageCount)").font(.system(size: 12, weight: .medium)).monospacedDigit()
            Button { studio.navigate(studio.page + 1) } label: { Image(systemName: "chevron.right") }
                .disabled(studio.page + 1 == studio.project.pageCount).accessibilityLabel("Hoja siguiente")
            Menu {
                Button("Agregar hoja") { studio.addPage() }
                Button("Vaciar esta hoja") { studio.clearPage() }
                Button("Quitar esta hoja") { studio.removePage() }
            } label: { Image(systemName: "ellipsis.circle") }
                .menuStyle(.borderlessButton).frame(width: 48, height: 48).accessibilityLabel("Opciones de hoja")
        }
        .buttonStyle(.borderless)
        .padding(.horizontal, Spacing.l).padding(.vertical, Spacing.s).background(polarSurface.opacity(0.65))
    }

    private var canvas: some View {
        GeometryReader { geo in
            let paper = studio.project.settings.paperSizePoints
            let scale = max(0.05, min((geo.size.width - 48) / paper.width, (geo.size.height - 38) / paper.height))
            ZStack(alignment: .topLeading) {
                Color.white
                if let image = studio.previewImage {
                    Image(nsImage: image).resizable().interpolation(.high).frame(width: paper.width * scale, height: paper.height * scale)
                }
                let cards = PolarRenderer.cardRects(project: studio.project, page: studio.page)
                ForEach(Array(cards.enumerated()), id: \.offset) { cardIndex, card in
                    let cardSettings = studio.project.settingsForCard(studio.page * studio.project.cardsPerPage + cardIndex)
                    let areas = PolarRenderer.photoRects(in: card, style: cardSettings.style, settings: cardSettings)
                    ForEach(Array(areas.enumerated()), id: \.offset) { areaIndex, area in
                        let index = studio.page * studio.project.settings.capacity + cardIndex * studio.project.settings.style.photosPerCard + areaIndex
                        Button {
                            studio.selectPhotoSlot(index)
                        } label: {
                            ZStack(alignment: .topLeading) {
                                RoundedRectangle(cornerRadius: PolarRadius.small)
                                    .fill(Color.clear)
                                    .contentShape(Rectangle())
                                    .overlay(RoundedRectangle(cornerRadius: PolarRadius.small).stroke((studio.batchSelecting ? studio.selectedSlots.contains(index) : studio.selectedSlot == index) ? ink : Color.clear, lineWidth: 2.5))
                                if studio.selectedSlot == index {
                                    Text("\(index + 1)").font(.system(size: 9, weight: .semibold))
                                        .padding(.horizontal, 5).padding(.vertical, 2)
                                        .foregroundStyle(polarSurface).background(ink, in: RoundedRectangle(cornerRadius: PolarRadius.small))
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        .frame(width: area.width * scale, height: area.height * scale)
                        .offset(x: area.minX * scale, y: area.minY * scale)
                        .accessibilityLabel("Espacio \(index + 1)")
                        .simultaneousGesture(DragGesture(minimumDistance: 4).onEnded { value in
                            if studio.editingTemplate {
                                let bounds = PolarRenderer.templateRect(settings: studio.project.settings)
                                studio.moveTemplateRegion(index: cardIndex, dx: value.translation.width / (bounds.width * scale), dy: value.translation.height / (bounds.height * scale))
                            }
                        })
                    }
                }
            }
            .frame(width: paper.width * scale, height: paper.height * scale)
            .shadow(color: .black.opacity(0.13), radius: 12, x: 0, y: 4)
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
        }
        .background(Color(nsColor: .unemphasizedSelectedContentBackgroundColor))
    }

    private var gallery: some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            HStack {
                Text("Tus fotos").font(.system(size: 13, weight: .semibold))
                Text("\(studio.project.photos.count)").font(.system(size: 11)).foregroundStyle(.secondary)
                Spacer()
                Button("Rellenar todo") { studio.fillAll() }
                    .disabled(studio.project.photos.isEmpty).buttonStyle(.borderless)
            }.font(.system(size: 11)).frame(minHeight: 48)
            VStack(alignment: .leading, spacing: Spacing.s) {
                HStack {
                Toggle("Seleccionar varias", isOn: $studio.batchSelecting).toggleStyle(.checkbox)
                    if studio.batchSelecting { Text("\(studio.selectedSlots.count) seleccionadas").font(.caption) }
                }
                if studio.batchSelecting {
                    HStack(spacing: Spacing.s) {
                    Button("Todas en esta hoja") { studio.selectAllOnPage() }
                    Button("Quitar") { studio.removeSelectedPhotos() }.disabled(studio.selectedSlots.isEmpty)
                    Button("Copiar a hoja nueva") { studio.copySelectedPhotos() }.disabled(studio.selectedSlots.isEmpty)
                    }
                }
            }.frame(minHeight: 48)
            if studio.project.photos.isEmpty {
                HStack(spacing: Spacing.m) {
                    Image(systemName: "photo.badge.plus").font(.system(size: 28)).foregroundStyle(ink)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Arrastra tus fotos o una carpeta aquí").font(.system(size: 13, weight: .medium))
                        Text("También puedes usar Agregar fotos.").font(.system(size: 11)).foregroundStyle(.secondary)
                    }
                    Spacer()
                }.frame(height: 82)
            } else {
                ScrollView(.horizontal) {
                    LazyHStack(spacing: Spacing.s) {
                        ForEach(studio.project.photos.filter { $0.isBackground != true }) { photo in
                            Button { studio.put(photo) } label: {
                                VStack(spacing: Spacing.xs) {
                                    ZStack(alignment: .bottomTrailing) {
                                        if let image = PhotoImporter.thumbnail(for: photo) {
                                            Image(nsImage: image).resizable().scaledToFill().frame(width: 76, height: 76).clipped()
                                        } else { Image(systemName: "photo").frame(width: 76, height: 76).background(cream) }
                                        if studio.project.placements.contains(where: { $0?.assetID == photo.id }) {
                                            Image(systemName: "checkmark.circle.fill").foregroundStyle(.white, ink)
                                                .font(.system(size: 14)).padding(3)
                                        }
                                        if let badge = studio.placedQuality(of: photo)?.badge {
                                            Text(badge).font(.system(size: 9, weight: .bold)).foregroundStyle(.white)
                                                .padding(.horizontal, 4).padding(.vertical, 1)
                                                .background(badge == "Baja" ? Color.red : Color.orange, in: Capsule())
                                                .padding(3).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                                        }
                                    }.clipShape(RoundedRectangle(cornerRadius: PolarRadius.small))
                                    Text(photo.name).font(.system(size: 10)).lineLimit(1).frame(width: 76)
                                }
                            }.buttonStyle(.plain).accessibilityLabel("Colocar \(photo.name)" + (studio.placedQuality(of: photo)?.badge.map { ". Resolución \($0.lowercased())" } ?? ""))
                        }
                    }
                }.scrollIndicators(.hidden)
            }
            Text("Selecciona un espacio de la hoja y haz clic en la foto que quieras colocar.")
                .font(.system(size: 10)).foregroundStyle(.secondary)
        }
        .padding(.horizontal, Spacing.l).padding(.vertical, Spacing.m)
        .background(studio.dropTarget ? ink.opacity(0.1) : polarSurface)
        .onDrop(of: ["public.file-url"], isTargeted: $studio.dropTarget) { providers in
            let group = DispatchGroup(), lock = NSLock()
            var urls: [URL] = []
            for provider in providers {
                group.enter()
                provider.loadItem(forTypeIdentifier: "public.file-url", options: nil) { item, _ in
                    let url: URL?
                    if let data = item as? Data { url = URL(dataRepresentation: data, relativeTo: nil) }
                    else { url = item as? URL }
                    if let url, url.isFileURL { lock.lock(); urls.append(url); lock.unlock() }
                    group.leave()
                }
            }
            group.notify(queue: .main) { studio.importURLs(urls, fill: true) }
            return true
        }
    }

    private var inspector: some View {
        VStack(spacing: 0) {
            FileTabs(selection: $studio.inspectorTab)
            if let preset = studio.suggestedLook {
                VStack(alignment: .leading, spacing: Spacing.s) {
                    Text("Las películas lucen mejor en blanco y negro").font(.callout).foregroundStyle(.primary)
                    Button("Aplicar") { studio.applyLook(PhotoLook(preset: preset), all: true); studio.suggestedLook = nil }
                }.padding(Spacing.m).background(polarCream)
            }
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.m) {
                    switch studio.inspectorTab {
                    case 0: designControls
                    case 1: textControls
                    case 2: paperControls
                    case 3: PhotoTools(studio: studio)
                    default: FiltersTools(studio: studio)
                    }
                }.padding(.horizontal, Spacing.m).padding(.bottom, Spacing.l)
            }
        }.background(polarSurface)
    }

    private var designControls: some View {
        VStack(alignment: .leading, spacing: 16) {
            section("Hazlo tuyo")
            if studio.project.settings.style == .imported { templateControls }
            else {
                Menu {
                    ForEach(MoodPreset.allCases) { mood in
                        Button(mood.name) { studio.applyMood(mood) }
                    }
                } label: { Label("Estilos de texto", systemImage: "wand.and.stars") }
                Text("Cambia la tipografía y los colores sin reemplazar tus frases.")
                    .font(.system(size: 11)).foregroundStyle(.secondary)
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: Spacing.s) { suggestedTextActions }
                    VStack(spacing: Spacing.s) { suggestedTextActions }
                }.font(.system(size: 12))
                Divider()
                section("Color del diseño")
                palette(selected: studio.project.settings.accentHex) { value in studio.change { $0.settings.accentHex = value } }
                ColorPicker("Otro color", selection: accentColor, supportsOpacity: false)
                Divider()
                section("Fotos por hoja")
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 54))], spacing: 8) {
                    ForEach([1, 2, 4, 6, 8, 9, 12, 16], id: \.self) { count in
                        Button("\(count)") { studio.applyLayout(count) }
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .overlay(alignment: .topTrailing) { if studio.project.settings.columns * studio.project.settings.rows == count { Image(systemName: "checkmark").font(.caption).foregroundStyle(ink) } }
                            .tint(studio.project.settings.columns * studio.project.settings.rows == count ? ink : .secondary)
                            .accessibilityLabel("Distribución de \(count) \(count == 1 ? "diseño" : "diseños") por hoja")
                    }
                }
                Text("En los diseños de película, cada tira reúne cinco fotos.")
                    .font(.system(size: 11)).foregroundStyle(.secondary)
                Picker("Formato", selection: Binding(get: { studio.designSettings.cardFormat }, set: { studio.setDesignFormat($0) })) {
                    ForEach(CardFormat.allCases) { value in Text(value.name).tag(value) }
                }
                VStack(spacing: Spacing.s) {
                    Stepper("Columnas \(studio.project.settings.columns)", value: setting(\.columns), in: 1...4).frame(minHeight: 48)
                    Stepper("Filas \(studio.project.settings.rows)", value: setting(\.rows), in: 1...6).frame(minHeight: 48)
                }.font(.system(size: 12))
                controlSlider("Separación", value: setting(\.gap), range: 0...30, suffix: "pt")
                Toggle("Fotos con esquinas redondas", isOn: setting(\.roundedPhotos))
                if studio.project.settings.style == .calendar {
                    Divider()
                    Stepper("Año \(String(studio.project.settings.calendarYear))", value: setting(\.calendarYear), in: 1900...2100)
                    Toggle("Marcar una fecha especial", isOn: setting(\.highlightDate))
                    if studio.project.settings.highlightDate {
                        DatePicker("Fecha", selection: setting(\.specialDate), displayedComponents: .date).datePickerStyle(.field)
                    }
                }
            }
            Divider()
            Text("Los espacios vacíos aparecen sólo al editar; no se imprimen.")
                .font(.system(size: 11)).foregroundStyle(.secondary).lineSpacing(3)
            Button("Guardar este diseño…") { studio.saveProject(asNew: true) }.frame(maxWidth: .infinity, minHeight: 48)
        }.font(.system(size: 13)).textFieldStyle(.roundedBorder)
    }

    private var textControls: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            section("Texto y tipografía")
            if studio.textRoles.isEmpty {
                Text(studio.project.settings.style == .imported ? "El texto que ya viene impreso en la plantilla forma parte de la imagen." : "Este diseño reserva todo el espacio para las fotografías.")
                    .font(.system(size: 13)).foregroundStyle(.secondary)
            } else {
                Picker("Texto", selection: $studio.selectedTextRole) {
                    ForEach(studio.textRoles) { role in Text(role.name).tag(role) }
                }
                Picker("Aplicar a", selection: $studio.textCardScope) {
                    Text("Todas las tarjetas").tag(false)
                    Text("Sólo tarjeta \(studio.selectedCard + 1)").tag(true)
                }
                if studio.selectedTextRole == .date {
                    Picker("Fecha", selection: Binding(get: { studio.dateSource }, set: { studio.setDateSource($0) })) {
                        ForEach(DateSource.allCases) { Text($0.name).tag($0) }
                    }
                    if studio.dateSource == .chosen {
                        DatePicker("Elegir fecha", selection: Binding(get: { studio.chosenDate }, set: { studio.setChosenDate($0) }), displayedComponents: .date)
                    }
                    Picker("Formato", selection: setting(\.dateStyle)) { ForEach(DateStyle.allCases) { Text($0.name).tag($0) } }
                    Text("La fecha de la foto usa sus datos de captura; si no existen, no se imprime.").font(.caption).foregroundStyle(.secondary)
                } else {
                    TextField(studio.selectedTextRole.name, text: textValue, axis: .vertical).lineLimit(2...4).focused($textFocused)
                    HStack(spacing: Spacing.s) {
                        Button("Editar y ver") { studio.beginEditing(); expandedText = true }
                        Button("Frases sugeridas") { phrases = true }
                    }.frame(minHeight: 48)
                }
                if studio.textCardScope { Button("Volver al texto general") { studio.returnToGeneralText() }.font(.system(size: 12)) }
                else if !studio.project.cardOverrides.isEmpty { Button("Aplicar texto y estilo general a todas") { studio.applyGeneralTextToAll() }.font(.system(size: 12)) }
                Toggle("Mostrar este texto", isOn: textSetting(\.visible))
                Divider()
                TextField("Buscar fuente", text: $studio.fontSearch).font(.system(size: 12))
                Picker("Fuente", selection: textSetting(\.fontName)) {
                    ForEach(studio.fontChoices.filter { studio.fontSearch.isEmpty || $0.name.localizedCaseInsensitiveContains(studio.fontSearch) || $0.id == textSetting(\.fontName).wrappedValue }) { choice in Text(choice.name).tag(choice.id) }
                }
                Text(textValue.wrappedValue.isEmpty ? "Así se verá tu texto" : textValue.wrappedValue)
                    .font(studio.textAppearance(studio.selectedTextRole).fontName == ".System" ? .system(size: 23) : .custom(FontCatalog.postScriptName(studio.textAppearance(studio.selectedTextRole).fontName), size: 23))
                    .foregroundStyle(Color(nsColor: color(studio.textAppearance(studio.selectedTextRole).hex.isEmpty ? studio.project.settings.accentHex : studio.textAppearance(studio.selectedTextRole).hex)))
                    .lineLimit(2).frame(maxWidth: .infinity, alignment: .leading).padding(Spacing.m).background(Color.white, in: RoundedRectangle(cornerRadius: PolarRadius.small))
                Toggle("Tamaño automático", isOn: automaticTextSize)
                if !automaticTextSize.wrappedValue {
                    Stepper("Tamaño \(Int(textSetting(\.size).wrappedValue)) pt", value: textSetting(\.size), in: 6...96, step: 1)
                    Slider(value: textSetting(\.size), in: 6...96, step: 1, onEditingChanged: { editing in if editing { studio.beginEditing() } else { studio.endEditing() } })
                }
                HStack(spacing: Spacing.m) {
                    Toggle("Negrita", isOn: textSetting(\.bold)).frame(maxWidth: .infinity, minHeight: 48)
                    Toggle("Cursiva", isOn: textSetting(\.italic)).frame(maxWidth: .infinity, minHeight: 48)
                }
                Picker("Alineación", selection: textSetting(\.alignment)) {
                    ForEach(TextAlignment.allCases) { value in Text(value.name).tag(value) }
                }
                ColorPicker("Color del texto", selection: textColor, supportsOpacity: false)
                Button("Usar color del diseño") { studio.editText { $0.hex = "" } }.font(.system(size: 12))
                Divider()
                section("Posición del texto")
                controlSlider("Horizontal", value: textSetting(\.offsetX), range: -60...60, suffix: "pt")
                controlSlider("Vertical", value: textSetting(\.offsetY), range: -60...60, suffix: "pt")
                Text("Si el texto no cabe, se reduce para ajustarse a su zona sin cubrir la foto.")
                    .font(.system(size: 11)).foregroundStyle(.secondary)
                Button("Restablecer este texto") { studio.resetTextStyle() }
                if studio.project.settings.style == .spotify {
                    Divider()
                    field("Enlace de la canción (opcional)", \.songURL)
                    if PolarRenderer.qrState(studio.project.settings.songURL) == .tooLong {
                        Label("Enlace muy largo para un QR. Usa uno más corto; en la hoja aparecerá un aviso en su lugar.", systemImage: "exclamationmark.triangle")
                            .font(.system(size: 11)).foregroundStyle(.orange)
                    } else {
                        Text("Incluye un QR que abre ese enlace.").font(.system(size: 11)).foregroundStyle(.secondary)
                    }
                }
            }
        }.font(.system(size: 13)).textFieldStyle(.roundedBorder)
    }

    private var paperControls: some View {
        VStack(alignment: .leading, spacing: 16) {
            section("Hoja de impresión")
            Picker("Papel", selection: setting(\.paperSize)) {
                ForEach(PaperSize.allCases) { value in Text(value.name).tag(value) }
            }
            Picker("Orientación", selection: setting(\.orientation)) {
                ForEach(PaperOrientation.allCases) { value in Text(value.name).tag(value) }
            }.pickerStyle(.segmented)
            if studio.project.settings.paperSize == .custom {
                HStack {
                    VStack(alignment: .leading) {
                        Text("Ancho (\(studio.preferences.units.abbreviation))").font(.system(size: 11)).foregroundStyle(.secondary)
                        TextField("Ancho", value: paperMeasure(\.customWidthMM), format: .number).onSubmit { studio.clampPaperSize() }
                    }
                    VStack(alignment: .leading) {
                        Text("Alto (\(studio.preferences.units.abbreviation))").font(.system(size: 11)).foregroundStyle(.secondary)
                        TextField("Alto", value: paperMeasure(\.customHeightMM), format: .number).onSubmit { studio.clampPaperSize() }
                    }
                }
                Text("Medidas entre 80 y 600 mm.").font(.system(size: 11)).foregroundStyle(.secondary)
            }
            Text(studio.paperDescription).font(.system(size: 12)).foregroundStyle(.secondary)
            controlSlider("Margen", value: pointMeasure(\.margin), range: 0...(60 / studio.preferences.units.pointsPerUnit), suffix: studio.preferences.units.abbreviation)
            Divider()
            section("Para recortar")
            Toggle("Marcas de corte", isOn: setting(\.cutGuides))
            if studio.project.settings.cutGuides {
                Picker("Guías", selection: setting(\.cutStyle)) {
                    ForEach(CutStyle.allCases) { value in Text(value.name).tag(value) }
                }
            }
            Toggle("Imprimir borde de las tarjetas", isOn: setting(\.drawBorders))
            Text("Las marcas en esquinas quedan fuera del diseño. Desactiva el borde para que no aparezca en la foto recortada.")
                .font(.system(size: 11)).foregroundStyle(.secondary).lineSpacing(3)
            Divider()
            section("Para imprimir")
            Text("Elige este mismo tamaño de papel en la impresora y usa Tamaño real o escala 100 %.")
                .font(.system(size: 12)).foregroundStyle(.secondary).lineSpacing(3)
            Text("La calidad de PDF y JPG se elige en Terminar: \(studio.preferences.exportQuality.title). PNG conserva los píxeles renderizados y pesa más.").font(.system(size: 11)).foregroundStyle(.secondary)
        }.font(.system(size: 13)).textFieldStyle(.roundedBorder)
    }

    private var templateControls: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            if let template = studio.project.settings.importedTemplate {
                Text(URL(fileURLWithPath: template.path).lastPathComponent).font(.system(size: 12)).lineLimit(2)
                Toggle("Editar los huecos", isOn: $studio.editingTemplate)
                Text("Selecciona un hueco de la hoja. Puedes arrastrarlo o ajustar sus medidas aquí.")
                    .font(.system(size: 11)).foregroundStyle(.secondary)
                let local = studio.selectedSlot % studio.project.settings.capacity
                if template.regions.indices.contains(local) {
                    Text("Hueco \(local + 1) de \(template.regions.count)").font(.system(size: 13, weight: .medium))
                    controlSlider("Izquierda", value: regionSetting(\.x), range: 0...98, suffix: "%")
                    controlSlider("Arriba", value: regionSetting(\.y), range: 0...98, suffix: "%")
                    controlSlider("Ancho", value: regionSetting(\.width), range: 2...100, suffix: "%")
                    controlSlider("Alto", value: regionSetting(\.height), range: 2...100, suffix: "%")
                }
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: Spacing.s) { templateActions(template) }
                    VStack(spacing: Spacing.s) { templateActions(template) }
                }.font(.system(size: 12)).buttonStyle(PolarButtonStyle(expands: true))
                Button("Importar otra plantilla…") { studio.importTemplate() }
            }
        }
    }

    @ViewBuilder private var suggestedTextActions: some View {
        ActionTile(title: "Frases sugeridas", icon: "quote.bubble") { phrases = true; studio.inspectorTab = 1 }.disabled(studio.textRoles.isEmpty)
        ActionTile(title: "Editar texto", icon: "textformat") { studio.inspectorTab = 1 }.disabled(studio.textRoles.isEmpty)
    }
    @ViewBuilder private func templateActions(_ template: ImportedTemplate) -> some View {
        Button("Añadir hueco") { studio.addTemplateRegion() }.frame(maxWidth: .infinity, minHeight: 48).disabled(template.regions.count >= 64)
        Button("Quitar hueco") { studio.removeTemplateRegion() }.frame(maxWidth: .infinity, minHeight: 48).disabled(template.regions.count <= 1)
    }

    private var textValue: Binding<String> {
        Binding(get: { studio.textValue(studio.selectedTextRole) }, set: { studio.setTextValue($0) })
    }
    private func textSetting<T>(_ key: WritableKeyPath<TextAppearance, T>) -> Binding<T> {
        Binding(get: { studio.textAppearance(studio.selectedTextRole)[keyPath: key] }, set: { value in
            studio.editText { $0[keyPath: key] = value }
        })
    }
    private var automaticTextSize: Binding<Bool> {
        Binding(get: { studio.textAppearance(studio.selectedTextRole).size == 0 }, set: { value in
            studio.editText { $0.size = value ? 0 : 12 }
        })
    }
    private var accentColor: Binding<Color> {
        Binding(get: { Color(nsColor: color(studio.project.settings.accentHex)) }, set: { value in
            studio.change { $0.settings.accentHex = hex(value) }
        })
    }
    private var textColor: Binding<Color> {
        Binding(get: {
            let appearance = studio.textAppearance(studio.selectedTextRole)
            return Color(nsColor: color(appearance.hex.isEmpty ? studio.project.settings.accentHex : appearance.hex))
        }, set: { value in studio.editText { $0.hex = hex(value) } })
    }
    private func regionSetting(_ key: WritableKeyPath<TemplateRegion, Double>) -> Binding<Double> {
        Binding(get: {
            guard let template = studio.project.settings.importedTemplate else { return 0 }
            let index = studio.selectedSlot % studio.project.settings.capacity
            return template.regions.indices.contains(index) ? template.regions[index][keyPath: key] * 100 : 0
        }, set: { value in studio.editTemplateRegion { $0[keyPath: key] = value / 100 } })
    }
    private func hex(_ value: Color) -> String {
        let rgb = NSColor(value).usingColorSpace(.sRGB) ?? .black
        return String(format: "%02X%02X%02X", Int((rgb.redComponent * 255).rounded()), Int((rgb.greenComponent * 255).rounded()), Int((rgb.blueComponent * 255).rounded()))
    }
    private func palette(selected: String, apply: @escaping (String) -> Void) -> some View {
        HStack(spacing: Spacing.xs) {
            ForEach(["92394A", "C34048", "486855", "38536F", "20242C", "BC8952"], id: \.self) { value in
                Button { apply(value) } label: {
                    Circle().fill(Color(nsColor: color(value))).frame(width: 32, height: 32)
                        .overlay(Circle().stroke(.white, lineWidth: 2).padding(2))
                        .overlay(Circle().stroke(selected == value ? polarInk : Color.clear, lineWidth: 2))
                        .overlay { if selected == value { Image(systemName: "checkmark").font(.system(size: 14, weight: .bold)).foregroundStyle(Color.white).shadow(color: .black, radius: 2) } }
                        .frame(width: 48, height: 48)
                }.buttonStyle(.plain)
                    .accessibilityLabel(["92394A": "Vino", "C34048": "Rojo", "486855": "Verde", "38536F": "Azul", "20242C": "Carbón", "BC8952": "Dorado"][value] ?? value)
                    .accessibilityValue(selected == value ? "Seleccionado" : "")
            }
        }
    }

    private func setting<T>(_ key: WritableKeyPath<PrintSettings, T>) -> Binding<T> {
        Binding(get: { studio.project.settings[keyPath: key] }, set: { value in
            studio.change { project in
                project.settings[keyPath: key] = value
                if key == \PrintSettings.customWidthMM || key == \PrintSettings.customHeightMM {
                    project.settings.customWidthMM = min(600, max(80, project.settings.customWidthMM.isFinite ? project.settings.customWidthMM : 215.9))
                    project.settings.customHeightMM = min(600, max(80, project.settings.customHeightMM.isFinite ? project.settings.customHeightMM : 279.4))
                }
                if key == \PrintSettings.columns || key == \PrintSettings.rows {
                    while project.placements.last.map({ $0 == nil }) == true { project.placements.removeLast() }
                }
            }
        })
    }
    private func paperMeasure(_ key: WritableKeyPath<PrintSettings, Double>) -> Binding<Double> {
        Binding(get: { studio.project.settings[keyPath: key] / (studio.preferences.units == .mm ? 1 : 25.4) }, set: { value in
            setting(key).wrappedValue = value * (studio.preferences.units == .mm ? 1 : 25.4)
        })
    }
    private func pointMeasure(_ key: WritableKeyPath<PrintSettings, Double>) -> Binding<Double> {
        Binding(get: { studio.project.settings[keyPath: key] / studio.preferences.units.pointsPerUnit }, set: { value in
            setting(key).wrappedValue = value * studio.preferences.units.pointsPerUnit
        })
    }
    private func placementBinding(_ key: WritableKeyPath<PhotoPlacement, Double>) -> Binding<Double> {
        Binding(get: { studio.project.placements[studio.selectedSlot]?[keyPath: key] ?? 1 }, set: { value in
            studio.editPlacement { $0[keyPath: key] = value }
        })
    }
    private func field(_ title: String, _ key: WritableKeyPath<PrintSettings, String>) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(title).font(.system(size: 12)).foregroundStyle(.secondary)
            TextField(title, text: setting(key)).font(.system(size: 13))
        }
    }
    private func section(_ title: String) -> some View { Text(title).font(.system(size: 14, weight: .semibold)) }
    private func controlSlider(_ name: String, value: Binding<Double>, range: ClosedRange<Double>, suffix: String) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack(alignment: .firstTextBaseline) {
                Text(name).frame(maxWidth: .infinity, alignment: .leading)
                Text(String(format: (suffix == "pt" || suffix == "%") ? "%.0f %@" : "%.2f%@", value.wrappedValue, suffix)).foregroundStyle(.secondary).monospacedDigit()
            }.font(.system(size: 12))
            Slider(value: value, in: range, onEditingChanged: { editing in if editing { studio.beginEditing() } else { studio.endEditing() } }).controlSize(.small).frame(minHeight: 48)
        }
    }
    private func color(_ hex: String) -> NSColor {
        let rgb = UInt32(hex, radix: 16) ?? 0x92394A
        return NSColor(srgbRed: CGFloat((rgb >> 16) & 255) / 255, green: CGFloat((rgb >> 8) & 255) / 255, blue: CGFloat(rgb & 255) / 255, alpha: 1)
    }
}
