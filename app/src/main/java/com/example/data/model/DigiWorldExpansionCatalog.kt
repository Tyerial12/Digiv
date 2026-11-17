package com.example.data.model

data class ExpeditionSpec(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val durationMinutes: Int,
    val durationLabel: String,
    val recommendedLevel: Int,
    val rewardCoins: Int,
    val rewardAtk: Int,
    val rewardDef: Int,
    val rewardSpd: Int,
    val rewardWis: Int,
    val rewardEvoSync: Int,
    val hungerDrainPerCycle: Int,
    val staminaDrainPerCycle: Int
) {
    val durationSeconds: Long
        get() = durationMinutes * 60L
}

data class MonsterSkillNode(
    val nodeId: String,
    val tier: Int,
    val branchName: String,
    val title: String,
    val description: String,
    val atkBonus: Int = 0,
    val defBonus: Int = 0,
    val spdBonus: Int = 0,
    val wisBonus: Int = 0,
    val cryoRegenBonus: Int = 0,
    val tammingRateBonusPercent: Int = 0,
    val prerequisiteNodeId: String? = null
)

data class MonsterSkillTreeSpec(
    val speciesId: String,
    val speciesName: String,
    val treeArchetypeTitle: String,
    val nodes: List<MonsterSkillNode>
)

enum class WildRarityTier(
    val displayName: String,
    val badgeLabel: String,
    val spawnChancePercent: Int,
    val radarLockChancePercent: Int,
    val minBaseTameRate: Int,
    val maxBaseTameRate: Int,
    val minHitGain: Int,
    val maxHitGain: Int,
    val minSurgeGain: Int,
    val maxSurgeGain: Int,
    val instantCritChancePercent: Int,
    val resistPenaltyOrGraze: IntRange,
    val colorHex: Long
) {
    COMMON(
        displayName = "Common",
        badgeLabel = "COMMON (60% SPAWN)",
        spawnChancePercent = 60,
        radarLockChancePercent = 85,
        minBaseTameRate = 56,
        maxBaseTameRate = 70,
        minHitGain = 22,
        maxHitGain = 34,
        minSurgeGain = 38,
        maxSurgeGain = 50,
        instantCritChancePercent = 12,
        resistPenaltyOrGraze = 2..5,
        colorHex = 0xFF00E676
    ),
    UNCOMMON(
        displayName = "Uncommon",
        badgeLabel = "UNCOMMON (24% SPAWN)",
        spawnChancePercent = 24,
        radarLockChancePercent = 45,
        minBaseTameRate = 34,
        maxBaseTameRate = 45,
        minHitGain = 15,
        maxHitGain = 23,
        minSurgeGain = 26,
        maxSurgeGain = 36,
        instantCritChancePercent = 6,
        resistPenaltyOrGraze = 1..3,
        colorHex = 0xFF00E5FF
    ),
    RARE(
        displayName = "Rare",
        badgeLabel = "RARE (11% SPAWN)",
        spawnChancePercent = 11,
        radarLockChancePercent = 20,
        minBaseTameRate = 18,
        maxBaseTameRate = 26,
        minHitGain = 10,
        maxHitGain = 16,
        minSurgeGain = 18,
        maxSurgeGain = 25,
        instantCritChancePercent = 3,
        resistPenaltyOrGraze = -1..1,
        colorHex = 0xFFA855F7
    ),
    ULTRA_RARE(
        displayName = "Ultra Rare",
        badgeLabel = "ULTRA RARE (4% SPAWN)",
        spawnChancePercent = 4,
        radarLockChancePercent = 8,
        minBaseTameRate = 8,
        maxBaseTameRate = 14,
        minHitGain = 6,
        maxHitGain = 11,
        minSurgeGain = 12,
        maxSurgeGain = 18,
        instantCritChancePercent = 1,
        resistPenaltyOrGraze = -3..0,
        colorHex = 0xFFFFB300
    ),
    LEGENDARY_APEX(
        displayName = "Legendary Apex",
        badgeLabel = "LEGENDARY APEX (1% SPAWN)",
        spawnChancePercent = 1,
        radarLockChancePercent = 3,
        minBaseTameRate = 4,
        maxBaseTameRate = 7,
        minHitGain = 4,
        maxHitGain = 8,
        minSurgeGain = 9,
        maxSurgeGain = 13,
        instantCritChancePercent = 0,
        resistPenaltyOrGraze = -5..-2,
        colorHex = 0xFFFF5252
    )
}

/**
 * Elemental Type Effectiveness Chart & Matchup Engine (Classic RPG 2.0x / 1.0x / 0.5x).
 */
object ElementalMatchupEngine {

    private fun canonicalGroup(rawAttr: String): String {
        val u = rawAttr.trim().uppercase()
        return when {
            u.contains("PYRO") || u.contains("FIRE") || u.contains("FLAME") || u.contains("SOLAR") -> "PYRO"
            u.contains("AQUA") || u.contains("WATER") || u.contains("TIDE") || u.contains("OCEAN") -> "AQUA"
            u.contains("FLORA") || u.contains("GRASS") || u.contains("LEAF") || u.contains("NATURE") || u.contains("SYLVAN") -> "FLORA"
            u.contains("STORM") || u.contains("VOLT") || u.contains("THUNDER") || u.contains("ELECTRIC") || u.contains("PLASMA") -> "STORM"
            u.contains("TERRA") || u.contains("EARTH") || u.contains("ROCK") || u.contains("STONE") || u.contains("GROUND") -> "TERRA"
            u.contains("FROST") || u.contains("ICE") || u.contains("CRYO") || u.contains("GLACIER") -> "FROST"
            u.contains("GALE") || u.contains("WIND") || u.contains("SKY") || u.contains("AERO") -> "GALE"
            u.contains("ASTRAL") || u.contains("PSYCHIC") || u.contains("MIND") || u.contains("COSMIC") || u.contains("STAR") -> "ASTRAL"
            u.contains("SHADOW") || u.contains("DARK") || u.contains("UMBRA") || u.contains("VOID") || u.contains("PHANTOM") -> "SHADOW"
            u.contains("AETHER") || u.contains("DRAGON") || u.contains("DRAKE") || u.contains("RADIANT") || u.contains("MYTHIC") -> "AETHER"
            u.contains("METAL") || u.contains("STEEL") || u.contains("IRON") || u.contains("CHROME") -> "METAL"
            u.contains("VENOM") || u.contains("TOXIN") || u.contains("POISON") || u.contains("BLIGHT") -> "VENOM"
            u.contains("BRAWLER") || u.contains("MARTIAL") || u.contains("COMBAT") || u.contains("VALOR") -> "BRAWLER"
            else -> "NEUTRAL"
        }
    }

