package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.example.data.model.AcornOrbType
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.EmeraldReserveEntry
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
    acornOrbInventory: Map<AcornOrbType, Int> = emptyMap(),
    selectedAcornOrbType: AcornOrbType = AcornOrbType.STANDARD_ACORN_ORB,
    emeraldReserveEntries: List<EmeraldReserveEntry> = emptyList(),
    standardMonsterPartsCount: Int = 0,
    ultraRareMonsterPartsCount: Int = 0,
    wildEncounter: WildEncounterState?,
    pendingTamedCreature: DigimonCreature?,
    initialTabIndex: Int = 0, // 0 = Wild Taming (100 Monsters), 1 = 10 Expeditions (Up to 24h)
    onScanWildMonster: (String?) -> Unit,
    onThrowTammingBerry: () -> Unit,
    onSelectAcornOrbType: (AcornOrbType) -> Unit = {},
    onBuyOrCraftAcornOrb: (AcornOrbType, Boolean) -> Unit = { _, _ -> },
    onDeployFromEmeraldReserve: (EmeraldReserveEntry) -> Unit = {},
    onBattleWildMonster: () -> Unit = {},
    onExecuteWildMove: (Int) -> Unit = {},
    onChallengeSanctumLeader: (com.example.data.model.SanctumLeaderSpec) -> Unit = {},
    onSellStandardPart: (Int) -> Unit = {},
    onSellUltraRarePart: (Int) -> Unit = {},
    onSellAllParts: () -> Unit = {},
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
            acornOrbInventory = acornOrbInventory,
            selectedAcornOrbType = selectedAcornOrbType,
            emeraldReserveEntries = emeraldReserveEntries,
            standardMonsterPartsCount = standardMonsterPartsCount,
            ultraRareMonsterPartsCount = ultraRareMonsterPartsCount,
            wildEncounter = wildEncounter,
            pendingTamedCreature = pendingTamedCreature,
            initialTabIndex = initialTabIndex,
            onScanWildMonster = onScanWildMonster,
            onThrowTammingBerry = onThrowTammingBerry,
            onSelectAcornOrbType = onSelectAcornOrbType,
            onBuyOrCraftAcornOrb = onBuyOrCraftAcornOrb,
            onDeployFromEmeraldReserve = onDeployFromEmeraldReserve,
            onBattleWildMonster = onBattleWildMonster,
            onExecuteWildMove = onExecuteWildMove,
            onChallengeSanctumLeader = onChallengeSanctumLeader,
            onSellStandardPart = onSellStandardPart,
            onSellUltraRarePart = onSellUltraRarePart,
            onSellAllParts = onSellAllParts,
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
    acornOrbInventory: Map<AcornOrbType, Int> = emptyMap(),
    selectedAcornOrbType: AcornOrbType = AcornOrbType.STANDARD_ACORN_ORB,
    emeraldReserveEntries: List<EmeraldReserveEntry> = emptyList(),
    standardMonsterPartsCount: Int = 0,
    ultraRareMonsterPartsCount: Int = 0,
    wildEncounter: WildEncounterState?,
    pendingTamedCreature: DigimonCreature?,
    initialTabIndex: Int = 0, // 0 = Safari & 4-Move Wild Combat, 1 = 16 Crest Routes (24h), 2 = 16 Sanctum Leader Battles
    onScanWildMonster: (String?) -> Unit,
    onThrowTammingBerry: () -> Unit,
    onSelectAcornOrbType: (AcornOrbType) -> Unit = {},
    onBuyOrCraftAcornOrb: (AcornOrbType, Boolean) -> Unit = { _, _ -> },
    onDeployFromEmeraldReserve: (EmeraldReserveEntry) -> Unit = {},
    onBattleWildMonster: () -> Unit = {},
    onExecuteWildMove: (Int) -> Unit = {},
    onChallengeSanctumLeader: (com.example.data.model.SanctumLeaderSpec) -> Unit = {},
    onSellStandardPart: (Int) -> Unit = {},
    onSellUltraRarePart: (Int) -> Unit = {},
    onSellAllParts: () -> Unit = {},
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
    val nowMs = System.currentTimeMillis()
    val isRecoveringFrom1Hp = activeCreature?.isRecoveringFrom1Hp(nowMs) == true
    val totalPartsCount = standardMonsterPartsCount + ultraRareMonsterPartsCount
    val totalPartsCoinValue =
        (standardMonsterPartsCount * DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS) +
            (ultraRareMonsterPartsCount * DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // Top Switcher: Emerald Safari (100), 16 Crest Routes (24h), & 16 Sanctum Leader Gym Battles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { selectedSection = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSection == 0) BioEmeraldHealth else HoloCardElevated,
                        contentColor = if (selectedSection == 0) CyberVoidBg else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("switch_wild_taming_section")
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAFARI (100)", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }

                Button(
                    onClick = { selectedSection = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSection == 1) HoloCyanPrimary else HoloCardElevated,
                        contentColor = if (selectedSection == 1) CyberVoidBg else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("switch_expeditions_section")
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ROUTES (24H)", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }

                Button(
                    onClick = { selectedSection = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSection == 2) EvolutionGoldSecondary else HoloCardElevated,
                        contentColor = if (selectedSection == 2) CyberVoidBg else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("switch_sanctum_leaders_section")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "16 SANCTUMS (${activeCreature?.earnedSanctumBadgesCount() ?: 0}/16)",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            // 1 HP 5-Minute Mandatory Recovery Lockout Warning Banner
            if (activeCreature != null && isRecoveringFrom1Hp) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                    border = BorderStroke(1.5.dp, VirusCrimsonError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wild_1hp_recovery_lockout_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚠️ 1 HP RECOVERY LOCKOUT — ${activeCreature.nickname.uppercase()} (${activeCreature.formattedRecoveryRemaining(nowMs)} LEFT)",
                            style = MaterialTheme.typography.titleMedium,
                            color = VirusCrimsonError
                        )
                        Text(
                            text = "${activeCreature.nickname} dropped to 1 HP battling a wild monster! Mandatory recovery lasts 5 minutes (${activeCreature.formattedRecoveryRemaining(nowMs)} remaining) before you can battle, train, tame, or do anything with ${activeCreature.nickname} again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
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
                // SECTION 0: WILD MONSTER TAMING OR BATTLING FOR MONSTER PARTS
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
                                text = "EMERALD RESERVE SAFARI & ACORN BATTLES • 100 COMPANIONS",
                                style = MaterialTheme.typography.headlineMedium,
                                color = BioEmeraldHealth
                            )
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "ACORN ORBS: $tammingBerryCount • RELICS: $totalPartsCount • SQUAD ${squadCreatures.size}/4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Throw Master Orin's Acorn Orbs (2 Coins) to capture wild companions, OR Battle wild companions you don't want to tame for Gilded Acorns & Relic Shards! Rarer companions have much lower wild spawn odds and are harder to capture:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Wild Spawn Rarity & Capture Difficulty Breakdown
                        Surface(
                            color = CyberVoidBg.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, HoloSlateSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "• COMMON (Rookie): 60% Spawn • 56–70% Capture Rate • +22–34% Orb Hit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BioEmeraldHealth
                                )
                                Text(
                                    text = "• UNCOMMON (Champion): 24% Spawn • 34–45% Capture Rate • +15–23% Orb Hit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HoloCyanPrimary
                                )
                                Text(
                                    text = "• RARE (Ultimate): 11% Spawn • 18–26% Capture Rate • +10–16% Orb Hit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PlasmaVioletStamina
                                )
                                Text(
                                    text = "• ULTRA RARE (Mega): 4% Spawn • 8–14% Capture Rate • +6–11% Hit (Can Shake Off -3%!)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary
                                )
                                Text(
                                    text = "• LEGENDARY SOVEREIGN: 1% Spawn • 4–7% Capture Rate • +4–8% Hit (0% Instant Crit, -5% Resist!)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VirusCrimsonError
                                )
                            }
                        }
                    }
                }

                val encounter = wildEncounter
                if (encounter != null) {
                    val spec = DigimonCatalog.getSpec(encounter.speciesId)
                    val rarity = encounter.rarityTier
                    val rarityColor = Color(rarity.colorHex)
                    val rawSkillBonus = activeCreature?.tammingRateBonusPercent() ?: 0
                    val scaledSkillBonus = when (rarity) {
                        com.example.data.model.WildRarityTier.COMMON -> rawSkillBonus
                        com.example.data.model.WildRarityTier.UNCOMMON -> (rawSkillBonus * 3) / 4
                        com.example.data.model.WildRarityTier.RARE -> rawSkillBonus / 2
                        com.example.data.model.WildRarityTier.ULTRA_RARE -> rawSkillBonus / 3
                        com.example.data.model.WildRarityTier.LEGENDARY_APEX -> rawSkillBonus / 4
                    }
                    val tacticalBonus = encounter.totalTacticalCaptureBonusPercent()
                    val heldCharmBonus = activeCreature?.heldRelic()?.captureBonusPercent ?: 0
                    val orbSpecialtyBonus = selectedAcornOrbType.effectiveCaptureBonusFor(encounter)
                    val effectiveRate = (
                        encounter.baseSuccessRatePercent +
                            scaledSkillBonus +
                            tacticalBonus +
                            heldCharmBonus +
                            orbSpecialtyBonus
                        ).coerceIn(3, 95)
                    val wildHpColor = when {
                        encounter.wildHpPercent <= 20 -> VirusCrimsonError
                        encounter.wildHpPercent <= 50 -> EvolutionGoldSecondary
                        else -> BioEmeraldHealth
                    }
                    val elemMult = if (activeCreature != null) {
                        com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
                            attackerAttr = activeCreature.attribute,
                            defenderAttr = spec.attribute
                        )
                    } else {
                        1.0f
                    }
                    val elemMatchupText = com.example.data.model.ElementalMatchupEngine.effectivenessBadge(elemMult)

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
                        border = BorderStroke(
                            width = if (encounter.isChromaticShiny) 2.dp else 1.5.dp,
                            color = if (encounter.isChromaticShiny) EvolutionGoldSecondary else rarityColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Rarity Tier, Chromatic Shiny & Tactical Capture Bonus Badges
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = rarityColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, rarityColor)
                                ) {
                                    Text(
                                        text = "★ ${rarity.badgeLabel} • BASE ${encounter.baseSuccessRatePercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = rarityColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                if (encounter.isChromaticShiny) {
                                    Surface(
                                        color = EvolutionGoldSecondary.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.5.dp, EvolutionGoldSecondary)
                                    ) {
                                        Text(
                                            text = "✨ STARLIGHT CHROMATIC (+15% STATS)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EvolutionGoldSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Surface(
                                    color = HoloCyanPrimary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, HoloCyanPrimary)
                                ) {
                                    Text(
                                        text = "⚔️ $elemMatchupText • NATURE: ${encounter.wildNatureName.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HoloCyanPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Animated Wild Taming Arena Stage (Berry Throw Trajectory + Capture Rings)
                            AnimatedWildTamingStage(
                                speciesId = spec.speciesId,
                                speciesName = spec.speciesName,
                                stage = spec.stage,
                                tameProgress = encounter.tameProgress,
                                isTamed = encounter.isTamed,
                                berryThrowTrigger = berryThrowTrigger + encounter.tameProgress,
                                lastRollResisted = encounter.lastRollOutcomeTitle.contains("RESISTED") || encounter.lastRollOutcomeTitle.contains("BROKE FREE")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DigimonHoloCrestAvatar(
                                    speciesId = spec.speciesId,
                                    stage = spec.stage,
                                    isResting = false,
                                    evolutionReady = encounter.isTamed || encounter.isUltraRareWild || encounter.isChromaticShiny,
                                    sizeDp = 56.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${if (encounter.isChromaticShiny) "✨ CHROMATIC " else ""}${rarity.displayName.uppercase()} ${spec.speciesName.uppercase()} (LVL ${encounter.wildLevel})",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = if (encounter.isChromaticShiny) EvolutionGoldSecondary else rarityColor
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
                                    if (activeCreature != null) {
                                        Text(
                                            text = "Partner: ${activeCreature.nickname} (${activeCreature.attribute}) • HP ${activeCreature.health}/100${if (isRecoveringFrom1Hp) " (RECOVERING ${activeCreature.formattedRecoveryRemaining(nowMs)})" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (activeCreature.health <= 25 || isRecoveringFrom1Hp) VirusCrimsonError else BioEmeraldHealth
                                        )
                                    }
                                }
                            }

                            // Wild Companion HP Bar & Status Condition (Weaken to Red HP + Inflict Sleep/Paralyze to Boost Capture Rate!)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wild_hp_status_panel")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "WILD HP: ${encounter.wildHpPercent}% (+${encounter.weakenedHpCaptureBonusPercent()}% WEAKENED BONUS)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = wildHpColor
                                    )
                                    Text(
                                        text = encounter.statusEffect.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (encounter.statusEffect == com.example.data.model.WildStatusEffect.NONE) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            EvolutionGoldSecondary
                                        }
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (encounter.wildHpPercent / 100f).coerceIn(0f, 1f) },
                                    color = wildHpColor,
                                    trackColor = HoloSlateSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                )
                            }

                            // Random Success Rate & Capture Progress Bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "CAPTURE GAUGE: ${encounter.tameProgress}/100%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = BioEmeraldHealth
                                    )
                                    Text(
                                        text = "EFFECTIVE CAPTURE: $effectiveRate% (+${tacticalBonus + heldCharmBonus}% BOOST)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (tacticalBonus > 0) EvolutionGoldSecondary else rarityColor
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

                            // 2x2 Tactical 4-Move Combat Pad (Strike / Mercy Slash 1% HP / Status Wave / Crest Heal)
                            if (activeCreature != null && !encounter.isTamed) {
                                val battleMoves = remember(activeCreature.speciesId, activeCreature.nickname) {
                                    DigimonCatalog.getFourBattleMoves(activeCreature)
                                }
                                Surface(
                                    color = HoloSlateSurface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("wild_tactical_4move_pad")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "4-MOVE TACTICAL COMBAT // WEAKEN TO 1% HP OR DEFEAT FOR RELICS",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = HoloCyanPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            battleMoves.take(2).forEach { move ->
                                                val btnColor = if (move.moveIndex == 1) BioEmeraldHealth else SolarCoralHunger
                                                FilledTonalButton(
                                                    onClick = { onExecuteWildMove(move.moveIndex) },
                                                    enabled = !isRecoveringFrom1Hp,
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = btnColor.copy(alpha = 0.22f),
                                                        contentColor = btnColor
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("wild_tactical_move_${move.moveIndex}")
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = if (move.moveIndex == 1) "🗡️ MERCY SLASH (1% HP)" else "⚔️ ${move.moveName.uppercase()}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = if (move.moveIndex == 1) "Never KOs (+35% Cap)" else "${move.elementAttribute} ($elemMatchupText)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            battleMoves.drop(2).take(2).forEach { move ->
                                                val btnColor = if (move.moveIndex == 2) EvolutionGoldSecondary else HoloCyanPrimary
                                                FilledTonalButton(
                                                    onClick = { onExecuteWildMove(move.moveIndex) },
                                                    enabled = !isRecoveringFrom1Hp,
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = btnColor.copy(alpha = 0.22f),
                                                        contentColor = btnColor
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("wild_tactical_move_${move.moveIndex}")
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = if (move.moveIndex == 2) "✨ ${move.moveName.substringBefore(" (").uppercase()}" else "🛡️ CREST RESONANCE",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = if (move.moveIndex == 2) "Status (+12–25% Cap)" else "+18 HP & Bond Surge",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
                                        color = if (encounter.lastRollOutcomeTitle.contains("RESISTED") || encounter.lastRollOutcomeTitle.contains("BROKE FREE")) SolarCoralHunger else BioEmeraldHealth
                                    )
                                    Text(
                                        text = encounter.lastRollOutcomeDetail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }

                            val selectedOrbCount = if (selectedAcornOrbType == AcornOrbType.STANDARD_ACORN_ORB) {
                                maxOf(tammingBerryCount, acornOrbInventory[AcornOrbType.STANDARD_ACORN_ORB] ?: tammingBerryCount)
                            } else {
                                acornOrbInventory[selectedAcornOrbType] ?: 0
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (encounter.isTamed || selectedOrbCount > 0) {
                                            berryThrowTrigger++
                                            onThrowTammingBerry()
                                        } else {
                                            onGoToStoreTab()
                                        }
                                    },
                                    enabled = !isRecoveringFrom1Hp,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedOrbCount > 0 || encounter.isTamed) Color(selectedAcornOrbType.colorHex) else EvolutionGoldSecondary,
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
                                            isRecoveringFrom1Hp && activeCreature != null ->
                                                "RECOVERING (${activeCreature.formattedRecoveryRemaining(nowMs)})"
                                            encounter.isTamed -> "SCAN NEXT WILD"
                                            selectedOrbCount > 0 -> "THROW ${selectedAcornOrbType.displayName.uppercase()} ($selectedOrbCount)"
                                            else -> "FORGE / BUY ORBS"
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

                            // Instant All-Out Battle Wild Companion for Gilded Acorns & Relic Shards Button
                            Button(
                                onClick = onBattleWildMonster,
                                enabled = activeCreature != null && !isRecoveringFrom1Hp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (encounter.isUltraRareWild) EvolutionGoldSecondary else SolarCoralHunger,
                                    contentColor = CyberVoidBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("battle_wild_monster_for_parts_button")
                            ) {
                                Text(
                                    text = when {
                                        isRecoveringFrom1Hp && activeCreature != null ->
                                            "1 HP RECOVERY LOCKOUT — WAIT ${activeCreature.formattedRecoveryRemaining(nowMs)} (5M RECOVERY)"
                                        encounter.isUltraRareWild ->
                                            "⚔️ ALL-OUT KO FOR RELIC SHARDS (5 COINS/SHARD)"
                                        else ->
                                            "⚔️ ALL-OUT KO FOR GILDED ACORNS (2 COINS/ACORN)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // MASTER ORIN'S ACORN ORB POUCH & FORGE (Firestore + Offline Cached)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.2.dp, BioEmeraldHealth.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("acorn_orb_pouch_and_forge_card")
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
                                text = "ACORN ORB POUCH & FORGE (OFFLINE-CACHED)",
                                style = MaterialTheme.typography.titleMedium,
                                color = BioEmeraldHealth
                            )
                            Surface(
                                color = BioEmeraldHealth.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "EQUIPPED: ${selectedAcornOrbType.displayName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BioEmeraldHealth,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Select which handcrafted Acorn Orb to throw, or craft specialty orbs using Gilded Acorns ($standardMonsterPartsCount) & Relic Shards ($ultraRareMonsterPartsCount). Synced to Firestore with local offline caching:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AcornOrbType.entries.chunked(2).forEach { rowOrbs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOrbs.forEach { orb ->
                                    val ownedCount = acornOrbInventory[orb]
                                        ?: if (orb == AcornOrbType.STANDARD_ACORN_ORB) tammingBerryCount else 0
                                    val isSelected = selectedAcornOrbType == orb
                                    val orbColor = Color(orb.colorHex)
                                    val bonusVsCurrent = wildEncounter?.let { orb.effectiveCaptureBonusFor(it) } ?: orb.specialtyBonusPercent
                                    val canCraftWithParts = standardMonsterPartsCount >= orb.craftGildedAcornsCost &&
                                        ultraRareMonsterPartsCount >= orb.craftRelicShardsCost
                                    Surface(
                                        color = if (isSelected) orbColor.copy(alpha = 0.18f) else CyberVoidBg.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) orbColor else orbColor.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectAcornOrbType(orb) }
                                            .testTag("select_acorn_orb_${orb.orbTypeId}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = orb.displayName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = orbColor,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "x$ownedCount",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color.White
                                                )
                                            }
                                            Text(
                                                text = if (bonusVsCurrent > 0) "+$bonusVsCurrent% Capture vs Target" else "Standard Base Rate",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (bonusVsCurrent >= 18) BioEmeraldHealth else Color.White.copy(alpha = 0.8f)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { onBuyOrCraftAcornOrb(orb, false) },
                                                    enabled = digiCoins >= orb.costCoins,
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 32.dp)
                                                        .testTag("buy_orb_${orb.orbTypeId}")
                                                ) {
                                                    Text("${orb.costCoins}c", style = MaterialTheme.typography.labelSmall)
                                                }
                                                FilledTonalButton(
                                                    onClick = { onBuyOrCraftAcornOrb(orb, true) },
                                                    enabled = canCraftWithParts,
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 32.dp)
                                                        .testTag("craft_orb_${orb.orbTypeId}")
                                                ) {
                                                    Text(
                                                        text = if (orb.craftRelicShardsCost > 0) "${orb.craftGildedAcornsCost}A+${orb.craftRelicShardsCost}S" else "${orb.craftGildedAcornsCost} Acorn",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // EMERALD RESERVE COLLECTION VAULT (Firestore + Offline Cached)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.2.dp, HoloCyanPrimary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emerald_reserve_collection_vault_card")
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
                                text = "EMERALD RESERVE COLLECTION VAULT (${emeraldReserveEntries.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = HoloCyanPrimary
                            )
                            Surface(
                                color = BioEmeraldHealth.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "OFFLINE CACHE READY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BioEmeraldHealth,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (emeraldReserveEntries.isEmpty()) {
                            Text(
                                text = "Your Emerald Reserve Sanctuary Vault is ready! Every wild companion you capture with Acorn Orbs is automatically archived here with offline caching.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            emeraldReserveEntries.take(8).forEach { entry ->
                                val tier = entry.resolvedRarityTier()
                                val tierColor = Color(tier.colorHex)
                                val orbUsed = entry.resolvedOrbType()
                                Surface(
                                    color = CyberVoidBg.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (entry.chromaticShiny) EvolutionGoldSecondary else tierColor.copy(alpha = 0.5f)),
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
                                                text = "${if (entry.chromaticShiny) "✨ CHROMATIC " else ""}${entry.nickname} (${entry.speciesName} • LVL ${entry.capturedLevel})",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (entry.chromaticShiny) EvolutionGoldSecondary else Color.White
                                            )
                                            Text(
                                                text = "${entry.stage} • ${entry.elementAttribute} • Nature: ${entry.resolvedNature().displayName} • Caught in ${orbUsed.displayName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tierColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (entry.inActiveParty || squadCreatures.any { it.id == entry.id || it.speciesId == entry.speciesId }) {
                                            Surface(
                                                color = BioEmeraldHealth.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "IN PARTY",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BioEmeraldHealth,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            FilledTonalButton(
                                                onClick = { onDeployFromEmeraldReserve(entry) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("deploy_reserve_${entry.id}")
                                            ) {
                                                Text("DEPLOY", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Gilded Acorns & Relic Shards Salvage Card (Sell Standard for 2 Coins, Ultra Rare for 5 Coins)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.2.dp, EvolutionGoldSecondary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wild_monster_parts_salvage_card")
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
                                text = "GILDED ACORNS & RELIC SHARDS POUCH",
                                style = MaterialTheme.typography.titleMedium,
                                color = EvolutionGoldSecondary
                            )
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "VALUE: $totalPartsCoinValue CREST COINS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Gilded Acorns: $standardMonsterPartsCount (${DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS} Crest Coins each) • Relic Shards: $ultraRareMonsterPartsCount (${DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS} Crest Coins each)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onSellStandardPart(1) },
                                enabled = standardMonsterPartsCount > 0,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = HoloCyanPrimary.copy(alpha = 0.2f),
                                    contentColor = HoloCyanPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("wild_sell_standard_part_button")
                            ) {
                                Text(
                                    text = "SELL STD (+2 COINS)",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            FilledTonalButton(
                                onClick = { onSellUltraRarePart(1) },
                                enabled = ultraRareMonsterPartsCount > 0,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EvolutionGoldSecondary.copy(alpha = 0.22f),
                                    contentColor = EvolutionGoldSecondary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("wild_sell_ultra_rare_part_button")
                            ) {
                                Text(
                                    text = "SELL ULTRA (+5 COINS)",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (totalPartsCount > 0) {
                            Button(
                                onClick = onSellAllParts,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BioEmeraldHealth,
                                    contentColor = CyberVoidBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("wild_sell_all_parts_button")
                            ) {
                                Text(
                                    text = "SELL ALL $totalPartsCount MONSTER PARTS (+$totalPartsCoinValue DIGICOINS)",
                                    style = MaterialTheme.typography.labelLarge
                                )
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
                        val specRarity = spec.wildRarityTier()
                        val specRarityColor = Color(specRarity.colorHex)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                            border = BorderStroke(1.dp, specRarityColor.copy(alpha = 0.45f)),
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
                                        text = "${specRarity.displayName.uppercase()} (${specRarity.spawnChancePercent}% Wild Spawn) • Base Tame ${specRarity.minBaseTameRate}–${specRarity.maxBaseTameRate}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = specRarityColor
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
                                    Text("RADAR (${specRarity.radarLockChancePercent}%)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            } else if (selectedSection == 1) {
                // SECTION 1: 16 DUAL-REALM SANCTUM CRESTS & 24-HOUR SILVER PEAK SUMMIT EXPEDITION
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
                            text = "16 DUAL-REALM SANCTUM CRESTS (5M TO 24 HOURS)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = HoloCyanPrimary
                        )
                        Text(
                            text = "Send your active companion across Aurelia & Argentia to earn all 16 Sanctum Crests and challenge the 24-Hour Silver Peak Summit: Grand Champion Odyssey. While on an expedition, your companion consumes Nectar (Satiety) and Stamina — feed it and use Sanctuary Rest (≤50% STA) to keep the expedition moving!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "★ ULTRA RARE BONUS LOOT: Every completed expedition has an Ultra Rare chance to grant Master Orin's Acorn Orbs or 1x Random Delicacy into your Pouch!",
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
                                text = "Rewards: +${exp.rewardCoins} Crest Coins • +${exp.rewardAtk} ATK / +${exp.rewardDef} DEF / +${exp.rewardSpd} SPD / +${exp.rewardWis} WIS • +${exp.rewardEvoSync}% Sync • +1 Skill Pt",
                                style = MaterialTheme.typography.labelSmall,
                                color = BioEmeraldHealth
                            )
                            Text(
                                text = "★ Ultra Rare Drop Chance: Master Orin's Acorn Orbs or 1x Random Delicacy!",
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
                                        "COMPANION ALREADY ON EXPEDITION"
                                    } else {
                                        "SEND ${activeCreature?.nickname?.uppercase() ?: "COMPANION"} (${exp.durationLabel.uppercase()})"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            } else if (selectedSection == 2) {
                // SECTION 2: 16 DUAL-REALM SANCTUM LEADER BATTLES (8 AURELIA + 8 ARGENTIA BADGES)
                val earnedCount = activeCreature?.earnedSanctumBadgesCount() ?: 0
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.5.dp, EvolutionGoldSecondary.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sanctum_leaders_header_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "16 DUAL-REALM SANCTUM LEADERS",
                                style = MaterialTheme.typography.headlineMedium,
                                color = EvolutionGoldSecondary
                            )
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, EvolutionGoldSecondary)
                            ) {
                                Text(
                                    text = "🏅 $earnedCount/16 BADGES (+${earnedCount * 25} CP)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Challenge the 8 Aurelia Sanctum Leaders and 8 Argentia Sanctum Leaders (culminating in Sovereign Red at Obsidian Keep)! Use Elemental Type Advantages (2.0x Super Effective), Natures, and Held Relics to win Sanctum Badges and Crest Coins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activeCreature != null) {
                            Text(
                                text = "Active Challenger: ${activeCreature.nickname} (LVL ${activeCreature.currentLevel()} • ${activeCreature.attribute} • CP ${activeCreature.combatPower()} • HP ${activeCreature.health}/100)",
                                style = MaterialTheme.typography.labelMedium,
                                color = BioEmeraldHealth
                            )
                        }
                    }
                }

                com.example.data.model.DigiWorldExpansionCatalog.allSanctumLeaders.forEach { leader ->
                    val hasBadge = activeCreature?.hasSanctumBadge(leader.badgeNumber) == true
                    val elemMult = if (activeCreature != null) {
                        com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
                            attackerAttr = activeCreature.attribute,
                            defenderAttr = leader.elementAttribute
                        )
                    } else {
                        1.0f
                    }
                    val elemLabel = com.example.data.model.ElementalMatchupEngine.effectivenessBadge(elemMult)
                    val borderColor = when {
                        hasBadge -> BioEmeraldHealth
                        leader.badgeNumber == 16 -> EvolutionGoldSecondary
                        else -> HoloCyanPrimary.copy(alpha = 0.45f)
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                        border = BorderStroke(if (hasBadge || leader.badgeNumber == 16) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sanctum_leader_card_${leader.badgeNumber}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DigimonHoloCrestAvatar(
                                    speciesId = leader.aceSpeciesId,
                                    stage = if (leader.recommendedLevel >= 65) "Mega" else if (leader.recommendedLevel >= 40) "Ultimate" else "Champion",
                                    isResting = false,
                                    evolutionReady = hasBadge,
                                    sizeDp = 50.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${if (hasBadge) "🏅 " else ""}#${leader.badgeNumber} ${leader.realmName.uppercase()} • ${leader.cityTitle.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (leader.realmName == "Aurelia") EvolutionGoldSecondary else HoloCyanPrimary
                                    )
                                    Text(
                                        text = "Leader ${leader.leaderName} — Ace: ${leader.aceSpeciesName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${leader.badgeName} • Element: ${leader.elementAttribute} ($elemLabel)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (elemMult >= 1.8f) BioEmeraldHealth else if (elemMult <= 0.6f) SolarCoralHunger else HoloCyanPrimary
                                    )
                                    Text(
                                        text = "Rec. LVL ${leader.recommendedLevel} • First Win: +${leader.rewardCoins} Crest Coins & +25 CP Badge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EvolutionGoldSecondary
                                    )
                                }
                            }
                            Button(
                                onClick = { onChallengeSanctumLeader(leader) },
                                enabled = activeCreature != null && !isRecoveringFrom1Hp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasBadge) BioEmeraldHealth else EvolutionGoldSecondary,
                                    contentColor = CyberVoidBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .testTag("challenge_sanctum_leader_${leader.badgeNumber}")
                            ) {
                                Text(
                                    text = when {
                                        isRecoveringFrom1Hp && activeCreature != null ->
                                            "RECOVERING (${activeCreature.formattedRecoveryRemaining(nowMs)})"
                                        hasBadge ->
                                            "✓ BADGE EARNED — REMATCH LEADER ${leader.leaderName.uppercase()}"
                                        else ->
                                            "⚔️ CHALLENGE LEADER ${leader.leaderName.uppercase()} (REC. LVL ${leader.recommendedLevel})"
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
