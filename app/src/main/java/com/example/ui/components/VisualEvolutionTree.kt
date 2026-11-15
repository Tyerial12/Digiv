package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CreatureRaisingProgress
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.EvolutionBranchRequirement
import com.example.ui.theme.BioEmeraldHealth
import com.example.ui.theme.CyberVoidBg
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCardElevated
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.HoloSlateSurface
import com.example.ui.theme.PlasmaVioletStamina

enum class EvolutionNodeStatus {
    CURRENT_ACTIVE,
    UNLOCKED_PAST,
    AVAILABLE_NOW,
    STATS_AND_LEVEL_MET,
    NEXT_STAGE_IN_PROGRESS,
    LOCKED_FUTURE
}

data class VisualTreeNode(
    val speciesId: String,
    val speciesName: String,
    val stageLabel: String,
    val attribute: String,
    val branchRequirement: EvolutionBranchRequirement?,
    val status: EvolutionNodeStatus,
    val isDirectNextBranch: Boolean,
    val statReadinessRatio: Float,
    val requirementSummary: String,
    val missingSummary: String
)

data class VisualTreeTier(
    val tierTitle: String,
    val stageKey: String,
    val nodes: List<VisualTreeNode>
)

object VisualEvolutionTreeBuilder {

    fun buildLineageTiers(
        creature: DigimonCreature,
        raisingProgress: CreatureRaisingProgress
    ): List<VisualTreeTier> {
        val currentSpec = DigimonCatalog.getSpec(creature.speciesId)
        val rootRookieId = resolveRootRookieId(creature)
        val rookieSpec = DigimonCatalog.getSpec(rootRookieId)

        // Collect Champion branches from Rookie (plus current species if it's a Champion not in default rookie branches)
        val championBranches = rookieSpec.evolutionBranches.toMutableList()
        if (creature.stage == "Champion" && championBranches.none { it.targetSpeciesId == creature.speciesId }) {
            championBranches.add(
                0,
                EvolutionBranchRequirement(
                    targetSpeciesId = creature.speciesId,
                    targetSpeciesName = creature.speciesName,
                    targetStage = "Champion",
                    targetAttribute = creature.attribute,
                    minAttack = currentSpec.baseAttack,
                    minDefense = currentSpec.baseDefense,
                    minBond = 35,
                    branchDescription = currentSpec.titleTag
                )
            )
        }

        // Collect Ultimate branches reachable from those Champions (or from current Champion)
        val ultimateSourceSpecs = if (creature.stage == "Champion") {
            listOf(currentSpec)
        } else {
            championBranches.map { DigimonCatalog.getSpec(it.targetSpeciesId) }
        }
        val ultimateBranches = ultimateSourceSpecs
            .flatMap { it.evolutionBranches }
            .distinctBy { it.targetSpeciesId }
            .toMutableList()
        if (creature.stage == "Ultimate" && ultimateBranches.none { it.targetSpeciesId == creature.speciesId }) {
            ultimateBranches.add(
                0,
                EvolutionBranchRequirement(
                    targetSpeciesId = creature.speciesId,
                    targetSpeciesName = creature.speciesName,
                    targetStage = "Ultimate",
                    targetAttribute = creature.attribute,
                    minAttack = currentSpec.baseAttack,
                    minDefense = currentSpec.baseDefense,
                    minBond = 60,
                    branchDescription = currentSpec.titleTag
                )
            )
        }

        // Collect Mega branches reachable from those Ultimates
        val megaSourceSpecs = if (creature.stage == "Ultimate") {
            listOf(currentSpec)
        } else {
            ultimateBranches.map { DigimonCatalog.getSpec(it.targetSpeciesId) }
        }
        val megaBranches = megaSourceSpecs
            .flatMap { it.evolutionBranches }
            .distinctBy { it.targetSpeciesId }
            .toMutableList()
        if (creature.stage == "Mega" && !creature.isRareLevel100Form() && megaBranches.none { it.targetSpeciesId == creature.speciesId }) {
            megaBranches.add(
                0,
                EvolutionBranchRequirement(
                    targetSpeciesId = creature.speciesId,
                    targetSpeciesName = creature.speciesName,
                    targetStage = "Mega",
                    targetAttribute = creature.attribute,
                    minAttack = currentSpec.baseAttack,
                    minDefense = currentSpec.baseDefense,
                    minBond = 80,
                    branchDescription = currentSpec.titleTag
                )
            )
        }

        // Tier 1: Rookie Origin
        val unlockedEvoIds = creature.unlockedEvolutionFormIds()
        val rookieNode = VisualTreeNode(
            speciesId = rookieSpec.speciesId,
            speciesName = rookieSpec.speciesName,
            stageLabel = "Rookie",
            attribute = rookieSpec.attribute,
            branchRequirement = null,
            status = when {
                creature.speciesId == rookieSpec.speciesId -> EvolutionNodeStatus.CURRENT_ACTIVE
                rookieSpec.speciesId in unlockedEvoIds || creature.stage != "Rookie" -> EvolutionNodeStatus.UNLOCKED_PAST
                else -> EvolutionNodeStatus.LOCKED_FUTURE
            },
            isDirectNextBranch = false,
            statReadinessRatio = 1f,
            requirementSummary = "Origin Partner • Base ATK ${rookieSpec.baseAttack} • DEF ${rookieSpec.baseDefense}",
            missingSummary = "Origin Form"
        )

        // Direct branches from the active creature
        val directBranchByTargetId = currentSpec.evolutionBranches.associateBy { it.targetSpeciesId }

        val championNodes = championBranches.map { branch ->
            evaluateBranchNode(
                creature = creature,
                raisingProgress = raisingProgress,
                branch = directBranchByTargetId[branch.targetSpeciesId] ?: branch,
                isDirectNext = directBranchByTargetId.containsKey(branch.targetSpeciesId)
            )
        }

        val ultimateNodes = ultimateBranches.map { branch ->
            evaluateBranchNode(
                creature = creature,
                raisingProgress = raisingProgress,
                branch = directBranchByTargetId[branch.targetSpeciesId] ?: branch,
                isDirectNext = directBranchByTargetId.containsKey(branch.targetSpeciesId)
            )
        }

        val megaNodes = megaBranches.map { branch ->
            evaluateBranchNode(
                creature = creature,
                raisingProgress = raisingProgress,
                branch = directBranchByTargetId[branch.targetSpeciesId] ?: branch,
                isDirectNext = directBranchByTargetId.containsKey(branch.targetSpeciesId)
            )
        }

        val mythicNodes = DigimonCatalog.level100RareBranches.map { branch ->
            evaluateBranchNode(
                creature = creature,
                raisingProgress = raisingProgress,
                branch = directBranchByTargetId[branch.targetSpeciesId] ?: branch,
                isDirectNext = directBranchByTargetId.containsKey(branch.targetSpeciesId)
            )
        }

        return listOf(
            VisualTreeTier("1. ROOKIE", "Rookie", listOf(rookieNode)),
            VisualTreeTier("2. CHAMPION PATHS", "Champion", championNodes),
            VisualTreeTier("3. ULTIMATE PATHS", "Ultimate", ultimateNodes),
            VisualTreeTier("4. MEGA APEX", "Mega", megaNodes),
            VisualTreeTier("5. LVL 100 RARE MYTHIC (6)", "Mythic", mythicNodes)
        )
    }

