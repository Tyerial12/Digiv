package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class DigimonCreature(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nickname: String = "Agumon",
    val speciesId: String = "agumon",
    val speciesName: String = "Agumon",
    val stage: String = "Rookie",
    val attribute: String = "Vaccine",
    val health: Int = 95,
    val stamina: Int = 85,
    val hunger: Int = 80,
    val evolutionProgress: Int = 25,
    val attack: Int = 45,
    val defense: Int = 38,
    val speed: Int = 42,
    val wisdom: Int = 34,
    val bondLevel: Int = 30,
    val careMistakes: Int = 0,
    @get:PropertyName("isResting") @set:PropertyName("isResting")
    var isResting: Boolean = false,
    val unlockedForms: List<String> = listOf("agumon"),
    val nonTrainingBonusStats: Int = 0,
    val unlockedSkillNodes: List<String> = emptyList(),
    val activeExpeditionId: String = "",
    val expeditionStartMs: Long = 0L,
    val expeditionEndMs: Long = 0L,
    val completedExpeditionsCount: Int = 0,
    val offlineTrainingEndMs: Long = 0L,
    val offlineTrainingLastTickMs: Long = 0L,
    val recoveryEndsAtMs: Long = 0L,
    val lastUpdatedMs: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun unlockedEvolutionFormIds(): List<String> {
        return unlockedForms.filter {
            !it.startsWith("sk:") &&
                !it.startsWith("bn:") &&
                !it.startsWith("ex:") &&
                !it.startsWith("ot:") &&
                !it.startsWith("rc:")
        }.distinct()
    }

    fun allUnlockedSkillNodeIds(): Set<String> {
        val fromForms = unlockedForms
            .filter { it.startsWith("sk:") }
            .map { it.removePrefix("sk:") }
        val combined = (unlockedSkillNodes + fromForms).toSet()
        val suffixMapped = combined.mapNotNull { id ->
            val idx = id.indexOf("_node_")
            if (idx >= 0) "${speciesId}${id.substring(idx)}" else null
        }
        return combined + suffixMapped
    }

    fun unlockedSkillCount(): Int {
        val tree = DigimonCatalog.getSkillTree(speciesId)
        val activeIds = allUnlockedSkillNodeIds()
        return tree.nodes.count { it.nodeId in activeIds }
    }

    fun persistedNonTrainingBonusStats(): Int {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("bn:") }
            ?.removePrefix("bn:")
            ?.toIntOrNull() ?: 0
        val tree = DigimonCatalog.getSkillTree(speciesId)
        val activeIds = allUnlockedSkillNodeIds()
        val skillStatSum = tree.nodes
            .filter { it.nodeId in activeIds }
            .sumOf { it.atkBonus + it.defBonus + it.spdBonus + it.wisBonus }
        return maxOf(nonTrainingBonusStats, fromForms, skillStatSum)
    }

    fun persistedCompletedExpeditions(): Int {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("ex:") }
            ?.removePrefix("ex:")
            ?.toIntOrNull() ?: 0
        return maxOf(completedExpeditionsCount, fromForms)
    }

    fun persistedOfflineTrainingEndMs(): Long {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("ot:") }
            ?.removePrefix("ot:")
            ?.split(":")
            ?.firstOrNull()
            ?.toLongOrNull()
            ?.let { it * 1000L } ?: 0L
        return maxOf(offlineTrainingEndMs, fromForms)
    }

    fun persistedOfflineTrainingLastTickMs(): Long {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("ot:") }
            ?.removePrefix("ot:")
            ?.split(":")
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let { it * 1000L } ?: 0L
        return maxOf(offlineTrainingLastTickMs, fromForms)
    }

    fun persistedRecoveryEndsAtMs(): Long {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("rc:") }
            ?.removePrefix("rc:")
            ?.toLongOrNull()
            ?.let { it * 1000L } ?: 0L
        return maxOf(recoveryEndsAtMs, fromForms)
    }

    fun isInOfflineTraining(nowMs: Long = System.currentTimeMillis()): Boolean =
        persistedOfflineTrainingEndMs() > nowMs

    fun remainingOfflineTrainingSec(nowMs: Long = System.currentTimeMillis()): Long =
        ((persistedOfflineTrainingEndMs() - nowMs) / 1000L).coerceAtLeast(0L)

    fun formattedOfflineTrainingRemaining(nowMs: Long = System.currentTimeMillis()): String {
        val remSec = remainingOfflineTrainingSec(nowMs)
        if (remSec <= 0L) return "0m 00s"
        val hours = remSec / 3600L
        val mins = (remSec % 3600L) / 60L
        val secs = remSec % 60L
        return if (hours > 0L) "${hours}h ${mins}m ${secs}s" else "${mins}m ${secs}s"
    }

    fun isRecoveringFrom1Hp(nowMs: Long = System.currentTimeMillis()): Boolean =
        persistedRecoveryEndsAtMs() > nowMs

    fun remainingRecoverySec(nowMs: Long = System.currentTimeMillis()): Long =
        ((persistedRecoveryEndsAtMs() - nowMs) / 1000L).coerceAtLeast(0L)

    fun formattedRecoveryRemaining(nowMs: Long = System.currentTimeMillis()): String {
        val remSec = remainingRecoverySec(nowMs)
        val mins = remSec / 60L
        val secs = remSec % 60L
        return "%d:%02d".format(java.util.Locale.US, mins, secs)
    }

    fun withUpdatedMetadataInForms(
        newSkillNodes: Collection<String> = allUnlockedSkillNodeIds(),
        newBonusStats: Int = persistedNonTrainingBonusStats(),
        newExpeditionsCount: Int = persistedCompletedExpeditions(),
        newOfflineTrainEndMs: Long = persistedOfflineTrainingEndMs(),
        newOfflineTrainTickMs: Long = persistedOfflineTrainingLastTickMs(),
        newRecoveryEndsMs: Long = persistedRecoveryEndsAtMs(),
        targetSpeciesOverride: String = speciesId
    ): List<String> {
        val baseEvolutionForms = unlockedEvolutionFormIds().take(5).map { it.take(40) }
        val skillTags = newSkillNodes
            .map { id ->
                val idx = id.indexOf("_node_")
                val rawTag = if (idx >= 0) "sk:${targetSpeciesOverride}${id.substring(idx)}" else "sk:$id"
                rawTag.take(40)
            }
            .distinct()
            .take(6)
        val metaTags = buildList {
            if (newBonusStats > 0) add("bn:$newBonusStats".take(40))
            if (newExpeditionsCount > 0) add("ex:$newExpeditionsCount".take(40))
            if (newOfflineTrainEndMs > 0L) {
                val endSec = newOfflineTrainEndMs / 1000L
                val tickSec = (newOfflineTrainTickMs.takeIf { it > 0L } ?: newOfflineTrainEndMs) / 1000L
                add("ot:$endSec:$tickSec".take(40))
            }
            if (newRecoveryEndsMs > 0L) {
                val recSec = newRecoveryEndsMs / 1000L
                add("rc:$recSec".take(40))
            }
        }
        return (baseEvolutionForms + skillTags + metaTags).take(15)
    }

    fun currentLevel(): Int {
        if (speciesId in DigimonCatalog.level100RareSpeciesIds) return 100
        val statSum = attack + defense + speed + wisdom
        val originRookieId = unlockedEvolutionFormIds().firstOrNull { id ->
            DigimonCatalog.allSpecies[id]?.stage == "Rookie"
        }
        val originSpec = when {
            stage == "Rookie" -> DigimonCatalog.allSpecies[speciesId]
            originRookieId != null -> DigimonCatalog.allSpecies[originRookieId]
            else -> null
        }
        val originBaseSum = if (originSpec != null) {
            originSpec.baseAttack + originSpec.baseDefense + originSpec.baseSpeed + originSpec.baseWisdom
        } else {
            172
        }
        val trainedStatsAboveBase = (statSum - originBaseSum - persistedNonTrainingBonusStats()).coerceAtLeast(0)
        // Monsters level ONLY through Training Drills (stats gained from training)
        val trainingLevels = trainedStatsAboveBase / 9
        return (1 + trainingLevels).coerceIn(1, 100)
    }

    fun isMaxLevel100(): Boolean = currentLevel() >= 100

    fun isRareLevel100Form(): Boolean = speciesId in DigimonCatalog.level100RareSpeciesIds

    fun canEvolveAnyBranch(): Boolean {
        val spec = DigimonCatalog.getSpec(speciesId)
        return spec.evolutionBranches.any { it.isStatsAndSyncMetBy(this) }
    }

    fun canEnterCryoRest(): Boolean = !isResting && stamina <= 50 && !isRecoveringFrom1Hp()

    fun isOnExpedition(): Boolean = activeExpeditionId.isNotBlank() && expeditionEndMs > 0L

    fun needsExpeditionCare(): Boolean = isOnExpedition() && (hunger <= 25 || stamina <= 20 || isResting)

    fun totalSkillPoints(): Int = (currentLevel() / 3) + persistedCompletedExpeditions() + 1

    fun availableSkillPoints(): Int = (totalSkillPoints() - unlockedSkillCount()).coerceAtLeast(0)

    fun cryoRegenBonus(): Int {
        val activeIds = allUnlockedSkillNodeIds()
        if (activeIds.isEmpty()) return 0
        val tree = DigimonCatalog.getSkillTree(speciesId)
        return tree.nodes.filter { it.nodeId in activeIds }.sumOf { it.cryoRegenBonus }
    }

    fun tammingRateBonusPercent(): Int {
        val activeIds = allUnlockedSkillNodeIds()
        if (activeIds.isEmpty()) return 0
        val tree = DigimonCatalog.getSkillTree(speciesId)
        return tree.nodes.filter { it.nodeId in activeIds }.sumOf { it.tammingRateBonusPercent }
    }

    fun combatPower(): Int {
        val stageMultiplier = when {
            isRareLevel100Form() -> 10
            stage == "Rookie" -> 1
            stage == "Champion" -> 2
            stage == "Ultimate" -> 4
            stage == "Mega" -> 7
            else -> 1
        }
        val baseStats = attack + defense + speed + wisdom
        val conditionBonus = ((health + stamina + hunger) / 3) + bondLevel
        return (baseStats * stageMultiplier) + (conditionBonus * 3)
    }

    fun moodLabel(): String = when {
        isRecoveringFrom1Hp() -> "1 HP RECOVERING (${formattedRecoveryRemaining()})"
        isInOfflineTraining() -> "24H AUTO-TRAIN (${formattedOfflineTrainingRemaining()})"
        health < 35 -> "CRITICAL DAMAGE"
        hunger < 25 -> "NEEDS FOOD!"
        isResting -> "CRYO-RESTING (${stamina}%/100%)"
        stamina <= 20 -> "NEEDS SLEEP!"
        isOnExpedition() -> "ON EXPEDITION"
        isRareLevel100Form() -> "LVL 100 MYTHIC"
        canEvolveAnyBranch() -> "EVO READY!"
        evolutionProgress >= 100 && currentLevel() < 20 -> "TRAIN TO LVL 20"
        evolutionProgress >= 100 -> "NEED BRANCH STATS"
        health >= 80 && hunger >= 70 && stamina >= 60 -> "PEAK RESONANCE"
        else -> "STABLE SYNC"
    }
}

