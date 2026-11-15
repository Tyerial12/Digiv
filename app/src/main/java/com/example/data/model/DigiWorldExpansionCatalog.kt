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

data class WildEncounterState(
    val speciesId: String,
    val speciesName: String,
    val stage: String,
    val attribute: String,
    val wildLevel: Int,
    val baseSuccessRatePercent: Int,
    val tameProgress: Int = 0,
    val berriesUsed: Int = 0,
    val lastRollOutcomeTitle: String = "Wild Monster Spotted!",
    val lastRollOutcomeDetail: String = "Throw a Tamming Berry (2 DigiCoins) to roll a random taming chance!",
    val isTamed: Boolean = false
)

object DigiWorldExpansionCatalog {

    // 10 Distinct Expeditions with different completion times (up to 24 Hours; ONLY the 24h expedition brings back 100 DigiCoins)
    val allExpeditions: List<ExpeditionSpec> = listOf(
        ExpeditionSpec(
            id = "exp_01_recon",
            number = 1,
            title = "Sector-01 Recon Patrol",
            subtitle = "Perimeter data sweep. Requires light food & sleep vigilance.",
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
            title = "Binary Forest Foraging",
            subtitle = "Track wild data signatures through dense emerald code-trees.",
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
            title = "Neon Canyon Survey",
            subtitle = "Navigate high-voltage rift valleys and mineral nodes.",
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
            title = "Chrome Mine Excavation",
            subtitle = "Deep Chrome-Digizoid vein extraction requiring steady stamina.",
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
            title = "Crystal Net-Ocean Dive",
            subtitle = "Submerged data trench exploration with pressure currents.",
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
            title = "Firewall Bastion Siege",
            subtitle = "Breach heavily armored security sectors; monitor food & rest.",
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
            title = "Sky-Server Cloud Ascent",
            subtitle = "High-altitude orbital relay climb demanding food and sleep care.",
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
            title = "Dark Web Labyrinth",
            subtitle = "Multi-hour endurance trek through shifting encrypted mazes.",
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
            title = "Core Sanctum Crusade",
            subtitle = "Ancient Royal citadel march; requires periodic meals and Cryo-Rest.",
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
            title = "Digital Abyss Odyssey (24h)",
            subtitle = "Full 24-Hour ultimate expedition! Only expedition that brings back 100 DigiCoins.",
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

    // Generates a unique, species-specific 6-node Skill Tree for each of the 100 monsters
    fun buildSkillTreeForSpecies(spec: DigimonSpeciesSpec): MonsterSkillTreeSpec {
        val seed = spec.speciesId.hashCode().let { if (it < 0) -it else it }
        val primaryBias = when {
            spec.baseAttack >= spec.baseDefense && spec.baseAttack >= spec.baseSpeed && spec.baseAttack >= spec.baseWisdom -> "Assault Vanguard"
            spec.baseDefense >= spec.baseAttack && spec.baseDefense >= spec.baseSpeed && spec.baseDefense >= spec.baseWisdom -> "Fortress Bastion"
            spec.baseSpeed >= spec.baseAttack && spec.baseSpeed >= spec.baseDefense && spec.baseSpeed >= spec.baseWisdom -> "Velocity Blitz"
            else -> "Celestial Cipher"
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
            treeArchetypeTitle = "${spec.speciesName} • $primaryBias Matrix (${spec.attribute})",
            nodes = listOf(
                MonsterSkillNode(
                    nodeId = n1Id,
                    tier = 1,
                    branchName = "Offense Core",
                    title = "${spec.speciesName} Fang Circuit",
                    description = "Amplifies ${spec.signatureMove} output (+${5 + bonusShift} ATK, +3 SPD).",
                    atkBonus = 5 + bonusShift,
                    spdBonus = 3
                ),
                MonsterSkillNode(
                    nodeId = n2Id,
                    tier = 1,
                    branchName = "Aegis Core",
                    title = "${spec.attribute} Hull Plating",
                    description = "Reinforces ${spec.speciesName}'s defensive shell (+${5 + bonusShift} DEF, +3 WIS).",
                    defBonus = 5 + bonusShift,
                    wisBonus = 3
                ),
                MonsterSkillNode(
                    nodeId = n3Id,
                    tier = 2,
                    branchName = "Cryo & Expedition",
                    title = "${spec.speciesName} Cryo-Metabolism",
                    description = "Boosts Cryo-Rest stamina recovery (+3/tick) & +5 DEF for long expeditions.",
                    defBonus = 5,
                    wisBonus = 4,
                    cryoRegenBonus = 3,
                    prerequisiteNodeId = n2Id
                ),
                MonsterSkillNode(
                    nodeId = n4Id,
                    tier = 2,
                    branchName = "Signature Mastery",
                    title = "${spec.signatureMove} Overclock",
                    description = "Overclocks ${spec.signatureMove} resonance (+${8 + bonusShift} ATK, +6 SPD).",
                    atkBonus = 8 + bonusShift,
                    spdBonus = 6,
                    prerequisiteNodeId = n1Id
                ),
                MonsterSkillNode(
                    nodeId = n5Id,
                    tier = 3,
                    branchName = "Wild Tamer Link",
                    title = "${spec.speciesName} Berry Resonance",
                    description = "+8% Tamming Berry catch rate & +7 WIS/+5 SPD when scanning wild monsters.",
                    spdBonus = 5,
                    wisBonus = 7,
                    tammingRateBonusPercent = 8,
                    prerequisiteNodeId = n3Id
                ),
                MonsterSkillNode(
                    nodeId = n6Id,
                    tier = 3,
                    branchName = "Apex Awakening",
                    title = "Crest of ${spec.speciesName}",
                    description = "Unlocks ${spec.speciesName}'s ultimate potential (+10 ATK, +10 DEF, +10 SPD, +10 WIS).",
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

    // 71 Additional Unique Monsters so total catalog = 29 + 71 = 100 Unique Monsters
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
        val evoTargetId: String
    )

    private val raw71Definitions: List<RawWildDef> = listOf(
        // 28 Wild Rookies (#30 - #57)
        RawWildDef("tentomon", "Tentomon", "Rookie", "Vaccine", "Super Shocker", 44, 52, 48, 54, "kabuterimon"),
        RawWildDef("palmon", "Palmon", "Rookie", "Data", "Poison Ivy", 46, 44, 50, 52, "togemon"),
        RawWildDef("gomamon", "Gomamon", "Rookie", "Vaccine", "Marching Fishes", 45, 50, 46, 51, "ikkakumon"),
        RawWildDef("biyo", "Biyomon", "Rookie", "Vaccine", "Spiral Twister", 48, 40, 58, 46, "birdramon"),
        RawWildDef("salamon", "Salamon", "Rookie", "Vaccine", "Puppy Howling", 42, 44, 52, 58, "gatomon"),
        RawWildDef("terriermon", "Terriermon", "Rookie", "Vaccine", "Bunny Blast", 47, 46, 56, 49, "gargomon"),
        RawWildDef("renamon", "Renamon", "Rookie", "Data", "Diamond Storm", 53, 41, 60, 52, "kyubimon"),
        RawWildDef("impmon", "Impmon", "Rookie", "Virus", "Night of Fire", 56, 38, 57, 47, "wizardmon"),
        RawWildDef("wormmon", "Wormmon", "Rookie", "Free", "Sticky Net", 43, 53, 49, 51, "stingmon"),
        RawWildDef("hawkmon", "Hawkmon", "Rookie", "Free", "Feather Slash", 51, 43, 59, 45, "aquilamon"),
        RawWildDef("armadillomon", "Armadillomon", "Rookie", "Free", "Diamond Shell", 45, 62, 39, 46, "ankylomon"),
        RawWildDef("monodramon", "Monodramon", "Rookie", "Vaccine", "Beat Knuckle", 57, 45, 54, 38, "strikedramon"),
        RawWildDef("elecmon", "Elecmon", "Rookie", "Data", "Sparkling Thunder", 50, 42, 55, 45, "leomon"),
        RawWildDef("betamon", "Betamon", "Rookie", "Virus", "Electric Shock", 46, 48, 49, 50, "seadramon"),
        RawWildDef("gotsumon", "Gotsumon", "Rookie", "Data", "Rock Fist", 52, 64, 36, 40, "monochromon"),
        RawWildDef("hagurumon", "Hagurumon", "Rookie", "Virus", "Darkness Gear", 44, 56, 42, 54, "guardromon"),
        RawWildDef("keramon", "Keramon", "Rookie", "Free", "Crazy Giggle", 59, 40, 56, 48, "chrysalimon"),
        RawWildDef("lopmon", "Lopmon", "Rookie", "Data", "Blazing Ice", 45, 47, 54, 56, "turuiemon"),
        RawWildDef("commandramon", "Commandramon", "Rookie", "Virus", "M16 Assassin", 55, 49, 53, 41, "sealsdramon"),
        RawWildDef("dracomon", "Dracomon", "Rookie", "Data", "Baby Breath", 58, 50, 48, 42, "coredramon"),
        RawWildDef("hackmon", "Hackmon", "Rookie", "Data", "Fifth Rush", 60, 51, 55, 44, "baohuckmon"),
        RawWildDef("zubamon", "Zubamon", "Rookie", "Vaccine", "Twenty Dive", 57, 52, 51, 40, "zubaeagermon"),
        RawWildDef("ludomon", "Ludomon", "Rookie", "Data", "Wall Rake", 43, 65, 41, 47, "tia_ludomon"),
        RawWildDef("pulsemon", "Pulsemon", "Rookie", "Vaccine", "Elec Rush", 54, 44, 62, 42, "bulkmon"),
        RawWildDef("gammamon", "Gammamon", "Rookie", "Virus", "Horn Attack", 56, 48, 52, 41, "betelgammamon"),
        RawWildDef("jellymon", "Jellymon", "Rookie", "Data", "Biboroca", 46, 43, 59, 57, "teslajellymon"),
        RawWildDef("angoramon", "Angoramon", "Rookie", "Vaccine", "Double Lariat", 51, 57, 45, 53, "symbareangoramon"),
        RawWildDef("loogamon", "Loogamon", "Rookie", "Virus", "Howling Fire", 58, 45, 58, 43, "loogarmon"),

        // 22 Wild Champions (#58 - #79)
        RawWildDef("kabuterimon", "Kabuterimon", "Champion", "Vaccine", "Electro Shocker", 92, 98, 78, 88, "megakabuterimon"),
        RawWildDef("togemon", "Togemon", "Champion", "Data", "Needle Spray", 90, 88, 82, 86, "lillymon"),
        RawWildDef("ikkakumon", "Ikkakumon", "Champion", "Vaccine", "Harpoon Torpedo", 94, 102, 70, 84, "zudomon"),
        RawWildDef("birdramon", "Birdramon", "Champion", "Vaccine", "Meteor Wing", 96, 76, 104, 80, "garudamon"),
        RawWildDef("gatomon", "Gatomon", "Champion", "Vaccine", "Lightning Paw", 86, 84, 98, 102, "angewomon"),
        RawWildDef("kyubimon", "Kyubimon", "Champion", "Data", "Dragon Wheel", 95, 85, 105, 96, "taomon"),
        RawWildDef("wizardmon", "Wizardmon", "Champion", "Data", "Thunder Cloud", 88, 80, 92, 110, "myotismon"),
        RawWildDef("stingmon", "Stingmon", "Champion", "Free", "Spiking Strike", 104, 82, 108, 78, "paildramon"),
        RawWildDef("aquilamon", "Aquilamon", "Champion", "Free", "Blast Laser", 98, 80, 106, 79, "silphymon"),
        RawWildDef("ankylomon", "Ankylomon", "Champion", "Free", "Tail Hammer", 89, 114, 68, 82, "shakkoumon"),
        RawWildDef("strikedramon", "Strikedramon", "Champion", "Vaccine", "Strike Fang", 106, 86, 99, 72, "cyberdramon"),
        RawWildDef("leomon", "Leomon", "Champion", "Vaccine", "Fist of the Beast King", 105, 90, 88, 79, "grapleomon"),
        RawWildDef("seadramon", "Seadramon", "Champion", "Data", "Ice Arrow", 91, 93, 86, 90, "megaseadramon"),
        RawWildDef("monochromon", "Monochromon", "Champion", "Data", "Volcanic Strike", 99, 112, 66, 74, "triceramon"),
        RawWildDef("guardromon", "Guardromon", "Champion", "Virus", "Guardian Barrage", 92, 116, 68, 86, "andromon"),
        RawWildDef("chrysalimon", "Chrysalimon", "Champion", "Free", "Data Crusher", 102, 95, 84, 88, "infermon"),
        RawWildDef("turuiemon", "Turuiemon", "Champion", "Data", "Gauntlet Claw", 96, 88, 101, 87, "antylamon"),
        RawWildDef("sealsdramon", "Sealsdramon", "Champion", "Virus", "Death Behind", 107, 87, 103, 76, "tankdramon"),
        RawWildDef("coredramon", "Coredramon", "Champion", "Data", "Blue Flare Breath", 103, 96, 88, 81, "wingdramon"),
        RawWildDef("baohuckmon", "BaoHuckmon", "Champion", "Data", "Teen Blade", 108, 94, 96, 80, "saviorhuckmon"),
        RawWildDef("zubaeagermon", "ZubaEagermon", "Champion", "Vaccine", "Road Toia", 104, 92, 95, 78, "duramon"),
        RawWildDef("tia_ludomon", "TiaLudomon", "Champion", "Data", "freeze Shield", 86, 118, 76, 85, "raijiludomon"),
        RawWildDef("bulkmon", "Bulkmon", "Champion", "Vaccine", "Thunder Dread", 106, 94, 98, 75, "boutmon"),
        RawWildDef("betelgammamon", "BetelGammamon", "Champion", "Virus", "Sol Shot", 105, 91, 94, 79, "canoweissmon"),
        RawWildDef("teslajellymon", "TeslaJellymon", "Champion", "Data", "Fizzy Punisher", 93, 86, 105, 98, "thetismon"),
        RawWildDef("symbareangoramon", "SymbareAngoramon", "Champion", "Vaccine", "Javelin Wind", 97, 96, 99, 92, "lamortmon"),
        RawWildDef("loogarmon", "Loogarmon", "Champion", "Virus", "Howling Burner", 108, 89, 102, 82, "soloogarmon"),

        // 16 Wild Ultimates (#85 - #100)
        RawWildDef("megakabuterimon", "MegaKabuterimon", "Ultimate", "Vaccine", "Horn Buster", 156, 168, 122, 138, "herculeskabuterimon"),
        RawWildDef("lillymon", "Lillymon", "Ultimate", "Data", "Flower Cannon", 148, 132, 164, 152, "rosemon"),
        RawWildDef("zudomon", "Zudomon", "Ultimate", "Vaccine", "Vulcan's Hammer", 166, 174, 118, 136, "vikemon"),
        RawWildDef("garudamon", "Garudamon", "Ultimate", "Vaccine", "Wing Blade", 168, 138, 170, 132, "phoenixmon"),
        RawWildDef("angewomon", "Angewomon", "Ultimate", "Vaccine", "Celestial Arrow", 150, 146, 158, 178, "ophanimon"),
        RawWildDef("taomon", "Taomon", "Ultimate", "Data", "Talisman of Light", 152, 148, 160, 176, "sakuyamon"),
        RawWildDef("myotismon", "Myotismon", "Ultimate", "Virus", "Grisly Wing", 168, 152, 148, 166, "venommyotismon"),
        RawWildDef("paildramon", "Paildramon", "Ultimate", "Free", "Desperado Blaster", 174, 150, 166, 134, "imperialdramon_fm"),
        RawWildDef("cyberdramon", "Cyberdramon", "Ultimate", "Vaccine", "Erase Claw", 176, 154, 158, 126, "justimon"),
        RawWildDef("andromon", "Andromon", "Ultimate", "Vaccine", "Spiral Sword", 164, 172, 134, 148, "craniamon"),
        RawWildDef("infermon", "Infermon", "Ultimate", "Free", "Spider Shooter", 172, 146, 162, 144, "diaboromon"),
        RawWildDef("saviorhuckmon", "SaviorHuckmon", "Ultimate", "Data", "Trident Saber", 178, 156, 164, 136, "jesmon"),
        RawWildDef("canoweissmon", "Canoweissmon", "Ultimate", "Vaccine", "Gran Nova", 175, 155, 162, 140, "siriusmon"),
        RawWildDef("soloogarmon", "Soloogarmon", "Ultimate", "Virus", "Surtr's Flame", 179, 150, 168, 138, "fenriloogamon"),
        RawWildDef("herculeskabuterimon", "HerculesKabuterimon", "Mega", "Vaccine", "Giga Blaster", 248, 255, 220, 215, "omegamon_x"),
        RawWildDef("jesmon", "Jesmon", "Mega", "Data", "OS Generics", 265, 240, 250, 215, "alphamon_ouryuken")
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
            "greymon" to "Greymon",
            "geogreymon" to "GeoGreymon",
            "tyrannomon" to "Tyrannomon",
            "garurumon" to "Garurumon",
            "dorugamon" to "Dorugamon",
            "angemon" to "Angemon",
            "gargomon" to "Gargomon",
            "growlmon" to "Growlmon",
            "metalgreymon" to "MetalGreymon",
            "rizegreymon" to "RizeGreymon",
            "weregarurumon" to "WereGarurumon",
            "magnaangemon" to "MagnaAngemon",
            "wargrowlmon" to "WarGrowlmon",
            "wargreymon" to "WarGreymon",
            "metalgarurumon" to "MetalGarurumon",
            "seraphimon" to "Seraphimon",
            "gallantmon" to "Gallantmon",
            "omegamon_x" to "Omegamon X",
            "alphamon_ouryuken" to "Alphamon Ouryuken"
        )
        val megaPool = listOf(
            "herculeskabuterimon" to "HerculesKabuterimon",
            "jesmon" to "Jesmon",
            "wargreymon" to "WarGreymon",
            "metalgarurumon" to "MetalGarurumon",
            "seraphimon" to "Seraphimon",
            "gallantmon" to "Gallantmon"
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
                ?: "Omegamon X"

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
                        branchDescription = "Evolution into $targetName ($nextStage). Requires LVL $reqLvl+, 100% Sync, and trained stats."
                    )
                )
            }

            DigimonSpeciesSpec(
                speciesId = raw.id,
                speciesName = raw.name,
                stage = raw.stage,
                attribute = raw.attribute,
                titleTag = "Wild Dex #${30 + idx} • ${raw.stage} Class",
                signatureMove = raw.move,
                primaryColorHex = c1,
                secondaryColorHex = c2,
                baseAttack = raw.atk,
                baseDefense = raw.def,
                baseSpeed = raw.spd,
                baseWisdom = raw.wis,
                apexTargetName = targetName,
                isRareLevel100Spec = false,
                evolutionBranches = branches
            )
        }
    }
}
