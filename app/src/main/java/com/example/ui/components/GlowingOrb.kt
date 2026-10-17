package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AssistantState
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaViolet
import com.example.ui.theme.NovaVioletGlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingOrb(
    state: AssistantState,
    audioAmplitude: Float,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Slow breath animation for idle state
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Rotation for thinking energy ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Speaking pulse
    val speakingWave by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_wave"
    )

    // Smooth amplitude damping
    val animatedAmplitude = remember { Animatable(0f) }
    LaunchedEffect(audioAmplitude) {
        animatedAmplitude.animateTo(
            targetValue = audioAmplitude,
            animationSpec = tween(durationMillis = 80, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 3.2f

            // Dynamic multiplier based on state
            val dynamicScale = when (state) {
                AssistantState.IDLE -> breathScale
                AssistantState.LISTENING -> (1f + animatedAmplitude.value * 0.45f)
                AssistantState.THINKING -> breathScale * 1.02f
                AssistantState.EXECUTING -> 1.08f
                AssistantState.SPEAKING -> if (isSpeaking) speakingWave else 1.05f
            }

            // Theme colors based on state
            val (coreColor1, coreColor2, auraColor) = when (state) {
                AssistantState.IDLE -> Triple(NovaCyan, NovaViolet, NovaCyanGlow)
                AssistantState.LISTENING -> Triple(NovaCyan, Color(0xFF00FFA3), NovaCyanGlow)
                AssistantState.THINKING -> Triple(NovaViolet, NovaMagenta, NovaVioletGlow)
                AssistantState.EXECUTING -> Triple(NovaEmerald, NovaCyan, Color(0x6600E676))
                AssistantState.SPEAKING -> Triple(NovaMagenta, NovaCyan, NovaVioletGlow)
            }

            // Layer 1: Outermost diffuse ambient glow
            val outerRadius = baseRadius * 1.8f * dynamicScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = centerOffset,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = centerOffset
            )

            // Layer 2: Animated wave rings for listening or speaking
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val waveCount = 3
                for (i in 1..waveCount) {
                    val waveRadius = baseRadius * (1.15f + i * 0.22f * dynamicScale)
                    drawCircle(
                        color = coreColor1.copy(alpha = (0.35f / i) * (if (state == AssistantState.LISTENING) animatedAmplitude.value + 0.3f else 0.7f).coerceIn(0.1f, 0.8f)),
                        radius = waveRadius,
                        center = centerOffset,
                        style = Stroke(width = 2.dp.toPx() * (waveCount - i + 1))
                    )
                }
            }

            // Layer 3: Thinking rotating particles / orbital segments
            if (state == AssistantState.THINKING) {
                val orbitRadius = baseRadius * 1.25f
                val rad = Math.toRadians(rotationAngle.toDouble())
                for (p in 0 until 4) {
                    val pAngle = rad + (p * Math.PI / 2)
                    val px = centerOffset.x + (orbitRadius * cos(pAngle)).toFloat()
                    val py = centerOffset.y + (orbitRadius * sin(pAngle)).toFloat()
                    drawCircle(
                        color = if (p % 2 == 0) NovaMagenta else NovaCyan,
                        radius = 6.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // Layer 4: Main spherical core gradient
            val currentRadius = baseRadius * dynamicScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreColor1.copy(alpha = 0.9f),
                        coreColor2.copy(alpha = 0.85f),
                        Color(0xFF0A0F24)
                    ),
                    center = Offset(centerOffset.x - currentRadius * 0.25f, centerOffset.y - currentRadius * 0.25f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = centerOffset
            )

            // Layer 5: Luminous rim ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(coreColor1, coreColor2, Color.White, coreColor1),
                    center = centerOffset
                ),
                radius = currentRadius,
                center = centerOffset,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