    private fun resolveRootRookieId(creature: DigimonCreature): String {
        val unlockedRookie = creature.unlockedEvolutionFormIds().firstOrNull { id ->
            DigimonCatalog.allSpecies[id]?.stage == "Rookie"
        }
        if (unlockedRookie != null) return unlockedRookie
        if (creature.stage == "Rookie") return creature.speciesId

        // Dynamically search all Rookies in the 100-monster catalog to find the lineage leading to creature.speciesId
        val rookies = DigimonCatalog.allSpeciesSpecs.filter { it.stage == "Rookie" }
        // 1. Direct Rookie -> Champion match
        val directRookie = rookies.firstOrNull { r ->
            r.evolutionBranches.any { it.targetSpeciesId == creature.speciesId }
        }
        if (directRookie != null) return directRookie.speciesId

        // 2. Rookie -> Champion -> Ultimate match
        val twoStepRookie = rookies.firstOrNull { r ->
            r.evolutionBranches.any { champBranch ->
                val champSpec = DigimonCatalog.allSpecies[champBranch.targetSpeciesId]
                champSpec?.evolutionBranches?.any { it.targetSpeciesId == creature.speciesId } == true
            }
        }
        if (twoStepRookie != null) return twoStepRookie.speciesId

        // 3. Rookie -> Champion -> Ultimate -> Mega match
        val threeStepRookie = rookies.firstOrNull { r ->
            r.evolutionBranches.any { champBranch ->
                val champSpec = DigimonCatalog.allSpecies[champBranch.targetSpeciesId]
                champSpec?.evolutionBranches?.any { ultBranch ->
                    val ultSpec = DigimonCatalog.allSpecies[ultBranch.targetSpeciesId]
                    ultSpec?.evolutionBranches?.any { it.targetSpeciesId == creature.speciesId } == true
                } == true
            }
        }
        if (threeStepRookie != null) return threeStepRookie.speciesId

        return "agumon"
    }

