package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.DigiRation
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.FriendConnection
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PetNotification
import com.example.data.model.TradeListing
import com.example.data.model.TrainingLogEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class DigiPulseRepository(private val db: FirebaseFirestore) {

    private var appContext: Context? = null

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    ) {
        this.appContext = context.applicationContext
    }

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCreatures(): Flow<List<DigimonCreature>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/creatures"
        emitAll(
            db.collection("users").document(uid).collection("creatures")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DigimonCreature::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTrainingLogs(): Flow<List<TrainingLogEntry>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/trainingLogs"
        emitAll(
            db.collection("users").document(uid).collection("trainingLogs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TrainingLogEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeNotifications(): Flow<List<PetNotification>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PetNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("leagueId", "global")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.totalCombatPower }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOpenTradeListings(): Flow<List<TradeListing>> = flow {
        val uid = requireUserId()
        val path = "tradeListings"
        emitAll(
            db.collection("tradeListings")
                .whereEqualTo("status", "OPEN")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TradeListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { !it.isNpcListing() }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyTradeListings(): Flow<List<TradeListing>> = flow {
        val uid = requireUserId()
        val path = "tradeListings"
        emitAll(
            db.collection("tradeListings")
                .whereEqualTo("creatorUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TradeListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { !it.isNpcListing() }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeGlobalChatMessages(): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "chatMessages"
        emitAll(
            db.collection("chatMessages")
                .whereEqualTo("channelId", "global")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { !it.isNpcMessage() }
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSentChatMessages(): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "chatMessages"
        emitAll(
            db.collection("chatMessages")
                .whereEqualTo("senderUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { !it.isNpcMessage() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeReceivedChatMessages(): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "chatMessages"
        emitAll(
            db.collection("chatMessages")
                .whereEqualTo("recipientUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { !it.isNpcMessage() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOutgoingFriendships(): Flow<List<FriendConnection>> = flow {
        val uid = requireUserId()
        val path = "friendships"
        emitAll(
            db.collection("friendships")
                .whereEqualTo("requesterUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FriendConnection::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { !it.isNpcConnection() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeIncomingFriendships(): Flow<List<FriendConnection>> = flow {
        val uid = requireUserId()
        val path = "friendships"
        emitAll(
            db.collection("friendships")
                .whereEqualTo("recipientUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FriendConnection::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { !it.isNpcConnection() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    private fun sanitizeDocId(rawId: String, prefix: String = "doc"): String {
        val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return cleaned.ifBlank { "${prefix}_${System.currentTimeMillis()}" }
    }

    private fun sanitizeStage(stage: String): String =
        stage.takeIf { it in VALID_STAGES } ?: "Rookie"

    private fun sanitizeAttribute(attribute: String): String =
        attribute.takeIf { it in VALID_ATTRIBUTES } ?: "Vaccine"

    private fun sanitizeActivityType(activityType: String): String =
        activityType.takeIf { it in VALID_ACTIVITY_TYPES } ?: "Power Drill"

    private fun sanitizeGrade(grade: String): String =
        grade.takeIf { it in VALID_GRADES } ?: "CARE"

    private fun sanitizeSeverity(severity: String): String =
        severity.takeIf { it in VALID_SEVERITIES } ?: "INFO"

    private fun sanitizeDateKey(dateKey: String): String {
        val trimmed = dateKey.trim().take(10)
        return if (DATE_KEY_REGEX.matches(trimmed)) {
            trimmed
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }

    suspend fun createCreature(creature: DigimonCreature): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = if (creature.id.isNotBlank()) {
            db.collection("users").document(uid).collection("creatures").document(sanitizeDocId(creature.id, "creature"))
        } else {
            db.collection("users").document(uid).collection("creatures").document()
        }
        val cleanSpeciesName = creature.speciesName.trim().take(50).ifBlank { "Agumon" }
        val payload = mapOf(
            "userId" to uid,
            "nickname" to creature.nickname.trim().take(40).ifBlank { cleanSpeciesName.take(40) },
            "speciesId" to creature.speciesId.trim().take(40).ifBlank { "agumon" },
            "speciesName" to cleanSpeciesName,
            "stage" to sanitizeStage(creature.stage),
            "attribute" to sanitizeAttribute(creature.attribute),
            "health" to creature.health.coerceIn(0, 100),
            "stamina" to creature.stamina.coerceIn(0, 100),
            "hunger" to creature.hunger.coerceIn(0, 100),
            "evolutionProgress" to creature.evolutionProgress.coerceIn(0, 100),
            "attack" to creature.attack.coerceIn(0, 9999),
            "defense" to creature.defense.coerceIn(0, 9999),
            "speed" to creature.speed.coerceIn(0, 9999),
            "wisdom" to creature.wisdom.coerceIn(0, 9999),
            "bondLevel" to creature.bondLevel.coerceIn(0, 100),
            "careMistakes" to creature.careMistakes.coerceIn(0, 999),
            "isResting" to creature.isResting,
            "unlockedForms" to creature.unlockedForms.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(15),
            "lastUpdatedMs" to creature.lastUpdatedMs.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            runCatching { docRef.delete().await() }
            try {
                docRef.set(payload).await()
                docRef.id
            } catch (retryEx: Exception) {
                handleFirestoreError(retryEx, OperationType.CREATE, docRef.path)
                throw retryEx
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCreature(creature: DigimonCreature): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(creature.id.isNotBlank()) { "Creature ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("creatures").document(sanitizeDocId(creature.id, "creature"))
        val cleanSpeciesName = creature.speciesName.trim().take(50).ifBlank { "Agumon" }
        val updates = mapOf(
            "nickname" to creature.nickname.trim().take(40).ifBlank { cleanSpeciesName.take(40) },
            "speciesId" to creature.speciesId.trim().take(40).ifBlank { "agumon" },
            "speciesName" to cleanSpeciesName,
            "stage" to sanitizeStage(creature.stage),
            "attribute" to sanitizeAttribute(creature.attribute),
            "health" to creature.health.coerceIn(0, 100),
            "stamina" to creature.stamina.coerceIn(0, 100),
            "hunger" to creature.hunger.coerceIn(0, 100),
            "evolutionProgress" to creature.evolutionProgress.coerceIn(0, 100),
            "attack" to creature.attack.coerceIn(0, 9999),
            "defense" to creature.defense.coerceIn(0, 9999),
            "speed" to creature.speed.coerceIn(0, 9999),
            "wisdom" to creature.wisdom.coerceIn(0, 9999),
            "bondLevel" to creature.bondLevel.coerceIn(0, 100),
            "careMistakes" to creature.careMistakes.coerceIn(0, 999),
            "isResting" to creature.isResting,
            "unlockedForms" to creature.unlockedForms.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(15),
            "lastUpdatedMs" to creature.lastUpdatedMs.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND ||
                e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                runCatching { docRef.delete().await() }
                createCreature(creature).getOrThrow()
            } else {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCreature(creatureId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(creatureId.isNotBlank()) { "Creature ID must not be blank for delete" }
        val docRef = db.collection("users").document(uid).collection("creatures").document(sanitizeDocId(creatureId, "creature"))
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.NOT_FOUND) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                throw e
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getCreatureById(ownerUserId: String, creatureId: String): Result<DigimonCreature?> = runCatching {
        val docRef = db.collection("users").document(ownerUserId).collection("creatures").document(creatureId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(
                DigimonCreature::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createTradeListing(
        offeredCreature: DigimonCreature,
        creatorTrainerName: String,
        requestedStageOrSpecies: String,
        customTradeId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = if (customTradeId.isNotBlank()) {
            db.collection("tradeListings").document(customTradeId)
        } else {
            db.collection("tradeListings").document()
        }
        val payload = mapOf(
            "creatorUserId" to uid,
            "creatorTrainerName" to creatorTrainerName.take(40).ifBlank { "Tamer" },
            "offeredCreatureId" to offeredCreature.id.take(128).ifBlank { "offered_${System.currentTimeMillis()}" },
            "offeredNickname" to offeredCreature.nickname.take(40).ifBlank { offeredCreature.speciesName },
            "offeredSpeciesId" to offeredCreature.speciesId.take(40),
            "offeredSpeciesName" to offeredCreature.speciesName.take(50),
            "offeredStage" to offeredCreature.stage,
            "offeredAttribute" to offeredCreature.attribute,
            "offeredLevel" to offeredCreature.currentLevel().coerceIn(1, 100),
            "offeredAttack" to offeredCreature.attack.coerceIn(0, 9999),
            "offeredDefense" to offeredCreature.defense.coerceIn(0, 9999),
            "offeredSpeed" to offeredCreature.speed.coerceIn(0, 9999),
            "offeredWisdom" to offeredCreature.wisdom.coerceIn(0, 9999),
            "offeredBondLevel" to offeredCreature.bondLevel.coerceIn(0, 100),
            "requestedStageOrSpecies" to requestedStageOrSpecies.take(50).ifBlank { "Any Stage" },
            "status" to "OPEN",
            "accepterUserId" to "",
            "accepterTrainerName" to "",
            "tradedInNickname" to "",
            "tradedInSpeciesId" to "",
            "tradedInSpeciesName" to "",
            "tradedInStage" to "Rookie",
            "tradedInAttribute" to "Vaccine",
            "tradedInLevel" to 1,
            "tradedInAttack" to 50,
            "tradedInDefense" to 50,
            "tradedInSpeed" to 50,
            "tradedInWisdom" to 50,
            "tradedInBondLevel" to 20,
            "claimedByCreator" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            if (offeredCreature.id.isNotBlank()) {
                runCatching { deleteCreature(offeredCreature.id).getOrThrow() }
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun acceptTradeListing(
        listing: TradeListing,
        myCreatureToTrade: DigimonCreature,
        accepterTrainerName: String
    ): Result<DigimonCreature> = runCatching {
        val uid = requireUserId()
        require(listing.id.isNotBlank()) { "TradeListing ID must not be blank" }
        require(listing.creatorUserId != uid) { "Cannot accept your own trade listing" }

        val tradeDocRef = db.collection("tradeListings").document(listing.id)
        val updates = mapOf(
            "status" to "COMPLETED",
            "accepterUserId" to uid,
            "accepterTrainerName" to accepterTrainerName.take(40).ifBlank { "Tamer" },
            "tradedInNickname" to myCreatureToTrade.nickname.take(40).ifBlank { myCreatureToTrade.speciesName },
            "tradedInSpeciesId" to myCreatureToTrade.speciesId.take(40),
            "tradedInSpeciesName" to myCreatureToTrade.speciesName.take(50),
            "tradedInStage" to myCreatureToTrade.stage,
            "tradedInAttribute" to myCreatureToTrade.attribute,
            "tradedInLevel" to myCreatureToTrade.currentLevel().coerceIn(1, 100),
            "tradedInAttack" to myCreatureToTrade.attack.coerceIn(0, 9999),
            "tradedInDefense" to myCreatureToTrade.defense.coerceIn(0, 9999),
            "tradedInSpeed" to myCreatureToTrade.speed.coerceIn(0, 9999),
            "tradedInWisdom" to myCreatureToTrade.wisdom.coerceIn(0, 9999),
            "tradedInBondLevel" to myCreatureToTrade.bondLevel.coerceIn(0, 100),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            tradeDocRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tradeDocRef.path)
            throw e
        }

        if (myCreatureToTrade.id.isNotBlank()) {
            runCatching { deleteCreature(myCreatureToTrade.id).getOrThrow() }
        }

        val nowMs = System.currentTimeMillis()
        val receivedCreature = DigimonCreature(
            id = "traded_${nowMs}_${listing.offeredSpeciesId}",
            userId = uid,
            nickname = listing.offeredNickname,
            speciesId = listing.offeredSpeciesId,
            speciesName = listing.offeredSpeciesName,
            stage = listing.offeredStage,
            attribute = listing.offeredAttribute,
            health = 100,
            stamina = 100,
            hunger = 90,
            evolutionProgress = 50,
            attack = listing.offeredAttack,
            defense = listing.offeredDefense,
            speed = listing.offeredSpeed,
            wisdom = listing.offeredWisdom,
            bondLevel = (listing.offeredBondLevel + 10).coerceIn(0, 100),
            careMistakes = 0,
            isResting = false,
            unlockedForms = listOf(listing.offeredSpeciesId),
            lastUpdatedMs = nowMs
        )
        createCreature(receivedCreature).getOrThrow()
        receivedCreature
    }

    suspend fun claimCompletedTradeListing(listing: TradeListing): Result<DigimonCreature> = runCatching {
        val uid = requireUserId()
        require(listing.id.isNotBlank()) { "TradeListing ID must not be blank" }
        val tradeDocRef = db.collection("tradeListings").document(listing.id)
        try {
            tradeDocRef.update(
                mapOf(
                    "claimedByCreator" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tradeDocRef.path)
            throw e
        }

        val nowMs = System.currentTimeMillis()
        val receivedSpeciesId = listing.tradedInSpeciesId.ifBlank { "agumon" }
        val receivedSpec = DigimonCatalog.getSpec(receivedSpeciesId)
        val receivedCreature = DigimonCreature(
            id = "claimed_${nowMs}_$receivedSpeciesId",
            userId = uid,
            nickname = listing.tradedInNickname.ifBlank { receivedSpec.speciesName },
            speciesId = receivedSpec.speciesId,
            speciesName = listing.tradedInSpeciesName.ifBlank { receivedSpec.speciesName },
            stage = listing.tradedInStage.ifBlank { receivedSpec.stage },
            attribute = listing.tradedInAttribute.ifBlank { receivedSpec.attribute },
            health = 100,
            stamina = 100,
            hunger = 90,
            evolutionProgress = 50,
            attack = listing.tradedInAttack.coerceAtLeast(receivedSpec.baseAttack),
            defense = listing.tradedInDefense.coerceAtLeast(receivedSpec.baseDefense),
            speed = listing.tradedInSpeed.coerceAtLeast(receivedSpec.baseSpeed),
            wisdom = listing.tradedInWisdom.coerceAtLeast(receivedSpec.baseWisdom),
            bondLevel = (listing.tradedInBondLevel + 10).coerceIn(0, 100),
            careMistakes = 0,
            isResting = false,
            unlockedForms = listOf(receivedSpec.speciesId),
            lastUpdatedMs = nowMs
        )
        createCreature(receivedCreature).getOrThrow()
        receivedCreature
    }

    suspend fun cancelTradeListing(listing: TradeListing): Result<DigimonCreature> = runCatching {
        val uid = requireUserId()
        require(listing.id.isNotBlank()) { "TradeListing ID must not be blank" }
        val tradeDocRef = db.collection("tradeListings").document(listing.id)
        try {
            tradeDocRef.update(
                mapOf(
                    "status" to "CANCELLED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tradeDocRef.path)
            throw e
        }

        val nowMs = System.currentTimeMillis()
        val restoredCreature = DigimonCreature(
            id = "restored_${nowMs}_${listing.offeredSpeciesId}",
            userId = uid,
            nickname = listing.offeredNickname,
            speciesId = listing.offeredSpeciesId,
            speciesName = listing.offeredSpeciesName,
            stage = listing.offeredStage,
            attribute = listing.offeredAttribute,
            health = 100,
            stamina = 100,
            hunger = 85,
            evolutionProgress = 30,
            attack = listing.offeredAttack,
            defense = listing.offeredDefense,
            speed = listing.offeredSpeed,
            wisdom = listing.offeredWisdom,
            bondLevel = listing.offeredBondLevel,
            careMistakes = 0,
            isResting = false,
            unlockedForms = listOf(listing.offeredSpeciesId),
            lastUpdatedMs = nowMs
        )
        createCreature(restoredCreature).getOrThrow()
        restoredCreature
    }

    suspend fun addTrainingLog(log: TrainingLogEntry): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("trainingLogs")
        val docRef = if (log.id.isNotBlank()) colRef.document(sanitizeDocId(log.id, "log")) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "creatureId" to sanitizeDocId(log.creatureId, "starter"),
            "creatureName" to log.creatureName.trim().take(50).ifBlank { "Digimon" },
            "activityType" to sanitizeActivityType(log.activityType),
            "grade" to sanitizeGrade(log.grade),
            "statDeltaSummary" to log.statDeltaSummary.trim().take(160).ifBlank { "+XP" },
            "xpGained" to log.xpGained.coerceIn(0, 100000),
            "dateKey" to sanitizeDateKey(log.dateKey),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addNotification(notification: PetNotification): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("notifications")
        val docRef = if (notification.id.isNotBlank()) colRef.document(sanitizeDocId(notification.id, "notif")) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "creatureId" to sanitizeDocId(notification.creatureId, "starter"),
            "creatureName" to notification.creatureName.trim().take(50).ifBlank { "Digimon" },
            "severity" to sanitizeSeverity(notification.severity),
            "title" to notification.title.trim().take(80).ifBlank { "Status Alert" },
            "message" to notification.message.trim().take(280).ifBlank { "Check your partner telemetry." },
            "isRead" to notification.isRead,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("notifications").document(notifId)
        val snap = runCatching { docRef.get().await() }.getOrNull()
        if (snap != null && !snap.exists()) return@runCatching
        try {
            docRef.update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun clearAllNotifications(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val notifsCol = db.collection("users").document(uid).collection("notifications")
        val notifsSnap = try {
            notifsCol.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, notifsCol.path)
            throw e
        }
        for (doc in notifsSnap.documents) {
            try {
                doc.reference.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, doc.reference.path)
                throw e
            }
        }
    }

    suspend fun upsertLeaderboardEntry(entry: LeaderboardEntry): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("leaderboard").document(uid)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            null
        }
        val fullPayload = mapOf(
            "userId" to uid,
            "leagueId" to "global",
            "trainerName" to entry.trainerName.trim().take(40).ifBlank { "Tamer" },
            "tamerTitle" to entry.tamerTitle.trim().take(50).ifBlank { "Holo Cadet" },
            "partnerName" to entry.partnerName.trim().take(50).ifBlank { "Agumon" },
            "partnerStage" to sanitizeStage(entry.partnerStage),
            "partnerAttribute" to sanitizeAttribute(entry.partnerAttribute),
            "totalCombatPower" to entry.totalCombatPower.coerceIn(0, 9999999),
            "evolutionsUnlocked" to entry.evolutionsUnlocked.coerceIn(1, 500),
            "trainingStreakDays" to entry.trainingStreakDays.coerceIn(0, 3650),
            "sparringWins" to entry.sparringWins.coerceIn(0, 999999),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            if (existingSnap != null && existingSnap.exists()) {
                try {
                    docRef.update(
                        mapOf(
                            "trainerName" to entry.trainerName.trim().take(40).ifBlank { "Tamer" },
                            "tamerTitle" to entry.tamerTitle.trim().take(50).ifBlank { "Holo Cadet" },
                            "partnerName" to entry.partnerName.trim().take(50).ifBlank { "Agumon" },
                            "partnerStage" to sanitizeStage(entry.partnerStage),
                            "partnerAttribute" to sanitizeAttribute(entry.partnerAttribute),
                            "totalCombatPower" to entry.totalCombatPower.coerceIn(0, 9999999),
                            "evolutionsUnlocked" to entry.evolutionsUnlocked.coerceIn(1, 500),
                            "trainingStreakDays" to entry.trainingStreakDays.coerceIn(0, 3650),
                            "sparringWins" to entry.sparringWins.coerceIn(0, 999999),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (updateEx: FirebaseFirestoreException) {
                    runCatching { docRef.delete().await() }
                    docRef.set(fullPayload).await()
                }
            } else {
                docRef.set(fullPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun draftThreeStarters(
        choices: List<com.example.data.model.StarterDraftChoice>,
        defaultTrainerName: String,
        prebuiltCreatures: List<DigimonCreature>? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val selected = choices.take(1)
        require(selected.isNotEmpty()) { "Please select 1 starter monster." }

        val creaturesCol = db.collection("users").document(uid).collection("creatures")
        runCatching {
            val existing = creaturesCol.get().await()
            for (doc in existing.documents) {
                runCatching { doc.reference.delete().await() }
            }
        }

        val nowMs = System.currentTimeMillis()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))

        val draftedCreatures = prebuiltCreatures?.take(1)?.map { prebuilt ->
            val rookieSpec = DigimonCatalog.requireRookieStarterSpec(prebuilt.speciesId)
            prebuilt.copy(
                userId = uid,
                speciesId = rookieSpec.speciesId,
                speciesName = rookieSpec.speciesName,
                stage = "Rookie",
                attribute = rookieSpec.attribute,
                attack = rookieSpec.baseAttack,
                defense = rookieSpec.baseDefense,
                speed = rookieSpec.baseSpeed,
                wisdom = rookieSpec.baseWisdom,
                unlockedForms = listOf(rookieSpec.speciesId)
            )
        }
            ?: selected.mapIndexed { index, choice ->
                val spec = DigimonCatalog.requireRookieStarterSpec(choice.speciesId)
                val cleanNickname = choice.customNickname.trim().take(36).ifBlank { spec.speciesName }
                DigimonCreature(
                    id = "starter_${nowMs}_${index + 1}_${spec.speciesId}",
                    userId = uid,
                    nickname = cleanNickname,
                    speciesId = spec.speciesId,
                    speciesName = spec.speciesName,
                    stage = "Rookie",
                    attribute = spec.attribute,
                    health = 100,
                    stamina = 100,
                    hunger = 85,
                    evolutionProgress = 0,
                    attack = spec.baseAttack,
                    defense = spec.baseDefense,
                    speed = spec.baseSpeed,
                    wisdom = spec.baseWisdom,
                    bondLevel = 10,
                    careMistakes = 0,
                    isResting = false,
                    unlockedForms = listOf(spec.speciesId),
                    lastUpdatedMs = nowMs
                )
            }

        draftedCreatures.forEach { createCreature(it).getOrThrow() }

        val lead = draftedCreatures.first()
        val namesSummary = "${lead.nickname} (${lead.speciesName})"

        runCatching {
            addTrainingLog(
                TrainingLogEntry(
                    userId = uid,
                    creatureId = lead.id,
                    creatureName = lead.nickname,
                    activityType = "Digivolution",
                    grade = "EVO",
                    statDeltaSummary = "Drafted Starter Monster: $namesSummary • 60 DigiCoins Initialized",
                    xpGained = 50,
                    dateKey = todayKey
                )
            )
        }

        runCatching {
            addNotification(
                PetNotification(
                    userId = uid,
                    creatureId = lead.id,
                    creatureName = lead.nickname,
                    severity = "INFO",
                    title = "Starter Monster Synchronized!",
                    message = "Train $namesSummary with DigiCoins (10 Coins/drill, +5 Coins every 5m) to reach LVL 20 and unlock evolution!",
                    isRead = false
                )
            )
        }

        val totalCp = draftedCreatures.sumOf { it.combatPower() }
        runCatching {
            upsertLeaderboardEntry(
                LeaderboardEntry(
                    id = uid,
                    userId = uid,
                    leagueId = "global",
                    trainerName = defaultTrainerName.take(40).ifBlank { "Tamer" },
                    tamerTitle = "Rookie Tamer",
                    partnerName = lead.speciesName,
                    partnerStage = "Rookie",
                    partnerAttribute = lead.attribute,
                    totalCombatPower = totalCp,
                    evolutionsUnlocked = draftedCreatures.size.coerceAtLeast(1),
                    trainingStreakDays = 1,
                    sparringWins = 0
                )
            )
        }

        lead.id
    }

    suspend fun resetAllPlayerProgress(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)

        // 1. Delete all creatures
        val creaturesCol = userDoc.collection("creatures")
        val creaturesSnap = try {
            creaturesCol.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, creaturesCol.path)
            throw e
        }
        for (doc in creaturesSnap.documents) {
            try {
                doc.reference.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, doc.reference.path)
                throw e
            }
        }

        // 2. Delete all training logs
        val logsCol = userDoc.collection("trainingLogs")
        val logsSnap = try {
            logsCol.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, logsCol.path)
            throw e
        }
        for (doc in logsSnap.documents) {
            try {
                doc.reference.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, doc.reference.path)
                throw e
            }
        }

        // 3. Delete all notifications
        val notifsCol = userDoc.collection("notifications")
        val notifsSnap = try {
            notifsCol.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, notifsCol.path)
            throw e
        }
        for (doc in notifsSnap.documents) {
            try {
                doc.reference.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, doc.reference.path)
                throw e
            }
        }

        // 4. Delete global leaderboard entry if present
        val leaderboardRef = db.collection("leaderboard").document(uid)
        val leaderboardSnap = try {
            leaderboardRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, leaderboardRef.path)
            null
        }
        if (leaderboardSnap != null && leaderboardSnap.exists()) {
            try {
                leaderboardRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, leaderboardRef.path)
                throw e
            }
        }
    }

    suspend fun sendChatMessage(
        senderTrainerName: String,
        senderPartnerName: String,
        senderPartnerStage: String,
        messageText: String,
        recipientUserId: String = "",
        customMessageId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanText = messageText.trim().take(300)
        require(cleanText.isNotEmpty()) { "Message text cannot be empty" }
        val channelId = if (recipientUserId.isBlank()) {
            "global"
        } else {
            FriendConnection.buildDirectChannelId(uid, recipientUserId)
        }
        val docRef = if (customMessageId.isNotBlank()) {
            db.collection("chatMessages").document(customMessageId)
        } else {
            db.collection("chatMessages").document()
        }
        val payload = mapOf(
            "senderUserId" to uid,
            "senderTrainerName" to senderTrainerName.trim().take(40).ifBlank { "Tamer" },
            "senderPartnerName" to senderPartnerName.trim().take(50).ifBlank { "Agumon" },
            "senderPartnerStage" to senderPartnerStage.takeIf { it in listOf("Rookie", "Champion", "Ultimate", "Mega") }.orEmpty().ifBlank { "Rookie" },
            "recipientUserId" to recipientUserId.trim().take(128),
            "channelId" to channelId.take(160),
            "messageText" to cleanText,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun sendFriendRequest(
        requesterTrainerName: String,
        requesterPartnerName: String,
        recipientUserId: String,
        recipientTrainerName: String,
        recipientPartnerName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(recipientUserId.isNotBlank() && recipientUserId != uid) {
            "Invalid recipient for friend request"
        }
        val docId = FriendConnection.buildFriendshipDocId(uid, recipientUserId)
        val docRef = db.collection("friendships").document(docId)
        val existingSnap = runCatching { docRef.get().await() }.getOrNull()
        if (existingSnap != null && existingSnap.exists()) {
            val existingStatus = existingSnap.getString("status").orEmpty()
            val existingRecipient = existingSnap.getString("recipientUserId").orEmpty()
            if (existingStatus == "ACCEPTED") {
                return@runCatching docRef.id
            }
            if (existingStatus == "PENDING" && existingRecipient == uid) {
                acceptFriendRequest(docId, requesterTrainerName, requesterPartnerName).getOrThrow()
                return@runCatching docRef.id
            }
            if (existingStatus == "PENDING") {
                return@runCatching docRef.id
            }
        }
        val payload = mapOf(
            "requesterUserId" to uid,
            "requesterTrainerName" to requesterTrainerName.trim().take(40).ifBlank { "Tamer" },
            "requesterPartnerName" to requesterPartnerName.trim().take(50).ifBlank { "Agumon" },
            "recipientUserId" to recipientUserId.trim().take(128),
            "recipientTrainerName" to recipientTrainerName.trim().take(40).ifBlank { "Tamer" },
            "recipientPartnerName" to recipientPartnerName.trim().take(50).ifBlank { "Agumon" },
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun acceptFriendRequest(
        friendshipId: String,
        recipientTrainerName: String,
        recipientPartnerName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(friendshipId.isNotBlank()) { "Friendship ID must not be blank" }
        val docRef = db.collection("friendships").document(friendshipId)
        val updates = mapOf(
            "status" to "ACCEPTED",
            "recipientTrainerName" to recipientTrainerName.trim().take(40).ifBlank { "Tamer" },
            "recipientPartnerName" to recipientPartnerName.trim().take(50).ifBlank { "Agumon" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> = runCatching {
        requireUserId()
        require(friendshipId.isNotBlank()) { "Friendship ID must not be blank" }
        val docRef = db.collection("friendships").document(friendshipId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadDigiCoins(userId: String): Int {
        val p = prefs() ?: return DigimonCatalog.STARTING_DIGI_COINS
        return p.getInt("coins_$userId", DigimonCatalog.STARTING_DIGI_COINS).coerceAtLeast(0)
    }

    fun saveDigiCoins(userId: String, coins: Int) {
        prefs()?.edit()?.putInt("coins_$userId", coins.coerceAtLeast(0))?.apply()
    }

    fun loadTammingBerryCount(userId: String): Int {
        val p = prefs() ?: return 0
        return p.getInt("berries_$userId", 0).coerceAtLeast(0)
    }

    fun saveTammingBerryCount(userId: String, count: Int) {
        prefs()?.edit()?.putInt("berries_$userId", count.coerceAtLeast(0))?.apply()
    }

    fun loadMonsterPartsCounts(userId: String): Pair<Int, Int> {
        val p = prefs() ?: return 0 to 0
        val standard = p.getInt("parts_std_$userId", 0).coerceAtLeast(0)
        val ultraRare = p.getInt("parts_ultra_$userId", 0).coerceAtLeast(0)
        return standard to ultraRare
    }

    fun saveMonsterPartsCounts(userId: String, standardParts: Int, ultraRareParts: Int) {
        prefs()?.edit()
            ?.putInt("parts_std_$userId", standardParts.coerceAtLeast(0))
            ?.putInt("parts_ultra_$userId", ultraRareParts.coerceAtLeast(0))
            ?.apply()
    }

    fun loadFoodInventory(userId: String): Map<DigiRation, Int> {
        val p = prefs() ?: return emptyMap()
        val raw = p.getString("food_$userId", null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            val result = mutableMapOf<DigiRation, Int>()
            for (ration in DigiRation.entries) {
                val qty = json.optInt(ration.name, 0)
                if (qty > 0) {
                    result[ration] = qty
                }
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun saveFoodInventory(userId: String, inventory: Map<DigiRation, Int>) {
        val p = prefs() ?: return
        val json = JSONObject()
        inventory.forEach { (ration, qty) ->
            if (qty > 0) json.put(ration.name, qty)
        }
        p.edit().putString("food_$userId", json.toString()).apply()
    }

    fun loadDailyLoginState(userId: String): Pair<String, String?> {
        val p = prefs() ?: return "" to null
        val dateKey = p.getString("daily_date_$userId", "").orEmpty()
        val summary = p.getString("daily_summary_$userId", null)
        return dateKey to summary
    }

    fun saveDailyLoginState(userId: String, dateKey: String, summary: String?) {
        val p = prefs() ?: return
        p.edit()
            .putString("daily_date_$userId", dateKey)
            .putString("daily_summary_$userId", summary)
            .apply()
    }

    fun loadLocalCreatures(userId: String): List<DigimonCreature> {
        val p = prefs() ?: return emptyList()
        val raw = p.getString("local_creatures_$userId", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<DigimonCreature>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val formsArr = obj.optJSONArray("unlockedForms") ?: JSONArray()
                val forms = mutableListOf<String>()
                for (j in 0 until formsArr.length()) {
                    val f = formsArr.optString(j)
                    if (f.isNotBlank()) forms.add(f)
                }
                val skillsArr = obj.optJSONArray("unlockedSkillNodes") ?: JSONArray()
                val skills = mutableListOf<String>()
                for (j in 0 until skillsArr.length()) {
                    val s = skillsArr.optString(j)
                    if (s.isNotBlank()) skills.add(s)
                }
                list.add(
                    DigimonCreature(
                        id = obj.optString("id"),
                        userId = obj.optString("userId", userId),
                        nickname = obj.optString("nickname", "Agumon"),
                        speciesId = obj.optString("speciesId", "agumon"),
                        speciesName = obj.optString("speciesName", "Agumon"),
                        stage = obj.optString("stage", "Rookie"),
                        attribute = obj.optString("attribute", "Vaccine"),
                        health = obj.optInt("health", 100),
                        stamina = obj.optInt("stamina", 100),
                        hunger = obj.optInt("hunger", 85),
                        evolutionProgress = obj.optInt("evolutionProgress", 0),
                        attack = obj.optInt("attack", 52),
                        defense = obj.optInt("defense", 42),
                        speed = obj.optInt("speed", 40),
                        wisdom = obj.optInt("wisdom", 38),
                        bondLevel = obj.optInt("bondLevel", 10),
                        careMistakes = obj.optInt("careMistakes", 0),
                        isResting = obj.optBoolean("isResting", false),
                        unlockedForms = forms.ifEmpty { listOf(obj.optString("speciesId", "agumon")) },
                        unlockedSkillNodes = skills,
                        nonTrainingBonusStats = obj.optInt("nonTrainingBonusStats", 0),
                        activeExpeditionId = obj.optString("activeExpeditionId", ""),
                        expeditionStartMs = obj.optLong("expeditionStartMs", 0L),
                        expeditionEndMs = obj.optLong("expeditionEndMs", 0L),
                        completedExpeditionsCount = obj.optInt("completedExpeditionsCount", 0),
                        offlineTrainingEndMs = obj.optLong("offlineTrainingEndMs", 0L),
                        offlineTrainingLastTickMs = obj.optLong("offlineTrainingLastTickMs", 0L),
                        recoveryEndsAtMs = obj.optLong("recoveryEndsAtMs", 0L),
                        lastUpdatedMs = obj.optLong("lastUpdatedMs", System.currentTimeMillis())
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveLocalCreatures(userId: String, creatures: List<DigimonCreature>) {
        val p = prefs() ?: return
        val arr = JSONArray()
        for (c in creatures) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("userId", c.userId)
            obj.put("nickname", c.nickname)
            obj.put("speciesId", c.speciesId)
            obj.put("speciesName", c.speciesName)
            obj.put("stage", c.stage)
            obj.put("attribute", c.attribute)
            obj.put("health", c.health)
            obj.put("stamina", c.stamina)
            obj.put("hunger", c.hunger)
            obj.put("evolutionProgress", c.evolutionProgress)
            obj.put("attack", c.attack)
            obj.put("defense", c.defense)
            obj.put("speed", c.speed)
            obj.put("wisdom", c.wisdom)
            obj.put("bondLevel", c.bondLevel)
            obj.put("careMistakes", c.careMistakes)
            obj.put("isResting", c.isResting)
            obj.put("unlockedForms", JSONArray(c.unlockedForms))
            obj.put("unlockedSkillNodes", JSONArray(c.allUnlockedSkillNodeIds().toList()))
            obj.put("nonTrainingBonusStats", c.persistedNonTrainingBonusStats())
            obj.put("activeExpeditionId", c.activeExpeditionId)
            obj.put("expeditionStartMs", c.expeditionStartMs)
            obj.put("expeditionEndMs", c.expeditionEndMs)
            obj.put("completedExpeditionsCount", c.persistedCompletedExpeditions())
            obj.put("offlineTrainingEndMs", c.persistedOfflineTrainingEndMs())
            obj.put("offlineTrainingLastTickMs", c.persistedOfflineTrainingLastTickMs())
            obj.put("recoveryEndsAtMs", c.persistedRecoveryEndsAtMs())
            obj.put("lastUpdatedMs", c.lastUpdatedMs)
            arr.put(obj)
        }
        p.edit().putString("local_creatures_$userId", arr.toString()).apply()
    }

    fun clearLocalUserProgress(userId: String) {
        val p = prefs() ?: return
        p.edit()
            .remove("coins_$userId")
            .remove("berries_$userId")
            .remove("parts_std_$userId")
            .remove("parts_ultra_$userId")
            .remove("food_$userId")
            .remove("daily_date_$userId")
            .remove("daily_summary_$userId")
            .remove("local_creatures_$userId")
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "digiverse_player_state"
        val VALID_STAGES = setOf("Rookie", "Champion", "Ultimate", "Mega")
        val VALID_ATTRIBUTES = setOf("Vaccine", "Data", "Virus", "Free")
        val VALID_ACTIVITY_TYPES = setOf(
            "Power Drill", "Speed Drill", "Defense Drill", "Wisdom Drill",
            "Nutrient Feed", "Medical Care", "Rest Cycle", "Digivolution",
            "Arena Sparring", "Monster Trade", "Mythic Ascension"
        )
        val VALID_GRADES = setOf("S", "A", "B", "C", "CARE", "EVO")
        val VALID_SEVERITIES = setOf("INFO", "WARNING", "CRITICAL", "EVOLUTION")
        val DATE_KEY_REGEX = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    }
}