    private val superEffectiveMap: Map<String, Set<String>> = mapOf(
        "PYRO" to setOf("FLORA", "FROST", "METAL"),
        "AQUA" to setOf("PYRO", "TERRA"),
        "FLORA" to setOf("AQUA", "TERRA"),
        "STORM" to setOf("AQUA", "GALE", "METAL"),
        "TERRA" to setOf("PYRO", "STORM", "VENOM", "METAL"),
        "FROST" to setOf("FLORA", "TERRA", "GALE", "AETHER"),
        "GALE" to setOf("FLORA", "BRAWLER"),
        "ASTRAL" to setOf("BRAWLER", "VENOM", "SHADOW"),
        "SHADOW" to setOf("ASTRAL", "AETHER"),
        "AETHER" to setOf("AETHER", "SHADOW", "PYRO"),
        "METAL" to setOf("FROST", "TERRA", "AETHER"),
        "VENOM" to setOf("FLORA", "BRAWLER"),
        "BRAWLER" to setOf("TERRA", "FROST", "METAL", "SHADOW")
    )

    private val notVeryEffectiveMap: Map<String, Set<String>> = mapOf(
        "PYRO" to setOf("AQUA", "TERRA", "AETHER"),
        "AQUA" to setOf("FLORA", "STORM", "AETHER"),
        "FLORA" to setOf("PYRO", "GALE", "VENOM", "METAL"),
        "STORM" to setOf("TERRA", "FLORA"),
        "TERRA" to setOf("FLORA", "AQUA", "GALE"),
        "FROST" to setOf("PYRO", "AQUA", "METAL"),
        "GALE" to setOf("STORM", "METAL", "TERRA"),
        "ASTRAL" to setOf("METAL", "ASTRAL"),
        "SHADOW" to setOf("BRAWLER", "METAL"),
        "AETHER" to setOf("METAL", "FROST"),
        "METAL" to setOf("PYRO", "AQUA", "STORM"),
        "VENOM" to setOf("TERRA", "METAL"),
        "BRAWLER" to setOf("GALE", "ASTRAL", "VENOM")
    )

    fun effectivenessMultiplier(attackerAttr: String, defenderAttr: String): Float {
        val atk = canonicalGroup(attackerAttr)
        val def = canonicalGroup(defenderAttr)
        if (atk == "NEUTRAL" || def == "NEUTRAL") return 1.0f
        return when {
            superEffectiveMap[atk]?.contains(def) == true -> 2.0f
            notVeryEffectiveMap[atk]?.contains(def) == true -> 0.5f
            else -> 1.0f
        }
    }

    fun effectivenessBadge(multiplier: Float): String = when {
        multiplier >= 1.8f -> "SUPER EFFECTIVE! (2.0x)"
        multiplier <= 0.6f -> "NOT VERY EFFECTIVE (0.5x)"
        else -> "NEUTRAL HIT (1.0x)"
    }

    fun strongAgainstSummary(rawAttr: String): String {
        val group = canonicalGroup(rawAttr)
        val targets = superEffectiveMap[group]
        return if (targets.isNullOrEmpty()) "Balanced vs All Elements (1.0x)" else targets.joinToString(", ")
    }

    fun weakAgainstSummary(rawAttr: String): String {
        val group = canonicalGroup(rawAttr)
        val counters = superEffectiveMap.entries
            .filter { (_, strongSet) -> group in strongSet }
            .map { it.key }
        return if (counters.isEmpty()) "No Elemental Weakness" else counters.take(3).joinToString(", ")
    }
}

/**
 * Companion Natures (Personality Traits that grant +10% to one stat and -10% to another, or Balanced).
 */
enum class CompanionNature(
    val code: String,
    val displayName: String,
    val effectSummary: String,
    val atkMult: Float,
    val defMult: Float,
    val spdMult: Float,
    val wisMult: Float
) {
    ADAMANT(
        code = "ADM",
        displayName = "Adamant",
        effectSummary = "+10% ATK / -10% WIS",
        atkMult = 1.10f,
        defMult = 1.00f,
        spdMult = 1.00f,
        wisMult = 0.90f
    ),
    MODEST(
        code = "MOD",
        displayName = "Modest",
        effectSummary = "+10% WIS / -10% ATK",
        atkMult = 0.90f,
        defMult = 1.00f,
        spdMult = 1.00f,
        wisMult = 1.10f
    ),
    JOLLY(
        code = "JLY",
        displayName = "Jolly",
        effectSummary = "+10% SPD / -10% WIS",
        atkMult = 1.00f,
        defMult = 1.00f,
        spdMult = 1.10f,
        wisMult = 0.90f
    ),
    BOLD(
        code = "BLD",
        displayName = "Bold",
        effectSummary = "+10% DEF / -10% ATK",
        atkMult = 0.90f,
        defMult = 1.10f,
        spdMult = 1.00f,
        wisMult = 1.00f
    ),
    BRAVE(
        code = "BRV",
        displayName = "Brave",
        effectSummary = "+10% ATK / -10% SPD",
        atkMult = 1.10f,
        defMult = 1.00f,
        spdMult = 0.90f,
        wisMult = 1.00f
    ),
    SERENE(
        code = "SRN",
        displayName = "Serene",
        effectSummary = "Balanced (+3% All Stats)",
        atkMult = 1.03f,
        defMult = 1.03f,
        spdMult = 1.03f,
        wisMult = 1.03f
    );

    companion object {
        fun fromCode(code: String): CompanionNature =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BRAVE
    }
}

/**
 * Equippable Held Relics (Classic Held Items for Combat, Capture & Care Bonuses).
 */