    private fun evaluateBranchNode(
        creature: DigimonCreature,
        raisingProgress: CreatureRaisingProgress,
        branch: EvolutionBranchRequirement,
        isDirectNext: Boolean
    ): VisualTreeNode {
        val curLvl = creature.currentLevel()
        val levelMet = curLvl >= branch.requiredLevel
        val syncMet = creature.evolutionProgress >= 100
        val atkMet = creature.attack >= branch.minAttack
        val defMet = creature.defense >= branch.minDefense
        val spdMet = creature.speed >= branch.minSpeed
        val wisMet = creature.wisdom >= branch.minWisdom
        val bondMet = creature.bondLevel >= branch.minBond
        val statsAndLevelMet = levelMet && atkMet && defMet && spdMet && wisMet && bondMet

        val checks = mutableListOf<Float>()
        if (branch.requiredLevel > 1) {
            checks.add((curLvl.toFloat() / branch.requiredLevel.toFloat()).coerceIn(0f, 1f))
        }
        if (isDirectNext) {
            checks.add((creature.evolutionProgress.toFloat() / 100f).coerceIn(0f, 1f))
        }
        if (branch.minAttack > 0) {
            checks.add((creature.attack.toFloat() / branch.minAttack.toFloat()).coerceIn(0f, 1f))
        }
        if (branch.minDefense > 0) {
            checks.add((creature.defense.toFloat() / branch.minDefense.toFloat()).coerceIn(0f, 1f))
        }
        if (branch.minSpeed > 0) {
            checks.add((creature.speed.toFloat() / branch.minSpeed.toFloat()).coerceIn(0f, 1f))
        }
        if (branch.minWisdom > 0) {
            checks.add((creature.wisdom.toFloat() / branch.minWisdom.toFloat()).coerceIn(0f, 1f))
        }
        if (branch.minBond > 0) {
            checks.add((creature.bondLevel.toFloat() / branch.minBond.toFloat()).coerceIn(0f, 1f))
        }
        val readiness = if (checks.isEmpty()) 1f else checks.average().toFloat()

        val canEvolveRightNow = isDirectNext && branch.isStatsAndSyncMetBy(creature)

        val status = when {
            creature.speciesId == branch.targetSpeciesId -> EvolutionNodeStatus.CURRENT_ACTIVE
            branch.targetSpeciesId in creature.unlockedForms -> EvolutionNodeStatus.UNLOCKED_PAST
            canEvolveRightNow -> EvolutionNodeStatus.AVAILABLE_NOW
            statsAndLevelMet && syncMet -> EvolutionNodeStatus.STATS_AND_LEVEL_MET
            isDirectNext -> EvolutionNodeStatus.NEXT_STAGE_IN_PROGRESS
            else -> EvolutionNodeStatus.LOCKED_FUTURE
        }

        val reqParts = buildList {
            if (branch.requiredLevel > 1) add("LVL $curLvl/${branch.requiredLevel}")
            add("SYNC ${creature.evolutionProgress}/100%")
            if (branch.minAttack > 0) add("ATK ${creature.attack}/${branch.minAttack}")
            if (branch.minDefense > 0) add("DEF ${creature.defense}/${branch.minDefense}")
            if (branch.minSpeed > 0) add("SPD ${creature.speed}/${branch.minSpeed}")
            if (branch.minWisdom > 0) add("WIS ${creature.wisdom}/${branch.minWisdom}")
            add("BOND ${creature.bondLevel}/${branch.minBond}")
        }

        val missingParts = buildList {
            if (!levelMet) add("Need LVL ${branch.requiredLevel} (Now LVL $curLvl)")
            if (!syncMet) add("+${100 - creature.evolutionProgress}% Sync")
            if (!atkMet) add("+${branch.minAttack - creature.attack} ATK")
            if (!defMet) add("+${branch.minDefense - creature.defense} DEF")
            if (!spdMet) add("+${branch.minSpeed - creature.speed} SPD")
            if (!wisMet) add("+${branch.minWisdom - creature.wisdom} WIS")
            if (!bondMet) add("+${branch.minBond - creature.bondLevel} Bond")
            if (!isDirectNext && creature.speciesId != branch.targetSpeciesId && branch.targetSpeciesId !in creature.unlockedForms) {
                add("Evolve to preceding stage first")
            }
        }

        return VisualTreeNode(
            speciesId = branch.targetSpeciesId,
            speciesName = branch.targetSpeciesName,
            stageLabel = if (branch.isRareLevel100Branch) "Rare Lv.100" else branch.targetStage,
            attribute = branch.targetAttribute,
            branchRequirement = branch,
            status = status,
            isDirectNextBranch = isDirectNext,
            statReadinessRatio = readiness,
            requirementSummary = reqParts.joinToString(" • "),
            missingSummary = if (missingParts.isEmpty()) "All Requirements Met — Ready to Evolve!" else missingParts.joinToString(" • ")
        )
    }
}

