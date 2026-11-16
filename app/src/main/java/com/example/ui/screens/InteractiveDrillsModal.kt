package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DigiRation
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.DrillType
import com.example.data.model.EvolutionBranchRequirement
import com.example.ui.components.AnimatedMonsterActionStage
import com.example.ui.components.DigimonHoloCrestAvatar
import com.example.ui.components.MonsterActionState
import com.example.ui.components.VisualEvolutionBranchingTreeCard
import com.example.ui.theme.BioEmeraldHealth
import com.example.ui.theme.CyberVoidBg
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCardElevated
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.HoloSlateSurface
import com.example.ui.theme.PlasmaVioletStamina
import com.example.ui.theme.SolarCoralHunger
import com.example.ui.theme.VirusCrimsonError
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrillsHubTab(
    activeCreature: DigimonCreature?,
    digiCoins: Int = DigimonCatalog.STARTING_DIGI_COINS,
    nextDigiCoinTickSec: Int = DigimonCatalog.DIGI_COIN_INTERVAL_SEC,
    onCompleteDrill: (DrillType, Int) -> Unit,
    onStart24hOfflineTraining: () -> Unit = {},
    onTogglePauseOfflineTraining: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeDrill by remember { mutableStateOf<DrillType?>(null) }
    val nowMs = System.currentTimeMillis()
    val isRecoveringFrom1Hp = activeCreature?.isRecoveringFrom1Hp(nowMs) == true
    val isInOfflineTraining = activeCreature?.isInOfflineTraining(nowMs) == true
    val isOfflineTrainingPaused = activeCreature?.isOfflineTrainingPaused() == true
    val hasOfflineTrainingSession = activeCreature?.hasOfflineTrainingSession(nowMs) == true
    val canAffordDrill = digiCoins >= DigimonCatalog.TRAINING_DRILL_COIN_COST && !isRecoveringFrom1Hp && !hasOfflineTrainingSession
    val canAfford24hOffline = digiCoins >= DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST && !isRecoveringFrom1Hp && !hasOfflineTrainingSession
    val nextCoinMins = nextDigiCoinTickSec / 60
    val nextCoinSecs = nextDigiCoinTickSec % 60
    val timerFormatted = "%d:%02d".format(java.util.Locale.US, nextCoinMins, nextCoinSecs)

    if (hasOfflineTrainingSession && activeDrill != null) {
        activeDrill = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "INTERACTIVE HOLO-TRAINING DRILLS",
            style = MaterialTheme.typography.headlineLarge,
            color = HoloCyanPrimary
        )

        // 1 HP 5-Minute Mandatory Recovery Lockout Warning Banner
        if (activeCreature != null && isRecoveringFrom1Hp) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drills_1hp_recovery_lockout_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                border = BorderStroke(1.5.dp, SolarCoralHunger)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚠️ 1 HP RECOVERY LOCKOUT (${activeCreature.formattedRecoveryRemaining(nowMs)} LEFT)",
                        style = MaterialTheme.typography.titleMedium,
                        color = SolarCoralHunger
                    )
                    Text(
                        text = "${activeCreature.nickname} dropped to 1 HP battling a wild monster! Recovery lasts 5 minutes — you cannot train or do anything with ${activeCreature.nickname} until recovery finishes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }

        // 24-Hour Offline Auto-Training (100 DigiCoins for 24 Hours when App is Closed, with Pause & Restart Timer) Card
        val offlineCardColor = when {
            isOfflineTrainingPaused -> SolarCoralHunger
            isInOfflineTraining -> BioEmeraldHealth
            else -> EvolutionGoldSecondary
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline_training_24h_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
            border = BorderStroke(
                width = 1.5.dp,
                color = offlineCardColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "24-HOUR OFFLINE AUTO-TRAINING",
                        style = MaterialTheme.typography.titleMedium,
                        color = offlineCardColor
                    )
                    Surface(
                        color = offlineCardColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, offlineCardColor)
                    ) {
                        Text(
                            text = when {
                                isOfflineTrainingPaused && activeCreature != null ->
                                    "PAUSED • ${activeCreature.formattedOfflineTrainingRemaining(nowMs)} SAVED"
                                isInOfflineTraining && activeCreature != null ->
                                    "ACTIVE • ${activeCreature.formattedOfflineTrainingRemaining(nowMs)} LEFT"
                                else ->
                                    "${DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST} DIGICOINS / 24 HRS"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = offlineCardColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        isOfflineTrainingPaused && activeCreature != null ->
                            "⏸ Offline Auto-Training is PAUSED (${activeCreature.formattedOfflineTrainingRemaining(nowMs)} remaining). The countdown timer is frozen and will not tick down until you restart offline training! Once in offline training, you cannot train normally."
                        isInOfflineTraining && activeCreature != null ->
                            "✓ ${activeCreature.nickname} is in 24-Hour Offline Auto-Training (${activeCreature.formattedOfflineTrainingRemaining(nowMs)} remaining)! Continues leveling (+2 ATK, +2 DEF, +2 SPD, +3 WIS & +4% Sync every 15m) even when the app is closed. Once in offline training, you cannot train normally — tap below to pause the timer anytime."
                        else ->
                            "Train ${activeCreature?.nickname ?: "your monster"} automatically for 24 hours — even when the app is closed — for ${DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST} DigiCoins! Once in offline training you cannot train normally, and you can pause & restart the 24h timer anytime."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Button(
                    onClick = {
                        if (hasOfflineTrainingSession) {
                            onTogglePauseOfflineTraining()
                        } else {
                            onStart24hOfflineTraining()
                        }
                    },
                    enabled = activeCreature != null && !isRecoveringFrom1Hp && (hasOfflineTrainingSession || canAfford24hOffline),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isOfflineTrainingPaused -> BioEmeraldHealth
                            isInOfflineTraining -> SolarCoralHunger
                            else -> EvolutionGoldSecondary
                        },
                        contentColor = CyberVoidBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .testTag("start_24h_offline_training_button")
                ) {
                    Text(
                        text = when {
                            isRecoveringFrom1Hp && activeCreature != null ->
                                "RECOVERING FROM 1 HP (${activeCreature.formattedRecoveryRemaining(nowMs)} LEFT)"
                            isOfflineTrainingPaused && activeCreature != null ->
                                "▶ RESTART OFFLINE TRAINING (RESUME ${activeCreature.formattedOfflineTrainingRemaining(nowMs)} TIMER)"
                            isInOfflineTraining && activeCreature != null ->
                                "⏸ PAUSE OFFLINE TRAINING (FREEZE TIMER AT ${activeCreature.formattedOfflineTrainingRemaining(nowMs)})"
                            digiCoins >= DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST ->
                                "START 24H OFFLINE TRAINING (${DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST} DIGICOINS)"
                            else ->
                                "NEED ${DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST} DIGICOINS FOR 24H OFFLINE TRAINING ($digiCoins/${DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST})"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // DigiCoin Training Economy & Leveling Rules Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("digicoin_economy_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
            border = BorderStroke(
                1.5.dp,
                if (canAffordDrill) EvolutionGoldSecondary else SolarCoralHunger
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DIGICOINS: $digiCoins",
                        style = MaterialTheme.typography.headlineMedium,
                        color = EvolutionGoldSecondary,
                        modifier = Modifier.testTag("drills_digicoin_balance_text")
                    )
                    Surface(
                        color = HoloCyanPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "+5 COINS IN $timerFormatted (EVERY 5M)",
                            style = MaterialTheme.typography.labelSmall,
                            color = HoloCyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "Cost: ${DigimonCatalog.TRAINING_DRILL_COIN_COST} DigiCoins per Drill OR ${DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST} DigiCoins for 24h Offline Auto-Training.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = if (activeCreature != null) {
                        "Active: ${activeCreature.nickname} • LVL ${activeCreature.currentLevel()}/100 • Sync ${activeCreature.evolutionProgress}% • Level ONLY via Training & evolve at LVL 20+!"
                    } else {
                        "Monsters level ONLY with Training Drills and can only evolve once reaching LVL 20+!"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canAffordDrill) BioEmeraldHealth else SolarCoralHunger
                )
            }
        }

        if (activeDrill != null && activeCreature != null && !isRecoveringFrom1Hp && !hasOfflineTrainingSession) {
            InteractiveMiniDrillCard(
                creature = activeCreature,
                drillType = activeDrill!!,
                onFinishDrill = { score ->
                    onCompleteDrill(activeDrill!!, score)
                    activeDrill = null
                },
                onCancel = { activeDrill = null }
            )
        } else {
            DrillType.entries.forEach { drill ->
                val accent = when (drill) {
                    DrillType.POWER -> Color(0xFFFF7043)
                    DrillType.SPEED -> PlasmaVioletStamina
                    DrillType.DEFENSE -> HoloCyanPrimary
                    DrillType.WISDOM -> BioEmeraldHealth
                }
                val icon = when (drill) {
                    DrillType.POWER -> Icons.Default.FitnessCenter
                    DrillType.SPEED -> Icons.Default.Speed
                    DrillType.DEFENSE -> Icons.Default.Shield
                    DrillType.WISDOM -> Icons.Default.Bolt
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drill_card_${drill.name.lowercase()}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
                    border = BorderStroke(1.2.dp, accent.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = drill.shortTitle,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${drill.shortTitle} (${drill.activityName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Level Gain: +${drill.primaryStatLabel} & +EVO Sync",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                            }
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "-${DigimonCatalog.TRAINING_DRILL_COIN_COST} COINS • -${drill.staminaCost} STA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = drill.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { activeDrill = drill },
                            enabled = canAffordDrill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = CyberVoidBg
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .testTag("start_drill_${drill.name.lowercase()}")
                        ) {
                            Text(
                                text = when {
                                    isRecoveringFrom1Hp && activeCreature != null ->
                                        "RECOVERING FROM 1 HP (${activeCreature.formattedRecoveryRemaining(nowMs)} LEFT)"
                                    isOfflineTrainingPaused && activeCreature != null ->
                                        "LOCKED — IN OFFLINE TRAINING (PAUSED • ${activeCreature.formattedOfflineTrainingRemaining(nowMs)} LEFT)"
                                    isInOfflineTraining && activeCreature != null ->
                                        "LOCKED — CANNOT TRAIN NORMALLY IN OFFLINE TRAINING (${activeCreature.formattedOfflineTrainingRemaining(nowMs)} LEFT)"
                                    canAffordDrill ->
                                        "START TRAINING DRILL (${DigimonCatalog.TRAINING_DRILL_COIN_COST} DIGICOINS)"
                                    else ->
                                        "NEED ${DigimonCatalog.TRAINING_DRILL_COIN_COST} DIGICOINS ($digiCoins/${DigimonCatalog.TRAINING_DRILL_COIN_COST} • +5 IN $timerFormatted)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveMiniDrillCard(
    creature: DigimonCreature,
    drillType: DrillType,
    onFinishDrill: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "drill_cursor")
    val cursorPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_pos"
    )

    var currentRound by remember { mutableIntStateOf(1) }
    var accumulatedScore by remember { mutableIntStateOf(0) }
    var lastHitFeedback by remember { mutableStateOf("Tap LOCK PULSE when the neon marker enters the Gold Center Zone!") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_drill_arena"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
        border = BorderStroke(2.dp, HoloCyanPrimary)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "${drillType.shortTitle} // ROUND $currentRound OF 3",
                style = MaterialTheme.typography.headlineMedium,
                color = EvolutionGoldSecondary
            )

            AnimatedMonsterActionStage(
                speciesId = creature.speciesId,
                nickname = creature.nickname,
                actionState = MonsterActionState.TRAINING,
                spriteSizeDp = 96.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            )

            Text(
                text = lastHitFeedback,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            // Oscillating Timing Gauge
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                // Background bar
                drawRoundRect(
                    color = CyberVoidBg,
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                // Sweet-spot center zone (0.35 to 0.65)
                val zoneLeft = size.width * 0.35f
                val zoneWidth = size.width * 0.30f
                drawRoundRect(
                    color = EvolutionGoldSecondary.copy(alpha = 0.35f),
                    topLeft = Offset(zoneLeft, 4.dp.toPx()),
                    size = Size(zoneWidth, size.height - 8.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                // Critical bullseye (0.45 to 0.55)
                val bullLeft = size.width * 0.45f
                val bullWidth = size.width * 0.10f
                drawRoundRect(
                    color = BioEmeraldHealth.copy(alpha = 0.65f),
                    topLeft = Offset(bullLeft, 4.dp.toPx()),
                    size = Size(bullWidth, size.height - 8.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Moving cursor
                val cursorX = (cursorPosition * (size.width - 16.dp.toPx())) + 8.dp.toPx()
                drawCircle(
                    color = HoloCyanPrimary,
                    radius = 12.dp.toPx(),
                    center = Offset(cursorX, size.height / 2f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("ABORT", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        val distance = abs(cursorPosition - 0.5f)
                        val roundScore = when {
                            distance <= 0.08f -> 35
                            distance <= 0.18f -> 28
                            else -> 20
                        }
                        val nextTotal = (accumulatedScore + roundScore).coerceAtMost(100)
                        lastHitFeedback = when {
                            roundScore >= 35 -> "CRITICAL SYNC! (+35 pts)"
                            roundScore >= 28 -> "SOLID HIT! (+28 pts)"
                            else -> "GRAZED PULSE (+20 pts)"
                        }
                        if (currentRound >= 3) {
                            onFinishDrill(nextTotal)
                        } else {
                            accumulatedScore = nextTotal
                            currentRound++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HoloCyanPrimary,
                        contentColor = CyberVoidBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .testTag("drill_strike_pulse_button")
                ) {
                    Text(
                        text = if (currentRound < 3) "LOCK PULSE ($currentRound/3)" else "COMPLETE DRILL",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberFeedBottomSheet(
    creature: DigimonCreature,
    foodInventory: Map<DigiRation, Int> = emptyMap(),
    onSelectRation: (DigiRation) -> Unit,
    onGoToStoreTab: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val totalOwned = foodInventory.values.sum()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HoloSlateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "FOOD BAG // FEED ${creature.nickname.uppercase()}",
                style = MaterialTheme.typography.headlineMedium,
                color = SolarCoralHunger
            )
            AnimatedMonsterActionStage(
                speciesId = creature.speciesId,
                nickname = creature.nickname,
                actionState = MonsterActionState.EATING,
                spriteSizeDp = 84.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            )
            Text(
                text = "Satiety: ${creature.hunger}% • HP: ${creature.health}% • STA: ${creature.stamina}% • Food in Bag: $totalOwned (Must buy/claim food in the Store tab first to feed your pet!)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    onDismiss()
                    onGoToStoreTab()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvolutionGoldSecondary,
                    contentColor = CyberVoidBg
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .testTag("feed_sheet_go_to_store_button")
            ) {
                Text(
                    text = "GO TO STORE TAB TO BUY FOOD & TAMMING BERRIES",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            DigiRation.entries.forEach { ration ->
                val owned = foodInventory[ration] ?: 0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (owned > 0) {
                                onSelectRation(ration)
                                onDismiss()
                            } else {
                                onDismiss()
                                onGoToStoreTab()
                            }
                        }
                        .testTag("ration_item_${ration.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(
                        1.dp,
                        if (owned > 0) BioEmeraldHealth.copy(alpha = 0.7f) else SolarCoralHunger.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = ration.displayName,
                                tint = if (owned > 0) BioEmeraldHealth else SolarCoralHunger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${ration.displayName} • In Bag: $owned",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = ration.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                if (owned > 0) {
                                    onSelectRation(ration)
                                    onDismiss()
                                } else {
                                    onDismiss()
                                    onGoToStoreTab()
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (owned > 0) BioEmeraldHealth.copy(alpha = 0.22f) else EvolutionGoldSecondary.copy(alpha = 0.22f),
                                contentColor = if (owned > 0) BioEmeraldHealth else EvolutionGoldSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (owned > 0) "FEED ($owned)" else "BUY IN STORE",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolutionMatrixBottomSheet(
    creature: DigimonCreature,
    trainingLogs: List<com.example.data.model.TrainingLogEntry> = emptyList(),
    onTriggerEvolution: (EvolutionBranchRequirement) -> Unit,
    onDismiss: () -> Unit
) {
    val spec = DigimonCatalog.getSpec(creature.speciesId)
    val branches = spec.evolutionBranches
    val raisingProgress = com.example.data.model.CreatureRaisingProgress.compute(creature, trainingLogs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HoloSlateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "BRANCHING DIGIVOLUTION MATRIX",
                style = MaterialTheme.typography.headlineLarge,
                color = EvolutionGoldSecondary
            )
            Text(
                text = "${creature.nickname} • LVL ${creature.currentLevel()}/100 (${creature.speciesName} • ${creature.stage}) — Sync: ${creature.evolutionProgress}% • Age: ${raisingProgress.formattedTotalAge()}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            // Interactive Visual Branching Evolution Tree inside Evolution Matrix BottomSheet
            VisualEvolutionBranchingTreeCard(
                creature = creature,
                raisingProgress = raisingProgress,
                onSelectBranchToEvolve = { branch: EvolutionBranchRequirement ->
                    onTriggerEvolution(branch)
                    if (branch.isFullyMetBy(creature, raisingProgress)) {
                        onDismiss()
                    }
                }
            )

            // Per-Monster Level 100 Rare Evolution Rule Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                border = BorderStroke(
                    1.dp,
                    if (creature.isMaxLevel100()) EvolutionGoldSecondary else HoloCyanPrimary.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "6 RARE LEVEL-100 MYTHIC MONSTERS // PER-MONSTER RULE",
                        style = MaterialTheme.typography.labelLarge,
                        color = EvolutionGoldSecondary
                    )
                    Text(
                        text = if (creature.isMaxLevel100()) {
                            "${creature.nickname} is at MAX LVL 100! This monster is individually qualified to evolve into the 6 Rare Level-100 Monsters once at Mega stage."
                        } else {
                            "${creature.nickname} is currently LVL ${creature.currentLevel()}/100 (${100 - creature.currentLevel()} levels remaining). Each monster you own must individually reach LVL 100 to evolve into one of the 6 Rare Monsters—if another monster is LVL 100, only that LVL 100 monster can evolve into a Rare form."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }

            // Stage Level & Training Roadmap Card
            if (branches.isNotEmpty()) {
                val anyBranchReady = branches.any { it.isStatsAndSyncMetBy(creature) }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                    border = BorderStroke(
                        1.dp,
                        if (anyBranchReady) BioEmeraldHealth else HoloCyanPrimary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "STAGE LEVEL & EVOLUTION REQUIREMENTS (${creature.stage.uppercase()} ➔ NEXT STAGE)",
                            style = MaterialTheme.typography.labelLarge,
                            color = HoloCyanPrimary
                        )
                        Text(
                            text = "★ Current Level: LVL ${creature.currentLevel()}/100 • Evolution Sync: ${creature.evolutionProgress}/100%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (anyBranchReady) BioEmeraldHealth else EvolutionGoldSecondary
                        )
                        Text(
                            text = "⚡ Career Training: Drills ${raisingProgress.drillsCompleted} • Meals ${raisingProgress.mealsFed} • Sparring ${raisingProgress.sparringMatches}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Apex Roadmap: Rookie ➔ Champion (LVL 20+) ➔ Ultimate (LVL 40+) ➔ Mega (LVL 65+) ➔ Rare Mythic (LVL 100) • Levels gained ONLY via Training Drills (10 DigiCoins/drill)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (branches.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.dp, EvolutionGoldSecondary)
                ) {
                    Text(
                        text = "${creature.nickname} (${creature.speciesName}) has ascended to a Level-100 Rare Mythic form! Continue training drills, trading with real players, and sparring on the global Tamer Leaderboard.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EvolutionGoldSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                branches.forEach { branch ->
                    val statsQualified = branch.isStatsAndSyncMetBy(creature)
                    val canEvolveNow = statsQualified

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("evo_branch_${branch.targetSpeciesId}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                        border = BorderStroke(
                            width = if (canEvolveNow) 2.dp else 1.dp,
                            color = if (canEvolveNow) EvolutionGoldSecondary else HoloCyanPrimary.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DigimonHoloCrestAvatar(
                                        speciesId = branch.targetSpeciesId,
                                        stage = branch.targetStage,
                                        isResting = false,
                                        evolutionReady = canEvolveNow,
                                        sizeDp = 54.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = branch.targetSpeciesName,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (branch.isRareLevel100Branch) {
                                                "RARE LVL-100 MYTHIC • ${branch.targetAttribute}"
                                            } else {
                                                "${branch.targetStage} • ${branch.targetAttribute} • Req LVL ${branch.requiredLevel}+"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EvolutionGoldSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (canEvolveNow) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (canEvolveNow) BioEmeraldHealth else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = branch.branchDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Requirement tags including Per-Monster Level Gate
                            val reqs = buildList {
                                if (branch.requiredLevel > 1) {
                                    add("LVL ${creature.currentLevel()}/${branch.requiredLevel}")
                                } else {
                                    add("LVL ${creature.currentLevel()}/100")
                                }
                                add("SYNC ${creature.evolutionProgress}/100%")
                                if (branch.minAttack > 0) add("ATK ${creature.attack}/${branch.minAttack}")
                                if (branch.minDefense > 0) add("DEF ${creature.defense}/${branch.minDefense}")
                                if (branch.minSpeed > 0) add("SPD ${creature.speed}/${branch.minSpeed}")
                                if (branch.minWisdom > 0) add("WIS ${creature.wisdom}/${branch.minWisdom}")
                                add("BOND ${creature.bondLevel}/${branch.minBond}")
                            }
                            Text(
                                text = "Gates: ${reqs.joinToString(" • ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statsQualified) BioEmeraldHealth else HoloCyanPrimary
                            )

                            val buttonLabel = when {
                                canEvolveNow -> "DIGIVOLVE TO ${branch.targetSpeciesName.uppercase()} NOW"
                                creature.currentLevel() < branch.requiredLevel ->
                                    "TRAIN TO LVL ${branch.requiredLevel} (NOW LVL ${creature.currentLevel()})"
                                creature.evolutionProgress < 100 ->
                                    "REACH 100% SYNC (${100 - creature.evolutionProgress}% LEFT)"
                                else -> "TRAIN STATS & BOND TO UNLOCK BRANCH"
                            }

                            Button(
                                onClick = {
                                    onTriggerEvolution(branch)
                                    if (canEvolveNow) {
                                        onDismiss()
                                    }
                                },
                                enabled = canEvolveNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EvolutionGoldSecondary,
                                    contentColor = CyberVoidBg
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .testTag("trigger_digivolve_${branch.targetSpeciesId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = buttonLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // If this monster is not yet at Mega stage, show a preview of the 6 Rare Level-100 Monsters
                if (creature.stage != "Mega" && !creature.isRareLevel100Form()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                        border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "6 RARE MONSTERS UNLOCKED AT MEGA + LVL 100",
                                style = MaterialTheme.typography.labelLarge,
                                color = EvolutionGoldSecondary
                            )
                            Text(
                                text = "Once ${creature.nickname} reaches Mega stage AND individually reaches Level 100 (currently LVL ${creature.currentLevel()}/100), it can evolve into any of these 6 Rare Mythic Monsters:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DigimonCatalog.level100RareBranches.forEach { rareBranch ->
                                Text(
                                    text = "★ ${rareBranch.targetSpeciesName} (${rareBranch.targetAttribute}) — Requires LVL 100 on this monster",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HoloCyanPrimary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StarterTrioDraftContent(
    onConfirmDraft: (List<com.example.data.model.StarterDraftChoice>) -> Unit,
    modifier: Modifier = Modifier
) {
    val rookieStarterSpecs = remember {
        DigimonCatalog.starterSpecs.filter { it.stage == "Rookie" && !it.isRareLevel100Spec }
    }
    var selectedSpeciesId by remember {
        mutableStateOf(rookieStarterSpecs.firstOrNull()?.speciesId ?: "agumon")
    }
    var customNickname by remember { mutableStateOf("") }
    var stageLockNotice by remember { mutableStateOf<String?>(null) }

    val chosenSpec = DigimonCatalog.requireRookieStarterSpec(selectedSpeciesId)

    val submitDraft: () -> Unit = {
        if (chosenSpec.stage == "Rookie" && !chosenSpec.isRareLevel100Spec) {
            onConfirmDraft(
                listOf(
                    com.example.data.model.StarterDraftChoice(
                        speciesId = chosenSpec.speciesId,
                        customNickname = customNickname
                    )
                )
            )
        } else {
            stageLockNotice = "Only a Rookie-stage monster can be chosen as your starter monster!"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sticky Header + Quick Continue Bar at Top
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
            border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CHOOSE 1 OF 10 ROOKIE STARTERS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = HoloCyanPrimary
                        )
                        Text(
                            text = "10 ROOKIE CHOICES • UNIQUE STATS & ABILITIES • 200 DIGICOINS (NEW GAME)",
                            style = MaterialTheme.typography.labelSmall,
                            color = BioEmeraldHealth
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = submitDraft,
                        enabled = chosenSpec.stage == "Rookie",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HoloCyanPrimary,
                            contentColor = CyberVoidBg
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("top_confirm_three_starters_button")
                    ) {
                        Text(
                            text = "CONTINUE ➔",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Text(
                    text = "New Player Guide: Pick 1 of the 10 Rookie Digital Monsters below! Each starter has distinct ATK/DEF/SPD/WIS stats, a Signature Move, and a unique Passive Ability. You start a New Game with 200 DigiCoins and evolve at LVL 20+!",
                    style = MaterialTheme.typography.bodySmall,
                    color = EvolutionGoldSecondary
                )

                // Stage Gate Indicator Strip showing Rookie Allowed vs Locked Higher Stages
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = BioEmeraldHealth.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BioEmeraldHealth),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { stageLockNotice = null }
                    ) {
                        Text(
                            text = "✓ ROOKIE (10)",
                            style = MaterialTheme.typography.labelSmall,
                            color = BioEmeraldHealth,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp)
                        )
                    }
                    listOf("CHAMPION", "ULTIMATE", "MEGA").forEach { lockedStage ->
                        Surface(
                            color = VirusCrimsonError.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, VirusCrimsonError.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    stageLockNotice =
                                        "LOCKED: You can ONLY choose a Rookie monster for your starter! $lockedStage monsters must be evolved from a Rookie."
                                }
                        ) {
                            Text(
                                text = "🔒 $lockedStage",
                                style = MaterialTheme.typography.labelSmall,
                                color = VirusCrimsonError,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                if (stageLockNotice != null) {
                    Surface(
                        color = VirusCrimsonError.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, VirusCrimsonError)
                    ) {
                        Text(
                            text = stageLockNotice.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Selected Digital Monster Live Spotlight Preview (Stats + Unique Ability)
                val chosenAccent = Color(chosenSpec.primaryColorHex)
                Surface(
                    color = HoloSlateSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, chosenAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DigimonHoloCrestAvatar(
                            speciesId = chosenSpec.speciesId,
                            stage = "Rookie",
                            isResting = false,
                            evolutionReady = true,
                            sizeDp = 56.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "SELECTED: ${chosenSpec.speciesName.uppercase()} (${chosenSpec.attribute.uppercase()} ROOKIE)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "ATK ${chosenSpec.baseAttack} • DEF ${chosenSpec.baseDefense} • SPD ${chosenSpec.baseSpeed} • WIS ${chosenSpec.baseWisdom} • Move: ${chosenSpec.signatureMove}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HoloCyanPrimary
                            )
                            Text(
                                text = "⚡ ABILITY — ${chosenSpec.resolvedAbilityName()}: ${chosenSpec.resolvedAbilityDescription()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EvolutionGoldSecondary
                            )
                        }
                    }
                }
            }
        }

        // Scrollable 10 Rookie Starter Cards & Nickname
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rookieStarterSpecs.forEachIndexed { idx, spec ->
                val isSelected = spec.speciesId == chosenSpec.speciesId
                val accent = Color(spec.primaryColorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (spec.stage == "Rookie" && !spec.isRareLevel100Spec) {
                                selectedSpeciesId = spec.speciesId
                                stageLockNotice = null
                            }
                        }
                        .testTag("starter_draft_card_${spec.speciesId}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) accent.copy(alpha = 0.20f) else HoloCardElevated
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) accent else accent.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DigimonHoloCrestAvatar(
                            speciesId = spec.speciesId,
                            stage = "Rookie",
                            isResting = false,
                            evolutionReady = isSelected,
                            sizeDp = 52.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${idx + 1} ${spec.speciesName} — ROOKIE (LVL 1)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = if (isSelected) accent else BioEmeraldHealth.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (isSelected) "SELECTED" else spec.attribute.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) CyberVoidBg else BioEmeraldHealth,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "ATK ${spec.baseAttack} • DEF ${spec.baseDefense} • SPD ${spec.baseSpeed} • WIS ${spec.baseWisdom} • Move: ${spec.signatureMove}",
                                style = MaterialTheme.typography.labelMedium,
                                color = HoloCyanPrimary
                            )
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "⚡ Ability: ${spec.resolvedAbilityName()} — ${spec.resolvedAbilityDescription()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "OPTIONAL NICKNAME FOR YOUR ROOKIE STARTER (${chosenSpec.speciesName.uppercase()})",
                style = MaterialTheme.typography.labelLarge,
                color = HoloCyanPrimary
            )
            OutlinedTextField(
                value = customNickname,
                onValueChange = { customNickname = it },
                label = { Text("Starter Nickname (Default: ${chosenSpec.speciesName})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Always-Visible Sticky Bottom Continue Button
        Button(
            onClick = submitDraft,
            enabled = chosenSpec.stage == "Rookie",
            colors = ButtonDefaults.buttonColors(
                containerColor = HoloCyanPrimary,
                contentColor = CyberVoidBg
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("confirm_three_starters_button")
        ) {
            Text(
                text = "START NEW GAME WITH ROOKIE ${chosenSpec.speciesName.uppercase()} & 200 DIGICOINS ➔",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarterTrioDraftBottomSheet(
    onConfirmDraft: (List<com.example.data.model.StarterDraftChoice>) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HoloSlateSurface
    ) {
        StarterTrioDraftContent(
            onConfirmDraft = { choices ->
                onConfirmDraft(choices)
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatchDigiEggBottomSheet(
    onHatch: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val rookieSpecs = remember {
        DigimonCatalog.starterSpecs.filter { it.stage == "Rookie" && !it.isRareLevel100Spec }
    }
    var selectedSpeciesId by remember { mutableStateOf("guilmon") }
    var customNickname by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HoloSlateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "HATCH NEW ROOKIE DIGI-EGG PARTNER",
                style = MaterialTheme.typography.headlineLarge,
                color = HoloCyanPrimary
            )

            OutlinedTextField(
                value = customNickname,
                onValueChange = { customNickname = it },
                label = { Text("Partner Nickname (Optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hatch_nickname_input")
            )

            rookieSpecs.forEach { spec ->
                val isSelected = spec.speciesId == selectedSpeciesId
                val accent = Color(spec.primaryColorHex)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSpeciesId = spec.speciesId },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) accent.copy(alpha = 0.2f) else HoloCardElevated
                    ),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, accent)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DigimonHoloCrestAvatar(
                            speciesId = spec.speciesId,
                            stage = "Rookie",
                            isResting = false,
                            evolutionReady = false,
                            sizeDp = 46.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${spec.speciesName} (${spec.attribute} • Rookie) ➔ Apex: ${spec.apexTargetName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Move: ${spec.signatureMove} • ${spec.titleTag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val safeRookie = DigimonCatalog.requireRookieStarterSpec(selectedSpeciesId)
                    onHatch(safeRookie.speciesId, customNickname)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HoloCyanPrimary,
                    contentColor = CyberVoidBg
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("confirm_hatch_button")
            ) {
                Text(
                    text = "HATCH & SYNCHRONIZE ROOKIE PARTNER",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
