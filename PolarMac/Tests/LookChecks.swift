import AppKit

@main struct LookChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }

    static func main() throws {
        var json = try JSONSerialization.jsonObject(with: JSONEncoder().encode(PolarProject())) as! [String: Any]
        var settings = json["settings"] as! [String: Any]
        settings["photoLook"] = ["preset": "bw"]
        json["settings"] = settings
        let loaded = try JSONDecoder().decode(PolarProject.self, from: JSONSerialization.data(withJSONObject: json))
        let saved = try JSONSerialization.jsonObject(with: JSONEncoder().encode(loaded)) as! [String: Any]
        check(((saved["settings"] as? [String: Any])?["photoLook"] as? [String: Any])?["preset"] as? String == "bw", "photoLook survives JSON roundtrip")
        let fixture = URL(fileURLWithPath: "../PolarAndroid/app/src/test/resources/fixtures/photo_looks.polar")
        var project = try JSONDecoder().decode(PolarProject.self, from: Data(contentsOf: fixture))
        try project.validated()
        check(LookResolver.resolve(project: project, slot: 0).preset == "bw", "general look")
        check(LookResolver.resolve(project: project, slot: 1).preset == "cool", "one photo in film")
        check(LookResolver.resolve(project: project, slot: 5).preset == "sepia", "card beats general")
        let roundtrip = try JSONDecoder().decode(PolarProject.self, from: JSONEncoder().encode(project))
        check(roundtrip == project, "cross-platform fixture roundtrip")
        let unknown = try JSONDecoder().decode(PhotoLook.self, from: Data("{\"preset\":\"future\"}".utf8))
        check(unknown == PhotoLook(), "unknown preset is original")
        var pages = project; pages.setPageLooks(PhotoLook(preset: "warm"), pages: [0])
        check(pages.placements[1]?.photoLook == nil && LookResolver.resolve(project: pages, slot: 1).preset == "warm", "page clears photo exception")
        check(LookResolver.resolve(project: pages, slot: 5).preset == "sepia", "other page unchanged")
        pages.setPageLooks(PhotoLook(preset: "vivid"), pages: [0, 1])
        check((0..<10).allSatisfy { LookResolver.resolve(project: pages, slot: $0).preset == "vivid" }, "several pages including vacant cards")
        pages.setAllLooks(PhotoLook(preset: "bw"))
        check(pages.cardOverrides.values.allSatisfy { $0.photoLook == nil } && pages.placements.compactMap { $0 }.allSatisfy { $0.photoLook == nil }, "all clears exceptions")
        pages.setPhotoLook(PhotoLook(), slot: 1)
        check(LookResolver.resolve(project: pages, slot: 1).isNeutral && LookResolver.resolve(project: pages, slot: 0).preset == "bw", "remove one film photo filter")
        let before = project.placements.indices.map { LookResolver.resolve(project: project, slot: $0) }
        project.selectStyle(.polaroid)
        check(project.placements.indices.allSatisfy { project.placements[$0] == nil || LookResolver.resolve(project: project, slot: $0) == before[$0] }, "style remap preserves each filtered photo")
        for look in [PhotoLook(intensity: -1), PhotoLook(light: .nan), PhotoLook(grain: 2)] {
            do { try look.validated(); check(false, "reject invalid look") } catch {}
        }
        project.placements[0]?.zoom = 0.5; try project.validated()
        project.placements[0]?.zoom = 0
        do { try project.validated(); check(false, "reject zero zoom") } catch {}
        let inputs = [[12,36,90], [200,70,40], [20,190,120], [240,235,220], [128,128,128]]
        let expected = [
            "original": inputs,
            "bw": [[35,35,35], [95,95,95], [149,149,149], [235,235,235], [128,128,128]],
            "film": [[21,21,21], [91,91,91], [152,152,152], [251,251,251], [128,128,128]],
            "sepia": [[49,44,34], [140,125,97], [177,157,123], [255,255,220], [173,154,120]],
            "warm": [[23,36,81], [217,69,25], [26,192,107], [252,235,207], [140,128,116]],
            "cool": [[0,36,102], [188,70,52], [8,190,132], [228,235,232], [116,128,140]],
            "faded": [[33,48,82], [168,86,67], [69,176,132], [231,227,218], [131,131,131]],
            "vivid": [[0,27,101], [236,57,16], [0,208,111], [253,246,225], [128,128,128]]]
        for preset in PhotoLook.presets { for i in inputs.indices {
            let actual = PhotoFilters.rgba(inputs[i] + [255], matrix: PhotoFilters.matrix(PhotoLook(preset: preset)))
            check(Array(actual.prefix(3)) == expected[preset]![i], "literal RGB \(preset) pixel\(i)")
        } }
        check(PhotoFilters.matrix(PhotoLook(preset: "film", intensity: 0)) == PhotoFilters.identity, "intensity zero identity")
        check(PhotoLook(preset: "film", intensity: 0).grainStrength == 0, "zero intensity no film grain")
        let composed = PhotoFilters.rgba([12,36,90,255], matrix: PhotoFilters.matrix(PhotoLook(preset: "bw", light: 0.5, contrast: 0.4, warmth: 0.5)))
        check(composed == [65,55,45,255], "preset then light contrast warmth; clamp once")
        let seed = PhotoFilters.seed(assetID: "01234567-89ab-cdef-0123-456789abcdef", card: 1)
        check(seed == PhotoFilters.seed(assetID: "01234567-89AB-CDEF-0123-456789ABCDEF", card: 1), "UUID uppercase seed")
        check((0..<20).map { PhotoFilters.noise(seed: seed, x: $0, y: 7, strength: 0.7) } == (0..<20).map { PhotoFilters.noise(seed: seed, x: $0, y: 7, strength: 0.7) }, "deterministic grain")
        let placement = PhotoPlacement(assetID: UUID())
        let fitZoom = PhotoFit.fitZoom(box: CGSize(width: 100, height: 100), source: CGSize(width: 200, height: 100), quarterTurns: 0)
        check(fitZoom == 0.5, "fit without clipping")
        var fitted = placement; fitted.zoom = fitZoom
        let fit = PhotoFit.compute(box: CGSize(width: 100, height: 100), source: CGSize(width: 200, height: 100), placement: fitted)
        check(fit.drawnSize == CGSize(width: 100, height: 50), "shared geometry fit")
        print("OK: JSON cruzado, alcance/resolver, película, rangos, 40 RGB literales, composición, intensidad, grano y encuadre")
    }
}
