package com.polar.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoLookRenderTest {
    @Test fun transientZeroSizeNeverRasterizesEmoji() {
        val bitmap=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        val project=PolarProject(settings=PrintSettings(title="♥"))
        for(scale in listOf(0f,-1f,Float.NaN,Float.POSITIVE_INFINITY)) {
            PolarRenderer.drawPage(Canvas(bitmap),project,0,true,scale)
            PolarRenderer.drawCardPreview(Canvas(bitmap),project,PolarRect(0.0,0.0,100.0,100.0),scale)
            assertEquals(Color.MAGENTA,bitmap.getPixel(0,0))
        }
        bitmap.recycle()
    }
    @Test fun blackAndWhiteFiltersThePhotoThroughThePageRenderer() {
        val source = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val rgb = listOf(Color.rgb(12,36,90), Color.rgb(200,70,40), Color.rgb(20,190,120), Color.rgb(240,235,220), Color.rgb(128,128,128))
        for (y in 0..49) for (x in 0..49) source.setPixel(x,y,rgb[x/10])
        val asset = PhotoAsset(id="12345678-1234-1234-1234-123456789ABC",path="sample",pixelWidth=50,pixelHeight=50)
        val project = PolarProject(settings=PrintSettings(style=TemplateStyle.BORDERLESS, columns=1,rows=1,margin=0.0,gap=0.0,
            paperSize=PaperSize.CUSTOM,customWidthMM=80.0,customHeightMM=80.0,cardFormat=CardFormat.FILL,cutGuides=false,photoLook=PhotoLook(preset="bw")),
            photos=listOf(asset),placements=listOf(PhotoPlacement(assetID=asset.id)))
        val output = Bitmap.createBitmap(50,50,Bitmap.Config.ARGB_8888)
        PolarRenderer.drawPage(Canvas(output),project,0,false,50f/project.settings.paperSizePoints.width.toFloat(),{source})
        for ((x,want) in listOf(35,95,149,235,128).withIndex()) {
            val color=output.getPixel(x*10+5,25)
            assertEquals(want.toDouble(),Color.red(color).toDouble(),1.0)
            assertEquals(want.toDouble(),Color.green(color).toDouble(),1.0)
            assertEquals(want.toDouble(),Color.blue(color).toDouble(),1.0)
        }
    }    @Test fun nativeRendererMatchesAllPresetsAndGrainRepeatsExactly() {
        val colors=listOf(Color.rgb(12,36,90),Color.rgb(200,70,40),Color.rgb(20,190,120),Color.rgb(240,235,220),Color.rgb(128,128,128))
        val expected=listOf(
            listOf("12,36,90","35,35,35","21,21,21","49,44,34","23,36,81","0,36,102","33,48,82","0,27,101"),
            listOf("200,70,40","95,95,95","91,91,91","140,125,97","217,69,25","188,70,52","168,86,67","236,57,16"),
            listOf("20,190,120","149,149,149","152,152,152","177,157,123","26,192,107","8,190,132","69,176,132","0,208,111"),
            listOf("240,235,220","235,235,235","251,251,251","255,255,220","252,235,207","228,235,232","231,227,218","253,246,225"),
            listOf("128,128,128","128,128,128","128,128,128","173,154,120","140,128,116","116,128,140","131,131,131","128,128,128")
        )
        val source=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
        for(y in 0..99) for(x in 0..99) source.setPixel(x,y,colors[x/20])
        val asset=PhotoAsset(id="12345678-1234-1234-1234-123456789ABC",path="fixture",pixelWidth=100,pixelHeight=100)
        val base=PolarProject(settings=PrintSettings(style=TemplateStyle.BORDERLESS,columns=1,rows=1,margin=0.0,gap=0.0,paperSize=PaperSize.CUSTOM,customWidthMM=80.0,customHeightMM=80.0,cardFormat=CardFormat.FILL,cutGuides=false),photos=listOf(asset),placements=listOf(PhotoPlacement(assetID=asset.id)))
        for((p,preset) in PhotoLook.PRESETS.withIndex()) {
            val project=base.copy(settings=base.settings.copy(photoLook=PhotoLook(preset=preset)))
            val bitmap=com.polar.app.export.PolarExporter.renderPageToBitmap(project,0,300,{source})
            val scale=300.0/72
            for(x in 0..4) {
                val px=((x+.5)*bitmap.width/5).toInt();val py=bitmap.height/2
                val delta=if(preset=="film") PhotoFilters.noise(PhotoFilters.seed(asset.id,0),kotlin.math.floor((px+.5)/scale).toInt(),kotlin.math.floor((py+.5)/scale).toInt(),.25) else 0
                val want=expected[x][p].split(",").map { (it.toInt()+delta).coerceIn(0,255) };val actual=bitmap.getPixel(px,py)
                for((i,c) in listOf(Color.red(actual),Color.green(actual),Color.blue(actual)).withIndex()) assertEquals("$preset/$x/$i",want[i].toDouble(),c.toDouble(),1.0)
            }
            if(preset=="film") {
                val again=com.polar.app.export.PolarExporter.renderPageToBitmap(project,0,300,{source})
                assertTrue(bitmap.sameAs(again));again.recycle()
            }
            bitmap.recycle()
        }
        source.recycle()
    }

}
