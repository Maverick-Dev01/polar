package com.polar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polar.app.ui.theme.HandwrittenStyle
import com.polar.app.ui.theme.PaperColors

/** Tres polaroids encimadas; decorativa (se oculta al lector de pantalla). Colores de "fotos" fijos: van sobre papel. */
@Composable
fun PolaroidStack(caption: String, modifier: Modifier = Modifier, photo: Color = PaperColors.PhotoRose) {
    Box(modifier.size(250.dp, 240.dp).clearAndSetSemantics { }) {
        Polaroid(Modifier.offset(10.dp, 28.dp).rotate(-9f), PaperColors.PhotoSage, null)
        Polaroid(Modifier.offset(94.dp, 6.dp).rotate(7f), PaperColors.PhotoSky, null)
        Polaroid(Modifier.offset(48.dp, 40.dp).rotate(-1.5f), photo, caption)
    }
}

@Composable
private fun Polaroid(modifier: Modifier, photo: Color, caption: String?) {
    Column(
        modifier.size(156.dp, 192.dp).shadow(10.dp).background(PaperColors.Paper).padding(start = 10.dp, end = 10.dp, top = 10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(136.dp).background(photo))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (caption != null) Text(caption, style = HandwrittenStyle, color = PaperColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
