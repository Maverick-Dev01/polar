import AppKit
import CoreText

enum FontCatalog {
    static let bundledChoices: [(id: String, name: String)] = [
        "Caveat", "Kalam", "Homemade Apple", "Sacramento", "Dancing Script", "Patrick Hand", "Shadows Into Light", "Amatic SC",
        "Gelasio", "Libre Baskerville", "EB Garamond", "Josefin Sans", "Nunito", "Quicksand", "Montserrat",
        "Special Elite", "Courier Prime", "Abril Fatface", "Pacifico", "Lobster"
    ].map { ($0, $0) }

    private static let aliases = [
        "Georgia": "Gelasio", "Baskerville": "Libre Baskerville", "AvenirNext-Medium": "Josefin Sans",
        "Avenir Next": "Josefin Sans", "ChalkboardSE-Regular": "Patrick Hand", "SnellRoundhand": "Dancing Script",
        "Courier": "Courier Prime", "serif": "Gelasio", "sans-serif-medium": "Montserrat", "casual": "Patrick Hand", "cursive": "Dancing Script"
    ]
    /// Where bundled fonts are looked up: the app's Resources/fonts, plus any directories in POLAR_FONT_DIRS
    /// (colon separated; used by check.sh so the test binaries find the fonts without a bundle).
    static var fontDirectories: [URL] {
        let extra = (ProcessInfo.processInfo.environment["POLAR_FONT_DIRS"] ?? "").split(separator: ":").map { URL(fileURLWithPath: String($0)) }
        return [Bundle.main.resourceURL?.appendingPathComponent("fonts")].compactMap { $0 } + extra
    }
    private static let registered: [String: String] = {
        var names: [String: String] = [:]
        for choice in bundledChoices {
            let filename = choice.id.lowercased().replacingOccurrences(of: " ", with: "_") + ".ttf"
            let candidates = fontDirectories.map { $0.appendingPathComponent(filename) }
            guard let url = candidates.first(where: { FileManager.default.isReadableFile(atPath: $0.path) }),
                  let descriptors = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor],
                  let descriptor = descriptors.first, let name = CTFontDescriptorCopyAttribute(descriptor, kCTFontNameAttribute) as? String else { continue }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
            names[choice.id] = name
        }
        return names
    }()
    static func registerFonts() { _ = registered }
    static func postScriptName(_ id: String) -> String { registered[aliases[id] ?? id] ?? id }
    static func font(_ id: String, size: CGFloat) -> NSFont? {
        guard id != ".System", let base = NSFont(name: postScriptName(id), size: size) else { return nil }
        let descriptor = base.fontDescriptor.addingAttributes([.variation: [NSNumber(value: 0x77676874): NSNumber(value: 400)]])
        return NSFont(descriptor: descriptor, size: size) ?? base
    }
}
