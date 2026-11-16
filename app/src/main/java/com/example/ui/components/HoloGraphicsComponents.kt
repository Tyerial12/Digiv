package com.example.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.DigimonCatalog
import com.example.ui.theme.BioEmeraldHealth
import com.example.ui.theme.CyberVoidBg
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.PlasmaVioletStamina
import com.example.ui.theme.SolarCoralHunger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class MonsterActionState(val label: String) {
    IDLE("IDLE"),
    SLEEPING("SLEEPING"),
    EATING("EATING"),
    TRAINING("TRAINING")
}

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
        // High-detail Active Digital Monster Image inside the holographic Digivice frame
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "${spec.speciesName} Digital Monster Portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp * 0.76f)
                .scale(pulseScale)
                .clip(RoundedCornerShape(22))
                .border(
                    BorderStroke(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(primaryColor, secondaryColor))
                    ),
                    shape = RoundedCornerShape(22)
                )
        )

        // Foreground Digivice HUD, Cyber-Scanlines, Corner V-Pet Brackets & Digital Crest Aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            // Outer radial digital aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR
                ),
                radius = maxR
            )

            // Subtle Digivice LCD horizontal scanlines across the portrait
            val boxSpan = maxR * 1.38f
            val leftX = center.x - boxSpan / 2f
            val rightX = center.x + boxSpan / 2f
            for (lineIdx in -3..3) {
                val y = center.y + lineIdx * (boxSpan / 8f)
                drawLine(
                    color = HoloCyanPrimary.copy(alpha = 0.14f),
                    start = Offset(leftX, y),
                    end = Offset(rightX, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // V-Pet Corner Targeting Brackets [ ]
            val bracketInset = maxR * 0.14f
            val bracketLen = maxR * 0.26f
            val strokeW = 2.2.dp.toPx()
            val bColor = if (evolutionReady) EvolutionGoldSecondary else primaryColor
            // Top-Left
            drawLine(bColor, Offset(bracketInset, bracketInset), Offset(bracketInset + bracketLen, bracketInset), strokeW, StrokeCap.Round)
            drawLine(bColor, Offset(bracketInset, bracketInset), Offset(bracketInset, bracketInset + bracketLen), strokeW, StrokeCap.Round)
            // Top-Right
            drawLine(bColor, Offset(size.width - bracketInset, bracketInset), Offset(size.width - bracketInset - bracketLen, bracketInset), strokeW, StrokeCap.Round)
            drawLine(bColor, Offset(size.width - bracketInset, bracketInset), Offset(size.width - bracketInset, bracketInset + bracketLen), strokeW, StrokeCap.Round)
            // Bottom-Left
            drawLine(bColor, Offset(bracketInset, size.height - bracketInset), Offset(bracketInset + bracketLen, size.height - bracketInset), strokeW, StrokeCap.Round)
            drawLine(bColor, Offset(bracketInset, size.height - bracketInset), Offset(bracketInset, size.height - bracketInset - bracketLen), strokeW, StrokeCap.Round)
            // Bottom-Right
            drawLine(bColor, Offset(size.width - bracketInset, size.height - bracketInset), Offset(size.width - bracketInset - bracketLen, size.height - bracketInset), strokeW, StrokeCap.Round)
            drawLine(bColor, Offset(size.width - bracketInset, size.height - bracketInset), Offset(size.width - bracketInset, size.height - bracketInset - bracketLen), strokeW, StrokeCap.Round)

            // Rotating cyber-digivice telemetry ring & binary data nodes
            rotate(degrees = ringRotation, pivot = center) {
                val ringRadius = maxR * 0.92f
                for (i in 0 until 6) {
                    drawArc(
                        color = if (i % 2 == 0) primaryColor else secondaryColor,
                        startAngle = i * 60f,
                        sweepAngle = 34f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val dotAngle = Math.toRadians((i * 60.0) + 47.0)
                    drawCircle(
                        color = EvolutionGoldSecondary,
                        radius = 2.4.dp.toPx(),
                        center = Offset(
                            center.x + (ringRadius * cos(dotAngle)).toFloat(),
                            center.y + (ringRadius * sin(dotAngle)).toFloat()
                        )
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
            val polyR = maxR * 0.84f * pulseScale
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
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}

/**
 * Animated Holo-Stage showcasing the monster performing live actions:
 * - SLEEPING: Gentle cryo-stasis float, breathing scale, stasis shield dome, and rising "Zzz" bubbles.
 * - EATING: Excited chomping squash-and-stretch bounce, flying cyber-rations/berries arcing into the monster, and satiety hearts.
 * - TRAINING: High-tempo combat lunges, neon shockwave rings, lightning slashes, and rising stat-boost arrows.
 * - IDLE: Hovering holo-scanline sweep and rotating cyber-crest telemetry.
 */
@Composable
fun AnimatedMonsterActionStage(
    speciesId: String,
    nickname: String,
    actionState: MonsterActionState,
    modifier: Modifier = Modifier,
    spriteSizeDp: Dp = 118.dp,
    imageTestTag: String? = null
) {
    val spec = DigimonCatalog.getSpec(speciesId)
    val imageRes = DigimonCatalog.getMonsterImageRes(speciesId)
    val primaryColor = Color(spec.primaryColorHex)
    val secondaryColor = Color(spec.secondaryColorHex)

    val infiniteTransition = rememberInfiniteTransition(label = "monster_action_stage")

    val masterPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (actionState) {
                    MonsterActionState.SLEEPING -> 3000
                    MonsterActionState.EATING -> 1100
                    MonsterActionState.TRAINING -> 750
                    MonsterActionState.IDLE -> 2400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "master_phase"
    )

    val bounceWave by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (actionState) {
                    MonsterActionState.SLEEPING -> 2200
                    MonsterActionState.EATING -> 420
                    MonsterActionState.TRAINING -> 320
                    MonsterActionState.IDLE -> 1500
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_wave"
    )

    val spriteOffsetX = when (actionState) {
        MonsterActionState.TRAINING -> (sin(masterPhase * 2f * PI.toFloat()) * 18f)
        MonsterActionState.EATING -> (sin(masterPhase * 2f * PI.toFloat()) * 4f)
        else -> 0f
    }
    val spriteOffsetY = when (actionState) {
        MonsterActionState.SLEEPING -> bounceWave * 7f
        MonsterActionState.EATING -> - kotlin.math.abs(bounceWave) * 14f
        MonsterActionState.TRAINING -> - kotlin.math.abs(cos(masterPhase * 2f * PI.toFloat())) * 12f
        MonsterActionState.IDLE -> bounceWave * 5f
    }
    val spriteScaleX = when (actionState) {
        MonsterActionState.EATING -> 1f + (bounceWave * 0.08f)
        MonsterActionState.TRAINING -> 1.04f + (bounceWave * 0.06f)
        MonsterActionState.SLEEPING -> 0.96f + (bounceWave * 0.03f)
        MonsterActionState.IDLE -> 1f + (bounceWave * 0.02f)
    }
    val spriteScaleY = when (actionState) {
        MonsterActionState.EATING -> 1f - (bounceWave * 0.08f)
        MonsterActionState.TRAINING -> 1.04f - (bounceWave * 0.05f)
        MonsterActionState.SLEEPING -> 0.96f - (bounceWave * 0.03f)
        MonsterActionState.IDLE -> 1f + (bounceWave * 0.02f)
    }
    val spriteRotation = when (actionState) {
        MonsterActionState.SLEEPING -> -6f + (bounceWave * 4f)
        MonsterActionState.EATING -> bounceWave * 7f
        MonsterActionState.TRAINING -> sin(masterPhase * 2f * PI.toFloat()) * 11f
        MonsterActionState.IDLE -> 0f
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background FX Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseR = size.minDimension * 0.36f

            when (actionState) {
                MonsterActionState.SLEEPING -> {
                    // Pulsing Cryo-Stasis Dome & soothing indigo/cyan rings
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PlasmaVioletStamina.copy(alpha = 0.36f),
                                HoloCyanPrimary.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseR * 1.45f
                        ),
                        radius = baseR * 1.45f,
                        center = center
                    )
                    val domeRadius = baseR * (1.02f + 0.08f * sin(masterPhase * 2f * PI.toFloat()))
                    drawCircle(
                        color = HoloCyanPrimary.copy(alpha = 0.55f),
                        radius = domeRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Floating cryo sleep bubbles
                    for (i in 0 until 5) {
                        val p = (masterPhase + i * 0.2f) % 1f
                        val bx = center.x + (i - 2) * 26.dp.toPx() + sin((p + i) * 6f) * 8.dp.toPx()
                        val by = center.y + baseR * 0.5f - (p * baseR * 1.6f)
                        drawCircle(
                            color = HoloCyanPrimary.copy(alpha = (1f - p) * 0.7f),
                            radius = (4f + i * 1.5f).dp.toPx(),
                            center = Offset(bx, by),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                MonsterActionState.EATING -> {
                    // Warm Nutrient Aura + flying Cyber-Ration / Berry morsels arcing toward center
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SolarCoralHunger.copy(alpha = 0.38f),
                                BioEmeraldHealth.copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseR * 1.45f
                        ),
                        radius = baseR * 1.45f,
                        center = center
                    )
                    // 2 alternating food projectiles arcing in from left and right
                    for (stream in 0..1) {
                        val t = (masterPhase + stream * 0.5f) % 1f
                        val startX = if (stream == 0) center.x - baseR * 1.5f else center.x + baseR * 1.5f
                        val startY = center.y + baseR * 0.85f
                        val curX = startX + (center.x - startX) * t
                        val arcHeight = sin(t * PI.toFloat()) * baseR * 0.95f
                        val curY = startY + (center.y - startY) * t - arcHeight
                        val morselColor = if (stream == 0) SolarCoralHunger else EvolutionGoldSecondary
                        drawCircle(
                            color = morselColor.copy(alpha = (1f - t * 0.3f).coerceIn(0f, 1f)),
                            radius = (10f * (1f - t * 0.35f)).dp.toPx(),
                            center = Offset(curX, curY)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = (4f * (1f - t * 0.35f)).dp.toPx(),
                            center = Offset(curX - 2.dp.toPx(), curY - 2.dp.toPx())
                        )
                    }
                    // Expanding Satiety Ring when food arrives
                    val ringProgress = (masterPhase * 2f) % 1f
                    drawCircle(
                        color = BioEmeraldHealth.copy(alpha = (1f - ringProgress) * 0.75f),
                        radius = baseR * (0.7f + ringProgress * 0.7f),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                MonsterActionState.TRAINING -> {
                    // High-intensity Combat Aura + Dual Shockwave Rings + Energy Slashes
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EvolutionGoldSecondary.copy(alpha = 0.42f),
                                primaryColor.copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseR * 1.55f
                        ),
                        radius = baseR * 1.55f,
                        center = center
                    )
                    for (wave in 0..1) {
                        val wPhase = (masterPhase + wave * 0.5f) % 1f
                        drawCircle(
                            color = if (wave == 0) EvolutionGoldSecondary.copy(alpha = (1f - wPhase) * 0.85f)
                            else HoloCyanPrimary.copy(alpha = (1f - wPhase) * 0.75f),
                            radius = baseR * (0.65f + wPhase * 0.95f),
                            center = center,
                            style = Stroke(width = (4f * (1f - wPhase * 0.5f)).dp.toPx())
                        )
                    }
                    // Orbiting combat sparks
                    for (s in 0 until 6) {
                        val angle = (masterPhase * 360f + s * 60f) * (PI / 180.0)
                        val r = baseR * 1.08f
                        val sx = center.x + (r * cos(angle)).toFloat()
                        val sy = center.y + (r * sin(angle)).toFloat()
                        drawCircle(
                            color = EvolutionGoldSecondary,
                            radius = 4.dp.toPx(),
                            center = Offset(sx, sy)
                        )
                    }
                }

                MonsterActionState.IDLE -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.34f),
                                secondaryColor.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseR * 1.35f
                        ),
                        radius = baseR * 1.35f,
                        center = center
                    )
                    // Scanning horizontal telemetry beam
                    val scanY = center.y - baseR + (masterPhase * baseR * 2f)
                    drawLine(
                        color = HoloCyanPrimary.copy(alpha = 0.45f),
                        start = Offset(center.x - baseR * 1.2f, scanY),
                        end = Offset(center.x + baseR * 1.2f, scanY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Animated Monster Sprite Portrait
        val imgModifier = Modifier
            .size(spriteSizeDp)
            .offset {
                IntOffset(
                    x = spriteOffsetX.dp.roundToPx(),
                    y = spriteOffsetY.dp.roundToPx()
                )
            }
            .graphicsLayer {
                scaleX = spriteScaleX
                scaleY = spriteScaleY
                rotationZ = spriteRotation
            }
            .clip(CircleShape)
            .background(CyberVoidBg.copy(alpha = 0.75f))
            .border(
                BorderStroke(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        when (actionState) {
                            MonsterActionState.SLEEPING -> listOf(PlasmaVioletStamina, HoloCyanPrimary)
                            MonsterActionState.EATING -> listOf(SolarCoralHunger, BioEmeraldHealth)
                            MonsterActionState.TRAINING -> listOf(EvolutionGoldSecondary, HoloCyanPrimary)
                            MonsterActionState.IDLE -> listOf(primaryColor, secondaryColor)
                        }
                    )
                ),
                shape = CircleShape
            )
            .let { mod ->
                if (imageTestTag != null) mod.testTag(imageTestTag) else mod
            }

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "$nickname ${actionState.label} Animation",
            contentScale = ContentScale.Crop,
            modifier = imgModifier
        )

        // Floating Foreground Action Callouts (Zzz, +SATIETY, +ATK/SPD)
        when (actionState) {
            MonsterActionState.SLEEPING -> {
                val zOffset = (-12f - masterPhase * 24f).roundToInt()
                Surface(
                    color = PlasmaVioletStamina.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, HoloCyanPrimary),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 46.dp, y = (18 + zOffset).dp)
                ) {
                    Text(
                        text = if (masterPhase < 0.5f) "Zzz... +STA" else "CRYO-SLEEP Zzz",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            MonsterActionState.EATING -> {
                val eatOffset = (-8f - masterPhase * 18f).roundToInt()
                Surface(
                    color = BioEmeraldHealth.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, EvolutionGoldSecondary),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 44.dp, y = (16 + eatOffset).dp)
                ) {
                    Text(
                        text = if (masterPhase < 0.5f) "NOM NOM! +HUNGER ♥" else "YUM! +SATIETY & HP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberVoidBg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            MonsterActionState.TRAINING -> {
                val trainOffset = (-10f - masterPhase * 20f).roundToInt()
                Surface(
                    color = EvolutionGoldSecondary.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, HoloCyanPrimary),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 46.dp, y = (16 + trainOffset).dp)
                ) {
                    Text(
                        text = if (masterPhase < 0.5f) "⚡ STRIKE! +STATS" else "🔥 DRILL COMBO! +LVL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberVoidBg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            MonsterActionState.IDLE -> Unit
        }
    }
}

/**
 * Animated Wild Taming Arena Stage:
 * Renders the Wild Monster inside a holographic containment ring and plays a multi-phase
 * Tamming Berry throw & capture wave animation whenever [berryThrowTrigger] increments,
 * plus a celebratory golden capture lock animation when [isTamed] is true.
 */
@Composable
fun AnimatedWildTamingStage(
    speciesId: String,
    speciesName: String,
    stage: String,
    tameProgress: Int,
    isTamed: Boolean,
    berryThrowTrigger: Int,
    lastRollResisted: Boolean,
    modifier: Modifier = Modifier
) {
    val spec = DigimonCatalog.getSpec(speciesId)
    val imageRes = DigimonCatalog.getMonsterImageRes(speciesId)
    val primaryColor = Color(spec.primaryColorHex)

    val throwAnim = remember { Animatable(0f) }
    LaunchedEffect(berryThrowTrigger) {
        if (berryThrowTrigger > 0) {
            throwAnim.snapTo(0.01f)
            throwAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
            )
            throwAnim.snapTo(0f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wild_taming_stage")
    val idleFloat by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wild_float"
    )
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isTamed) 3200 else 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wild_ring_angle"
    )

    val t = throwAnim.value
    val isBerryInFlight = t in 0.01f..0.55f
    val isImpactPhase = t > 0.55f

    val monsterShakeX = if (isImpactPhase) {
        sin((t - 0.55f) * 36f) * (if (lastRollResisted) 14f else 7f)
    } else 0f

    val monsterScale = when {
        isTamed -> 1.06f + idleFloat * 0.03f
        isImpactPhase -> 0.90f + sin((t - 0.55f) * 14f) * 0.12f
        else -> 1f + idleFloat * 0.03f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(178.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberVoidBg,
                        if (isTamed) EvolutionGoldSecondary.copy(alpha = 0.22f) else BioEmeraldHealth.copy(alpha = 0.14f),
                        CyberVoidBg
                    )
                )
            )
            .border(
                BorderStroke(
                    1.5.dp,
                    if (isTamed) EvolutionGoldSecondary else BioEmeraldHealth.copy(alpha = 0.65f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("wild_taming_animated_stage"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.38f

            // Containment Radar Grid Rings
            for (rStep in 1..3) {
                drawCircle(
                    color = (if (isTamed) EvolutionGoldSecondary else BioEmeraldHealth).copy(alpha = 0.14f),
                    radius = maxR * (0.5f + rStep * 0.28f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Rotating Tame Lock Ring
            rotate(degrees = ringAngle, pivot = center) {
                val ringR = maxR * 1.12f
                for (i in 0 until 4) {
                    drawArc(
                        color = if (isTamed) EvolutionGoldSecondary else BioEmeraldHealth,
                        startAngle = i * 90f,
                        sweepAngle = 48f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringR, center.y - ringR),
                        size = Size(ringR * 2, ringR * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Phase 1: Parabolic Tamming Berry Throw Trajectory (t in 0.01..0.55)
            if (isBerryInFlight) {
                val flightProgress = (t / 0.55f).coerceIn(0f, 1f)
                val startX = size.width * 0.14f
                val startY = size.height * 0.86f
                val curX = startX + (center.x - startX) * flightProgress
                val arcLift = sin(flightProgress * PI.toFloat()) * (size.height * 0.44f)
                val curY = startY + (center.y - startY) * flightProgress - arcLift

                // Sparkling trail behind the thrown Tamming Berry
                for (trail in 1..4) {
                    val tp = (flightProgress - trail * 0.06f).coerceIn(0f, 1f)
                    val tx = startX + (center.x - startX) * tp
                    val ty = startY + (center.y - startY) * tp - sin(tp * PI.toFloat()) * (size.height * 0.44f)
                    drawCircle(
                        color = EvolutionGoldSecondary.copy(alpha = (1f - trail * 0.22f)),
                        radius = (6 - trail).dp.toPx(),
                        center = Offset(tx, ty)
                    )
                }

                // Glowing Tamming Berry Orb
                drawCircle(
                    color = SolarCoralHunger,
                    radius = 12.dp.toPx(),
                    center = Offset(curX, curY)
                )
                drawCircle(
                    color = EvolutionGoldSecondary,
                    radius = 7.dp.toPx(),
                    center = Offset(curX - 2.dp.toPx(), curY - 2.dp.toPx())
                )
            }

            // Phase 2: Impact Taming Ripples & Capture Nodes (t > 0.55)
            if (isImpactPhase) {
                val impactProgress = ((t - 0.55f) / 0.45f).coerceIn(0f, 1f)
                val waveColor = if (lastRollResisted) SolarCoralHunger else BioEmeraldHealth
                drawCircle(
                    color = waveColor.copy(alpha = (1f - impactProgress) * 0.9f),
                    radius = maxR * (0.6f + impactProgress * 1.1f),
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
                for (i in 0 until 6) {
                    val ang = (i * 60f + impactProgress * 120f) * (PI / 180.0)
                    val dist = maxR * (0.5f + impactProgress * 0.9f)
                    val px = center.x + (dist * cos(ang)).toFloat()
                    val py = center.y + (dist * sin(ang)).toFloat()
                    drawCircle(
                        color = if (lastRollResisted) SolarCoralHunger else EvolutionGoldSecondary,
                        radius = (6f * (1f - impactProgress * 0.5f)).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Wild Monster Portrait
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Wild $speciesName",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .offset {
                    IntOffset(
                        x = monsterShakeX.dp.roundToPx(),
                        y = (idleFloat * 5f).dp.roundToPx()
                    )
                }
                .scale(monsterScale)
                .clip(CircleShape)
                .border(
                    BorderStroke(
                        width = 3.dp,
                        color = if (isTamed) EvolutionGoldSecondary else primaryColor
                    ),
                    shape = CircleShape
                )
        )

        // Top-Left Stage & Tame Status Pill
        Surface(
            color = CyberVoidBg.copy(alpha = 0.85f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, if (isTamed) EvolutionGoldSecondary else BioEmeraldHealth),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = when {
                    isTamed -> "★ TAMED & LOCKED: ${speciesName.uppercase()}!"
                    isBerryInFlight -> "🍒 THROWING TAMMING BERRY..."
                    isImpactPhase && lastRollResisted -> "⚡ WILD RESISTED! (+BOND PROGRESS)"
                    isImpactPhase -> "💚 BERRY HIT! TAME METER RISING!"
                    else -> "WILD $stage // TAME ${tameProgress}%"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isTamed) EvolutionGoldSecondary else BioEmeraldHealth,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