enum class HeldRelicItem(
    val code: String,
    val displayName: String,
    val effectSummary: String,
    val atkBonusPercent: Int = 0,
    val defBonusPercent: Int = 0,
    val spdBonusPercent: Int = 0,
    val wisBonusPercent: Int = 0,
    val captureBonusPercent: Int = 0,
    val passiveHealBonus: Int = 0
) {
    SEARING_CHARCOAL(
        code = "CHR",
        displayName = "Searing Charcoal",
        effectSummary = "+12% Strike ATK & Pyro Power",
        atkBonusPercent = 12
    ),
    MYSTIC_DEWDROP(
        code = "DEW",
        displayName = "Mystic Dewdrop",
        effectSummary = "+12% Wisdom & Elemental Surge",
        wisBonusPercent = 12
    ),
    VERDANT_SEED(
        code = "SED",
        displayName = "Verdant Miracle Seed",
        effectSummary = "+12% Guard DEF & Vitality",
        defBonusPercent = 12
    ),
    STORM_MAGNET(
        code = "MAG",
        displayName = "Storm Magnet Core",
        effectSummary = "+12% Speed & First-Strike Crit",
        spdBonusPercent = 12
    ),
    LEFTOVER_AMBROSIA(
        code = "AMB",
        displayName = "Leftover Ambrosia",
        effectSummary = "Restores +8 HP after every Wild/Sanctum turn",
        defBonusPercent = 5,
        passiveHealBonus = 8
    ),
    LUCKY_CAPTURE_CHARM(
        code = "LCK",
        displayName = "Master Orin's Capture Charm",
        effectSummary = "+12% Acorn Orb Capture Rate & 2x Chromatic Odds",
        captureBonusPercent = 12
    );

    companion object {
        fun fromCode(code: String): HeldRelicItem =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SEARING_CHARCOAL
    }
}

/**
 * Wild Companion Status Conditions that boost Acorn Orb Capture Rate.
 */
enum class WildStatusEffect(
    val badgeLabel: String,
    val captureBonusPercent: Int
) {
    NONE("HEALTHY", 0),
    SLEEP("💤 ASLEEP (+25% CAPTURE)", 25),
    PARALYZED("⚡ PARALYZED (+18% CAPTURE)", 18),
    BURNED("🔥 SEARED (+12% CAPTURE)", 12)
}

enum class BattleMoveCategory {
    ELEMENTAL_STRIKE,
    MERCY_SUBDUE,
    STATUS_WAVE,
    CREST_SURGE
}

data class BattleMoveSpec(
    val moveIndex: Int,
    val moveName: String,
    val elementAttribute: String,
    val category: BattleMoveCategory,
    val power: Int,
    val accuracy: Int,
    val maxPp: Int,
    val effectDescription: String
)

/**
 * 16 Dual-Realm Sanctum Leaders (8 Aurelia Sanctums + 8 Argentia Sanctums).
 */
data class SanctumLeaderSpec(
    val badgeNumber: Int,
    val realmName: String,
    val cityTitle: String,
    val leaderName: String,
    val badgeName: String,
    val aceSpeciesId: String,
    val aceSpeciesName: String,
    val elementAttribute: String,
    val recommendedLevel: Int,
    val rewardCoins: Int
)

data class WildEncounterState(
    val speciesId: String,
    val speciesName: String,
    val stage: String,
    val attribute: String,
    val wildLevel: Int,
    val baseSuccessRatePercent: Int,
    val rarityTier: WildRarityTier = WildRarityTier.COMMON,
    val isUltraRareWild: Boolean = false,
    val droppedPartName: String = "Gilded Acorn Shell",
    val tameProgress: Int = 0,
    val berriesUsed: Int = 0,
    val lastRollOutcomeTitle: String = "Wild Companion Spotted!",
    val lastRollOutcomeDetail: String = "Weaken with Mercy Slash & Status Wave, then throw an Acorn Orb — or defeat for Gilded Acorns & Relic Shards!",
    val isTamed: Boolean = false,
    val wildHpPercent: Int = 100,
    val statusEffect: WildStatusEffect = WildStatusEffect.NONE,
    val isChromaticShiny: Boolean = false,
    val wildNatureName: String = "Brave",
    val lastEffectivenessLabel: String = ""
) {
    /**
     * Classic Weakened-HP Capture Bonus:
     * - Red HP (1%..20%): +35% Capture Rate
     * - Yellow HP (21%..50%): +20% Capture Rate
     * - Scratched HP (51%..75%): +10% Capture Rate
     * - Full HP (76%..100%): +0% Capture Rate
     */
    fun weakenedHpCaptureBonusPercent(): Int = when {
        wildHpPercent in 1..20 -> 35
        wildHpPercent in 21..50 -> 20
        wildHpPercent in 51..75 -> 10
        else -> 0
    }

    fun totalTacticalCaptureBonusPercent(): Int =
        weakenedHpCaptureBonusPercent() + statusEffect.captureBonusPercent
}

object DigiWorldExpansionCatalog {