@IgnoreExtraProperties
data class TrainingLogEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatureId: String = "",
    val creatureName: String = "",
    val activityType: String = "Power Drill",
    val grade: String = "A",
    val statDeltaSummary: String = "+10 ATK, +12% EVO",
    val xpGained: Int = 100,
    val dateKey: String = "2026-11-14",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PetNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatureId: String = "",
    val creatureName: String = "",
    val severity: String = "INFO", // INFO, WARNING, CRITICAL, EVOLUTION
    val title: String = "",
    val message: String = "",
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val leagueId: String = "global",
    val trainerName: String = "Tamer",
    val tamerTitle: String = "Holo Cadet",
    val partnerName: String = "Agumon",
    val partnerStage: String = "Rookie",
    val partnerAttribute: String = "Vaccine",
    val totalCombatPower: Int = 600,
    val evolutionsUnlocked: Int = 3,
    val trainingStreakDays: Int = 1,
    val sparringWins: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TradeListing(
    @DocumentId val id: String = "",
    val creatorUserId: String = "",
    val creatorTrainerName: String = "Tamer",
    val offeredCreatureId: String = "",
    val offeredNickname: String = "Agumon",
    val offeredSpeciesId: String = "agumon",
    val offeredSpeciesName: String = "Agumon",
    val offeredStage: String = "Rookie",
    val offeredAttribute: String = "Vaccine",
    val offeredLevel: Int = 5,
    val offeredAttack: Int = 50,
    val offeredDefense: Int = 42,
    val offeredSpeed: Int = 44,
    val offeredWisdom: Int = 38,
    val offeredBondLevel: Int = 20,
    val requestedStageOrSpecies: String = "Any Stage",
    val status: String = "OPEN", // OPEN, COMPLETED, CANCELLED
    val accepterUserId: String = "",
    val accepterTrainerName: String = "",
    val tradedInNickname: String = "",
    val tradedInSpeciesId: String = "",
    val tradedInSpeciesName: String = "",
    val tradedInStage: String = "Rookie",
    val tradedInAttribute: String = "Vaccine",
    val tradedInLevel: Int = 1,
    val tradedInAttack: Int = 50,
    val tradedInDefense: Int = 50,
    val tradedInSpeed: Int = 50,
    val tradedInWisdom: Int = 50,
    val tradedInBondLevel: Int = 20,
    @get:PropertyName("claimedByCreator") @set:PropertyName("claimedByCreator")
    var claimedByCreator: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun offeredCombatPower(): Int {
        val mult = when {
            offeredSpeciesId in DigimonCatalog.level100RareSpeciesIds -> 10
            offeredStage == "Rookie" -> 1
            offeredStage == "Champion" -> 2
            offeredStage == "Ultimate" -> 4
            offeredStage == "Mega" -> 7
            else -> 1
        }
        return ((offeredAttack + offeredDefense + offeredSpeed + offeredWisdom) * mult) + ((90 + offeredBondLevel) * 3)
    }

    fun isNpcListing(): Boolean {
        return creatorUserId.isBlank() ||
            DigimonCatalog.isNpcOrBotIdentifier(id) ||
            DigimonCatalog.isNpcOrBotIdentifier(creatorUserId) ||
            DigimonCatalog.isNpcOrBotIdentifier(creatorTrainerName) ||
            DigimonCatalog.isNpcOrBotIdentifier(accepterUserId) ||
            DigimonCatalog.isNpcOrBotIdentifier(accepterTrainerName)
    }
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderUserId: String = "",
    val senderTrainerName: String = "Tamer",
    val senderPartnerName: String = "Agumon",
    val senderPartnerStage: String = "Rookie",
    val recipientUserId: String = "",
    val channelId: String = "global",
    val messageText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun isNpcMessage(): Boolean {
        return senderUserId.isBlank() ||
            DigimonCatalog.isNpcOrBotIdentifier(id) ||
            DigimonCatalog.isNpcOrBotIdentifier(senderUserId) ||
            DigimonCatalog.isNpcOrBotIdentifier(senderTrainerName) ||
            DigimonCatalog.isNpcOrBotIdentifier(recipientUserId) ||
            DigimonCatalog.isNpcOrBotIdentifier(channelId)
    }
}

