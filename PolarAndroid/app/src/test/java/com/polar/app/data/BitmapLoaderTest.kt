package com.polar.app.data

import android.graphics.Bitmap
import android.net.Uri
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

    @Test
    fun readInfoReturnsOrientedSize() {
        val info = loader().readInfo(Uri.fromFile(png()))
        assertNotNull(info)
        assertEquals(400, info!!.width)
        assertEquals(200, info.height)
    }
}
