package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaViolet

@Composable
fun WaveformVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 18
    val transition = rememberInfiniteTransition(label = "waveform_anim")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val active = isListening || isSpeaking
        val baseMultiplier = if (isListening) (amplitude * 2.2f).coerceIn(0.2f, 1.0f) else if (isSpeaking) 0.8f else 0.15f

        for (i in 0 until barCount) {
            val sineVal = kotlin.math.sin(phase + (i * 0.4f)).toFloat()
            val normalizedSine = (sineVal + 1f) / 2f
            val barHeightDp = (8 + (normalizedSine * 34 * baseMultiplier)).dp

            val color = when {
                isListening -> NovaCyan
                isSpeaking -> NovaViolet
                else -> Color.Gray.copy(alpha = 0.35f)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeightDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                color,
                                if (active) NovaMagenta else color
                            )
                        )
                    )
            )

            if (i < barCount - 1) {
                Box(modifier = Modifier.width(4.dp))
            }
        }
    }
}
