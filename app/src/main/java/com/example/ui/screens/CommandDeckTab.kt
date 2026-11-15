package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CreatureRaisingProgress
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.EvolutionBranchRequirement
import com.example.data.model.TrainingLogEntry
import com.example.ui.components.DigimonHoloCrestAvatar
import com.example.ui.components.HoloArcGauge
import com.example.ui.components.SegmentedPlasmaBar
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
import com.example.viewmodel.DashboardUiState

@Composable
fun CommandDeckTab(
    uiState: DashboardUiState,
    onSelectCreature: (String) -> Unit,
    onOpenWildTamingModal: () -> Unit = {},
    onOpenExpeditionsModal: () -> Unit = {},
    onOpenSkillTreeModal: () -> Unit = {},
    onOpenStarterDraftModal: () -> Unit = {},
    onRequestFullReset: () -> Unit = {},
    onHealClicked: () -> Unit,
    onToggleRest: () -> Unit,
    onOpenFeedSheet: () -> Unit,
    onOpenEvolutionMatrix: () -> Unit,
    onTriggerEvolution: (EvolutionBranchRequirement) -> Unit = {},
    onUnlockSkillNode: (com.example.data.model.MonsterSkillNode) -> Unit = {},
    onOpenDrillsModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = uiState.activeCreature
    var selectedDeckSubTab by remember { mutableStateOf(0) } // 0 = Care & Vitals, 1 = Evo Tree, 2 = Skill Tree, 3 = Widgets & Reset

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 4-Monster Squad Switcher Strip (Quick Squad Selection & Shortcuts)
        SquadSwitcherRow(
            creatures = uiState.creatures,
            activeCreatureId = active?.id.orEmpty(),
            onSelectCreature = onSelectCreature,
            onOpenWildTamingModal = onOpenWildTamingModal,
            onOpenExpeditionsModal = onOpenExpeditionsModal,
            onOpenSkillTreeModal = { selectedDeckSubTab = 2 },
            onRequestFullReset = onRequestFullReset
        )

        if (active != null) {
            val raisingProgress = remember(active, uiState.trainingLogs) {
                CreatureRaisingProgress.compute(active, uiState.trainingLogs)
            }

            // Streamlined 4-Tab Bar inside Command Deck to eliminate excessive vertical scrolling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deck_subtab_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val deckTabs = listOf(
                    Triple(0, "CARE & VITALS", "deck_subtab_care"),
                    Triple(1, "EVO TREE", "deck_subtab_evo"),
                    Triple(2, "SKILLS (${active.availableSkillPoints()} SP)", "deck_subtab_skills"),
                    Triple(3, "WIDGETS", "deck_subtab_system")
                )
                deckTabs.forEach { (idx, label, tag) ->
                    val isSelected = selectedDeckSubTab == idx
                    val accent = when (idx) {
                        0 -> HoloCyanPrimary
                        1 -> EvolutionGoldSecondary
                        2 -> PlasmaVioletStamina
                        else -> BioEmeraldHealth
                    }
                    Surface(
                        color = if (isSelected) accent else HoloSlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = if (isSelected) 1f else 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedDeckSubTab = idx }
                            .testTag(tag)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) CyberVoidBg else accent,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            when (selectedDeckSubTab) {
                0 -> {
                    // Sub-Tab 0: Active Partner Stage + 2x2 Care & Vitals Gauges
                    ActivePartnerHoloStageCard(
                        creature = active,
                        raisingProgress = raisingProgress,
                        liveBpm = uiState.liveHeartbeatBpm,
                        digiCoins = uiState.digiCoins,
                        nextDigiCoinTickSec = uiState.nextDigiCoinTickSec,
                        onOpenDrillsModal = onOpenDrillsModal,
                        onOpenEvolutionMatrix = onOpenEvolutionMatrix,
                        onOpenWildTamingModal = onOpenWildTamingModal,
                        onOpenExpeditionsModal = onOpenExpeditionsModal,
                        onOpenSkillTreeModal = { selectedDeckSubTab = 2 }
                    )

                    Text(
                        text = "REAL-TIME BIOMETRIC & EVOLUTION TELEMETRY",
                        style = MaterialTheme.typography.labelLarge,
                        color = HoloCyanPrimary
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HealthTelemetryWidget(
                                    creature = active,
                                    onHealClicked = onHealClicked,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("widget_health")
                                )
                                StaminaTelemetryWidget(
                                    creature = active,
                                    nextTickSec = uiState.nextStaminaTickSec,
                                    onToggleRest = onToggleRest,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("widget_stamina")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HungerTelemetryWidget(
                                    creature = active,
                                    nextTickSec = uiState.nextHungerTickSec,
                                    onOpenFeedSheet = onOpenFeedSheet,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("widget_hunger")
                                )
                                EvolutionTelemetryWidget(
                                    creature = active,
                                    raisingProgress = raisingProgress,
                                    onOpenEvolutionMatrix = onOpenEvolutionMatrix,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("widget_evolution")
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Sub-Tab 1: Dedicated Interactive Visual Branching Evolution Tree
                    VisualEvolutionBranchingTreeCard(
                        creature = active,
                        raisingProgress = raisingProgress,
                        onSelectBranchToEvolve = onTriggerEvolution
                    )
                }
                2 -> {
                    // Sub-Tab 2: Dedicated Inline Monster Skill Tree (6 Unique Nodes)
                    MonsterSkillTreeTabContent(
                        creature = active,
                        onUnlockNode = onUnlockSkillNode
                    )
                }
                3 -> {
                    // Sub-Tab 3: Phone Home Screen AppWidgets (2x1) & Full Reset Controls
                    PhoneHomeScreenWidgetsCard(creature = active)
                    SystemFullResetCard(onRequestFullReset = onRequestFullReset)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SquadSwitcherRow(
    creatures: List<DigimonCreature>,
    activeCreatureId: String,
    onSelectCreature: (String) -> Unit,
    onOpenWildTamingModal: () -> Unit,
    onOpenExpeditionsModal: () -> Unit,
    onOpenSkillTreeModal: () -> Unit,
    onRequestFullReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = EvolutionGoldSecondary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.65f))
        ) {
            Text(
                text = "SQUAD ${creatures.size}/${DigimonCatalog.MAX_SQUAD_SIZE}",
                style = MaterialTheme.typography.labelLarge,
                color = EvolutionGoldSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)
            )
        }

        creatures.forEach { creature ->
            val isSelected = creature.id == activeCreatureId
            val spec = DigimonCatalog.getSpec(creature.speciesId)
            val accent = Color(spec.primaryColorHex)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) accent.copy(alpha = 0.22f) else HoloSlateSurface,
                border = BorderStroke(
                    width = if (isSelected) 1.8.dp else 1.dp,
                    color = if (isSelected) accent else HoloCyanPrimary.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectCreature(creature.id) }
                    .testTag("squad_chip_${creature.speciesId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = DigimonCatalog.getMonsterImageRes(creature.speciesId)),
                        contentDescription = "${creature.nickname} portrait",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberVoidBg)
                    )
                    Column {
                        Text(
                            text = creature.nickname,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusTag = when {
                            creature.isOnExpedition() -> "EXPEDITION"
                            creature.isResting -> "RESTING"
                            else -> creature.stage
                        }
                        Text(
                            text = "LVL ${creature.currentLevel()}/100 • $statusTag • CP ${creature.combatPower()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (creature.isMaxLevel100()) EvolutionGoldSecondary else if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenWildTamingModal,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BioEmeraldHealth.copy(alpha = 0.75f)),
            modifier = Modifier
                .height(52.dp)
                .testTag("open_wild_taming_button")
        ) {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = "Tame Wild Monsters",
                tint = BioEmeraldHealth,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TAME WILD (2c)",
                style = MaterialTheme.typography.labelLarge,
                color = BioEmeraldHealth
            )
        }

        OutlinedButton(
            onClick = onOpenExpeditionsModal,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.7f)),
            modifier = Modifier
                .height(52.dp)
                .testTag("open_expeditions_button")
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "10 Expeditions",
                tint = HoloCyanPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "10 EXPEDITIONS",
                style = MaterialTheme.typography.labelLarge,
                color = HoloCyanPrimary
            )
        }

        OutlinedButton(
            onClick = onOpenSkillTreeModal,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PlasmaVioletStamina.copy(alpha = 0.7f)),
            modifier = Modifier
                .height(52.dp)
                .testTag("open_skill_tree_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Skill Tree",
                tint = PlasmaVioletStamina,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SKILL TREE",
                style = MaterialTheme.typography.labelLarge,
                color = PlasmaVioletStamina
            )
        }

        OutlinedButton(
            onClick = onRequestFullReset,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, VirusCrimsonError.copy(alpha = 0.75f)),
            modifier = Modifier
                .height(52.dp)
                .testTag("squad_strip_full_reset_button")
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Full Reset",
                tint = VirusCrimsonError,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FULL RESET",
                style = MaterialTheme.typography.labelLarge,
                color = VirusCrimsonError
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivePartnerHoloStageCard(
    creature: DigimonCreature,
    raisingProgress: CreatureRaisingProgress,
    liveBpm: Int,
    digiCoins: Int,
    nextDigiCoinTickSec: Int,
    onOpenDrillsModal: () -> Unit,
    onOpenEvolutionMatrix: () -> Unit,
    onOpenWildTamingModal: () -> Unit = {},
    onOpenExpeditionsModal: () -> Unit = {},
    onOpenSkillTreeModal: () -> Unit = {}
) {
    val spec = DigimonCatalog.getSpec(creature.speciesId)
    val monsterImageRes = DigimonCatalog.getMonsterImageRes(creature.speciesId)
    val accent = Color(spec.primaryColorHex)
    val canDigivolve = creature.canEvolveAnyBranch()
    val gatesMet = canDigivolve
    val nextCoinMins = nextDigiCoinTickSec / 60
    val nextCoinSecs = nextDigiCoinTickSec % 60
    val coinTimerText = "%d:%02d".format(java.util.Locale.US, nextCoinMins, nextCoinSecs)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_partner_stage_card"),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, if (canDigivolve) EvolutionGoldSecondary else accent.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = HoloCardElevated)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Visual Showcase Banner of the Active Monster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(206.dp)
                    .testTag("active_monster_hero_image_box")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_holo_arena_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.35f,
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painter = painterResource(id = monsterImageRes),
                    contentDescription = "${creature.nickname} Active Monster Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("active_monster_image")
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CyberVoidBg.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    HoloCardElevated.copy(alpha = 0.96f)
                                )
                            )
                        )
                )

                // Top Telemetry Overlay Badges (FlowRow so badges never overflow on mobile)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HoloTagBadge(
                        text = "LVL ${creature.currentLevel()}/100",
                        color = if (creature.isMaxLevel100()) EvolutionGoldSecondary else BioEmeraldHealth
                    )
                    HoloTagBadge(text = creature.stage.uppercase(), color = accent)
                    HoloTagBadge(text = creature.attribute.uppercase(), color = HoloCyanPrimary)
                    HoloTagBadge(
                        text = "AGE ${raisingProgress.formattedTotalAge()}",
                        color = EvolutionGoldSecondary
                    )
                    HoloTagBadge(
                        text = "♥ $liveBpm BPM",
                        color = BioEmeraldHealth
                    )
                }

                // Bottom Signature Skill & Species Overlay on the Monster Showcase
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ACTIVE PARTNER SCAN // ${creature.speciesName.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = HoloCyanPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = creature.nickname,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HoloTagBadge(
                            text = "SKILL: ${spec.signatureMove.uppercase()}",
                            color = EvolutionGoldSecondary
                        )
                    }
                }
            }

            // Lower Control & Holo-Avatar Telemetry Deck
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HoloSlateSurface.copy(alpha = 0.94f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Compact Holo Avatar + Core Partner Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DigimonHoloCrestAvatar(
                        speciesId = creature.speciesId,
                        stage = creature.stage,
                        isResting = creature.isResting,
                        evolutionReady = canDigivolve,
                        sizeDp = 72.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = spec.titleTag,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "LVL ${creature.currentLevel()}/100 • CP ${creature.combatPower()} • BOND ${creature.bondLevel}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = EvolutionGoldSecondary
                        )
                        Text(
                            text = "DIGICOINS: $digiCoins (10 Coins/Drill • +5 in $coinTimerText)",
                            style = MaterialTheme.typography.labelLarge,
                            color = HoloCyanPrimary
                        )
                    }
                }

                // Row 2: Full-width Evolution & Leveling Status Banner so every word is visible
                Surface(
                    color = CyberVoidBg.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (canDigivolve) BioEmeraldHealth.copy(alpha = 0.6f) else EvolutionGoldSecondary.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            creature.isOnExpedition() -> {
                                val exp = DigimonCatalog.getExpedition(creature.activeExpeditionId)
                                "ON EXPEDITION: ${exp?.title ?: "Active"} (${exp?.durationLabel ?: ""}) • Keep Fed & Cryo-Rested!"
                            }
                            creature.isRareLevel100Form() ->
                                "RARE LVL-100 MYTHIC APEX ACHIEVED • Total Drills ${raisingProgress.drillsCompleted}"
                            canDigivolve ->
                                "ALL EVOLUTION REQUIREMENTS MET (LVL ${creature.currentLevel()})! Tap EVOLVE READY to Digivolve!"
                            creature.stage == "Mega" ->
                                "MEGA STAGE (LVL ${creature.currentLevel()}/100) • Train THIS monster to LVL 100 & meet stats to unlock the 6 Rare Monsters!"
                            creature.currentLevel() < 20 ->
                                "EVOLUTION LOCKED UNTIL LVL 20 (Now LVL ${creature.currentLevel()}/20) • Monsters level ONLY with Training!"
                            else ->
                                "LVL ${creature.currentLevel()}/100 • Sync ${creature.evolutionProgress}/100% • Meet all branch stats & Sync to evolve"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (canDigivolve) BioEmeraldHealth else EvolutionGoldSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Row 3: Full-width 4-Stat Strip (ATK, DEF, SPD, WIS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStatChip("ATK", creature.attack, Color(0xFFFF7043), Modifier.weight(1f))
                    MiniStatChip("DEF", creature.defense, HoloCyanPrimary, Modifier.weight(1f))
                    MiniStatChip("SPD", creature.speed, PlasmaVioletStamina, Modifier.weight(1f))
                    MiniStatChip("WIS", creature.wisdom, BioEmeraldHealth, Modifier.weight(1f))
                }

                // Row 4: Quick Action Strip inside Holo-Stage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenDrillsModal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HoloCyanPrimary,
                            contentColor = CyberVoidBg
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("launch_drills_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Launch Interactive Drill",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "TRAIN (10 COINS)",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenEvolutionMatrix,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (canDigivolve) EvolutionGoldSecondary else HoloSlateSurface,
                            contentColor = if (canDigivolve) CyberVoidBg else EvolutionGoldSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("open_evolution_matrix_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Evolution Matrix",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (canDigivolve) "EVOLVE READY!" else "EVOLVE (LVL 20+)",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Row 5: Wild Taming (100 Monsters), 10 Expeditions (24h), & Unique Skill Tree
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenWildTamingModal,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BioEmeraldHealth.copy(alpha = 0.2f),
                            contentColor = BioEmeraldHealth
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("stage_wild_taming_button")
                    ) {
                        Text(
                            text = "TAME WILD (2c)",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenExpeditionsModal,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = HoloCyanPrimary.copy(alpha = 0.2f),
                            contentColor = HoloCyanPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("stage_expeditions_button")
                    ) {
                        Text(
                            text = if (creature.isOnExpedition()) "EXPEDITION LIVE" else "10 EXPEDITIONS",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenSkillTreeModal,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PlasmaVioletStamina.copy(alpha = 0.22f),
                            contentColor = Color(0xFFDDD6FE)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("stage_skill_tree_button")
                    ) {
                        Text(
                            text = "SKILLS (${creature.availableSkillPoints()} SP)",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthTelemetryWidget(
    creature: DigimonCreature,
    onHealClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val healthColor = if (creature.health <= 35) VirusCrimsonError else BioEmeraldHealth
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
        border = BorderStroke(1.2.dp, healthColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = healthColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.widget_health_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = healthColor,
                        maxLines = 1
                    )
                }
                Text(
                    text = "W-01",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HoloArcGauge(
                percentage = creature.health,
                accentColor = healthColor,
                centerLabel = "${creature.health}%",
                subLabel = if (creature.health >= 80) "OPTIMAL" else if (creature.health > 35) "STABLE" else "WARNING",
                sizeDp = 76.dp
            )

            Text(
                text = "Mistakes: ${creature.careMistakes} • DEF ${creature.defense}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            FilledTonalButton(
                onClick = onHealClicked,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = healthColor.copy(alpha = 0.2f),
                    contentColor = healthColor
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("widget_health_heal_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = stringResource(R.string.action_nano_heal),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.action_nano_heal),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StaminaTelemetryWidget(
    creature: DigimonCreature,
    nextTickSec: Int,
    onToggleRest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
        border = BorderStroke(1.2.dp, PlasmaVioletStamina.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = PlasmaVioletStamina,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.widget_stamina_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = PlasmaVioletStamina,
                        maxLines = 1
                    )
                }
                Text(
                    text = "W-02",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HoloArcGauge(
                percentage = creature.stamina,
                accentColor = PlasmaVioletStamina,
                centerLabel = "${creature.stamina}%",
                subLabel = when {
                    creature.isResting -> "REST ➔ 100%"
                    creature.stamina <= 50 -> "CAN REST"
                    else -> "REST ≤50%"
                },
                sizeDp = 76.dp
            )

            SegmentedPlasmaBar(
                percentage = creature.stamina,
                accentColor = PlasmaVioletStamina
            )

            FilledTonalButton(
                onClick = onToggleRest,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = PlasmaVioletStamina.copy(alpha = 0.22f),
                    contentColor = Color(0xFFDDD6FE)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("widget_stamina_rest_button")
            ) {
                Icon(
                    imageVector = if (creature.isResting) Icons.Default.WbSunny else Icons.Default.Hotel,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when {
                        creature.isResting -> "RESTING (${creature.stamina}%/100%)"
                        creature.stamina <= 50 -> stringResource(R.string.action_rest_toggle)
                        else -> "REST AT ≤50%"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HungerTelemetryWidget(
    creature: DigimonCreature,
    nextTickSec: Int,
    onOpenFeedSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hungerColor = if (creature.hunger <= 25) VirusCrimsonError else SolarCoralHunger
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
        border = BorderStroke(1.2.dp, hungerColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = hungerColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.widget_hunger_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = hungerColor,
                        maxLines = 1
                    )
                }
                Text(
                    text = "W-03",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HoloArcGauge(
                percentage = creature.hunger,
                accentColor = hungerColor,
                centerLabel = "${creature.hunger}%",
                subLabel = if (creature.isResting) "PAUSED" else "-2/${nextTickSec}s",
                sizeDp = 76.dp
            )

            Text(
                text = "1 Free Feed • 5c Stat Feeds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            FilledTonalButton(
                onClick = onOpenFeedSheet,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = hungerColor.copy(alpha = 0.2f),
                    contentColor = hungerColor
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("widget_hunger_feed_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocalDining,
                    contentDescription = stringResource(R.string.action_feed_menu),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.action_feed_menu),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EvolutionTelemetryWidget(
    creature: DigimonCreature,
    raisingProgress: CreatureRaisingProgress,
    onOpenEvolutionMatrix: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = DigimonCatalog.getSpec(creature.speciesId)
    val branches = spec.evolutionBranches
    val isReady = creature.canEvolveAnyBranch()
    val gatesMet = isReady
    val accent = if (isReady) EvolutionGoldSecondary else HoloCyanPrimary

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
        border = BorderStroke(if (isReady) 1.8.dp else 1.2.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.widget_evolution_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        maxLines = 1
                    )
                }
                Text(
                    text = "W-04",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HoloArcGauge(
                percentage = creature.evolutionProgress,
                accentColor = accent,
                centerLabel = "${creature.evolutionProgress}%",
                subLabel = if (branches.isEmpty()) "MEGA APEX" else "LVL ${creature.currentLevel()}/20+",
                sizeDp = 76.dp
            )

            val effortSummary = if (branches.isEmpty()) {
                "Mega Apex • Age ${raisingProgress.formattedTotalAge()}"
            } else {
                "Drills ${raisingProgress.drillsCompleted} • Meals ${raisingProgress.mealsFed}"
            }
            Text(
                text = effortSummary,
                style = MaterialTheme.typography.bodySmall,
                color = if (gatesMet) BioEmeraldHealth else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            FilledTonalButton(
                onClick = onOpenEvolutionMatrix,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isReady) EvolutionGoldSecondary else accent.copy(alpha = 0.2f),
                    contentColor = if (isReady) CyberVoidBg else accent
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("widget_evolution_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isReady) {
                        stringResource(R.string.action_digivolve_now)
                    } else {
                        stringResource(R.string.action_inspect_branches)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HoloTagBadge(text: String, color: Color) {
    Surface(
        color = CyberVoidBg.copy(alpha = 0.72f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
private fun MiniStatChip(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CyberVoidBg.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneHomeScreenWidgetsCard(creature: DigimonCreature) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var syncNotice by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_widgets_panel"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
        border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.45f))
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "4 COMPACT 2×1 PHONE WIDGETS",
                            style = MaterialTheme.typography.titleMedium,
                            color = HoloCyanPrimary
                        )
                        HoloTagBadge(text = "AUTO-REFRESH ON", color = BioEmeraldHealth)
                    }
                    Text(
                        text = "Compact 2×1 launcher tiles auto-refresh every 15s live & 60s on your home screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        com.example.widget.DigiWidgetSyncManager.scheduleAutoRefresh(context)
                        com.example.widget.DigiWidgetSyncManager.refreshAllWidgets(context)
                        syncNotice = "Auto-refresh armed & all 4 compact widgets synced with ${creature.nickname}!"
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HoloCyanPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("sync_phone_widgets_button")
                ) {
                    Text("SYNC NOW", style = MaterialTheme.typography.labelMedium, color = HoloCyanPrimary)
                }
            }

            if (syncNotice != null) {
                Text(
                    text = syncNotice.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = BioEmeraldHealth
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhoneWidgetPinChip(
                    title = "1. Health (${creature.health}%)",
                    subtitle = "2×1 • Auto-Sync",
                    accent = BioEmeraldHealth,
                    onClick = {
                        val pinned = com.example.widget.DigiWidgetSyncManager.requestPinWidget(
                            context,
                            com.example.widget.DigiHealthWidgetProvider::class.java
                        )
                        syncNotice = if (pinned) {
                            "Requested pin for Compact Health 2×1 Widget!"
                        } else {
                            "Health Widget synced! Long-press your phone home screen -> Widgets -> digi to place."
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                PhoneWidgetPinChip(
                    title = "2. Stamina (${creature.stamina}%)",
                    subtitle = "2×1 • Auto-Sync",
                    accent = PlasmaVioletStamina,
                    onClick = {
                        val pinned = com.example.widget.DigiWidgetSyncManager.requestPinWidget(
                            context,
                            com.example.widget.DigiStaminaWidgetProvider::class.java
                        )
                        syncNotice = if (pinned) {
                            "Requested pin for Compact Stamina 2×1 Widget!"
                        } else {
                            "Stamina Widget synced! Long-press your phone home screen -> Widgets -> digi to place."
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhoneWidgetPinChip(
                    title = "3. Satiety (${creature.hunger}%)",
                    subtitle = "2×1 • Auto-Sync",
                    accent = SolarCoralHunger,
                    onClick = {
                        val pinned = com.example.widget.DigiWidgetSyncManager.requestPinWidget(
                            context,
                            com.example.widget.DigiHungerWidgetProvider::class.java
                        )
                        syncNotice = if (pinned) {
                            "Requested pin for Compact Satiety 2×1 Widget!"
                        } else {
                            "Satiety Widget synced! Long-press your phone home screen -> Widgets -> digi to place."
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                PhoneWidgetPinChip(
                    title = "4. Evolution (${creature.evolutionProgress}%)",
                    subtitle = "2×1 • Auto-Sync",
                    accent = EvolutionGoldSecondary,
                    onClick = {
                        val pinned = com.example.widget.DigiWidgetSyncManager.requestPinWidget(
                            context,
                            com.example.widget.DigiEvolutionWidgetProvider::class.java
                        )
                        syncNotice = if (pinned) {
                            "Requested pin for Compact Evolution 2×1 Widget!"
                        } else {
                            "Evolution Widget synced! Long-press your phone home screen -> Widgets -> digi to place."
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PhoneWidgetPinChip(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = HoloSlateSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$subtitle • PIN",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SystemFullResetCard(
    onRequestFullReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("system_full_reset_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
        border = BorderStroke(1.dp, VirusCrimsonError.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = VirusCrimsonError,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SYSTEM RESET // START OVER",
                    style = MaterialTheme.typography.titleMedium,
                    color = VirusCrimsonError
                )
            }
            Text(
                text = "Wipe all partners, evolution stages, training logs, and rankings to re-draft 1 fresh starter monster with 60 DigiCoins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequestFullReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VirusCrimsonError,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("command_deck_full_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Full Reset",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FULL RESET",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

