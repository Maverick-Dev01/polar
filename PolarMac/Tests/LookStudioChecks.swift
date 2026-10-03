import AppKit

@main @MainActor struct LookStudioChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }
    static func main() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("polar-look-studio-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: root) }
        let studio = Studio(storageRoot: root)
        let asset = PhotoAsset(path: root.appendingPathComponent("not-needed.png").path, pixelWidth: 1000, pixelHeight: 500)
        studio.change { $0.selectStyle(.filmVertical); $0.settings.columns = 1; $0.settings.rows = 1; $0.photos = [asset]; $0.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 10) }
        let original = studio.project
        studio.lookScope = 3; studio.selectedSlot = 1; studio.applyLook(PhotoLook(preset: "cool"))
        let own = studio.project
        check(LookResolver.resolve(project: own, slot: 1).preset == "cool" && LookResolver.resolve(project: own, slot: 0).isNeutral, "individual film UI scope")
        studio.undo(); check(studio.project == original, "one undo for preset")
        studio.redo(); check(studio.project == own, "redo preset")
        studio.beginEditing()
        for value in stride(from: 0.1, through: 0.9, by: 0.1) { studio.adjustLook(PhotoLook(preset: "cool", light: value)) }
        studio.endEditing(); studio.undo(); check(studio.project == own, "one undo for complete ring gesture")
        check(studio.canRedo, "redo exists before new gesture")
        studio.beginEditing(); studio.adjustLook(PhotoLook(preset: "sepia")); check(!studio.canRedo, "open transaction clears redo immediately")
        studio.endEditing()
        studio.lookScope = 2; studio.lookPages = [0, 1]; studio.applyLook(PhotoLook(preset: "warm"))
        check(studio.project.placements[1]?.photoLook == nil && (0..<10).allSatisfy { LookResolver.resolve(project: studio.project, slot: $0).preset == "warm" }, "multi-page UI scope")
        let pages = studio.project
        studio.lookScope = 3; studio.applyLook(PhotoLook(preset: "bw"), all: true)
        check(studio.project.cardOverrides.values.allSatisfy { $0.photoLook == nil }, "apply all clears page overrides")
        studio.undo(); check(studio.project == pages, "apply all undo restores exceptions")
        let dirty = studio.isDirty, beforeCompare = studio.project, undo = studio.canUndo, redo = studio.canRedo
        studio.setComparing(true); studio.setComparing(false)
        check(studio.project == beforeCompare && studio.isDirty == dirty && studio.canUndo == undo && studio.canRedo == redo, "held comparison changes no model/history/save state")
        let beforeInvalid = studio.project
        studio.change { $0.settings.customWidthMM = 80; $0.settings.customHeightMM = 80; $0.settings.paperSize = .custom; $0.settings.margin = 60; $0.settings.rows = 6; $0.settings.gap = 30 }
        check(studio.project == beforeInvalid && studio.errorMessage != nil, "invalid edit is rejected before autosave")
        studio.errorMessage = nil; studio.openCrop(slot: 6)
        check(studio.showingCrop && studio.page == 1 && studio.selectedSlot == 6, "review opens real crop of selected page")
        studio.beginEditing()
        for value in stride(from: 1.1, through: 2.0, by: 0.1) { studio.editPlacement { $0.zoom = value } }
        studio.endEditing(); studio.undo(); check(studio.project == beforeInvalid, "one undo for whole crop gesture")
        print("OK: scopes de UI, foto de película, aplicar todas/deshacer, gesto, rehacer, comparación sin guardar y validación antes de editar")
    }
}
