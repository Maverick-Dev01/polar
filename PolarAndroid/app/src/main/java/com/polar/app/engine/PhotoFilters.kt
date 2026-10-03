package com.polar.app.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.polar.app.model.PhotoLook
import kotlin.math.floor

/** Matrices sRGB y ruido espacial del contrato docs/filtros.md. Nunca edita el bitmap fuente. */
object PhotoFilters {
    val identity = doubleArrayOf(1.0,0.0,0.0,0.0,0.0, 0.0,1.0,0.0,0.0,0.0, 0.0,0.0,1.0,0.0,0.0, 0.0,0.0,0.0,1.0,0.0)
    private val matrices = mapOf(
        "original" to identity,
        "bw" to doubleArrayOf(.2126,.7152,.0722,0.0,0.0, .2126,.7152,.0722,0.0,0.0, .2126,.7152,.0722,0.0,0.0, 0.0,0.0,0.0,1.0,0.0),
        "film" to doubleArrayOf(.24449,.82248,.08303,0.0,-19.125, .24449,.82248,.08303,0.0,-19.125, .24449,.82248,.08303,0.0,-19.125, 0.0,0.0,0.0,1.0,0.0),
        "sepia" to doubleArrayOf(.393,.769,.189,0.0,0.0, .349,.686,.168,0.0,0.0, .272,.534,.131,0.0,0.0, 0.0,0.0,0.0,1.0,0.0),
        "warm" to doubleArrayOf(1.03937,-.03576,-.00361,0.0,12.0, -.01063,1.01424,-.00361,0.0,0.0, -.01063,-.03576,1.04639,0.0,-12.0, 0.0,0.0,0.0,1.0,0.0),
        "cool" to doubleArrayOf(1.0,0.0,0.0,0.0,-12.0, 0.0,1.0,0.0,0.0,0.0, 0.0,0.0,1.0,0.0,12.0, 0.0,0.0,0.0,1.0,0.0),
        "faded" to doubleArrayOf(.687402,.193104,.019494,0.0,16.0, .057402,.823104,.019494,0.0,16.0, .057402,.193104,.649494,0.0,16.0, 0.0,0.0,0.0,1.0,0.0),
        "vivid" to doubleArrayOf(1.316535,-.19668,-.019855,0.0,-12.75, -.058465,1.17832,-.019855,0.0,-12.75, -.058465,-.19668,1.355145,0.0,-12.75, 0.0,0.0,0.0,1.0,0.0)
    )
    fun matrix(look: PhotoLook): DoubleArray {
        val preset = matrices[look.preset] ?: identity
        val m = DoubleArray(20) { identity[it] + look.intensity * (preset[it] - identity[it]) }
        val f = 1 + .5 * look.contrast
        for (row in 0..2) {
            for (col in 0..3) m[row * 5 + col] *= f
            m[row * 5 + 4] = f * (m[row * 5 + 4] + 64 * look.light) + 127.5 * (1 - f) + when(row) { 0 -> 20 * look.warmth; 2 -> -20 * look.warmth; else -> 0.0 }
        }
        return m
    }
    fun rgba(channels: IntArray, matrix: DoubleArray, delta: Int = 0): IntArray = IntArray(4) { row ->
        val value = (0..3).fold(matrix[row*5+4]) { sum, c -> sum + matrix[row*5+c]*channels[c] }
        (floor(value+.5).toInt().coerceIn(0,255) + if(row<3) delta else 0).coerceIn(0,255)
    }
    fun seed(assetID: String, card: Int): UInt = (assetID.uppercase()+":"+card).toByteArray(Charsets.UTF_8)
        .fold(2166136261u) { h, b -> (h xor b.toUByte().toUInt()) * 16777619u }
    fun noise(seed: UInt, x: Int, y: Int, strength: Double): Int {
        var h = seed xor (x.toUInt()*0x9E3779B9u) xor (y.toUInt()*0x85EBCA6Bu)
        h = (h xor (h shr 16))*0x7FEB352Du; h = (h xor (h shr 15))*0x846CA68Bu; h = h xor (h shr 16)
        return floor((2*(h shr 24).toDouble()/255-1)*8*strength+.5).toInt()
    }
    fun grainStrength(look: PhotoLook) = (look.grain + if(look.preset=="film") .25*look.intensity else 0.0).coerceIn(0.0,1.0)
    fun grain(bitmap: Bitmap, look: PhotoLook, seed: UInt, originX: Double, originY: Double, pointsPerPixelX: Double, pointsPerPixelY: Double) {
        val strength = grainStrength(look)
        if(strength==0.0) return
        val pixels = IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        for(y in 0 until bitmap.height) for(x in 0 until bitmap.width) {
            val i=y*bitmap.width+x; val color=pixels[i]
            if(Color.alpha(color)==0) continue
            val delta=noise(seed, floor(originX+(x+.5)*pointsPerPixelX).toInt(), floor(originY+(y+.5)*pointsPerPixelY).toInt(), strength)
            pixels[i]=Color.argb(Color.alpha(color),(Color.red(color)+delta).coerceIn(0,255),(Color.green(color)+delta).coerceIn(0,255),(Color.blue(color)+delta).coerceIn(0,255))
        }
        bitmap.setPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
    }
}