@IgnoreExtraProperties
data class FriendConnection(
    @DocumentId val id: String = "",
    val requesterUserId: String = "",
    val requesterTrainerName: String = "Tamer",
    val requesterPartnerName: String = "Agumon",
    val recipientUserId: String = "",
    val recipientTrainerName: String = "Tamer",
    val recipientPartnerName: String = "Agumon",
    val status: String = "PENDING", // PENDING, ACCEPTED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun otherPlayerId(myUserId: String): String =
        if (requesterUserId == myUserId) recipientUserId else requesterUserId

    fun otherPlayerName(myUserId: String): String =
        if (requesterUserId == myUserId) recipientTrainerName else requesterTrainerName

    fun isNpcConnection(): Boolean {
        return DigimonCatalog.isNpcOrBotIdentifier(id) ||
            DigimonCatalog.isNpcOrBotIdentifier(requesterUserId) ||
            DigimonCatalog.isNpcOrBotIdentifier(requesterTrainerName) ||
            DigimonCatalog.isNpcOrBotIdentifier(recipientUserId) ||
            DigimonCatalog.isNpcOrBotIdentifier(recipientTrainerName)
    }

    fun otherPlayerPartner(myUserId: String): String =
        if (requesterUserId == myUserId) recipientPartnerName else requesterPartnerName

    companion object {
        fun buildFriendshipDocId(uidA: String, uidB: String): String {
            val sorted = listOf(uidA.trim(), uidB.trim()).sorted()
            return "fr_${sorted[0]}_${sorted[1]}"
        }

        fun buildDirectChannelId(uidA: String, uidB: String): String {
            val sorted = listOf(uidA.trim(), uidB.trim()).sorted()
            return "dm_${sorted[0]}_${sorted[1]}"
        }
    }
}

data class StarterDraftChoice(
    val speciesId: String,
    val customNickname: String = ""
)

data class CreatureRaisingProgress(
    val totalAgeMinutes: Long,
    val stageAgeMinutes: Long,
    val requiredStageAgeMinutes: Int,
    val drillsCompleted: Int,
    val requiredDrills: Int,
    val mealsFed: Int,
    val requiredMeals: Int,
    val sparringMatches: Int,
    val requiredSparring: Int
) {
    val isTimeGateMet: Boolean
        get() = stageAgeMinutes >= requiredStageAgeMinutes

    val isEffortGateMet: Boolean
        get() = drillsCompleted >= requiredDrills &&
            mealsFed >= requiredMeals &&
            sparringMatches >= requiredSparring

    val remainingStageMinutes: Long
        get() = (requiredStageAgeMinutes - stageAgeMinutes).coerceAtLeast(0L)

    fun formattedTotalAge(): String = formatMinutes(totalAgeMinutes)

    fun formattedStageMaturationStatus(): String {
        if (requiredStageAgeMinutes <= 0) return "APEX MATURED"
        return if (isTimeGateMet) {
            "TIME GATE MET (${formatMinutes(stageAgeMinutes)} / ${formatMinutes(requiredStageAgeMinutes.toLong())})"
        } else {
            "AGE IN STAGE: ${formatMinutes(stageAgeMinutes)}"
        }
    }

    private fun formatMinutes(mins: Long): String {
        if (mins < 60L) return "${mins}m"
        val hours = mins / 60L
        val remMins = mins % 60L
        if (hours < 24L) {
            return if (remMins > 0L) "${hours}h ${remMins}m" else "${hours}h"
        }
        val days = hours / 24L
        val remHours = hours % 24L
        return "${days}d ${remHours}h"
    }

    companion object {
        fun compute(
            creature: DigimonCreature,
            logs: List<TrainingLogEntry>,
            nowMs: Long = System.currentTimeMillis()
        ): CreatureRaisingProgress {
            val creatureLogs = logs.filter {
                it.creatureId == creature.id || it.creatureName.equals(creature.nickname, ignoreCase = true)
            }
            val createdMs = creature.createdAt?.toDate()?.time
                ?: creatureLogs.minOfOrNull { it.createdAt?.toDate()?.time ?: nowMs }
                ?: creature.lastUpdatedMs.takeIf { it > 0L }
                ?: nowMs

            val latestEvoMs = creatureLogs
                .filter {
                    (it.activityType == "Digivolution" || it.activityType == "Mythic Ascension") &&
                        !it.statDeltaSummary.startsWith("Hatched", ignoreCase = true) &&
                        !it.statDeltaSummary.startsWith("Drafted", ignoreCase = true)
                }
                .maxOfOrNull { it.createdAt?.toDate()?.time ?: createdMs }
                ?: createdMs

            val totalAgeMins = ((nowMs - createdMs).coerceAtLeast(0L)) / 60_000L
            val stageAgeMins = ((nowMs - latestEvoMs).coerceAtLeast(0L)) / 60_000L

            val (reqStageMins, reqDrills, reqMeals, reqSpar) = when (creature.stage) {
                "Rookie" -> listOf(0, 6, 3, 0)
                "Champion" -> listOf(0, 14, 6, 0)
                "Ultimate" -> listOf(0, 24, 10, 0)
                else -> listOf(0, 35, 14, 0)
            }

            val drills = creatureLogs.count { it.activityType.endsWith("Drill") }
            val meals = creatureLogs.count { it.activityType == "Nutrient Feed" }
            val spars = creatureLogs.count { it.activityType == "Arena Sparring" }

            return CreatureRaisingProgress(
                totalAgeMinutes = totalAgeMins,
                stageAgeMinutes = stageAgeMins,
                requiredStageAgeMinutes = reqStageMins,
                drillsCompleted = drills,
                requiredDrills = reqDrills,
                mealsFed = meals,
                requiredMeals = reqMeals,
                sparringMatches = spars,
                requiredSparring = reqSpar
            )
        }
    }
}