@Composable
fun VisualEvolutionBranchingTreeCard(
    creature: DigimonCreature,
    raisingProgress: CreatureRaisingProgress,
    onSelectBranchToEvolve: (EvolutionBranchRequirement) -> Unit,
    modifier: Modifier = Modifier
) {
    val tiers = remember(
        creature.speciesId,
        creature.stage,
        creature.attack,
        creature.defense,
        creature.speed,
        creature.wisdom,
        creature.bondLevel,
        creature.evolutionProgress,
        creature.unlockedForms,
        raisingProgress
    ) {
        VisualEvolutionTreeBuilder.buildLineageTiers(creature, raisingProgress)
    }

    val allNodes = remember(tiers) { tiers.flatMap { it.nodes } }
    var userClickedSpeciesId by remember(creature.speciesId) { mutableStateOf("") }

    val inspected = remember(allNodes, userClickedSpeciesId) {
        allNodes.firstOrNull { it.speciesId == userClickedSpeciesId }
            ?: allNodes.firstOrNull { it.status == EvolutionNodeStatus.AVAILABLE_NOW }
            ?: allNodes.firstOrNull { it.isDirectNextBranch }
            ?: allNodes.firstOrNull { it.status == EvolutionNodeStatus.CURRENT_ACTIVE }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tree_pulse")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_evolution_branching_tree_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
        border = BorderStroke(1.5.dp, HoloCyanPrimary.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EvolutionGoldSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "VISUAL DIGIVOLUTION BRANCHING TREE",
                            style = MaterialTheme.typography.titleMedium,
                            color = EvolutionGoldSecondary
                        )
                    }
                    Text(
                        text = "${creature.nickname} • LVL ${creature.currentLevel()}/100 • SYNC ${creature.evolutionProgress}% • ATK ${creature.attack} • DEF ${creature.defense} • SPD ${creature.speed} • WIS ${creature.wisdom}",
                        style = MaterialTheme.typography.labelMedium,
                        color = HoloCyanPrimary
                    )
                }
            }

            // Legend Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TreeLegendBadge("★ Ready to Evolve", EvolutionGoldSecondary)
                TreeLegendBadge("✓ Stats & LVL Met", BioEmeraldHealth)
                TreeLegendBadge("● Current Form", HoloCyanPrimary)
                TreeLegendBadge("⚡ Next Target", PlasmaVioletStamina)
            }

            // Horizontal Scrollable Visual Tree Graph with Connector Lines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
                    .testTag("visual_evolution_tree_scroll"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tiers.forEachIndexed { index, tier ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(if (tier.stageKey == "Mythic") 172.dp else 156.dp)
                    ) {
                        Text(
                            text = tier.tierTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (tier.stageKey == "Mythic") EvolutionGoldSecondary else HoloCyanPrimary
                        )

                        tier.nodes.forEach { node ->
                            VisualTreeNodeCard(
                                node = node,
                                isSelected = inspected?.speciesId == node.speciesId,
                                pulseAlpha = highlightAlpha,
                                onClick = { userClickedSpeciesId = node.speciesId }
                            )
                        }
                    }

                    if (index < tiers.lastIndex) {
                        val nextTier = tiers[index + 1]
                        BranchConnectorCanvas(
                            fromNodes = tier.nodes,
                            toNodes = nextTier.nodes,
                            highlightAlpha = highlightAlpha
                        )
                    }
                }
            }

            // Selected Node Live Telemetry & Action Inspector
            if (inspected != null) {
                SelectedTreeNodeInspector(
                    creature = creature,
                    node = inspected,
                    onTriggerEvolve = onSelectBranchToEvolve
                )
            }
        }
    }
}

