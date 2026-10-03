import AppKit
import CoreImage
import ImageIO
import Vision

struct SuggestedPhrase: Codable, Identifiable {
    var id: String
    var category: String
    var text: String
    var tags: String
    static func load() -> [SuggestedPhrase] {
        guard let url = Bundle.main.url(forResource: "phrases", withExtension: "json"), let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([SuggestedPhrase].self, from: data)) ?? []
    }
}

enum BackgroundRemover {
    static let ci = CIContext(options: [.cacheIntermediates: false])
    static func matchingColor(photo: PhotoAsset) -> String? {
        guard let source = CGImageSourceCreateWithURL(photo.url as CFURL, nil), let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceCreateThumbnailWithTransform: true, kCGImageSourceThumbnailMaxPixelSize: 128] as CFDictionary) else { return nil }
        let pixels = NSBitmapImageRep(cgImage: image)
        let colors = [(0,0), (image.width-1,0), (0,image.height-1), (image.width-1,image.height-1)].compactMap { pixels.colorAt(x: $0.0, y: $0.1)?.usingColorSpace(.sRGB) }
        guard colors.count == 4 else { return nil }
        return String(format: "%02X%02X%02X", Int(colors.map(\.redComponent).reduce(0,+)*255/4), Int(colors.map(\.greenComponent).reduce(0,+)*255/4), Int(colors.map(\.blueComponent).reduce(0,+)*255/4))
    }
    static func mask(photo: PhotoAsset, directory: URL) throws -> String {
        guard let source = CGImageSourceCreateWithURL(photo.url as CFURL, nil), let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
            kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: 1600
        ] as CFDictionary) else { throw PolarError.unreadablePhoto(photo.name) }
        let handler = VNImageRequestHandler(cgImage: image, options: [:])
        let request = VNGenerateForegroundInstanceMaskRequest()
        try handler.perform([request])
        guard let result = request.results?.first, !result.allInstances.isEmpty else {
            throw PolarError.invalidProject("No se encontró un sujeto claro. Se conserva la foto original.")
        }
        let mask = try result.generateScaledMaskForImage(forInstances: result.allInstances, from: handler)
        let rgba = CIImage(cvPixelBuffer: mask).applyingFilter("CIMaskToAlpha")
        guard let output = ci.createCGImage(rgba, from: rgba.extent) else { throw PolarError.exportFailed }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let path = directory.appendingPathComponent("mask-\(UUID().uuidString).png")
        let data = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(data, "public.png" as CFString, 1, nil) else { throw PolarError.exportFailed }
        CGImageDestinationAddImage(destination, output, nil)
        guard CGImageDestinationFinalize(destination) else { throw PolarError.exportFailed }
        try (data as Data).write(to: path, options: .atomic)
        return path.path
    }

    static func composite(subject: CGImage, mask: CGImage, options: PhotoBackground, background: PhotoAsset?) throws -> CGImage {
        let extent = CGRect(x: 0, y: 0, width: subject.width, height: subject.height)
        var matte = CIImage(cgImage: mask)
        if options.feather > 0 {
            matte = matte.clampedToExtent().applyingFilter("CIGaussianBlur", parameters: [kCIInputRadiusKey: Double(min(subject.width, subject.height)) * 0.006 * options.feather]).cropped(to: extent)
        }
        var backdrop = CIImage(color: options.colorHex.flatMap { CIColor(color: PolarRenderer.color($0)) } ?? .clear).cropped(to: extent)
        if let background {
            guard let source = CGImageSourceCreateWithURL(background.url as CFURL, nil) else { throw PolarError.unreadablePhoto(background.name) }
            let factor = max(Double(subject.width)/Double(background.pixelWidth), Double(subject.height)/Double(background.pixelHeight))
            let target = Int(ceil(min(Double(max(background.pixelWidth, background.pixelHeight)), Double(max(background.pixelWidth, background.pixelHeight))*factor)))
            guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceCreateThumbnailWithTransform: true, kCGImageSourceThumbnailMaxPixelSize: max(1,target)] as CFDictionary) else { throw PolarError.unreadablePhoto(background.name) }
            let scale = max(extent.width/CGFloat(image.width), extent.height/CGFloat(image.height))
            backdrop = CIImage(cgImage: image).transformed(by: CGAffineTransform(scaleX: scale, y: scale))
            backdrop = backdrop.transformed(by: CGAffineTransform(translationX: (extent.width-backdrop.extent.width)/2, y: (extent.height-backdrop.extent.height)/2)).cropped(to: extent)
        }
        if options.shadow > 0 {
            let shadow = matte.applyingFilter("CIColorMatrix", parameters: ["inputRVector": CIVector(x:0,y:0,z:0,w:0), "inputGVector": CIVector(x:0,y:0,z:0,w:0), "inputBVector": CIVector(x:0,y:0,z:0,w:0), "inputAVector": CIVector(x:0,y:0,z:0,w:options.shadow*0.43)])
                .applyingFilter("CIGaussianBlur", parameters: [kCIInputRadiusKey: Double(min(subject.width,subject.height))*0.025])
                .transformed(by: CGAffineTransform(translationX: extent.width*0.012, y: -extent.height*0.018))
            backdrop = shadow.composited(over: backdrop).cropped(to: extent)
        }
        let cutout = CIImage(cgImage: subject).applyingFilter("CIBlendWithAlphaMask", parameters: [kCIInputBackgroundImageKey: CIImage(color: .clear).cropped(to: extent), kCIInputMaskImageKey: matte])
        let result = cutout.composited(over: backdrop)
        guard let output = ci.createCGImage(result, from: extent) else { throw PolarError.exportFailed }
        return output
    }
}
