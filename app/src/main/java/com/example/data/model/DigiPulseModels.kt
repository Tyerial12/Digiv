package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class DigimonCreature(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nickname: String = "Sproutling",
    val speciesId: String = "patamon",
    val speciesName: String = "Sproutling",
    val stage: String = "Rookie",
    val attribute: String = "Flora",
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
    val offlineTrainingPausedRemainingMs: Long = 0L,
    val offlineTrainingPausedCycleElapsedMs: Long = 0L,
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
                !it.startsWith("rc:") &&
                !it.startsWith("rg:")
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
            ?.getOrNull(0)
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

    fun persistedOfflineTrainingPausedRemainingMs(): Long {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("ot:") }
            ?.removePrefix("ot:")
            ?.split(":")
            ?.getOrNull(2)
            ?.toLongOrNull()
            ?.let { it * 1000L } ?: 0L
        return maxOf(offlineTrainingPausedRemainingMs, fromForms)
    }

    fun persistedOfflineTrainingPausedCycleElapsedMs(): Long {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("ot:") }
            ?.removePrefix("ot:")
            ?.split(":")
            ?.getOrNull(3)
            ?.toLongOrNull()
            ?.let { it * 1000L } ?: 0L
        return maxOf(offlineTrainingPausedCycleElapsedMs, fromForms)
    }

    fun persistedRecoveryEndsAtMs(): Long {
        val fromForms = unlockedForms
            .firstOrNull { it.startsWith("rc:") }
            ?.removePrefix("rc:")
            ?.toLongOrNull()
            ?.let { it * 1000L } ?: 0L
        return maxOf(recoveryEndsAtMs, fromForms)
    }

    fun isOfflineTrainingPaused(): Boolean =
        persistedOfflineTrainingPausedRemainingMs() > 0L && persistedOfflineTrainingEndMs() <= 0L

    fun isInOfflineTraining(nowMs: Long = System.currentTimeMillis()): Boolean =
        persistedOfflineTrainingEndMs() > nowMs && !isOfflineTrainingPaused()

    fun hasOfflineTrainingSession(nowMs: Long = System.currentTimeMillis()): Boolean =
        isInOfflineTraining(nowMs) || isOfflineTrainingPaused()

    fun remainingOfflineTrainingSec(nowMs: Long = System.currentTimeMillis()): Long {
        return if (isOfflineTrainingPaused()) {
            (persistedOfflineTrainingPausedRemainingMs() / 1000L).coerceAtLeast(0L)
        } else {
            ((persistedOfflineTrainingEndMs() - nowMs) / 1000L).coerceAtLeast(0L)
        }
    }

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

    private fun persistedRpgParts(): List<String>? {
        return unlockedForms
            .firstOrNull { it.startsWith("rg:") }
            ?.removePrefix("rg:")
            ?.split(":")
    }

    fun companionNature(): CompanionNature {
        val code = persistedRpgParts()?.getOrNull(0)
        if (!code.isNullOrBlank()) {
            return CompanionNature.fromCode(code)
        }
        val entries = CompanionNature.entries
        val seed = kotlin.math.abs((id.ifBlank { speciesId }).hashCode())
        return entries[seed % entries.size]
    }

    fun heldRelic(): HeldRelicItem {
        val code = persistedRpgParts()?.getOrNull(1)
        if (!code.isNullOrBlank()) {
            return HeldRelicItem.fromCode(code)
        }
        val attr = DigimonCatalog.getSpec(speciesId).attribute.uppercase()
        return when {
            attr.contains("PYRO") || attr.contains("FIRE") -> HeldRelicItem.SEARING_CHARCOAL
            attr.contains("AQUA") || attr.contains("WATER") || attr.contains("ASTRAL") -> HeldRelicItem.MYSTIC_DEWDROP
            attr.contains("FLORA") || attr.contains("TERRA") -> HeldRelicItem.VERDANT_SEED
            attr.contains("STORM") || attr.contains("GALE") -> HeldRelicItem.STORM_MAGNET
            else -> HeldRelicItem.LEFTOVER_AMBROSIA
        }
    }

    fun isChromaticShiny(): Boolean {
        return persistedRpgParts()?.getOrNull(2) == "1"
    }

    fun earnedSanctumBadgesMask(): Int {
        return persistedRpgParts()?.getOrNull(3)?.toIntOrNull() ?: 0
    }

    fun hasSanctumBadge(badgeNumber: Int): Boolean {
        if (badgeNumber !in 1..16) return false
        val bit = 1 shl (badgeNumber - 1)
        return (earnedSanctumBadgesMask() and bit) != 0
    }

    fun earnedSanctumBadgesCount(): Int {
        return Integer.bitCount(earnedSanctumBadgesMask() and 0xFFFF)
    }

    fun effectiveAttack(): Int {
        val natureMult = companionNature().atkMult
        val relicMult = 1f + (heldRelic().atkBonusPercent / 100f)
        val shinyMult = if (isChromaticShiny()) 1.15f else 1.0f
        return (attack * natureMult * relicMult * shinyMult).toInt().coerceAtLeast(1)
    }

    fun effectiveDefense(): Int {
        val natureMult = companionNature().defMult
        val relicMult = 1f + (heldRelic().defBonusPercent / 100f)
        val shinyMult = if (isChromaticShiny()) 1.15f else 1.0f
        return (defense * natureMult * relicMult * shinyMult).toInt().coerceAtLeast(1)
    }

    fun effectiveSpeed(): Int {
        val natureMult = companionNature().spdMult
        val relicMult = 1f + (heldRelic().spdBonusPercent / 100f)
        val shinyMult = if (isChromaticShiny()) 1.15f else 1.0f
        return (speed * natureMult * relicMult * shinyMult).toInt().coerceAtLeast(1)
    }

    fun effectiveWisdom(): Int {
        val natureMult = companionNature().wisMult
        val relicMult = 1f + (heldRelic().wisBonusPercent / 100f)
        val shinyMult = if (isChromaticShiny()) 1.15f else 1.0f
        return (wisdom * natureMult * relicMult * shinyMult).toInt().coerceAtLeast(1)
    }

    fun withUpdatedMetadataInForms(
        newSkillNodes: Collection<String> = allUnlockedSkillNodeIds(),
        newBonusStats: Int = persistedNonTrainingBonusStats(),
        newExpeditionsCount: Int = persistedCompletedExpeditions(),
        newOfflineTrainEndMs: Long = persistedOfflineTrainingEndMs(),
        newOfflineTrainTickMs: Long = persistedOfflineTrainingLastTickMs(),
        newOfflineTrainPausedRemMs: Long = persistedOfflineTrainingPausedRemainingMs(),
        newOfflineTrainPausedCycleMs: Long = persistedOfflineTrainingPausedCycleElapsedMs(),
        newRecoveryEndsMs: Long = persistedRecoveryEndsAtMs(),
        newNatureCode: String = companionNature().code,
        newRelicCode: String = heldRelic().code,
        newIsShiny: Boolean = isChromaticShiny(),
        newBadgesMask: Int = earnedSanctumBadgesMask(),
        targetSpeciesOverride: String = speciesId
    ): List<String> {
        val baseEvolutionForms = unlockedEvolutionFormIds().take(4).map { it.take(40) }
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
            if (newOfflineTrainEndMs > 0L || newOfflineTrainPausedRemMs > 0L) {
                val endSec = newOfflineTrainEndMs / 1000L
                val tickSec = (newOfflineTrainTickMs.takeIf { it > 0L } ?: newOfflineTrainEndMs) / 1000L
                val pausedRemSec = newOfflineTrainPausedRemMs / 1000L
                val pausedCycleSec = newOfflineTrainPausedCycleMs / 1000L
                add("ot:$endSec:$tickSec:$pausedRemSec:$pausedCycleSec".take(40))
            }
            if (newRecoveryEndsMs > 0L) {
                val recSec = newRecoveryEndsMs / 1000L
                add("rc:$recSec".take(40))
            }
            val shinyFlag = if (newIsShiny) 1 else 0
            add("rg:$newNatureCode:$newRelicCode:$shinyFlag:$newBadgesMask".take(40))
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
        val effectiveStats = effectiveAttack() + effectiveDefense() + effectiveSpeed() + effectiveWisdom()
        val conditionBonus = ((health + stamina + hunger) / 3) + bondLevel
        val badgeBonus = earnedSanctumBadgesCount() * 25
        return (effectiveStats * stageMultiplier) + (conditionBonus * 3) + badgeBonus
    }

    fun moodLabel(): String = when {
        isRecoveringFrom1Hp() -> "1 HP RECOVERING (${formattedRecoveryRemaining()})"
        isOfflineTrainingPaused() -> "STRIDEWALKER PAUSED (${formattedOfflineTrainingRemaining()})"
        isInOfflineTraining() -> "24H STRIDEWALKER (${formattedOfflineTrainingRemaining()})"
        health < 35 -> "CRITICAL DAMAGE"
        hunger < 25 -> "NEEDS NECTAR!"
        isResting -> "SANCTUARY REST (${stamina}%/100%)"
        stamina <= 20 -> "NEEDS SLEEP!"
        isOnExpedition() -> "ON EXPEDITION"
        isRareLevel100Form() -> "LVL 100 SOVEREIGN"
        canEvolveAnyBranch() -> "ASCENSION READY!"
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

enum class AcornOrbType(
    val orbTypeId: String,
    val displayName: String,
    val shortLabel: String,
    val baseCaptureBonusPercent: Int,
    val specialtyBonusPercent: Int,
    val costCoins: Int,
    val craftGildedAcornsCost: Int,
    val craftRelicShardsCost: Int,
    val description: String,
    val colorHex: Long
) {
    STANDARD_ACORN_ORB(
        orbTypeId = "standard_acorn_orb",
        displayName = "Acorn Orb",
        shortLabel = "Acorn",
        baseCaptureBonusPercent = 0,
        specialtyBonusPercent = 5,
        costCoins = 2,
        craftGildedAcornsCost = 1,
        craftRelicShardsCost = 0,
        description = "Master Orin's classic handcrafted orb (2 Coins or 1 Gilded Acorn).",
        colorHex = 0xFFFFB300
    ),
    CRIMSON_FAST_ORB(
        orbTypeId = "crimson_fast_orb",
        displayName = "Crimson Fast Orb",
        shortLabel = "Fast (+18%)",
        baseCaptureBonusPercent = 12,
        specialtyBonusPercent = 18,
        costCoins = 5,
        craftGildedAcornsCost = 2,
        craftRelicShardsCost = 0,
        description = "+18% Capture Rate vs Gale, Storm, Pyro, or swift wild companions.",
        colorHex = 0xFFFF5252
    ),
    COBALT_LURE_ORB(
        orbTypeId = "cobalt_lure_orb",
        displayName = "Cobalt Lure Orb",
        shortLabel = "Lure (+18%)",
        baseCaptureBonusPercent = 12,
        specialtyBonusPercent = 18,
        costCoins = 5,
        craftGildedAcornsCost = 2,
        craftRelicShardsCost = 0,
        description = "+18% Capture Rate vs Aqua, Frost, Flora, or Astral wild companions.",
        colorHex = 0xFF00E5FF
    ),
    AUREATE_HEAVY_ORB(
        orbTypeId = "aureate_heavy_orb",
        displayName = "Aureate Heavy Orb",
        shortLabel = "Heavy (+20%)",
        baseCaptureBonusPercent = 14,
        specialtyBonusPercent = 20,
        costCoins = 6,
        craftGildedAcornsCost = 3,
        craftRelicShardsCost = 0,
        description = "+20% Capture Rate vs Stage 2 (Ultimate), Ascended (Mega), Terra, or Metal companions.",
        colorHex = 0xFFFFD54F
    ),
    VERDANT_FRIEND_ORB(
        orbTypeId = "verdant_friend_orb",
        displayName = "Verdant Friend Orb",
        shortLabel = "Friend (+15%)",
        baseCaptureBonusPercent = 15,
        specialtyBonusPercent = 15,
        costCoins = 5,
        craftGildedAcornsCost = 2,
        craftRelicShardsCost = 0,
        description = "+15% Capture Rate on all companions & grants +25 Starting Bond on capture!",
        colorHex = 0xFF00E676
    ),
    LUNAR_MOON_ORB(
        orbTypeId = "lunar_moon_orb",
        displayName = "Lunar Moon Orb",
        shortLabel = "Lunar (+25%)",
        baseCaptureBonusPercent = 18,
        specialtyBonusPercent = 25,
        costCoins = 8,
        craftGildedAcornsCost = 2,
        craftRelicShardsCost = 1,
        description = "+25% Capture Rate vs Rare, Ultra Rare, Sovereign, or ✨ Starlight Chromatic companions!",
        colorHex = 0xFFA855F7
    );

    fun effectiveCaptureBonusFor(encounter: WildEncounterState): Int {
        val attr = encounter.attribute.uppercase()
        return when (this) {
            STANDARD_ACORN_ORB -> 0
            CRIMSON_FAST_ORB -> if (
                attr.contains("GALE") || attr.contains("STORM") || attr.contains("PYRO") || attr.contains("VOLT")
            ) specialtyBonusPercent else baseCaptureBonusPercent
            COBALT_LURE_ORB -> if (
                attr.contains("AQUA") || attr.contains("FROST") || attr.contains("FLORA") || attr.contains("ASTRAL")
            ) specialtyBonusPercent else baseCaptureBonusPercent
            AUREATE_HEAVY_ORB -> if (
                encounter.stage == "Ultimate" || encounter.stage == "Mega" || attr.contains("TERRA") || attr.contains("METAL")
            ) specialtyBonusPercent else baseCaptureBonusPercent
            VERDANT_FRIEND_ORB -> specialtyBonusPercent
            LUNAR_MOON_ORB -> if (
                encounter.isChromaticShiny ||
                encounter.rarityTier == WildRarityTier.RARE ||
                encounter.rarityTier == WildRarityTier.ULTRA_RARE ||
                encounter.rarityTier == WildRarityTier.LEGENDARY_APEX
            ) specialtyBonusPercent else baseCaptureBonusPercent
        }
    }

    companion object {
        fun fromId(raw: String): AcornOrbType =
            entries.firstOrNull { it.orbTypeId.equals(raw.trim(), ignoreCase = true) } ?: STANDARD_ACORN_ORB
    }
}

@Keep
@IgnoreExtraProperties
data class AcornOrbInventory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orbTypeId: String = AcornOrbType.STANDARD_ACORN_ORB.orbTypeId,
    val orbDisplayName: String = AcornOrbType.STANDARD_ACORN_ORB.displayName,
    val quantity: Int = 0,
    val captureBonusPercent: Int = 0,
    val gildedAcornCount: Int = 0,
    val relicShardCount: Int = 0,
    val lastSyncedMs: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedOrbType(): AcornOrbType = AcornOrbType.fromId(orbTypeId.ifBlank { id })
}