    val allSanctumLeaders: List<SanctumLeaderSpec> = listOf(
        // 8 AURELIA REALM SANCTUM LEADERS
        SanctumLeaderSpec(1, "Aurelia", "Zephyr Roost", "Vane", "Gale Wing Crest", "hawkmon", "Talonwing", "Gale", 8, 25),
        SanctumLeaderSpec(2, "Aurelia", "Bramble Glen", "Rowan", "Cocoon Thorn Crest", "tentomon", "Scarabite", "Flora", 12, 30),
        SanctumLeaderSpec(3, "Aurelia", "Goldcrest Plaza", "Clairette", "Starlight Meadow Crest", "salamon", "Mewlet", "Neutral", 16, 35),
        SanctumLeaderSpec(4, "Aurelia", "Duskbell Tower", "Malakor", "Phantom Fog Crest", "bakemon", "Wraithcape", "Shadow", 20, 40),
        SanctumLeaderSpec(5, "Aurelia", "Surgetide Cove", "Bram", "Breaker Fist Crest", "leomon", "Manestrike", "Brawler", 25, 45),
        SanctumLeaderSpec(6, "Aurelia", "Ironbeacon Port", "Selene", "Aegis Alloy Crest", "guardromon", "Aegisbot", "Metal", 30, 50),
        SanctumLeaderSpec(7, "Aurelia", "Rimefrost Tarn", "Kaelen", "Glacier Fang Crest", "frigimon", "Rimebear", "Frost", 35, 55),
        SanctumLeaderSpec(8, "Aurelia", "Wyrmfang Cavern", "Lyra", "Rising Drake Crest", "seadramon", "Tidedrake", "Aether", 42, 65),
        // 8 ARGENTIA REALM SANCTUM LEADERS
        SanctumLeaderSpec(9, "Argentia", "Granite Crag", "Garrick", "Boulder Bastion Crest", "gotsumon", "Cragling", "Terra", 48, 70),
        SanctumLeaderSpec(10, "Argentia", "Azure Cascade", "Marina", "Cerulean Tide Crest", "zudomon", "Tuskhammer", "Aqua", 54, 75),
        SanctumLeaderSpec(11, "Argentia", "Thunderport Bay", "Vance", "Dynamo Bolt Crest", "magnagarurumon", "Aetherfang", "Storm", 60, 80),
        SanctumLeaderSpec(12, "Argentia", "Prism Conservatory", "Evelyn", "Sunpetal Bloom Crest", "lillymon", "Sylphbloom", "Flora", 66, 85),
        SanctumLeaderSpec(13, "Argentia", "Nightshade Ward", "Kira", "Venom Lotus Crest", "myotismon", "Nocturnus", "Venom", 72, 90),
        SanctumLeaderSpec(14, "Argentia", "Astral Spire", "Seraphina", "Mindseye Halo Crest", "magnaangemon", "Archseraph", "Astral", 78, 95),
        SanctumLeaderSpec(15, "Argentia", "Caldera Bastion", "Ignatius", "Volcanic Caldera Crest", "meramon", "Pyrogeist", "Pyro", 86, 105),
        SanctumLeaderSpec(16, "Argentia", "Obsidian Keep", "Sovereign Red", "Dual-Realm Legend Crest", "omegamon", "AegisValkyr", "Aether", 96, 150)
    )

    // 10 Dual-Realm 16-Crest Sanctum & Silver Peak Expeditions (up to 24 Hours; ONLY the 24h expedition brings back 100 Crest Coins)
    val allExpeditions: List<ExpeditionSpec> = listOf(
        ExpeditionSpec(
            id = "exp_01_recon",
            number = 1,
            title = "Crest 1–2: Zephyr Roost & Sprout Spire",
            subtitle = "Aurelia wind sanctum & ancient pagoda patrol. Keep fed & rested.",
            durationMinutes = 5,
            durationLabel = "5 Mins",
            recommendedLevel = 1,
            rewardCoins = 5,
            rewardAtk = 1,
            rewardDef = 1,
            rewardSpd = 0,
            rewardWis = 0,
            rewardEvoSync = 4,
            hungerDrainPerCycle = 3,
            staminaDrainPerCycle = 4
        ),
        ExpeditionSpec(
            id = "exp_02_forest",
            number = 2,
            title = "Crest 3–4: Whispering Woods & Sunspire Plaza",
            subtitle = "Track wild companions through deep emerald groves to the Grand Bazaar.",
            durationMinutes = 15,
            durationLabel = "15 Mins",
            recommendedLevel = 3,
            rewardCoins = 10,
            rewardAtk = 1,
            rewardDef = 0,
            rewardSpd = 2,
            rewardWis = 1,
            rewardEvoSync = 6,
            hungerDrainPerCycle = 4,
            staminaDrainPerCycle = 4
        ),
        ExpeditionSpec(
            id = "exp_03_canyon",
            number = 3,
            title = "Crest 5–6: Brass Bell Sanctum & Beacon Lighthouse",
            subtitle = "Ascend the autumn chime tower and illuminate the coastal storm beacon.",
            durationMinutes = 30,
            durationLabel = "30 Mins",
            recommendedLevel = 6,
            rewardCoins = 15,
            rewardAtk = 2,
            rewardDef = 2,
            rewardSpd = 1,
            rewardWis = 1,
            rewardEvoSync = 8,
            hungerDrainPerCycle = 4,
            staminaDrainPerCycle = 5
        ),
        ExpeditionSpec(
            id = "exp_04_mine",
            number = 4,
            title = "Crest 7–8: Frostlake Glacier & Wyrm Den",
            subtitle = "Cross the frozen lake caverns and pass the Elder's Dragon Shrine trial.",
            durationMinutes = 60,
            durationLabel = "1 Hour",
            recommendedLevel = 10,
            rewardCoins = 22,
            rewardAtk = 2,
            rewardDef = 3,
            rewardSpd = 1,
            rewardWis = 2,
            rewardEvoSync = 10,
            hungerDrainPerCycle = 5,
            staminaDrainPerCycle = 5
        ),
        ExpeditionSpec(
            id = "exp_05_sea",
            number = 5,
            title = "Whirlpool Trench & Silver Tide Islands",
            subtitle = "Submerged tidal trench dive where Thalassor's silver currents surge.",
            durationMinutes = 120,
            durationLabel = "2 Hours",
            recommendedLevel = 15,
            rewardCoins = 30,
            rewardAtk = 2,
            rewardDef = 2,
            rewardSpd = 3,
            rewardWis = 3,
            rewardEvoSync = 14,
            hungerDrainPerCycle = 5,
            staminaDrainPerCycle = 6
        ),
        ExpeditionSpec(
            id = "exp_06_bastion",
            number = 6,
            title = "Crest 9–11: Argentia Stone, Cascade & Thunder Sanctums",
            subtitle = "Voyage east to Argentia's first three sanctums; monitor food & rest.",
            durationMinutes = 240,
            durationLabel = "4 Hours",
            recommendedLevel = 22,
            rewardCoins = 40,
            rewardAtk = 4,
            rewardDef = 4,
            rewardSpd = 2,
            rewardWis = 2,
            rewardEvoSync = 18,
            hungerDrainPerCycle = 6,
            staminaDrainPerCycle = 6
        ),
        ExpeditionSpec(
            id = "exp_07_sky",
            number = 7,
            title = "Sunheart Sky Bell Tower Summit",
            subtitle = "High-altitude golden bell ascent where Solphoenix's rainbow feathers glow.",
            durationMinutes = 360,
            durationLabel = "6 Hours",
            recommendedLevel = 30,
            rewardCoins = 50,
            rewardAtk = 3,
            rewardDef = 3,
            rewardSpd = 5,
            rewardWis = 4,
            rewardEvoSync = 22,
            hungerDrainPerCycle = 6,
            staminaDrainPerCycle = 7
        ),
        ExpeditionSpec(
            id = "exp_08_labyrinth",
            number = 8,
            title = "Crest 12–14: Prism, Astral & Shadow Sanctums",
            subtitle = "Multi-hour endurance trek across central Argentia's mystic sanctums.",
            durationMinutes = 720,
            durationLabel = "12 Hours",
            recommendedLevel = 42,
            rewardCoins = 65,
            rewardAtk = 5,
            rewardDef = 5,
            rewardSpd = 4,
            rewardWis = 5,
            rewardEvoSync = 28,
            hungerDrainPerCycle = 7,
            staminaDrainPerCycle = 7
        ),
        ExpeditionSpec(
            id = "exp_09_sanctum",
            number = 9,
            title = "Crest 15–16: Caldera Isle & Viridian Crown",
            subtitle = "Claim the final two Realm Crests and enter the Starlight Hall of Fame.",
            durationMinutes = 1080,
            durationLabel = "18 Hours",
            recommendedLevel = 55,
            rewardCoins = 80,
            rewardAtk = 6,
            rewardDef = 6,
            rewardSpd = 6,
            rewardWis = 6,
            rewardEvoSync = 35,
            hungerDrainPerCycle = 7,
            staminaDrainPerCycle = 8
        ),
        ExpeditionSpec(
            id = "exp_10_abyss_24h",
            number = 10,
            title = "Silver Peak Summit: Grand Champion Odyssey (24h)",
            subtitle = "Full 24-Hour ultimate expedition atop Silver Peak! Only expedition that brings back 100 Crest Coins.",
            durationMinutes = 1440,
            durationLabel = "24 Hours",
            recommendedLevel = 70,
            rewardCoins = 100,
            rewardAtk = 9,
            rewardDef = 9,
            rewardSpd = 9,
            rewardWis = 9,
            rewardEvoSync = 50,
            hungerDrainPerCycle = 8,
            staminaDrainPerCycle = 8
        )
    )

