package com.polar.app.export

import android.print.PrintAttributes
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PrintMediaTest {
    @Test
    fun mapsPolarPaperToAndroidMedia() {
        assertEquals(PrintAttributes.MediaSize.NA_LETTER.id, PrintMedia.mediaFor(PrintSettings())!!.id)
        assertEquals(PrintAttributes.MediaSize.ISO_A4.id, PrintMedia.mediaFor(PrintSettings(paperSize = PaperSize.A4))!!.id)
        assertFalse(PrintMedia.mediaFor(PrintSettings(paperSize = PaperSize.A4, orientation = PaperOrientation.LANDSCAPE))!!.isPortrait)
        assertNull(PrintMedia.mediaFor(PrintSettings(paperSize = PaperSize.CUSTOM)))
    }
}
