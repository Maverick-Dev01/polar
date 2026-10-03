package com.polar.app.data

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapLoaderTest {
    @get:Rule val folder = TemporaryFolder()

    private fun png(): File {
        val file = folder.newFile("foto.png")
        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private fun loader() = BitmapLoader(ApplicationProvider.getApplicationContext())

    @Test
    fun loadDecodesWithSampling() {
        val bitmap = loader().load(png().path, 100)
        assertNotNull(bitmap)
        assertTrue(bitmap!!.width <= 200)
    }

    @Test
    fun printDecodeKeepsTheRequestedResolutionBetweenSamplingSteps() {
        val bitmap = loader().loadForPrint(png().path, 150)
        assertNotNull(bitmap)
        assertEquals(150, bitmap!!.width)
        assertEquals(75, bitmap.height)
    }

    @Test
    fun loadOfMissingFileIsNull() {
        assertNull(loader().load(File(folder.root, "no-existe.jpg").path, 100))
    }

    @Test(expected = com.polar.app.model.PolarException::class)
    fun printOfMissingPhotoFailsInsteadOfExportingAnEmptyFrame() {
        loader().loadForPrint(File(folder.root, "no-existe.jpg").path, 100)
    }

    @Test
    fun printNeverUpscalesTheOriginalToFillTheRequestedPixels() {
        val bitmap = loader().loadForPrint(png().path, 8000)!!
        assertEquals(400, bitmap.width)
        assertEquals(200, bitmap.height)
    }

    @Test
    fun printDecoderPreservesHighResolutionAlphaAndFineDetail() {
        val file = folder.newFile("detalle.png")
        val original = Bitmap.createBitmap(4800, 64, Bitmap.Config.ARGB_8888)
        val row = IntArray(4800) { if (it % 4 < 2) Color.RED else Color.TRANSPARENT }
        for (y in 0 until 64) original.setPixels(row, 0, 4800, 0, y, 4800, 1)
        FileOutputStream(file).use { original.compress(Bitmap.CompressFormat.PNG, 100, it) }
        original.recycle()
        val loaded = loader().loadForPrint(file.path, 4800)
        assertEquals(4800, loaded.width)
        assertEquals(Bitmap.Config.ARGB_8888, loaded.config)
        for (x in 4000..4400) assertEquals(row[x], loaded.getPixel(x, 32))
    }

    @Test
    fun printDecoderAppliesExifOrientationOnce() {
        val file = folder.newFile("orientada.jpg")
        val original = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.RED)
        FileOutputStream(file).use { original.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        original.recycle()
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val loaded = loader().loadForPrint(file.path, 300)
        assertEquals(150, loaded.width)
        assertEquals(300, loaded.height)
    }

    @Test
    @Config(sdk = [27])
    fun legacyPrintDecodeKeepsExactSizeBetweenSamplingSteps() {
        val loaded = loader().loadForPrint(png().path, 150)
        assertEquals(150, loaded.width)
        assertEquals(75, loaded.height)
    }

    @Test
    fun readInfoReturnsOrientedSize() {
        val info = loader().readInfo(Uri.fromFile(png()))
        assertNotNull(info)
        assertEquals(400, info!!.width)
        assertEquals(200, info.height)
    }
}
