import CoreGraphics
import ImageIO
import Foundation

let folder = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
let sizes = [(16,"icon_16x16"),(32,"icon_16x16@2x"),(32,"icon_32x32"),(64,"icon_32x32@2x"),(128,"icon_128x128"),(256,"icon_128x128@2x"),(256,"icon_256x256"),(512,"icon_256x256@2x"),(512,"icon_512x512"),(1024,"icon_512x512@2x")]
func color(_ r: CGFloat, _ g: CGFloat, _ b: CGFloat) -> CGColor { CGColor(srgbRed:r, green:g, blue:b, alpha:1) }
for (size,name) in sizes {
    let c = CGContext(data:nil, width:size, height:size, bitsPerComponent:8, bytesPerRow:0, space:CGColorSpace(name:CGColorSpace.sRGB)!, bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
    c.scaleBy(x:CGFloat(size)/1024, y:CGFloat(size)/1024)
    c.setFillColor(color(0.48,0.16,0.23))
    c.addPath(CGPath(roundedRect:CGRect(x:40,y:40,width:944,height:944),cornerWidth:210,cornerHeight:210,transform:nil)); c.fillPath()
    c.saveGState(); c.translateBy(x:485,y:515); c.rotate(by:-0.16)
    c.setFillColor(color(0.80,0.59,0.53))
    c.fill(CGRect(x:-265,y:-245,width:490,height:605)); c.restoreGState()
    c.saveGState(); c.translateBy(x:540,y:480); c.rotate(by:0.12)
    c.setFillColor(color(0.99,0.96,0.91)); c.fill(CGRect(x:-245,y:-270,width:490,height:605))
    c.setFillColor(color(0.88,0.77,0.67)); c.fill(CGRect(x:-210,y:-110,width:420,height:410))
    c.setFillColor(color(0.99,0.89,0.69)); c.fillEllipse(in:CGRect(x:60,y:170,width:100,height:100))
    c.setFillColor(color(0.38,0.48,0.42)); c.beginPath(); c.move(to:CGPoint(x:-210,y:-110)); c.addLine(to:CGPoint(x:-210,y:30)); c.addLine(to:CGPoint(x:-60,y:170)); c.addLine(to:CGPoint(x:80,y:15)); c.addLine(to:CGPoint(x:210,y:85)); c.addLine(to:CGPoint(x:210,y:-110)); c.closePath(); c.fillPath()
    c.setFillColor(color(0.48,0.16,0.23))
    c.beginPath(); c.move(to:CGPoint(x:0,y:-225)); c.addCurve(to:CGPoint(x:-48,y:-160),control1:CGPoint(x:-30,y:-201),control2:CGPoint(x:-65,y:-183)); c.addCurve(to:CGPoint(x:0,y:-160),control1:CGPoint(x:-48,y:-129),control2:CGPoint(x:-10,y:-137)); c.addCurve(to:CGPoint(x:48,y:-160),control1:CGPoint(x:10,y:-137),control2:CGPoint(x:48,y:-129)); c.addCurve(to:CGPoint(x:0,y:-225),control1:CGPoint(x:65,y:-183),control2:CGPoint(x:30,y:-201)); c.closePath(); c.fillPath()
    c.restoreGState()
    let target = CGImageDestinationCreateWithURL(folder.appendingPathComponent(name+".png") as CFURL,"public.png" as CFString,1,nil)!
    CGImageDestinationAddImage(target,c.makeImage()!,nil)
    precondition(CGImageDestinationFinalize(target))
}
