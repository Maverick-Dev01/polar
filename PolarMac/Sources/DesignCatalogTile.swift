import SwiftUI

extension TemplateStyle {
    var designDescription: String {
        switch self {
        case .polaroid, .mini, .square: return "Un recuerdo con marco y dedicatoria."
        case .spotify, .playerRed, .playerGray: return "Tu foto y la canción que la acompaña."
        case .filmVertical, .filmHorizontal: return "Cinco recuerdos en una tira de película."
        case .calendar: return "Una foto para cada mes del año."
        case .ticket: return "Un boleto para recordar una ocasión."
        case .instagram: return "Tu recuerdo como una publicación."
        case .borderless: return "La foto ocupa todo el espacio."
        case .imported: return "Coloca tus fotos en tu propio molde."
        case .postcard: return "Una foto y un mensaje para compartir."
        case .botanical: return "Un marco inspirado en la naturaleza."
        case .celebration: return "Una ocasión especial para guardar."
        case .pets: return "Tu mejor compañía, en papel."
        case .heart: return "Un recuerdo en forma de corazón."
        case .editorial: return "Fotos y frases como en una revista."
        case .custom: return "Tu propia distribución de recuerdos."
        }
    }
}

@MainActor struct DesignCatalogTile: View {
    @ObservedObject var studio: Studio
    let style: TemplateStyle
    private var selected: Bool { studio.designSettings.style == style }
    var body: some View {
        Button { studio.chooseStyle(style); studio.inspectorTab = 0 } label: {
            VStack(alignment: .leading, spacing: Spacing.s) {
                Color.clear.aspectRatio(1, contentMode: .fit).overlay {
                    if let image = studio.thumbnail(style) { Image(nsImage: image).resizable().scaledToFit() }
                    else { Image(systemName: style.symbol).font(.title).foregroundStyle(polarInk) }
                }.background(polarCream, in: RoundedRectangle(cornerRadius: PolarRadius.small)).clipped()
                Text(style.name).font(.caption.bold()).lineLimit(1, reservesSpace: true)
                Text(style.designDescription).font(.system(size: 10)).foregroundStyle(.secondary).lineLimit(2, reservesSpace: true)
            }.padding(Spacing.m).frame(maxWidth: .infinity)
                .background(polarSurface, in: RoundedRectangle(cornerRadius: PolarRadius.card))
                .overlay(RoundedRectangle(cornerRadius: PolarRadius.card).stroke(selected ? polarInk : Color.secondary.opacity(0.25), lineWidth: selected ? 2 : 1))
                .overlay(alignment: .topTrailing) { if selected { Image(systemName: "checkmark.circle.fill").foregroundStyle(polarInk).padding(Spacing.xs) } }
        }.buttonStyle(.plain).accessibilityLabel("\(style.name). \(style.designDescription)").accessibilityAddTraits(selected ? .isSelected : [])
    }
}
