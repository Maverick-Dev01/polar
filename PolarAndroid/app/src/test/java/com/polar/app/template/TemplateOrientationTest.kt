package com.polar.app.template

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TemplateOrientationTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun detectedHoleFollowsExifRotationAndMirroring() {
        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.DKGRAY)
        Canvas(bitmap).drawRect(40f, 20f, 140f, 100f, Paint().apply { color = Color.WHITE })
        // (x, y, width, height) in the upright image for all eight EXIF orientations.
        val expected = listOf(
            listOf(.10, .10, .25, .40), listOf(.65, .10, .25, .40),
            listOf(.65, .50, .25, .40), listOf(.10, .50, .25, .40),
            listOf(.10, .10, .40, .25), listOf(.50, .10, .40, .25),
            listOf(.50, .65, .40, .25), listOf(.10, .65, .40, .25)
        )
        for (orientation in 1..8) {
            val file = folder.newFile("template-$orientation.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString()); saveAttributes()
            }
            val load = TemplateImporter.loadTemplate(file)
            assertEquals(1, load.detectedCount)
            assertEquals(if (orientation >= 5) 200 else 400, load.template.pixelWidth)
            val region = load.template.regions.single()
            listOf(region.x, region.y, region.width, region.height).forEachIndexed { i, value ->
                assertEquals("EXIF $orientation coordinate $i", expected[orientation - 1][i], value, .015)
            }
        }
        bitmap.recycle()
    }
}
