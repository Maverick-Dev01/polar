import SwiftUI
import AppKit

@MainActor struct PhrasePicker: View {
    let onPick: (String) -> Void
    let onDismiss: () -> Void
    @NativeState<String> private var query = ""
    @NativeState<String> private var category = "Todas"
    @NativeState<String?> private var draft = nil
    @NativeState<NSRange> private var selection = NSRange(location: 0, length: 0)
    private let phrases = SuggestedPhrase.load()
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            Text("Frases para tus recuerdos").font(.title2)
            if let draft {
                Text("Edita o selecciona sólo el fragmento que quieres imprimir.").foregroundStyle(.secondary)
                PhraseTextEditor(text: Binding(get: { self.draft ?? "" }, set: { self.draft = String($0.prefix(500)) }), selection: $selection).frame(minHeight: 160)
                Text("\(draft.count) / 500 caracteres").font(.caption).foregroundStyle(.secondary)
                Button(selection.length > 0 ? "Usar selección (\(selection.length))" : "Usar este texto") {
                    let value = draft as NSString
                    let range = NSIntersectionRange(selection, NSRange(location: 0, length: value.length))
                    onPick(range.length > 0 ? value.substring(with: range) : draft); onDismiss()
                }.disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                Button("Volver al catálogo") { self.draft = nil; selection = NSRange(location: 0, length: 0) }
            } else {
                Text("120 frases originales de Polar, editables y disponibles sin conexión.").font(.callout).foregroundStyle(.secondary)
                TextField("Buscar tema o palabras", text: $query).textFieldStyle(.roundedBorder)
                Picker("Categoría", selection: $category) { ForEach(["Todas"] + Array(Set(phrases.map(\.category))).sorted(), id: \.self) { Text($0).tag($0) } }
                ScrollView { LazyVStack(alignment: .leading, spacing: Spacing.s) { ForEach(phrases.filter { (category == "Todas" || $0.category == category) && (query.isEmpty || "\($0.text) \($0.tags)".localizedCaseInsensitiveContains(query)) }) { phrase in
                    Button { self.draft = phrase.text; selection = NSRange(location: 0, length: 0) } label: {
                        VStack(alignment: .leading, spacing: 6) { Text(phrase.text); Text(phrase.category).font(.caption).foregroundStyle(.secondary) }.padding(12).frame(maxWidth: .infinity, alignment: .leading).background(polarSurface, in: RoundedRectangle(cornerRadius: 8))
                    }.buttonStyle(.plain).accessibilityLabel(phrase.text)
                } } }.frame(minHeight: 240)
                Button("Escribir mi propia frase") { self.draft = "" }
            }
            Button("Cerrar", action: onDismiss)
        }.padding(Spacing.l).frame(width: 600, height: 560).background(polarCream).tint(polarInk)
    }
}

private struct PhraseTextEditor: NSViewRepresentable {
    @Binding var text: String
    @Binding var selection: NSRange
    func makeCoordinator() -> Coordinator { Coordinator(self) }
    func makeNSView(context: Context) -> NSScrollView {
        let scroll = NSScrollView(); scroll.hasVerticalScroller = true; scroll.borderType = .bezelBorder
        let input = NSTextView(); input.isRichText = false; input.isAutomaticQuoteSubstitutionEnabled = false
        input.font = .systemFont(ofSize: 18); input.textColor = .textColor; input.backgroundColor = .textBackgroundColor
        input.string = text; input.delegate = context.coordinator; input.isVerticallyResizable = true
        input.autoresizingMask = [.width]; input.textContainer?.widthTracksTextView = true; input.setAccessibilityLabel("Tu frase")
        scroll.documentView = input; return scroll
    }
    func updateNSView(_ view: NSScrollView, context: Context) {
        context.coordinator.parent = self
        if let input = view.documentView as? NSTextView, input.string != text { input.string = text }
    }
    class Coordinator: NSObject, NSTextViewDelegate {
        var parent: PhraseTextEditor
        init(_ parent: PhraseTextEditor) { self.parent = parent }
        func textDidChange(_ notification: Notification) { if let view = notification.object as? NSTextView { parent.text = view.string } }
        func textViewDidChangeSelection(_ notification: Notification) { if let view = notification.object as? NSTextView { parent.selection = view.selectedRange() } }
    }
}

