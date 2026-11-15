package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.DigiRation
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.DrillType
import com.example.data.model.EvolutionBranchRequirement
import com.example.data.model.ExpeditionSpec
import com.example.data.model.FriendConnection
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MonsterSkillNode
import com.example.data.model.PetNotification
import com.example.data.model.TradeListing
import com.example.data.model.TrainingLogEntry
import com.example.data.model.WildEncounterState
import com.example.data.repository.DigiPulseRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SystemAlertEvent(
    val id: Int,
    val title: String,
    val message: String
)

data class SparringBattleResult(
    val opponentName: String,
    val opponentPartner: String,
    val playerCp: Int,
    val opponentCp: Int,
    val won: Boolean,
    val summaryText: String,
    val evoSyncBonus: Int,
    val xpBonus: Int
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val creatures: List<DigimonCreature> = emptyList(),
    val activeCreatureId: String = "",
    val trainingLogs: List<TrainingLogEntry> = emptyList(),
    val notifications: List<PetNotification> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val openTradeListings: List<TradeListing> = emptyList(),
    val myTradeListings: List<TradeListing> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val friendships: List<FriendConnection> = emptyList(),
    val activeDirectChatFriendId: String = "",
    val trainerName: String = "Tamer",
    val tamerTitle: String = "Digi-Link Vanguard",
    val nextStaminaTickSec: Int = 15,
    val nextHungerTickSec: Int = 25,
    val digiCoins: Int = DigimonCatalog.STARTING_DIGI_COINS,
    val nextDigiCoinTickSec: Int = DigimonCatalog.DIGI_COIN_INTERVAL_SEC,
    val liveHeartbeatBpm: Int = 78,
    val statusBannerMessage: String? = null,
    val recentEvolutionUnlocked: String? = null,
    val latestSparringResult: SparringBattleResult? = null,
    val wildEncounter: WildEncounterState? = null,
    val pendingTamedCreature: DigimonCreature? = null,
    val foodInventory: Map<DigiRation, Int> = emptyMap(),
    val tammingBerryCount: Int = 0,
    val dailyLoginClaimedToday: Boolean = false,
    val dailyLoginStreakDays: Int = 1,
    val lastDailyLoginRewardText: String? = null,
    val errorMessage: String? = null,
    val isGoogleSignedIn: Boolean = true
) {
    val activeCreature: DigimonCreature?
        get() = creatures.firstOrNull { it.id == activeCreatureId } ?: creatures.firstOrNull()

    val unreadNotificationCount: Int
        get() = notifications.count { !it.isRead }

    val needsStarterDraft: Boolean
        get() = !isLoading && creatures.isEmpty()

    val totalFoodOwned: Int
        get() = foodInventory.values.sum()

    fun getFoodCount(ration: DigiRation): Int = foodInventory[ration] ?: 0
}

