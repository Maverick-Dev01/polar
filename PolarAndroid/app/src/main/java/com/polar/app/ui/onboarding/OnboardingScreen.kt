package com.polar.app.ui.onboarding

import com.polar.app.ui.rememberReduceMotion
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.ui.components.PolaroidStack
import com.polar.app.ui.theme.PaperColors

private data class Step(val title: Int, val body: Int, val caption: Int, val photo: Color)

private val steps = listOf(
    Step(R.string.onb_1_title, R.string.onb_1_body, R.string.onb_1_caption, PaperColors.PhotoRose),
    Step(R.string.onb_2_title, R.string.onb_2_body, R.string.onb_2_caption, PaperColors.PhotoSage),
    Step(R.string.onb_3_title, R.string.onb_3_body, R.string.onb_3_caption, PaperColors.PhotoSky)
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val reduceMotion=rememberReduceMotion()
    var index by rememberSaveable { mutableIntStateOf(0) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.widthIn(max = 460.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone) { Text(stringResource(R.string.action_skip)) }
            }
            // El contenido se desplaza si no cabe (letra grande o pantalla baja); el botón queda fijo abajo.
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(targetState = index, transitionSpec = { fadeIn(tween(if(reduceMotion) 0 else 240)) togetherWith fadeOut(tween(if(reduceMotion) 0 else 240)) }, label = "onboarding") { i ->
                    val s = steps[i]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PolaroidStack(caption = stringResource(s.caption), photo = s.photo)
                        Spacer(Modifier.height(32.dp))
                        Text(stringResource(s.title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(s.body), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 420.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clearAndSetSemantics { }) {
                    steps.indices.forEach { i ->
                        Box(
                            Modifier.height(8.dp).width(if (i == index) 22.dp else 8.dp).background(
                                if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (index < steps.lastIndex) index++ else onDone() },
                modifier = Modifier.widthIn(min = 220.dp).heightIn(min = 56.dp)
            ) {
                Text(stringResource(if (index < steps.lastIndex) R.string.action_next else R.string.action_start))
            }
        }
    }
}
