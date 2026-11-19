package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AcornOrbType
import com.example.data.model.BattleMoveCategory
import com.example.data.model.CompanionNature
import com.example.data.model.DigiWorldExpansionCatalog
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.DrillType
import com.example.data.model.ElementalMatchupEngine
import com.example.data.model.EmeraldReserveEntry
import com.example.data.model.FrontierBpShopItem
import com.example.data.model.HeldRelicItem
import com.example.data.model.LongPlayRpgCatalog
import com.example.data.model.ResonanceRune
import com.example.data.model.SanctumLeaderSpec
import com.example.data.model.TurnBasedBattleMode
import com.example.data.model.WildStatusEffect
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

// ============================================================================
// HUB 1 COMPONENT: SANCTUARY DAYCARE BREEDING, EGG TRAITS & SHINY NURSERY
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SanctuaryDaycareNurseryCard(
    uiState: DashboardUiState,
    onAssignParents: (String, String) -> Unit,
    onNurtureEgg: () -> Unit,
    onHatchEgg: () -> Unit,
    onDeployFromReserve: (EmeraldReserveEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val daycare = uiState.daycareState
    val party = uiState.creatures
    val reserve = uiState.emeraldReserveEntries
    var selectedParentOneId by remember(party) {
        mutableStateOf(daycare.parentOneId.ifBlank { party.firstOrNull()?.id.orEmpty() })
    }
    var selectedParentTwoId by remember(party, reserve) {
        mutableStateOf(
            daycare.parentTwoId.ifBlank {
                party.getOrNull(1)?.id ?: reserve.firstOrNull()?.id.orEmpty()
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sanctuary_daycare_nursery_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
        border = BorderStroke(1.5.dp, EvolutionGoldSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = EvolutionGoldSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "SANCTUARY DAYCARE & SHINY NURSERY",
                            style = MaterialTheme.typography.titleMedium,
                            color = EvolutionGoldSecondary
                        )
                        Text(
                            text = "Pair Companions • Inherit Natures, Egg Moves & +18..+36 IVs • Hunt ✨ Chromatic Shinies",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = BioEmeraldHealth.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BioEmeraldHealth)
                ) {
                    Text(
                        text = "HATCHED ${daycare.totalEggsHatched} (✨${daycare.totalShinyHatched})",
                        style = MaterialTheme.typography.labelSmall,
                        color = BioEmeraldHealth,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Parent 1 & Parent 2 Quick Selector Chips
            Text(
                text = "SELECT DAYCARE PARENTS (PARTY & EMERALD RESERVE COMPATIBLE)",
                style = MaterialTheme.typography.labelSmall,
                color = HoloCyanPrimary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                party.forEach { comp ->
                    val isP1 = selectedParentOneId == comp.id
                    val isP2 = selectedParentTwoId == comp.id && !isP1
                    Surface(
                        color = when {
                            isP1 -> HoloCyanPrimary.copy(alpha = 0.22f)
                            isP2 -> EvolutionGoldSecondary.copy(alpha = 0.22f)
                            else -> HoloSlateSurface
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isP1 -> HoloCyanPrimary
                                isP2 -> EvolutionGoldSecondary
                                else -> HoloCyanPrimary.copy(alpha = 0.35f)
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (selectedParentOneId != comp.id && (selectedParentOneId.isBlank() || selectedParentTwoId == comp.id)) {
                                    selectedParentOneId = comp.id
                                } else if (selectedParentOneId == comp.id) {
                                    selectedParentTwoId = comp.id
                                } else {
                                    selectedParentTwoId = comp.id
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = DigimonCatalog.getMonsterImageRes(comp.speciesId)),
                                contentDescription = comp.nickname,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(
                                    text = "${if (comp.isChromaticShiny()) "✨" else ""}${comp.nickname}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = when {
                                        isP1 -> "PARENT 1 • ${comp.attribute}"
                                        isP2 -> "PARENT 2 • ${comp.attribute}"
                                        else -> "TAP TO PAIR • ${comp.attribute}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isP1 -> HoloCyanPrimary
                                        isP2 -> EvolutionGoldSecondary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onAssignParents(selectedParentOneId, selectedParentTwoId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HoloCyanPrimary,
                    contentColor = CyberVoidBg
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("daycare_pair_parents_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (daycare.hasActiveEgg) {
                        "RE-PAIR PARENTS & REFRESH EGG GENETICS"
                    } else {
                        "PAIR PARENTS & INCUBATE SANCTUARY EGG"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (daycare.hasActiveEgg) {
                val nature = CompanionNature.fromCode(daycare.inheritedNatureCode)
                Surface(
                    color = CyberVoidBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, EvolutionGoldSecondary.copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🥚 ${daycare.offspringSpeciesName.uppercase()} SANCTUARY EGG",
                                style = MaterialTheme.typography.titleSmall,
                                color = EvolutionGoldSecondary
                            )
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, EvolutionGoldSecondary)
                            ) {
                                Text(
                                    text = "✨ ${daycare.chromaticShinyOddsPercent}% SHINY ODDS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Lineage: ${daycare.parentOneName} (${daycare.parentOneAttribute}) × ${daycare.parentTwoName} (${daycare.parentTwoAttribute})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TraitPillBadge("Nature: ${nature.displayName}", HoloCyanPrimary)
                            TraitPillBadge("+${daycare.inheritedBonusStats} Inherited IVs", BioEmeraldHealth)
                            TraitPillBadge("Egg Move: ${daycare.inheritedEggMove}", PlasmaVioletStamina)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Incubation Progress (Battles, Drills, or Warmth)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${daycare.incubationPercent}% / 100%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (daycare.isReadyToHatch) BioEmeraldHealth else EvolutionGoldSecondary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { daycare.incubationPercent / 100f },
                                color = if (daycare.isReadyToHatch) BioEmeraldHealth else EvolutionGoldSecondary,
                                trackColor = HoloSlateSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNurtureEgg,
                                border = BorderStroke(1.dp, EvolutionGoldSecondary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("daycare_nurture_egg_button")
                            ) {
                                Text(
                                    text = if (daycare.isReadyToHatch) "READY!" else "WARM EGG (+38%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EvolutionGoldSecondary
                                )
                            }
                            Button(
                                onClick = if (daycare.isReadyToHatch) onHatchEgg else onNurtureEgg,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (daycare.isReadyToHatch) BioEmeraldHealth else EvolutionGoldSecondary,
                                    contentColor = CyberVoidBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .heightIn(min = 48.dp)
                                    .testTag("daycare_hatch_egg_button")
                            ) {
                                Text(
                                    text = if (daycare.isReadyToHatch) "🐣 HATCH EGG NOW!" else "INCUBATE & HATCH",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Quick Emerald Reserve Vault Preview & Swap into Active Party
            if (reserve.isNotEmpty()) {
                Text(
                    text = "EMERALD RESERVE VAULT (${reserve.size} STORED) • TAP TO DEPLOY INTO PARTY",
                    style = MaterialTheme.typography.labelSmall,
                    color = BioEmeraldHealth
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reserve.take(10).forEach { entry ->
                        Surface(
                            color = HoloSlateSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (entry.chromaticShiny) EvolutionGoldSecondary else BioEmeraldHealth.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onDeployFromReserve(entry) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = DigimonCatalog.getMonsterImageRes(entry.speciesId)),
                                    contentDescription = entry.nickname,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Text(
                                        text = "${if (entry.chromaticShiny) "✨" else ""}${entry.nickname}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "LVL ${entry.capturedLevel} • DEPLOY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BioEmeraldHealth
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

@Composable
private fun TraitPillBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ============================================================================
// HUB 2 COMPONENT: VISUAL DUAL-REALM WORLD MAP & 4-MOVE TURN-BASED BATTLE ARENA
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualWorldMapAndTurnBasedArenaCard(
    uiState: DashboardUiState,
    onSelectRegion: (String) -> Unit,
    onExecuteTurnMove: (Int) -> Unit,
    onSwitchPartyCompanion: (String) -> Unit,
    onThrowAcornOrb: () -> Unit,
    onSelectAcornOrbType: (AcornOrbType) -> Unit,
    onStartSanctumLeaderBattle: (SanctumLeaderSpec) -> Unit,
    onStartFrontierFloorBattle: () -> Unit,
    onScanWildInRegion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = uiState.activeCreature
    val currentRegion = uiState.selectedWorldRegion
    val battle = uiState.turnBasedBattle
    val forgedRelic = active?.let { uiState.getForgedRelicState(it.heldRelic()) }
    val bonusSpdPct = forgedRelic?.totalBonusSpdPct() ?: 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Visual Dual-Realm World Map Card (8 Interactive Biome Nodes across Aurelia & Argentia)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("visual_world_map_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
            border = BorderStroke(1.5.dp, Color(currentRegion.accentColorHex))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(currentRegion.accentColorHex),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "DUAL-REALM WORLD ATLAS // AURELIA & ARGENTIA",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(currentRegion.accentColorHex)
                            )
                            Text(
                                text = "${currentRegion.regionTitle} (${currentRegion.realmName} • ${currentRegion.recommendedLevelRange})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        color = Color(currentRegion.accentColorHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(currentRegion.accentColorHex))
                    ) {
                        Text(
                            text = currentRegion.weatherLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(currentRegion.accentColorHex),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Horizontal scrollable 8-node visual world map
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LongPlayRpgCatalog.worldRegions.forEach { region ->
                        val isSelected = region.regionId == currentRegion.regionId
                        val accent = Color(region.accentColorHex)
                        Surface(
                            color = if (isSelected) accent.copy(alpha = 0.24f) else HoloSlateSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent else accent.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectRegion(region.regionId) }
                                .testTag("world_region_node_${region.regionId}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = region.realmName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent
                                    )
                                    Text(
                                        text = region.recommendedLevelRange,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = region.regionTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${region.primaryAttribute} • ${region.weatherLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "🌦️ Biome Effect: ${currentRegion.weatherEffectSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Interactive 4-Move Turn-Based Battle Arena (Supports Wild, Sanctum Leader, and Frontier Tower)
        if (active != null && battle != null) {
            val moves = remember(active.speciesId, active.attribute, active.stage) {
                DigimonCatalog.getFourBattleMoves(active)
            }
            val playerFirst = battle.playerActsFirst(active, bonusSpdPct)
            val modeAccent = when (battle.mode) {
                TurnBasedBattleMode.WILD_ENCOUNTER -> BioEmeraldHealth
                TurnBasedBattleMode.SANCTUM_LEADER -> EvolutionGoldSecondary
                TurnBasedBattleMode.FRONTIER_TOWER -> PlasmaVioletStamina
            }
            val modeBadgeText = when (battle.mode) {
                TurnBasedBattleMode.WILD_ENCOUNTER -> "WILD ENCOUNTER // TURN ${battle.turnNumber}"
                TurnBasedBattleMode.SANCTUM_LEADER -> "SANCTUM TRIAL #${battle.sanctumBadgeNumber} // TURN ${battle.turnNumber}"
                TurnBasedBattleMode.FRONTIER_TOWER -> "FRONTIER FLOOR ${battle.frontierFloorNumber} // TURN ${battle.turnNumber}"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("turn_based_battle_arena_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                border = BorderStroke(1.8.dp, modeAccent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Battle Mode & Speed Turn Order Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = modeAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, modeAccent)
                        ) {
                            Text(
                                text = modeBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = modeAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = (if (playerFirst) HoloCyanPrimary else SolarCoralHunger).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (playerFirst) HoloCyanPrimary else SolarCoralHunger)
                        ) {
                            Text(
                                text = if (playerFirst) {
                                    "⚡ FIRST STRIKE (YOUR SPD ${active.effectiveSpeed()})"
                                } else {
                                    "⚠️ ENEMY FASTER (SPD ${battle.enemySpeed})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (playerFirst) HoloCyanPrimary else SolarCoralHunger,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Combatants Split Stage (Player Companion vs Enemy Boss/Wild)
                    Surface(
                        color = CyberVoidBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, modeAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Player Active Companion
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = DigimonCatalog.getMonsterImageRes(active.speciesId)),
                                            contentDescription = active.nickname,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column {
                                            Text(
                                                text = "${if (active.isChromaticShiny()) "✨" else ""}${active.nickname}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = HoloCyanPrimary,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "LVL ${active.currentLevel()} • ${active.attribute}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { (active.health / 100f).coerceIn(0f, 1f) },
                                        color = if (active.health > 30) BioEmeraldHealth else VirusCrimsonError,
                                        trackColor = HoloSlateSurface,
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                    Text(
                                        text = "HP ${active.health}/100 • ${active.heldRelic().displayName} +${forgedRelic?.upgradeLevel ?: 0}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "VS",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EvolutionGoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // Right: Opponent Companion
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = battle.enemyName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (battle.isChromaticShiny) EvolutionGoldSecondary else modeAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "LVL ${battle.enemyLevel} • ${battle.enemyAttribute}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                        Image(
                                            painter = painterResource(id = DigimonCatalog.getMonsterImageRes(battle.enemySpeciesId)),
                                            contentDescription = battle.enemyName,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (battle.enemyHpPercent / 100f).coerceIn(0f, 1f) },
                                        color = when {
                                            battle.enemyHpPercent <= 20 -> VirusCrimsonError
                                            battle.enemyHpPercent <= 50 -> EvolutionGoldSecondary
                                            else -> BioEmeraldHealth
                                        },
                                        trackColor = HoloSlateSurface,
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (battle.enemyStatusEffect != WildStatusEffect.NONE) {
                                            TraitPillBadge(battle.enemyStatusEffect.badgeLabel, PlasmaVioletStamina)
                                        }
                                        Text(
                                            text = "HP ${battle.enemyCurrentHp}/${battle.enemyMaxHp} (${battle.enemyHpPercent}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4-Move Tactical Grid with PP & Elemental Matchup Badges
                    Text(
                        text = "TACTICAL 4-MOVE COMMAND DECK (SPEED ORDER & ELEMENTAL MATCHUPS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = HoloCyanPrimary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        moves.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { move ->
                                    val ppLeft = battle.movePpRemaining[move.moveIndex] ?: move.maxPp
                                    val mult = ElementalMatchupEngine.effectivenessMultiplier(
                                        attackerAttr = move.elementAttribute,
                                        defenderAttr = battle.enemyAttribute
                                    )
                                    val moveColor = when (move.category) {
                                        BattleMoveCategory.ELEMENTAL_STRIKE -> when {
                                            mult >= 1.8f -> BioEmeraldHealth
                                            mult <= 0.6f -> SolarCoralHunger
                                            else -> HoloCyanPrimary
                                        }
                                        BattleMoveCategory.MERCY_SUBDUE -> EvolutionGoldSecondary
                                        BattleMoveCategory.STATUS_WAVE -> PlasmaVioletStamina
                                        BattleMoveCategory.CREST_SURGE -> BioEmeraldHealth
                                    }
                                    val effTag = when (move.category) {
                                        BattleMoveCategory.ELEMENTAL_STRIKE -> when {
                                            mult >= 1.8f -> "2.0x SUPER"
                                            mult <= 0.6f -> "0.5x RESIST"
                                            else -> "1.0x STAB"
                                        }
                                        BattleMoveCategory.MERCY_SUBDUE -> "SAFE 1% HP"
                                        BattleMoveCategory.STATUS_WAVE -> "SLEEP/PARA"
                                        BattleMoveCategory.CREST_SURGE -> "+18 HP HEAL"
                                    }

                                    Surface(
                                        color = moveColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.2.dp, moveColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onExecuteTurnMove(move.moveIndex) }
                                            .testTag("turn_move_btn_${move.moveIndex}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = move.moveName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "PP $ppLeft/${move.maxPp}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = moveColor
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${move.elementAttribute} • PWR ${move.power}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = effTag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = moveColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mid-Battle Active Party Switch Bar (4 Companions)
                    if (uiState.creatures.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "🔄 MID-BATTLE TACTICAL PARTY SWITCH (+15% AEGIS GUARD ON SWITCH)",
                                style = MaterialTheme.typography.labelSmall,
                                color = EvolutionGoldSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.creatures.forEach { member ->
                                    val isCurrent = member.id == active.id
                                    val vsMult = ElementalMatchupEngine.effectivenessMultiplier(
                                        attackerAttr = member.attribute,
                                        defenderAttr = battle.enemyAttribute
                                    )
                                    Surface(
                                        color = if (isCurrent) HoloCyanPrimary.copy(alpha = 0.22f) else HoloSlateSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isCurrent -> HoloCyanPrimary
                                                vsMult >= 1.8f -> BioEmeraldHealth
                                                else -> HoloCyanPrimary.copy(alpha = 0.35f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !isCurrent) {
                                                onSwitchPartyCompanion(member.id)
                                            }
                                            .testTag("mid_battle_switch_${member.speciesId}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = DigimonCatalog.getMonsterImageRes(member.speciesId)),
                                                contentDescription = member.nickname,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column {
                                                Text(
                                                    text = member.nickname,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = when {
                                                        isCurrent -> "LEADING • HP ${member.health}%"
                                                        vsMult >= 1.8f -> "2.0x ADV • HP ${member.health}%"
                                                        else -> "${member.attribute} • HP ${member.health}%"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (vsMult >= 1.8f) BioEmeraldHealth else HoloCyanPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Action Row: Throw Acorn Orb (Wild Mode) or Next Encounter / Frontier Floor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (battle.mode == TurnBasedBattleMode.WILD_ENCOUNTER && !battle.isFinished) {
                            val selectedOrb = uiState.selectedAcornOrbType
                            val orbCount = uiState.getAcornOrbCount(selectedOrb)
                            Button(
                                onClick = onThrowAcornOrb,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BioEmeraldHealth,
                                    contentColor = CyberVoidBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .heightIn(min = 48.dp)
                                    .testTag("turn_arena_throw_orb_button")
                            ) {
                                Text(
                                    text = "THROW ${selectedOrb.shortLabel.uppercase()} ($orbCount)",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                when (battle.mode) {
                                    TurnBasedBattleMode.FRONTIER_TOWER -> onStartFrontierFloorBattle()
                                    else -> onScanWildInRegion()
                                }
                            },
                            border = BorderStroke(1.dp, modeAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("turn_arena_next_encounter_button")
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = modeAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    battle.mode == TurnBasedBattleMode.FRONTIER_TOWER ->
                                        "FLOOR ${uiState.frontierTowerState.currentFloor}"
                                    else -> "SCAN BIOME"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = modeAccent
                            )
                        }
                    }

                    // Live Turn-by-Turn Combat Log Feed
                    if (battle.combatLog.isNotEmpty()) {
                        Surface(
                            color = CyberVoidBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = battle.lastActionHeadline,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = modeAccent
                                )
                                battle.combatLog.take(3).forEach { line ->
                                    Text(
                                        text = "• $line",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
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

// ============================================================================
// HUB 3 SCREEN: BATTLE FRONTIER TOWER, TAMER QUESTS & PENTATHLON DRILLS
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrontierAndQuestsHubTab(
    uiState: DashboardUiState,
    onStartFrontierBattle: () -> Unit,
    onRedeemBpItem: (FrontierBpShopItem) -> Unit,
    onClaimQuest: (String) -> Unit,
    onClaimPassMilestone: (Int) -> Unit,
    onCompleteDrill: (DrillType, Int) -> Unit,
    onStart24hOfflineTraining: () -> Unit,
    onTogglePauseOfflineTraining: () -> Unit,
    onOpenBattleArenaInWorld: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Frontier Tower, 1 = Quests & Pass, 2 = Pentathlon & 24h Strider
    val tower = uiState.frontierTowerState
    val pass = uiState.tamerPassState
    val unclaimedQuests = uiState.tamerQuests.count { it.isCompleted && !it.isClaimed }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub-Tab Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val subTabs = listOf(
                Triple(0, "🗼 FRONTIER (FL ${tower.currentFloor})", PlasmaVioletStamina),
                Triple(1, "📜 QUESTS${if (unclaimedQuests > 0) " ($unclaimedQuests!)" else ""}", EvolutionGoldSecondary),
                Triple(2, "⚡ DRILLS & 24H", HoloCyanPrimary)
            )
            subTabs.forEach { (idx, title, accent) ->
                val selected = selectedSubTab == idx
                Button(
                    onClick = { selectedSubTab = idx },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) accent else HoloCardElevated,
                        contentColor = if (selected) CyberVoidBg else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("frontier_hub_subtab_$idx")
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                // SUB-TAB 0: ENDLESS BATTLE FRONTIER TOWER & BP EXCHANGE SHOP
                val previewBattle = remember(tower.currentFloor) {
                    LongPlayRpgCatalog.buildFrontierOpponentForFloor(tower.currentFloor)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                        border = BorderStroke(1.8.dp, PlasmaVioletStamina),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("battle_frontier_tower_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AETHERION BATTLE FRONTIER TOWER",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PlasmaVioletStamina
                                    )
                                    Text(
                                        text = "Endless Scaling Gauntlet • Boss Wardens Every 5th Floor",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = EvolutionGoldSecondary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, EvolutionGoldSecondary)
                                ) {
                                    Text(
                                        text = "${tower.battlePoints} BP",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EvolutionGoldSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TraitPillBadge("Current Floor: ${tower.currentFloor}", PlasmaVioletStamina)
                                TraitPillBadge("Best Floor: ${tower.highestFloorReached}", HoloCyanPrimary)
                                TraitPillBadge("Win Streak: x${tower.currentWinStreak} (Best x${tower.bestWinStreak})", EvolutionGoldSecondary)
                                TraitPillBadge("Bosses Cleared: ${tower.totalBossesDefeated}", BioEmeraldHealth)
                            }

                            // Next Floor Opponent Preview Card
                            Surface(
                                color = CyberVoidBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (tower.isBossFloor) EvolutionGoldSecondary else PlasmaVioletStamina
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = DigimonCatalog.getMonsterImageRes(previewBattle.enemySpeciesId)),
                                        contentDescription = previewBattle.enemyName,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = if (tower.isBossFloor) {
                                                "👑 BOSS FLOOR ${tower.currentFloor}: ${previewBattle.enemyName.uppercase()}"
                                            } else {
                                                "⚔️ FLOOR ${tower.currentFloor}: ${previewBattle.enemyName.uppercase()}"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (tower.isBossFloor) EvolutionGoldSecondary else Color.White
                                        )
                                        Text(
                                            text = "LVL ${previewBattle.enemyLevel} • ${previewBattle.enemyAttribute} • HP ${previewBattle.enemyMaxHp} • SPD ${previewBattle.enemySpeed}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HoloCyanPrimary
                                        )
                                        Text(
                                            text = "Clear Reward: +${if (tower.isBossFloor) 18 else 8}+ BP, +${15 + tower.currentFloor * 3} Coins, & +1 Companion Level!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BioEmeraldHealth
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onStartFrontierBattle()
                                    onOpenBattleArenaInWorld()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tower.isBossFloor) EvolutionGoldSecondary else PlasmaVioletStamina,
                                    contentColor = CyberVoidBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("start_frontier_floor_battle_button")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (tower.isBossFloor) {
                                        "CHALLENGE BOSS WARDEN (FLOOR ${tower.currentFloor})"
                                    } else {
                                        "ENTER TURN-BASED TOWER BATTLE (FLOOR ${tower.currentFloor})"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    // Frontier BP Exchange Shop Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                        border = BorderStroke(1.5.dp, EvolutionGoldSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "FRONTIER BP EXCHANGE // ENDGAME RELICS & CHARMS",
                                style = MaterialTheme.typography.titleMedium,
                                color = EvolutionGoldSecondary
                            )
                            FrontierBpShopItem.entries.forEach { item ->
                                val alreadyUnlockedCharm =
                                    item == FrontierBpShopItem.SHINY_STARLIGHT_CHARM && tower.shinyCharmUnlocked
                                Surface(
                                    color = CyberVoidBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = { onRedeemBpItem(item) },
                                            enabled = !alreadyUnlockedCharm,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (alreadyUnlockedCharm) BioEmeraldHealth else EvolutionGoldSecondary,
                                                contentColor = CyberVoidBg
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("frontier_bp_redeem_${item.itemId}")
                                        ) {
                                            Text(
                                                text = if (alreadyUnlockedCharm) "OWNED" else "${item.bpCost} BP",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // SUB-TAB 1: DAILY & WEEKLY TAMER QUESTS + MILESTONE REWARD TRACK
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tamer Pass Milestone Reward Track Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                        border = BorderStroke(1.8.dp, EvolutionGoldSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tamer_pass_milestone_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SUNHEART TAMER PASS // TIER ${pass.currentTier}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EvolutionGoldSecondary
                                    )
                                    Text(
                                        text = "Complete Daily & Weekly Quests to unlock Milestone Chests",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TraitPillBadge("${pass.xpInCurrentTier}/100 PASS XP", HoloCyanPrimary)
                            }

                            LinearProgressIndicator(
                                progress = { (pass.xpInCurrentTier / 100f).coerceIn(0f, 1f) },
                                color = EvolutionGoldSecondary,
                                trackColor = HoloSlateSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..6).forEach { tier ->
                                    val unlocked = tier <= pass.currentTier
                                    val claimed = tier in pass.claimedMilestoneTiers
                                    Surface(
                                        color = when {
                                            claimed -> BioEmeraldHealth.copy(alpha = 0.2f)
                                            unlocked -> EvolutionGoldSecondary.copy(alpha = 0.24f)
                                            else -> HoloSlateSurface
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            1.2.dp,
                                            when {
                                                claimed -> BioEmeraldHealth
                                                unlocked -> EvolutionGoldSecondary
                                                else -> HoloCyanPrimary.copy(alpha = 0.35f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .width(128.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(enabled = unlocked && !claimed) {
                                                onClaimPassMilestone(tier)
                                            }
                                            .testTag("tamer_pass_tier_$tier")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "TIER $tier CHEST",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (unlocked) EvolutionGoldSecondary else Color.White
                                            )
                                            Text(
                                                text = "+${40 + tier * 10} Coins\n+2 Shards • +1 Lunar Orb",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = when {
                                                    claimed -> "✓ CLAIMED"
                                                    unlocked -> "TAP TO CLAIM!"
                                                    else -> "LOCKED"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when {
                                                    claimed -> BioEmeraldHealth
                                                    unlocked -> EvolutionGoldSecondary
                                                    else -> HoloCyanPrimary
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Daily & Weekly Quests List
                    uiState.tamerQuests.forEach { quest ->
                        val accent = if (quest.isWeekly) PlasmaVioletStamina else HoloCyanPrimary
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                            border = BorderStroke(
                                1.2.dp,
                                when {
                                    quest.isClaimed -> BioEmeraldHealth.copy(alpha = 0.5f)
                                    quest.isCompleted -> EvolutionGoldSecondary
                                    else -> accent.copy(alpha = 0.6f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${if (quest.isWeekly) "[WEEKLY] " else "[DAILY] "}${quest.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = quest.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${quest.currentProgress}/${quest.targetProgress}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (quest.isCompleted) BioEmeraldHealth else accent
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { quest.progressFraction },
                                    color = if (quest.isCompleted) BioEmeraldHealth else accent,
                                    trackColor = HoloSlateSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rewards: +${quest.rewardCoins} Coins • +${quest.rewardRelicShards} Shards • +${quest.rewardBp} BP • +${quest.rewardPassXp} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EvolutionGoldSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { onClaimQuest(quest.questId) },
                                        enabled = quest.isCompleted && !quest.isClaimed,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                quest.isClaimed -> BioEmeraldHealth.copy(alpha = 0.25f)
                                                quest.isCompleted -> EvolutionGoldSecondary
                                                else -> HoloSlateSurface
                                            },
                                            contentColor = CyberVoidBg
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("claim_quest_${quest.questId}")
                                    ) {
                                        Text(
                                            text = when {
                                                quest.isClaimed -> "CLAIMED"
                                                quest.isCompleted -> "CLAIM REWARD"
                                                else -> "IN PROGRESS"
                                            },
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // SUB-TAB 2: PENTATHLON DRILLS & 24-HOUR HEARTLAND STRIDER
                DrillsHubTab(
                    activeCreature = uiState.activeCreature,
                    digiCoins = uiState.digiCoins,
                    nextDigiCoinTickSec = uiState.nextDigiCoinTickSec,
                    onCompleteDrill = onCompleteDrill,
                    onStart24hOfflineTraining = onStart24hOfflineTraining,
                    onTogglePauseOfflineTraining = onTogglePauseOfflineTraining
                )
            }
        }
    }
}

// ============================================================================
// HUB 4 COMPONENT: RELIC CRAFTING FORGE (+1..+5) & RESONANCE RUNE SOCKETS
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelicCraftingForgeCard(
    uiState: DashboardUiState,
    onEquipRelic: (HeldRelicItem) -> Unit,
    onUpgradeRelic: (HeldRelicItem) -> Unit,
    onCraftOrSocketRune: (HeldRelicItem, Int, ResonanceRune) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = uiState.activeCreature
    val equippedRelic = active?.heldRelic() ?: HeldRelicItem.SEARING_CHARCOAL
    var selectedRelic by remember(equippedRelic) { mutableStateOf(equippedRelic) }
    var activeSocketSlot by remember { mutableIntStateOf(1) }
    val upgradeState = uiState.getForgedRelicState(selectedRelic)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("relic_crafting_forge_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
        border = BorderStroke(1.8.dp, PlasmaVioletStamina)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hardware,
                        contentDescription = null,
                        tint = PlasmaVioletStamina,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "AETHERION RELIC FORGE & RUNE SOCKETS",
                            style = MaterialTheme.typography.titleMedium,
                            color = PlasmaVioletStamina
                        )
                        Text(
                            text = "Enhance Held Relics (+0 ➔ +5) & Socket 2 Custom Resonance Runes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TraitPillBadge(
                    text = "${uiState.standardMonsterPartsCount} Acorns • ${uiState.ultraRareMonsterPartsCount} Shards",
                    color = EvolutionGoldSecondary
                )
            }

            // 6 Held Relics Selector Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeldRelicItem.entries.forEach { relic ->
                    val st = uiState.getForgedRelicState(relic)
                    val isSelected = relic == selectedRelic
                    val isEquipped = relic == equippedRelic
                    Surface(
                        color = if (isSelected) PlasmaVioletStamina.copy(alpha = 0.24f) else HoloSlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = when {
                                isSelected -> PlasmaVioletStamina
                                isEquipped -> BioEmeraldHealth
                                else -> HoloCyanPrimary.copy(alpha = 0.35f)
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedRelic = relic }
                            .testTag("forge_select_relic_${relic.code}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${relic.displayName} +${st.upgradeLevel}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Text(
                                text = if (isEquipped) "✓ EQUIPPED ON LEAD" else relic.effectSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEquipped) BioEmeraldHealth else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Selected Relic Anvil Details & +1..+5 Forge Action
            Surface(
                color = CyberVoidBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, PlasmaVioletStamina),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedRelic.displayName.uppercase()} +${upgradeState.upgradeLevel}/5",
                                style = MaterialTheme.typography.titleSmall,
                                color = EvolutionGoldSecondary
                            )
                            Text(
                                text = "Base: ${selectedRelic.effectSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        OutlinedButton(
                            onClick = { onEquipRelic(selectedRelic) },
                            border = BorderStroke(1.dp, BioEmeraldHealth),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = if (selectedRelic == equippedRelic) "✓ EQUIPPED" else "EQUIP RELIC",
                                style = MaterialTheme.typography.labelMedium,
                                color = BioEmeraldHealth
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TraitPillBadge("+${upgradeState.totalBonusAtkPct()}% ATK", SolarCoralHunger)
                        TraitPillBadge("+${upgradeState.totalBonusDefPct()}% DEF", HoloCyanPrimary)
                        TraitPillBadge("+${upgradeState.totalBonusSpdPct()}% SPD", BioEmeraldHealth)
                        TraitPillBadge("+${upgradeState.totalCritChancePct()}% CRIT", EvolutionGoldSecondary)
                        if (upgradeState.totalLifestealPct() > 0) {
                            TraitPillBadge("+${upgradeState.totalLifestealPct()}% LIFESTEAL", PlasmaVioletStamina)
                        }
                    }

                    val nextLvl = (upgradeState.upgradeLevel + 1).coerceAtMost(5)
                    val acornCost = nextLvl * 2
                    val shardCost = if (nextLvl >= 3) 2 else 1

                    Button(
                        onClick = { onUpgradeRelic(selectedRelic) },
                        enabled = upgradeState.upgradeLevel < 5,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlasmaVioletStamina,
                            contentColor = CyberVoidBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("forge_upgrade_relic_button")
                    ) {
                        Text(
                            text = if (upgradeState.upgradeLevel >= 5) {
                                "★ MAXIMUM SOVEREIGN +5 ENHANCEMENT REACHED"
                            } else {
                                "⚒️ FORGE +$nextLvl ENHANCEMENT (${acornCost}x Acorns + ${shardCost}x Shards or ${nextLvl * 12} Coins)"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // 2 Resonance Rune Sockets
                    Text(
                        text = "RESONANCE RUNE SOCKETS (TAP SOCKET #1 OR #2 THEN SELECT A RUNE BELOW)",
                        style = MaterialTheme.typography.labelSmall,
                        color = HoloCyanPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val s1 = upgradeState.resolvedRuneOne()
                        val s2 = upgradeState.resolvedRuneTwo()
                        listOf(1 to s1, 2 to s2).forEach { (slotNum, rune) ->
                            val isTargetSlot = activeSocketSlot == slotNum
                            Surface(
                                color = if (isTargetSlot) EvolutionGoldSecondary.copy(alpha = 0.2f) else HoloSlateSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isTargetSlot) EvolutionGoldSecondary else HoloCyanPrimary.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeSocketSlot = slotNum }
                                    .testTag("forge_socket_slot_$slotNum")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "SOCKET #$slotNum ${if (isTargetSlot) "(ACTIVE)" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isTargetSlot) EvolutionGoldSecondary else HoloCyanPrimary
                                    )
                                    Text(
                                        text = rune?.displayName ?: "Empty Rune Socket",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = rune?.shortBadge ?: "Tap a Rune below to socket",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BioEmeraldHealth
                                    )
                                }
                            }
                        }
                    }

                    // Craftable Resonance Runes Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResonanceRune.entries.forEach { rune ->
                            val isUnlocked = rune.runeCode in uiState.unlockedRuneCodes
                            Surface(
                                color = HoloSlateSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isUnlocked) BioEmeraldHealth else EvolutionGoldSecondary
                                ),
                                modifier = Modifier
                                    .width(175.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onCraftOrSocketRune(selectedRelic, activeSocketSlot, rune)
                                    }
                                    .testTag("forge_rune_chip_${rune.runeCode}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = rune.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = rune.shortBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HoloCyanPrimary
                                    )
                                    Text(
                                        text = if (isUnlocked) {
                                            "✓ UNLOCKED • Tap to Socket #$activeSocketSlot"
                                        } else {
                                            "Craft: ${rune.craftGildedAcornsCost} Acorns + ${rune.craftRelicShardsCost} Shards"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUnlocked) BioEmeraldHealth else EvolutionGoldSecondary
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
