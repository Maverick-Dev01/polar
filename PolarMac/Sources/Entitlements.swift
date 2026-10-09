import Foundation

/// Funciones que una compra de Polar Pro podría desbloquear. Hoy ninguna está bloqueada (ver docs/monetizacion.md).
enum ProFeature: String, CaseIterable {
    case seasonalDesigns, removeBackground, unlimitedMolds, maxQuality
}

/// Capa de «derechos»: consulta si el usuario tiene Pro. Aún no se usa para bloquear nada.
protocol Entitlements {
    func isPro() -> Bool
    func hasFeature(_ feature: ProFeature) -> Bool
}

/// Implementación por defecto: todo desbloqueado.
struct AllUnlocked: Entitlements {
    func isPro() -> Bool { true }
    func hasFeature(_ feature: ProFeature) -> Bool { true }
}
