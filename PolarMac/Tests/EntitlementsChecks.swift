import Foundation

@main struct EntitlementsChecks {
    static func main() {
        let entitlements: Entitlements = AllUnlocked()
        guard entitlements.isPro() else { fputs("FAIL: AllUnlocked debe ser Pro\n", stderr); exit(1) }
        for feature in ProFeature.allCases where !entitlements.hasFeature(feature) {
            fputs("FAIL: \(feature) debe estar desbloqueada\n", stderr); exit(1)
        }
        guard Set(ProFeature.allCases.map(\.rawValue)) == ["seasonalDesigns", "removeBackground", "unlimitedMolds", "maxQuality"] else {
            fputs("FAIL: lista de funciones Pro distinta de la propuesta\n", stderr); exit(1)
        }
        print("EntitlementsChecks: todo desbloqueado y cuatro funciones Pro passed")
    }
}
