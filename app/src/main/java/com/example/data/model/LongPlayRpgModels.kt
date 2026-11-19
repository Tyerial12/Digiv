package com.example.data.model

/**
 * Visual World Map Regions across Aurelia (West) and Argentia (East).
 */
data class WorldRegionBiome(
    val regionId: String,
    val realmName: String,
    val regionTitle: String,
    val subtitle: String,
    val primaryAttribute: String,
    val secondaryAttribute: String,
    val recommendedLevelRange: String,
    val weatherLabel: String,
    val weatherEffectSummary: String,
    val featuredSpeciesIds: List<String>,
    val linkedSanctumBadgeNumbers: List<Int>,
    val accentColorHex: Long
)

enum class TurnBasedBattleMode(val displayName: String) {
    WILD_ENCOUNTER("WILD ENCOUNTER"),
    SANCTUM_LEADER("SANCTUM CREST TRIAL"),
    FRONTIER_TOWER("BATTLE FRONTIER TOWER")
}

data class TurnBasedBattleState(
    val battleId: String,
    val mode: TurnBasedBattleMode,
    val regionId: String = "aurelia_meadows",
    val turnNumber: Int = 1,
    val enemyName: String,
    val enemyTitle: String,
    val enemySpeciesId: String,
    val enemyStage: String,
    val enemyAttribute: String,
    val enemyLevel: Int,
    val enemyCurrentHp: Int,
    val enemyMaxHp: Int,
    val enemyAttack: Int,
    val enemyDefense: Int,
    val enemySpeed: Int,
    val enemyStatusEffect: WildStatusEffect = WildStatusEffect.NONE,
    val isChromaticShiny: Boolean = false,
    val isBossTier: Boolean = false,
    val rarityTier: WildRarityTier = WildRarityTier.COMMON,
    val baseCaptureRatePercent: Int = 45,
    val movePpRemaining: Map<Int, Int> = mapOf(0 to 15, 1 to 25, 2 to 15, 3 to 10),
    val playerTempAtkStage: Int = 0,
    val playerTempDefStage: Int = 0,
    val playerTempSpdStage: Int = 0,
    val combatLog: List<String> = emptyList(),
    val lastActionHeadline: String = "Battle Commenced!",
    val lastActionDetail: String = "Choose one of your 4 tactical moves, switch companions, or throw an Acorn Orb.",
    val lastEffectivenessMultiplier: Float = 1.0f,
    val isFinished: Boolean = false,
    val playerWon: Boolean = false,
    val capturedWild: Boolean = false,
    val sanctumBadgeNumber: Int = 0,
    val frontierFloorNumber: Int = 0,
    val rewardCoinsEarned: Int = 0,
    val rewardBpEarned: Int = 0
) {
    val enemyHpPercent: Int
        get() = ((enemyCurrentHp.toFloat() / enemyMaxHp.coerceAtLeast(1).toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 100)

    fun playerActsFirst(playerCreature: DigimonCreature, speedRuneBonusPercent: Int = 0): Boolean {
        val stageMult = 1f + (playerTempSpdStage * 0.15f)
        val playerSpd = (playerCreature.effectiveSpeed() * stageMult * (1f + speedRuneBonusPercent / 100f)).toInt()
        val enemySpd = if (enemyStatusEffect == WildStatusEffect.PARALYZED) {
            (enemySpeed * 0.55f).toInt()
        } else {
            enemySpeed
        }
        return playerSpd >= enemySpd
    }

    fun weakenedCaptureBonusPercent(): Int = when {
        enemyHpPercent in 1..20 -> 35
        enemyHpPercent in 21..50 -> 22
        enemyHpPercent in 51..75 -> 12
        else -> 0
    }
}

/**
 * Sanctuary Daycare Breeding & Starlight Chromatic (Shiny) Hunting State.
 */
data class DaycareBreedingState(
    val parentOneId: String = "",
    val parentOneName: String = "",
    val parentOneSpeciesId: String = "",
    val parentOneAttribute: String = "",
    val parentOneShiny: Boolean = false,
    val parentTwoId: String = "",
    val parentTwoName: String = "",
    val parentTwoSpeciesId: String = "",
    val parentTwoAttribute: String = "",
    val parentTwoShiny: Boolean = false,
    val hasActiveEgg: Boolean = false,
    val offspringSpeciesId: String = "patamon",
    val offspringSpeciesName: String = "Sproutling",
    val inheritedNatureCode: String = "ADM",
    val inheritedBonusStats: Int = 18,
    val inheritedEggMove: String = "Starlight Resonance",
    val chromaticShinyOddsPercent: Int = 15,
    val currentIncubationPoints: Int = 0,
    val targetIncubationPoints: Int = 100,
    val totalEggsHatched: Int = 0,
    val totalShinyHatched: Int = 0
) {
    val incubationPercent: Int
        get() = if (!hasActiveEgg || targetIncubationPoints <= 0) 0
        else ((currentIncubationPoints * 100) / targetIncubationPoints).coerceIn(0, 100)

    val isReadyToHatch: Boolean
        get() = hasActiveEgg && currentIncubationPoints >= targetIncubationPoints

    val compatibilityLabel: String
        get() = when {
            parentOneId.isBlank() -> "Select a Lead Parent to Begin Breeding"
            parentOneAttribute.equals(parentTwoAttribute, ignoreCase = true) ->
                "RADIANT ELEMENTAL HARMONY (+25 Bonus IVs & +12% Shiny Odds)"
            else ->
                "DUAL-ELEMENT SYNERGY (+18 Bonus IVs & Hybrid Egg Move)"
        }
}

/**
 * Battle Frontier Tower State & BP Exchange Items.
 */
data class FrontierTowerState(
    val currentFloor: Int = 1,
    val highestFloorReached: Int = 1,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val battlePoints: Int = 25,
    val totalBossesDefeated: Int = 0,
    val shinyCharmUnlocked: Boolean = false
) {
    val isBossFloor: Boolean
        get() = currentFloor % 5 == 0
}

enum class FrontierBpShopItem(
    val itemId: String,
    val title: String,
    val subtitle: String,
    val bpCost: Int,
    val badgeTag: String
) {
    SHINY_STARLIGHT_CHARM(
        itemId = "bp_shiny_charm",
        title = "Starlight Chromatic Charm",
        subtitle = "Permanent +15% Starlight Chromatic (Shiny) odds in Wild & Daycare Eggs!",
        bpCost = 40,
        badgeTag = "PERMANENT CHARM"
    ),
    LUNAR_ORB_BUNDLE(
        itemId = "bp_lunar_orbs",
        title = "3x Lunar Moon Orbs",
        subtitle = "Adds +3 Lunar Moon Orbs (+25% Capture vs Rare & Chromatic monsters).",
        bpCost = 18,
        badgeTag = "x3 ORBS"
    ),
    RELIC_FORGE_CORE(
        itemId = "bp_relic_shards",
        title = "5x Relic Shards & 10x Gilded Acorns",
        subtitle = "Crafting bundle for upgrading Held Relics to +5 and forging Resonance Runes.",
        bpCost = 22,
        badgeTag = "FORGE BUNDLE"
    ),
    SOVEREIGN_STAT_TOME(
        itemId = "bp_stat_tome",
        title = "Sovereign Ascendant Tome",
        subtitle = "Grants +6 ALL STATS (ATK/DEF/SPD/WIS) & +25% Crest Sync to active companion.",
        bpCost = 30,
        badgeTag = "+24 TOTAL STATS"
    )
}

/**
 * Daily & Weekly Tamer Quests + Milestone Reward Pass.
 */
enum class TamerQuestType {
    WIN_BATTLES,
    CATCH_WILD,
    HATCH_OR_NURTURE_EGG,
    FORGE_OR_UPGRADE_RELIC,
    CLIMB_FRONTIER_TOWER,
    DEFEAT_BOSS_OR_LEADER
}

data class TamerQuestItem(
    val questId: String,
    val title: String,
    val description: String,
    val questType: TamerQuestType,
    val isWeekly: Boolean,
    val currentProgress: Int = 0,
    val targetProgress: Int,
    val rewardCoins: Int,
    val rewardRelicShards: Int,
    val rewardBp: Int,
    val rewardPassXp: Int,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean
        get() = currentProgress >= targetProgress

    val progressFraction: Float
        get() = (currentProgress.toFloat() / targetProgress.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

data class TamerPassState(
    val totalPassXp: Int = 0,
    val claimedMilestoneTiers: Set<Int> = emptySet()
) {
    val currentTier: Int
        get() = (1 + (totalPassXp / 100)).coerceIn(1, 20)

    val xpInCurrentTier: Int
        get() = totalPassXp % 100
}

/**
 * Relic Crafting Forge (+1..+5 Upgrades) & Socketable Resonance Runes.
 */
enum class ResonanceRune(
    val runeCode: String,
    val displayName: String,
    val shortBadge: String,
    val effectSummary: String,
    val craftGildedAcornsCost: Int,
    val craftRelicShardsCost: Int,
    val atkPctBonus: Int = 0,
    val defPctBonus: Int = 0,
    val spdPctBonus: Int = 0,
    val wisPctBonus: Int = 0,
    val lifestealPercent: Int = 0,
    val critChancePercent: Int = 0,
    val captureAndShinyBonusPercent: Int = 0,
    val colorHex: Long
) {
    SOLAR_CRIT(
        runeCode = "RUNE_CRIT",
        displayName = "Solar Crit Rune",
        shortBadge = "☀️ +20% CRIT",
        effectSummary = "+15% Attack Power & +20% Critical Hit Surge in Turn-Based Battles.",
        craftGildedAcornsCost = 3,
        craftRelicShardsCost = 1,
        atkPctBonus = 15,
        critChancePercent = 20,
        colorHex = 0xFFFFB300
    ),
    GALE_SWIFTNESS(
        runeCode = "RUNE_SPD",
        displayName = "Gale Swiftness Rune",
        shortBadge = "⚡ +20% SPD",
        effectSummary = "+20% Battle Speed for Turn-Order Priority & +10% Dodge Grace.",
        craftGildedAcornsCost = 3,
        craftRelicShardsCost = 1,
        spdPctBonus = 20,
        colorHex = 0xFF00E5FF
    ),
    VAMPIRIC_PULSE(
        runeCode = "RUNE_VAMP",
        displayName = "Vampiric Pulse Rune",
        shortBadge = "💚 18% LIFESTEAL",
        effectSummary = "Restores 18% of all turn-based attack damage dealt directly to Companion HP.",
        craftGildedAcornsCost = 4,
        craftRelicShardsCost = 1,
        atkPctBonus = 8,
        lifestealPercent = 18,
        colorHex = 0xFF00E676
    ),
    AEGIS_WARD(
        runeCode = "RUNE_WARD",
        displayName = "Aegis Bastion Rune",
        shortBadge = "🛡️ +22% GUARD",
        effectSummary = "+22% Defense & Wisdom Guard; reduces incoming Boss & Tower damage.",
        craftGildedAcornsCost = 3,
        craftRelicShardsCost = 1,
        defPctBonus = 22,
        wisPctBonus = 15,
        colorHex = 0xFF38BDF8
    ),
    STARLIGHT_LURE(
        runeCode = "RUNE_SHINY",
        displayName = "Starlight Chromatic Rune",
        shortBadge = "✨ +15% SHINY/CAP",
        effectSummary = "+15% Acorn Orb Capture Rate & +15% Starlight Chromatic Shiny encounter odds.",
        craftGildedAcornsCost = 3,
        craftRelicShardsCost = 2,
        captureAndShinyBonusPercent = 15,
        colorHex = 0xFFA855F7
    ),
    SOVEREIGN_CREST(
        runeCode = "RUNE_SOV",
        displayName = "Sovereign Omnicrest Rune",
        shortBadge = "👑 +12% ALL",
        effectSummary = "+12% All Combat Stats (ATK/DEF/SPD/WIS) & +10% Critical Hit Chance.",
        craftGildedAcornsCost = 5,
        craftRelicShardsCost = 2,
        atkPctBonus = 12,
        defPctBonus = 12,
        spdPctBonus = 12,
        wisPctBonus = 12,
        critChancePercent = 10,
        colorHex = 0xFFFF5252
    );

    companion object {
        fun fromCode(code: String?): ResonanceRune? =
            entries.firstOrNull { it.runeCode.equals(code?.trim(), ignoreCase = true) }
    }
}

data class ForgedRelicUpgradeState(
    val relicCode: String,
    val upgradeLevel: Int = 0, // 0..5
    val socketOneRuneCode: String? = null,
    val socketTwoRuneCode: String? = null
) {
    fun resolvedRelic(): HeldRelicItem = HeldRelicItem.fromCode(relicCode)
    fun resolvedRuneOne(): ResonanceRune? = ResonanceRune.fromCode(socketOneRuneCode)
    fun resolvedRuneTwo(): ResonanceRune? = ResonanceRune.fromCode(socketTwoRuneCode)

    fun totalBonusAtkPct(): Int {
        val base = resolvedRelic().atkBonusPercent
        val upg = upgradeLevel * 5
        val r1 = resolvedRuneOne()?.atkPctBonus ?: 0
        val r2 = resolvedRuneTwo()?.atkPctBonus ?: 0
        return base + upg + r1 + r2
    }

    fun totalBonusDefPct(): Int {
        val base = resolvedRelic().defBonusPercent
        val upg = upgradeLevel * 5
        val r1 = resolvedRuneOne()?.defPctBonus ?: 0
        val r2 = resolvedRuneTwo()?.defPctBonus ?: 0
        return base + upg + r1 + r2
    }

    fun totalBonusSpdPct(): Int {
        val base = resolvedRelic().spdBonusPercent
        val upg = upgradeLevel * 5
        val r1 = resolvedRuneOne()?.spdPctBonus ?: 0
        val r2 = resolvedRuneTwo()?.spdPctBonus ?: 0
        return base + upg + r1 + r2
    }

    fun totalBonusWisPct(): Int {
        val base = resolvedRelic().wisBonusPercent
        val upg = upgradeLevel * 5
        val r1 = resolvedRuneOne()?.wisPctBonus ?: 0
        val r2 = resolvedRuneTwo()?.wisPctBonus ?: 0
        return base + upg + r1 + r2
    }

    fun totalLifestealPct(): Int =
        (resolvedRuneOne()?.lifestealPercent ?: 0) + (resolvedRuneTwo()?.lifestealPercent ?: 0)

    fun totalCritChancePct(): Int =
        5 + (upgradeLevel * 2) + (resolvedRuneOne()?.critChancePercent ?: 0) + (resolvedRuneTwo()?.critChancePercent ?: 0)

    fun totalCaptureAndShinyBonusPct(): Int =
        resolvedRelic().captureBonusPercent + (upgradeLevel * 2) +
            (resolvedRuneOne()?.captureAndShinyBonusPercent ?: 0) +
            (resolvedRuneTwo()?.captureAndShinyBonusPercent ?: 0)
}

object LongPlayRpgCatalog {

    val worldRegions: List<WorldRegionBiome> = listOf(
        WorldRegionBiome(
            regionId = "aurelia_meadows",
            realmName = "Aurelia",
            regionTitle = "Sunleaf Meadows & Zephyr Roost",
            subtitle = "Breezy golden fields and ancient wind towers teeming with Flora & Gale companions.",
            primaryAttribute = "Flora",
            secondaryAttribute = "Gale",
            recommendedLevelRange = "LVL 1–18",
            weatherLabel = "☀️ Sunlit Breeze",
            weatherEffectSummary = "+15% Flora & Gale Move Power • High Rookie Spawn Rate",
            featuredSpeciesIds = listOf("patamon", "hawkmon", "tentomon", "salamon"),
            linkedSanctumBadgeNumbers = listOf(1, 2),
            accentColorHex = 0xFF10B981
        ),
        WorldRegionBiome(
            regionId = "aurelia_goldcrest",
            realmName = "Aurelia",
            regionTitle = "Goldcrest Plaza & Duskbell Woods",
            subtitle = "Bustling sun-paved boulevard bordering the twilight phantom forest.",
            primaryAttribute = "Shadow",
            secondaryAttribute = "Aether",
            recommendedLevelRange = "LVL 15–32",
            weatherLabel = "🌙 Twilight Mist",
            weatherEffectSummary = "+15% Shadow & Astral Power • +5% Chromatic Odds",
            featuredSpeciesIds = listOf("renamon", "guilmon", "bakemon", "gargomon"),
            linkedSanctumBadgeNumbers = listOf(3, 4),
            accentColorHex = 0xFFA855F7
        ),
        WorldRegionBiome(
            regionId = "aurelia_surgetide",
            realmName = "Aurelia",
            regionTitle = "Surgetide Coast & Beacon Port",
            subtitle = "Storm-lashed lighthouse cliffs and tidal whirlpool coves.",
            primaryAttribute = "Aqua",
            secondaryAttribute = "Storm",
            recommendedLevelRange = "LVL 24–45",
            weatherLabel = "⛈️ Coastal Squall",
            weatherEffectSummary = "+18%Cobalt Lure Orb Rate • +15% Aqua & Storm Moves",
            featuredSpeciesIds = listOf("gabumon", "veemon", "garurumon", "guardromon"),
            linkedSanctumBadgeNumbers = listOf(5, 6),
            accentColorHex = 0xFF00E5FF
        ),
        WorldRegionBiome(
            regionId = "aurelia_wyrmfang",
            realmName = "Aurelia",
            regionTitle = "Rimefrost Tarn & Wyrmfang Den",
            subtitle = "Glacial mountain caverns guarding the ancient Aurelia Dragon Shrine.",
            primaryAttribute = "Frost",
            secondaryAttribute = "Aether",
            recommendedLevelRange = "LVL 35–55",
            weatherLabel = "❄️ Diamond Dust",
            weatherEffectSummary = "+15% Frost & Aether Power • Rare Stage-2 Dragons",
            featuredSpeciesIds = listOf("terriermon", "dorumon", "weregarurumon", "magnaangemon"),
            linkedSanctumBadgeNumbers = listOf(7, 8),
            accentColorHex = 0xFF38BDF8
        ),
        WorldRegionBiome(
            regionId = "argentia_granite",
            realmName = "Argentia",
            regionTitle = "Granite Crag & Cerulean Cascade",
            subtitle = "Eastern realm rock citadels and thundering sapphire waterfalls.",
            primaryAttribute = "Terra",
            secondaryAttribute = "Aqua",
            recommendedLevelRange = "LVL 45–68",
            weatherLabel = "🪨 Tectonic Resonance",
            weatherEffectSummary = "+20% Aureate Heavy Orb Capture Rate • +15% DEF",
            featuredSpeciesIds = listOf("gammamon", "dorugamon", "tyrannomon", "metalgarurumon"),
            linkedSanctumBadgeNumbers = listOf(9, 10),
            accentColorHex = 0xFFF59E0B
        ),
        WorldRegionBiome(
            regionId = "argentia_thunderport",
            realmName = "Argentia",
            regionTitle = "Thunderport Bay & Prism Gardens",
            subtitle = "High-voltage harbor grid and luminescent botanical conservatory.",
            primaryAttribute = "Storm",
            secondaryAttribute = "Flora",
            recommendedLevelRange = "LVL 58–78",
            weatherLabel = "⚡ Ionized Bloom",
            weatherEffectSummary = "+15% Speed & Critical Surge • +18% Crimson Fast Orb Rate",
            featuredSpeciesIds = listOf("geogreymon", "angemon", "rizegreymon", "seraphimon"),
            linkedSanctumBadgeNumbers = listOf(11, 12),
            accentColorHex = 0xFFFACC15
        ),
        WorldRegionBiome(
            regionId = "argentia_astral",
            realmName = "Argentia",
            regionTitle = "Nightshade Ward & Astral Spire",
            subtitle = "Mystic starlight observatory rising above the neon shadow district.",
            primaryAttribute = "Astral",
            secondaryAttribute = "Venom",
            recommendedLevelRange = "LVL 70–88",
            weatherLabel = "✨ Starlight Surge",
            weatherEffectSummary = "+25% Lunar Moon Orb Bonus • 2x Chromatic Encounter Rate",
            featuredSpeciesIds = listOf("growlmon", "wargrowlmon", "magnaangemon", "gallantmon"),
            linkedSanctumBadgeNumbers = listOf(13, 14),
            accentColorHex = 0xFFEC4899
        ),
        WorldRegionBiome(
            regionId = "argentia_silver_peak",
            realmName = "Argentia",
            regionTitle = "Caldera Bastion & Silver Peak Summit",
            subtitle = "Volcanic fortress leading to Sovereign Red's dual-realm apex throne.",
            primaryAttribute = "Pyro",
            secondaryAttribute = "Aether",
            recommendedLevelRange = "LVL 85–100",
            weatherLabel = "🔥 Solar Corona",
            weatherEffectSummary = "+20% All Elemental Damage • Apex & Sovereign Encounters",
            featuredSpeciesIds = listOf("agumon", "metalgreymon", "wargreymon", "gallantmon"),
            linkedSanctumBadgeNumbers = listOf(15, 16),
            accentColorHex = 0xFFEF4444
        )
    )

    fun defaultTamerQuests(): List<TamerQuestItem> = listOf(
        TamerQuestItem(
            questId = "q_daily_battles",
            title = "Tactical Arena Vanguard",
            description = "Win 3 interactive turn-based battles in the World Arena or Sanctums.",
            questType = TamerQuestType.WIN_BATTLES,
            isWeekly = false,
            targetProgress = 3,
            rewardCoins = 35,
            rewardRelicShards = 1,
            rewardBp = 10,
            rewardPassXp = 45
        ),
        TamerQuestItem(
            questId = "q_daily_catch",
            title = "Emerald Reserve Ranger",
            description = "Capture 2 wild companions using any handcrafted Acorn Orb.",
            questType = TamerQuestType.CATCH_WILD,
            isWeekly = false,
            targetProgress = 2,
            rewardCoins = 40,
            rewardRelicShards = 1,
            rewardBp = 12,
            rewardPassXp = 50
        ),
        TamerQuestItem(
            questId = "q_daily_daycare",
            title = "Sanctuary Egg Caretaker",
            description = "Nurture or hatch 2 Daycare Egg incubation milestones.",
            questType = TamerQuestType.HATCH_OR_NURTURE_EGG,
            isWeekly = false,
            targetProgress = 2,
            rewardCoins = 30,
            rewardRelicShards = 1,
            rewardBp = 10,
            rewardPassXp = 40
        ),
        TamerQuestItem(
            questId = "q_daily_forge",
            title = "Relic Artificer's Anvil",
            description = "Upgrade a Held Relic or socket a Resonance Rune in the Relic Forge.",
            questType = TamerQuestType.FORGE_OR_UPGRADE_RELIC,
            isWeekly = false,
            targetProgress = 1,
            rewardCoins = 35,
            rewardRelicShards = 2,
            rewardBp = 10,
            rewardPassXp = 45
        ),
        TamerQuestItem(
            questId = "q_weekly_frontier",
            title = "Frontier Tower Ascendant",
            description = "Clear 3 floors in the endless Battle Frontier Tower gauntlet.",
            questType = TamerQuestType.CLIMB_FRONTIER_TOWER,
            isWeekly = true,
            targetProgress = 3,
            rewardCoins = 85,
            rewardRelicShards = 3,
            rewardBp = 25,
            rewardPassXp = 100
        ),
        TamerQuestItem(
            questId = "q_weekly_bosses",
            title = "Dual-Realm Crest Conqueror",
            description = "Defeat 2 Sanctum Leaders or Battle Frontier Warden Bosses.",
            questType = TamerQuestType.DEFEAT_BOSS_OR_LEADER,
            isWeekly = true,
            targetProgress = 2,
            rewardCoins = 100,
            rewardRelicShards = 4,
            rewardBp = 30,
            rewardPassXp = 120
        )
    )

    fun buildFrontierOpponentForFloor(floor: Int): TurnBasedBattleState {
        val isBoss = floor % 5 == 0
        val bossNames = listOf(
            "Frontier Warden Kael",
            "Grand Tactician Vespera",
            "Iron Bastion Thorne",
            "Starlight Empress Solaris",
            "Sovereign Arbiter Orion"
        )
        val specs = DigimonCatalog.allSpeciesSpecs.filter {
            when {
                floor >= 20 -> it.stage == "Mega"
                floor >= 10 -> it.stage == "Ultimate" || it.stage == "Mega"
                floor >= 5 -> it.stage == "Champion" || it.stage == "Ultimate"
                else -> it.stage == "Rookie" || it.stage == "Champion"
            }
        }.ifEmpty { DigimonCatalog.allSpeciesSpecs }

        val chosenSpec = specs[(floor * 7) % specs.size]
        val enemyLvl = (floor * 4 + if (isBoss) 6 else 2).coerceIn(5, 100)
        val maxHp = 110 + (floor * 24) + (if (isBoss) 85 else 0)
        val atk = chosenSpec.baseAttack + (floor * 7) + (if (isBoss) 25 else 0)
        val def = chosenSpec.baseDefense + (floor * 6) + (if (isBoss) 20 else 0)
        val spd = chosenSpec.baseSpeed + (floor * 5) + (if (isBoss) 15 else 0)
        val wardenName = bossNames[(floor / 5) % bossNames.size]

        return TurnBasedBattleState(
            battleId = "frontier_floor_$floor",
            mode = TurnBasedBattleMode.FRONTIER_TOWER,
            regionId = "frontier_tower",
            turnNumber = 1,
            enemyName = if (isBoss) "$wardenName's ${chosenSpec.speciesName}" else "Tower ${chosenSpec.speciesName}",
            enemyTitle = if (isBoss) "FLOOR $floor WARDEN BOSS • 2x BP" else "FRONTIER FLOOR $floor CHALLENGER",
            enemySpeciesId = chosenSpec.speciesId,
            enemyStage = chosenSpec.stage,
            enemyAttribute = chosenSpec.attribute,
            enemyLevel = enemyLvl,
            enemyCurrentHp = maxHp,
            enemyMaxHp = maxHp,
            enemyAttack = atk,
            enemyDefense = def,
            enemySpeed = spd,
            isChromaticShiny = isBoss && floor >= 15,
            isBossTier = isBoss,
            rarityTier = chosenSpec.wildRarityTier(),
            combatLog = listOf(
                if (isBoss) "⚠️ FRONTIER WARDEN BOSS! $wardenName deployed ${chosenSpec.speciesName} (LVL $enemyLvl)!"
                else "⚔️ Frontier Floor $floor: Challenger ${chosenSpec.speciesName} (LVL $enemyLvl) entered the arena!"
            ),
            lastActionHeadline = if (isBoss) "WARDEN BOSS FLOOR $floor!" else "FRONTIER FLOOR $floor READY",
            lastActionDetail = "Select one of your 4 moves or switch companions to exploit ${chosenSpec.attribute} weaknesses (${ElementalMatchupEngine.weakAgainstSummary(chosenSpec.attribute)}).",
            frontierFloorNumber = floor
        )
    }
}