@Keep
@IgnoreExtraProperties
data class EmeraldReserveEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val speciesId: String = "patamon",
    val speciesName: String = "Sproutling",
    val nickname: String = "Sproutling",
    val stage: String = "Rookie",
    val elementAttribute: String = "Flora",
    val rarityTier: String = "COMMON",
    val capturedLevel: Int = 1,
    val attack: Int = 45,
    val defense: Int = 38,
    val speed: Int = 42,
    val wisdom: Int = 34,
    val bondLevel: Int = 20,
    val natureCode: String = "BRAVE",
    val heldRelicCode: String = "VERDANT_SEED",
    val chromaticShiny: Boolean = false,
    val orbUsedId: String = AcornOrbType.STANDARD_ACORN_ORB.orbTypeId,
    val biomeZone: String = "Emerald Reserve Safari",
    val inActiveParty: Boolean = false,
    val capturedAtMs: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRarityTier(): WildRarityTier =
        WildRarityTier.entries.firstOrNull { it.name == rarityTier } ?: WildRarityTier.COMMON

    fun resolvedNature(): CompanionNature = CompanionNature.fromCode(natureCode)

    fun resolvedRelic(): HeldRelicItem = HeldRelicItem.fromCode(heldRelicCode)

    fun resolvedOrbType(): AcornOrbType = AcornOrbType.fromId(orbUsedId)

    fun toDigimonCreature(): DigimonCreature {
        val spec = DigimonCatalog.getSpec(speciesId)
        val shinyFlag = if (chromaticShiny) 1 else 0
        val bonus = (attack + defense + speed + wisdom - 172).coerceAtLeast(0)
        val forms = buildList {
            add(spec.speciesId)
            if (bonus > 0) add("bn:$bonus")
            add("rg:${resolvedNature().code}:${resolvedRelic().code}:$shinyFlag:0")
        }
        return DigimonCreature(
            id = id.ifBlank { "reserve_${capturedAtMs}_${spec.speciesId}" },
            userId = userId,
            nickname = nickname.ifBlank { spec.speciesName },
            speciesId = spec.speciesId,
            speciesName = spec.speciesName,
            stage = stage.takeIf { it in listOf("Rookie", "Champion", "Ultimate", "Mega") } ?: spec.stage,
            attribute = spec.attribute,
            health = 100,
            stamina = 100,
            hunger = 90,
            evolutionProgress = 0,
            attack = attack,
            defense = defense,
            speed = speed,
            wisdom = wisdom,
            nonTrainingBonusStats = bonus,
            bondLevel = bondLevel.coerceIn(0, 100),
            careMistakes = 0,
            isResting = false,
            unlockedForms = forms,
            lastUpdatedMs = capturedAtMs
        )
    }
}

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
    val apexTargetName: String = "Ignisovereign",
    val isRareLevel100Spec: Boolean = false,
    val starterAbilityName: String = "",
    val starterAbilityDescription: String = "",
    val evolutionBranches: List<EvolutionBranchRequirement> = emptyList()
) {
    fun resolvedAbilityName(): String {
        if (starterAbilityName.isNotBlank()) return starterAbilityName
        return when (speciesId) {
            "patamon", "sproutling" -> "Overgrowth"
            "agumon", "cinderkit" -> "Kindle Core"
            "gabumon", "riptide" -> "Rushing Surge"
            "tentomon", "voltkit" -> "Static Pulse"
            "renamon", "vulpineon" -> "Resonant Bond"
            "veemon", "woolspark" -> "Charged Fleece"
            "gammamon", "ursaflare" -> "Nimble Stride"
            "guilmon", "duskfang" -> "Searing Fang"
            "dorumon", "cragscale" -> "Bedrock Grit"
            "terriermon", "wyrmlet" -> "Mystic Scales"
            else -> "$attribute Crest Aura"
        }
    }

    fun resolvedAbilityDescription(): String {
        if (starterAbilityDescription.isNotBlank()) return starterAbilityDescription
        return when (speciesId) {
            "patamon", "sproutling" -> "+15% Flora Power & +4 Bonus HP Recovery on Sanctuary Rest"
            "agumon", "cinderkit" -> "+15% Pyro Attack Power & +2 Bonus ATK Synergy on Power Courses"
            "gabumon", "riptide" -> "+15% Aqua Fang Speed & +10% Emerald Reserve Acorn Orb Resonance"
            "tentomon", "voltkit" -> "High Speed (60 SPD) & +18% Critical Surge on Storm strikes"
            "renamon", "vulpineon" -> "+15% Bond Evolution Sync towards Solareon & Lunareon"
            "veemon", "woolspark" -> "Charged Fleece reflects contact shock & boosts DEF/WIS Synergy"
            "gammamon", "ursaflare" -> "+15% Dual Attack & Defense Power in Sanctuary & Reserve trials"
            "guilmon", "duskfang" -> "+20% Critical Strike Damage in Wild Battles & High Base ATK (58)"
            "dorumon", "cragscale" -> "+15% Stone Armor Defense & Reduced Stamina Drain on Courses"
            "terriermon", "wyrmlet" -> "Balanced Dragon Aura granting +15% All-Around Stamina Efficiency"
            else -> "Amplifies $signatureMove damage and $attribute elemental synchronization."
        }
    }

    fun totalBaseStats(): Int = baseAttack + baseDefense + baseSpeed + baseWisdom

    fun wildRarityTier(): WildRarityTier {
        val statSum = totalBaseStats()
        return when {
            isRareLevel100Spec || (stage == "Mega" && statSum >= 500) -> WildRarityTier.LEGENDARY_APEX
            stage == "Mega" -> WildRarityTier.ULTRA_RARE
            stage == "Ultimate" -> WildRarityTier.RARE
            stage == "Champion" -> WildRarityTier.UNCOMMON
            else -> WildRarityTier.COMMON
        }
    }
}

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
        shortTitle = "SOLAR BREAKER",
        subtitle = "Aetherlon Power Course: Strike when the Sunheart ring locks inside the golden crest zone.",
        staminaCost = 18,
        hungerCost = 12,
        primaryStatLabel = "ATK"
    ),
    SPEED(
        activityName = "Speed Drill",
        shortTitle = "GALE HURDLES",
        subtitle = "Aetherlon Speed Course: Vault high-velocity wind hurdles before the cadence decays.",
        staminaCost = 16,
        hungerCost = 10,
        primaryStatLabel = "SPD"
    ),
    DEFENSE(
        activityName = "Defense Drill",
        shortTitle = "IRON AEGIS",
        subtitle = "Aetherlon Stamina Course: Brace the SoulCrest barrier ring under heavy kinetic impact.",
        staminaCost = 18,
        hungerCost = 10,
        primaryStatLabel = "DEF"
    ),
    WISDOM(
        activityName = "Wisdom Drill",
        shortTitle = "ARCANE RELAY",
        subtitle = "Aetherlon Skill Course: Harmonize dual-realm runes to boost Crest evolution sync.",
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
        displayName = "Spiced Nectar Bar (FREE)",
        description = "FREE Aurelia Treat: +28 Hunger, +4 HP, 0 Stamina, 0 Stats (Use Sanctuary Rest for Stamina)",
        costCoins = 0,
        hungerRestore = 28,
        healthRestore = 4,
        staminaRestore = 0,
        statBoostSummary = "FREE • +28 HUN, +4 HP (0 Stats, 0 STA)"
    ),
    DIGI_MEAT(
        displayName = "Cinder Hearth Roast (5 Coins)",
        description = "Costs 5 Crest Coins: +30 Hunger, +2 ATK, +1 Bond (0 Stamina)",
        costCoins = 5,
        hungerRestore = 30,
        healthRestore = 4,
        staminaRestore = 0,
        atkBonus = 2,
        statBoostSummary = "5 COINS • +2 ATK, +30 HUN, +1 BOND"
    ),
    PROTEIN_CHIP(
        displayName = "Meadow Cream Milk (5 Coins)",
        description = "Costs 5 Crest Coins: +30 Hunger, +2 DEF, +1 Bond (0 Stamina)",
        costCoins = 5,
        hungerRestore = 30,
        healthRestore = 4,
        staminaRestore = 0,
        defBonus = 2,
        statBoostSummary = "5 COINS • +2 DEF, +30 HUN, +1 BOND"
    ),
    STAMINA_BERRY(
        displayName = "Gale Sweetberry (5 Coins)",
        description = "Costs 5 Crest Coins: +28 Hunger, +2 SPD, +1 Stamina, +1 Bond",
        costCoins = 5,
        hungerRestore = 28,
        healthRestore = 2,
        staminaRestore = 1,
        spdBonus = 2,
        statBoostSummary = "5 COINS • +2 SPD, +28 HUN, +1 STA"
    ),
    GOLDEN_ACORN(
        displayName = "Starlight Ambrosia (5 Coins)",
        description = "Costs 5 Crest Coins: +32 Hunger, +2 WIS, +8 HP, +2 Bond (0 Stamina)",
        costCoins = 5,
        hungerRestore = 32,
        healthRestore = 8,
        staminaRestore = 0,
        wisBonus = 2,
        statBoostSummary = "5 COINS • +2 WIS, +32 HUN, +8 HP"
    ),
    ROYAL_FEAST(
        displayName = "Sunheart Sacred Feast (5 Coins)",
        description = "Costs 5 Crest Coins: +38 Hunger, +1 ALL STATS (ATK/DEF/SPD/WIS), +1 Stamina",
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
    const val DIGI_COIN_INTERVAL_SEC = 300 // 5 minutes per +5 Crest Coins
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
            targetSpeciesName = "Solphoenix",
            targetStage = "Mega",
            targetAttribute = "Pyro",
            minAttack = 250,
            minDefense = 220,
            minSpeed = 210,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "SOVEREIGN LVL 100 ASCENSION: Sunheart Guardian of the Sky Bell Tower wielding Sacred Sunfire. Requires this specific companion to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "alphamon_ouryuken",
            targetSpeciesName = "Thalassor",
            targetStage = "Mega",
            targetAttribute = "Aqua",
            minAttack = 240,
            minDefense = 230,
            minWisdom = 210,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "SOVEREIGN LVL 100 ASCENSION: SoulCrest Lord of the Whirlpool Trench wielding Abyssal Gale. Requires this specific companion to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "imperialdramon_pm",
            targetSpeciesName = "Borealis",
            targetStage = "Mega",
            targetAttribute = "Frost",
            minAttack = 250,
            minSpeed = 225,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "SOVEREIGN LVL 100 ASCENSION: Northern Gale Sovereign purifying the realm with Aurora Crystal. Requires this specific companion to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "susanoomon_zero",
            targetSpeciesName = "Tempestigon",
            targetStage = "Mega",
            targetAttribute = "Storm",
            minAttack = 240,
            minWisdom = 240,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "SOVEREIGN LVL 100 ASCENSION: Stormcloud Sovereign commanding Thunderbolt Roar across Aurelia. Requires this specific companion to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "gallantmon_crimson",
            targetSpeciesName = "Ignisaur",
            targetStage = "Mega",
            targetAttribute = "Pyro",
            minAttack = 255,
            minDefense = 220,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "SOVEREIGN LVL 100 ASCENSION: Volcanic Caldera Sovereign unleashing Magma Eruption. Requires this specific companion to reach Level 100."
        ),
        EvolutionBranchRequirement(
            targetSpeciesId = "armageddemon_core",
            targetSpeciesName = "Aetherion",
            targetStage = "Mega",
            targetAttribute = "Astral",
            minAttack = 260,
            minDefense = 235,
            minBond = 85,
            requiredLevel = 100,
            isRareLevel100Branch = true,
            branchDescription = "SOVEREIGN LVL 100 ASCENSION: Silver Peak Astral Sovereign wielding Chrono Singularity. Requires this specific companion to reach Level 100."
        )
    )

    val allSpecies: Map<String, DigimonSpeciesSpec> = (listOf(
        // BASE-STAGE STARTERS (Core Aurelia Lineages)
        DigimonSpeciesSpec(
            speciesId = "agumon",
            speciesName = "Cinderkit",
            stage = "Rookie",
            attribute = "Pyro",
            titleTag = "Hearth Quill • Aurelia Starter #02",
            signatureMove = "Blaze Wheel",
            primaryColorHex = 0xFFFF9800,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 52,
            baseDefense = 42,
            baseSpeed = 44,
            baseWisdom = 36,
            apexTargetName = "Ignisovereign",
            starterAbilityName = "Kindle Core",
            starterAbilityDescription = "+15% Pyro Attack Power & +2 Bonus ATK Synergy on Power Courses",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "greymon",
                    targetSpeciesName = "Pyroquill",
                    targetStage = "Champion",
                    targetAttribute = "Pyro",
                    minAttack = 65,
                    minDefense = 50,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Balanced Sunheart flame lineage requiring LVL 20+ and 100% Crest Sync."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "geogreymon",
                    targetSpeciesName = "Flareburst",
                    targetStage = "Champion",
                    targetAttribute = "Pyro",
                    minAttack = 72,
                    minSpeed = 58,
                    minBond = 40,
                    requiredLevel = 20,
                    branchDescription = "High-velocity volcanic striker path requiring LVL 20+."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "tyrannomon",
                    targetSpeciesName = "Magmasaur",
                    targetStage = "Champion",
                    targetAttribute = "Terra",
                    minDefense = 55,
                    minBond = 20,
                    requiredLevel = 20,
                    branchDescription = "Resilient magma-armored saurian path requiring LVL 20+ and DEF 55+."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "gabumon",
            speciesName = "Riptide",
            stage = "Rookie",
            attribute = "Aqua",
            titleTag = "Tidal Jaw • Aurelia Starter #03",
            signatureMove = "Tidal Fang",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFFA855F7,
            baseAttack = 46,
            baseDefense = 44,
            baseSpeed = 54,
            baseWisdom = 44,
            apexTargetName = "Titanorex",
            starterAbilityName = "Rushing Surge",
            starterAbilityDescription = "+15% Aqua Fang Speed & +10% Emerald Reserve Acorn Orb Resonance",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "garurumon",
                    targetSpeciesName = "Jawcurrent",
                    targetStage = "Champion",
                    targetAttribute = "Aqua",
                    minSpeed = 68,
                    minAttack = 58,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Silver-tide torrent predator unlocked via LVL 20+, Speed, and Attack courses."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "dorugamon",
                    targetSpeciesName = "Shellcrag",
                    targetStage = "Champion",
                    targetAttribute = "Terra",
                    minDefense = 62,
                    minWisdom = 56,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Armored reef-crag guardian requiring LVL 20+, Defense & Skill."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "patamon",
            speciesName = "Sproutling",
            stage = "Rookie",
            attribute = "Flora",
            titleTag = "Sunleaf Bud • Aurelia Starter #01",
            signatureMove = "Verdant Leaf",
            primaryColorHex = 0xFF10B981,
            secondaryColorHex = 0xFFFBBF24,
            baseAttack = 40,
            baseDefense = 46,
            baseSpeed = 50,
            baseWisdom = 56,
            apexTargetName = "Aetherdrake",
            starterAbilityName = "Overgrowth",
            starterAbilityDescription = "+15% Flora Power & +4 Bonus HP Recovery on Sanctuary Rest",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "angemon",
                    targetSpeciesName = "Blossomane",
                    targetStage = "Champion",
                    targetAttribute = "Flora",
                    minWisdom = 70,
                    minBond = 45,
                    requiredLevel = 20,
                    branchDescription = "Aromatic sun-blossom guardian requiring LVL 20+, Wisdom 70+, and Bond 45+."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "gargomon",
                    targetSpeciesName = "Skyserpent",
                    targetStage = "Champion",
                    targetAttribute = "Astral",
                    minAttack = 58,
                    minSpeed = 60,
                    minBond = 30,
                    requiredLevel = 20,
                    branchDescription = "Wind-gliding wyrm lineage unlocked with LVL 20+, Attack & Speed."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "guilmon",
            speciesName = "Duskfang",
            stage = "Rookie",
            attribute = "Shadow",
            titleTag = "Ember Hound • Starter #08",
            signatureMove = "Hellfire Howl",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFF59E0B,
            baseAttack = 58,
            baseDefense = 40,
            baseSpeed = 48,
            baseWisdom = 34,
            apexTargetName = "Netherlord",
            starterAbilityName = "Searing Fang",
            starterAbilityDescription = "+20% Critical Strike Damage in Wild Battles & High Base ATK (58)",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "growlmon",
                    targetSpeciesName = "Cerberux",
                    targetStage = "Champion",
                    targetAttribute = "Shadow",
                    minAttack = 74,
                    minDefense = 52,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Twin-flame shadow hound forged through LVL 20+, Power & Stamina courses."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "veemon",
            speciesName = "Woolspark",
            stage = "Rookie",
            attribute = "Storm",
            titleTag = "Beacon Fleece • Starter #06",
            signatureMove = "Fleece Shock",
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 50,
            baseDefense = 48,
            baseSpeed = 52,
            baseWisdom = 44,
            apexTargetName = "Ignisovereign",
            starterAbilityName = "Charged Fleece",
            starterAbilityDescription = "Charged Fleece reflects contact shock & boosts DEF/WIS Synergy",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "geogreymon",
                    targetSpeciesName = "Flareburst",
                    targetStage = "Champion",
                    targetAttribute = "Storm",
                    minAttack = 70,
                    minSpeed = 62,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "High-voltage beacon striker unlocked via LVL 20+, Speed & Power courses."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "dorugamon",
                    targetSpeciesName = "Shellcrag",
                    targetStage = "Champion",
                    targetAttribute = "Terra",
                    minDefense = 60,
                    minAttack = 64,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Armored thunder-bastion path requiring LVL 20+, Defense & Attack."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "dorumon",
            speciesName = "Cragscale",
            stage = "Rookie",
            attribute = "Terra",
            titleTag = "Mountain Larva • Starter #09",
            signatureMove = "Tectonic Crush",
            primaryColorHex = 0xFFA855F7,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 50,
            baseDefense = 56,
            baseSpeed = 44,
            baseWisdom = 46,
            apexTargetName = "Titanorex",
            starterAbilityName = "Bedrock Grit",
            starterAbilityDescription = "+15% Stone Armor Defense & Reduced Stamina Drain on Courses",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "dorugamon",
                    targetSpeciesName = "Shellcrag",
                    targetStage = "Champion",
                    targetAttribute = "Terra",
                    minDefense = 66,
                    minWisdom = 58,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Iron-shelled mountain carapace unlocked via LVL 20+, Defense & Skill."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "garurumon",
                    targetSpeciesName = "Jawcurrent",
                    targetStage = "Champion",
                    targetAttribute = "Aqua",
                    minSpeed = 64,
                    minAttack = 60,
                    minBond = 35,
                    requiredLevel = 20,
                    branchDescription = "Rushing canyon-fang evolution path requiring LVL 20+."
                )
            )
        ),

        // CHAMPIONS (STAGE 1 EVOLUTIONS)
        DigimonSpeciesSpec(
            speciesId = "greymon",
            speciesName = "Pyroquill",
            stage = "Champion",
            attribute = "Pyro",
            titleTag = "Volcanic Mantle • Champion Class",
            signatureMove = "Eruption Wheel",
            primaryColorHex = 0xFFFF7043,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 95,
            baseDefense = 84,
            baseSpeed = 72,
            baseWisdom = 66,
            apexTargetName = "Ignisovereign",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgreymon",
                    targetSpeciesName = "Vulcanis",
                    targetStage = "Ultimate",
                    targetAttribute = "Pyro",
                    minAttack = 130,
                    minDefense = 110,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Blazing caldera colossus with twin magma vents (Requires LVL 40+)."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "rizegreymon",
                    targetSpeciesName = "Solarflare",
                    targetStage = "Ultimate",
                    targetAttribute = "Pyro",
                    minAttack = 135,
                    minSpeed = 105,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Sunheart sky-artillery specialist (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "geogreymon",
            speciesName = "Flareburst",
            stage = "Champion",
            attribute = "Pyro",
            titleTag = "Sunburst Striker • Champion Class",
            signatureMove = "Corona Burst",
            primaryColorHex = 0xFFFF5722,
            secondaryColorHex = 0xFFFFD54F,
            baseAttack = 104,
            baseDefense = 78,
            baseSpeed = 86,
            baseWisdom = 64,
            apexTargetName = "Ignisovereign",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "rizegreymon",
                    targetSpeciesName = "Solarflare",
                    targetStage = "Ultimate",
                    targetAttribute = "Pyro",
                    minAttack = 135,
                    minSpeed = 105,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Supersonic solar-flame drake (Requires LVL 40+)."
                ),
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgreymon",
                    targetSpeciesName = "Vulcanis",
                    targetStage = "Ultimate",
                    targetAttribute = "Pyro",
                    minAttack = 130,
                    minDefense = 105,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Obsidian-armored volcanic evolution (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "tyrannomon",
            speciesName = "Magmasaur",
            stage = "Champion",
            attribute = "Terra",
            titleTag = "Caldera Dino • Champion Class",
            signatureMove = "Magma Breath",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFF10B981,
            baseAttack = 92,
            baseDefense = 94,
            baseSpeed = 68,
            baseWisdom = 62,
            apexTargetName = "Ignisovereign",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgreymon",
                    targetSpeciesName = "Vulcanis",
                    targetStage = "Ultimate",
                    targetAttribute = "Pyro",
                    minAttack = 128,
                    minDefense = 115,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Ascends into Ultimate volcanic sovereign (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "garurumon",
            speciesName = "Jawcurrent",
            stage = "Champion",
            attribute = "Aqua",
            titleTag = "Riptide Maw • Champion Class",
            signatureMove = "Maelstrom Crunch",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFFE2E8F0,
            baseAttack = 88,
            baseDefense = 78,
            baseSpeed = 102,
            baseWisdom = 76,
            apexTargetName = "Titanorex",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "weregarurumon",
                    targetSpeciesName = "Leviathor",
                    targetStage = "Ultimate",
                    targetAttribute = "Aqua",
                    minSpeed = 135,
                    minAttack = 120,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Abyssal tide warlord master of Hydro Cleave (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "dorugamon",
            speciesName = "Shellcrag",
            stage = "Champion",
            attribute = "Terra",
            titleTag = "Granite Carapace • Champion Class",
            signatureMove = "Iron Quake",
            primaryColorHex = 0xFFA855F7,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 94,
            baseDefense = 92,
            baseSpeed = 80,
            baseWisdom = 84,
            apexTargetName = "Titanorex",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "weregarurumon",
                    targetSpeciesName = "Leviathor",
                    targetStage = "Ultimate",
                    targetAttribute = "Aqua",
                    minAttack = 122,
                    minDefense = 115,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Heavy armored Ultimate juggernaut (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "angemon",
            speciesName = "Blossomane",
            stage = "Champion",
            attribute = "Flora",
            titleTag = "Sunpetal Guardian • Champion Class",
            signatureMove = "Solar Petal",
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFF10B981,
            baseAttack = 84,
            baseDefense = 82,
            baseSpeed = 90,
            baseWisdom = 106,
            apexTargetName = "Aetherdrake",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "magnaangemon",
                    targetSpeciesName = "Solarchid",
                    targetStage = "Ultimate",
                    targetAttribute = "Flora",
                    minWisdom = 140,
                    minBond = 65,
                    requiredLevel = 40,
                    branchDescription = "Radiant canopy sovereign wielding Sunbeam Sanctuary (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "gargomon",
            speciesName = "Skyserpent",
            stage = "Champion",
            attribute = "Astral",
            titleTag = "Starlight Wyrm • Champion Class",
            signatureMove = "Astral Twister",
            primaryColorHex = 0xFF10B981,
            secondaryColorHex = 0xFFFBBF24,
            baseAttack = 90,
            baseDefense = 86,
            baseSpeed = 88,
            baseWisdom = 80,
            apexTargetName = "Aetherdrake",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "magnaangemon",
                    targetSpeciesName = "Solarchid",
                    targetStage = "Ultimate",
                    targetAttribute = "Astral",
                    minAttack = 120,
                    minWisdom = 115,
                    minBond = 55,
                    requiredLevel = 40,
                    branchDescription = "Celestial Ultimate wyrm commander (Requires LVL 40+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "growlmon",
            speciesName = "Cerberux",
            stage = "Champion",
            attribute = "Shadow",
            titleTag = "Nether Hound • Champion Class",
            signatureMove = "Midnight Scorch",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFF97316,
            baseAttack = 106,
            baseDefense = 82,
            baseSpeed = 82,
            baseWisdom = 68,
            apexTargetName = "Netherlord",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "wargrowlmon",
                    targetSpeciesName = "Dreadhound",
                    targetStage = "Ultimate",
                    targetAttribute = "Shadow",
                    minAttack = 142,
                    minDefense = 110,
                    minBond = 60,
                    requiredLevel = 40,
                    branchDescription = "Armored eclipse hound with twin infernal fangs (Requires LVL 40+)."
                )
            )
        ),

        // ULTIMATES (STAGE 2 EVOLUTIONS)
        DigimonSpeciesSpec(
            speciesId = "metalgreymon",
            speciesName = "Vulcanis",
            stage = "Ultimate",
            attribute = "Pyro",
            titleTag = "Caldera Colossus • Ultimate Class",
            signatureMove = "Magma Overdrive",
            primaryColorHex = 0xFFFF5722,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 165,
            baseDefense = 148,
            baseSpeed = 124,
            baseWisdom = 112,
            apexTargetName = "Ignisovereign",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "wargreymon",
                    targetSpeciesName = "Ignisovereign",
                    targetStage = "Mega",
                    targetAttribute = "Pyro",
                    minAttack = 210,
                    minDefense = 180,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Sunheart Pyro Monarch wielding Solar Cataclysm (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "rizegreymon",
            speciesName = "Solarflare",
            stage = "Ultimate",
            attribute = "Pyro",
            titleTag = "Sunburst Drake • Ultimate Class",
            signatureMove = "Helios Cannon",
            primaryColorHex = 0xFFF59E0B,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 172,
            baseDefense = 136,
            baseSpeed = 142,
            baseWisdom = 118,
            apexTargetName = "Ignisovereign",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "wargreymon",
                    targetSpeciesName = "Ignisovereign",
                    targetStage = "Mega",
                    targetAttribute = "Pyro",
                    minAttack = 210,
                    minSpeed = 175,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Supreme Sunheart dragon monarch clad in golden mail (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "weregarurumon",
            speciesName = "Leviathor",
            stage = "Ultimate",
            attribute = "Aqua",
            titleTag = "Tidal Juggernaut • Ultimate Class",
            signatureMove = "Abyssal Fang",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFF8B5CF6,
            baseAttack = 158,
            baseDefense = 134,
            baseSpeed = 174,
            baseWisdom = 124,
            apexTargetName = "Titanorex",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "metalgarurumon",
                    targetSpeciesName = "Titanorex",
                    targetStage = "Mega",
                    targetAttribute = "Terra",
                    minSpeed = 215,
                    minAttack = 185,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Earth-shaking mountain & tide sovereign firing Tectonic Torrent (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "magnaangemon",
            speciesName = "Solarchid",
            stage = "Ultimate",
            attribute = "Flora",
            titleTag = "Sunbloom Sage • Ultimate Class",
            signatureMove = "Sanctuary Bloom",
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFFA855F7,
            baseAttack = 146,
            baseDefense = 142,
            baseSpeed = 152,
            baseWisdom = 182,
            apexTargetName = "Aetherdrake",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "seraphimon",
                    targetSpeciesName = "Aetherdrake",
                    targetStage = "Mega",
                    targetAttribute = "Astral",
                    minWisdom = 220,
                    minBond = 85,
                    requiredLevel = 65,
                    branchDescription = "Winged celestial dragon sovereign casting Starlight Tempest (Requires LVL 65+)."
                )
            )
        ),
        DigimonSpeciesSpec(
            speciesId = "wargrowlmon",
            speciesName = "Dreadhound",
            stage = "Ultimate",
            attribute = "Shadow",
            titleTag = "Eclipse Hound • Ultimate Class",
            signatureMove = "Nether Flare",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 178,
            baseDefense = 148,
            baseSpeed = 136,
            baseWisdom = 118,
            apexTargetName = "Netherlord",
            evolutionBranches = listOf(
                EvolutionBranchRequirement(
                    targetSpeciesId = "gallantmon",
                    targetSpeciesName = "Netherlord",
                    targetStage = "Mega",
                    targetAttribute = "Shadow",
                    minAttack = 215,
                    minDefense = 180,
                    minBond = 80,
                    requiredLevel = 65,
                    branchDescription = "Dusk-crowned sovereign wielding Crimson Eclipse Lance (Requires LVL 65+)."
                )
            )
        ),

        // MEGAS (Can ascend into the 6 Level-100 Sovereign Legendaries ONLY when THIS specific companion reaches Level 100)
        DigimonSpeciesSpec(
            speciesId = "wargreymon",
            speciesName = "Ignisovereign",
            stage = "Mega",
            attribute = "Pyro",
            titleTag = "Sunheart Monarch • Mega Apex",
            signatureMove = "Solar Cataclysm",
            primaryColorHex = 0xFFFFB800,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 260,
            baseDefense = 230,
            baseSpeed = 210,
            baseWisdom = 185,
            apexTargetName = "Solphoenix (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),
        DigimonSpeciesSpec(
            speciesId = "metalgarurumon",
            speciesName = "Titanorex",
            stage = "Mega",
            attribute = "Terra",
            titleTag = "SoulCrest Colossus • Mega Apex",
            signatureMove = "Tectonic Torrent",
            primaryColorHex = 0xFF00F0FF,
            secondaryColorHex = 0xFF3B82F6,
            baseAttack = 235,
            baseDefense = 215,
            baseSpeed = 268,
            baseWisdom = 205,
            apexTargetName = "Thalassor (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),
        DigimonSpeciesSpec(
            speciesId = "seraphimon",
            speciesName = "Aetherdrake",
            stage = "Mega",
            attribute = "Astral",
            titleTag = "Sky Dragon Sovereign • Mega Apex",
            signatureMove = "Starlight Tempest",
            primaryColorHex = 0xFFFDE047,
            secondaryColorHex = 0xFF00F0FF,
            baseAttack = 220,
            baseDefense = 225,
            baseSpeed = 230,
            baseWisdom = 275,
            apexTargetName = "Tempestigon (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),
        DigimonSpeciesSpec(
            speciesId = "gallantmon",
            speciesName = "Netherlord",
            stage = "Mega",
            attribute = "Shadow",
            titleTag = "Eclipse Knight • Mega Apex",
            signatureMove = "Crimson Eclipse",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFFFB800,
            baseAttack = 268,
            baseDefense = 238,
            baseSpeed = 225,
            baseWisdom = 200,
            apexTargetName = "Ignisaur (Lvl 100)",
            evolutionBranches = level100RareBranches
        ),

        // 6 LEVEL-100 SOVEREIGN LEGENDARIES (Only obtainable by ascending a companion that has personally reached Level 100)
        DigimonSpeciesSpec(
            speciesId = "omegamon_x",
            speciesName = "Solphoenix",
            stage = "Mega",
            attribute = "Pyro",
            titleTag = "Lvl 100 Sovereign • Sunheart Bell Guardian",
            signatureMove = "Sacred Sunfire",
            primaryColorHex = 0xFFFFB800,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 350,
            baseDefense = 320,
            baseSpeed = 335,
            baseWisdom = 315,
            apexTargetName = "Solphoenix",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "alphamon_ouryuken",
            speciesName = "Thalassor",
            stage = "Mega",
            attribute = "Aqua",
            titleTag = "Lvl 100 Sovereign • SoulCrest Tide Guardian",
            signatureMove = "Abyssal Gale",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFFA855F7,
            baseAttack = 345,
            baseDefense = 340,
            baseSpeed = 315,
            baseWisdom = 335,
            apexTargetName = "Thalassor",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "imperialdramon_pm",
            speciesName = "Borealis",
            stage = "Mega",
            attribute = "Frost",
            titleTag = "Lvl 100 Sovereign • Northern Gale Beast",
            signatureMove = "Aurora Crystal",
            primaryColorHex = 0xFF00F0FF,
            secondaryColorHex = 0xFFFDE047,
            baseAttack = 355,
            baseDefense = 315,
            baseSpeed = 340,
            baseWisdom = 310,
            apexTargetName = "Borealis",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "susanoomon_zero",
            speciesName = "Tempestigon",
            stage = "Mega",
            attribute = "Storm",
            titleTag = "Lvl 100 Sovereign • Stormcloud Beast",
            signatureMove = "Thunderbolt Roar",
            primaryColorHex = 0xFFFFB800,
            secondaryColorHex = 0xFF38BDF8,
            baseAttack = 350,
            baseDefense = 325,
            baseSpeed = 320,
            baseWisdom = 355,
            apexTargetName = "Tempestigon",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "gallantmon_crimson",
            speciesName = "Ignisaur",
            stage = "Mega",
            attribute = "Pyro",
            titleTag = "Lvl 100 Sovereign • Caldera Beast",
            signatureMove = "Magma Eruption",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFFFFD700,
            baseAttack = 368,
            baseDefense = 318,
            baseSpeed = 335,
            baseWisdom = 310,
            apexTargetName = "Ignisaur",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        ),
        DigimonSpeciesSpec(
            speciesId = "armageddemon_core",
            speciesName = "Aetherion",
            stage = "Mega",
            attribute = "Astral",
            titleTag = "Lvl 100 Sovereign • Silver Peak Apex",
            signatureMove = "Chrono Singularity",
            primaryColorHex = 0xFFA855F7,
            secondaryColorHex = 0xFFEF4444,
            baseAttack = 380,
            baseDefense = 350,
            baseSpeed = 295,
            baseWisdom = 300,
            apexTargetName = "Aetherion",
            isRareLevel100Spec = true,
            evolutionBranches = emptyList()
        )
    ) + DigiWorldExpansionCatalog.extra71SpeciesSpecs).associateBy { it.speciesId }

    private val speciesAliases: Map<String, String> = mapOf(
        "sproutling" to "patamon",
        "cinderkit" to "agumon",
        "riptide" to "gabumon",
        "voltkit" to "tentomon",
        "vulpineon" to "renamon",
        "woolspark" to "veemon",
        "ursaflare" to "gammamon",
        "duskfang" to "guilmon",
        "cragscale" to "dorumon",
        "wyrmlet" to "terriermon",
        "solphoenix" to "omegamon_x",
        "thalassor" to "alphamon_ouryuken",
        "borealis" to "imperialdramon_pm",
        "tempestigon" to "susanoomon_zero",
        "ignisaur" to "gallantmon_crimson",
        "aetherion" to "armageddemon_core"
    )

    val allSpeciesSpecs: List<DigimonSpeciesSpec>
        get() = allSpecies.values.toList()

    val wildTamableSpecs: List<DigimonSpeciesSpec>
        get() = allSpeciesSpecs.filter { !it.isRareLevel100Spec }

    fun rollWeightedWildMonster(random: kotlin.random.Random = kotlin.random.Random.Default): DigimonSpeciesSpec {
        val pool = wildTamableSpecs
        val roll = random.nextInt(100)
        val targetTier = when {
            roll < 60 -> WildRarityTier.COMMON          // 60% Common (Base Stage)
            roll < 84 -> WildRarityTier.UNCOMMON        // 24% Uncommon (Stage 1)
            roll < 95 -> WildRarityTier.RARE            // 11% Rare (Stage 2)
            roll < 99 -> WildRarityTier.ULTRA_RARE      // 4% Ultra Rare (Ascended)
            else -> WildRarityTier.LEGENDARY_APEX       // 1% Legendary Apex (Sovereign)
        }
        val tierCandidates = pool.filter { it.wildRarityTier() == targetTier }
        val candidates = tierCandidates.ifEmpty {
            pool.filter { it.stage == "Rookie" }.ifEmpty { pool }
        }
        return candidates[random.nextInt(candidates.size)]
    }

    val allExpeditions: List<ExpeditionSpec>
        get() = DigiWorldExpansionCatalog.allExpeditions

    fun getExpedition(expeditionId: String): ExpeditionSpec? =
        DigiWorldExpansionCatalog.getExpedition(expeditionId)

    fun getSkillTree(speciesId: String): MonsterSkillTreeSpec =
        DigiWorldExpansionCatalog.buildSkillTreeForSpecies(getSpec(speciesId))

    fun getSkillTreeForSpecies(speciesId: String): MonsterSkillTreeSpec =
        getSkillTree(speciesId)

    fun getSpec(speciesId: String): DigimonSpeciesSpec {
        val clean = speciesId.trim().lowercase()
        val resolvedId = speciesAliases[clean] ?: clean
        return allSpecies[resolvedId] ?: allSpecies[speciesId] ?: allSpecies.getValue("patamon")
    }

    fun getMonsterImageRes(speciesId: String): Int {
        val id = (speciesAliases[speciesId.lowercase()] ?: speciesId.lowercase())
        return when {
            id in level100RareSpeciesIds ->
                com.example.R.drawable.img_monster_mythic_apex
            id.contains("rena") || id.contains("terrier") || id.contains("lop") || id.contains("kyubi") || id.contains("tao") || id.contains("sala") || id.contains("gato") || id.contains("angora") ->
                com.example.R.drawable.img_monster_cyber_kitsune
            id.contains("tento") || id.contains("kabuteri") || id.contains("gamma") || id.contains("canoweiss") || id.contains("worm") || id.contains("sting") || id.contains("haguru") || id.contains("guardro") || id.contains("andro") || id.contains("pulse") ->
                com.example.R.drawable.img_monster_electro_beetle
            id.contains("garuru") || id.contains("gabu") || id.contains("doru") || id.contains("wolf") || id.contains("looga") || id.contains("jelly") ->
                com.example.R.drawable.img_monster_frost_wolf
            id.contains("ange") || id.contains("seraph") || id.contains("pata") || id.contains("gargo") || id.contains("biyo") || id.contains("hawk") || id.contains("bird") ->
                com.example.R.drawable.img_monster_celestial_wing
            id.contains("guil") || id.contains("growl") || id.contains("gallant") || id.contains("imp") || id.contains("myotis") ->
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
        val clean = speciesAliases[speciesId.trim().lowercase()] ?: speciesId
        val spec = allSpecies[clean] ?: return false
        return spec.stage == "Rookie" && !spec.isRareLevel100Spec
    }

    fun requireRookieStarterSpec(speciesId: String): DigimonSpeciesSpec {
        val clean = speciesAliases[speciesId.trim().lowercase()] ?: speciesId
        val candidate = allSpecies[clean]
        return if (candidate != null && candidate.stage == "Rookie" && !candidate.isRareLevel100Spec) {
            candidate
        } else {
            allSpecies.getValue("patamon")
        }
    }

    // 10 Distinct Base-Stage Starters led by the Aurelia Trio (Sproutling, Cinderkit, Riptide)
    val starterSpecs: List<DigimonSpeciesSpec>
        get() = listOf(
            getSpec("patamon"),    // #1 Sproutling (Flora • Overgrowth)
            getSpec("agumon"),     // #2 Cinderkit (Pyro • Kindle Core)
            getSpec("gabumon"),    // #3 Riptide (Aqua • Rushing Surge)
            getSpec("tentomon"),   // #4 Voltkit (Storm • Static Pulse)
            getSpec("renamon"),    // #5 Vulpineon (Aether • Resonant Bond)
            getSpec("veemon"),     // #6 Woolspark (Storm • Charged Fleece)
            getSpec("gammamon"),   // #7 Ursaflare (Terra • Nimble Stride)
            getSpec("guilmon"),    // #8 Duskfang (Shadow • Searing Fang)
            getSpec("dorumon"),    // #9 Cragscale (Terra • Bedrock Grit)
            getSpec("terriermon")  // #10 Wyrmlet (Astral • Mystic Scales)
        ).filter { it.stage == "Rookie" && !it.isRareLevel100Spec }

    val allRookieSpecs: List<DigimonSpeciesSpec>
        get() {
            val coreIds = listOf(
                "patamon", "agumon", "gabumon", "tentomon", "renamon",
                "veemon", "gammamon", "guilmon", "dorumon", "terriermon"
            )
            val core = coreIds.mapNotNull { allSpecies[it] }.filter { it.stage == "Rookie" && !it.isRareLevel100Spec }
            val extra = allSpeciesSpecs.filter { it.stage == "Rookie" && !it.isRareLevel100Spec && it.speciesId !in coreIds }
            return (core + extra).filter { it.stage == "Rookie" && !it.isRareLevel100Spec }
        }

    val level100RareSpecs: List<DigimonSpeciesSpec>
        get() = level100RareSpeciesIds.map { getSpec(it) }

    fun getFourBattleMoves(creature: DigimonCreature): List<BattleMoveSpec> {
        val spec = getSpec(creature.speciesId)
        val elem = spec.attribute
        val statusMoveTitle = when {
            elem.contains("Flora", ignoreCase = true) || elem.contains("Astral", ignoreCase = true) ->
                "Slumber Spore (💤 Sleep)"
            elem.contains("Storm", ignoreCase = true) || elem.contains("Gale", ignoreCase = true) || elem.contains("Metal", ignoreCase = true) ->
                "Static Pulse (⚡ Paralyze)"
            else ->
                "Ember Sear (🔥 Burn)"
        }
        val statusEffectDesc = when {
            statusMoveTitle.contains("Sleep") -> "Inflicts 💤 ASLEEP (+25% Acorn Orb Capture Rate) & light chip"
            statusMoveTitle.contains("Paralyze") -> "Inflicts ⚡ PARALYZED (+18% Acorn Orb Capture Rate) & stuns"
            else -> "Inflicts 🔥 SEARED (+12% Acorn Orb Capture Rate) & weakens guard"
        }
        return listOf(
            BattleMoveSpec(
                moveIndex = 0,
                moveName = spec.signatureMove,
                elementAttribute = elem,
                category = BattleMoveCategory.ELEMENTAL_STRIKE,
                power = 85,
                accuracy = 100,
                maxPp = 15,
                effectDescription = "STAB $elem Signature Strike (2.0x vs ${ElementalMatchupEngine.strongAgainstSummary(elem)})"
            ),
            BattleMoveSpec(
                moveIndex = 1,
                moveName = "Mercy Slash",
                elementAttribute = "Neutral",
                category = BattleMoveCategory.MERCY_SUBDUE,
                power = 55,
                accuracy = 100,
                maxPp = 25,
                effectDescription = "Subdue Strike: NEVER drops Wild HP below 1% (+35% Red-HP Capture Bonus!)"
            ),
            BattleMoveSpec(
                moveIndex = 2,
                moveName = statusMoveTitle,
                elementAttribute = elem,
                category = BattleMoveCategory.STATUS_WAVE,
                power = 20,
                accuracy = 95,
                maxPp = 15,
                effectDescription = statusEffectDesc
            ),
            BattleMoveSpec(
                moveIndex = 3,
                moveName = "Crest Resonance",
                elementAttribute = "Aether",
                category = BattleMoveCategory.CREST_SURGE,
                power = 40,
                accuracy = 100,
                maxPp = 10,
                effectDescription = "Restores +18 HP to ${creature.nickname}, boosts Bond, & strikes with Aether wave"
            )
        )
    }
}
