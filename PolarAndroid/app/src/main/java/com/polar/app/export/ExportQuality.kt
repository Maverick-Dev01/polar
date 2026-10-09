package com.polar.app.export

/** Calidad de exportación: resolución de las fotos, calidad JPEG y si el PDF las recomprime. */
enum class ExportQuality(val dpi: Int, val jpegQuality: Int, val optimizePhotos: Boolean) {
    LIGHT(200, 85, true),
    HIGH(300, 94, true),
    MAX(300, 94, false)
}