    fun getExpedition(expeditionId: String): ExpeditionSpec? =
        allExpeditions.firstOrNull { it.id == expeditionId }

    // Generates a unique, species-specific 6-node Skill Tree for each of the 100 companions
    fun buildSkillTreeForSpecies(spec: DigimonSpeciesSpec): MonsterSkillTreeSpec {
        val seed = spec.speciesId.hashCode().let { if (it < 0) -it else it }
        val primaryBias = when {
            spec.baseAttack >= spec.baseDefense && spec.baseAttack >= spec.baseSpeed && spec.baseAttack >= spec.baseWisdom -> "Sunheart Striker"
            spec.baseDefense >= spec.baseAttack && spec.baseDefense >= spec.baseSpeed && spec.baseDefense >= spec.baseWisdom -> "Iron Citadel"
            spec.baseSpeed >= spec.baseAttack && spec.baseSpeed >= spec.baseDefense && spec.baseSpeed >= spec.baseWisdom -> "Zephyr Runner"
            else -> "SoulCrest Sage"
        }
        val bonusShift = (seed % 4) + 1
        val n1Id = "${spec.speciesId}_node_1"
        val n2Id = "${spec.speciesId}_node_2"
        val n3Id = "${spec.speciesId}_node_3"
        val n4Id = "${spec.speciesId}_node_4"
        val n5Id = "${spec.speciesId}_node_5"
        val n6Id = "${spec.speciesId}_node_6"

        return MonsterSkillTreeSpec(
            speciesId = spec.speciesId,
            speciesName = spec.speciesName,
            treeArchetypeTitle = "${spec.speciesName} • $primaryBias Mastery (${spec.attribute})",
            nodes = listOf(
                MonsterSkillNode(
                    nodeId = n1Id,
                    tier = 1,
                    branchName = "Offense Crest",
                    title = "${spec.speciesName} Strike Arts",
                    description = "Amplifies ${spec.signatureMove} power (+${5 + bonusShift} ATK, +3 SPD).",
                    atkBonus = 5 + bonusShift,
                    spdBonus = 3
                ),
                MonsterSkillNode(
                    nodeId = n2Id,
                    tier = 1,
                    branchName = "Aegis Crest",
                    title = "${spec.attribute} Ward Coat",
                    description = "Reinforces ${spec.speciesName}'s natural guard (+${5 + bonusShift} DEF, +3 WIS).",
                    defBonus = 5 + bonusShift,
                    wisBonus = 3
                ),
                MonsterSkillNode(
                    nodeId = n3Id,
                    tier = 2,
                    branchName = "Sanctuary & Route",
                    title = "${spec.speciesName} Rest Vitality",
                    description = "Boosts Sanctuary Rest stamina recovery (+3/tick) & +5 DEF for 16-Crest expeditions.",
                    defBonus = 5,
                    wisBonus = 4,
                    cryoRegenBonus = 3,
                    prerequisiteNodeId = n2Id
                ),
                MonsterSkillNode(
                    nodeId = n4Id,
                    tier = 2,
                    branchName = "Signature Mastery",
                    title = "${spec.signatureMove} Awakening",
                    description = "Awakens ${spec.signatureMove} elemental surge (+${8 + bonusShift} ATK, +6 SPD).",
                    atkBonus = 8 + bonusShift,
                    spdBonus = 6,
                    prerequisiteNodeId = n1Id
                ),
                MonsterSkillNode(
                    nodeId = n5Id,
                    tier = 3,
                    branchName = "Acorn Orb Link",
                    title = "${spec.speciesName} Reserve Harmony",
                    description = "+8% Acorn Orb capture rate & +7 WIS/+5 SPD when scanning Emerald Reserve companions.",
                    spdBonus = 5,
                    wisBonus = 7,
                    tammingRateBonusPercent = 8,
                    prerequisiteNodeId = n3Id
                ),
                MonsterSkillNode(
                    nodeId = n6Id,
                    tier = 3,
                    branchName = "Sovereign Crest",
                    title = "SoulCrest of ${spec.speciesName}",
                    description = "Unlocks ${spec.speciesName}'s dual-realm potential (+10 ATK, +10 DEF, +10 SPD, +10 WIS).",
                    atkBonus = 10,
                    defBonus = 10,
                    spdBonus = 10,
                    wisBonus = 10,
                    cryoRegenBonus = 2,
                    tammingRateBonusPercent = 5,
                    prerequisiteNodeId = n4Id
                )
            )
        )
    }