data class EvolutionBranchRequirement(
    val targetSpeciesId: String,
    val targetSpeciesName: String,
    val targetStage: String,
    val targetAttribute: String,
    val minAttack: Int = 0,
    val minDefense: Int = 0,
    val minSpeed: Int = 0,
    val minWisdom: Int = 0,
    val minBond: Int = 20,
    val maxCareMistakes: Int = 999,
    val requiredLevel: Int = 20,
    val isRareLevel100Branch: Boolean = false,
    val branchDescription: String
) {
    fun isMetBy(creature: DigimonCreature): Boolean = isStatsAndSyncMetBy(creature)

    fun isStatsAndSyncMetBy(creature: DigimonCreature): Boolean {
        val minLevelGate = if (isRareLevel100Branch) 100 else maxOf(20, requiredLevel)
        return creature.currentLevel() >= minLevelGate &&
            creature.evolutionProgress >= 100 &&
            creature.attack >= minAttack &&
            creature.defense >= minDefense &&
            creature.speed >= minSpeed &&
            creature.wisdom >= minWisdom &&
            creature.bondLevel >= minBond
    }

    fun isFullyMetBy(creature: DigimonCreature, raisingProgress: CreatureRaisingProgress): Boolean {
        return isStatsAndSyncMetBy(creature)
    }
}

data class DigimonSpeciesSpec(
    val speciesId: String,
    val speciesName: String,
    val stage: String,
    val attribute: String,
    val titleTag: String,
    val signatureMove: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Int,
    val baseWisdom: Int,
    val apexTargetName: String = "WarGreymon",
    val isRareLevel100Spec: Boolean = false,
    val evolutionBranches: List<EvolutionBranchRequirement> = emptyList()
)

enum class DrillType(
    val activityName: String,
    val shortTitle: String,
    val subtitle: String,
    val staminaCost: Int,
    val hungerCost: Int,
    val primaryStatLabel: String
) {
    POWER(
        activityName = "Power Drill",
        shortTitle = "OMEGA STRIKE",
        subtitle = "Tap when the plasma ring locks inside the critical overload zone.",
        staminaCost = 18,
        hungerCost = 12,
        primaryStatLabel = "ATK"
    ),
    SPEED(
        activityName = "Speed Drill",
        shortTitle = "VECTOR REFLEX",
        subtitle = "Intercept high-velocity holo-nodes before their signal decays.",
        staminaCost = 16,
        hungerCost = 10,
        primaryStatLabel = "SPD"
    ),
    DEFENSE(
        activityName = "Defense Drill",
        shortTitle = "AEGIS BARRIER",
        subtitle = "Stabilize the hexagonal shield harmonics under kinetic pressure.",
        staminaCost = 18,
        hungerCost = 10,
        primaryStatLabel = "DEF"
    ),
    WISDOM(
        activityName = "Wisdom Drill",
        shortTitle = "CIPHER MATRIX",
        subtitle = "Decode binary tactical sequences to boost evolution sync.",
        staminaCost = 14,
        hungerCost = 9,
        primaryStatLabel = "WIS"
    )
}

enum class DigiRation(
    val displayName: String,
    val description: String,
    val costCoins: Int,
    val hungerRestore: Int,
    val healthRestore: Int,
    val staminaRestore: Int,
    val atkBonus: Int = 0,
    val defBonus: Int = 0,
    val spdBonus: Int = 0,
    val wisBonus: Int = 0,
    val evoBonus: Int = 0,
    val statBoostSummary: String
) {
    FREE_CYBER_WAFER(
        displayName = "Basic Digi-Wafer (FREE)",
        description = "FREE Ration: +28 Hunger, +4 HP, 0 Stamina, 0 Stats (Use Cryo-Rest for Stamina)",
        costCoins = 0,
        hungerRestore = 28,
        healthRestore = 4,
        staminaRestore = 0,
        statBoostSummary = "FREE • +28 HUN, +4 HP (0 Stats, 0 STA)"
    ),
    DIGI_MEAT(
        displayName = "Blaze Sirloin (5 Coins)",
        description = "Costs 5 DigiCoins: +30 Hunger, +2 ATK, +1 Bond (0 Stamina)",
        costCoins = 5,
        hungerRestore = 30,
        healthRestore = 4,
        staminaRestore = 0,
        atkBonus = 2,
        statBoostSummary = "5 COINS • +2 ATK, +30 HUN, +1 BOND"
    ),
    PROTEIN_CHIP(
        displayName = "Aegis Shellfruit (5 Coins)",
        description = "Costs 5 DigiCoins: +30 Hunger, +2 DEF, +1 Bond (0 Stamina)",
        costCoins = 5,
        hungerRestore = 30,
        healthRestore = 4,
        staminaRestore = 0,
        defBonus = 2,
        statBoostSummary = "5 COINS • +2 DEF, +30 HUN, +1 BOND"
    ),
    STAMINA_BERRY(
        displayName = "Volt Speedberry (5 Coins)",
        description = "Costs 5 DigiCoins: +28 Hunger, +2 SPD, +1 Stamina, +1 Bond",
        costCoins = 5,
        hungerRestore = 28,
        healthRestore = 2,
        staminaRestore = 1,
        spdBonus = 2,
        statBoostSummary = "5 COINS • +2 SPD, +28 HUN, +1 STA"
    ),
    GOLDEN_ACORN(
        displayName = "Cipher Mind-Elixir (5 Coins)",
        description = "Costs 5 DigiCoins: +32 Hunger, +2 WIS, +8 HP, +2 Bond (0 Stamina)",
        costCoins = 5,
        hungerRestore = 32,
        healthRestore = 8,
        staminaRestore = 0,
        wisBonus = 2,
        statBoostSummary = "5 COINS • +2 WIS, +32 HUN, +8 HP"
    ),
    ROYAL_FEAST(
        displayName = "Royal Core Feast (5 Coins)",
        description = "Costs 5 DigiCoins: +38 Hunger, +1 ALL STATS (ATK/DEF/SPD/WIS), +1 Stamina",
        costCoins = 5,
        hungerRestore = 38,
        healthRestore = 10,
        staminaRestore = 1,
        atkBonus = 1,
        defBonus = 1,
        spdBonus = 1,
        wisBonus = 1,
        statBoostSummary = "5 COINS • +1 ALL STATS, +38 HUN"
    )
}

object DigimonCatalog {

