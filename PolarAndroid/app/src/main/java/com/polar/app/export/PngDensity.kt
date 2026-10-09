package com.polar.app.export

import java.io.File
import java.nio.ByteBuffer
import java.util.zip.CRC32

/** Android no escribe la densidad en los PNG: se agrega el bloque pHYs justo después de IHDR. */
internal object PngDensity {
    private val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    fun pixelsPerMeter(dpi: Int): Int = Math.round(dpi / 0.0254).toInt()

    fun write(file: File, dpi: Int) {
        val bytes = file.readBytes()
        if (bytes.size < 33 || !bytes.copyOfRange(0, 8).contentEquals(signature)) return
        if (String(bytes, 12, 4, Charsets.ISO_8859_1) != "IHDR") return
        val data = ByteBuffer.allocate(9).putInt(pixelsPerMeter(dpi)).putInt(pixelsPerMeter(dpi)).put(1).array()
        val type = "pHYs".toByteArray(Charsets.ISO_8859_1)
        val crc = CRC32().apply { update(type); update(data) }.value
        val chunk = ByteBuffer.allocate(21).putInt(9).put(type).put(data).putInt(crc.toInt()).array()
        val ihdrEnd = 8 + 12 + ByteBuffer.wrap(bytes, 8, 4).int
        file.writeBytes(bytes.copyOfRange(0, ihdrEnd) + chunk + bytes.copyOfRange(ihdrEnd, bytes.size))
    }
}
