package com.polar.app.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

sealed interface QrResult {
    data class Ok(val bitmap: Bitmap) : QrResult
    /** El enlace no cabe en un QR legible. */
    data object TooLong : QrResult
    data object Empty : QrResult
}

object QrGenerator {
    private val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 0
    )

    /** Sirve para avisar mientras se escribe el enlace. */
    fun fits(content: String): Boolean = content.isBlank() || try {
        Encoder.encode(content, ErrorCorrectionLevel.M, hints); true
    } catch (_: WriterException) { false }

    fun generate(content: String, sizePx: Int): QrResult {
        if (content.isBlank() || sizePx <= 0) return QrResult.Empty
        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            QrResult.Ok(Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888))
        } catch (_: WriterException) {
            QrResult.TooLong
        }
    }

    fun generateQrBitmap(content: String, sizePx: Int): Bitmap? = (generate(content, sizePx) as? QrResult.Ok)?.bitmap
}