class DigiPulseViewModel(
    private val repository: DigiPulseRepository,
    val currentUserId: String,
    initialTrainerName: String,
    val isGoogleSignedIn: Boolean = true
) : ViewModel() {

    private val _selectedCreatureId = MutableStateFlow("")
    private val _trainerName = MutableStateFlow(initialTrainerName.ifBlank { "CyberTamer" })
    private val _tamerTitle = MutableStateFlow("Rookie Tamer")
    private val _nextStaminaTickSec = MutableStateFlow(15)
    private val _nextHungerTickSec = MutableStateFlow(25)
    private val _digiCoins = MutableStateFlow(DigimonCatalog.STARTING_DIGI_COINS)
    private val _nextDigiCoinTickSec = MutableStateFlow(DigimonCatalog.DIGI_COIN_INTERVAL_SEC)
    private val _liveHeartbeatBpm = MutableStateFlow(78)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    private val _recentEvolutionUnlocked = MutableStateFlow<String?>(null)
    private val _latestSparringResult = MutableStateFlow<SparringBattleResult?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Optimistic local state so selecting 3 starters, trading, & care actions transition immediately
    private val _optimisticCreatures = MutableStateFlow<List<DigimonCreature>>(emptyList())
    private val _removedCreatureIds = MutableStateFlow<Set<String>>(emptySet())
    private val _optimisticLogs = MutableStateFlow<List<TrainingLogEntry>>(emptyList())
    private val _optimisticNotifications = MutableStateFlow<List<PetNotification>>(emptyList())
    private val _readNotificationKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _clearedNotificationKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _optimisticMyTrades = MutableStateFlow<List<TradeListing>>(emptyList())
    private val _completedOrCancelledTradeIds = MutableStateFlow<Set<String>>(emptySet())
    private val _optimisticChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _optimisticFriendships = MutableStateFlow<List<FriendConnection>>(emptyList())
    private val _removedFriendshipIds = MutableStateFlow<Set<String>>(emptySet())
    private val _activeDirectChatFriendId = MutableStateFlow("")
    private val _isResetting = MutableStateFlow(false)
    private val _wildEncounter = MutableStateFlow<WildEncounterState?>(null)
    private val _pendingTamedCreature = MutableStateFlow<DigimonCreature?>(null)
    private val _foodInventory = MutableStateFlow<Map<DigiRation, Int>>(emptyMap())
    private val _tammingBerryCount = MutableStateFlow(0)
    private val _lastClaimedDailyLoginDateKey = MutableStateFlow("")
    private val _lastDailyLoginRewardSummary = MutableStateFlow<String?>(null)

    private val _systemAlertEvents = MutableSharedFlow<SystemAlertEvent>(extraBufferCapacity = 8)
    val systemAlertEvents: SharedFlow<SystemAlertEvent> = _systemAlertEvents.asSharedFlow()

    // Track recently sent threshold alerts per creature to avoid duplicate spam
    private val alertedHungerLowIds = mutableSetOf<String>()
    private val alertedEvolutionReadyIds = mutableSetOf<String>()
    private val alertedHealthLowIds = mutableSetOf<String>()
    private val offlineCatchUpAppliedIds = mutableSetOf<String>()
    private val claimedTradeIds = mutableSetOf<String>()

    private val creaturesFlow = if (isGoogleSignedIn) {
        repository.observeCreatures()
            .catch { e ->
                handleStreamException("creatures", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val logsFlow = if (isGoogleSignedIn) {
        repository.observeTrainingLogs()
            .catch { e ->
                handleStreamException("trainingLogs", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val notificationsFlow = if (isGoogleSignedIn) {
        repository.observeNotifications()
            .catch { e ->
                handleStreamException("notifications", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val leaderboardFlow = if (isGoogleSignedIn) {
        repository.observeLeaderboard()
            .catch { e ->
                handleStreamException("leaderboard", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val openTradesFlow = if (isGoogleSignedIn) {
        repository.observeOpenTradeListings()
            .catch { e ->
                handleStreamException("openTrades", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val myTradesFlow = if (isGoogleSignedIn) {
        repository.observeMyTradeListings()
            .catch { e ->
                handleStreamException("myTrades", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val globalChatFlow = if (isGoogleSignedIn) {
        repository.observeGlobalChatMessages()
            .catch { e ->
                handleStreamException("globalChat", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val sentChatFlow = if (isGoogleSignedIn) {
        repository.observeSentChatMessages()
            .catch { e ->
                handleStreamException("sentChat", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val receivedChatFlow = if (isGoogleSignedIn) {
        repository.observeReceivedChatMessages()
            .catch { e ->
                handleStreamException("receivedChat", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val outgoingFriendshipsFlow = if (isGoogleSignedIn) {
        repository.observeOutgoingFriendships()
            .catch { e ->
                handleStreamException("outgoingFriends", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val incomingFriendshipsFlow = if (isGoogleSignedIn) {
        repository.observeIncomingFriendships()
            .catch { e ->
                handleStreamException("incomingFriends", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val cloudChatAndFriendsFlow = combine(
        combine(globalChatFlow, sentChatFlow, receivedChatFlow) { g, s, r ->
            (g + s + r).distinctBy { it.id }.sortedBy { it.createdAt?.seconds ?: 0L }
        },
        combine(outgoingFriendshipsFlow, incomingFriendshipsFlow) { outF, inF ->
            (outF + inF).distinctBy { it.id }
        }
    ) { chats, friends ->
        Pair(chats, friends)
    }

    private data class CloudStreams(
        val creatures: List<DigimonCreature>,
        val logs: List<TrainingLogEntry>,
        val notifications: List<PetNotification>,
        val leaderboard: List<LeaderboardEntry>,
        val openTrades: List<TradeListing>,
        val myTrades: List<TradeListing>,
        val chatMessages: List<ChatMessage>,
        val friendships: List<FriendConnection>
    )

    private data class OptimisticStreams(
        val creatures: List<DigimonCreature>,
        val removedCreatureIds: Set<String>,
        val logs: List<TrainingLogEntry>,
        val notifications: List<PetNotification>,
        val readNotificationKeys: Set<String>,
        val clearedNotificationKeys: Set<String>,
        val myTrades: List<TradeListing>,
        val closedTradeIds: Set<String>,
        val isResetting: Boolean,
        val chatMessages: List<ChatMessage>,
        val friendships: List<FriendConnection>,
        val removedFriendshipIds: Set<String>,
        val activeDirectChatFriendId: String
    )

    private val cloudStreamsFlow = combine(
        combine(creaturesFlow, logsFlow, notificationsFlow) { c, l, n -> Triple(c, l, n) },
        combine(leaderboardFlow, openTradesFlow, myTradesFlow) { lb, ot, mt -> Triple(lb, ot, mt) },
        cloudChatAndFriendsFlow
    ) { part1, part2, chatAndFriends ->
        CloudStreams(
            creatures = part1.first,
            logs = part1.second,
            notifications = part1.third,
            leaderboard = part2.first,
            openTrades = part2.second,
            myTrades = part2.third,
            chatMessages = chatAndFriends.first,
            friendships = chatAndFriends.second
        )
    }

    private val optimisticStreamsFlow = combine(
        combine(_optimisticCreatures, _removedCreatureIds, _optimisticLogs, _optimisticNotifications) { c, r, l, n ->
            Tuple4(c, r, l, n)
        },
        combine(_optimisticMyTrades, _completedOrCancelledTradeIds, _isResetting, _readNotificationKeys, _clearedNotificationKeys) { mt, ct, rst, rnk, cnk ->
            Tuple5(mt, ct, rst, rnk, cnk)
        },
        combine(_optimisticChatMessages, _optimisticFriendships, _removedFriendshipIds, _activeDirectChatFriendId) { cm, fr, rf, df ->
            Tuple4(cm, fr, rf, df)
        }
    ) { part1, part2, part3 ->
        OptimisticStreams(
            creatures = part1.first,
            removedCreatureIds = part1.second,
            logs = part1.third,
            notifications = part1.fourth,
            readNotificationKeys = part2.fourth,
            clearedNotificationKeys = part2.fifth,
            myTrades = part2.first,
            closedTradeIds = part2.second,
            isResetting = part2.third,
            chatMessages = part3.first,
            friendships = part3.second,
            removedFriendshipIds = part3.third,
            activeDirectChatFriendId = part3.fourth
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        cloudStreamsFlow,
        optimisticStreamsFlow,
        combine(
            _selectedCreatureId,
            _trainerName,
            _tamerTitle,
            _nextStaminaTickSec,
            _nextHungerTickSec
        ) { selectedId, name, title, staSec, hunSec ->
            Tuple5(selectedId, name, title, staSec, hunSec)
        },
        combine(
            _liveHeartbeatBpm,
            _statusBannerMessage,
            _recentEvolutionUnlocked,
            _latestSparringResult,
            _errorMessage
        ) { bpm, banner, evo, spar, err ->
            Tuple5(bpm, banner, evo, spar, err)
        },
        combine(
            combine(_digiCoins, _nextDigiCoinTickSec, _wildEncounter, _pendingTamedCreature) { coins, coinSec, wild, pendingTamed ->
                Tuple4(coins, coinSec, wild, pendingTamed)
            },
            _foodInventory,
            _tammingBerryCount,
            _lastClaimedDailyLoginDateKey,
            _lastDailyLoginRewardSummary
        ) { coinWild, foodInv, berries, claimedDateKey, claimSummary ->
            Tuple5(coinWild, foodInv, berries, claimedDateKey, claimSummary)
        }
    ) { cloud, optimistic, meta1, meta2, storeAndCoinMeta ->
        val coinMeta = storeAndCoinMeta.first
        val foodInv = storeAndCoinMeta.second
        val berryCount = storeAndCoinMeta.third
        val claimedDateKey = storeAndCoinMeta.fourth
        val claimSummary = storeAndCoinMeta.fifth
        val rawCreatures = if (optimistic.isResetting) {
            emptyList()
        } else if (optimistic.creatures.isNotEmpty() || optimistic.removedCreatureIds.isNotEmpty()) {
            val cloudFiltered = cloud.creatures.filter { it.id !in optimistic.removedCreatureIds }
            val cloudById = cloudFiltered.associateBy { it.id }
            val optFiltered = optimistic.creatures.filter { it.id !in optimistic.removedCreatureIds }
            val merged = optFiltered.map { opt ->
                val cld = cloudById[opt.id]
                val basePicked = if (cld != null && cld.lastUpdatedMs > opt.lastUpdatedMs) cld else opt
                val combinedSkills = (opt.allUnlockedSkillNodeIds() + (cld?.allUnlockedSkillNodeIds() ?: emptySet())).toList()
                val combinedBonus = maxOf(opt.persistedNonTrainingBonusStats(), cld?.persistedNonTrainingBonusStats() ?: 0)
                val combinedExpCount = maxOf(opt.persistedCompletedExpeditions(), cld?.persistedCompletedExpeditions() ?: 0)
                basePicked.copy(
                    unlockedSkillNodes = combinedSkills,
                    nonTrainingBonusStats = combinedBonus,
                    activeExpeditionId = opt.activeExpeditionId.ifBlank { cld?.activeExpeditionId.orEmpty() },
                    expeditionStartMs = if (opt.activeExpeditionId.isNotBlank()) opt.expeditionStartMs else (cld?.expeditionStartMs ?: 0L),
                    expeditionEndMs = if (opt.activeExpeditionId.isNotBlank()) opt.expeditionEndMs else (cld?.expeditionEndMs ?: 0L),
                    completedExpeditionsCount = combinedExpCount
                )
            }
            val extraCloud = cloudFiltered
                .filter { cld -> merged.none { it.id == cld.id } }
                .map { cld ->
                    cld.copy(
                        unlockedSkillNodes = cld.allUnlockedSkillNodeIds().toList(),
                        nonTrainingBonusStats = cld.persistedNonTrainingBonusStats(),
                        completedExpeditionsCount = cld.persistedCompletedExpeditions()
                    )
                }
            merged + extraCloud
        } else {
            cloud.creatures.map { cld ->
                cld.copy(
                    unlockedSkillNodes = cld.allUnlockedSkillNodeIds().toList(),
                    nonTrainingBonusStats = cld.persistedNonTrainingBonusStats(),
                    completedExpeditionsCount = cld.persistedCompletedExpeditions()
                )
            }
        }
        val creatures = rawCreatures.take(DigimonCatalog.MAX_SQUAD_SIZE)

        val logs = if (optimistic.isResetting) {
            emptyList()
        } else {
            (optimistic.logs + cloud.logs).distinctBy { "${it.creatureId}_${it.activityType}_${it.statDeltaSummary}_${it.dateKey}" }
        }

        val notifications = if (optimistic.isResetting) {
            emptyList()
        } else {
            val combined = optimistic.notifications + cloud.notifications
            val filtered = combined.filter { n ->
                n.id !in optimistic.clearedNotificationKeys &&
                    notificationDedupKey(n) !in optimistic.clearedNotificationKeys
            }
            filtered
                .groupBy { notificationDedupKey(it) }
                .values
                .map { group ->
                    val preferred = group.firstOrNull { it.id.isNotBlank() } ?: group.first()
                    val readState = group.any {
                        it.isRead ||
                            it.id in optimistic.readNotificationKeys ||
                            notificationDedupKey(it) in optimistic.readNotificationKeys
                    }
                    preferred.copy(isRead = readState)
                }
        }

        val cloudLeaderboard = cloud.leaderboard
        val selectedId = meta1.first.takeIf { id -> creatures.any { it.id == id } }
            ?: creatures.firstOrNull()?.id.orEmpty()
        val myEntry = cloudLeaderboard.firstOrNull { it.userId == currentUserId }
        val resolvedTrainerName = myEntry?.trainerName ?: meta1.second
        val resolvedTitle = myEntry?.tamerTitle ?: meta1.third

        val mergedLeaderboard = buildMergedLeaderboard(
            cloudEntries = cloudLeaderboard,
            creatures = creatures,
            logs = logs,
            trainerName = resolvedTrainerName,
            tamerTitle = resolvedTitle
        )

        val mergedMyTrades = (optimistic.myTrades + cloud.myTrades)
            .distinctBy { it.id }
            .filter { !(it.status == "CANCELLED" && it.id in optimistic.closedTradeIds) }

        val mergedOpenTrades = (cloud.openTrades + optimistic.myTrades.filter { it.status == "OPEN" })
            .distinctBy { it.id }
            .filter { it.id !in optimistic.closedTradeIds && it.status == "OPEN" }

        val mergedChatMessages = if (!isGoogleSignedIn) {
            emptyList()
        } else {
            (cloud.chatMessages + optimistic.chatMessages)
                .distinctBy { it.id }
                .sortedBy { it.createdAt?.seconds ?: 0L }
        }

        val mergedFriendships = if (!isGoogleSignedIn) {
            emptyList()
        } else {
            (optimistic.friendships + cloud.friendships)
                .distinctBy { it.id }
                .filter { it.id !in optimistic.removedFriendshipIds }
        }

        val todayKey = todayDateKey()
        val logClaimToday = logs.firstOrNull {
            it.dateKey == todayKey && it.statDeltaSummary.startsWith("Daily Login Reward")
        }
        val isDailyClaimedToday = claimedDateKey == todayKey || logClaimToday != null
        val streakDays = ((logs.map { it.dateKey } + listOfNotNull(claimedDateKey.takeIf { it.isNotBlank() }))
            .distinct()
            .size)
            .coerceIn(1, 7)
        val resolvedDailyRewardText = claimSummary ?: logClaimToday?.statDeltaSummary

        DashboardUiState(
            isLoading = false,
            creatures = creatures,
            activeCreatureId = selectedId,
            trainingLogs = logs,
            notifications = notifications,
            leaderboard = mergedLeaderboard,
            openTradeListings = mergedOpenTrades,
            myTradeListings = mergedMyTrades,
            chatMessages = mergedChatMessages,
            friendships = mergedFriendships,
            activeDirectChatFriendId = optimistic.activeDirectChatFriendId,
            trainerName = resolvedTrainerName,
            tamerTitle = resolvedTitle,
            nextStaminaTickSec = meta1.fourth,
            nextHungerTickSec = meta1.fifth,
            digiCoins = coinMeta.first,
            nextDigiCoinTickSec = coinMeta.second,
            liveHeartbeatBpm = meta2.first,
            statusBannerMessage = meta2.second,
            recentEvolutionUnlocked = meta2.third,
            latestSparringResult = meta2.fourth,
            wildEncounter = coinMeta.third,
            pendingTamedCreature = coinMeta.fourth,
            foodInventory = foodInv,
            tammingBerryCount = berryCount,
            dailyLoginClaimedToday = isDailyClaimedToday,
            dailyLoginStreakDays = streakDays,
            lastDailyLoginRewardText = resolvedDailyRewardText,
            errorMessage = meta2.fifth,
            isGoogleSignedIn = isGoogleSignedIn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState(
            isLoading = false,
            trainerName = initialTrainerName,
            isGoogleSignedIn = isGoogleSignedIn
        )
    )

    init {
        scanWildMonster()
        startRealTimeTelemetryTicker()
        if (isGoogleSignedIn) {
            observeCompletedTradesToClaim()
        }
    }

    private fun observeCompletedTradesToClaim() {
        viewModelScope.launch {
            myTradesFlow.collect { myListings ->
                for (listing in myListings) {
                    if (listing.status == "COMPLETED" && !listing.claimedByCreator && claimedTradeIds.add(listing.id)) {
                        val claimRes = repository.claimCompletedTradeListing(listing)
                        claimRes.onSuccess { received ->
                            _removedCreatureIds.value = _removedCreatureIds.value - received.id
                            upsertOptimisticCreature(received)
                            val logEntry = TrainingLogEntry(
                                userId = currentUserId,
                                creatureId = received.id,
                                creatureName = received.nickname,
                                activityType = "Monster Trade",
                                grade = "EVO",
                                statDeltaSummary = "Trade Completed with ${listing.accepterTrainerName}! Received ${received.nickname} (${received.speciesName} • LVL ${received.currentLevel()}) for ${listing.offeredNickname}.",
                                xpGained = 200,
                                dateKey = todayDateKey()
                            )
                            val notifTitle = "PLAYER TRADE COMPLETED!"
                            val notifMsg = "${listing.accepterTrainerName} traded their ${received.nickname} (${received.speciesName}) for your ${listing.offeredNickname}!"
                            val notif = PetNotification(
                                userId = currentUserId,
                                creatureId = received.id,
                                creatureName = received.nickname,
                                severity = "EVOLUTION",
                                title = notifTitle,
                                message = notifMsg,
                                isRead = false
                            )
                            prependOptimisticLog(logEntry)
                            prependOptimisticNotification(notif)
                            _statusBannerMessage.value = notifMsg
                            _systemAlertEvents.tryEmit(SystemAlertEvent(listing.id.hashCode(), notifTitle, notifMsg))
                            repository.addTrainingLog(logEntry)
                            repository.addNotification(notif)
                            syncLeaderboardWithSquad()
                        }
                    }
                }
            }
        }
    }

    private fun upsertOptimisticCreature(updated: DigimonCreature) {
        val current = uiState.value.creatures
        val next = if (current.any { it.id == updated.id }) {
            current.map { if (it.id == updated.id) updated else it }
        } else {
            current + updated
        }
        _optimisticCreatures.value = next
    }

    private fun removeOptimisticCreature(creatureId: String) {
        _removedCreatureIds.value = _removedCreatureIds.value + creatureId
        _optimisticCreatures.value = _optimisticCreatures.value.filter { it.id != creatureId }
    }

    private fun notificationDedupKey(notif: PetNotification): String {
        return "${notif.creatureId}_${notif.title}_${notif.message}"
    }

    private fun prependOptimisticLog(log: TrainingLogEntry): TrainingLogEntry {
        val cleanId = log.id.ifBlank {
            "log_${System.currentTimeMillis()}_${log.creatureId.take(10).replace(Regex("[^a-zA-Z0-9_\\-]"), "")}"
        }
        val stamped = log.copy(
            id = cleanId,
            createdAt = log.createdAt ?: Timestamp.now()
        )
        _optimisticLogs.value = listOf(stamped) + _optimisticLogs.value
        return stamped
    }

    private fun prependOptimisticNotification(notif: PetNotification): PetNotification {
        val cleanId = notif.id.ifBlank {
            "notif_${System.currentTimeMillis()}_${notif.creatureId.take(10).replace(Regex("[^a-zA-Z0-9_\\-]"), "")}"
        }
        val nowTs = Timestamp.now()
        val stamped = notif.copy(
            id = cleanId,
            createdAt = notif.createdAt ?: nowTs,
            updatedAt = notif.updatedAt ?: nowTs
        )
        _clearedNotificationKeys.value = _clearedNotificationKeys.value - setOf(cleanId, notificationDedupKey(stamped))
        _optimisticNotifications.value = listOf(stamped) + _optimisticNotifications.value
        return stamped
    }

    fun draftStarterTrio(choices: List<com.example.data.model.StarterDraftChoice>) {
        val selectedOne = choices.take(1)
        if (selectedOne.isEmpty()) {
            _statusBannerMessage.value = "Please select 1 starter monster to begin your raising journey."
            return
        }

        val nowMs = System.currentTimeMillis()
        val nowTimestamp = Timestamp.now()
        val todayKey = todayDateKey()

        val draftedCreatures = selectedOne.mapIndexed { index, choice ->
            val spec = DigimonCatalog.getSpec(choice.speciesId)
            val cleanNickname = choice.customNickname.trim().take(36).ifBlank { spec.speciesName }
            DigimonCreature(
                id = "starter_${nowMs}_${index + 1}_${spec.speciesId}",
                userId = currentUserId,
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
                lastUpdatedMs = nowMs,
                createdAt = nowTimestamp,
                updatedAt = nowTimestamp
            )
        }

        val lead = draftedCreatures.first()
        val namesSummary = "${lead.nickname} (${lead.speciesName})"

        _isResetting.value = false
        _removedCreatureIds.value = emptySet()
        _optimisticCreatures.value = draftedCreatures
        _selectedCreatureId.value = lead.id
        _digiCoins.value = DigimonCatalog.STARTING_DIGI_COINS
        _nextDigiCoinTickSec.value = DigimonCatalog.DIGI_COIN_INTERVAL_SEC
        prependOptimisticLog(
            TrainingLogEntry(
                id = "log_draft_$nowMs",
                userId = currentUserId,
                creatureId = lead.id,
                creatureName = lead.nickname,
                activityType = "Digivolution",
                grade = "EVO",
                statDeltaSummary = "Drafted Starter Monster: $namesSummary • 60 DigiCoins Initialized",
                xpGained = 50,
                dateKey = todayKey,
                createdAt = nowTimestamp
            )
        )
        prependOptimisticNotification(
            PetNotification(
                id = "notif_draft_$nowMs",
                userId = currentUserId,
                creatureId = lead.id,
                creatureName = lead.nickname,
                severity = "INFO",
                title = "Starter Monster Synchronized!",
                message = "Raise $namesSummary via Training Drills (10 DigiCoins/drill, +5 Coins every 5m). Monsters level ONLY with training and require LVL 20+ to evolve!",
                isRead = false,
                createdAt = nowTimestamp,
                updatedAt = nowTimestamp
            )
        )
        _statusBannerMessage.value =
            "Starter Monster $namesSummary synchronized! You have 60 DigiCoins (+5 every 5m). Each training costs 10 DigiCoins and monsters level ONLY with training (reach LVL 20 to evolve)!"

        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.draftThreeStarters(selectedOne, _trainerName.value, draftedCreatures)
            }
        }
    }

    fun resetAllProgress() {
        _isResetting.value = true
        _optimisticCreatures.value = emptyList()
        _removedCreatureIds.value = emptySet()
        _optimisticLogs.value = emptyList()
        _optimisticNotifications.value = emptyList()
        _readNotificationKeys.value = emptySet()
        _clearedNotificationKeys.value = emptySet()
        _optimisticMyTrades.value = emptyList()
        _completedOrCancelledTradeIds.value = emptySet()
        _selectedCreatureId.value = ""
        _tamerTitle.value = "Rookie Tamer"
        _digiCoins.value = DigimonCatalog.STARTING_DIGI_COINS
        _nextDigiCoinTickSec.value = DigimonCatalog.DIGI_COIN_INTERVAL_SEC
        _foodInventory.value = emptyMap()
        _tammingBerryCount.value = 0
        _lastClaimedDailyLoginDateKey.value = ""
        _lastDailyLoginRewardSummary.value = null
        _recentEvolutionUnlocked.value = null
        _latestSparringResult.value = null
        alertedHungerLowIds.clear()
        alertedEvolutionReadyIds.clear()
        alertedHealthLowIds.clear()
        offlineCatchUpAppliedIds.clear()
        _statusBannerMessage.value =
            "Full reset complete! You have 60 DigiCoins ready. Choose your 1 Starter Monster below."

        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.resetAllPlayerProgress()
            }
        }
    }

    fun postCreatureForTrade(creatureId: String, requestedStageOrSpecies: String) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value =
                "Sign in with Google to trade monsters with other real-life players!"
            return
        }
        val currentSquad = uiState.value.creatures
        if (currentSquad.size <= 1) {
            _statusBannerMessage.value =
                "You must keep at least 1 active monster on your Holo-Deck! Hatch a new Digi-Egg first if you want to trade this partner."
            return
        }
        val offered = currentSquad.firstOrNull { it.id == creatureId } ?: return
        val nowMs = System.currentTimeMillis()
        val tradeId = "trade_${nowMs}_${offered.speciesId}"
        val nowTs = Timestamp.now()

        val optimisticListing = TradeListing(
            id = tradeId,
            creatorUserId = currentUserId,
            creatorTrainerName = _trainerName.value,
            offeredCreatureId = offered.id,
            offeredNickname = offered.nickname,
            offeredSpeciesId = offered.speciesId,
            offeredSpeciesName = offered.speciesName,
            offeredStage = offered.stage,
            offeredAttribute = offered.attribute,
            offeredLevel = offered.currentLevel(),
            offeredAttack = offered.attack,
            offeredDefense = offered.defense,
            offeredSpeed = offered.speed,
            offeredWisdom = offered.wisdom,
            offeredBondLevel = offered.bondLevel,
            requestedStageOrSpecies = requestedStageOrSpecies.ifBlank { "Any Stage" },
            status = "OPEN",
            claimedByCreator = false,
            createdAt = nowTs,
            updatedAt = nowTs
        )

        removeOptimisticCreature(offered.id)
        _optimisticMyTrades.value = listOf(optimisticListing) + _optimisticMyTrades.value
        val logEntry = TrainingLogEntry(
            userId = currentUserId,
            creatureId = offered.id,
            creatureName = offered.nickname,
            activityType = "Monster Trade",
            grade = "CARE",
            statDeltaSummary = "Listed ${offered.nickname} (${offered.speciesName} • LVL ${offered.currentLevel()}) on Global Player Trade Exchange (Seeking: $requestedStageOrSpecies)",
            xpGained = 50,
            dateKey = todayDateKey()
        )
        prependOptimisticLog(logEntry)
        _statusBannerMessage.value =
            "Posted ${offered.nickname} (LVL ${offered.currentLevel()} ${offered.speciesName}) to the Global Player Trade Exchange!"

        viewModelScope.launch {
            repository.createTradeListing(
                offeredCreature = offered,
                creatorTrainerName = _trainerName.value,
                requestedStageOrSpecies = requestedStageOrSpecies,
                customTradeId = tradeId
            )
            repository.addTrainingLog(logEntry)
            syncLeaderboardWithSquad()
        }
    }

    fun acceptPlayerTrade(listing: TradeListing, myCreatureIdToTrade: String) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value =
                "Sign in with Google to trade monsters with other real-life players!"
            return
        }
        if (listing.creatorUserId == currentUserId) {
            _statusBannerMessage.value = "That is your own trade listing! Use Cancel if you want to return it to your squad."
            return
        }
        val myCreature = uiState.value.creatures.firstOrNull { it.id == myCreatureIdToTrade } ?: return
        val nowMs = System.currentTimeMillis()

        val receivedLocal = DigimonCreature(
            id = "traded_${nowMs}_${listing.offeredSpeciesId}",
            userId = currentUserId,
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

        removeOptimisticCreature(myCreature.id)
        upsertOptimisticCreature(receivedLocal)
        _completedOrCancelledTradeIds.value = _completedOrCancelledTradeIds.value + listing.id
        _selectedCreatureId.value = receivedLocal.id

        val summary =
            "Traded ${myCreature.nickname} (LVL ${myCreature.currentLevel()}) with ${listing.creatorTrainerName} for ${receivedLocal.nickname} (${receivedLocal.speciesName} • LVL ${listing.offeredLevel})!"
        val tradeLog = TrainingLogEntry(
            userId = currentUserId,
            creatureId = receivedLocal.id,
            creatureName = receivedLocal.nickname,
            activityType = "Monster Trade",
            grade = "EVO",
            statDeltaSummary = summary,
            xpGained = 220,
            dateKey = todayDateKey()
        )
        prependOptimisticLog(tradeLog)
        _statusBannerMessage.value = "TRADE COMPLETE! $summary"

        viewModelScope.launch {
            val res = repository.acceptTradeListing(
                listing = listing,
                myCreatureToTrade = myCreature,
                accepterTrainerName = _trainerName.value
            )
            res.onSuccess { persistedCreature ->
                removeOptimisticCreature(receivedLocal.id)
                _removedCreatureIds.value = _removedCreatureIds.value - persistedCreature.id
                upsertOptimisticCreature(persistedCreature)
                _selectedCreatureId.value = persistedCreature.id
                repository.addTrainingLog(tradeLog.copy(creatureId = persistedCreature.id))
                syncLeaderboardWithSquad()
            }
        }
    }

    fun cancelMyTradeListing(listing: TradeListing) {
        val nowMs = System.currentTimeMillis()
        val restored = DigimonCreature(
            id = "restored_${nowMs}_${listing.offeredSpeciesId}",
            userId = currentUserId,
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

        _completedOrCancelledTradeIds.value = _completedOrCancelledTradeIds.value + listing.id
        _optimisticMyTrades.value = _optimisticMyTrades.value.filter { it.id != listing.id }
        upsertOptimisticCreature(restored)
        _selectedCreatureId.value = restored.id
        _statusBannerMessage.value =
            "Trade listing cancelled! ${restored.nickname} (${restored.speciesName}) returned to your squad."

        if (isGoogleSignedIn) {
            viewModelScope.launch {
                val res = repository.cancelTradeListing(listing)
                res.onSuccess { persisted ->
                    removeOptimisticCreature(restored.id)
                    _removedCreatureIds.value = _removedCreatureIds.value - persisted.id
                    upsertOptimisticCreature(persisted)
                    _selectedCreatureId.value = persisted.id
                    syncLeaderboardWithSquad()
                }
            }
        }
    }

    private suspend fun applyRealWorldOfflineCatchUpIfNeeded(creatures: List<DigimonCreature>) {
        val nowMs = System.currentTimeMillis()
        var coinCatchUpApplied = false
        for (creature in creatures) {
            if (creature.id.isBlank() || !offlineCatchUpAppliedIds.add(creature.id)) continue
            val elapsedMinutes = ((nowMs - creature.lastUpdatedMs).coerceAtLeast(0L)) / 60_000L
            if (elapsedMinutes >= 5L) {
                val cappedMins = elapsedMinutes.coerceAtMost(720L).toInt()
                if (!coinCatchUpApplied) {
                    val intervals = (cappedMins / 5).coerceAtLeast(1)
                    val offlineCoinsEarned = intervals * DigimonCatalog.DIGI_COINS_PER_INTERVAL
                    _digiCoins.value = _digiCoins.value + offlineCoinsEarned
                    coinCatchUpApplied = true
                }
                // Monsters ONLY gain Stamina when in Cryo-Rest; auto-wake when reaching 100% Stamina
                val staminaRecovered = if (creature.isResting) (cappedMins * 3) else 0
                val expSpec = if (creature.isOnExpedition()) DigimonCatalog.getExpedition(creature.activeExpeditionId) else null
                val expStaminaDrain = if (expSpec != null && !creature.isResting) (cappedMins / 4) * expSpec.staminaDrainPerCycle else 0
                val hungerDecayBase = if (creature.isResting) (cappedMins / 6) else (cappedMins / 3)
                val expHungerDrain = if (expSpec != null) (cappedMins / 4) * expSpec.hungerDrainPerCycle else 0
                val newHunger = (creature.hunger - hungerDecayBase - expHungerDrain).coerceIn(10, 100)
                val newStamina = (creature.stamina + staminaRecovered - expStaminaDrain).coerceIn(0, 100)
                val stillResting = if (creature.isResting && newStamina >= 100) false else creature.isResting

                val updated = creature.copy(
                    stamina = newStamina,
                    hunger = newHunger,
                    isResting = stillResting,
                    lastUpdatedMs = nowMs
                )
                upsertOptimisticCreature(updated)
                if (isGoogleSignedIn) {
                    repository.updateCreature(updated)
                }
            }
        }
    }

    private fun startRealTimeTelemetryTicker() {
        viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(1000L)
                tickCount++
                val nowMs = System.currentTimeMillis()

                val currentCreatures = uiState.value.creatures
                if (currentCreatures.isNotEmpty()) {
                    applyRealWorldOfflineCatchUpIfNeeded(currentCreatures)
                }

                // +5 DigiCoins every 5 minutes (300 seconds)
                val nextCoinSec = _nextDigiCoinTickSec.value - 1
                if (nextCoinSec <= 0) {
                    _digiCoins.value = _digiCoins.value + DigimonCatalog.DIGI_COINS_PER_INTERVAL
                    _nextDigiCoinTickSec.value = DigimonCatalog.DIGI_COIN_INTERVAL_SEC
                } else {
                    _nextDigiCoinTickSec.value = nextCoinSec
                }

                val staCountdown = if (_nextStaminaTickSec.value <= 1) 15 else _nextStaminaTickSec.value - 1
                val hunCountdown = if (_nextHungerTickSec.value <= 1) 25 else _nextHungerTickSec.value - 1
                _nextStaminaTickSec.value = staCountdown
                _nextHungerTickSec.value = hunCountdown

                val active = uiState.value.activeCreature
                if (active != null) {
                    val baseBpm = when {
                        active.isResting -> 56
                        active.canEvolveAnyBranch() -> 118
                        active.stamina < 25 -> 96
                        else -> 78
                    }
                    val jitter = ((tickCount % 5) - 2) * 2
                    _liveHeartbeatBpm.value = (baseBpm + jitter).coerceIn(48, 145)
                }

                // Update all squad creatures for Cryo-Rest stamina recovery & Expedition food/sleep drain
                for (creature in uiState.value.creatures) {
                    var changed = false
                    var newStamina = creature.stamina
                    var newHunger = creature.hunger
                    var newHealth = creature.health
                    var newIsResting = creature.isResting
                    var newExpEndMs = creature.expeditionEndMs
                    val expSpec = if (creature.isOnExpedition()) DigimonCatalog.getExpedition(creature.activeExpeditionId) else null

                    // If on expedition and needs food or sleep (or is sleeping in Cryo-Rest), pause expedition timer
                    if (expSpec != null && creature.needsExpeditionCare() && nowMs < newExpEndMs) {
                        newExpEndMs += 1000L
                        changed = true
                    }

                    if (staCountdown == 15) {
                        if (creature.isResting) {
                            // ONLY recover stamina while in Cryo-Rest! Auto-wake at 100% Stamina.
                            val gain = 6 + creature.cryoRegenBonus()
                            newStamina = (newStamina + gain).coerceIn(0, 100)
                            if (newStamina >= 100) {
                                newStamina = 100
                                newIsResting = false
                                _statusBannerMessage.value =
                                    "${creature.nickname} reached 100% Stamina in Cryo-Rest and woke up! Cannot go back to bed until Stamina is 50% or lower."
                            }
                            changed = true
                        } else if (expSpec != null && nowMs < newExpEndMs) {
                            // Expedition drains stamina (requires Cryo-Rest sleep on long expeditions)
                            newStamina = (newStamina - expSpec.staminaDrainPerCycle).coerceIn(0, 100)
                            changed = true
                        }
                    }

                    if (hunCountdown == 25 && !newIsResting) {
                        val baseDrain = 2
                        val expDrain = if (expSpec != null && nowMs < newExpEndMs) expSpec.hungerDrainPerCycle else 0
                        newHunger = (newHunger - baseDrain - expDrain).coerceIn(0, 100)
                        val healthDelta = if (newHunger < 15) -2 else if (newHunger >= 75 && newHealth < 100) 1 else 0
                        newHealth = (newHealth + healthDelta).coerceIn(0, 100)
                        changed = true
                    }

                    if (changed) {
                        val updated = creature.copy(
                            stamina = newStamina,
                            hunger = newHunger,
                            health = newHealth,
                            isResting = newIsResting,
                            expeditionEndMs = newExpEndMs,
                            lastUpdatedMs = nowMs
                        )
                        upsertOptimisticCreature(updated)
                        if (isGoogleSignedIn && (staCountdown == 15 || hunCountdown == 25)) {
                            repository.updateCreature(updated)
                        }
                        checkAndDispatchThresholds(updated)
                    }
                }
            }
        }
    }

    private suspend fun checkAndDispatchThresholds(creature: DigimonCreature) {
        if (creature.hunger <= 25 && alertedHungerLowIds.add(creature.id)) {
            val expSuffix = if (creature.isOnExpedition()) " Expedition paused until fed!" else ""
            val title = "${creature.nickname} Needs Food (${creature.hunger}%)"
            val msg = "Feed ${creature.nickname} a Ration now!$expSuffix"
            val notif = prependOptimisticNotification(
                PetNotification(
                    userId = currentUserId,
                    creatureId = creature.id,
                    creatureName = creature.nickname,
                    severity = "WARNING",
                    title = title,
                    message = msg,
                    isRead = false
                )
            )
            if (isGoogleSignedIn) {
                repository.addNotification(notif)
            }
            _systemAlertEvents.tryEmit(SystemAlertEvent(creature.id.hashCode() + 1, title, msg))
        } else if (creature.hunger > 45) {
            alertedHungerLowIds.remove(creature.id)
        }

        if (creature.health <= 35 && alertedHealthLowIds.add(creature.id)) {
            val title = "CRITICAL: ${creature.nickname} HP Integrity Low!"
            val msg = "Apply Nano-Heal immediately on the Health Widget to stabilize ${creature.nickname}'s core."
            val notif = prependOptimisticNotification(
                PetNotification(
                    userId = currentUserId,
                    creatureId = creature.id,
                    creatureName = creature.nickname,
                    severity = "CRITICAL",
                    title = title,
                    message = msg,
                    isRead = false
                )
            )
            if (isGoogleSignedIn) {
                repository.addNotification(notif)
            }
            _systemAlertEvents.tryEmit(SystemAlertEvent(creature.id.hashCode() + 2, title, msg))
        } else if (creature.health > 60) {
            alertedHealthLowIds.remove(creature.id)
        }

        // STRICT EVOLUTION GATE: Only tell the user the monster is ready for evolution when ALL requirements (including LVL 20+) are met!
        if (creature.canEvolveAnyBranch() && alertedEvolutionReadyIds.add(creature.id)) {
            val spec = DigimonCatalog.getSpec(creature.speciesId)
            val readyBranches = spec.evolutionBranches.filter { it.isStatsAndSyncMetBy(creature) }
            if (readyBranches.isNotEmpty()) {
                val branchNames = readyBranches.joinToString(", ") { it.targetSpeciesName }
                val title = "DIGIVOLUTION READY: ${creature.nickname} (LVL ${creature.currentLevel()})!"
                val msg = "${creature.nickname} met ALL level, stat, bond, and 100% Sync requirements to evolve into $branchNames!"
                val notif = prependOptimisticNotification(
                    PetNotification(
                        userId = currentUserId,
                        creatureId = creature.id,
                        creatureName = creature.nickname,
                        severity = "EVOLUTION",
                        title = title,
                        message = msg,
                        isRead = false
                    )
                )
                if (isGoogleSignedIn) {
                    repository.addNotification(notif)
                }
                _systemAlertEvents.tryEmit(SystemAlertEvent(creature.id.hashCode() + 3, title, msg))
            }
        } else if (!creature.canEvolveAnyBranch()) {
            alertedEvolutionReadyIds.remove(creature.id)
        }
    }

    fun selectCreature(creatureId: String) {
        _selectedCreatureId.value = creatureId
        _statusBannerMessage.value = null
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
        _errorMessage.value = null
    }

    fun dismissEvolutionOverlay() {
        _recentEvolutionUnlocked.value = null
    }

    fun dismissSparringResult() {
        _latestSparringResult.value = null
    }

    fun buyFoodInStore(ration: DigiRation, quantity: Int = 1) {
        val qty = quantity.coerceAtLeast(1)
        val totalCost = ration.costCoins * qty
        val currentCoins = _digiCoins.value
        if (totalCost > 0 && currentCoins < totalCost) {
            _statusBannerMessage.value =
                "Not enough DigiCoins to buy ${ration.displayName}! Costs $totalCost DigiCoins (you have $currentCoins). Wait for +5 DigiCoins every 5m or claim the FREE Cyber-Wafer!"
            return
        }
        if (totalCost > 0) {
            _digiCoins.value = (currentCoins - totalCost).coerceAtLeast(0)
        }
        val currentMap = _foodInventory.value.toMutableMap()
        val newOwned = (currentMap[ration] ?: 0) + qty
        currentMap[ration] = newOwned
        _foodInventory.value = currentMap

        _statusBannerMessage.value = if (totalCost == 0) {
            "Claimed +$qty ${ration.displayName} (FREE) in the Store! You now have $newOwned in your Food Bag to feed your pet."
        } else {
            "Bought +$qty ${ration.displayName} (-$totalCost Coins) in the Store! You now have $newOwned in your Food Bag."
        }
    }

    fun buyTammingBerriesInStore(quantity: Int = 1) {
        val qty = quantity.coerceAtLeast(1)
        val totalCost = DigimonCatalog.TAMMING_BERRY_COIN_COST * qty
        val currentCoins = _digiCoins.value
        if (currentCoins < totalCost) {
            _statusBannerMessage.value =
                "Not enough DigiCoins for $qty Tamming Berry! Costs $totalCost DigiCoins (${DigimonCatalog.TAMMING_BERRY_COIN_COST} Coins/berry, you have $currentCoins)."
            return
        }
        _digiCoins.value = (currentCoins - totalCost).coerceAtLeast(0)
        val newTotal = _tammingBerryCount.value + qty
        _tammingBerryCount.value = newTotal
        _statusBannerMessage.value =
            "Purchased +$qty Tamming Berry (-$totalCost DigiCoins)! You now have $newTotal Tamming Berries in your bag for Wild Taming."
    }

    fun claimDailyLoginReward() {
        val todayKey = todayDateKey()
        val currentState = uiState.value
        if (currentState.dailyLoginClaimedToday || _lastClaimedDailyLoginDateKey.value == todayKey) {
            _statusBannerMessage.value =
                "You already claimed today's Daily Login Reward! Come back tomorrow to keep your retention streak going."
            return
        }

        val streakDay = currentState.dailyLoginStreakDays.coerceIn(1, 7)
        // Grant either a small amount of DigiCoins (10-25 Coins) or a random Berry reward (1-2x Tamming Berries)
        val grantBerry = Random.nextBoolean()
        val rewardDetail = if (streakDay == 7) {
            val jackpotCoins = 25
            val jackpotBerries = 2
            _digiCoins.value = _digiCoins.value + jackpotCoins
            _tammingBerryCount.value = _tammingBerryCount.value + jackpotBerries
            "+$jackpotCoins DigiCoins & +${jackpotBerries}x Tamming Berries (Day 7 Streak Bonus!)"
        } else if (grantBerry) {
            val berryQty = if (Random.nextInt(100) < 35) 2 else 1
            _tammingBerryCount.value = _tammingBerryCount.value + berryQty
            "+${berryQty}x Random Tamming ${if (berryQty == 1) "Berry" else "Berries"} (added to Bag)"
        } else {
            val coinAmounts = listOf(10, 12, 15, 18, 20)
            val coinsWon = coinAmounts[Random.nextInt(coinAmounts.size)] + (streakDay - 1)
            _digiCoins.value = _digiCoins.value + coinsWon
            "+$coinsWon DigiCoins (added to Wallet)"
        }

        _lastClaimedDailyLoginDateKey.value = todayKey
        val fullSummary = "Daily Login Reward (Day $streakDay/7): $rewardDetail"
        _lastDailyLoginRewardSummary.value = fullSummary
        _statusBannerMessage.value = "★ $fullSummary!"

        val lead = currentState.activeCreature
        val logEntry = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = lead?.id ?: "daily_login",
                creatureName = lead?.nickname ?: "Tamer",
                activityType = "Nutrient Feed",
                grade = "CARE",
                statDeltaSummary = fullSummary.take(160),
                xpGained = 50,
                dateKey = todayKey
            )
        )
        val notif = prependOptimisticNotification(
            PetNotification(
                userId = currentUserId,
                creatureId = lead?.id ?: "daily_login",
                creatureName = lead?.nickname ?: "Tamer",
                severity = "INFO",
                title = "Daily Login Reward Claimed!",
                message = "Day $streakDay Streak Reward: $rewardDetail",
                isRead = false
            )
        )
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.addTrainingLog(logEntry)
                repository.addNotification(notif)
            }
        }
    }

    fun feedCreature(ration: DigiRation) {
        val active = uiState.value.activeCreature ?: return
        val currentMap = _foodInventory.value.toMutableMap()
        val ownedCount = currentMap[ration] ?: 0
        if (ownedCount <= 0) {
            _statusBannerMessage.value =
                "You have 0 ${ration.displayName} in your bag! You must buy/claim food in the Store tab first before feeding ${active.nickname}."
            return
        }

        // Consume 1 ration from Store inventory
        val remainingOwned = (ownedCount - 1).coerceAtLeast(0)
        if (remainingOwned == 0) {
            currentMap.remove(ration)
        } else {
            currentMap[ration] = remainingOwned
        }
        _foodInventory.value = currentMap

        viewModelScope.launch {
            val newHunger = (active.hunger + ration.hungerRestore).coerceIn(0, 100)
            val newHealth = (active.health + ration.healthRestore).coerceIn(0, 100)
            val newStamina = (active.stamina + ration.staminaRestore).coerceIn(0, 100)
            val bondBoost = if (ration.costCoins > 0) 2 else 1
            val newBond = (active.bondLevel + bondBoost).coerceIn(0, 100)
            val statGainTotal = ration.atkBonus + ration.defBonus + ration.spdBonus + ration.wisBonus
            val newBonusStats = active.persistedNonTrainingBonusStats() + statGainTotal
            val newForms = active.withUpdatedMetadataInForms(
                newSkillNodes = active.allUnlockedSkillNodeIds(),
                newBonusStats = newBonusStats,
                newExpeditionsCount = active.persistedCompletedExpeditions()
            )

            val updated = active.copy(
                hunger = newHunger,
                health = newHealth,
                stamina = newStamina,
                attack = active.attack + ration.atkBonus,
                defense = active.defense + ration.defBonus,
                speed = active.speed + ration.spdBonus,
                wisdom = active.wisdom + ration.wisBonus,
                nonTrainingBonusStats = newBonusStats,
                unlockedSkillNodes = active.allUnlockedSkillNodeIds().toList(),
                unlockedForms = newForms,
                bondLevel = newBond,
                lastUpdatedMs = System.currentTimeMillis()
            )
            val logEntry = prependOptimisticLog(
                TrainingLogEntry(
                    userId = currentUserId,
                    creatureId = active.id,
                    creatureName = active.nickname,
                    activityType = "Nutrient Feed",
                    grade = if (ration.costCoins > 0) "S" else "CARE",
                    statDeltaSummary = "${ration.displayName} (from Store Bag): ${ration.statBoostSummary} (LVL ${updated.currentLevel()}/100)",
                    xpGained = if (ration.costCoins > 0) 35 else 15,
                    dateKey = todayDateKey()
                )
            )
            upsertOptimisticCreature(updated)
            _statusBannerMessage.value = if (ration.costCoins > 0) {
                "Fed ${active.nickname} 1x ${ration.displayName} from your bag ($remainingOwned left): ${ration.statBoostSummary}!"
            } else {
                "Fed ${active.nickname} 1x ${ration.displayName} from your bag ($remainingOwned left): +${ration.hungerRestore} Hunger (0 Stats, 0 Stamina)."
            }
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    fun healCreature() {
        val active = uiState.value.activeCreature ?: return
        viewModelScope.launch {
            val healAmount = (100 - active.health).coerceAtLeast(15).coerceAtMost(45)
            val newHealth = (active.health + healAmount).coerceIn(0, 100)
            val newBond = (active.bondLevel + 3).coerceIn(0, 100)
            val updated = active.copy(
                health = newHealth,
                bondLevel = newBond,
                lastUpdatedMs = System.currentTimeMillis()
            )
            val logEntry = TrainingLogEntry(
                userId = currentUserId,
                creatureId = active.id,
                creatureName = active.nickname,
                activityType = "Medical Care",
                grade = "CARE",
                statDeltaSummary = "Nano-Heal Patch: +$healAmount HP, +3 Bond",
                xpGained = 40,
                dateKey = todayDateKey()
            )
            upsertOptimisticCreature(updated)
            prependOptimisticLog(logEntry)
            _statusBannerMessage.value = "${active.nickname} Nano-Heal complete! Integrity at $newHealth%."
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    fun toggleRestMode() {
        val active = uiState.value.activeCreature ?: return
        // Anti-Spam Cryo-Rest:
        // 1. If already resting, it must stay in Cryo-Rest until reaching 100% Stamina (cannot spam toggle!)
        if (active.isResting) {
            _statusBannerMessage.value =
                "${active.nickname} is already in Cryo-Rest (${active.stamina}%/100% STA). It must stay in Cryo-Rest until reaching 100% Stamina!"
            return
        }
        // 2. Cannot go back to bed until Stamina is 50% or lower!
        if (active.stamina > 50) {
            _statusBannerMessage.value =
                "${active.nickname} cannot go back to bed until Stamina is 50% or lower (currently ${active.stamina}% STA)!"
            return
        }
        // 3. Click once at <=50% Stamina to enter Cryo-Rest with 0 instant boost; recovers over time until 100%
        viewModelScope.launch {
            val updated = active.copy(
                isResting = true,
                lastUpdatedMs = System.currentTimeMillis()
            )
            upsertOptimisticCreature(updated)
            val logEntry = TrainingLogEntry(
                userId = currentUserId,
                creatureId = active.id,
                creatureName = active.nickname,
                activityType = "Rest Cycle",
                grade = "CARE",
                statDeltaSummary = "Entered Cryo-Rest at ${active.stamina}% STA (Resting until 100% STA)",
                xpGained = 30,
                dateKey = todayDateKey()
            )
            prependOptimisticLog(logEntry)
            _statusBannerMessage.value =
                "${active.nickname} entered Cryo-Rest (${active.stamina}% STA). It will recover Stamina over time until reaching 100%!"
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
        }
    }

    fun completeTrainingDrill(drillType: DrillType, scorePercent: Int) {
        val active = uiState.value.activeCreature ?: return
        val currentCoins = _digiCoins.value
        if (currentCoins < DigimonCatalog.TRAINING_DRILL_COIN_COST) {
            val mins = _nextDigiCoinTickSec.value / 60
            val secs = _nextDigiCoinTickSec.value % 60
            _statusBannerMessage.value =
                "Not enough DigiCoins! Each training costs ${DigimonCatalog.TRAINING_DRILL_COIN_COST} DigiCoins (you have $currentCoins). Gain +${DigimonCatalog.DIGI_COINS_PER_INTERVAL} DigiCoins every 5m (next in %d:%02d).".format(
                    Locale.US,
                    mins,
                    secs
                )
            return
        }

        // Deduct 10 DigiCoins per training drill
        val remainingCoins = (currentCoins - DigimonCatalog.TRAINING_DRILL_COIN_COST).coerceAtLeast(0)
        _digiCoins.value = remainingCoins

        viewModelScope.launch {
            val grade = when {
                scorePercent >= 90 -> "S"
                scorePercent >= 75 -> "A"
                scorePercent >= 55 -> "B"
                else -> "C"
            }
            val primaryStatGain = when (grade) {
                "S" -> 3
                "A" -> 2
                "B" -> 2
                else -> 1
            }
            val secondaryStatGain = when (grade) {
                "S" -> 1
                "A" -> 1
                else -> 0
            }
            val evoGain = when (grade) {
                "S" -> 5
                "A" -> 4
                "B" -> 3
                else -> 2
            }
            val xpEarned = when (grade) {
                "S" -> 90
                "A" -> 70
                "B" -> 55
                else -> 40
            }

            val newAtk = active.attack + if (drillType == DrillType.POWER) primaryStatGain else secondaryStatGain
            val newSpd = active.speed + if (drillType == DrillType.SPEED) primaryStatGain else secondaryStatGain
            val newDef = active.defense + if (drillType == DrillType.DEFENSE) primaryStatGain else secondaryStatGain
            val newWis = active.wisdom + if (drillType == DrillType.WISDOM) primaryStatGain else secondaryStatGain

            val newStamina = (active.stamina - drillType.staminaCost).coerceIn(0, 100)
            val newHunger = (active.hunger - drillType.hungerCost).coerceIn(0, 100)
            val newEvo = (active.evolutionProgress + evoGain).coerceIn(0, 100)
            val newBond = (active.bondLevel + 2).coerceIn(0, 100)
            val mistakeAdded = if (active.stamina < drillType.staminaCost || active.hunger < 15) 1 else 0

            val updated = active.copy(
                attack = newAtk,
                speed = newSpd,
                defense = newDef,
                wisdom = newWis,
                stamina = newStamina,
                hunger = newHunger,
                evolutionProgress = newEvo,
                bondLevel = newBond,
                careMistakes = active.careMistakes + mistakeAdded,
                isResting = false,
                lastUpdatedMs = System.currentTimeMillis()
            )
            val summary = "Grade $grade (-${DigimonCatalog.TRAINING_DRILL_COIN_COST} Coins, $remainingCoins left): +$primaryStatGain ${drillType.primaryStatLabel}, +$evoGain% EVO • LVL ${updated.currentLevel()}/100"
            val logEntry = prependOptimisticLog(
                TrainingLogEntry(
                    userId = currentUserId,
                    creatureId = active.id,
                    creatureName = active.nickname,
                    activityType = drillType.activityName,
                    grade = grade,
                    statDeltaSummary = summary,
                    xpGained = xpEarned,
                    dateKey = todayDateKey()
                )
            )
            upsertOptimisticCreature(updated)
            _statusBannerMessage.value = "${drillType.shortTitle} Complete! $summary"

            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    fun evolveCreature(branch: EvolutionBranchRequirement) {
        val active = uiState.value.activeCreature ?: return
        val minRequiredLvl = if (branch.isRareLevel100Branch) 100 else maxOf(20, branch.requiredLevel)
        if (!branch.isStatsAndSyncMetBy(active)) {
            val missing = buildList {
                if (active.currentLevel() < minRequiredLvl) {
                    add("LVL ${active.currentLevel()}/$minRequiredLvl (Monsters can only evolve at LVL $minRequiredLvl+)")
                }
                if (active.evolutionProgress < 100) {
                    add("Sync ${active.evolutionProgress}%/100%")
                }
                if (active.attack < branch.minAttack) add("ATK ${active.attack}/${branch.minAttack}")
                if (active.defense < branch.minDefense) add("DEF ${active.defense}/${branch.minDefense}")
                if (active.speed < branch.minSpeed) add("SPD ${active.speed}/${branch.minSpeed}")
                if (active.wisdom < branch.minWisdom) add("WIS ${active.wisdom}/${branch.minWisdom}")
                if (active.bondLevel < branch.minBond) add("Bond ${active.bondLevel}/${branch.minBond}")
            }
            _statusBannerMessage.value = "Cannot evolve ${active.nickname} yet! Missing: ${missing.joinToString(", ")}"
            return
        }

        val targetSpec = DigimonCatalog.getSpec(branch.targetSpeciesId)
        viewModelScope.launch {
            val mappedSkills = active.allUnlockedSkillNodeIds().map { id ->
                val idx = id.indexOf("_node_")
                if (idx >= 0) "${targetSpec.speciesId}${id.substring(idx)}" else id
            }
            val newForms = (active.copy(unlockedForms = active.unlockedForms + targetSpec.speciesId)).withUpdatedMetadataInForms(
                newSkillNodes = mappedSkills,
                newBonusStats = active.persistedNonTrainingBonusStats(),
                newExpeditionsCount = active.persistedCompletedExpeditions(),
                targetSpeciesOverride = targetSpec.speciesId
            )
            // Keep trained stats on normal evolution so monsters level ONLY with training drills
            val updated = active.copy(
                speciesId = targetSpec.speciesId,
                speciesName = targetSpec.speciesName,
                stage = targetSpec.stage,
                attribute = targetSpec.attribute,
                nickname = if (active.nickname == active.speciesName || active.nickname.startsWith("Nova") || active.nickname.startsWith("Frost") || active.nickname.startsWith("Aero")) {
                    targetSpec.speciesName
                } else {
                    active.nickname
                },
                health = 100,
                stamina = 100,
                evolutionProgress = if (branch.isRareLevel100Branch) 100 else 0,
                attack = if (branch.isRareLevel100Branch) maxOf(active.attack, targetSpec.baseAttack) else active.attack,
                defense = if (branch.isRareLevel100Branch) maxOf(active.defense, targetSpec.baseDefense) else active.defense,
                speed = if (branch.isRareLevel100Branch) maxOf(active.speed, targetSpec.baseSpeed) else active.speed,
                wisdom = if (branch.isRareLevel100Branch) maxOf(active.wisdom, targetSpec.baseWisdom) else active.wisdom,
                bondLevel = (active.bondLevel + 5).coerceIn(0, 100),
                unlockedSkillNodes = mappedSkills,
                nonTrainingBonusStats = active.persistedNonTrainingBonusStats(),
                completedExpeditionsCount = active.persistedCompletedExpeditions(),
                unlockedForms = newForms,
                lastUpdatedMs = System.currentTimeMillis()
            )

            val actType = if (branch.isRareLevel100Branch) "Mythic Ascension" else "Digivolution"
            val evoLog = prependOptimisticLog(
                TrainingLogEntry(
                    userId = currentUserId,
                    creatureId = active.id,
                    creatureName = targetSpec.speciesName,
                    activityType = actType,
                    grade = "EVO",
                    statDeltaSummary = if (branch.isRareLevel100Branch) {
                        "LVL 100 RARE MYTHIC EVOLUTION! ${active.speciesName} ascended into ${targetSpec.speciesName}!"
                    } else {
                        "Digivolved from ${active.speciesName} to ${targetSpec.speciesName} (${targetSpec.stage} • LVL ${updated.currentLevel()}/100)!"
                    },
                    xpGained = if (branch.isRareLevel100Branch) 1000 else 400,
                    dateKey = todayDateKey()
                )
            )
            val notifTitle = if (branch.isRareLevel100Branch) {
                "RARE LVL 100 MYTHIC UNLOCKED: ${targetSpec.speciesName}!"
            } else {
                "DIGIVOLUTION COMPLETE: ${targetSpec.speciesName}!"
            }
            val notifMsg = "${active.speciesName} evolved into ${targetSpec.speciesName} (${targetSpec.titleTag})! Signature Move: ${targetSpec.signatureMove}."
            val evoNotif = prependOptimisticNotification(
                PetNotification(
                    userId = currentUserId,
                    creatureId = active.id,
                    creatureName = targetSpec.speciesName,
                    severity = "EVOLUTION",
                    title = notifTitle,
                    message = notifMsg,
                    isRead = false
                )
            )
            upsertOptimisticCreature(updated)
            _systemAlertEvents.tryEmit(SystemAlertEvent(active.id.hashCode() + 99, notifTitle, notifMsg))
            _recentEvolutionUnlocked.value = "${active.speciesName} ➔ ${targetSpec.speciesName}"
            _statusBannerMessage.value = notifMsg

            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(evoLog)
                repository.addNotification(evoNotif)
            }
            syncLeaderboardWithSquad()
        }
    }

    // Egg hatching is removed; all monsters beyond the 1 starter must be tamed in the wild using Tamming Berries (2 DigiCoins each)
    fun scanWildMonster(preferredSpeciesId: String? = null) {
        val pool = DigimonCatalog.wildTamableSpecs
        val chosen = if (!preferredSpeciesId.isNullOrBlank()) {
            DigimonCatalog.getSpec(preferredSpeciesId)
        } else {
            pool[Random.nextInt(pool.size)]
        }
        val baseRate = when (chosen.stage) {
            "Rookie" -> Random.nextInt(55, 76)
            "Champion" -> Random.nextInt(38, 56)
            "Ultimate" -> Random.nextInt(25, 42)
            else -> Random.nextInt(15, 28)
        }
        val wildLvl = when (chosen.stage) {
            "Rookie" -> Random.nextInt(1, 12)
            "Champion" -> Random.nextInt(20, 35)
            "Ultimate" -> Random.nextInt(40, 58)
            else -> Random.nextInt(65, 80)
        }
        _wildEncounter.value = WildEncounterState(
            speciesId = chosen.speciesId,
            speciesName = chosen.speciesName,
            stage = chosen.stage,
            attribute = chosen.attribute,
            wildLevel = wildLvl,
            baseSuccessRatePercent = baseRate,
            tameProgress = 0,
            berriesUsed = 0,
            lastRollOutcomeTitle = "Wild ${chosen.speciesName} (${chosen.stage}) Appeared!",
            lastRollOutcomeDetail = "Base Tame Rate: $baseRate% (Random Roll per Tamming Berry • Costs ${DigimonCatalog.TAMMING_BERRY_COIN_COST} DigiCoins/berry)"
        )
    }

    fun throwTammingBerry() {
        val currentEncounter = _wildEncounter.value ?: run {
            scanWildMonster()
            return
        }
        if (currentEncounter.isTamed) {
            scanWildMonster()
            return
        }
        val ownedBerries = _tammingBerryCount.value
        if (ownedBerries <= 0) {
            _statusBannerMessage.value =
                "You have 0 Tamming Berries in your bag! Go to the Store tab to buy Tamming Berries (${DigimonCatalog.TAMMING_BERRY_COIN_COST} DigiCoins each) first."
            return
        }
        val remainingBerries = (ownedBerries - 1).coerceAtLeast(0)
        _tammingBerryCount.value = remainingBerries

        val activeBonus = uiState.value.activeCreature?.tammingRateBonusPercent() ?: 0
        val effectiveSuccessChance = (currentEncounter.baseSuccessRatePercent + activeBonus).coerceIn(15, 92)
        val roll = Random.nextInt(100)
        val newBerriesUsed = currentEncounter.berriesUsed + 1

        val (gain, outcomeTitle, outcomeDetail) = if (roll < effectiveSuccessChance) {
            val critRoll = Random.nextInt(100)
            when {
                critRoll < 14 -> Triple(
                    100,
                    "CRITICAL INSTANT TAME! (Roll ${100 - roll}/100)",
                    "Lucky Critical Roll! ${currentEncounter.speciesName} was instantly tamed by the Tamming Berry!"
                )
                critRoll < 50 -> {
                    val surge = Random.nextInt(30, 48)
                    Triple(
                        surge,
                        "SUPER TAME SURGE! (+$surge% Tame)",
                        "High random resonance roll! ${currentEncounter.speciesName}'s tame meter surged by +$surge%."
                    )
                }
                else -> {
                    val hit = Random.nextInt(16, 29)
                    Triple(
                        hit,
                        "TAMMING BERRY HIT! (+$hit% Tame)",
                        "Successful random roll! ${currentEncounter.speciesName}'s tame meter rose by +$hit%."
                    )
                }
            }
        } else {
            val graze = Random.nextInt(0, 6)
            Triple(
                graze,
                "WILD MONSTER RESISTED! (+$graze% Tame)",
                "Unlucky random roll (${100 - roll}% vs $effectiveSuccessChance% rate)! ${currentEncounter.speciesName} shook off the Tamming Berry."
            )
        }

        val updatedProgress = (currentEncounter.tameProgress + gain).coerceIn(0, 100)
        val nowTamed = updatedProgress >= 100

        _wildEncounter.value = currentEncounter.copy(
            tameProgress = updatedProgress,
            berriesUsed = newBerriesUsed,
            lastRollOutcomeTitle = outcomeTitle,
            lastRollOutcomeDetail = outcomeDetail,
            isTamed = nowTamed
        )

        if (nowTamed) {
            finalizeWildTaming(currentEncounter)
        } else {
            _statusBannerMessage.value = "$outcomeTitle — ${currentEncounter.speciesName} Tame Gauge: $updatedProgress/100% ($remainingBerries Berries left in bag)"
        }
    }

    private fun finalizeWildTaming(encounter: WildEncounterState) {
        val spec = DigimonCatalog.getSpec(encounter.speciesId)
        val nowMs = System.currentTimeMillis()
        val localId = "tamed_${nowMs}_${spec.speciesId}"
        val tamedBaseBonus = (spec.baseAttack + spec.baseDefense + spec.baseSpeed + spec.baseWisdom - 172).coerceAtLeast(0)
        val initialForms = buildList {
            add(spec.speciesId)
            if (tamedBaseBonus > 0) add("bn:$tamedBaseBonus")
        }
        val tamedCreature = DigimonCreature(
            id = localId,
            userId = currentUserId,
            nickname = spec.speciesName,
            speciesId = spec.speciesId,
            speciesName = spec.speciesName,
            stage = spec.stage,
            attribute = spec.attribute,
            health = 100,
            stamina = 100,
            hunger = 85,
            evolutionProgress = 0,
            attack = spec.baseAttack,
            defense = spec.baseDefense,
            speed = spec.baseSpeed,
            wisdom = spec.baseWisdom,
            nonTrainingBonusStats = tamedBaseBonus,
            bondLevel = 20,
            careMistakes = 0,
            isResting = false,
            unlockedForms = initialForms,
            lastUpdatedMs = nowMs
        )

        val currentSquad = uiState.value.creatures
        if (currentSquad.size >= DigimonCatalog.MAX_SQUAD_SIZE) {
            _pendingTamedCreature.value = tamedCreature
            _statusBannerMessage.value =
                "Tamed Wild ${spec.speciesName}! Your 4-Monster Squad is full (4/4) — choose a squad monster to swap/release."
            return
        }

        addTamedCreatureToSquad(tamedCreature)
    }

    private fun addTamedCreatureToSquad(tamedCreature: DigimonCreature) {
        val tameLog = TrainingLogEntry(
            userId = currentUserId,
            creatureId = tamedCreature.id,
            creatureName = tamedCreature.nickname,
            activityType = "Digivolution",
            grade = "EVO",
            statDeltaSummary = "Tamed Wild ${tamedCreature.speciesName} (${tamedCreature.stage}) in the wild with Tamming Berries!",
            xpGained = 120,
            dateKey = todayDateKey()
        )
        upsertOptimisticCreature(tamedCreature)
        prependOptimisticLog(tameLog)
        _selectedCreatureId.value = tamedCreature.id
        _pendingTamedCreature.value = null
        _statusBannerMessage.value =
            "Wild ${tamedCreature.speciesName} tamed and added to your Squad (${(uiState.value.creatures.size + 1).coerceAtMost(4)}/4)!"

        if (isGoogleSignedIn) {
            viewModelScope.launch {
                val result = repository.createCreature(tamedCreature)
                result.onSuccess { newId ->
                    _selectedCreatureId.value = newId
                    repository.addTrainingLog(tameLog.copy(creatureId = newId))
                    syncLeaderboardWithSquad()
                }
            }
        }
    }

    fun replaceSquadMonsterWithTamed(creatureIdToReplace: String) {
        val pending = _pendingTamedCreature.value ?: return
        val oldCreature = uiState.value.creatures.firstOrNull { it.id == creatureIdToReplace } ?: return
        _removedCreatureIds.value = _removedCreatureIds.value + oldCreature.id
        _optimisticCreatures.value = _optimisticCreatures.value.filter { it.id != oldCreature.id }
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.deleteCreature(oldCreature.id)
            }
        }
        addTamedCreatureToSquad(pending)
    }

    fun dismissPendingTamedCreature() {
        _pendingTamedCreature.value = null
    }

    fun startExpedition(expedition: ExpeditionSpec) {
        val active = uiState.value.activeCreature ?: return
        if (active.isOnExpedition()) {
            _statusBannerMessage.value = "${active.nickname} is already deployed on an expedition!"
            return
        }
        if (active.hunger <= 30 || active.stamina <= 30) {
            _statusBannerMessage.value =
                "${active.nickname} needs at least 31% Hunger and 31% Stamina before launching an expedition! Feed or Cryo-Rest first."
            return
        }
        val nowMs = System.currentTimeMillis()
        val endMs = nowMs + (expedition.durationSeconds * 1000L)
        val updated = active.copy(
            activeExpeditionId = expedition.id,
            expeditionStartMs = nowMs,
            expeditionEndMs = endMs,
            isResting = false,
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updated)
        val logEntry = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = active.id,
                creatureName = active.nickname,
                activityType = "Speed Drill",
                grade = "CARE",
                statDeltaSummary = "Deployed on ${expedition.title} (${expedition.durationLabel}). Monitor Food & Cryo-Rest sleep!",
                xpGained = 45,
                dateKey = todayDateKey()
            )
        )
        _statusBannerMessage.value =
            "${active.nickname} deployed on ${expedition.title} (${expedition.durationLabel})! Keep it fed and rested during the expedition."
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
        }
    }

    fun completeOrRecallExpedition(creatureId: String = uiState.value.activeCreature?.id.orEmpty()) {
        val target = uiState.value.creatures.firstOrNull { it.id == creatureId } ?: return
        if (!target.isOnExpedition()) return
        val expSpec = DigimonCatalog.getExpedition(target.activeExpeditionId)
        val nowMs = System.currentTimeMillis()
        val isCompleted = nowMs >= target.expeditionEndMs

        if (!isCompleted) {
            // Early recall
            val recalled = target.copy(
                activeExpeditionId = "",
                expeditionStartMs = 0L,
                expeditionEndMs = 0L,
                lastUpdatedMs = nowMs
            )
            upsertOptimisticCreature(recalled)
            _statusBannerMessage.value = "Recalled ${target.nickname} early from ${expSpec?.title ?: "Expedition"}."
            if (isGoogleSignedIn) {
                viewModelScope.launch { repository.updateCreature(recalled) }
            }
            return
        }

        // Completed expedition rewards
        val coinsWon = expSpec?.rewardCoins ?: 20
        val atkWon = expSpec?.rewardAtk ?: 1
        val defWon = expSpec?.rewardDef ?: 1
        val spdWon = expSpec?.rewardSpd ?: 1
        val wisWon = expSpec?.rewardWis ?: 1
        val syncWon = expSpec?.rewardEvoSync ?: 8
        val statSumWon = atkWon + defWon + spdWon + wisWon

        // Ultra Rare Chance (11% to 20% scaling by expedition tier) to drop Tamming Berries or 1 Random Food Item!
        val ultraRareChancePercent = (10 + (expSpec?.number ?: 1)).coerceIn(10, 20)
        val ultraRareDropText = if (Random.nextInt(100) < ultraRareChancePercent) {
            if (Random.nextBoolean()) {
                val berryBonus = if ((expSpec?.durationMinutes ?: 5) >= 240) 2 else 1
                _tammingBerryCount.value = _tammingBerryCount.value + berryBonus
                " • ★ ULTRA RARE DROP: +${berryBonus}x Tamming ${if (berryBonus == 1) "Berry" else "Berries"}!"
            } else {
                val randomFood = DigiRation.entries[Random.nextInt(DigiRation.entries.size)]
                val currentMap = _foodInventory.value.toMutableMap()
                currentMap[randomFood] = (currentMap[randomFood] ?: 0) + 1
                _foodInventory.value = currentMap.toMap()
                " • ★ ULTRA RARE DROP: +1x ${randomFood.displayName}!"
            }
        } else {
            ""
        }

        _digiCoins.value = _digiCoins.value + coinsWon
        val newBonusStats = target.persistedNonTrainingBonusStats() + statSumWon
        val newExpCount = target.persistedCompletedExpeditions() + 1
        val newForms = target.withUpdatedMetadataInForms(
            newSkillNodes = target.allUnlockedSkillNodeIds(),
            newBonusStats = newBonusStats,
            newExpeditionsCount = newExpCount
        )
        val updated = target.copy(
            activeExpeditionId = "",
            expeditionStartMs = 0L,
            expeditionEndMs = 0L,
            attack = target.attack + atkWon,
            defense = target.defense + defWon,
            speed = target.speed + spdWon,
            wisdom = target.wisdom + wisWon,
            nonTrainingBonusStats = newBonusStats,
            unlockedSkillNodes = target.allUnlockedSkillNodeIds().toList(),
            unlockedForms = newForms,
            evolutionProgress = (target.evolutionProgress + syncWon).coerceIn(0, 100),
            bondLevel = (target.bondLevel + 4).coerceIn(0, 100),
            completedExpeditionsCount = newExpCount,
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updated)
        val logEntry = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = target.id,
                creatureName = target.nickname,
                activityType = "Power Drill",
                grade = "S",
                statDeltaSummary = "Completed ${expSpec?.title}: +$coinsWon Coins, +$atkWon ATK/+$defWon DEF/+$spdWon SPD/+$wisWon WIS, +1 Skill Pt$ultraRareDropText",
                xpGained = 150,
                dateKey = todayDateKey()
            )
        )
        // Also trigger a fresh wild monster discovery on expedition completion
        scanWildMonster()
        _statusBannerMessage.value =
            "${target.nickname} completed ${expSpec?.title}! Earned +$coinsWon DigiCoins, +1 Skill Point, and spotted a Wild Monster!$ultraRareDropText"
        viewModelScope.launch {
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    fun unlockSkillNode(node: MonsterSkillNode) {
        val active = uiState.value.activeCreature ?: return
        val currentUnlocked = active.allUnlockedSkillNodeIds()
        if (node.nodeId in currentUnlocked) {
            _statusBannerMessage.value = "${node.title} is already unlocked for ${active.nickname}!"
            return
        }
        if (node.prerequisiteNodeId != null && node.prerequisiteNodeId !in currentUnlocked) {
            _statusBannerMessage.value = "Unlock the prerequisite Tier 1 skill node first!"
            return
        }
        val availableBefore = active.availableSkillPoints()
        if (availableBefore <= 0) {
            _statusBannerMessage.value =
                "No Skill Points available! Train ${active.nickname} (every 3 levels = +1 SP) or complete Expeditions (+1 SP) to earn more."
            return
        }
        val statBonusTotal = node.atkBonus + node.defBonus + node.spdBonus + node.wisBonus
        val newUnlockedList = (currentUnlocked + node.nodeId).toList()
        val originRookieId = active.unlockedEvolutionFormIds().firstOrNull { id ->
            DigimonCatalog.allSpecies[id]?.stage == "Rookie"
        }
        val originSpec = when {
            originRookieId != null -> DigimonCatalog.allSpecies[originRookieId]
            active.stage == "Rookie" -> DigimonCatalog.allSpecies[active.speciesId]
            else -> null
        }
        val originBaseSum = if (originSpec != null) {
            originSpec.baseAttack + originSpec.baseDefense + originSpec.baseSpeed + originSpec.baseWisdom
        } else {
            172
        }
        val currentStatSum = active.attack + active.defense + active.speed + active.wisdom
        val currentLevelTrainedStats = (active.currentLevel() - 1) * 9
        val baselineNonTrainingBonus = maxOf(
            active.persistedNonTrainingBonusStats(),
            (currentStatSum - originBaseSum - currentLevelTrainedStats).coerceAtLeast(0)
        )
        val newBonusStats = baselineNonTrainingBonus + statBonusTotal
        val newForms = active.withUpdatedMetadataInForms(
            newSkillNodes = newUnlockedList,
            newBonusStats = newBonusStats,
            newExpeditionsCount = active.persistedCompletedExpeditions()
        )
        val updated = active.copy(
            attack = active.attack + node.atkBonus,
            defense = active.defense + node.defBonus,
            speed = active.speed + node.spdBonus,
            wisdom = active.wisdom + node.wisBonus,
            nonTrainingBonusStats = newBonusStats,
            unlockedSkillNodes = newUnlockedList,
            unlockedForms = newForms,
            lastUpdatedMs = System.currentTimeMillis()
        )
        upsertOptimisticCreature(updated)
        val logEntry = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = active.id,
                creatureName = active.nickname,
                activityType = "Digivolution",
                grade = "EVO",
                statDeltaSummary = "Unlocked Skill Node ${node.title} (${node.branchName}): ${node.description}",
                xpGained = 60,
                dateKey = todayDateKey()
            )
        )
        _statusBannerMessage.value =
            "Unlocked ${node.title} on ${active.nickname}'s Skill Tree! (-1 SP • ${updated.availableSkillPoints()} SP left)"
        viewModelScope.launch {
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    fun runSparringMatch(opponent: LeaderboardEntry) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value =
                "Rankings progression is locked without Google Sign-In! Sign in with Google to spar on the Global Rankings and climb the leaderboard."
            return
        }
        val active = uiState.value.activeCreature ?: return
        viewModelScope.launch {
            val squadCp = uiState.value.creatures.sumOf { it.combatPower() }.coerceAtLeast(active.combatPower())
            val attributeBonus = hasAttributeAdvantage(active.attribute, opponent.partnerAttribute)
            val effectivePlayerPower = (squadCp * (if (attributeBonus) 1.15 else 1.0) * (0.92 + (active.health / 500.0))).toInt()
            val won = effectivePlayerPower >= opponent.totalCombatPower

            val evoBonus = if (won) 4 else 2
            val xpBonus = if (won) 120 else 50

            // Sparring grants Leaderboard Wins, Bond, and Evolution Sync ONLY — monsters level ONLY with Training Drills!
            val updatedCreature = active.copy(
                stamina = (active.stamina - 12).coerceIn(0, 100),
                evolutionProgress = (active.evolutionProgress + evoBonus).coerceIn(0, 100),
                bondLevel = (active.bondLevel + 2).coerceIn(0, 100),
                lastUpdatedMs = System.currentTimeMillis()
            )
            val outcomeTag = if (won) "VICTORY" else "TACTICAL DRAW"
            val summary = "$outcomeTag vs ${opponent.trainerName}'s ${opponent.partnerName}! +$evoBonus% Sync, +2 Bond (Leveling via Training Drills only • LVL ${updatedCreature.currentLevel()}/100)"
            val sparringLog = prependOptimisticLog(
                TrainingLogEntry(
                    userId = currentUserId,
                    creatureId = active.id,
                    creatureName = active.nickname,
                    activityType = "Arena Sparring",
                    grade = if (won) "S" else "A",
                    statDeltaSummary = summary,
                    xpGained = xpBonus,
                    dateKey = todayDateKey()
                )
            )

            upsertOptimisticCreature(updatedCreature)
            _latestSparringResult.value = SparringBattleResult(
                opponentName = opponent.trainerName,
                opponentPartner = opponent.partnerName,
                playerCp = effectivePlayerPower,
                opponentCp = opponent.totalCombatPower,
                won = won,
                summaryText = summary,
                evoSyncBonus = evoBonus,
                xpBonus = xpBonus
            )

            repository.updateCreature(updatedCreature)
            repository.addTrainingLog(sparringLog)
            checkAndDispatchThresholds(updatedCreature)
            syncLeaderboardWithSquad(extraSparringWin = if (won) 1 else 0)
        }
    }

    fun updateTrainerProfile(newCallsign: String, newTitle: String) {
        val cleanName = newCallsign.trim().take(36).ifBlank { "CyberTamer" }
        val cleanTitle = newTitle.trim().take(44).ifBlank { "Digi-Link Vanguard" }
        _trainerName.value = cleanName
        _tamerTitle.value = cleanTitle
        viewModelScope.launch {
            syncLeaderboardWithSquad()
            _statusBannerMessage.value = if (isGoogleSignedIn) {
                "Updated Tamer ID: $cleanName ($cleanTitle)"
            } else {
                "Updated Local Callsign: $cleanName (Sign in with Google to appear on Rankings)"
            }
        }
    }

    fun markNotificationRead(notifIdOrKey: String) {
        if (notifIdOrKey.isBlank()) return
        val matching = uiState.value.notifications.filter {
            it.id == notifIdOrKey || notificationDedupKey(it) == notifIdOrKey
        }
        val keysToAdd = buildSet {
            add(notifIdOrKey)
            matching.forEach {
                if (it.id.isNotBlank()) add(it.id)
                add(notificationDedupKey(it))
            }
        }
        _readNotificationKeys.value = _readNotificationKeys.value + keysToAdd
        _optimisticNotifications.value = _optimisticNotifications.value.map { notif ->
            if (notif.id in keysToAdd || notificationDedupKey(notif) in keysToAdd) {
                notif.copy(isRead = true)
            } else {
                notif
            }
        }
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                val cloudIds = matching.mapNotNull { it.id.takeIf(String::isNotBlank) }
                    .ifEmpty { listOf(notifIdOrKey) }
                cloudIds.forEach { id ->
                    repository.markNotificationRead(id)
                }
            }
        }
    }

    fun markAllNotificationsRead() {
        val currentNotifs = uiState.value.notifications
        val keysToMark = buildSet {
            currentNotifs.forEach {
                if (it.id.isNotBlank()) add(it.id)
                add(notificationDedupKey(it))
            }
            _optimisticNotifications.value.forEach {
                if (it.id.isNotBlank()) add(it.id)
                add(notificationDedupKey(it))
            }
        }
        _readNotificationKeys.value = _readNotificationKeys.value + keysToMark
        _optimisticNotifications.value = _optimisticNotifications.value.map { it.copy(isRead = true) }
        if (isGoogleSignedIn) {
            val unreadCloudIds = currentNotifs.filter { !it.isRead && it.id.isNotBlank() }.map { it.id }
            viewModelScope.launch {
                unreadCloudIds.forEach { id ->
                    repository.markNotificationRead(id)
                }
            }
        }
    }

    fun clearAllNotifications() {
        val currentNotifs = uiState.value.notifications
        val keysToClear = buildSet {
            currentNotifs.forEach {
                if (it.id.isNotBlank()) add(it.id)
                add(notificationDedupKey(it))
            }
            _optimisticNotifications.value.forEach {
                if (it.id.isNotBlank()) add(it.id)
                add(notificationDedupKey(it))
            }
        }
        _clearedNotificationKeys.value = _clearedNotificationKeys.value + keysToClear
        _optimisticNotifications.value = emptyList()
        _statusBannerMessage.value = "All pet status notifications cleared."
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.clearAllNotifications()
            }
        }
    }

    fun selectDirectChatFriend(friendUserId: String) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value = "You can only use Friends & Player Chat when logged into Google!"
            return
        }
        _activeDirectChatFriendId.value = friendUserId
    }

    fun sendChatMessage(messageText: String, recipientUserId: String = _activeDirectChatFriendId.value) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value = "You can only use Friends & Player Chat when logged into Google!"
            return
        }
        val cleanText = messageText.trim().take(300)
        if (cleanText.isBlank()) return
        val active = uiState.value.activeCreature
        val partnerName = active?.speciesName ?: "Agumon"
        val partnerStage = active?.stage ?: "Rookie"
        val nowMs = System.currentTimeMillis()
        val msgId = "msg_${nowMs}_${currentUserId.take(8)}"
        val channelId = if (recipientUserId.isBlank()) {
            "global"
        } else {
            FriendConnection.buildDirectChannelId(currentUserId, recipientUserId)
        }
        val optimisticMsg = ChatMessage(
            id = msgId,
            senderUserId = currentUserId,
            senderTrainerName = _trainerName.value,
            senderPartnerName = partnerName,
            senderPartnerStage = partnerStage,
            recipientUserId = recipientUserId,
            channelId = channelId,
            messageText = cleanText,
            createdAt = Timestamp.now()
        )
        _optimisticChatMessages.value = _optimisticChatMessages.value + optimisticMsg

        viewModelScope.launch {
            repository.sendChatMessage(
                senderTrainerName = _trainerName.value,
                senderPartnerName = partnerName,
                senderPartnerStage = partnerStage,
                messageText = cleanText,
                recipientUserId = recipientUserId,
                customMessageId = msgId
            )
        }
    }

    fun sendFriendRequest(
        targetUserId: String,
        targetTrainerName: String,
        targetPartnerName: String
    ) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value = "You can only use Friends & Player Chat when logged into Google!"
            return
        }
        val cleanTargetId = targetUserId.trim()
        if (cleanTargetId.isBlank() || cleanTargetId == currentUserId) {
            _statusBannerMessage.value = "Enter another real player's Tamer ID or select a player from Chat/Rankings."
            return
        }
        val docId = FriendConnection.buildFriendshipDocId(currentUserId, cleanTargetId)
        if (uiState.value.friendships.any { it.id == docId }) {
            _statusBannerMessage.value = "Friend connection or request with $targetTrainerName already exists!"
            return
        }
        val active = uiState.value.activeCreature
        val myPartner = active?.speciesName ?: "Agumon"
        val nowTs = Timestamp.now()
        val optimisticFriend = FriendConnection(
            id = docId,
            requesterUserId = currentUserId,
            requesterTrainerName = _trainerName.value,
            requesterPartnerName = myPartner,
            recipientUserId = cleanTargetId,
            recipientTrainerName = targetTrainerName.ifBlank { "Tamer" },
            recipientPartnerName = targetPartnerName.ifBlank { "Digimon" },
            status = "PENDING",
            createdAt = nowTs,
            updatedAt = nowTs
        )
        _removedFriendshipIds.value = _removedFriendshipIds.value - docId
        _optimisticFriendships.value = listOf(optimisticFriend) + _optimisticFriendships.value
        _statusBannerMessage.value = "Friend request sent to $targetTrainerName!"

        viewModelScope.launch {
            repository.sendFriendRequest(
                requesterTrainerName = _trainerName.value,
                requesterPartnerName = myPartner,
                recipientUserId = cleanTargetId,
                recipientTrainerName = targetTrainerName.ifBlank { "Tamer" },
                recipientPartnerName = targetPartnerName.ifBlank { "Digimon" }
            )
        }
    }

    fun acceptFriendRequest(friendship: FriendConnection) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value = "You can only use Friends & Player Chat when logged into Google!"
            return
        }
        val active = uiState.value.activeCreature
        val myPartner = active?.speciesName ?: "Agumon"
        val updated = friendship.copy(
            status = "ACCEPTED",
            recipientTrainerName = _trainerName.value,
            recipientPartnerName = myPartner,
            updatedAt = Timestamp.now()
        )
        _optimisticFriendships.value = listOf(updated) + _optimisticFriendships.value.filter { it.id != friendship.id }
        _statusBannerMessage.value = "You and ${friendship.requesterTrainerName} are now Friends! Tap CHAT to message them."

        viewModelScope.launch {
            repository.acceptFriendRequest(
                friendshipId = friendship.id,
                recipientTrainerName = _trainerName.value,
                recipientPartnerName = myPartner
            )
        }
    }

    fun removeFriend(friendship: FriendConnection) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value = "You can only use Friends & Player Chat when logged into Google!"
            return
        }
        _removedFriendshipIds.value = _removedFriendshipIds.value + friendship.id
        _optimisticFriendships.value = _optimisticFriendships.value.filter { it.id != friendship.id }
        if (_activeDirectChatFriendId.value == friendship.otherPlayerId(currentUserId)) {
            _activeDirectChatFriendId.value = ""
        }
        _statusBannerMessage.value = "Removed friend connection with ${friendship.otherPlayerName(currentUserId)}."

        viewModelScope.launch {
            repository.removeFriendship(friendship.id)
        }
    }

    private suspend fun syncLeaderboardWithSquad(extraSparringWin: Int = 0) {
        // Players who do not log into Google cannot progress on Rankings
        if (!isGoogleSignedIn) return
        val currentState = uiState.value
        val creatures = currentState.creatures
        if (creatures.isEmpty()) return
        val lead = currentState.activeCreature ?: creatures.first()
        val totalCp = creatures.sumOf { it.combatPower() }
        val totalForms = creatures.flatMap { it.unlockedEvolutionFormIds() }.distinct().size.coerceAtLeast(1)
        val distinctDays = currentState.trainingLogs.map { it.dateKey }.distinct().size.coerceAtLeast(1)
        val existingMyEntry = currentState.leaderboard.firstOrNull { it.userId == currentUserId }
        val currentWins = (existingMyEntry?.sparringWins ?: 0) + extraSparringWin

        repository.upsertLeaderboardEntry(
            LeaderboardEntry(
                id = currentUserId,
                userId = currentUserId,
                leagueId = "global",
                trainerName = _trainerName.value,
                tamerTitle = _tamerTitle.value,
                partnerName = lead.speciesName,
                partnerStage = lead.stage,
                partnerAttribute = lead.attribute,
                totalCombatPower = totalCp,
                evolutionsUnlocked = totalForms,
                trainingStreakDays = distinctDays,
                sparringWins = currentWins
            )
        )
    }

    private fun hasAttributeAdvantage(attacker: String, defender: String): Boolean {
        return (attacker == "Vaccine" && defender == "Virus") ||
            (attacker == "Virus" && defender == "Data") ||
            (attacker == "Data" && defender == "Vaccine")
    }

    private fun buildMergedLeaderboard(
        cloudEntries: List<LeaderboardEntry>,
        creatures: List<DigimonCreature>,
        logs: List<TrainingLogEntry>,
        trainerName: String,
        tamerTitle: String
    ): List<LeaderboardEntry> {
        val rivalBenchmarks = listOf(
            LeaderboardEntry(
                id = "rival_taichi",
                userId = "rival_taichi",
                leagueId = "global",
                trainerName = "Kamiya_Taichi",
                tamerTitle = "Crest of Courage",
                partnerName = "WarGreymon",
                partnerStage = "Mega",
                partnerAttribute = "Vaccine",
                totalCombatPower = 5420,
                evolutionsUnlocked = 8,
                trainingStreakDays = 14,
                sparringWins = 28
            ),
            LeaderboardEntry(
                id = "rival_yamato",
                userId = "rival_yamato",
                leagueId = "global",
                trainerName = "Ishida_Matt",
                tamerTitle = "Crest of Friendship",
                partnerName = "MetalGarurumon",
                partnerStage = "Mega",
                partnerAttribute = "Data",
                totalCombatPower = 4980,
                evolutionsUnlocked = 7,
                trainingStreakDays = 11,
                sparringWins = 22
            ),
            LeaderboardEntry(
                id = "rival_rika",
                userId = "rival_rika",
                leagueId = "global",
                trainerName = "Nonaka_Rika",
                tamerTitle = "Digital Queen",
                partnerName = "MetalGreymon",
                partnerStage = "Ultimate",
                partnerAttribute = "Vaccine",
                totalCombatPower = 3450,
                evolutionsUnlocked = 5,
                trainingStreakDays = 9,
                sparringWins = 17
            ),
            LeaderboardEntry(
                id = "rival_takato",
                userId = "rival_takato",
                leagueId = "global",
                trainerName = "Matsuki_Takato",
                tamerTitle = "Crimson Pioneer",
                partnerName = "WarGrowlmon",
                partnerStage = "Ultimate",
                partnerAttribute = "Virus",
                totalCombatPower = 2690,
                evolutionsUnlocked = 4,
                trainingStreakDays = 6,
                sparringWins = 11
            ),
            LeaderboardEntry(
                id = "rival_hikari",
                userId = "rival_hikari",
                leagueId = "global",
                trainerName = "Tamer_Kari",
                tamerTitle = "Crest of Light",
                partnerName = "Angemon",
                partnerStage = "Champion",
                partnerAttribute = "Vaccine",
                totalCombatPower = 1820,
                evolutionsUnlocked = 3,
                trainingStreakDays = 4,
                sparringWins = 6
            )
        )

        val lead = creatures.firstOrNull()
        val localPlayerFallback = if (isGoogleSignedIn && cloudEntries.none { it.userId == currentUserId } && lead != null) {
            listOf(
                LeaderboardEntry(
                    id = currentUserId,
                    userId = currentUserId,
                    leagueId = "global",
                    trainerName = trainerName,
                    tamerTitle = tamerTitle,
                    partnerName = lead.speciesName,
                    partnerStage = lead.stage,
                    partnerAttribute = lead.attribute,
                    totalCombatPower = creatures.sumOf { it.combatPower() },
                    evolutionsUnlocked = creatures.flatMap { it.unlockedEvolutionFormIds() }.distinct().size.coerceAtLeast(1),
                    trainingStreakDays = logs.map { it.dateKey }.distinct().size.coerceAtLeast(1),
                    sparringWins = 0
                )
            )
        } else {
            emptyList()
        }

        return (cloudEntries + localPlayerFallback + rivalBenchmarks)
            .distinctBy { it.userId }
            .sortedByDescending { it.totalCombatPower }
    }

    private fun handleStreamException(streamName: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        _errorMessage.value = rootError.message ?: "Failed to sync $streamName"
    }

    private fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private companion object {
        const val TAG = "DigiPulseVM"
    }
}
