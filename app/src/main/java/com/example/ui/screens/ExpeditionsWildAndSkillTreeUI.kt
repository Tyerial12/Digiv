package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.ExpeditionSpec
import com.example.data.model.MonsterSkillNode
import com.example.data.model.WildEncounterState
import com.example.ui.components.AnimatedWildTamingStage
import com.example.ui.components.DigimonHoloCrestAvatar
import com.example.ui.theme.BioEmeraldHealth
import com.example.ui.theme.CyberVoidBg
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCardElevated
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.HoloSlateSurface
import com.example.ui.theme.PlasmaVioletStamina
import com.example.ui.theme.SolarCoralHunger
import com.example.ui.theme.VirusCrimsonError

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WildAndExpeditionsBottomSheet(
    activeCreature: DigimonCreature?,
    squadCreatures: List<DigimonCreature>,
    digiCoins: Int,
    tammingBerryCount: Int = 0,
    wildEncounter: WildEncounterState?,
    pendingTamedCreature: DigimonCreature?,
    initialTabIndex: Int = 0, // 0 = Wild Taming (100 Monsters), 1 = 10 Expeditions (Up to 24h)
    onScanWildMonster: (String?) -> Unit,
    onThrowTammingBerry: () -> Unit,
    onReplaceSquadMonster: (String) -> Unit,
    onDismissPendingTamed: () -> Unit,
    onStartExpedition: (ExpeditionSpec) -> Unit,
    onCompleteOrRecallExpedition: (String) -> Unit,
    onOpenFeedSheet: () -> Unit,
    onGoToStoreTab: () -> Unit = {},
    onTriggerCryoRest: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HoloSlateSurface
    ) {
        WildAndExpeditionsTabContent(
            activeCreature = activeCreature,
            squadCreatures = squadCreatures,
            digiCoins = digiCoins,
            tammingBerryCount = tammingBerryCount,
            wildEncounter = wildEncounter,
            pendingTamedCreature = pendingTamedCreature,
            initialTabIndex = initialTabIndex,
            onScanWildMonster = onScanWildMonster,
            onThrowTammingBerry = onThrowTammingBerry,
            onReplaceSquadMonster = onReplaceSquadMonster,
            onDismissPendingTamed = onDismissPendingTamed,
            onStartExpedition = onStartExpedition,
            onCompleteOrRecallExpedition = onCompleteOrRecallExpedition,
            onOpenFeedSheet = onOpenFeedSheet,
            onGoToStoreTab = {
                onDismiss()
                onGoToStoreTab()
            },
            onTriggerCryoRest = onTriggerCryoRest
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WildAndExpeditionsTabContent(
    activeCreature: DigimonCreature?,
    squadCreatures: List<DigimonCreature>,
    digiCoins: Int,
    tammingBerryCount: Int = 0,
    wildEncounter: WildEncounterState?,
    pendingTamedCreature: DigimonCreature?,
    initialTabIndex: Int = 0, // 0 = Wild Taming (100 Monsters), 1 = 10 Expeditions (Up to 24h)
    onScanWildMonster: (String?) -> Unit,
    onThrowTammingBerry: () -> Unit,
    onReplaceSquadMonster: (String) -> Unit,
    onDismissPendingTamed: () -> Unit,
    onStartExpedition: (ExpeditionSpec) -> Unit,
    onCompleteOrRecallExpedition: (String) -> Unit,
    onOpenFeedSheet: () -> Unit,
    onGoToStoreTab: () -> Unit = {},
    onTriggerCryoRest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex) }
    var showDexBrowser by remember { mutableStateOf(false) }
    var berryThrowTrigger by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // Top Switcher: Wild Taming (100 Monsters) vs 10 Expeditions (Up to 24h)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedSection = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSection == 0) BioEmeraldHealth else HoloCardElevated,
                        contentColor = if (selectedSection == 0) CyberVoidBg else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("switch_wild_taming_section")
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WILD TAMING (100)", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { selectedSection = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSection == 1) HoloCyanPrimary else HoloCardElevated,
                        contentColor = if (selectedSection == 1) CyberVoidBg else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("switch_expeditions_section")
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("10 EXPEDITIONS (24H)", style = MaterialTheme.typography.labelLarge)
                }
            }

            // If 4/4 Squad is full after taming a wild monster, show Swap Prompt
            if (pendingTamedCreature != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                    border = BorderStroke(1.5.dp, EvolutionGoldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SQUAD FULL (4/4) — SWAP FOR TAMED ${pendingTamedCreature.speciesName.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = EvolutionGoldSecondary
                        )
                        Text(
                            text = "You can hold up to 4 monsters (1 starter + 3 tamed). Select which squad monster to replace with ${pendingTamedCreature.speciesName}:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        squadCreatures.forEach { member ->
                            OutlinedButton(
                                onClick = { onReplaceSquadMonster(member.id) },
                                border = BorderStroke(1.dp, HoloCyanPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = HoloCyanPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Replace ${member.nickname} (${member.speciesName} • LVL ${member.currentLevel()})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onDismissPendingTamed,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Release Wild ${pendingTamedCreature.speciesName} Instead")
                        }
                    }
                }
            }

            if (selectedSection == 0) {
                // SECTION 0: WILD MONSTER TAMING WITH TAMMING BERRIES (2 COINS / RANDOM SUCCESS RATE)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.dp, BioEmeraldHealth.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "WILD TAMING • 100 MONSTERS",
                                style = MaterialTheme.typography.headlineMedium,
                                color = BioEmeraldHealth
                            )
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "BERRIES IN BAG: $tammingBerryCount • SQUAD ${squadCreatures.size}/4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Buy Tamming Berries in the Store tab (2 DigiCoins each) and throw them here with randomized taming success rates!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val encounter = wildEncounter
                if (encounter != null) {
                    val spec = DigimonCatalog.getSpec(encounter.speciesId)
                    val skillBonus = activeCreature?.tammingRateBonusPercent() ?: 0
                    val effectiveRate = (encounter.baseSuccessRatePercent + skillBonus).coerceIn(15, 92)

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                        border = BorderStroke(1.5.dp, BioEmeraldHealth),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Animated Wild Taming Arena Stage (Berry Throw Trajectory + Capture Rings)
                            AnimatedWildTamingStage(
                                speciesId = spec.speciesId,
                                speciesName = spec.speciesName,
                                stage = spec.stage,
                                tameProgress = encounter.tameProgress,
                                isTamed = encounter.isTamed,
                                berryThrowTrigger = berryThrowTrigger + encounter.tameProgress,
                                lastRollResisted = encounter.lastRollOutcomeTitle.contains("RESISTED")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DigimonHoloCrestAvatar(
                                    speciesId = spec.speciesId,
                                    stage = spec.stage,
                                    isResting = false,
                                    evolutionReady = encounter.isTamed,
                                    sizeDp = 56.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "WILD ${spec.speciesName.uppercase()}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${spec.stage} • ${spec.attribute} • Move: ${spec.signatureMove}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = HoloCyanPrimary
                                    )
                                    Text(
                                        text = "Stats: ATK ${spec.baseAttack} • DEF ${spec.baseDefense} • SPD ${spec.baseSpeed} • WIS ${spec.baseWisdom}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EvolutionGoldSecondary
                                    )
                                }
                            }

                            // Random Success Rate & Tame Progress Bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "TAME METER: ${encounter.tameProgress}/100%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = BioEmeraldHealth
                                    )
                                    Text(
                                        text = "RANDOM SUCCESS RATE: $effectiveRate%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EvolutionGoldSecondary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (encounter.tameProgress / 100f).coerceIn(0f, 1f) },
                                    color = BioEmeraldHealth,
                                    trackColor = HoloSlateSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                )
                            }

                            Surface(
                                color = HoloCardElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = encounter.lastRollOutcomeTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (encounter.lastRollOutcomeTitle.contains("RESISTED")) SolarCoralHunger else BioEmeraldHealth
                                    )
                                    Text(
                                        text = encounter.lastRollOutcomeDetail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (encounter.isTamed || tammingBerryCount > 0) {
                                            berryThrowTrigger++
                                            onThrowTammingBerry()
                                        } else {
                                            onGoToStoreTab()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tammingBerryCount > 0 || encounter.isTamed) BioEmeraldHealth else EvolutionGoldSecondary,
                                        contentColor = CyberVoidBg
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .heightIn(min = 46.dp)
                                        .testTag("throw_tamming_berry_button")
                                ) {
                                    Text(
                                        text = when {
                                            encounter.isTamed -> "SCAN NEXT WILD"
                                            tammingBerryCount > 0 -> "THROW BERRY ($tammingBerryCount IN BAG)"
                                            else -> "BUY BERRIES IN STORE"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onScanWildMonster(null) },
                                    border = BorderStroke(1.dp, HoloCyanPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .testTag("scan_new_wild_button")
                                ) {
                                    Text(
                                        text = "SCAN WILD",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = HoloCyanPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 100-Monster Catalog Browser Toggle
                OutlinedButton(
                    onClick = { showDexBrowser = !showDexBrowser },
                    border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showDexBrowser) "HIDE 100-MONSTER WILD CATALOG" else "BROWSE ALL ${DigimonCatalog.allSpeciesSpecs.size} UNIQUE MONSTERS",
                        style = MaterialTheme.typography.labelLarge,
                        color = EvolutionGoldSecondary
                    )
                }

                if (showDexBrowser) {
                    DigimonCatalog.wildTamableSpecs.forEachIndexed { idx, spec ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${idx + 1} ${spec.speciesName} (${spec.stage} • ${spec.attribute})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "ATK ${spec.baseAttack} • DEF ${spec.baseDefense} • SPD ${spec.baseSpeed} • WIS ${spec.baseWisdom}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HoloCyanPrimary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        onScanWildMonster(spec.speciesId)
                                        showDexBrowser = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("TRACK", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            } else {
                // SECTION 1: 10 TIMED EXPEDITIONS (INCLUDING 24-HOUR EXPEDITION) WITH SLEEP & FOOD NEEDS
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "10 DIGITAL EXPEDITIONS (5M TO 24 HOURS)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = HoloCyanPrimary
                        )
                        Text(
                            text = "Send your active monster on 10 timed expeditions (including the 24-Hour Digital Abyss Odyssey). While on an expedition, your monster consumes Food (Hunger) and Sleep (Stamina) — feed it and use Cryo-Rest (≤50% STA) to keep the expedition moving!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "★ ULTRA RARE BONUS LOOT: Every completed expedition has an Ultra Rare chance to grant Tamming Berries or 1x Random Food Item into your Bag!",
                            style = MaterialTheme.typography.labelMedium,
                            color = EvolutionGoldSecondary
                        )
                    }
                }

                // Active Monster Expedition Status Card
                if (activeCreature != null && activeCreature.isOnExpedition()) {
                    val expSpec = DigimonCatalog.getExpedition(activeCreature.activeExpeditionId)
                    val nowMs = System.currentTimeMillis()
                    val remSec = ((activeCreature.expeditionEndMs - nowMs) / 1000L).coerceAtLeast(0L)
                    val totalSec = (expSpec?.durationSeconds ?: 300L).coerceAtLeast(1L)
                    val progress = (1f - (remSec.toFloat() / totalSec.toFloat())).coerceIn(0f, 1f)
                    val hours = remSec / 3600L
                    val mins = (remSec % 3600L) / 60L
                    val secs = remSec % 60L
                    val timeText = if (remSec <= 0L) "COMPLETED!" else if (hours > 0L) "${hours}h ${mins}m ${secs}s" else "${mins}m ${secs}s"

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                        border = BorderStroke(1.5.dp, if (activeCreature.needsExpeditionCare()) VirusCrimsonError else EvolutionGoldSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DEPLOYED: ${activeCreature.nickname.uppercase()} ➔ ${expSpec?.title ?: "EXPEDITION"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = EvolutionGoldSecondary
                            )
                            Text(
                                text = "Remaining Time: $timeText • Satiety: ${activeCreature.hunger}% • Stamina: ${activeCreature.stamina}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                color = EvolutionGoldSecondary,
                                trackColor = HoloSlateSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )

                            if (activeCreature.needsExpeditionCare()) {
                                Text(
                                    text = if (activeCreature.isResting) {
                                        "CRYO-RESTING ON EXPEDITION (${activeCreature.stamina}%/100% STA): Expedition resumes once Stamina hits 100%!"
                                    } else {
                                        "EXPEDITION PAUSED: ${activeCreature.nickname} needs Food (Hunger ≤25%) or Cryo-Rest Sleep (Stamina ≤20%)!"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SolarCoralHunger
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onOpenFeedSheet,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("FEED", style = MaterialTheme.typography.labelMedium)
                                }
                                FilledTonalButton(
                                    onClick = onTriggerCryoRest,
                                    enabled = activeCreature.canEnterCryoRest(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            activeCreature.isResting -> "RESTING (${activeCreature.stamina}%)"
                                            activeCreature.stamina <= 50 -> "CRYO-REST"
                                            else -> "REST ≤50%"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Button(
                                    onClick = { onCompleteOrRecallExpedition(activeCreature.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (remSec <= 0L) BioEmeraldHealth else SolarCoralHunger,
                                        contentColor = CyberVoidBg
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (remSec <= 0L) "CLAIM!" else "RECALL",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // List of all 10 Expeditions
                DigimonCatalog.allExpeditions.forEach { exp ->
                    val is24h = exp.durationMinutes == 1440
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                        border = BorderStroke(
                            width = if (is24h) 1.5.dp else 1.dp,
                            color = if (is24h) EvolutionGoldSecondary else HoloCyanPrimary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expedition_card_${exp.number}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${exp.number} ${exp.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (is24h) EvolutionGoldSecondary else Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = HoloCyanPrimary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = exp.durationLabel.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HoloCyanPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = exp.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Rewards: +${exp.rewardCoins} DigiCoins • +${exp.rewardAtk} ATK / +${exp.rewardDef} DEF / +${exp.rewardSpd} SPD / +${exp.rewardWis} WIS • +${exp.rewardEvoSync}% Sync • +1 Skill Pt",
                                style = MaterialTheme.typography.labelSmall,
                                color = BioEmeraldHealth
                            )
                            Text(
                                text = "★ Ultra Rare Drop Chance: Tamming Berries or 1x Random Food Item!",
                                style = MaterialTheme.typography.labelSmall,
                                color = EvolutionGoldSecondary
                            )
                            Button(
                                onClick = { onStartExpedition(exp) },
                                enabled = activeCreature != null && !activeCreature.isOnExpedition(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (is24h) EvolutionGoldSecondary else HoloCyanPrimary,
                                    contentColor = CyberVoidBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .testTag("start_expedition_${exp.number}")
                            ) {
                                Text(
                                    text = if (activeCreature?.isOnExpedition() == true) {
                                        "MONSTER ALREADY ON EXPEDITION"
                                    } else {
                                        "SEND ${activeCreature?.nickname?.uppercase() ?: "MONSTER"} (${exp.durationLabel.uppercase()})"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonsterSkillTreeBottomSheet(
    creature: DigimonCreature,
    onUnlockNode: (MonsterSkillNode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HoloSlateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MonsterSkillTreeTabContent(
                creature = creature,
                onUnlockNode = onUnlockNode
            )
        }
    }
}

@Composable
fun MonsterSkillTreeTabContent(
    creature: DigimonCreature,
    onUnlockNode: (MonsterSkillNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val skillTree = DigimonCatalog.getSkillTree(creature.speciesId)
    val unlockedIds = creature.allUnlockedSkillNodeIds()
    val unlockedCount = creature.unlockedSkillCount()
    val availableSp = creature.availableSkillPoints()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                border = BorderStroke(1.dp, PlasmaVioletStamina),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "UNIQUE SKILL TREE // ${creature.nickname.uppercase()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PlasmaVioletStamina
                    )
                    Text(
                        text = skillTree.treeArchetypeTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = HoloCyanPrimary
                    )
                    Text(
                        text = "Available Skill Points: $availableSp SP (Unlocked $unlockedCount/${skillTree.nodes.size}) • Earn +1 SP every 3 Training Levels and +1 SP per completed Expedition!",
                        style = MaterialTheme.typography.bodySmall,
                        color = EvolutionGoldSecondary
                    )
                }
            }

            skillTree.nodes.forEach { node ->
                val isUnlocked = node.nodeId in unlockedIds
                val prereqMet = node.prerequisiteNodeId == null || node.prerequisiteNodeId in unlockedIds
                val canUnlock = !isUnlocked && prereqMet && availableSp > 0

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(
                        width = if (isUnlocked) 1.5.dp else 1.dp,
                        color = when {
                            isUnlocked -> BioEmeraldHealth
                            canUnlock -> EvolutionGoldSecondary
                            else -> HoloCyanPrimary.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skill_node_${node.nodeId}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tier ${node.tier} • ${node.branchName}: ${node.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = node.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = when {
                                    isUnlocked -> Icons.Default.CheckCircle
                                    canUnlock -> Icons.Default.AutoAwesome
                                    else -> Icons.Default.Lock
                                },
                                contentDescription = null,
                                tint = when {
                                    isUnlocked -> BioEmeraldHealth
                                    canUnlock -> EvolutionGoldSecondary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        if (!isUnlocked) {
                            Button(
                                onClick = { onUnlockNode(node) },
                                enabled = canUnlock,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlasmaVioletStamina,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp)
                            ) {
                                Text(
                                    text = when {
                                        !prereqMet -> "LOCKED (REQUIRES TIER 1 NODE)"
                                        availableSp <= 0 -> "NEED 1 SKILL POINT (0 SP)"
                                        else -> "UNLOCK SKILL NODE (1 SP)"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        } else {
                            Text(
                                text = "✓ ACTIVE ON ${creature.nickname.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = BioEmeraldHealth
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
    }
}