    // 71 Additional Unique Aurelia & Argentia Companions so total catalog = 29 + 71 = 100 Unique Species
    private data class RawWildDef(
        val id: String,
        val name: String,
        val stage: String,
        val attribute: String,
        val move: String,
        val atk: Int,
        val def: Int,
        val spd: Int,
        val wis: Int,
        val evoTargetId: String,
        val abilityName: String = "",
        val abilityDesc: String = ""
    )

    private val raw71Definitions: List<RawWildDef> = listOf(
        // 28 Base-Stage / Rookie Companions (#30 - #57, including Starters #4-#10)
        RawWildDef(
            id = "tentomon",
            name = "Voltkit",
            stage = "Rookie",
            attribute = "Storm",
            move = "Spark Dash",
            atk = 48,
            def = 44,
            spd = 60,
            wis = 50,
            evoTargetId = "kabuterimon",
            abilityName = "Static Pulse",
            abilityDesc = "High Speed (60 SPD) & +18% Critical Surge on Storm strikes"
        ),
        RawWildDef("palmon", "Briarbud", "Rookie", "Flora", "Vine Lash", 46, 44, 50, 52, "togemon"),
        RawWildDef("gomamon", "Pebblefin", "Rookie", "Aqua", "Bubble Torrent", 45, 50, 46, 51, "ikkakumon"),
        RawWildDef("biyo", "Zephyrchick", "Rookie", "Zephyr", "Gust Wing", 48, 40, 58, 46, "birdramon"),
        RawWildDef("salamon", "Moonkit", "Rookie", "Aether", "Starlight Purr", 42, 44, 52, 58, "gatomon"),
        RawWildDef(
            id = "terriermon",
            name = "Wyrmlet",
            stage = "Rookie",
            attribute = "Astral",
            move = "Astral Cyclone",
            atk = 50,
            def = 48,
            spd = 54,
            wis = 52,
            evoTargetId = "gargomon",
            abilityName = "Mystic Scales",
            abilityDesc = "Balanced Dragon Aura granting +15% All-Around Stamina Efficiency"
        ),
        RawWildDef(
            id = "renamon",
            name = "Vulpineon",
            stage = "Rookie",
            attribute = "Aether",
            move = "Starlight Swift",
            atk = 51,
            def = 43,
            spd = 58,
            wis = 54,
            evoTargetId = "kyubimon",
            abilityName = "Resonant Bond",
            abilityDesc = "+15% Bond Evolution Sync towards Solareon & Lunareon"
        ),
        RawWildDef("impmon", "Duskimp", "Rookie", "Shadow", "Umbral Ember", 56, 38, 57, 47, "wizardmon"),
        RawWildDef("wormmon", "Silklarva", "Rookie", "Flora", "Gossamer Snare", 43, 53, 49, 51, "stingmon"),
        RawWildDef("hawkmon", "Taloncrest", "Rookie", "Zephyr", "Sky Feather", 51, 43, 59, 45, "aquilamon"),
        RawWildDef("armadillomon", "Shellburrow", "Rookie", "Terra", "Quartz Roll", 45, 62, 39, 46, "ankylomon"),
        RawWildDef("monodramon", "Scaleclaw", "Rookie", "Astral", "Drake Knuckle", 57, 45, 54, 38, "strikedramon"),
        RawWildDef("elecmon", "Sparkmane", "Rookie", "Storm", "Thunder Flash", 50, 42, 55, 45, "leomon"),
        RawWildDef("betamon", "Reefnewt", "Rookie", "Aqua", "Current Jolt", 46, 48, 49, 50, "seadramon"),
        RawWildDef("gotsumon", "Pebblegolem", "Rookie", "Terra", "Boulder Fist", 52, 64, 36, 40, "monochromon"),
        RawWildDef("hagurumon", "Coglet", "Rookie", "Alloy", "Chrono Gear", 44, 56, 42, 54, "guardromon"),
        RawWildDef("keramon", "Gloomwisp", "Rookie", "Shadow", "Mirage Pulse", 59, 40, 56, 48, "chrysalimon"),
        RawWildDef("lopmon", "Frosthare", "Rookie", "Frost", "Rime Flurry", 45, 47, 54, 56, "turuiemon"),
        RawWildDef("commandramon", "Raptorscout", "Rookie", "Alloy", "Sonic Dart", 55, 49, 53, 41, "sealsdramon"),
        RawWildDef("dracomon", "Emberdrake", "Rookie", "Pyro", "Cinder Breath", 58, 50, 48, 42, "coredramon"),
        RawWildDef("hackmon", "Silverfang", "Rookie", "Alloy", "Steel Rush", 60, 51, 55, 44, "baohuckmon"),
        RawWildDef("zubamon", "Gildedblade", "Rookie", "Aether", "Sunburst Dive", 57, 52, 51, 40, "zubaeagermon"),
        RawWildDef("ludomon", "Bucklerpup", "Rookie", "Alloy", "Bastion Bash", 43, 65, 41, 47, "tia_ludomon"),
        RawWildDef("pulsemon", "Galestrider", "Rookie", "Storm", "Volt Sprint", 54, 44, 62, 42, "bulkmon"),
        RawWildDef(
            id = "gammamon",
            name = "Ursaflare",
            stage = "Rookie",
            attribute = "Terra",
            move = "Crescent Claw",
            atk = 56,
            def = 48,
            spd = 50,
            wis = 42,
            evoTargetId = "betelgammamon",
            abilityName = "Nimble Stride",
            abilityDesc = "+15% Dual Attack & Defense Power in Sanctuary & Reserve trials"
        ),
        RawWildDef("jellymon", "Coralbell", "Rookie", "Aqua", "TidalBell", 46, 43, 59, 57, "teslajellymon"),
        RawWildDef("angoramon", "Cloudmane", "Rookie", "Zephyr", "Cyclone Lariat", 51, 57, 45, 53, "symbareangoramon"),
        RawWildDef("loogamon", "Nightfang", "Rookie", "Shadow", "Howling Ember", 58, 45, 58, 43, "loogarmon"),

        // 27 Evolved / Champion Companions (#58 - #84)
        RawWildDef("kabuterimon", "Raijolt", "Champion", "Storm", "Thunder Lance", 92, 98, 96, 88, "megakabuterimon"),
        RawWildDef("togemon", "Cacthorne", "Champion", "Flora", "Briar Barrage", 90, 88, 82, 86, "lillymon"),
        RawWildDef("ikkakumon", "Tuskwalrus", "Champion", "Frost", "Glacier Horn", 94, 102, 70, 84, "zudomon"),
        RawWildDef("birdramon", "Sunhawk", "Champion", "Pyro", "Solar Wing", 96, 76, 104, 80, "garudamon"),
        RawWildDef("gatomon", "Sylphcat", "Champion", "Aether", "Moonbeam Claw", 86, 84, 98, 102, "angewomon"),
        RawWildDef("kyubimon", "Solareon", "Champion", "Aether", "Sunflare Orb", 95, 85, 105, 96, "taomon"),
        RawWildDef("wizardmon", "Arcanist", "Champion", "Aether", "Starlight Bolt", 88, 80, 92, 110, "myotismon"),
        RawWildDef("stingmon", "Emeraldmantis", "Champion", "Flora", "Thorny Skewer", 104, 82, 108, 78, "paildramon"),
        RawWildDef("aquilamon", "Stormroc", "Champion", "Zephyr", "Gale Dive", 98, 80, 106, 79, "garudamon"),
        RawWildDef("ankylomon", "Cragbastion", "Champion", "Terra", "Granite Tail", 89, 114, 68, 82, "zudomon"),
        RawWildDef("strikedramon", "Blazewyrm", "Champion", "Pyro", "Crimson Fang", 106, 86, 99, 72, "cyberdramon"),
        RawWildDef("leomon", "Sunmane", "Champion", "Aether", "Sovereign Roar", 105, 90, 88, 79, "canoweissmon"),
        RawWildDef("seadramon", "Tideserpent", "Champion", "Aqua", "Riptide Vortex", 91, 93, 86, 90, "zudomon"),
        RawWildDef("monochromon", "Obsidianhorn", "Champion", "Terra", "Tectonic Charge", 99, 112, 66, 74, "andromon"),
        RawWildDef("guardromon", "Aegisknight", "Champion", "Alloy", "Iron Salvo", 92, 116, 68, 86, "andromon"),
        RawWildDef("chrysalimon", "Phantomcocoon", "Champion", "Shadow", "Void Tendril", 102, 95, 84, 88, "infermon"),
        RawWildDef("turuiemon", "Glacierblade", "Champion", "Frost", "Frostbite Claw", 96, 88, 101, 87, "taomon"),
        RawWildDef("sealsdramon", "Stealthdrake", "Champion", "Alloy", "Shadow Edge", 107, 87, 103, 76, "cyberdramon"),
        RawWildDef("coredramon", "Sapphiredrake", "Champion", "Astral", "Azure Flare", 103, 96, 88, 81, "paildramon"),
        RawWildDef("baohuckmon", "Argentblade", "Champion", "Alloy", "Silver Cleave", 108, 94, 96, 80, "saviorhuckmon"),
        RawWildDef("zubaeagermon", "Aurumfang", "Champion", "Aether", "Golden Charge", 104, 92, 95, 78, "canoweissmon"),
        RawWildDef("tia_ludomon", "Aegiswolf", "Champion", "Frost", "Glacial Shield", 86, 118, 76, 85, "andromon"),
        RawWildDef("bulkmon", "Thunderbrawl", "Champion", "Storm", "Tempest Fist", 106, 94, 98, 75, "megakabuterimon"),
        RawWildDef("betelgammamon", "Ursatitan", "Champion", "Terra", "Meteor Maul", 105, 95, 90, 79, "canoweissmon"),
        RawWildDef("teslajellymon", "Sirenreef", "Champion", "Aqua", "Abyssal Pulse", 93, 86, 105, 98, "angewomon"),
        RawWildDef("symbareangoramon", "Zephyrlord", "Champion", "Zephyr", "Tempest Gale", 97, 96, 99, 92, "garudamon"),
        RawWildDef("loogarmon", "Shadowwolf", "Champion", "Shadow", "Dusk Howl", 108, 89, 102, 82, "soloogarmon"),

        // 16 Ascended / Ultimate & Mega Companions (#85 - #100)
        RawWildDef("megakabuterimon", "Thunderhorn", "Ultimate", "Storm", "Fulmination Horn", 156, 168, 132, 138, "herculeskabuterimon"),
        RawWildDef("lillymon", "Blossomqueen", "Ultimate", "Flora", "Petal Hurricane", 148, 132, 164, 152, "seraphimon"),
        RawWildDef("zudomon", "Glaciermaul", "Ultimate", "Frost", "Permafrost Hammer", 166, 174, 118, 136, "metalgarurumon"),
        RawWildDef("garudamon", "Solargriffin", "Ultimate", "Pyro", "Sunfire Talon", 168, 138, 170, 132, "wargreymon"),
        RawWildDef("angewomon", "Valkyria", "Ultimate", "Aether", "Starlight Arrow", 150, 146, 158, 178, "seraphimon"),
        RawWildDef("taomon", "Lunareon", "Ultimate", "Shadow", "Eclipse Mirage", 152, 148, 160, 176, "seraphimon"),
        RawWildDef("myotismon", "Nocturnelord", "Ultimate", "Shadow", "Crimson Eclipse", 168, 152, 148, 166, "gallantmon"),
        RawWildDef("paildramon", "Aetherwyrm", "Ultimate", "Astral", "Twin Star Cannon", 174, 150, 166, 134, "wargreymon"),
        RawWildDef("cyberdramon", "Chronodrake", "Ultimate", "Astral", "Temporal Lance", 176, 154, 158, 126, "wargreymon"),
        RawWildDef("andromon", "Titanguard", "Ultimate", "Alloy", "Aegis Blade", 164, 172, 134, 148, "metalgarurumon"),
        RawWildDef("infermon", "Abysscrawler", "Ultimate", "Shadow", "Nether Barrage", 172, 146, 162, 144, "gallantmon"),
        RawWildDef("saviorhuckmon", "Paladinwyrm", "Ultimate", "Alloy", "Argent Saber", 178, 156, 164, 136, "jesmon"),
        RawWildDef("canoweissmon", "Astralbear", "Ultimate", "Terra", "Supernova Roar", 175, 155, 162, 140, "wargreymon"),
        RawWildDef("soloogarmon", "Fenrirflame", "Ultimate", "Shadow", "Hellfire Maw", 179, 150, 168, 138, "gallantmon"),
        RawWildDef("herculeskabuterimon", "Fulminarch", "Mega", "Storm", "Heavenly Bolt", 248, 255, 220, 215, "omegamon_x"),
        RawWildDef("jesmon", "Aetherpaladin", "Mega", "Alloy", "Starlight Judgment", 265, 240, 250, 215, "alphamon_ouryuken")
    )

