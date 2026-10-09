package com.polar.app.export

import androidx.exifinterface.media.ExifInterface
import com.polar.app.model.PolarProject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JpegDensityTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun lightJpgDeclaresItsOwnDensity() {
        val file = folder.newFile("ligero.jpg")
        PolarExporter.exportJpeg(PolarProject(), 0, file, ExportQuality.LIGHT.dpi, jpegQuality = ExportQuality.LIGHT.jpegQuality)
        val exif = ExifInterface(file)
        assertEquals(200.0, exif.getAttributeDouble(ExifInterface.TAG_X_RESOLUTION, 0.0), 0.0)
        assertEquals(200.0, exif.getAttributeDouble(ExifInterface.TAG_Y_RESOLUTION, 0.0), 0.0)
    }
}
