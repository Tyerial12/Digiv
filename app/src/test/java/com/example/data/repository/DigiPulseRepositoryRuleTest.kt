package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DigimonCreature
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PetNotification
import com.example.data.model.TrainingLogEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DigiPulseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveCreature_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DigiPulseRepository(firestore)
        val uniqueId = "creature_${UUID.randomUUID().toString().replace("-", "")}"

        val creature = DigimonCreature(
            id = uniqueId,
            userId = aliceUid,
            nickname = "CyberAgumon",
            speciesId = "agumon",
            speciesName = "Agumon",
            stage = "Rookie",
            attribute = "Vaccine",
            health = 95,
            stamina = 85,
            hunger = 80,
            evolutionProgress = 40
        )

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createCreature(creature) }
        assertTrue("Expected creature creation to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCreatures().first { list -> list.any { it.id == uniqueId } }
        }
        val fetched = emitted.first { it.id == uniqueId }
        assertEquals("CyberAgumon", fetched.nickname)
        assertEquals(40, fetched.evolutionProgress)

        // Update creature telemetry
        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateCreature(fetched.copy(evolutionProgress = 75, stamina = 70))
        }
        assertTrue("Expected creature update to succeed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)
    }

    @Test
    fun trainingLogsAndNotifications_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DigiPulseRepository(firestore)

        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTrainingLog(
                TrainingLogEntry(
                    userId = aliceUid,
                    creatureId = "starter_agumon",
                    creatureName = "NovaAgumon",
                    activityType = "Power Drill",
                    grade = "S",
                    statDeltaSummary = "+12 ATK, +20% EVO",
                    xpGained = 150,
                    dateKey = "2026-11-14"
                )
            )
        }
        assertTrue("Expected training log creation to succeed", logRes.isSuccess)

        val notifRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addNotification(
                PetNotification(
                    userId = aliceUid,
                    creatureId = "starter_agumon",
                    creatureName = "NovaAgumon",
                    severity = "EVOLUTION",
                    title = "Digivolution Ready!",
                    message = "NovaAgumon reached 100% Evolution Sync!",
                    isRead = false
                )
            )
        }
        assertTrue("Expected notification creation to succeed", notifRes.isSuccess)
        val notifId = notifRes.getOrThrow()

        val markReadRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markNotificationRead(notifId)
        }
        assertTrue("Expected markNotificationRead to succeed", markReadRes.isSuccess)
    }

    @Test
    fun leaderboard_upsertAndObserveGlobal_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DigiPulseRepository(firestore)

        val upsertRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertLeaderboardEntry(
                LeaderboardEntry(
                    id = aliceUid,
                    userId = aliceUid,
                    leagueId = "global",
                    trainerName = "HoloAlice",
                    tamerTitle = "Digital Sovereign",
                    partnerName = "WarGreymon",
                    partnerStage = "Mega",
                    partnerAttribute = "Vaccine",
                    totalCombatPower = 5120,
                    evolutionsUnlocked = 5,
                    trainingStreakDays = 7,
                    sparringWins = 9
                )
            )
        }
        assertTrue("Expected leaderboard upsert to succeed: ${upsertRes.exceptionOrNull()}", upsertRes.isSuccess)

        val rankings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLeaderboard().first { list -> list.any { it.userId == aliceUid } }
        }
        assertTrue(rankings.any { it.trainerName == "HoloAlice" && it.totalCombatPower == 5120 })
    }

    @Test
    fun tradeListings_createAcceptAndClaimBetweenTwoPlayers_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DigiPulseRepository(firestore)
        val aliceCreatureId = "creature_${UUID.randomUUID().toString().replace("-", "")}"
        val tradeId = "trade_${UUID.randomUUID().toString().replace("-", "")}"

        val aliceGreymon = DigimonCreature(
            id = aliceCreatureId,
            userId = aliceUid,
            nickname = "AliceGreymon",
            speciesId = "greymon",
            speciesName = "Greymon",
            stage = "Champion",
            attribute = "Vaccine",
            attack = 100,
            defense = 88,
            speed = 76,
            wisdom = 70,
            bondLevel = 45
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCreature(aliceGreymon).getOrThrow()
        }

        val createTradeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTradeListing(
                offeredCreature = aliceGreymon,
                creatorTrainerName = "HoloAlice",
                requestedStageOrSpecies = "Champion",
                customTradeId = tradeId
            )
        }
        assertTrue("Expected trade listing creation to succeed: ${createTradeRes.exceptionOrNull()}", createTradeRes.isSuccess)

        // Sign in as Bob to observe and accept Alice's OPEN trade
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = DigiPulseRepository(firestore)
        val bobCreatureId = "creature_${UUID.randomUUID().toString().replace("-", "")}"
        val bobGarurumon = DigimonCreature(
            id = bobCreatureId,
            userId = bobUid,
            nickname = "BobGarurumon",
            speciesId = "garurumon",
            speciesName = "Garurumon",
            stage = "Champion",
            attribute = "Vaccine",
            attack = 92,
            defense = 82,
            speed = 108,
            wisdom = 78,
            bondLevel = 50
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createCreature(bobGarurumon).getOrThrow()
        }

        val openTrades = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeOpenTradeListings().first { list -> list.any { it.id == tradeId } }
        }
        val targetListing = openTrades.first { it.id == tradeId }

        val acceptRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.acceptTradeListing(
                listing = targetListing,
                myCreatureToTrade = bobGarurumon,
                accepterTrainerName = "HoloBob"
            )
        }
        assertTrue("Expected Bob to accept trade listing: ${acceptRes.exceptionOrNull()}", acceptRes.isSuccess)
        assertEquals("AliceGreymon", acceptRes.getOrThrow().nickname)

        // Sign back in as Alice and claim the completed trade
        signInTestUser(ALICE_EMAIL)
        val myListings = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMyTradeListings().first { list ->
                list.any { it.id == tradeId && it.status == "COMPLETED" }
            }
        }
        val completedListing = myListings.first { it.id == tradeId }
        val claimRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.claimCompletedTradeListing(completedListing)
        }
        assertTrue("Expected Alice to claim completed trade: ${claimRes.exceptionOrNull()}", claimRes.isSuccess)
        assertEquals("BobGarurumon", claimRes.getOrThrow().nickname)
    }

    @Test
    fun getCreatureById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DigiPulseRepository(firestore)
        val uniqueId = "creature_${UUID.randomUUID().toString().replace("-", "")}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCreature(
                DigimonCreature(id = uniqueId, userId = aliceUid, nickname = "AliceAgumon")
            ).getOrThrow()
        }

        // Switch to Bob and attempt to read Alice's creature
        signInTestUser(BOB_EMAIL)
        val bobRepo = DigiPulseRepository(firestore)
        val bobReadRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCreatureById(aliceUid, uniqueId)
        }
        assertTrue("Expected cross-user access to fail", bobReadRes.isFailure)
        val firestoreEx = bobReadRes.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeLeaderboard_unauthenticatedUser_fails() = runBlocking {
        auth.signOut()
        val repository = DigiPulseRepository(firestore)

        var thrownException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLeaderboard().first()
            }
        } catch (e: Throwable) {
            thrownException = e
        }
        assertNotNull("Expected unauthenticated observeLeaderboard to throw", thrownException)
    }

    @Test
    fun chatMessagesAndFriendships_authenticatedPlayers_succeeds() = runBlocking {
        val runSuffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val chatAliceEmail = "alice_chat_$runSuffix@test.com"
        val chatBobEmail = "bob_chat_$runSuffix@test.com"

        val aliceUid = signInTestUser(chatAliceEmail)
        val aliceRepo = DigiPulseRepository(firestore)

        // Sign in Bob to get Bob's UID
        val bobUid = signInTestUser(chatBobEmail)
        val bobRepo = DigiPulseRepository(firestore)

        // Sign back in as Alice to send a global chat message and a friend request to Bob
        signInTestUser(chatAliceEmail)
        val sendMsgRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendChatMessage(
                senderTrainerName = "HoloAlice",
                senderPartnerName = "Omegamon",
                senderPartnerStage = "Mega",
                messageText = "Hello Tamers!"
            )
        }
        assertTrue("Expected Alice global chat message to succeed: ${sendMsgRes.exceptionOrNull()}", sendMsgRes.isSuccess)

        val friendReqRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendFriendRequest(
                requesterTrainerName = "HoloAlice",
                requesterPartnerName = "Omegamon",
                recipientUserId = bobUid,
                recipientTrainerName = "HoloBob",
                recipientPartnerName = "MetalGarurumon"
            )
        }
        assertTrue("Expected Alice friend request to succeed: ${friendReqRes.exceptionOrNull()}", friendReqRes.isSuccess)

        // Switch to Bob: observe global chat and accept Alice's friend request
        signInTestUser(chatBobEmail)
        val globalMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeGlobalChatMessages().first { list -> list.any { it.senderUserId == aliceUid } }
        }
        assertTrue(globalMsgs.any { it.messageText == "Hello Tamers!" })

        val incomingFriends = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeIncomingFriendships().first { list -> list.any { it.requesterUserId == aliceUid } }
        }
        val pendingReq = incomingFriends.first { it.requesterUserId == aliceUid }

        val acceptFriendRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.acceptFriendRequest(
                friendshipId = pendingReq.id,
                recipientTrainerName = "HoloBob",
                recipientPartnerName = "MetalGarurumon"
            )
        }
        assertTrue("Expected Bob to accept friend request: ${acceptFriendRes.exceptionOrNull()}", acceptFriendRes.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_digipulse@test.com"
        const val BOB_EMAIL = "bob_digipulse@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