    const val MAX_SQUAD_SIZE = 4
    const val STARTING_DIGI_COINS = 200 // Awarded on New Game only
    const val TRAINING_DRILL_COIN_COST = 10
    const val OFFLINE_TRAINING_24H_COIN_COST = 100
    const val OFFLINE_TRAINING_24H_DURATION_MS = 24L * 60L * 60L * 1000L
    const val OFFLINE_TRAINING_CYCLE_MS = 15L * 60L * 1000L // +1 Training Cycle every 15 mins (up to 96 cycles in 24h)
    const val BATTLE_RECOVERY_5MIN_DURATION_MS = 5L * 60L * 1000L // 5-minute mandatory recovery when reaching 1 HP
    const val STANDARD_MONSTER_PART_SELL_COINS = 2
    const val ULTRA_RARE_MONSTER_PART_SELL_COINS = 5
    const val PAID_FEED_COIN_COST = 5
    const val TAMMING_BERRY_COIN_COST = 2
    const val DIGI_COIN_INTERVAL_SEC = 300 // 5 minutes per +5 DigiCoins
    const val DIGI_COINS_PER_INTERVAL = 5

    val level100RareSpeciesIds: Set<String> = setOf(
        "omegamon_x",
        "alphamon_ouryuken",
        "imperialdramon_pm",
        "susanoomon_zero",
        "gallantmon_crimson",
        "armageddemon_core"
    )

    val level100RareBranches: List<EvolutionBranchRequirement> = listOf(
        EvolutionBranchRequirement(
            targetSpeciesId = "omegamon_x",
            targetSpeciesName = "Omegamon X",
            targetStage = "Mega",
            targetAttribute = "Vaccine",
            minAttack = 250,
            minDefense = 220,
            minSpeed = 210,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "RARE LVL 100 EVOLUTION: Royal Knight X-Antibody fusion wielding All Delete. Requires this specific monster to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "alphamon_ouryuken",
            targetSpeciesName = "Alphamon Ouryuken",
            targetStage = "Mega",
            targetAttribute = "Vaccine",
            minAttack = 240,
            minDefense = 230,
            minWisdom = 210,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "RARE LVL 100 EVOLUTION: Lord of the Empty Seat wielding Ultimate-Ouryuken. Requires this specific monster to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "imperialdramon_pm",
            targetSpeciesName = "Imperialdramon PM",
            targetStage = "Mega",
            targetAttribute = "Vaccine",
            minAttack = 250,
            minSpeed = 225,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "RARE LVL 100 EVOLUTION: Ancient Paladin Founder wielding the Omni Sword. Requires this specific monster to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "susanoomon_zero",
            targetSpeciesName = "Susanoomon",
            targetStage = "Mega",
            targetAttribute = "Free",
            minAttack = 240,
            minWisdom = 240,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "RARE LVL 100 EVOLUTION: Ten Legendary Warriors unified into the Celestial Emperor. Requires this specific monster to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "gallantmon_crimson",
            targetSpeciesName = "Gallantmon Crimson",
            targetStage = "Mega",
            targetAttribute = "Virus",
            minAttack = 255,
            minDefense = 220,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "RARE LVL 100 EVOLUTION: Crimson Mode overload wielding Gungnir & Blutgang. Requires this specific monster to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "armageddemon_core",
            targetSpeciesName = "Armageddemon",
            targetStage = "Mega",
            targetAttribute = "Free",
            minAttack = 260,
            minDefense = 235,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "RARE LVL 100 EVOLUTION: Apocalypse Super-Titan firing Destiny Destroyer. Requires this specific monster to reach Level 100."
        )
    )

    val allSpecies: Map<String, DigimonSpeciesSpec> = (listOf(
        // ROOKIES (6 Selectable Starter Lineages)
        DigimonSpeciesSpec(
            speciesId = "agumon",
            speciesName = "Agumon",
            stage = "Rookie",
            attribute = "Vaccine",
            titleTag = "Flame Reptile • Core Unit 01",
            signatureMove = "Pepper Breath",
            primaryColorHex = 0xFFFF9800,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 52,
            baseDefense = 42,
            baseSpeed = 44,
            baseWisdom = 36,
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "greymon",
                    targetSpeciesName = "Greymon",
                    targetStage = "Champion",
                    targetAttribute = "Vaccine",
                    minAttack = 65,
                    minDefense = 50,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Balanced offensive/defensive discipline requiring LVL 20+ and 100% Sync."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "geogreymon",
                    targetSpeciesName = "GeoGreymon",
                    targetStage = "Champion",
                    targetAttribute = "Vaccine",
                    minAttack = 72,
                    minSpeed = 58,
                    minBond = 40,
                    requiredLevel = 20,
                    branchDescription = "High-velocity offensive burst training path requiring LVL 20+."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "tyrannomon",
                    targetSpeciesName = "Tyrannomon",
                    targetStage = "Champion",
                    targetAttribute = "Data",
                    minDefense = 55,
                    minBond = 20,
                    requiredLevel = 20,
                    branchDescription = "Resilient Data-adapted dinosaur path requiring LVL 20+ and DEF 55+."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "gabumon",
            speciesName = "Gabumon",
            stage = "Rookie",
            attribute = "Data",
            titleTag = "Pelt Beast • Frost Data Unit",
            signatureMove = "Blue Blaster",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFFA855F7,
            baseAttack = 46,
            baseDefense = 44,
            baseSpeed = 54,
            baseWisdom = 44,
            apexTargetName = "MetalGarurumon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "garurumon",
                    targetSpeciesName = "Garurumon",
                    targetStage = "Champion",
                    targetAttribute = "Vaccine",
                    minSpeed = 68,
                    minAttack = 58,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Arctic wolf predator unlocked via LVL 20+, Speed, and Attack drills."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "dorugamon",
                    targetSpeciesName = "Dorugamon",
                    targetStage = "Champion",
                    targetAttribute = "Data",
                    minDefense = 62,
                    minWisdom = 56,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Ancient X-Antibody dragon beast requiring LVL 20+, Defense & Wisdom."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "patamon",
            speciesName = "Patamon",
            stage = "Rookie",
            attribute = "Data",
            titleTag = "Sky Mammal • Holy Pulse Unit",
            signatureMove = "Boom Bubble",
            primaryColorHex = 0xFFFBBF24,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 40,
            baseDefense = 42,
            baseSpeed = 52,
            baseWisdom = 56,
            apexTargetName = "Seraphimon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "angemon",
                    targetSpeciesName = "Angemon",
                    targetStage = "Champion",
                    targetAttribute = "Vaccine",
                    minWisdom = 70,
                    minBond = 45,
                    requiredLevel = 20,
                    branchDescription = "Six-winged celestial guardian requiring LVL 20+, Wisdom 70+, and Bond 45+."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "gargomon",
                    targetSpeciesName = "Gargomon",
                    targetStage = "Champion",
                    targetAttribute = "Vaccine",
                    minAttack = 58,
                    minSpeed = 60,
                    minBond = 30,
                    requiredLevel = 20,
                    branchDescription = "Heavy twin-gatling brawler unlocked with LVL 20+, Attack & Speed."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "guilmon",
            speciesName = "Guilmon",
            stage = "Rookie",
            attribute = "Virus",
            titleTag = "Digital Hazard • Crimson Unit",
            signatureMove = "Pyro Sphere",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFF59E0B,
            baseAttack = 58,
            baseDefense = 40,
            baseSpeed = 48,
            baseWisdom = 34,
            apexTargetName = "Gallantmon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "growlmon",
                    targetSpeciesName = "Growlmon",
                    targetStage = "Champion",
                    targetAttribute = "Virus",
                    minAttack = 74,
                    minDefense = 52,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Crimson dark dragon forged through LVL 20+, Power & Defense drills."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "veemon",
            speciesName = "Veemon",
            stage = "Rookie",
            attribute = "Vaccine",
            titleTag = "Azure Dragon • Vanguard Unit",
            signatureMove = "Vee Headbutt",
            primaryColorHex = 0xFF00F0FF,
            secondaryColorHex = 0xFFFFB800,
            baseAttack = 54,
            baseDefense = 44,
            baseSpeed = 56,
            baseWisdom = 38,
            apexTargetName = "WarGreymon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "geogreymon",
                    targetSpeciesName = "GeoGreymon",
                    targetStage = "Champion",
                    targetAttribute = "Vaccine",
                    minAttack = 70,
                    minSpeed = 62,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "High-velocity dragon striker path unlocked via LVL 20+, Speed & Power drills."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "dorugamon",
                    targetSpeciesName = "Dorugamon",
                    targetStage = "Champion",
                    targetAttribute = "Data",
                    minDefense = 60,
                    minAttack = 64,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Armored chrome dragon beast path requiring LVL 20+, Defense & Attack."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "dorumon",
            speciesName = "Dorumon",
            stage = "Rookie",
            attribute = "Data",
            titleTag = "Chrono Beast • Prototype Unit",
            signatureMove = "Metal Cannon",
            primaryColorHex = 0xFFA855F7,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 50,
            baseDefense = 52,
            baseSpeed = 46,
            baseWisdom = 48,
            apexTargetName = "MetalGarurumon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "dorugamon",
                    targetSpeciesName = "Dorugamon",
                    targetStage = "Champion",
                    targetAttribute = "Data",
                    minDefense = 66,
                    minWisdom = 58,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Ancient Chrono-Core dragon unlocked via LVL 20+, Defense & Wisdom."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "garurumon",
                    targetSpeciesName = "Garurumon",
                    targetStage = "Champion",
                    targetAttribute = "Vaccine",
                    minSpeed = 64,
                    minAttack = 60,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Supersonic cyber-wolf evolution path requiring LVL 20+."
                )
            )
        ),