@MainActor struct BackgroundControls: View {
    @ObservedObject var studio: Studio
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.m) {
            Divider(); Text("Fondo de la foto").font(.headline)
            if let options = studio.project.placements[studio.selectedSlot]?.background {
                HStack {
                    Button("Blanco") { studio.editBackground { _ in var b = options; b.colorHex = "FFFFFF"; b.imageID = nil; return b } }
                    Button("Negro") { studio.editBackground { _ in var b = options; b.colorHex = "000000"; b.imageID = nil; return b } }
                    Button("Transparente") { studio.editBackground { _ in var b = options; b.colorHex = nil; b.imageID = nil; return b } }
                }
                ColorPicker("Otro color", selection: Binding(get: { Color(nsColor: PolarRenderer.color(options.colorHex ?? "FFFFFF")) }, set: { value in
                    let color = NSColor(value).usingColorSpace(.sRGB) ?? .white
                    let hex = String(format: "%02X%02X%02X", Int((color.redComponent*255).rounded()), Int((color.greenComponent*255).rounded()), Int((color.blueComponent*255).rounded()))
                    studio.editBackground { _ in var b = options; b.colorHex = hex; b.imageID = nil; return b }
                }), supportsOpacity: false)
                Button("Color de la foto") {
                    if let photo = studio.project.asset(for: studio.project.placements[studio.selectedSlot]), let hex = BackgroundRemover.matchingColor(photo: photo) {
                        studio.editBackground { _ in var b = options; b.colorHex = hex; b.imageID = nil; return b }
                    }
                }
                Button("Agregar una foto de fondo…") { studio.addBackgroundPhoto() }
                Text("Suavizar borde")
                Slider(value: Binding(get: { options.feather }, set: { value in studio.editBackground { _ in var b = options; b.feather = value; return b } }), in: 0...1, onEditingChanged: { if $0 { studio.beginEditing() } else { studio.endEditing() } })
                Text("Sombra suave")
                Slider(value: Binding(get: { options.shadow }, set: { value in studio.editBackground { _ in var b = options; b.shadow = value; return b } }), in: 0...1, onEditingChanged: { if $0 { studio.beginEditing() } else { studio.endEditing() } })
                Button("Restaurar fondo original") { studio.editBackground { _ in nil } }
            } else {
                Button("Quitar fondo") { studio.removeBackground() }.disabled(studio.busy)
                Text("Se conserva el original y el detalle del rostro. Revisa el borde del cabello y los fondos complejos.").font(.caption).foregroundStyle(.secondary)
            }
            if studio.busy { ProgressView("Preparando el recorte…") }
        }.frame(maxWidth: 800)
    }
}

@MainActor struct TextPreviewEditor: View {
    @ObservedObject var studio: Studio
    let onDismiss: () -> Void
    @NativeState<NSImage?> private var image = nil
    @NativeState<Double> private var zoom = 1
    var body: some View {
        VStack(spacing: Spacing.m) {
            Text("Editar \(studio.selectedTextRole.name.lowercased())").font(.title2)
            if let image { ScrollView([.horizontal, .vertical]) { Image(nsImage: image).resizable().scaledToFit().frame(width: 500*zoom, height: 280*zoom) }.frame(height: 280) }
            HStack { Text("Ampliar vista"); Slider(value: $zoom, in: 1...4); Button("Restablecer") { zoom = 1 } }
            TextField("Texto que se imprimirá", text: Binding(get: { studio.textValue(studio.selectedTextRole) }, set: { studio.setTextValue($0) }), axis: .vertical).lineLimit(2...5).textFieldStyle(.roundedBorder)
            Button("Listo", action: onDismiss)
        }.padding(Spacing.l).frame(width: 600, height: 520).background(polarCream)
        .task(id: studio.project) {
            let snapshot = studio.project, slot = studio.selectedSlot
            let next = await Task.detached { PolarRenderer.cardPreview(project: snapshot, slot: slot, width: 1000) }.value
            if !Task.isCancelled { image = next }
        }
    }
}
