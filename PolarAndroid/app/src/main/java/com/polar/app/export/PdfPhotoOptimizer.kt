package com.polar.app.export

import android.graphics.Bitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.InflaterInputStream

/** Sólo fotos opacas dibujadas por el motor; ni moldes, ni QR, ni emojis. */
internal data class PdfPhotoFingerprint(val width: Int, val height: Int, val digest: String) {
    companion object {
        fun fromBitmap(bitmap: Bitmap): PdfPhotoFingerprint? {
            val rows = minOf(64, bitmap.height)
            val pixels = IntArray(bitmap.width * rows)
            val rgb = ByteArray(pixels.size * 3)
            val digest = MessageDigest.getInstance("SHA-256")
            for (y in 0 until bitmap.height step rows) {
                val count = bitmap.width * minOf(rows, bitmap.height - y)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, y, bitmap.width, count / bitmap.width)
                for (i in 0 until count) {
                    if (pixels[i] ushr 24 != 255) return null
                    rgb[i * 3] = (pixels[i] shr 16).toByte()
                    rgb[i * 3 + 1] = (pixels[i] shr 8).toByte()
                    rgb[i * 3 + 2] = pixels[i].toByte()
                }
                digest.update(rgb, 0, count * 3)
            }
            // El bitmap nuevo tiene alpha por defecto; marcarlo opaco evita un soft mask inútil.
            bitmap.setHasAlpha(false)
            return PdfPhotoFingerprint(bitmap.width, bitmap.height, hex(digest.digest()))
        }

        fun fromRgb(width: Int, height: Int, rgb: ByteArray) = PdfPhotoFingerprint(width, height,
            hex(MessageDigest.getInstance("SHA-256").digest(rgb)))

        private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    }
}

/** Reescribe sólo el PDF clásico de PdfDocument; un formato distinto conserva la salida nativa. */
internal object PdfPhotoOptimizer {
    private val latin = Charsets.ISO_8859_1
    private data class Entry(val offset: Long, val generation: Int)

    fun optimize(source: File, output: File, photos: Set<PdfPhotoFingerprint>, jpegQuality: Int = 94): Boolean {
        if (photos.isEmpty()) return false
        try {
            RandomAccessFile(source, "r").use { input ->
                val tailSize = minOf(input.length(), 65_536).toInt()
                input.seek(input.length() - tailSize)
                val tail = ByteArray(tailSize).also(input::readFully).toString(latin)
                val xref = Regex("startxref\\s+(\\d+)\\s+%%EOF\\s*$").find(tail)?.groupValues?.get(1)?.toLong()
                    ?: return false
                require(xref in 0 until input.length())
                input.seek(xref)
                if (input.readLine() != "xref") return false
                val entries = mutableMapOf<Int, Entry>()
                var line = input.readLine() ?: return false
                while (line != "trailer") {
                    val section = Regex("(\\d+)\\s+(\\d+)").matchEntire(line.trim()) ?: return false
                    val first = section.groupValues[1].toInt()
                    val count = section.groupValues[2].toInt()
                    require(first >= 0 && count in 1..100_000 && first.toLong() + count <= 100_000)
                    repeat(count) { index ->
                        val row = Regex("(\\d{10})\\s+(\\d{5})\\s+([nf])\\s*")
                            .matchEntire(input.readLine() ?: throw IOException("xref incompleto"))
                            ?: throw IOException("xref no compatible")
                        if (row.groupValues[3] == "n") entries[first + index] = Entry(row.groupValues[1].toLong(), row.groupValues[2].toInt())
                    }
                    line = input.readLine() ?: return false
                }
                val remaining = (input.length() - input.filePointer).toInt()
                require(remaining in 1..65_536)
                val trailerTail = ByteArray(remaining).also(input::readFully).toString(latin)
                val trailer = trailerTail.substringBefore("startxref").trim()
                if (!trailer.startsWith("<<") || !trailer.endsWith(">>") || Regex("/(Prev|XRefStm)\\b").containsMatchIn(trailer)) return false
                val size = number(trailer, "Size") ?: return false
                require(size in 1..100_000 && entries.isNotEmpty() && entries.keys.all { it in 1 until size })
                val sorted = entries.entries.sortedBy { it.value.offset }
                require(sorted.all { it.value.offset in 0 until xref } && sorted.map { it.value.offset }.distinct().size == sorted.size)
                val offsets = mutableMapOf<Int, Long>()
                var changed = false
                RandomAccessFile(output, "rw").use { dest ->
                    dest.setLength(0)
                    input.seek(0)
                    copy(input, dest, sorted.first().value.offset)
                    for ((index, item) in sorted.withIndex()) {
                        val end = sorted.getOrNull(index + 1)?.value?.offset ?: xref
                        val length = end - item.value.offset
                        require(length > 0 && length <= Int.MAX_VALUE)
                        input.seek(item.value.offset)
                        // ponytail: una imagen por vez en memoria; usar streaming si una foto supera el máximo imprimible.
                        val body = ByteArray(length.toInt()).also(input::readFully)
                        val header = "${item.key} ${item.value.generation} obj"
                        require(String(body, 0, minOf(body.size, header.length + 2), latin).startsWith(header))
                        val compact = jpegImage(body, photos, jpegQuality)
                        offsets[item.key] = dest.filePointer
                        dest.write(compact ?: body)
                        changed = changed || compact != null
                    }
                    val newXref = dest.filePointer
                    dest.write("xref\n0 $size\n0000000000 65535 f \n".toByteArray(latin))
                    for (id in 1 until size) {
                        val offset = offsets[id]
                        val row = if (offset == null) "0000000000 00000 f \n" else
                            String.format(Locale.ROOT, "%010d %05d n \n", offset, entries.getValue(id).generation)
                        dest.write(row.toByteArray(latin))
                    }
                    dest.write("trailer\n$trailer\nstartxref\n$newXref\n%%EOF\n".toByteArray(latin))
                }
                return changed && output.length() < source.length()
            }
        } catch (_: IOException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        } catch (_: OutOfMemoryError) {
            return false
        }
    }