        // CHAMPIONS
        DigimonSpeciesSpec(
            speciesId = "greymon",
            speciesName = "Greymon",
            stage = "Champion",
            attribute = "Vaccine",
            titleTag = "Armored Saurian • Champion Class",
            signatureMove = "Nova Blast",
            primaryColorHex = 0xFFFF7043,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 95,
            baseDefense = 84,
            baseSpeed = 72,
            baseWisdom = 66,
            apexTargetName = "WarGreymon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgreymon",
                    targetSpeciesName = "MetalGreymon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minAttack = 130,
                    minDefense = 110,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Cyborg tactical form with Giga Destroyer missile pods (Requires LVL 40+)."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "rizegreymon",
                    targetSpeciesName = "RizeGreymon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minAttack = 135,
                    minSpeed = 105,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Trident Revolver aerial bombardment specialist (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "geogreymon",
            speciesName = "GeoGreymon",
            stage = "Champion",
            attribute = "Vaccine",
            titleTag = "Burst Saurian • Champion Class",
            signatureMove = "Mega Burst",
            primaryColorHex = 0xFFFF5722,
            secondaryColorHex = 0xFFFFD54F,
            baseAttack = 104,
            baseDefense = 78,
            baseSpeed = 86,
            baseWisdom = 64,
            apexTargetName = "WarGreymon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "rizegreymon",
                    targetSpeciesName = "RizeGreymon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minAttack = 135,
                    minSpeed = 105,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Revolver-equipped supersonic dragon cyborg (Requires LVL 40+)."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgreymon",
                    targetSpeciesName = "MetalGreymon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minAttack = 130,
                    minDefense = 105,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Chrome Digizoid armored assault evolution (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "tyrannomon",
            speciesName = "Tyrannomon",
            stage = "Champion",
            attribute = "Data",
            titleTag = "Primeval Dino • Champion Class",
            signatureMove = "Fire Breath",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFF10B981,
            baseAttack = 92,
            baseDefense = 94,
            baseSpeed = 68,
            baseWisdom = 62,
            apexTargetName = "WarGreymon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgreymon",
                    targetSpeciesName = "MetalGreymon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minAttack = 128,
                    minDefense = 115,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Mechanized upgrade into Ultimate cyborg form (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "garurumon",
            speciesName = "Garurumon",
            stage = "Champion",
            attribute = "Vaccine",
            titleTag = "Cyber Lupine • Champion Class",
            signatureMove = "Howling Blaster",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFFE2E8F0,
            baseAttack = 88,
            baseDefense = 78,
            baseSpeed = 102,
            baseWisdom = 76,
            apexTargetName = "MetalGarurumon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "weregarurumon",
                    targetSpeciesName = "WereGarurumon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minSpeed = 135,
                    minAttack = 120,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Bipedal commando beast master of Wolf Claw (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "dorugamon",
            speciesName = "Dorugamon",
            stage = "Champion",
            attribute = "Data",
            titleTag = "Chrome Beast • Champion Class",
            signatureMove = "Power Metal",
            primaryColorHex = 0xFFA855F7,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 94,
            baseDefense = 92,
            baseSpeed = 80,
            baseWisdom = 84,
            apexTargetName = "MetalGarurumon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "weregarurumon",
                    targetSpeciesName = "WereGarurumon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minAttack = 122,
                    minDefense = 115,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Tactical close-quarters Ultimate fighter (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "angemon",
            speciesName = "Angemon",
            stage = "Champion",
            attribute = "Vaccine",
            titleTag = "Seraphic Vanguard • Champion Class",
            signatureMove = "Hand of Fate",
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 84,
            baseDefense = 82,
            baseSpeed = 90,
            baseWisdom = 106,
            apexTargetName = "Seraphimon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "magnaangemon",
                    targetSpeciesName = "MagnaAngemon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minWisdom = 140,
                    minBond = 65,
                    requiredLevel = 40,
                    branchDescription = "Eight-winged archangel wielding Gate of Destiny (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "gargomon",
            speciesName = "Gargomon",
            stage = "Champion",
            attribute = "Vaccine",
            titleTag = "Heavy Artillery • Champion Class",
            signatureMove = "Gargo Laser",
            primaryColorHex = 0xFF10B981,
            secondaryColorHex = 0xFFFBBF24,
            baseAttack = 90,
            baseDefense = 86,
            baseSpeed = 88,
            baseWisdom = 80,
            apexTargetName = "Seraphimon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "magnaangemon",
                    targetSpeciesName = "MagnaAngemon",
                    targetStage = "Ultimate",
                    targetAttribute = "Vaccine",
                    minAttack = 120,
                    minWisdom = 115,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Radiant Ultimate commander (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "growlmon",
            speciesName = "Growlmon",
            stage = "Champion",
            attribute = "Virus",
            titleTag = "Plasma Blade • Champion Class",
            signatureMove = "Exhaust Flame",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFF97316,
            baseAttack = 106,
            baseDefense = 82,
            baseSpeed = 82,
            baseWisdom = 68,
            apexTargetName = "Gallantmon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "wargrowlmon",
                    targetSpeciesName = "WarGrowlmon",
                    targetStage = "Ultimate",
                    targetAttribute = "Virus",
                    minAttack = 142,
                    minDefense = 110,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Atomic Blaster cyborg dragon with twin Pendulum Blades (Requires LVL 40+)."
                )
            )
        ),

        // ULTIMATES
        DigimonSpeciesSpec(
            speciesId = "metalgreymon",
            speciesName = "MetalGreymon",
            stage = "Ultimate",
            attribute = "Vaccine",
            titleTag = "Chrome Digizoid • Ultimate Class",
            signatureMove = "Giga Destroyer",
            primaryColorHex = 0xFFFF5722,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 165,
            baseDefense = 148,
            baseSpeed = 124,
            baseWisdom = 112,
            apexTargetName = "WarGreymon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "wargreymon",
                    targetSpeciesName = "WarGreymon",
                    targetStage = "Mega",
                    targetAttribute = "Vaccine",
                    minAttack = 210,
                    minDefense = 180,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Dramon Killer Mega warrior wielding Terra Force (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "rizegreymon",
            speciesName = "RizeGreymon",
            stage = "Ultimate",
            attribute = "Vaccine",
            titleTag = "Solar Cannon • Ultimate Class",
            signatureMove = "Trident Revolver",
            primaryColorHex = 0xFFF59E0B,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 172,
            baseDefense = 136,
            baseSpeed = 142,
            baseWisdom = 118,
            apexTargetName = "WarGreymon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "wargreymon",
                    targetSpeciesName = "WarGreymon",
                    targetStage = "Mega",
                    targetAttribute = "Vaccine",
                    minAttack = 210,
                    minSpeed = 175,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Ultimate dragon knight clad in Chrome Digizoid (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "weregarurumon",
            speciesName = "WereGarurumon",
            stage = "Ultimate",
            attribute = "Vaccine",
            titleTag = "Assault Striker • Ultimate Class",
            signatureMove = "Wolf Claw",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFF8B5CF6,
            baseAttack = 158,
            baseDefense = 134,
            baseSpeed = 174,
            baseWisdom = 124,
            apexTargetName = "MetalGarurumon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgarurumon",
                    targetSpeciesName = "MetalGarurumon",
                    targetStage = "Mega",
                    targetAttribute = "Data",
                    minSpeed = 215,
                    minAttack = 185,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Absolute-zero cybernetic wolf firing Metal Wolf Claw (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "magnaangemon",
            speciesName = "MagnaAngemon",
            stage = "Ultimate",
            attribute = "Vaccine",
            titleTag = "Gatekeeper • Ultimate Class",
            signatureMove = "Gate of Destiny",
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFFA855F7,
            baseAttack = 146,
            baseDefense = 142,
            baseSpeed = 152,
            baseWisdom = 182,
            apexTargetName = "Seraphimon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "seraphimon",
                    targetSpeciesName = "Seraphimon",
                    targetStage = "Mega",
                    targetAttribute = "Vaccine",
                    minWisdom = 220,
                    minBond = 85,
                    requiredLevel = 65,
                    branchDescription = "Ten-winged celestial sovereign casting Strike of the Seven Stars (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "wargrowlmon",
            speciesName = "WarGrowlmon",
            stage = "Ultimate",
            attribute = "Virus",
            titleTag = "Atomic Buster • Ultimate Class",
            signatureMove = "Atomic Blaster",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 178,
            baseDefense = 148,
            baseSpeed = 136,
            baseWisdom = 118,
            apexTargetName = "Gallantmon",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "gallantmon",
                    targetSpeciesName = "Gallantmon",
                    targetStage = "Mega",
                    targetAttribute = "Virus",
                    minAttack = 215,
                    minDefense = 180,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Royal Knight wielding Sacred Lance Gram and Shield Aegis (Requires LVL 65+)."
                )
            )
        ),

        // MEGAS (Can evolve into the 6 Rare Level-100 Monsters ONLY when THIS specific monster reaches Level 100)
        DigimonSpeciesSpec(
            speciesId = "wargreymon",
            speciesName = "WarGreymon",
            stage = "Mega",
            attribute = "Vaccine",
            titleTag = "Dragon Sovereign • Mega Apex",
            signatureMove = "Terra Force",
            primaryColorHex = 0xFFFFB800,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 260,
            baseDefense = 230,
            baseSpeed = 210,
            baseWisdom = 185,
            apexTargetName = "Omegamon X (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),
        DigimonSpeciesSpec(
            speciesId = "metalgarurumon",
            speciesName = "MetalGarurumon",
            stage = "Mega",
            attribute = "Data",
            titleTag = "Cryo Artillery • Mega Apex",
            signatureMove = "Metal Wolf Claw",
            primaryColorHex = 0xFF00F0FF,
            secondaryColorHex = 0xFF3B82F6,
            baseAttack = 235,
            baseDefense = 215,
            baseSpeed = 268,
            baseWisdom = 205,
            apexTargetName = "Omegamon X (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),
        DigimonSpeciesSpec(
            speciesId = "seraphimon",
            speciesName = "Seraphimon",
            stage = "Mega",
            attribute = "Vaccine",
            titleTag = "Celestial Apex • Mega Apex",
            signatureMove = "Seven Heavens",
            primaryColorHex = 0xFFFDE047,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 220,
            baseDefense = 225,
            baseSpeed = 230,
            baseWisdom = 275,
            apexTargetName = "Susanoomon (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),
        DigimonSpeciesSpec(
            speciesId = "gallantmon",
            speciesName = "Gallantmon",
            stage = "Mega",
            attribute = "Virus",
            titleTag = "Royal Knight • Mega Apex",
            signatureMove = "Lightning Joust",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFFFB800,
            baseAttack = 268,
            baseDefense = 238,
            baseSpeed = 225,
            baseWisdom = 200,
            apexTargetName = "Gallantmon Crimson (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),

        // 6 NEW RARE LEVEL-100 MYTHIC MONSTERS (Only obtainable by evolving a monster that has personally reached Level 100)
        DigimonSpeciesSpec(
            speciesId = "omegamon_x",
            speciesName = "Omegamon X",
            stage = "Mega",
            attribute = "Vaccine",
            titleTag = "Rare Lvl 100 Mythic • Royal Fusion",
            signatureMove = "All Delete",
            primaryColorHex = 0xFF00F0FF,
            secondaryColorHex = 0xFFFFD700,
            baseAttack = 350,
            baseDefense = 320,
            baseSpeed = 335,
            baseWisdom = 315,
            apexTargetName = "Omegamon X",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "alphamon_ouryuken",
            speciesName = "Alphamon Ouryuken",
            stage = "Mega",
            attribute = "Vaccine",
            titleTag = "Rare Lvl 100 Mythic • Chrono Sovereign",
            signatureMove = "Digitalize of Soul",
            primaryColorHex = 0xFFFFD700,
            secondaryColorHex = 0xFFA855F7,
            baseAttack = 345,
            baseDefense = 340,
            baseSpeed = 315,
            baseWisdom = 335,
            apexTargetName = "Alphamon Ouryuken",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "imperialdramon_pm",
            speciesName = "Imperialdramon PM",
            stage = "Mega",
            attribute = "Vaccine",
            titleTag = "Rare Lvl 100 Mythic • Paladin Founder",
            signatureMove = "Omni Sword",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFFFDE047,
            baseAttack = 355,
            baseDefense = 315,
            baseSpeed = 340,
            baseWisdom = 310,
            apexTargetName = "Imperialdramon PM",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "susanoomon_zero",
            speciesName = "Susanoomon",
            stage = "Mega",
            attribute = "Free",
            titleTag = "Rare Lvl 100 Mythic • Celestial Emperor",
            signatureMove = "Celestial Blade",
            primaryColorHex = 0xFFFFB800,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 350,
            baseDefense = 325,
            baseSpeed = 320,
            baseWisdom = 355,
            apexTargetName = "Susanoomon",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "gallantmon_crimson",
            speciesName = "Gallantmon Crimson",
            stage = "Mega",
            attribute = "Virus",
            titleTag = "Rare Lvl 100 Mythic • Crimson Quo Vadis",
            signatureMove = "Quo Vadis",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFFFD700,
            baseAttack = 368,
            baseDefense = 318,
            baseSpeed = 335,
            baseWisdom = 310,
            apexTargetName = "Gallantmon Crimson",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "armageddemon_core",
            speciesName = "Armageddemon",
            stage = "Mega",
            attribute = "Free",
            titleTag = "Rare Lvl 100 Mythic • Apocalypse Titan",
            signatureMove = "Destiny Destroyer",
            primaryColorHex = 0xFFA855F7,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 380,
            baseDefense = 350,
            baseSpeed = 295,
            baseWisdom = 300,
            apexTargetName = "Armageddemon",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        )
    ) + DigiWorldExpansionCatalog.extra71SpeciesSpecs).associateBy { it.speciesId }

    val allSpeciesSpecs: List<DigimonSpeciesSpec>
        get() = allSpecies.values.toList()

    val wildTamableSpecs: List<DigimonSpeciesSpec>
        get() = allSpeciesSpecs.filter { !it.isRareLevel100Spec }

    val allExpeditions: List<ExpeditionSpec>
        get() = DigiWorldExpansionCatalog.allExpeditions

    fun getExpedition(expeditionId: String): ExpeditionSpec? =
        DigiWorldExpansionCatalog.getExpedition(expeditionId)

    fun getSkillTree(speciesId: String): MonsterSkillTreeSpec =
        DigiWorldExpansionCatalog.buildSkillTreeForSpecies(getSpec(speciesId))

    fun getSkillTreeForSpecies(speciesId: String): MonsterSkillTreeSpec =
        getSkillTree(speciesId)

    fun getSpec(speciesId: String): DigimonSpeciesSpec {
        return allSpecies[speciesId] ?: allSpecies.getValue("agumon")
    }

    fun getMonsterImageRes(speciesId: String): Int {
        val id = speciesId.lowercase()
        return when {
            id in level100RareSpeciesIds ->
                com.example.R.drawable.img_monster_mythic_apex
            id.contains("garuru") || id.contains("gabu") || id.contains("doru") || id.contains("wolf") || id.contains("looga") || id.contains("jelly") ->
                com.example.R.drawable.img_monster_frost_wolf
            id.contains("ange") || id.contains("seraph") || id.contains("pata") || id.contains("gargo") || id.contains("biyo") || id.contains("hawk") || id.contains("bird") ->
                com.example.R.drawable.img_monster_celestial_wing
            id.contains("guil") || id.contains("growl") || id.contains("gallant") || id.contains("imp") || id.contains("myotis") || id.contains("gamma") ->
                com.example.R.drawable.img_monster_crimson_dragon
            else ->
                com.example.R.drawable.img_monster_flame_dino
        }
    }

    private val npcIdPrefixes = listOf(
        "npc_",
        "bot_",
        "rival_",
        "seed_",
        "demo_",
        "ai_",
        "mock_",
        "system_",
        "sample_",
        "test_npc"
    )

    private val knownNpcIdentifiers = setOf(
        "rival_taichi",
        "rival_yamato",
        "rival_rika",
        "rival_takato",
        "rival_hikari",
        "kamiya_taichi",
        "ishida_matt",
        "nonaka_rika",
        "matsuki_takato",
        "tamer_kari",
        "omni_tamer_kai",
        "cyber_queen_rika",
        "crest_guardian_matt",
        "tamer_taichi",
        "tamer_matt",
        "tamer_rika",
        "tamer_takato"
    )

    fun isNpcOrBotIdentifier(raw: String): Boolean {
        val clean = raw.trim().lowercase()
        if (clean.isEmpty()) return false
        if (clean in knownNpcIdentifiers) return true
        if (npcIdPrefixes.any { clean.startsWith(it) || clean.contains(":$it") || clean.contains("_$it") }) {
            return true
        }
        return clean.contains("[npc]") || clean.contains("(npc)") || clean.contains("npc tamer")
    }

    fun isRookieStarterAllowed(speciesId: String): Boolean {
        val spec = allSpecies[speciesId] ?: return false
        return spec.stage == "Rookie" && !spec.isRareLevel100Spec
    }

    fun requireRookieStarterSpec(speciesId: String): DigimonSpeciesSpec {
        val candidate = allSpecies[speciesId]
        return if (candidate != null && candidate.stage == "Rookie" && !candidate.isRareLevel100Spec) {
            candidate
        } else {
            allSpecies.getValue("agumon")
        }
    }

    val starterSpecs: List<DigimonSpeciesSpec>
        get() = listOf(
            getSpec("agumon"),
            getSpec("gabumon"),
            getSpec("patamon"),
            getSpec("guilmon"),
            getSpec("veemon"),
            getSpec("dorumon")
        ).filter { it.stage == "Rookie" && !it.isRareLevel100Spec }

    val allRookieSpecs: List<DigimonSpeciesSpec>
        get() {
            val coreIds = listOf("agumon", "gabumon", "patamon", "guilmon", "veemon", "dorumon")
            val core = coreIds.mapNotNull { allSpecies[it] }.filter { it.stage == "Rookie" && !it.isRareLevel100Spec }
            val extra = allSpeciesSpecs.filter { it.stage == "Rookie" && !it.isRareLevel100Spec && it.speciesId !in coreIds }
            return (core + extra).filter { it.stage == "Rookie" && !it.isRareLevel100Spec }
        }

    val level100RareSpecs: List<DigimonSpeciesSpec>
        get() = level100RareSpeciesIds.map { getSpec(it) }
}
