import AppKit

/// Navegación de la guía: «Llévame ahí», el recorrido y el modo «?». La interfaz está en HelpViews.swift.
extension Studio {
    /// Abre el centro de ayuda (opcionalmente en un artículo).
    func openHelp(article: String? = nil) {
        helpMode = false; tourIndex = nil
        helpArticleID = article; showingHelp = true
    }

    /// Lleva al editor: el diseño abierto, o el más reciente, o uno nuevo. Cierra Terminar y Encuadrar si estaban abiertos.
    @discardableResult func ensureEditor() -> Bool {
        if showingFinish { showingFinish = false }
        if showingCrop { endEditing(); showingCrop = false }
        guard showingLibrary else { return true }
        libraryItems = library.list()
        if let latest = libraryItems.first { openDesign(latest.id) } else { newProject() }
        return !showingLibrary
    }

    /// Qué hizo «Llévame ahí»; la prueba lo usa para comprobar que cada destino abre algo.
    @discardableResult func go(to destino: String) -> Bool {
        showingHelp = false; helpMode = false; tourIndex = nil
        if destino == HelpDestination.tourID { startTour(); return true }
        guard let target = HelpDestination(rawValue: destino) else { return false }
        switch target {
        case .inicio:
            showLibrary()
        case .ajustes:
            NSApp.sendAction(Selector(("showSettingsWindow:")), to: nil, from: nil)
        case .moldesAsistente:
            importTemplate()
        default:
            guard ensureEditor() else { return false }
            if let tab = target.inspectorTab { inspectorTab = tab }
            switch target {
            case .catalogo:
                designSearch = ""; designCategory = "Todos"; status = "Elige un diseño del catálogo."
            case .misMoldes:
                designCategory = "Mis moldes"
            case .editorEncuadrar:
                if let slot = project.placements.firstIndex(where: { $0 != nil }) { openCrop(slot: slot) }
                else { inspectorTab = 3; status = "Agrega una foto para poder encuadrarla." }
            case .terminar:
                if project.placedCount > 0 { finish() } else { inspectorTab = 3; status = "Coloca al menos una foto para llegar a Terminar." }
            default: break
            }
        }
        return true
    }

    // MARK: Recorrido y modo «?»

    /// El recorrido sólo empieza solo la primera vez y con el editor a la vista; desde Ayuda se pide a propósito.
    var shouldAutoStartTour: Bool {
        !preferences.tourSeen && !showingWelcome && !showingHelp && moldWizard == nil && !showingLibrary && !showingFinish && !showingCrop && tourIndex == nil && !helpMode
    }
    func startTour() {
        helpMode = false; showingHelp = false
        guard ensureEditor() else { return }
        inspectorTab = 3
        tourIndex = 0
    }
    /// `completed`: terminó o lo saltó (no vuelve a salir solo). Si no se mostró ningún paso, no se marca como visto.
    func endTour(seen: Bool) {
        tourIndex = nil
        if seen && !preferences.tourSeen { preferences.tourSeen = true }
    }
    /// Menú Ayuda → «¿Qué hace cada botón?».
    func toggleHelpMode() {
        if helpMode { helpMode = false; helpSelected = nil; return }
        showingHelp = false; tourIndex = nil
        guard ensureEditor() else { return }
        helpMode = true
    }
}