@Composable
private fun TreeLegendBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun BranchConnectorCanvas(
    fromNodes: List<VisualTreeNode>,
    toNodes: List<VisualTreeNode>,
    highlightAlpha: Float
) {
    val maxCount = maxOf(fromNodes.size, toNodes.size).coerceAtLeast(1)
    val canvasHeight = (maxCount * 82).coerceIn(90, 460).dp

    Canvas(
        modifier = Modifier
            .width(38.dp)
            .height(canvasHeight)
    ) {
        val fromCount = fromNodes.size.coerceAtLeast(1)
        val toCount = toNodes.size.coerceAtLeast(1)

        for (i in fromNodes.indices) {
            val fromNode = fromNodes[i]
            val startY = size.height * ((i + 0.5f) / fromCount)

            for (j in toNodes.indices) {
                val toNode = toNodes[j]
                val endY = size.height * ((j + 0.5f) / toCount)

                val isHighlightedPath =
                    toNode.status == EvolutionNodeStatus.AVAILABLE_NOW ||
                        toNode.status == EvolutionNodeStatus.STATS_AND_LEVEL_MET ||
                        toNode.status == EvolutionNodeStatus.CURRENT_ACTIVE ||
                        toNode.status == EvolutionNodeStatus.UNLOCKED_PAST ||
                        (fromNode.status == EvolutionNodeStatus.CURRENT_ACTIVE && toNode.isDirectNextBranch)

                val strokeColor = when (toNode.status) {
                    EvolutionNodeStatus.AVAILABLE_NOW -> EvolutionGoldSecondary.copy(alpha = highlightAlpha)
                    EvolutionNodeStatus.STATS_AND_LEVEL_MET -> BioEmeraldHealth.copy(alpha = 0.9f)
                    EvolutionNodeStatus.CURRENT_ACTIVE,
                    EvolutionNodeStatus.UNLOCKED_PAST -> HoloCyanPrimary.copy(alpha = 0.85f)
                    EvolutionNodeStatus.NEXT_STAGE_IN_PROGRESS -> PlasmaVioletStamina.copy(alpha = 0.7f)
                    EvolutionNodeStatus.LOCKED_FUTURE -> Color.White.copy(alpha = 0.22f)
                }

                val path = Path().apply {
                    moveTo(0f, startY)
                    cubicTo(
                        size.width * 0.48f,
                        startY,
                        size.width * 0.52f,
                        endY,
                        size.width,
                        endY
                    )
                }

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = if (isHighlightedPath) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = if (isHighlightedPath) null else PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                )
            }
        }
    }
}

