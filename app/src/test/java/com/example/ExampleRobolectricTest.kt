package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify branching evolution catalog and level 100 rare evolution rule`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Digi Verse", appName)

        val agumonSpec = DigimonCatalog.getSpec("agumon")
        assertTrue(agumonSpec.evolutionBranches.size >= 3)

        // Verify DigiCoin training economy constants (60 starting coins, 10 cost per training, +5 every 5 mins)
        assertEquals(60, DigimonCatalog.STARTING_DIGI_COINS)
        assertEquals(10, DigimonCatalog.TRAINING_DRILL_COIN_COST)
        assertEquals(300, DigimonCatalog.DIGI_COIN_INTERVAL_SEC)
        assertEquals(5, DigimonCatalog.DIGI_COINS_PER_INTERVAL)

        val rookieStarter = DigimonCreature(
            speciesId = "agumon",
            evolutionProgress = 100,
            attack = agumonSpec.baseAttack,
            defense = agumonSpec.baseDefense,
            speed = agumonSpec.baseSpeed,
            wisdom = agumonSpec.baseWisdom,
            bondLevel = 100, // max bond alone does NOT raise level (training-only leveling)
            careMistakes = 0
        )
        assertEquals(1, rookieStarter.currentLevel())
        assertFalse("Fresh Rookie at LVL 1 cannot evolve into Champion before reaching LVL 20", agumonSpec.evolutionBranches.first().isMetBy(rookieStarter))

        val belowLvl20Creature = DigimonCreature(
            speciesId = "agumon",
            evolutionProgress = 100,
            attack = 80,
            defense = 65,
            speed = 65,
            wisdom = 34,
            bondLevel = 50
        )
        assertTrue(belowLvl20Creature.currentLevel() < 20)
        assertFalse("Monster below Level 20 cannot evolve even if stats/sync are high", agumonSpec.evolutionBranches.first().isMetBy(belowLvl20Creature))

        val readyCreature = DigimonCreature(
            speciesId = "agumon",
            evolutionProgress = 100,
            attack = 105,
            defense = 90,
            speed = 85,
            wisdom = 75,
            bondLevel = 50,
            careMistakes = 5 // even with care mistakes, meeting level 20+/stats/sync allows evolution
        )
        assertTrue(readyCreature.currentLevel() >= 20)
        assertTrue(readyCreature.currentLevel() >= agumonSpec.evolutionBranches.first().requiredLevel)
        assertTrue(agumonSpec.evolutionBranches.first().isMetBy(readyCreature))
        assertTrue(agumonSpec.evolutionBranches.first().isStatsAndSyncMetBy(readyCreature))

        // Verify 6 Rare Level-100 Monsters exist and require THAT specific monster to be Level 100
        assertEquals(6, DigimonCatalog.level100RareSpecs.size)
        val rareBranch = DigimonCatalog.level100RareBranches.first()

        val megaNot100 = DigimonCreature(
            speciesId = "wargreymon",
            stage = "Mega",
            evolutionProgress = 100,
            attack = 220,
            defense = 190,
            speed = 180,
            wisdom = 160,
            bondLevel = 80
        )
        assertTrue(megaNot100.currentLevel() < 100)
        assertFalse("Monster below Level 100 cannot evolve into Rare Level 100 monster", rareBranch.isMetBy(megaNot100))

        val megaAt100 = DigimonCreature(
            speciesId = "wargreymon",
            stage = "Mega",
            evolutionProgress = 100,
            attack = 310,
            defense = 280,
            speed = 270,
            wisdom = 260,
            bondLevel = 100
        )
        assertEquals(100, megaAt100.currentLevel())
        assertTrue("Monster at Level 100 can evolve into Rare Level 100 monster", rareBranch.isMetBy(megaAt100))

        // Verify 100 unique monsters, 4-monster max squad cap, 10 timed expeditions (with 24h), Tamming Berries, and Feeding Economy
        assertEquals(100, DigimonCatalog.allSpeciesSpecs.size)
        assertEquals(4, DigimonCatalog.MAX_SQUAD_SIZE)
        assertEquals(10, DigimonCatalog.allExpeditions.size)
        val exp24h = DigimonCatalog.allExpeditions.first { it.durationMinutes == 1440 }
        assertEquals(100, exp24h.rewardCoins)
        assertTrue(
            "Only the 24h expedition can bring back 100 DigiCoins",
            DigimonCatalog.allExpeditions.filter { it.durationMinutes < 1440 }.all { it.rewardCoins < 100 }
        )
        assertEquals(2, DigimonCatalog.TAMMING_BERRY_COIN_COST)
        assertEquals(5, DigimonCatalog.PAID_FEED_COIN_COST)

        // Verify 1 free feed with 0 stats and 0 stamina, and paid feeds cost 5 DigiCoins and grant stats
        val freeRation = com.example.data.model.DigiRation.FREE_CYBER_WAFER
        assertEquals(0, freeRation.costCoins)
        assertEquals(0, freeRation.atkBonus + freeRation.defBonus + freeRation.spdBonus + freeRation.wisBonus)
        assertEquals(0, freeRation.staminaRestore)
        val paidRations = com.example.data.model.DigiRation.entries.filter { it != freeRation }
        assertTrue(paidRations.all {
            it.costCoins == 5 &&
                (it.atkBonus + it.defBonus + it.spdBonus + it.wisBonus) > 0 &&
                it.staminaRestore <= 1
        })

        // Verify unique skill tree for each monster and that unlocking a skill node consumes 1 SP, unlocks the node, and does NOT falsely increase level/SP even after Firestore roundtrip
        val agumonTree = DigimonCatalog.getSkillTreeForSpecies("agumon")
        val gabumonTree = DigimonCatalog.getSkillTreeForSpecies("gabumon")
        assertEquals(6, agumonTree.nodes.size)
        assertEquals(6, gabumonTree.nodes.size)
        assertFalse(agumonTree.treeArchetypeTitle == gabumonTree.treeArchetypeTitle)

        assertEquals(1, rookieStarter.totalSkillPoints())
        assertEquals(1, rookieStarter.availableSkillPoints())
        val firstNode = agumonTree.nodes.first()
        val nodeStatBonus = firstNode.atkBonus + firstNode.defBonus + firstNode.spdBonus + firstNode.wisBonus
        val unlockedList = listOf(firstNode.nodeId)
        val newBonusStats = rookieStarter.persistedNonTrainingBonusStats() + nodeStatBonus
        val newForms = rookieStarter.withUpdatedMetadataInForms(
            newSkillNodes = unlockedList,
            newBonusStats = newBonusStats,
            newExpeditionsCount = 0
        )
        // Simulate Firestore roundtrip where only Firestore-persisted fields (like unlockedForms and stats) come back
        val afterFirestoreRoundtrip = rookieStarter.copy(
            attack = rookieStarter.attack + firstNode.atkBonus,
            defense = rookieStarter.defense + firstNode.defBonus,
            speed = rookieStarter.speed + firstNode.spdBonus,
            wisdom = rookieStarter.wisdom + firstNode.wisBonus,
            unlockedForms = newForms,
            unlockedSkillNodes = emptyList(),
            nonTrainingBonusStats = 0
        )
        assertEquals(1, afterFirestoreRoundtrip.currentLevel())
        assertEquals(1, afterFirestoreRoundtrip.totalSkillPoints())
        assertEquals(1, afterFirestoreRoundtrip.unlockedSkillCount())
        assertEquals(0, afterFirestoreRoundtrip.availableSkillPoints())
        assertTrue(firstNode.nodeId in afterFirestoreRoundtrip.allUnlockedSkillNodeIds())

        // Verify anti-spam Cryo-Rest gate (cannot enter rest above 50% stamina) and strict evolution readiness gate
        assertFalse(rookieStarter.copy(stamina = 51, isResting = false).canEnterCryoRest())
        assertTrue(rookieStarter.copy(stamina = 50, isResting = false).canEnterCryoRest())
        assertFalse("Monster below Level 20 must never report canEvolveAnyBranch()", belowLvl20Creature.canEvolveAnyBranch())
        assertFalse("Monster below Level 20 must never show EVO READY! mood", belowLvl20Creature.moodLabel() == "EVO READY!")
        assertTrue("Monster meeting Level 20+ and all stats reports canEvolveAnyBranch()", readyCreature.canEvolveAnyBranch())
        assertEquals("EVO READY!", readyCreature.moodLabel())

        // Verify MonsterActionState animated action modes (Idle, Sleeping, Eating, Training)
        val actionModes = com.example.ui.components.MonsterActionState.entries.map { it.label }
        assertEquals(listOf("IDLE", "SLEEPING", "EATING", "TRAINING"), actionModes)
    }
}
