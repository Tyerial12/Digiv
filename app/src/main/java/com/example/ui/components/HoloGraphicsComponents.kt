package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DigimonCatalog
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HoloArcGauge(
    percentage: Int,
    accentColor: Color,
    centerLabel: String,
    subLabel: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 84.dp
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (percentage.coerceIn(0, 100) / 100f) * 260f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
        label = "arc_sweep"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.11f
            val inset = strokeWidth / 2f + 4f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)

            // Track arc
            drawArc(
                color = accentColor.copy(alpha = 0.16f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active glow arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.75f),
                        accentColor,
                        Color.White.copy(alpha = 0.9f),
                        accentColor
                    )
                ),
                startAngle = 140f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
    }
}

@Composable
fun SegmentedPlasmaBar(
    percentage: Int,
    accentColor: Color,
    segmentCount: Int = 10,
    modifier: Modifier = Modifier
) {
    val animatedRatio by animateFloatAsState(
        targetValue = percentage.coerceIn(0, 100) / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
        label = "plasma_bar"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val gap = 4.dp.toPx()
        val totalGap = gap * (segmentCount - 1)
        val segWidth = ((size.width - totalGap) / segmentCount).coerceAtLeast(2f)
        val activeSegments = animatedRatio * segmentCount

        for (i in 0 until segmentCount) {
            val left = i * (segWidth + gap)
            val fillAlpha = when {
                i + 1 <= activeSegments -> 1f
                i < activeSegments -> (activeSegments - i).coerceIn(0.25f, 1f)
                else -> 0.14f
            }
            drawRoundRect(
                color = accentColor.copy(alpha = fillAlpha),
                topLeft = Offset(left, 0f),
                size = Size(segWidth, size.height),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
fun DigimonHoloCrestAvatar(
    speciesId: String,
    stage: String,
    isResting: Boolean,
    evolutionReady: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 116.dp
) {
    val spec = DigimonCatalog.getSpec(speciesId)
    val imageRes = DigimonCatalog.getMonsterImageRes(speciesId)
    val primaryColor = Color(spec.primaryColorHex)
    val secondaryColor = Color(spec.secondaryColorHex)

    val infiniteTransition = rememberInfiniteTransition(label = "holo_avatar")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (evolutionReady) 4500 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isResting) 2200 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            // Outer radial aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.42f),
                        secondaryColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR
                ),
                radius = maxR
            )

            // Rotating cyber-digivice telemetry ring
            rotate(degrees = ringRotation, pivot = center) {
                val ringRadius = maxR * 0.92f
                for (i in 0 until 6) {
                    drawArc(
                        color = if (i % 2 == 0) primaryColor else secondaryColor,
                        startAngle = i * 60f,
                        sweepAngle = 38f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Stage polygon frame around the portrait
            val sides = when (stage) {
                "Rookie" -> 4
                "Champion" -> 6
                "Ultimate" -> 8
                else -> 12
            }
            val polyPath = Path()
            val polyR = maxR * 0.82f * pulseScale
            for (i in 0 until sides) {
                val angleRad = Math.toRadians((i * (360.0 / sides)) - 90.0)
                val x = center.x + (polyR * cos(angleRad)).toFloat()
                val y = center.y + (polyR * sin(angleRad)).toFloat()
                if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
            }
            polyPath.close()

            drawPath(
                path = polyPath,
                color = primaryColor.copy(alpha = 0.65f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // High-detail Active Monster Image inside the holographic telemetry frame
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "${spec.speciesName} Portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp * 0.74f)
                .scale(pulseScale)
                .clip(CircleShape)
                .border(
                    BorderStroke(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(primaryColor, secondaryColor))
                    ),
                    shape = CircleShape
                )
        )
    }
}
