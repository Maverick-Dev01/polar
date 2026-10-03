import AppKit
import ImageIO

@main struct LookRenderChecks {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fputs("FAIL: \(message)\n", stderr); exit(1) }
    }
    static func image(_ colors: [[Int]], width: Int = 100, height: Int = 20) -> CGImage {
        var data = [UInt8](repeating: 255, count: width * height * 4)
        for y in 0..<height { for x in 0..<width { for c in 0..<3 { data[(y * width + x) * 4 + c] = UInt8(colors[min(colors.count - 1, x * colors.count / width)][c]) } } }
        return CGImage(width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: width * 4,
                       space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue),
                       provider: CGDataProvider(data: Data(data) as CFData)!, decode: nil, shouldInterpolate: false, intent: .defaultIntent)!
    }
    static func pixel(_ image: CGImage, _ x: Int, _ y: Int) -> [Int] {
        let context = CGContext(data: nil, width: image.width, height: image.height, bitsPerComponent: 8, bytesPerRow: image.width * 4,
                                space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue)!
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        let p = context.data!.assumingMemoryBound(to: UInt8.self), i = (y * image.width + x) * 4
        return [Int(p[i]), Int(p[i + 1]), Int(p[i + 2]), Int(p[i + 3])]
    }
    static func main() throws {
        let colors = [[12,36,90], [200,70,40], [20,190,120], [240,235,220], [128,128,128]]
        let expected = [
            "original": colors, "bw": [[35,35,35],[95,95,95],[149,149,149],[235,235,235],[128,128,128]],
            "film": [[21,21,21],[91,91,91],[152,152,152],[251,251,251],[128,128,128]],
            "sepia": [[49,44,34],[140,125,97],[177,157,123],[255,255,220],[173,154,120]],
            "warm": [[23,36,81],[217,69,25],[26,192,107],[252,235,207],[140,128,116]],
            "cool": [[0,36,102],[188,70,52],[8,190,132],[228,235,232],[116,128,140]],
            "faded": [[33,48,82],[168,86,67],[69,176,132],[231,227,218],[131,131,131]],
            "vivid": [[0,27,101],[236,57,16],[0,208,111],[253,246,225],[128,128,128]]]
        let source = image(colors)
        for preset in PhotoLook.presets {
            let look = PhotoLook(preset: preset), rendered = try PhotoFilters.apply(source, look: look, seed: 42)
            for i in colors.indices {
                let actual = pixel(rendered, i * 20 + 10, 10)
                let delta = preset == "film" ? PhotoFilters.noise(seed: 42, x: i * 20 + 10, y: 10, strength: 0.25) : 0
                for c in 0..<3 { check(abs(actual[c] - min(255, max(0, expected[preset]![i][c] + delta))) <= 1, "native pixel \(preset) color \(i) channel \(c): \(actual)") }
                check(actual[3] == 255, "alpha retained")
            }
        }
        let composed = try PhotoFilters.apply(source, look: PhotoLook(preset: "bw", light: 0.5, contrast: 0.4, warmth: 0.5))
        check(pixel(composed, 10, 10) == [65,55,45,255], "actual composed pixel")
        let neutral = try PhotoFilters.apply(source, look: PhotoLook(preset: "film", intensity: 0))
        check(pixel(neutral, 10, 10) == [12,36,90,255], "actual intensity zero")
        let look = PhotoLook(grain: 1), seed = PhotoFilters.seed(assetID: "01234567-89AB-CDEF-0123-456789ABCDEF", card: 1)
        let low = try PhotoFilters.apply(image([[128,128,128]], width: 10, height: 10), look: look, seed: seed)
        let high = try PhotoFilters.apply(image([[128,128,128]], width: 20, height: 20), look: look, seed: seed, pointsPerPixel: CGSize(width: 0.5, height: 0.5))
        let again = try PhotoFilters.apply(image([[128,128,128]], width: 20, height: 20), look: look, seed: seed, pointsPerPixel: CGSize(width: 0.5, height: 0.5))
        for y in 0..<10 { for x in 0..<10 {
            check(pixel(low, x, y) == pixel(high, x * 2, y * 2), "same grain cell at two resolutions")
            check(pixel(high, x * 2, y * 2) == pixel(again, x * 2, y * 2), "two real grain renders match")
        } }
        let half = CGImage(width: 1, height: 1, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: 4,
                           space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue),
                           provider: CGDataProvider(data: Data([32,64,96,128]) as CFData)!, decode: nil, shouldInterpolate: false, intent: .defaultIntent)!
        let halfBW = try PhotoFilters.apply(half, look: PhotoLook(preset: "bw"))
        check(pixel(halfBW, 0, 0) == [60,60,60,128], "premultiplied alpha retained with RGB transformed once")
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("polar-look-render-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: folder) }
        let photoURL = folder.appendingPathComponent("colors.png")
        let destination = CGImageDestinationCreateWithURL(photoURL as CFURL, "public.png" as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, source, nil); check(CGImageDestinationFinalize(destination), "synthetic source saved")
        let originalBytes = try Data(contentsOf: photoURL)
        var project = PolarProject(); project.selectStyle(.filmHorizontal)
        project.settings.columns = 1; project.settings.rows = 1; project.settings.accentHex = "C34048"
        let asset = PhotoAsset(path: photoURL.path, pixelWidth: 100, pixelHeight: 20)
        project.photos = [asset]; project.placements = Array(repeating: PhotoPlacement(assetID: asset.id), count: 5)
        project.setAllLooks(PhotoLook(preset: "bw")); project.setPhotoLook(PhotoLook(preset: "sepia"), slot: 1)
        project.normalized()
        let pngURL = folder.appendingPathComponent("filtered.png"), pdfURL = folder.appendingPathComponent("filtered.pdf")
        try PolarRenderer.writePNG(project: project, page: 0, to: pngURL); try PolarRenderer.writePDF(project: project, to: pdfURL)
        let png = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(pngURL as CFURL, nil)!, 0, nil)!
        let context = CGContext(data: nil, width: png.width, height: png.height, bitsPerComponent: 8, bytesPerRow: 0,
                                space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.scaleBy(x: 300 / 72, y: 300 / 72); context.drawPDFPage(CGPDFDocument(pdfURL as CFURL)!.page(at: 1)!)
        let pdf = context.makeImage()!, factor: CGFloat = 300 / 72
        for slot in [0, 1, 2] {
            let rect = PolarRenderer.cropGeometry(project: project, slot: slot)!.photo
            let a = pixel(png, Int(rect.midX * factor), Int(rect.midY * factor))
            let b = pixel(pdf, Int(rect.midX * factor), Int(rect.midY * factor))
            if slot == 1 { check(a[0] > a[1] && a[1] > a[2] && b[0] > b[1] && b[1] > b[2], "one film photo keeps its own sepia") }
            else { check(abs(a[0] - a[1]) <= 1 && abs(a[1] - a[2]) <= 1 && abs(b[0] - b[1]) <= 1 && abs(b[1] - b[2]) <= 1, "PDF/PNG BW photos are gray") }
        }
        project.selectStyle(.polaroid); project.settings.columns = 1; project.settings.rows = 1
        project.settings.title = "COLOR"; project.setAllLooks(PhotoLook(preset: "bw")); project.placements = [PhotoPlacement(assetID: asset.id)]
        project.normalized(); try PolarRenderer.writePNG(project: project, page: 0, to: pngURL); try PolarRenderer.writePDF(project: project, to: pdfURL)
        let coloredPNG = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(pngURL as CFURL, nil)!, 0, nil)!
        let coloredContext = CGContext(data: nil, width: coloredPNG.width, height: coloredPNG.height, bitsPerComponent: 8, bytesPerRow: 0,
                                       space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        coloredContext.scaleBy(x: 300 / 72, y: 300 / 72); coloredContext.drawPDFPage(CGPDFDocument(pdfURL as CFURL)!.page(at: 1)!)
        let coloredPDF = coloredContext.makeImage()!
        let card = PolarRenderer.cropGeometry(project: project, slot: 0)!.card
        let caption = CGRect(x: card.minX, y: card.minY + card.height * 0.8, width: card.width, height: card.height * 0.2)
        func containsColor(_ image: CGImage, region: CGRect) -> Bool {
            guard let crop = image.cropping(to: region) else { return false }
            let context = CGContext(data: nil, width: crop.width, height: crop.height, bitsPerComponent: 8, bytesPerRow: crop.width * 4,
                                    space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
            context.draw(crop, in: CGRect(x: 0, y: 0, width: crop.width, height: crop.height))
            let data = context.data!.assumingMemoryBound(to: UInt8.self)
            return stride(from: 0, to: crop.width * crop.height * 4, by: 4).contains { Int(data[$0]) - Int(data[$0 + 1]) > 25 }
        }
        check(containsColor(coloredPNG, region: CGRect(x: caption.minX * factor, y: caption.minY * factor, width: caption.width * factor, height: caption.height * factor)), "PNG text stays colored")
        check(containsColor(coloredPDF, region: CGRect(x: caption.minX * factor, y: caption.minY * factor, width: caption.width * factor, height: caption.height * factor)), "PDF text stays colored")
        let mini = PolarRenderer.cardPreview(project: project, slot: 0, width: 100, photoOnly: true)!.cgImage(forProposedRect: nil, context: nil, hints: nil)!
        let miniPixel = pixel(mini, mini.width / 2, mini.height / 2)
        check(abs(miniPixel[0] - miniPixel[1]) <= 1 && abs(miniPixel[1] - miniPixel[2]) <= 1, "preset/card preview uses the real photo renderer")
        var empty = project; empty.placements = [nil]; empty.normalized()
        let proof = PolarRenderer.preview(project: empty, page: 0, scale: 0.2, printReady: true).cgImage(forProposedRect: nil, context: nil, hints: nil)!
        check(pixel(proof, proof.width / 2, proof.height / 2) == [255,255,255,255], "print proof omits empty editing placeholders")
        let after = try Data(contentsOf: photoURL)
        check(after == originalBytes, "source photo untouched")
        print("OK: 40 píxeles nativos, alfa, composición, intensidad cero, grano real a dos escalas, PDF/PNG y una foto de película")
    }
}
