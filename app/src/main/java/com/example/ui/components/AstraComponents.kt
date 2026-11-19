package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyFlashcard
import com.example.ui.theme.AstraIndigo
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.FormulaTextStyle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NebulaViolet
import com.example.ui.theme.ScholarAmber
import kotlin.math.PI
import kotlin.math.sin

enum class AstraOrbState {
    IDLE,
    THINKING,
    LISTENING,
    SPEAKING
}

@Composable
fun AstraBreathingOrb(
    state: AstraOrbState,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp
) {
    val transition = rememberInfiniteTransition(label = "astra_orb_transition")

    val breathPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AstraOrbState.IDLE -> 3400
                    AstraOrbState.THINKING -> 1400
                    AstraOrbState.LISTENING -> 1100
                    AstraOrbState.SPEAKING -> 1600
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_phase"
    )

    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AstraOrbState.THINKING) 2600 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val intensity by animateFloatAsState(
        targetValue = when (state) {
            AstraOrbState.IDLE -> 0.55f
            AstraOrbState.THINKING -> 0.95f
            AstraOrbState.LISTENING -> 1.0f
            AstraOrbState.SPEAKING -> 0.85f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "orb_intensity"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "Biểu tượng chuyển động gia sư AI AstraMác"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = this.size.minDimension
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val pulseWave = (sin(breathPhase) + 1f) / 2f

            // Outer ambient aura
            val outerRadius = minDim * (0.42f + 0.06f * pulseWave * intensity)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.28f * intensity),
                        primaryColor.copy(alpha = 0.14f * intensity),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius * 1.25f
                ),
                radius = outerRadius * 1.25f,
                center = center
            )

            // Rotating dialectical orbit rings
            rotate(degrees = orbitAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.7f),
                            secondaryColor.copy(alpha = 0.35f),
                            Color.Transparent,
                            tertiaryColor.copy(alpha = 0.6f)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = Offset(center.x - minDim * 0.38f, center.y - minDim * 0.38f),
                    size = Size(minDim * 0.76f, minDim * 0.76f),
                    style = Stroke(width = minDim * 0.028f, cap = StrokeCap.Round)
                )
            }

            rotate(degrees = -orbitAngle * 1.3f, pivot = center) {
                drawArc(
                    color = secondaryColor.copy(alpha = 0.55f * intensity),
                    startAngle = 45f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(center.x - minDim * 0.31f, center.y - minDim * 0.31f),
                    size = Size(minDim * 0.62f, minDim * 0.62f),
                    style = Stroke(width = minDim * 0.02f, cap = StrokeCap.Round)
                )
            }

            // Core glowing sphere
            val coreRadius = minDim * (0.23f + 0.03f * pulseWave)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor,
                        secondaryColor,
                        if (state == AstraOrbState.LISTENING) tertiaryColor else primaryColor.copy(alpha = 0.85f)
                    ),
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                ),
                radius = coreRadius,
                center = center
            )

            // Specular highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.32f),
                radius = coreRadius * 0.36f,
                center = Offset(center.x - coreRadius * 0.28f, center.y - coreRadius * 0.28f)
            )
        }

        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(size * 0.22f)
        )
    }
}

@Composable
fun AstraVoiceWaveform(
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "voice_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val activityScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.18f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "wave_scale"
    )

    Canvas(
        modifier = modifier
            .height(36.dp)
            .width(120.dp)
            .semantics { contentDescription = "Sóng âm phân tích giọng nói" }
    ) {
        val barCount = 9
        val spacing = size.width / barCount
        val barWidth = spacing * 0.48f
        val maxH = size.height * 0.9f
        for (i in 0 until barCount) {
            val offset = i * 0.65f
            val wave = ((sin(phase + offset) + 1f) / 2f)
            val centerBias = 1f - (kotlin.math.abs(i - (barCount - 1) / 2f) / 5f)
            val barH = (size.height * 0.16f) + maxH * wave * centerBias * activityScale
            val top = (size.height - barH) / 2f
            drawRoundRect(
                color = color.copy(alpha = 0.45f + 0.55f * centerBias),
                topLeft = Offset(i * spacing + (spacing - barWidth) / 2f, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun AnimatedMasteryRing(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    strokeWidth: Dp = 8.dp
) {
    val clamped = percent.coerceIn(0, 100)
    val sweep by animateFloatAsState(
        targetValue = (clamped / 100f) * 360f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "mastery_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val activeStart = AstraIndigo
    val activeEnd = NebulaViolet

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(activeStart, activeEnd, activeStart)),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = "$clamped%",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FlippingFlashcard(
    card: StudyFlashcard,
    flipped: Boolean,
    onToggleFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_flip"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleFlip()
            },
        shape = RoundedCornerShape(24.dp),
        color = if (rotation <= 90f) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        },
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        if (rotation <= 90f) {
            // Front Face
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (card.chapterNumber in 1..6) "Chương ${card.chapterNumber}" else "Tổng hợp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    if (card.mastered) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Đã thuộc",
                                tint = EmeraldMastery,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đã thuộc",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldMastery
                            )
                        }
                    }
                }

                Text(
                    text = card.frontTerm,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chạm để lật xem giải nghĩa & công thức",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Back Face (un-mirror along Y)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = card.frontTerm,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = card.backDefinition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (card.formula.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = card.formula,
                                style = FormulaTextStyle,
                                color = ScholarAmber,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    if (card.example.isNotBlank()) {
                        Text(
                            text = "Ví dụ: ${card.example}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Chạm để lật lại mặt trước",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FormattedAiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val monoBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val primaryColor = MaterialTheme.colorScheme.primary

    val formatted = remember(text, color, monoBg, primaryColor) {
        buildAnnotatedString {
            val lines = text.lines()
            lines.forEachIndexed { lineIdx, rawLine ->
                val trimmed = rawLine.trimStart()
                when {
                    trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                        val headerText = trimmed.dropWhile { it == '#' }.trim()
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)) {
                            appendInlineFormatted(headerText, monoBg, primaryColor)
                        }
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        append("• ")
                        appendInlineFormatted(trimmed.drop(2), monoBg, primaryColor)
                    }
                    else -> {
                        appendInlineFormatted(rawLine, monoBg, primaryColor)
                    }
                }
                if (lineIdx < lines.lastIndex) {
                    append("\n")
                }
            }
        }
    }

    Text(
        text = formatted,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = modifier
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineFormatted(
    input: String,
    monoBg: Color,
    accent: Color
) {
    var i = 0
    while (i < input.length) {
        when {
            input.startsWith("**", i) -> {
                val end = input.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(input.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(input[i])
                    i++
                }
            }
            input[i] == '`' -> {
                val end = input.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            background = monoBg,
                            color = accent
                        )
                    ) {
                        append(" ${input.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(input[i])
                    i++
                }
            }
            else -> {
                append(input[i])
                i++
            }
        }
    }
}