@Composable
private fun VisualTreeNodeCard(
    node: VisualTreeNode,
    isSelected: Boolean,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val accentColor = when (node.status) {
        EvolutionNodeStatus.AVAILABLE_NOW -> EvolutionGoldSecondary
        EvolutionNodeStatus.STATS_AND_LEVEL_MET -> BioEmeraldHealth
        EvolutionNodeStatus.CURRENT_ACTIVE -> HoloCyanPrimary
        EvolutionNodeStatus.UNLOCKED_PAST -> BioEmeraldHealth.copy(alpha = 0.85f)
        EvolutionNodeStatus.NEXT_STAGE_IN_PROGRESS -> PlasmaVioletStamina
        EvolutionNodeStatus.LOCKED_FUTURE -> Color(0xFF64748B)
    }

    val statusBadgeText = when (node.status) {
        EvolutionNodeStatus.AVAILABLE_NOW -> "★ AVAILABLE"
        EvolutionNodeStatus.STATS_AND_LEVEL_MET -> "✓ STATS MET"
        EvolutionNodeStatus.CURRENT_ACTIVE -> "● ACTIVE"
        EvolutionNodeStatus.UNLOCKED_PAST -> "✓ UNLOCKED"
        EvolutionNodeStatus.NEXT_STAGE_IN_PROGRESS -> "${(node.statReadinessRatio * 100).toInt()}% STATS"
        EvolutionNodeStatus.LOCKED_FUTURE -> if (node.branchRequirement?.requiredLevel == 100) "NEEDS LVL 100" else "LOCKED"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.20f) else HoloSlateSurface,
        border = BorderStroke(
            width = if (isSelected || node.status == EvolutionNodeStatus.AVAILABLE_NOW || node.status == EvolutionNodeStatus.CURRENT_ACTIVE) 2.dp else 1.dp,
            color = if (node.status == EvolutionNodeStatus.AVAILABLE_NOW) {
                accentColor.copy(alpha = pulseAlpha)
            } else {
                accentColor.copy(alpha = if (isSelected) 1f else 0.65f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("visual_tree_node_${node.speciesId}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = DigimonCatalog.getMonsterImageRes(node.speciesId)),
                    contentDescription = node.speciesName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CyberVoidBg)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.speciesName,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = when (node.status) {
                        EvolutionNodeStatus.AVAILABLE_NOW -> Icons.Default.Stars
                        EvolutionNodeStatus.STATS_AND_LEVEL_MET,
                        EvolutionNodeStatus.CURRENT_ACTIVE,
                        EvolutionNodeStatus.UNLOCKED_PAST -> Icons.Default.CheckCircle
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Mini Stat/Level Readiness Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CyberVoidBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(node.statReadinessRatio.coerceIn(0.08f, 1f))
                        .height(4.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun SelectedTreeNodeInspector(
    creature: DigimonCreature,
    node: VisualTreeNode,
    onTriggerEvolve: (EvolutionBranchRequirement) -> Unit
) {
    val branch = node.branchRequirement
    val accent = when (node.status) {
        EvolutionNodeStatus.AVAILABLE_NOW -> EvolutionGoldSecondary
        EvolutionNodeStatus.STATS_AND_LEVEL_MET -> BioEmeraldHealth
        EvolutionNodeStatus.CURRENT_ACTIVE -> HoloCyanPrimary
        else -> PlasmaVioletStamina
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visual_tree_inspector_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberVoidBg),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PATH INSPECTOR // ${node.speciesName.uppercase()} (${node.stageLabel.uppercase()})",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${(node.statReadinessRatio * 100).toInt()}% READY",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }

            Text(
                text = "Current vs Required: ${node.requirementSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            Text(
                text = if (node.status == EvolutionNodeStatus.CURRENT_ACTIVE) {
                    "Active Form: ${creature.nickname} is currently in this stage."
                } else {
                    "Path Status: ${node.missingSummary}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (node.status == EvolutionNodeStatus.AVAILABLE_NOW || node.status == EvolutionNodeStatus.STATS_AND_LEVEL_MET) {
                    BioEmeraldHealth
                } else {
                    EvolutionGoldSecondary
                }
            )

            if (branch != null && node.isDirectNextBranch) {
                val canEvolveNow = node.status == EvolutionNodeStatus.AVAILABLE_NOW
                Button(
                    onClick = { onTriggerEvolve(branch) },
                    enabled = canEvolveNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvolutionGoldSecondary,
                        contentColor = CyberVoidBg
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tree_inspector_evolve_button_${node.speciesId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (canEvolveNow) {
                            "EVOLVE INTO ${node.speciesName.uppercase()} NOW"
                        } else {
                            "LOCKED: ${node.missingSummary.uppercase()}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