    private fun jpegImage(body: ByteArray, photos: Set<PdfPhotoFingerprint>, jpegQuality: Int): ByteArray? {
        val prefix = String(body, 0, minOf(body.size, 65_536), latin)
        val stream = Regex("\\bstream\\r?\\n").find(prefix) ?: return null
        val dictionary = prefix.substring(0, stream.range.first)
        if (!Regex("/Subtype\\s*/Image\\b").containsMatchIn(dictionary) ||
            !Regex("/ColorSpace\\s*/DeviceRGB\\b").containsMatchIn(dictionary) ||
            !Regex("/Filter\\s*/FlateDecode\\b").containsMatchIn(dictionary) ||
            Regex("/(SMask|Mask|Decode|DecodeParms)\\b").containsMatchIn(dictionary) || number(dictionary, "BitsPerComponent") != 8) return null
        val width = number(dictionary, "Width") ?: return null
        val height = number(dictionary, "Height") ?: return null
        if (photos.none { it.width == width && it.height == height }) return null
        val lengthMatch = Regex("/Length\\s+(\\d+)\\b").find(dictionary) ?: return null
        if (Regex("^\\s+\\d+\\s+R\\b").containsMatchIn(dictionary.substring(lengthMatch.range.last + 1))) return null
        val length = lengthMatch.groupValues[1].toIntOrNull() ?: return null
        val start = stream.range.last + 1
        require(length > 0 && start.toLong() + length < body.size && width.toLong() * height * 3 <= Int.MAX_VALUE)
        val rgb = ByteArray(width * height * 3)
        InflaterInputStream(ByteArrayInputStream(body, start, length)).use { compressed ->
            var read = 0
            while (read < rgb.size) {
                val count = compressed.read(rgb, read, rgb.size - read)
                if (count <= 0) return null
                read += count
            }
            if (compressed.read() != -1) return null
        }
        if (PdfPhotoFingerprint.fromRgb(width, height, rgb) !in photos) return null
        val pixels = IntArray(width * height) { i ->
            (0xff shl 24) or ((rgb[i * 3].toInt() and 255) shl 16) or
                ((rgb[i * 3 + 1].toInt() and 255) shl 8) or (rgb[i * 3 + 2].toInt() and 255)
        }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val jpeg = try {
            ByteArrayOutputStream().apply {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, this)) return null
            }.toByteArray()
        } finally { bitmap.recycle() }
        if (jpeg.size >= length) return null
        val newDictionary = dictionary.replaceRange(lengthMatch.range, "/Length ${jpeg.size}")
            .replace(Regex("/Filter\\s*/FlateDecode\\b"), "/Filter /DCTDecode")
        return ByteArrayOutputStream().apply {
            write(newDictionary.toByteArray(latin))
            write(prefix.substring(stream.range).toByteArray(latin))
            write(jpeg)
            write(body, start + length, body.size - start - length)
        }.toByteArray()
    }

    private fun number(dictionary: String, key: String) =
        Regex("/$key\\s+(\\d+)\\b").find(dictionary)?.groupValues?.get(1)?.toIntOrNull()

    private fun copy(source: RandomAccessFile, destination: RandomAccessFile, length: Long) {
        val buffer = ByteArray(65_536)
        var remaining = length
        while (remaining > 0) {
            val count = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (count < 0) throw IOException("PDF incompleto")
            destination.write(buffer, 0, count)
            remaining -= count
        }
    }
}