    val extra71SpeciesSpecs: List<DigimonSpeciesSpec> by lazy {
        val palettes = listOf(
            0xFF00F0FFL to 0xFFFFB800L,
            0xFFFF7043L to 0xFF38BDF8L,
            0xFF10B981L to 0xFFFACC15L,
            0xFFA855F7L to 0xFF38BDF8L,
            0xFFEF4444L to 0xFFF59E0BL
        )
        val rawById = raw71Definitions.associateBy { it.id }
        val baseCatalogNames = mapOf(
            "greymon" to "Pyroquill",
            "geogreymon" to "Flareburst",
            "tyrannomon" to "Magmasaur",
            "garurumon" to "Jawcurrent",
            "dorugamon" to "Shellcrag",
            "angemon" to "Blossomane",
            "gargomon" to "Skyserpent",
            "growlmon" to "Cerberux",
            "metalgreymon" to "Vulcanis",
            "rizegreymon" to "Solarflare",
            "weregarurumon" to "Leviathor",
            "magnaangemon" to "Solarchid",
            "wargrowlmon" to "Dreadhound",
            "wargreymon" to "Ignisovereign",
            "metalgarurumon" to "Titanorex",
            "seraphimon" to "Aetherdrake",
            "gallantmon" to "Netherlord",
            "omegamon_x" to "Solphoenix",
            "alphamon_ouryuken" to "Thalassor"
        )
        val megaPool = listOf(
            "herculeskabuterimon" to "Fulminarch",
            "jesmon" to "Aetherpaladin",
            "wargreymon" to "Ignisovereign",
            "metalgarurumon" to "Titanorex",
            "seraphimon" to "Aetherdrake",
            "gallantmon" to "Netherlord"
        )
        val ultimatePool = listOf(
            "megakabuterimon",
            "lillymon",
            "zudomon",
            "garudamon",
            "angewomon",
            "taomon",
            "myotismon",
            "paildramon",
            "cyberdramon",
            "andromon",
            "infermon",
            "saviorhuckmon",
            "canoweissmon",
            "soloogarmon",
            "metalgreymon",
            "weregarurumon",
            "magnaangemon",
            "wargrowlmon"
        )
        raw71Definitions.mapIndexed { idx, raw ->
            val (c1, c2) = palettes[idx % palettes.size]
            val reqLvl = when (raw.stage) {
                "Rookie" -> 20
                "Champion" -> 40
                "Ultimate" -> 65
                else -> 100
            }
            val nextStage = when (raw.stage) {
                "Rookie" -> "Champion"
                "Champion" -> "Ultimate"
                "Ultimate" -> "Mega"
                else -> "Mega"
            }
            val fallbackTargetId = when (raw.stage) {
                "Rookie" -> if (rawById.containsKey(raw.evoTargetId) || baseCatalogNames.containsKey(raw.evoTargetId)) {
                    raw.evoTargetId
                } else {
                    "greymon"
                }
                "Champion" -> if (rawById.containsKey(raw.evoTargetId) || baseCatalogNames.containsKey(raw.evoTargetId)) {
                    raw.evoTargetId
                } else {
                    ultimatePool[idx % ultimatePool.size]
                }
                "Ultimate" -> if (rawById.containsKey(raw.evoTargetId) || baseCatalogNames.containsKey(raw.evoTargetId)) {
                    raw.evoTargetId
                } else {
                    megaPool[idx % megaPool.size].first
                }
                else -> "omegamon_x"
            }
            val targetName = rawById[fallbackTargetId]?.name
                ?: baseCatalogNames[fallbackTargetId]
                ?: "Solphoenix"

            val branches = if (raw.stage == "Mega") {
                DigimonCatalog.level100RareBranches
            } else {
                listOf(
                    EvolutionBranchRequirement(
                        targetSpeciesId = fallbackTargetId,
                        targetSpeciesName = targetName,
                        targetStage = nextStage,
                        targetAttribute = raw.attribute,
                        minAttack = raw.atk + 18,
                        minDefense = raw.def + 14,
                        minSpeed = raw.spd + 14,
                        minWisdom = raw.wis + 12,
                        minBond = if (reqLvl == 20) 35 else if (reqLvl == 40) 55 else 75,
                        requiredLevel = reqLvl,
                        branchDescription = "Ascends into $targetName ($nextStage). Requires LVL $reqLvl+, 100% Sync, and trained stats."
                    )
                )
            }

            DigimonSpeciesSpec(
                speciesId = raw.id,
                speciesName = raw.name,
                stage = raw.stage,
                attribute = raw.attribute,
                titleTag = "Dual-Realm Dex #${30 + idx} • ${raw.stage} Class",
                signatureMove = raw.move,
                primaryColorHex = c1,
                secondaryColorHex = c2,
                baseAttack = raw.atk,
                baseDefense = raw.def,
                baseSpeed = raw.spd,
                baseWisdom = raw.wis,
                apexTargetName = targetName,
                isRareLevel100Spec = false,
                starterAbilityName = raw.abilityName,
                starterAbilityDescription = raw.abilityDesc,
                evolutionBranches = branches
            )
        }
    }
}
