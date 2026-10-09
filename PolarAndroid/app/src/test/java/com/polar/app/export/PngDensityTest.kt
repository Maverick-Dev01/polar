package com.polar.app.export

import android.graphics.BitmapFactory
import com.polar.app.model.PolarProject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PngDensityTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun exportedPngDeclares300PpiAndStillDecodes() {
        val file = folder.newFile("hoja.png")
        PolarExporter.exportPng(PolarProject(), 0, file, 300)
        val bytes = file.readBytes()
        assertEquals("pHYs", String(bytes, 37, 4, Charsets.ISO_8859_1)) // justo después de IHDR
        assertEquals(11811, ByteBuffer.wrap(bytes, 41, 4).int)
        assertEquals(11811, ByteBuffer.wrap(bytes, 45, 4).int)
        assertEquals(1, bytes[49].toInt())
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        assertEquals(2550, bounds.outWidth)
        assertEquals(3300, bounds.outHeight)
    }
}
