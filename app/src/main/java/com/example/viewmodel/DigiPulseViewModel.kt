package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AcornOrbInventory
import com.example.data.model.AcornOrbType
import com.example.data.model.ChatMessage
import com.example.data.model.DaycareBreedingState
import com.example.data.model.DigiRation
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.DrillType
import com.example.data.model.EmeraldReserveEntry
import com.example.data.model.EvolutionBranchRequirement
import com.example.data.model.ExpeditionSpec
import com.example.data.model.ForgedRelicUpgradeState
import com.example.data.model.FriendConnection
import com.example.data.model.FrontierBpShopItem
import com.example.data.model.FrontierTowerState
import com.example.data.model.HeldRelicItem
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LongPlayRpgCatalog
import com.example.data.model.MonsterSkillNode
import com.example.data.model.PetNotification
import com.example.data.model.ResonanceRune
import com.example.data.model.SanctumLeaderSpec
import com.example.data.model.TamerPassState
import com.example.data.model.TamerQuestItem
import com.example.data.model.TamerQuestType
import com.example.data.model.TradeListing
import com.example.data.model.TrainingLogEntry
import com.example.data.model.TurnBasedBattleMode
import com.example.data.model.TurnBasedBattleState
import com.example.data.model.WildEncounterState
import com.example.data.model.WorldRegionBiome
import com.example.data.repository.DigiPulseRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONArray
import org.json.JSONObject
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
    val acornOrbInventory: Map<AcornOrbType, Int> = emptyMap(),
    val selectedAcornOrbType: AcornOrbType = AcornOrbType.STANDARD_ACORN_ORB,
    val emeraldReserveEntries: List<EmeraldReserveEntry> = emptyList(),
    val standardMonsterPartsCount: Int = 0,
    val ultraRareMonsterPartsCount: Int = 0,
    val dailyLoginClaimedToday: Boolean = false,
    val dailyLoginStreakDays: Int = 1,
    val lastDailyLoginRewardText: String? = null,
    val selectedWorldRegionId: String = "aurelia_meadows",
    val turnBasedBattle: TurnBasedBattleState? = null,
    val daycareState: DaycareBreedingState = DaycareBreedingState(),
    val frontierTowerState: FrontierTowerState = FrontierTowerState(),
    val tamerQuests: List<TamerQuestItem> = LongPlayRpgCatalog.defaultTamerQuests(),
    val tamerPassState: TamerPassState = TamerPassState(),
    val forgedRelics: Map<String, ForgedRelicUpgradeState> = emptyMap(),
    val unlockedRuneCodes: Set<String> = setOf(ResonanceRune.SOLAR_CRIT.runeCode, ResonanceRune.GALE_SWIFTNESS.runeCode),
    val errorMessage: String? = null,
    val isGoogleSignedIn: Boolean = true
) {
    val activeCreature: DigimonCreature?
        get() = creatures.firstOrNull { it.id == activeCreatureId } ?: creatures.firstOrNull()

    val selectedWorldRegion: WorldRegionBiome
        get() = LongPlayRpgCatalog.worldRegions.firstOrNull { it.regionId == selectedWorldRegionId }
            ?: LongPlayRpgCatalog.worldRegions.first()

    val claimableQuestCount: Int
        get() = tamerQuests.count { it.isCompleted && !it.isClaimed }

    fun getForgedRelicState(relic: HeldRelicItem): ForgedRelicUpgradeState =
        forgedRelics[relic.code] ?: ForgedRelicUpgradeState(relicCode = relic.code)

    val unreadNotificationCount: Int
        get() = notifications.count { !it.isRead }

    val needsStarterDraft: Boolean
        get() = !isLoading && creatures.isEmpty()

    val totalFoodOwned: Int
        get() = foodInventory.values.sum()

    val totalAcornOrbsOwned: Int
        get() = acornOrbInventory.values.sum().coerceAtLeast(tammingBerryCount)

    fun getAcornOrbCount(orbType: AcornOrbType): Int =
        acornOrbInventory[orbType] ?: if (orbType == AcornOrbType.STANDARD_ACORN_ORB) tammingBerryCount else 0

    val totalMonsterPartsCount: Int
        get() = standardMonsterPartsCount + ultraRareMonsterPartsCount

    val totalMonsterPartsCoinValue: Int
        get() = (standardMonsterPartsCount * DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS) +
            (ultraRareMonsterPartsCount * DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS)

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
    private val _acornOrbInventory = MutableStateFlow<Map<AcornOrbType, Int>>(emptyMap())
    private val _selectedAcornOrbType = MutableStateFlow(AcornOrbType.STANDARD_ACORN_ORB)
    private val _optimisticEmeraldReserve = MutableStateFlow<List<EmeraldReserveEntry>>(emptyList())
    private val _standardMonsterPartsCount = MutableStateFlow(0)
    private val _ultraRareMonsterPartsCount = MutableStateFlow(0)
    private val _lastClaimedDailyLoginDateKey = MutableStateFlow("")
    private val _lastDailyLoginRewardSummary = MutableStateFlow<String?>(null)
    private val _selectedWorldRegionId = MutableStateFlow("aurelia_meadows")
    private val _turnBasedBattle = MutableStateFlow<TurnBasedBattleState?>(null)
    private val _daycareState = MutableStateFlow(DaycareBreedingState())
    private val _frontierTowerState = MutableStateFlow(FrontierTowerState())
    private val _tamerQuests = MutableStateFlow(LongPlayRpgCatalog.defaultTamerQuests())
    private val _tamerPassState = MutableStateFlow(TamerPassState())
    private val _forgedRelics = MutableStateFlow<Map<String, ForgedRelicUpgradeState>>(emptyMap())
    private val _unlockedRuneCodes = MutableStateFlow(
        setOf(ResonanceRune.SOLAR_CRIT.runeCode, ResonanceRune.GALE_SWIFTNESS.runeCode)
    )

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

    private val acornOrbsFlow = if (isGoogleSignedIn) {
        repository.observeAcornOrbs()
            .catch { e ->
                handleStreamException("acornOrbs", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    private val emeraldReserveFlow = if (isGoogleSignedIn) {
        repository.observeEmeraldReserve()
            .catch { e ->
                handleStreamException("emeraldReserve", e)
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
        },
        combine(acornOrbsFlow, emeraldReserveFlow) { orbs, reserve ->
            Pair(orbs, reserve)
        }
    ) { chats, friends, orbsAndReserve ->
        Triple(chats, friends, orbsAndReserve)
    }

    private data class CloudStreams(
        val creatures: List<DigimonCreature>,
        val logs: List<TrainingLogEntry>,
        val notifications: List<PetNotification>,
        val leaderboard: List<LeaderboardEntry>,
        val openTrades: List<TradeListing>,
        val myTrades: List<TradeListing>,
        val chatMessages: List<ChatMessage>,
        val friendships: List<FriendConnection>,
        val acornOrbs: List<AcornOrbInventory>,
        val emeraldReserve: List<EmeraldReserveEntry>
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
            friendships = chatAndFriends.second,
            acornOrbs = chatAndFriends.third.first,
            emeraldReserve = chatAndFriends.third.second
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
            combine(
                _tammingBerryCount,
                _standardMonsterPartsCount,
                _ultraRareMonsterPartsCount,
                _acornOrbInventory,
                combine(_selectedAcornOrbType, _optimisticEmeraldReserve) { selOrb, optRes -> selOrb to optRes }
            ) { berries, stdParts, ultraParts, orbInv, selOrbAndRes ->
                Tuple5(berries, stdParts, ultraParts, orbInv, selOrbAndRes)
            },
            combine(_lastClaimedDailyLoginDateKey, _lastDailyLoginRewardSummary) { d, s -> d to s },
            combine(
                combine(_selectedWorldRegionId, _turnBasedBattle, _daycareState, _frontierTowerState) { reg, tb, dc, ft ->
                    Tuple4(reg, tb, dc, ft)
                },
                combine(_tamerQuests, _tamerPassState, _forgedRelics, _unlockedRuneCodes) { q, p, r, u ->
                    Tuple4(q, p, r, u)
                }
            ) { rpg1, rpg2 -> rpg1 to rpg2 }
        ) { coinWild, foodInv, berryAndParts, dailyMeta, rpgMeta ->
            Tuple5(coinWild, foodInv, berryAndParts, dailyMeta, rpgMeta)
        }
    ) { cloud, optimistic, meta1, meta2, storeAndCoinMeta ->
        val coinMeta = storeAndCoinMeta.first
        val foodInv = storeAndCoinMeta.second
        val berryCount = storeAndCoinMeta.third.first
        val stdPartsCount = storeAndCoinMeta.third.second
        val ultraPartsCount = storeAndCoinMeta.third.third
        val localOrbInv = storeAndCoinMeta.third.fourth
        val selectedOrbType = storeAndCoinMeta.third.fifth.first
        val optReserveList = storeAndCoinMeta.third.fifth.second
        val claimedDateKey = storeAndCoinMeta.fourth.first
        val claimSummary = storeAndCoinMeta.fourth.second
        val rpgPart1 = storeAndCoinMeta.fifth.first
        val rpgPart2 = storeAndCoinMeta.fifth.second
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
                val newerForOffline = if (opt.lastUpdatedMs >= (cld?.lastUpdatedMs ?: 0L)) opt else (cld ?: opt)
                val resolvedOfflineEnd = newerForOffline.persistedOfflineTrainingEndMs()
                val resolvedOfflineTick = newerForOffline.persistedOfflineTrainingLastTickMs()
                val resolvedOfflinePausedRem = newerForOffline.persistedOfflineTrainingPausedRemainingMs()
                val resolvedOfflinePausedCycle = newerForOffline.persistedOfflineTrainingPausedCycleElapsedMs()
                val resolvedRecoveryEnd = if (opt.lastUpdatedMs >= (cld?.lastUpdatedMs ?: 0L)) {
                    opt.persistedRecoveryEndsAtMs()
                } else {
                    cld?.persistedRecoveryEndsAtMs() ?: opt.persistedRecoveryEndsAtMs()
                }
                basePicked.copy(
                    unlockedSkillNodes = combinedSkills,
                    nonTrainingBonusStats = combinedBonus,
                    activeExpeditionId = opt.activeExpeditionId.ifBlank { cld?.activeExpeditionId.orEmpty() },
                    expeditionStartMs = if (opt.activeExpeditionId.isNotBlank()) opt.expeditionStartMs else (cld?.expeditionStartMs ?: 0L),
                    expeditionEndMs = if (opt.activeExpeditionId.isNotBlank()) opt.expeditionEndMs else (cld?.expeditionEndMs ?: 0L),
                    completedExpeditionsCount = combinedExpCount,
                    offlineTrainingEndMs = resolvedOfflineEnd,
                    offlineTrainingLastTickMs = resolvedOfflineTick,
                    offlineTrainingPausedRemainingMs = resolvedOfflinePausedRem,
                    offlineTrainingPausedCycleElapsedMs = resolvedOfflinePausedCycle,
                    recoveryEndsAtMs = resolvedRecoveryEnd
                )
            }
            val extraCloud = cloudFiltered
                .filter { cld -> merged.none { it.id == cld.id } }
                .map { cld ->
                    cld.copy(
                        unlockedSkillNodes = cld.allUnlockedSkillNodeIds().toList(),
                        nonTrainingBonusStats = cld.persistedNonTrainingBonusStats(),
                        completedExpeditionsCount = cld.persistedCompletedExpeditions(),
                        offlineTrainingEndMs = cld.persistedOfflineTrainingEndMs(),
                        offlineTrainingLastTickMs = cld.persistedOfflineTrainingLastTickMs(),
                        offlineTrainingPausedRemainingMs = cld.persistedOfflineTrainingPausedRemainingMs(),
                        offlineTrainingPausedCycleElapsedMs = cld.persistedOfflineTrainingPausedCycleElapsedMs(),
                        recoveryEndsAtMs = cld.persistedRecoveryEndsAtMs()
                    )
                }
            merged + extraCloud
        } else {
            cloud.creatures.map { cld ->
                cld.copy(
                    unlockedSkillNodes = cld.allUnlockedSkillNodeIds().toList(),
                    nonTrainingBonusStats = cld.persistedNonTrainingBonusStats(),
                    completedExpeditionsCount = cld.persistedCompletedExpeditions(),
                    offlineTrainingEndMs = cld.persistedOfflineTrainingEndMs(),
                    offlineTrainingLastTickMs = cld.persistedOfflineTrainingLastTickMs(),
                    offlineTrainingPausedRemainingMs = cld.persistedOfflineTrainingPausedRemainingMs(),
                    offlineTrainingPausedCycleElapsedMs = cld.persistedOfflineTrainingPausedCycleElapsedMs(),
                    recoveryEndsAtMs = cld.persistedRecoveryEndsAtMs()
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
            .filter { !it.isNpcListing() && !(it.status == "CANCELLED" && it.id in optimistic.closedTradeIds) }

        val mergedOpenTrades = (cloud.openTrades + optimistic.myTrades.filter { it.status == "OPEN" })
            .distinctBy { it.id }
            .filter { !it.isNpcListing() && it.id !in optimistic.closedTradeIds && it.status == "OPEN" }

        val mergedChatMessages = if (!isGoogleSignedIn) {
            emptyList()
        } else {
            (cloud.chatMessages + optimistic.chatMessages)
                .distinctBy { it.id }
                .filter { !it.isNpcMessage() }
                .sortedBy { it.createdAt?.seconds ?: 0L }
        }

        val mergedFriendships = if (!isGoogleSignedIn) {
            emptyList()
        } else {
            (optimistic.friendships + cloud.friendships)
                .distinctBy { it.id }
                .filter { !it.isNpcConnection() && it.id !in optimistic.removedFriendshipIds }
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

        val mergedOrbInventory = buildMap<AcornOrbType, Int> {
            val cloudByType = cloud.acornOrbs.associateBy { it.resolvedOrbType() }
            for (orbType in AcornOrbType.entries) {
                val localVal = localOrbInv[orbType] ?: if (orbType == AcornOrbType.STANDARD_ACORN_ORB) berryCount else 0
                val cloudVal = cloudByType[orbType]?.quantity
                val resolved = if (localOrbInv.containsKey(orbType)) {
                    localVal
                } else {
                    cloudVal ?: localVal
                }
                put(orbType, resolved.coerceAtLeast(0))
            }
            put(AcornOrbType.STANDARD_ACORN_ORB, berryCount.coerceAtLeast(0))
        }

        val mergedEmeraldReserve = if (optimistic.isResetting) {
            emptyList()
        } else {
            val partyIds = creatures.map { it.id }.toSet()
            val partySpecies = creatures.map { it.speciesId }.toSet()
            (optReserveList + cloud.emeraldReserve)
                .distinctBy { it.id.ifBlank { "${it.speciesId}_${it.capturedAtMs}" } }
                .map { entry ->
                    val isPartyMatch = entry.id in partyIds || (entry.inActiveParty && entry.speciesId in partySpecies)
                    entry.copy(inActiveParty = isPartyMatch)
                }
                .sortedByDescending { it.capturedAtMs }
        }

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
            acornOrbInventory = mergedOrbInventory,
            selectedAcornOrbType = selectedOrbType,
            emeraldReserveEntries = mergedEmeraldReserve,
            standardMonsterPartsCount = stdPartsCount,
            ultraRareMonsterPartsCount = ultraPartsCount,
            dailyLoginClaimedToday = isDailyClaimedToday,
            dailyLoginStreakDays = streakDays,
            lastDailyLoginRewardText = resolvedDailyRewardText,
            selectedWorldRegionId = rpgPart1.first,
            turnBasedBattle = rpgPart1.second,
            daycareState = rpgPart1.third,
            frontierTowerState = rpgPart1.fourth,
            tamerQuests = rpgPart2.first,
            tamerPassState = rpgPart2.second,
            forgedRelics = rpgPart2.third,
            unlockedRuneCodes = rpgPart2.fourth,
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
        val savedLocalCreatures = repository.loadLocalCreatures(currentUserId)
        val isNewGame = savedLocalCreatures.isEmpty() && !repository.hasStartedGame(currentUserId)
        val savedCoins = if (isNewGame) {
            DigimonCatalog.STARTING_DIGI_COINS
        } else {
            repository.loadDigiCoins(currentUserId)
        }
        val savedBerries = repository.loadTammingBerryCount(currentUserId)
        val savedOrbMap = repository.loadLocalAcornOrbInventory(currentUserId).toMutableMap()
        savedOrbMap[AcornOrbType.STANDARD_ACORN_ORB] = maxOf(savedBerries, savedOrbMap[AcornOrbType.STANDARD_ACORN_ORB] ?: 0)
        val savedEmeraldReserve = repository.loadLocalEmeraldReserve(currentUserId)
        val (savedStdParts, savedUltraParts) = repository.loadMonsterPartsCounts(currentUserId)
        val savedFood = repository.loadFoodInventory(currentUserId)
        val (savedDailyDate, savedDailySummary) = repository.loadDailyLoginState(currentUserId)
        _digiCoins.value = savedCoins
        _tammingBerryCount.value = savedOrbMap[AcornOrbType.STANDARD_ACORN_ORB] ?: savedBerries
        _acornOrbInventory.value = savedOrbMap
        _optimisticEmeraldReserve.value = savedEmeraldReserve
        _standardMonsterPartsCount.value = savedStdParts
        _ultraRareMonsterPartsCount.value = savedUltraParts
        _foodInventory.value = savedFood
        _lastClaimedDailyLoginDateKey.value = savedDailyDate
        _lastDailyLoginRewardSummary.value = savedDailySummary
        if (savedLocalCreatures.isNotEmpty()) {
            repository.markGameStarted(currentUserId, true)
            _optimisticCreatures.value = savedLocalCreatures
        }
        loadLongPlayRpgStateFromStorage()

        observeLocalEconomyPersistence()
        scanWildMonster()
        startRealTimeTelemetryTicker()
        if (isGoogleSignedIn) {
            observeCompletedTradesToClaim()
        }
    }

    private fun observeLocalEconomyPersistence() {
        viewModelScope.launch {
            _digiCoins.collect { coins ->
                if (!_isResetting.value) {
                    repository.saveDigiCoins(currentUserId, coins)
                }
            }
        }
        viewModelScope.launch {
            _tammingBerryCount.collect { count ->
                if (!_isResetting.value) {
                    repository.saveTammingBerryCount(currentUserId, count)
                    val map = _acornOrbInventory.value.toMutableMap()
                    if (map[AcornOrbType.STANDARD_ACORN_ORB] != count) {
                        map[AcornOrbType.STANDARD_ACORN_ORB] = count
                        _acornOrbInventory.value = map
                    }
                    repository.saveLocalAcornOrbInventory(currentUserId, _acornOrbInventory.value)
                }
            }
        }
        viewModelScope.launch {
            _acornOrbInventory.collect { orbMap ->
                if (!_isResetting.value) {
                    repository.saveLocalAcornOrbInventory(currentUserId, orbMap)
                }
            }
        }
        viewModelScope.launch {
            _optimisticEmeraldReserve.collect { reserveList ->
                if (!_isResetting.value) {
                    repository.saveLocalEmeraldReserve(currentUserId, reserveList)
                }
            }
        }
        viewModelScope.launch {
            combine(_standardMonsterPartsCount, _ultraRareMonsterPartsCount) { s, u -> s to u }
                .collect { (stdParts, ultraParts) ->
                    if (!_isResetting.value) {
                        repository.saveMonsterPartsCounts(currentUserId, stdParts, ultraParts)
                    }
                }
        }
        viewModelScope.launch {
            _foodInventory.collect { inv ->
                if (!_isResetting.value) {
                    repository.saveFoodInventory(currentUserId, inv)
                }
            }
        }
        viewModelScope.launch {
            combine(_lastClaimedDailyLoginDateKey, _lastDailyLoginRewardSummary) { d, s -> d to s }
                .collect { (dateKey, summary) ->
                    if (!_isResetting.value && dateKey.isNotBlank()) {
                        repository.saveDailyLoginState(currentUserId, dateKey, summary)
                    }
                }
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
        repository.saveLocalCreatures(currentUserId, next)
    }

    private fun removeOptimisticCreature(creatureId: String) {
        _removedCreatureIds.value = _removedCreatureIds.value + creatureId
        val next = _optimisticCreatures.value.filter { it.id != creatureId }
        _optimisticCreatures.value = next
        repository.saveLocalCreatures(currentUserId, next)
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
            _statusBannerMessage.value = "Please select 1 Rookie starter monster to begin your raising journey."
            return
        }

        val requestedChoice = selectedOne.first()
        if (!DigimonCatalog.isRookieStarterAllowed(requestedChoice.speciesId)) {
            _statusBannerMessage.value =
                "Starter Rule: You can ONLY choose a Rookie-stage monster as your starter! Please select a Rookie monster."
            return
        }

        val nowMs = System.currentTimeMillis()
        val nowTimestamp = Timestamp.now()
        val todayKey = todayDateKey()

        val draftedCreatures = selectedOne.mapIndexed { index, choice ->
            val spec = DigimonCatalog.requireRookieStarterSpec(choice.speciesId)
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
        val isNewGameDraft = !repository.hasStartedGame(currentUserId) || _optimisticCreatures.value.isEmpty()

        _isResetting.value = false
        _removedCreatureIds.value = emptySet()
        _optimisticCreatures.value = draftedCreatures
        repository.saveLocalCreatures(currentUserId, draftedCreatures)
        repository.markGameStarted(currentUserId, true)
        _selectedCreatureId.value = lead.id
        if (isNewGameDraft) {
            _digiCoins.value = DigimonCatalog.STARTING_DIGI_COINS
            repository.saveDigiCoins(currentUserId, DigimonCatalog.STARTING_DIGI_COINS)
        }
        _nextDigiCoinTickSec.value = DigimonCatalog.DIGI_COIN_INTERVAL_SEC
        prependOptimisticLog(
            TrainingLogEntry(
                id = "log_draft_$nowMs",
                userId = currentUserId,
                creatureId = lead.id,
                creatureName = lead.nickname,
                activityType = "Digivolution",
                grade = "EVO",
                statDeltaSummary = if (isNewGameDraft) {
                    "New Game Started: $namesSummary • ${DigimonCatalog.STARTING_DIGI_COINS} DigiCoins Initialized"
                } else {
                    "Drafted Starter Monster: $namesSummary • Balance ${_digiCoins.value} DigiCoins Kept"
                },
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
                title = "New Game Starter Synchronized!",
                message = "You start your New Game with ${DigimonCatalog.STARTING_DIGI_COINS} DigiCoins! Raise $namesSummary via Training Drills (10 DigiCoins/drill, +5 Coins every 5m, or 100 DigiCoins for 24h Offline Training).",
                isRead = false,
                createdAt = nowTimestamp,
                updatedAt = nowTimestamp
            )
        )
        _statusBannerMessage.value = if (isNewGameDraft) {
            "New Game started with Rookie $namesSummary and ${DigimonCatalog.STARTING_DIGI_COINS} DigiCoins (+5 every 5m)! Train (10 Coins/drill or 100 Coins for 24h offline) to reach LVL 20 and evolve!"
        } else {
            "Starter Monster $namesSummary synchronized! Current balance: ${_digiCoins.value} DigiCoins (200 DigiCoins awarded on New Game only)."
        }

        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.draftThreeStarters(selectedOne, _trainerName.value, draftedCreatures)
            }
        }
    }

    fun resetAllProgress() {
        _isResetting.value = true
        repository.clearLocalUserProgress(currentUserId)
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
        _standardMonsterPartsCount.value = 0
        _ultraRareMonsterPartsCount.value = 0
        _lastClaimedDailyLoginDateKey.value = ""
        _lastDailyLoginRewardSummary.value = null
        _recentEvolutionUnlocked.value = null
        _latestSparringResult.value = null
        _selectedWorldRegionId.value = LongPlayRpgCatalog.worldRegions.first().regionId
        _turnBasedBattle.value = null
        _daycareState.value = DaycareBreedingState()
        _frontierTowerState.value = FrontierTowerState()
        _tamerQuests.value = LongPlayRpgCatalog.defaultTamerQuests()
        _tamerPassState.value = TamerPassState()
        _forgedRelics.value = emptyMap()
        _unlockedRuneCodes.value = setOf(
            ResonanceRune.SOLAR_CRIT.runeCode,
            ResonanceRune.GALE_SWIFTNESS.runeCode
        )
        alertedHungerLowIds.clear()
        alertedEvolutionReadyIds.clear()
        alertedHealthLowIds.clear()
        offlineCatchUpAppliedIds.clear()
        _statusBannerMessage.value =
            "New Game initialized! You start with ${DigimonCatalog.STARTING_DIGI_COINS} DigiCoins (New Game only). Choose your 1 Rookie Starter Monster below."

        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.resetAllPlayerProgress()
            }
        }
    }

    private fun checkBlockedBy1HpRecovery(
        creature: DigimonCreature,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (creature.isRecoveringFrom1Hp(nowMs)) {
            val rem = creature.formattedRecoveryRemaining(nowMs)
            _statusBannerMessage.value =
                "1 HP RECOVERY LOCKOUT: ${creature.nickname} reached 1 HP in wild battle and must recover for $rem (5 mins total) before you can do anything with ${creature.nickname} again!"
            return true
        }
        return false
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
        if (checkBlockedBy1HpRecovery(offered)) return
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
        if (listing.isNpcListing()) {
            _statusBannerMessage.value =
                "NPC trades are disabled! You can only trade monsters with real-life players."
            return
        }
        if (listing.creatorUserId == currentUserId) {
            _statusBannerMessage.value = "That is your own trade listing! Use Cancel if you want to return it to your squad."
            return
        }
        val myCreature = uiState.value.creatures.firstOrNull { it.id == myCreatureIdToTrade } ?: return
        if (checkBlockedBy1HpRecovery(myCreature)) return
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
            if (creature.id.isBlank()) continue

            // Always check 24-Hour Offline Auto-Training & 5-Min 1 HP Recovery catch-up
            var working = creature
            var changedSpecial = false

            val recEndMs = working.persistedRecoveryEndsAtMs()
            if (recEndMs > 0L && nowMs >= recEndMs) {
                val newForms = working.withUpdatedMetadataInForms(
                    newSkillNodes = working.allUnlockedSkillNodeIds(),
                    newBonusStats = working.persistedNonTrainingBonusStats(),
                    newExpeditionsCount = working.persistedCompletedExpeditions(),
                    newOfflineTrainEndMs = working.persistedOfflineTrainingEndMs(),
                    newOfflineTrainTickMs = working.persistedOfflineTrainingLastTickMs(),
                    newRecoveryEndsMs = 0L
                )
                working = working.copy(
                    health = 100,
                    recoveryEndsAtMs = 0L,
                    unlockedForms = newForms,
                    lastUpdatedMs = nowMs
                )
                changedSpecial = true
                _statusBannerMessage.value =
                    "${working.nickname} finished its 5-minute recovery from 1 HP and is back at 100% HP!"
            }

            val otEndMs = working.persistedOfflineTrainingEndMs()
            val otLastTickMs = working.persistedOfflineTrainingLastTickMs().let {
                if (it > 0L) it else (otEndMs - DigimonCatalog.OFFLINE_TRAINING_24H_DURATION_MS).coerceAtLeast(0L)
            }
            if (otEndMs > 0L) {
                val effectiveNow = minOf(nowMs, otEndMs)
                val elapsedTrainMs = (effectiveNow - otLastTickMs).coerceAtLeast(0L)
                val cycles = (elapsedTrainMs / DigimonCatalog.OFFLINE_TRAINING_CYCLE_MS).toInt()
                val sessionFinished = nowMs >= otEndMs
                if (cycles > 0 || sessionFinished) {
                    val atkGain = cycles * 2
                    val defGain = cycles * 2
                    val spdGain = cycles * 2
                    val wisGain = cycles * 3
                    val syncGain = cycles * 4
                    val bondGain = cycles * 1
                    val nextTickMs = if (sessionFinished) 0L else (otLastTickMs + cycles * DigimonCatalog.OFFLINE_TRAINING_CYCLE_MS)
                    val nextEndMs = if (sessionFinished) 0L else otEndMs
                    val newForms = working.withUpdatedMetadataInForms(
                        newSkillNodes = working.allUnlockedSkillNodeIds(),
                        newBonusStats = working.persistedNonTrainingBonusStats(),
                        newExpeditionsCount = working.persistedCompletedExpeditions(),
                        newOfflineTrainEndMs = nextEndMs,
                        newOfflineTrainTickMs = nextTickMs,
                        newOfflineTrainPausedRemMs = 0L,
                        newOfflineTrainPausedCycleMs = 0L,
                        newRecoveryEndsMs = working.persistedRecoveryEndsAtMs()
                    )
                    working = working.copy(
                        attack = working.attack + atkGain,
                        defense = working.defense + defGain,
                        speed = working.speed + spdGain,
                        wisdom = working.wisdom + wisGain,
                        evolutionProgress = (working.evolutionProgress + syncGain).coerceIn(0, 100),
                        bondLevel = (working.bondLevel + bondGain).coerceIn(0, 100),
                        offlineTrainingEndMs = nextEndMs,
                        offlineTrainingLastTickMs = nextTickMs,
                        offlineTrainingPausedRemainingMs = 0L,
                        offlineTrainingPausedCycleElapsedMs = 0L,
                        unlockedForms = newForms,
                        lastUpdatedMs = nowMs
                    )
                    changedSpecial = true
                    if (cycles > 0) {
                        val summary =
                            "24h Offline Auto-Training (${cycles}x cycles while closed/active): +$atkGain ATK, +$defGain DEF, +$spdGain SPD, +$wisGain WIS (+$cycles LVL • Now LVL ${working.currentLevel()}/100)"
                        val logEntry = prependOptimisticLog(
                            TrainingLogEntry(
                                userId = currentUserId,
                                creatureId = working.id,
                                creatureName = working.nickname,
                                activityType = "Power Drill",
                                grade = "S",
                                statDeltaSummary = summary,
                                xpGained = cycles * 65,
                                dateKey = todayDateKey()
                            )
                        )
                        _statusBannerMessage.value = "${working.nickname} gained +$cycles Levels from 24h Offline Auto-Training! ($summary)"
                        if (isGoogleSignedIn) {
                            repository.addTrainingLog(logEntry)
                        }
                    } else if (sessionFinished) {
                        _statusBannerMessage.value =
                            "${working.nickname}'s 24-Hour Offline Auto-Training session has completed (LVL ${working.currentLevel()}/100)!"
                    }
                }
            }

            if (!offlineCatchUpAppliedIds.add(creature.id)) {
                if (changedSpecial) {
                    upsertOptimisticCreature(working)
                    if (isGoogleSignedIn) {
                        repository.updateCreature(working)
                    }
                }
                continue
            }

            val elapsedMinutes = ((nowMs - working.lastUpdatedMs).coerceAtLeast(0L)) / 60_000L
            if (elapsedMinutes >= 5L) {
                val cappedMins = elapsedMinutes.coerceAtMost(720L).toInt()
                if (!coinCatchUpApplied) {
                    val intervals = (cappedMins / 5).coerceAtLeast(1)
                    val offlineCoinsEarned = intervals * DigimonCatalog.DIGI_COINS_PER_INTERVAL
                    _digiCoins.value = _digiCoins.value + offlineCoinsEarned
                    coinCatchUpApplied = true
                }
                // Monsters ONLY gain Stamina when in Cryo-Rest; auto-wake when reaching 100% Stamina
                val staminaRecovered = if (working.isResting) (cappedMins * 3) else 0
                val expSpec = if (working.isOnExpedition()) DigimonCatalog.getExpedition(working.activeExpeditionId) else null
                val expStaminaDrain = if (expSpec != null && !working.isResting) (cappedMins / 4) * expSpec.staminaDrainPerCycle else 0
                val hungerDecayBase = if (working.isResting) (cappedMins / 6) else (cappedMins / 3)
                val expHungerDrain = if (expSpec != null) (cappedMins / 4) * expSpec.hungerDrainPerCycle else 0
                val newHunger = (working.hunger - hungerDecayBase - expHungerDrain).coerceIn(10, 100)
                val newStamina = (working.stamina + staminaRecovered - expStaminaDrain).coerceIn(0, 100)
                val stillResting = if (working.isResting && newStamina >= 100) false else working.isResting

                val updated = working.copy(
                    stamina = newStamina,
                    hunger = newHunger,
                    isResting = stillResting,
                    lastUpdatedMs = nowMs
                )
                upsertOptimisticCreature(updated)
                if (isGoogleSignedIn) {
                    repository.updateCreature(updated)
                }
            } else if (changedSpecial) {
                upsertOptimisticCreature(working)
                if (isGoogleSignedIn) {
                    repository.updateCreature(working)
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
                        active.isRecoveringFrom1Hp(nowMs) -> 52
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

                    if (hunCountdown == 25 && !newIsResting && !creature.isRecoveringFrom1Hp(nowMs)) {
                        val baseDrain = 2
                        val expDrain = if (expSpec != null && nowMs < newExpEndMs) expSpec.hungerDrainPerCycle else 0
                        newHunger = (newHunger - baseDrain - expDrain).coerceIn(0, 100)
                        val healthDelta = if (newHunger < 15) -2 else if (newHunger >= 75 && newHealth < 100) 1 else 0
                        newHealth = (newHealth + healthDelta).coerceIn(1, 100)
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

    fun selectAcornOrbType(orbType: AcornOrbType) {
        _selectedAcornOrbType.value = orbType
    }

    private fun syncOrbTypeToFirestore(orbType: AcornOrbType) {
        if (!isGoogleSignedIn) return
        val qty = _acornOrbInventory.value[orbType]
            ?: if (orbType == AcornOrbType.STANDARD_ACORN_ORB) _tammingBerryCount.value else 0
        val gilded = _standardMonsterPartsCount.value
        val relic = _ultraRareMonsterPartsCount.value
        viewModelScope.launch {
            repository.upsertAcornOrbInventory(
                orbType = orbType,
                quantity = qty,
                gildedAcornCount = gilded,
                relicShardCount = relic
            )
        }
    }

    fun buyOrCraftAcornOrb(orbType: AcornOrbType, useForagedMaterials: Boolean = false, quantity: Int = 1) {
        val qty = quantity.coerceAtLeast(1)
        if (useForagedMaterials) {
            val reqGilded = orbType.craftGildedAcornsCost * qty
            val reqRelic = orbType.craftRelicShardsCost * qty
            val ownedGilded = _standardMonsterPartsCount.value
            val ownedRelic = _ultraRareMonsterPartsCount.value
            if (ownedGilded < reqGilded || ownedRelic < reqRelic) {
                _statusBannerMessage.value =
                    "Need ${reqGilded}x Gilded Acorn${if (reqRelic > 0) " + ${reqRelic}x Relic Shard" else ""} to craft ${qty}x ${orbType.displayName} (You have $ownedGilded Gilded Acorns & $ownedRelic Relic Shards)!"
                return
            }
            _standardMonsterPartsCount.value = (ownedGilded - reqGilded).coerceAtLeast(0)
            _ultraRareMonsterPartsCount.value = (ownedRelic - reqRelic).coerceAtLeast(0)
        } else {
            val totalCost = orbType.costCoins * qty
            val currentCoins = _digiCoins.value
            if (currentCoins < totalCost) {
                _statusBannerMessage.value =
                    "Not enough Crest Coins for ${qty}x ${orbType.displayName}! Costs $totalCost Crest Coins (you have $currentCoins)."
                return
            }
            _digiCoins.value = (currentCoins - totalCost).coerceAtLeast(0)
        }

        val map = _acornOrbInventory.value.toMutableMap()
        val prev = map[orbType] ?: if (orbType == AcornOrbType.STANDARD_ACORN_ORB) _tammingBerryCount.value else 0
        val newTotal = prev + qty
        map[orbType] = newTotal
        _acornOrbInventory.value = map
        if (orbType == AcornOrbType.STANDARD_ACORN_ORB) {
            _tammingBerryCount.value = newTotal
        }
        _selectedAcornOrbType.value = orbType
        syncOrbTypeToFirestore(orbType)
        _statusBannerMessage.value = if (useForagedMaterials) {
            "Crafted +${qty}x ${orbType.displayName} at Master Orin's Forge! Now holding $newTotal in your Acorn Orb Pouch (Offline-Cached)."
        } else {
            "Acquired +${qty}x ${orbType.displayName}! Now holding $newTotal in your Acorn Orb Pouch (Offline-Cached)."
        }
    }

    fun buyTammingBerriesInStore(quantity: Int = 1) {
        val qty = quantity.coerceAtLeast(1)
        val totalCost = DigimonCatalog.TAMMING_BERRY_COIN_COST * qty
        val currentCoins = _digiCoins.value
        if (currentCoins < totalCost) {
            _statusBannerMessage.value =
                "Not enough Crest Coins for $qty Acorn Orb! Costs $totalCost Crest Coins (${DigimonCatalog.TAMMING_BERRY_COIN_COST} Coins/orb, you have $currentCoins)."
            return
        }
        _digiCoins.value = (currentCoins - totalCost).coerceAtLeast(0)
        val newTotal = _tammingBerryCount.value + qty
        _tammingBerryCount.value = newTotal
        val map = _acornOrbInventory.value.toMutableMap()
        map[AcornOrbType.STANDARD_ACORN_ORB] = newTotal
        _acornOrbInventory.value = map
        syncOrbTypeToFirestore(AcornOrbType.STANDARD_ACORN_ORB)
        _statusBannerMessage.value =
            "Purchased +$qty Acorn Orb (-$totalCost Crest Coins)! You now have $newTotal Acorn Orbs in your pouch for Wild Taming."
    }

    fun sellStandardMonsterParts(quantity: Int = 1) {
        val owned = _standardMonsterPartsCount.value
        if (owned <= 0) {
            _statusBannerMessage.value =
                "You have 0 Standard Monster Parts to sell! Battle wild monsters you don't want to tame in the Wild tab to collect Monster Parts."
            return
        }
        val sellQty = quantity.coerceIn(1, owned)
        val coinsEarned = sellQty * DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS
        _standardMonsterPartsCount.value = (owned - sellQty).coerceAtLeast(0)
        _digiCoins.value = _digiCoins.value + coinsEarned
        _statusBannerMessage.value =
            "Sold ${sellQty}x Standard Monster ${if (sellQty == 1) "Part" else "Parts"} for +$coinsEarned DigiCoins (${DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS} DigiCoins each)!"
    }

    fun sellUltraRareMonsterParts(quantity: Int = 1) {
        val owned = _ultraRareMonsterPartsCount.value
        if (owned <= 0) {
            _statusBannerMessage.value =
                "You have 0 Ultra Rare Monster Parts to sell! Battle Ultra Rare wild monsters in the Wild tab to collect Ultra Rare Monster Parts (${DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS} DigiCoins each)."
            return
        }
        val sellQty = quantity.coerceIn(1, owned)
        val coinsEarned = sellQty * DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS
        _ultraRareMonsterPartsCount.value = (owned - sellQty).coerceAtLeast(0)
        _digiCoins.value = _digiCoins.value + coinsEarned
        _statusBannerMessage.value =
            "Sold ${sellQty}x Ultra Rare Monster ${if (sellQty == 1) "Part" else "Parts"} for +$coinsEarned DigiCoins (${DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS} DigiCoins each)!"
    }

    fun sellAllMonsterParts() {
        val stdOwned = _standardMonsterPartsCount.value
        val ultraOwned = _ultraRareMonsterPartsCount.value
        if (stdOwned <= 0 && ultraOwned <= 0) {
            _statusBannerMessage.value =
                "You have 0 Monster Parts in your salvage bag! Battle wild monsters in the Wild tab to earn Standard Parts (2 DigiCoins) & Ultra Rare Parts (5 DigiCoins)."
            return
        }
        val stdCoins = stdOwned * DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS
        val ultraCoins = ultraOwned * DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS
        val totalCoins = stdCoins + ultraCoins
        _standardMonsterPartsCount.value = 0
        _ultraRareMonsterPartsCount.value = 0
        _digiCoins.value = _digiCoins.value + totalCoins
        _statusBannerMessage.value =
            "Sold ALL Monster Parts (${stdOwned}x Standard @ 2 Coins + ${ultraOwned}x Ultra Rare @ 5 Coins) for +$totalCoins DigiCoins!"
    }

    fun start24hOfflineTraining() {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return

        if (active.isOfflineTrainingPaused()) {
            restartOfflineTraining()
            return
        }

        if (active.isInOfflineTraining(nowMs)) {
            _statusBannerMessage.value =
                "${active.nickname} is already in 24-Hour Offline Auto-Training (${active.formattedOfflineTrainingRemaining(nowMs)} remaining)! You can pause the offline training timer anytime."
            return
        }

        val currentCoins = _digiCoins.value
        val cost = DigimonCatalog.OFFLINE_TRAINING_24H_COIN_COST
        if (currentCoins < cost) {
            _statusBannerMessage.value =
                "Not enough DigiCoins for 24-Hour Offline Auto-Training! Costs $cost DigiCoins (you have $currentCoins). Earn +5 Coins every 5m or sell Monster Parts from Wild Battles!"
            return
        }

        val remainingCoins = (currentCoins - cost).coerceAtLeast(0)
        _digiCoins.value = remainingCoins

        val endMs = nowMs + DigimonCatalog.OFFLINE_TRAINING_24H_DURATION_MS
        // Grant an immediate kickoff training cycle (+2 ATK, +2 DEF, +2 SPD, +3 WIS = +9 trained stats = +1 Level) + 24 hours of background/offline training
        val newForms = active.withUpdatedMetadataInForms(
            newSkillNodes = active.allUnlockedSkillNodeIds(),
            newBonusStats = active.persistedNonTrainingBonusStats(),
            newExpeditionsCount = active.persistedCompletedExpeditions(),
            newOfflineTrainEndMs = endMs,
            newOfflineTrainTickMs = nowMs,
            newOfflineTrainPausedRemMs = 0L,
            newOfflineTrainPausedCycleMs = 0L,
            newRecoveryEndsMs = active.persistedRecoveryEndsAtMs()
        )
        val updated = active.copy(
            attack = active.attack + 2,
            defense = active.defense + 2,
            speed = active.speed + 2,
            wisdom = active.wisdom + 3,
            evolutionProgress = (active.evolutionProgress + 6).coerceIn(0, 100),
            bondLevel = (active.bondLevel + 3).coerceIn(0, 100),
            offlineTrainingEndMs = endMs,
            offlineTrainingLastTickMs = nowMs,
            offlineTrainingPausedRemainingMs = 0L,
            offlineTrainingPausedCycleElapsedMs = 0L,
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )

        upsertOptimisticCreature(updated)
        val summary =
            "Activated 24-Hour Offline Auto-Training (-$cost DigiCoins, $remainingCoins left): Normal training is locked while in offline training, and you can pause/restart the 24h timer anytime! (+1 Kickoff LVL • LVL ${updated.currentLevel()}/100)"
        val logEntry = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = active.id,
                creatureName = active.nickname,
                activityType = "Power Drill",
                grade = "S",
                statDeltaSummary = summary,
                xpGained = 150,
                dateKey = todayDateKey()
            )
        )
        _statusBannerMessage.value = "★ $summary"

        viewModelScope.launch {
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    fun pauseOfflineTraining() {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (!active.isInOfflineTraining(nowMs)) {
            if (active.isOfflineTrainingPaused()) {
                _statusBannerMessage.value =
                    "${active.nickname}'s Offline Auto-Training is already paused (${active.formattedOfflineTrainingRemaining(nowMs)} remaining). Tap RESTART OFFLINE TRAINING to resume the timer!"
            }
            return
        }

        val otEndMs = active.persistedOfflineTrainingEndMs()
        val otLastTickMs = active.persistedOfflineTrainingLastTickMs().let {
            if (it > 0L) it else (otEndMs - DigimonCatalog.OFFLINE_TRAINING_24H_DURATION_MS).coerceAtLeast(0L)
        }
        val effectiveNow = minOf(nowMs, otEndMs)
        val elapsedTrainMs = (effectiveNow - otLastTickMs).coerceAtLeast(0L)
        val completedCycles = (elapsedTrainMs / DigimonCatalog.OFFLINE_TRAINING_CYCLE_MS).toInt()
        val cycleElapsedRemainderMs = elapsedTrainMs % DigimonCatalog.OFFLINE_TRAINING_CYCLE_MS
        val remainingMs = (otEndMs - nowMs).coerceAtLeast(1000L)

        val atkGain = completedCycles * 2
        val defGain = completedCycles * 2
        val spdGain = completedCycles * 2
        val wisGain = completedCycles * 3
        val syncGain = completedCycles * 4
        val bondGain = completedCycles * 1

        val newForms = active.withUpdatedMetadataInForms(
            newSkillNodes = active.allUnlockedSkillNodeIds(),
            newBonusStats = active.persistedNonTrainingBonusStats(),
            newExpeditionsCount = active.persistedCompletedExpeditions(),
            newOfflineTrainEndMs = 0L,
            newOfflineTrainTickMs = 0L,
            newOfflineTrainPausedRemMs = remainingMs,
            newOfflineTrainPausedCycleMs = cycleElapsedRemainderMs,
            newRecoveryEndsMs = active.persistedRecoveryEndsAtMs()
        )
        val updated = active.copy(
            attack = active.attack + atkGain,
            defense = active.defense + defGain,
            speed = active.speed + spdGain,
            wisdom = active.wisdom + wisGain,
            evolutionProgress = (active.evolutionProgress + syncGain).coerceIn(0, 100),
            bondLevel = (active.bondLevel + bondGain).coerceIn(0, 100),
            offlineTrainingEndMs = 0L,
            offlineTrainingLastTickMs = 0L,
            offlineTrainingPausedRemainingMs = remainingMs,
            offlineTrainingPausedCycleElapsedMs = cycleElapsedRemainderMs,
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )

        upsertOptimisticCreature(updated)
        _statusBannerMessage.value =
            "⏸ Offline Auto-Training PAUSED for ${active.nickname}! Timer is frozen at ${updated.formattedOfflineTrainingRemaining(nowMs)} — you can now train manually (10 Coins/Drill) or tap Restart Offline Training anytime."

        viewModelScope.launch {
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
            }
        }
    }

    fun restartOfflineTraining() {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return
        if (!active.isOfflineTrainingPaused()) {
            return
        }

        val pausedRemMs = active.persistedOfflineTrainingPausedRemainingMs().coerceAtLeast(1000L)
        val pausedCycleMs = active.persistedOfflineTrainingPausedCycleElapsedMs().coerceAtLeast(0L)
        val newEndMs = nowMs + pausedRemMs
        val newLastTickMs = (nowMs - pausedCycleMs).coerceAtLeast(0L)

        val newForms = active.withUpdatedMetadataInForms(
            newSkillNodes = active.allUnlockedSkillNodeIds(),
            newBonusStats = active.persistedNonTrainingBonusStats(),
            newExpeditionsCount = active.persistedCompletedExpeditions(),
            newOfflineTrainEndMs = newEndMs,
            newOfflineTrainTickMs = newLastTickMs,
            newOfflineTrainPausedRemMs = 0L,
            newOfflineTrainPausedCycleMs = 0L,
            newRecoveryEndsMs = active.persistedRecoveryEndsAtMs()
        )
        val updated = active.copy(
            offlineTrainingEndMs = newEndMs,
            offlineTrainingLastTickMs = newLastTickMs,
            offlineTrainingPausedRemainingMs = 0L,
            offlineTrainingPausedCycleElapsedMs = 0L,
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )

        upsertOptimisticCreature(updated)
        _statusBannerMessage.value =
            "▶ Offline Auto-Training RESTARTED for ${active.nickname}! Countdown timer resumed from ${updated.formattedOfflineTrainingRemaining(nowMs)} remaining."

        viewModelScope.launch {
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
            }
        }
    }

    fun togglePauseOfflineTraining() {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        when {
            active.isInOfflineTraining(nowMs) -> pauseOfflineTraining()
            active.isOfflineTrainingPaused() -> restartOfflineTraining()
            else -> start24hOfflineTraining()
        }
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
        val safeCreatureId = lead?.id?.takeIf { it.isNotBlank() } ?: "daily_login"
        val safeCreatureName = lead?.nickname?.takeIf { it.isNotBlank() } ?: "Tamer"
        val logEntry = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = safeCreatureId,
                creatureName = safeCreatureName,
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
                creatureId = safeCreatureId,
                creatureName = safeCreatureName,
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
        if (checkBlockedBy1HpRecovery(active)) return
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
        if (checkBlockedBy1HpRecovery(active)) return
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
        if (checkBlockedBy1HpRecovery(active)) return
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
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return
        if (active.isInOfflineTraining(nowMs)) {
            _statusBannerMessage.value =
                "Once in active Offline Auto-Training (${active.formattedOfflineTrainingRemaining(nowMs)} remaining), ${active.nickname} cannot train normally! Pause Offline Training first to train manually."
            return
        }
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

            val newForms = active.withUpdatedMetadataInForms(
                newSkillNodes = active.allUnlockedSkillNodeIds(),
                newBonusStats = active.persistedNonTrainingBonusStats(),
                newExpeditionsCount = active.persistedCompletedExpeditions(),
                newOfflineTrainEndMs = active.persistedOfflineTrainingEndMs(),
                newOfflineTrainTickMs = active.persistedOfflineTrainingLastTickMs(),
                newOfflineTrainPausedRemMs = active.persistedOfflineTrainingPausedRemainingMs(),
                newOfflineTrainPausedCycleMs = active.persistedOfflineTrainingPausedCycleElapsedMs(),
                newRecoveryEndsMs = active.persistedRecoveryEndsAtMs()
            )

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
                offlineTrainingPausedRemainingMs = active.persistedOfflineTrainingPausedRemainingMs(),
                offlineTrainingPausedCycleElapsedMs = active.persistedOfflineTrainingPausedCycleElapsedMs(),
                unlockedForms = newForms,
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
            recordQuestProgress(TamerQuestType.WIN_BATTLES, 1)
            advanceDaycareIncubation(12)
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
        if (checkBlockedBy1HpRecovery(active)) return
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

    fun changeCompanionNature(nature: com.example.data.model.CompanionNature) {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return
        val newForms = active.withUpdatedMetadataInForms(
            newNatureCode = nature.code
        )
        val updated = active.copy(
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updated)
        _statusBannerMessage.value =
            "🌿 Attuned ${active.nickname}'s Nature to ${nature.displayName.uppercase()} (${nature.effectSummary})! Effective CP: ${updated.combatPower()}."
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.updateCreature(updated)
                syncLeaderboardWithSquad()
            }
        }
    }

    fun equipHeldRelic(relic: com.example.data.model.HeldRelicItem) {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return
        val newForms = active.withUpdatedMetadataInForms(
            newRelicCode = relic.code
        )
        val updated = active.copy(
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updated)
        _statusBannerMessage.value =
            "💎 Equipped ${relic.displayName} on ${active.nickname} (${relic.effectSummary})!"
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.updateCreature(updated)
                syncLeaderboardWithSquad()
            }
        }
    }

    fun challengeSanctumLeader(leader: com.example.data.model.SanctumLeaderSpec) {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return

        val alreadyEarned = active.hasSanctumBadge(leader.badgeNumber)
        val elemMult = com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
            attackerAttr = active.attribute,
            defenderAttr = leader.elementAttribute
        )
        val elemBadge = com.example.data.model.ElementalMatchupEngine.effectivenessBadge(elemMult)
        val playerScore = (active.combatPower() * elemMult).toInt() + (active.currentLevel() * 14) + Random.nextInt(20, 95)
        val leaderScore = (leader.recommendedLevel * 24) + 120
        val wonBattle = playerScore >= leaderScore || active.currentLevel() >= leader.recommendedLevel

        val passiveHeal = active.heldRelic().passiveHealBonus
        val rawHpLoss = if (wonBattle) {
            Random.nextInt(10, 22)
        } else {
            Random.nextInt(28, 48)
        }
        val netHp = (active.health - rawHpLoss + passiveHeal).coerceIn(1, 100)
        val hit1HpLockout = netHp <= 1
        val recoveryEndMs = if (hit1HpLockout) {
            nowMs + DigimonCatalog.BATTLE_RECOVERY_5MIN_DURATION_MS
        } else {
            active.persistedRecoveryEndsAtMs()
        }

        val bit = 1 shl (leader.badgeNumber - 1)
        val newBadgesMask = if (wonBattle) {
            active.earnedSanctumBadgesMask() or bit
        } else {
            active.earnedSanctumBadgesMask()
        }
        val coinsEarned = when {
            wonBattle && !alreadyEarned -> leader.rewardCoins
            wonBattle -> (leader.rewardCoins / 3).coerceAtLeast(8)
            else -> 0
        }
        if (coinsEarned > 0) {
            _digiCoins.value = _digiCoins.value + coinsEarned
        }

        val newForms = active.withUpdatedMetadataInForms(
            newRecoveryEndsMs = recoveryEndMs,
            newBadgesMask = newBadgesMask
        )
        val updated = active.copy(
            health = netHp,
            stamina = (active.stamina - 8).coerceIn(0, 100),
            bondLevel = (active.bondLevel + if (wonBattle) 3 else 1).coerceIn(0, 100),
            isResting = false,
            recoveryEndsAtMs = recoveryEndMs,
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updated)

        val summary = if (wonBattle) {
            val badgeNote = if (!alreadyEarned) {
                "🏅 EARNED ${leader.badgeName.uppercase()} (Badge #${leader.badgeNumber}/16) & +$coinsEarned Crest Coins!"
            } else {
                "Defended Sanctum Title vs ${leader.leaderName} (+$coinsEarned Rematch Crest Coins)!"
            }
            "SANCTUM VICTORY vs Leader ${leader.leaderName}'s ${leader.aceSpeciesName} ($elemBadge)! $badgeNote"
        } else {
            "Sanctum Leader ${leader.leaderName}'s ${leader.aceSpeciesName} (Rec. LVL ${leader.recommendedLevel}) held the line ($elemBadge)! Train ${active.nickname} or use a Super-Effective element and challenge again."
        }

        val logEntry = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = active.id,
                creatureName = active.nickname,
                activityType = "Arena Sparring",
                grade = if (wonBattle) "S" else "B",
                statDeltaSummary = summary.take(190),
                xpGained = if (wonBattle) 140 else 45,
                dateKey = todayDateKey()
            )
        )
        _statusBannerMessage.value = summary
        if (wonBattle) {
            recordQuestProgress(TamerQuestType.WIN_BATTLES, 1)
            recordQuestProgress(TamerQuestType.DEFEAT_BOSS_OR_LEADER, 1)
        }
        advanceDaycareIncubation(10)

        viewModelScope.launch {
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(logEntry)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    // Egg hatching is removed; all monsters beyond the 1 starter must be tamed in the wild using Tamming Berries (2 DigiCoins each)
    fun scanWildMonster(preferredSpeciesId: String? = null) {
        var radarEvadedNotice: String? = null
        val chosen = if (!preferredSpeciesId.isNullOrBlank()) {
            val targetSpec = DigimonCatalog.getSpec(preferredSpeciesId)
            val lockChance = targetSpec.wildRarityTier().radarLockChancePercent
            if (!targetSpec.isRareLevel100Spec && Random.nextInt(100) < lockChance) {
                targetSpec
            } else {
                radarEvadedNotice =
                    "${targetSpec.speciesName} (${targetSpec.wildRarityTier().displayName} • $lockChance% Radar Lock) evaded the scanner! Encountered a different wild signal instead."
                DigimonCatalog.rollWeightedWildMonster(Random)
            }
        } else {
            DigimonCatalog.rollWeightedWildMonster(Random)
        }

        val rarity = chosen.wildRarityTier()
        val rawBaseRate = Random.nextInt(rarity.minBaseTameRate, rarity.maxBaseTameRate + 1)
        // Within each rarity tier, monsters with higher total stats are even harder to capture
        val statDifficultyPenalty = when (rarity) {
            com.example.data.model.WildRarityTier.COMMON -> ((chosen.totalBaseStats() - 170).coerceAtLeast(0) / 10).coerceAtMost(6)
            com.example.data.model.WildRarityTier.UNCOMMON -> ((chosen.totalBaseStats() - 250).coerceAtLeast(0) / 14).coerceAtMost(5)
            com.example.data.model.WildRarityTier.RARE -> ((chosen.totalBaseStats() - 160).coerceAtLeast(0) / 16).coerceAtMost(4)
            com.example.data.model.WildRarityTier.ULTRA_RARE -> ((chosen.totalBaseStats() - 430).coerceAtLeast(0) / 20).coerceAtMost(3)
            com.example.data.model.WildRarityTier.LEGENDARY_APEX -> 1
        }
        val baseRate = (rawBaseRate - statDifficultyPenalty).coerceIn(rarity.minBaseTameRate, rarity.maxBaseTameRate)

        val wildLvl = when (chosen.stage) {
            "Rookie" -> Random.nextInt(1, 12)
            "Champion" -> Random.nextInt(20, 35)
            "Ultimate" -> Random.nextInt(40, 58)
            else -> Random.nextInt(65, 85)
        }
        val activeCharm = uiState.value.activeCreature?.heldRelic() == com.example.data.model.HeldRelicItem.LUCKY_CAPTURE_CHARM
        val frontierShinyBoost = if (_frontierTowerState.value.shinyCharmUnlocked) 15 else 0
        val runeShinyBoost = uiState.value.activeCreature?.let {
            uiState.value.getForgedRelicState(it.heldRelic()).totalCaptureAndShinyBonusPct() / 2
        } ?: 0
        val shinyOddsPercent = ((if (activeCharm) 10 else 5) + frontierShinyBoost + runeShinyBoost).coerceIn(5, 45)
        val isChromatic = Random.nextInt(100) < shinyOddsPercent
        val wildNature = com.example.data.model.CompanionNature.entries[Random.nextInt(com.example.data.model.CompanionNature.entries.size)]

        val isUltraRare = isChromatic ||
            rarity == com.example.data.model.WildRarityTier.LEGENDARY_APEX ||
            rarity == com.example.data.model.WildRarityTier.ULTRA_RARE ||
            rarity == com.example.data.model.WildRarityTier.RARE ||
            Random.nextInt(100) < 15
        val partLabel = if (isUltraRare) {
            "Relic Shard (${DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS} Crest Coins)"
        } else {
            "Gilded Acorn (${DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS} Crest Coins)"
        }
        val shinyPrefix = if (isChromatic) "✨ STARLIGHT CHROMATIC " else ""
        val rarityPrefix = when (rarity) {
            com.example.data.model.WildRarityTier.LEGENDARY_APEX -> "★★ LEGENDARY SOVEREIGN "
            com.example.data.model.WildRarityTier.ULTRA_RARE -> "★ ULTRA RARE "
            com.example.data.model.WildRarityTier.RARE -> "◆ RARE "
            com.example.data.model.WildRarityTier.UNCOMMON -> "◇ UNCOMMON "
            com.example.data.model.WildRarityTier.COMMON -> "Wild "
        }
        val detailMsg = if (radarEvadedNotice != null) {
            "$radarEvadedNotice • ${rarity.displayName} (${rarity.spawnChancePercent}% Spawn) • Base Capture: $baseRate% • Nature: ${wildNature.displayName}"
        } else {
            "Wild HP: 100% • Nature: ${wildNature.displayName} (${wildNature.effectSummary}) • Use Mercy Slash (1% HP = +35% Capture) & Status Wave (+12–25% Capture) before throwing Acorn Orbs!"
        }
        _wildEncounter.value = WildEncounterState(
            speciesId = chosen.speciesId,
            speciesName = chosen.speciesName,
            stage = chosen.stage,
            attribute = chosen.attribute,
            wildLevel = wildLvl,
            baseSuccessRatePercent = baseRate,
            rarityTier = rarity,
            tameProgress = 0,
            berriesUsed = 0,
            isUltraRareWild = isUltraRare,
            droppedPartName = partLabel,
            lastRollOutcomeTitle = "$shinyPrefix$rarityPrefix${chosen.speciesName} (${chosen.stage}) Appeared!",
            lastRollOutcomeDetail = detailMsg,
            wildHpPercent = 100,
            statusEffect = com.example.data.model.WildStatusEffect.NONE,
            isChromaticShiny = isChromatic,
            wildNatureName = wildNature.displayName,
            lastEffectivenessLabel = ""
        )
        syncTurnBasedBattleFromWild()
        if (radarEvadedNotice != null) {
            _statusBannerMessage.value = radarEvadedNotice
        }
    }

    /**
     * Classic 4-Move Tactical Turn-Based Combat against the current Wild Companion.
     * - Move 0 (ELEMENTAL_STRIKE): Full STAB + Elemental Effectiveness (2.0x / 1.0x / 0.5x). Knocking Wild HP to 0% loots Gilded Acorns / Relic Shards!
     * - Move 1 (MERCY_SUBDUE - Mercy Slash): Controlled False-Swipe strike that NEVER drops Wild HP below 1% (maximizes +35% Red-HP Capture Bonus!).
     * - Move 2 (STATUS_WAVE): Inflicts SLEEP (+25% Capture), PARALYZED (+18% Capture), or BURNED (+12% Capture) and deals light chip damage (clamped >= 1%).
     * - Move 3 (CREST_SURGE): Restores +18 HP to active companion, boosts Bond, and deals light Aether chip (clamped >= 1%).
     */
    fun executeWildTacticalMove(moveIndex: Int) {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return

        val currentEncounter = _wildEncounter.value ?: run {
            scanWildMonster()
            return
        }
        if (currentEncounter.isTamed) {
            scanWildMonster()
            return
        }

        val moves = DigimonCatalog.getFourBattleMoves(active)
        val move = moves.getOrElse(moveIndex.coerceIn(0, moves.lastIndex)) { moves.first() }
        val elemMult = com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
            attackerAttr = move.elementAttribute,
            defenderAttr = currentEncounter.attribute
        )
        val elemBadge = com.example.data.model.ElementalMatchupEngine.effectivenessBadge(elemMult)
        val stabMult = if (move.elementAttribute.equals(active.attribute, ignoreCase = true)) 1.25f else 1.0f

        val rawDamagePercent: Int
        var newStatus = currentEncounter.statusEffect
        var selfHealAmount = active.heldRelic().passiveHealBonus

        when (move.category) {
            com.example.data.model.BattleMoveCategory.ELEMENTAL_STRIKE -> {
                val baseStrike = Random.nextInt(32, 48)
                rawDamagePercent = (baseStrike * elemMult * stabMult).toInt().coerceIn(14, 95)
            }
            com.example.data.model.BattleMoveCategory.MERCY_SUBDUE -> {
                val subdueHit = Random.nextInt(34, 52)
                rawDamagePercent = subdueHit
            }
            com.example.data.model.BattleMoveCategory.STATUS_WAVE -> {
                rawDamagePercent = Random.nextInt(8, 16)
                newStatus = when {
                    move.moveName.contains("Sleep", ignoreCase = true) -> com.example.data.model.WildStatusEffect.SLEEP
                    move.moveName.contains("Paralyze", ignoreCase = true) -> com.example.data.model.WildStatusEffect.PARALYZED
                    else -> com.example.data.model.WildStatusEffect.BURNED
                }
            }
            com.example.data.model.BattleMoveCategory.CREST_SURGE -> {
                rawDamagePercent = Random.nextInt(14, 24)
                selfHealAmount += 18
            }
        }

        val newWildHp = when (move.category) {
            com.example.data.model.BattleMoveCategory.ELEMENTAL_STRIKE ->
                (currentEncounter.wildHpPercent - rawDamagePercent).coerceAtLeast(0)
            else ->
                // Mercy Slash, Status Wave, and Crest Surge NEVER knock out a wild companion below 1% HP!
                (currentEncounter.wildHpPercent - rawDamagePercent).coerceAtLeast(1)
        }

        if (newWildHp <= 0) {
            // Wild companion was knocked out by Signature Elemental Strike -> Loot Gilded Acorns / Relic Shards!
            battleWildMonster()
            return
        }

        // Wild companion counter-attack (Asleep or Paralyzed wild companions have a high chance to skip counter-attack!)
        val statusStunSkipped = when (newStatus) {
            com.example.data.model.WildStatusEffect.SLEEP -> Random.nextInt(100) < 75
            com.example.data.model.WildStatusEffect.PARALYZED -> Random.nextInt(100) < 50
            else -> false
        }
        val counterDamage = if (statusStunSkipped) {
            0
        } else {
            val baseCounter = when (currentEncounter.stage) {
                "Mega" -> Random.nextInt(16, 28)
                "Ultimate" -> Random.nextInt(12, 22)
                "Champion" -> Random.nextInt(9, 17)
                else -> Random.nextInt(6, 13)
            }
            val burnReduction = if (newStatus == com.example.data.model.WildStatusEffect.BURNED) 0.65f else 1.0f
            (baseCounter * burnReduction).toInt().coerceAtLeast(3)
        }

        val updatedHealth = (active.health - counterDamage + selfHealAmount).coerceIn(1, 100)
        val hit1HpLockout = updatedHealth <= 1
        val recoveryEndMs = if (hit1HpLockout) {
            nowMs + DigimonCatalog.BATTLE_RECOVERY_5MIN_DURATION_MS
        } else {
            active.persistedRecoveryEndsAtMs()
        }

        val newForms = active.withUpdatedMetadataInForms(
            newRecoveryEndsMs = recoveryEndMs
        )
        val updatedPartner = active.copy(
            health = updatedHealth,
            stamina = (active.stamina - 4).coerceIn(0, 100),
            bondLevel = (active.bondLevel + 1).coerceIn(0, 100),
            isResting = false,
            recoveryEndsAtMs = recoveryEndMs,
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updatedPartner)

        val updatedEncounter = currentEncounter.copy(
            wildHpPercent = newWildHp,
            statusEffect = newStatus,
            lastEffectivenessLabel = if (move.category == com.example.data.model.BattleMoveCategory.ELEMENTAL_STRIKE) elemBadge else move.moveName.uppercase(),
            lastRollOutcomeTitle = "${active.nickname} used ${move.moveName}! (Wild HP: $newWildHp%)",
            lastRollOutcomeDetail = buildString {
                if (move.category == com.example.data.model.BattleMoveCategory.MERCY_SUBDUE && newWildHp == 1) {
                    append("MERCY SLASH SUBDUED ${currentEncounter.speciesName} TO 1% HP (+35% RED-HP CAPTURE BONUS)! ")
                } else if (move.category == com.example.data.model.BattleMoveCategory.ELEMENTAL_STRIKE) {
                    append("$elemBadge! Dealt -$rawDamagePercent% HP. ")
                } else {
                    append("Dealt -$rawDamagePercent% HP. ")
                }
                if (newStatus != com.example.data.model.WildStatusEffect.NONE) {
                    append("Status: ${newStatus.badgeLabel}! ")
                }
                if (statusStunSkipped) {
                    append("Wild ${currentEncounter.speciesName} is immobilized & couldn't counter-attack!")
                } else {
                    append("Wild counter-hit: -$counterDamage HP${if (selfHealAmount > 0) " (+$selfHealAmount HP restored)" else ""}.")
                }
            }
        )
        _wildEncounter.value = updatedEncounter

        val totalCaptureBonus = updatedEncounter.totalTacticalCaptureBonusPercent() + active.heldRelic().captureBonusPercent
        _statusBannerMessage.value =
            "${move.moveName} hit ${currentEncounter.speciesName}! Wild HP: $newWildHp% • Tactical Capture Bonus: +$totalCaptureBonus%!"

        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.updateCreature(updatedPartner)
            }
        }
    }

    fun battleWildMonster() {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return

        val currentEncounter = _wildEncounter.value ?: run {
            scanWildMonster()
            return
        }

        val basePartsDropped = if (Random.nextInt(100) < 35) 2 else 1
        val shinyBonusParts = if (currentEncounter.isChromaticShiny) 1 else 0
        val partsDropped = basePartsDropped + shinyBonusParts
        val isUltraRare = currentEncounter.isUltraRareWild
        if (isUltraRare) {
            _ultraRareMonsterPartsCount.value = _ultraRareMonsterPartsCount.value + partsDropped
        } else {
            _standardMonsterPartsCount.value = _standardMonsterPartsCount.value + partsDropped
        }

        // Wild battles deal combat damage and can drop the monster down to 1 HP!
        // When a monster reaches 1 HP, it enters a mandatory 5-minute recovery lockout before you can do anything with that monster again.
        val elemMult = com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
            attackerAttr = active.attribute,
            defenderAttr = currentEncounter.attribute
        )
        val elemBadge = com.example.data.model.ElementalMatchupEngine.effectivenessBadge(elemMult)
        val fierceCounterHit = (elemMult < 1.0f && Random.nextInt(100) < 38) || active.health <= 35
        val rawDamage = when (currentEncounter.stage) {
            "Mega" -> Random.nextInt(40, 68)
            "Ultimate" -> Random.nextInt(32, 56)
            "Champion" -> Random.nextInt(24, 44)
            else -> Random.nextInt(18, 36)
        }
        val reducedByAdvantage = if (elemMult >= 1.8f) (rawDamage * 0.65f).toInt() else rawDamage
        val passiveHeal = active.heldRelic().passiveHealBonus
        val newHealth = if (fierceCounterHit) {
            1
        } else {
            (active.health - reducedByAdvantage + passiveHeal).coerceIn(1, 100)
        }

        val hit1HpLockout = newHealth <= 1
        val recoveryEndMs = if (hit1HpLockout) {
            nowMs + DigimonCatalog.BATTLE_RECOVERY_5MIN_DURATION_MS
        } else {
            active.persistedRecoveryEndsAtMs()
        }

        val newForms = active.withUpdatedMetadataInForms(
            newSkillNodes = active.allUnlockedSkillNodeIds(),
            newBonusStats = active.persistedNonTrainingBonusStats(),
            newExpeditionsCount = active.persistedCompletedExpeditions(),
            newOfflineTrainEndMs = active.persistedOfflineTrainingEndMs(),
            newOfflineTrainTickMs = active.persistedOfflineTrainingLastTickMs(),
            newRecoveryEndsMs = recoveryEndMs
        )

        val updated = active.copy(
            health = newHealth,
            stamina = (active.stamina - 10).coerceIn(0, 100),
            bondLevel = (active.bondLevel + 2).coerceIn(0, 100),
            isResting = false,
            recoveryEndsAtMs = recoveryEndMs,
            unlockedForms = newForms,
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updated)

        val partTypeLabel = if (isUltraRare) {
            "Relic ${if (partsDropped == 1) "Shard" else "Shards"} (Sells for ${DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS} Crest Coins each)"
        } else {
            "Gilded ${if (partsDropped == 1) "Acorn" else "Acorns"} (Sells for ${DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS} Crest Coins each)"
        }

        val recoveryNotice = if (hit1HpLockout) {
            " • ⚠️ ${active.nickname} dropped to 1 HP! Must recover for 5:00 before doing anything with ${active.nickname} again."
        } else {
            " • ${active.nickname} HP: $newHealth/100"
        }

        val summary =
            "Defeated Wild ${currentEncounter.speciesName} ($elemBadge): Looted +${partsDropped}x $partTypeLabel!$recoveryNotice"
        val battleLog = prependOptimisticLog(
            TrainingLogEntry(
                userId = currentUserId,
                creatureId = active.id,
                creatureName = active.nickname,
                activityType = "Arena Sparring",
                grade = if (isUltraRare) "S" else "A",
                statDeltaSummary = summary.take(190),
                xpGained = if (isUltraRare) 95 else 60,
                dateKey = todayDateKey()
            )
        )

        if (hit1HpLockout) {
            val notif = prependOptimisticNotification(
                PetNotification(
                    userId = currentUserId,
                    creatureId = active.id,
                    creatureName = active.nickname,
                    severity = "CRITICAL",
                    title = "1 HP Knockdown: ${active.nickname} Recovering (5m)!",
                    message = "${active.nickname} dropped to 1 HP battling Wild ${currentEncounter.speciesName}! Looted +${partsDropped}x $partTypeLabel. ${active.nickname} must recover for 5 minutes before any action.",
                    isRead = false
                )
            )
            if (isGoogleSignedIn) {
                viewModelScope.launch { repository.addNotification(notif) }
            }
        }

        _statusBannerMessage.value = summary
        recordQuestProgress(TamerQuestType.WIN_BATTLES, 1)
        advanceDaycareIncubation(10)
        // Automatically scan a fresh wild monster after battle
        scanWildMonster()

        viewModelScope.launch {
            if (isGoogleSignedIn) {
                repository.updateCreature(updated)
                repository.addTrainingLog(battleLog)
            }
            checkAndDispatchThresholds(updated)
            syncLeaderboardWithSquad()
        }
    }

    fun throwTammingBerry() {
        val active = uiState.value.activeCreature
        if (active != null && checkBlockedBy1HpRecovery(active)) return
        val currentEncounter = _wildEncounter.value ?: run {
            scanWildMonster()
            return
        }
        if (currentEncounter.isTamed) {
            scanWildMonster()
            return
        }

        val currentState = uiState.value
        val preferredOrb = _selectedAcornOrbType.value
        val chosenOrb = if (currentState.getAcornOrbCount(preferredOrb) > 0) {
            preferredOrb
        } else {
            AcornOrbType.entries.firstOrNull { currentState.getAcornOrbCount(it) > 0 } ?: preferredOrb
        }
        val ownedOfChosen = currentState.getAcornOrbCount(chosenOrb)
        if (ownedOfChosen <= 0) {
            _statusBannerMessage.value =
                "You have 0 Acorn Orbs in your pouch! Craft Acorn Orbs below with Gilded Acorns or buy them for ${DigimonCatalog.TAMMING_BERRY_COIN_COST} Crest Coins."
            return
        }

        val remainingOfChosen = (ownedOfChosen - 1).coerceAtLeast(0)
        val updatedOrbMap = _acornOrbInventory.value.toMutableMap()
        updatedOrbMap[chosenOrb] = remainingOfChosen
        _acornOrbInventory.value = updatedOrbMap
        if (chosenOrb == AcornOrbType.STANDARD_ACORN_ORB) {
            _tammingBerryCount.value = remainingOfChosen
        }
        _selectedAcornOrbType.value = chosenOrb
        syncOrbTypeToFirestore(chosenOrb)

        val rarity = currentEncounter.rarityTier
        val activeBonus = active?.tammingRateBonusPercent() ?: 0
        // Rarer monsters have half or a third of the skill bonus effectiveness so they remain challenging
        val scaledSkillBonus = when (rarity) {
            com.example.data.model.WildRarityTier.COMMON -> activeBonus
            com.example.data.model.WildRarityTier.UNCOMMON -> (activeBonus * 3) / 4
            com.example.data.model.WildRarityTier.RARE -> activeBonus / 2
            com.example.data.model.WildRarityTier.ULTRA_RARE -> activeBonus / 3
            com.example.data.model.WildRarityTier.LEGENDARY_APEX -> activeBonus / 4
        }
        // Tactical Weakened-HP (+10% to +35%), Status Wave (+12% to +25%), Held Capture Charm (+12%), and Specialty Acorn Orb bonuses!
        val tacticalBonus = currentEncounter.totalTacticalCaptureBonusPercent()
        val heldCharmBonus = active?.heldRelic()?.captureBonusPercent ?: 0
        val orbSpecialtyBonus = chosenOrb.effectiveCaptureBonusFor(currentEncounter)
        val effectiveSuccessChance = (
            currentEncounter.baseSuccessRatePercent +
                scaledSkillBonus +
                tacticalBonus +
                heldCharmBonus +
                orbSpecialtyBonus
            ).coerceIn(3, 95)
        val roll = Random.nextInt(100)
        val newBerriesUsed = currentEncounter.berriesUsed + 1
        val hpCaptureBoostGain = when {
            currentEncounter.wildHpPercent in 1..20 -> 14
            currentEncounter.wildHpPercent in 21..50 -> 8
            else -> 0
        }
        val orbGaugeBoost = if (orbSpecialtyBonus >= 18) 8 else if (orbSpecialtyBonus > 0) 4 else 0

        val (gain, outcomeTitle, outcomeDetail) = if (roll < effectiveSuccessChance) {
            val critRoll = Random.nextInt(100)
            when {
                rarity.instantCritChancePercent > 0 && critRoll < rarity.instantCritChancePercent -> Triple(
                    100,
                    "CRITICAL ${chosenOrb.displayName.uppercase()} CAPTURE!",
                    "Lucky Critical Lock (${rarity.instantCritChancePercent}% Crit Odds)! ${currentEncounter.speciesName} (${rarity.displayName}) was instantly captured in a ${chosenOrb.displayName}!"
                )
                critRoll < 38 -> {
                    val surge = Random.nextInt(rarity.minSurgeGain, rarity.maxSurgeGain + 1) + hpCaptureBoostGain + orbGaugeBoost
                    Triple(
                        surge,
                        "${chosenOrb.displayName.uppercase()} SURGE! (+$surge% Capture)",
                        "High resonance lock vs ${currentEncounter.speciesName} (Wild HP ${currentEncounter.wildHpPercent}%, ${currentEncounter.statusEffect.badgeLabel})! Capture gauge surged by +$surge%."
                    )
                }
                else -> {
                    val hit = Random.nextInt(rarity.minHitGain, rarity.maxHitGain + 1) + hpCaptureBoostGain + orbGaugeBoost
                    Triple(
                        hit,
                        "${chosenOrb.displayName.uppercase()} LOCKED IN! (+$hit% Capture)",
                        "${chosenOrb.displayName} connected on ${currentEncounter.speciesName} (Effective Rate $effectiveSuccessChance%)! Capture gauge rose by +$hit%."
                    )
                }
            }
        } else {
            val deltaRange = rarity.resistPenaltyOrGraze
            val resistDelta = Random.nextInt(deltaRange.first, deltaRange.last + 1)
            val deltaText = if (resistDelta >= 0) "+$resistDelta% Capture" else "$resistDelta% Capture"
            val resistDetail = if (resistDelta < 0) {
                "Fierce ${rarity.displayName} resistance ($effectiveSuccessChance% Capture Rate)! Use Mercy Slash & Status Wave to weaken ${currentEncounter.speciesName} first!"
            } else {
                "Broke free from ${chosenOrb.displayName} ($effectiveSuccessChance% Capture Rate)! Weaken ${currentEncounter.speciesName} to Red HP (1–20%) or inflict Sleep/Paralyze!"
            }
            Triple(
                resistDelta,
                "WILD ${rarity.displayName.uppercase()} BROKE FREE! ($deltaText)",
                resistDetail
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
            recordQuestProgress(TamerQuestType.CATCH_WILD, 1)
            advanceDaycareIncubation(15)
            finalizeWildTaming(currentEncounter, chosenOrb)
        } else {
            _statusBannerMessage.value = "$outcomeTitle — ${currentEncounter.speciesName} Capture Gauge: $updatedProgress/100% ($remainingOfChosen ${chosenOrb.displayName}s left)"
        }
    }

    private fun finalizeWildTaming(
        encounter: WildEncounterState,
        orbUsed: AcornOrbType = _selectedAcornOrbType.value
    ) {
        val spec = DigimonCatalog.getSpec(encounter.speciesId)
        val nowMs = System.currentTimeMillis()
        val localId = "tamed_${nowMs}_${spec.speciesId}"
        val shinyStatBoost = if (encounter.isChromaticShiny) 6 else 0
        val tamedBaseBonus = if (spec.stage == "Rookie") {
            shinyStatBoost * 4
        } else {
            (spec.baseAttack + spec.baseDefense + spec.baseSpeed + spec.baseWisdom - 172 + (shinyStatBoost * 4)).coerceAtLeast(0)
        }
        val matchedNature = com.example.data.model.CompanionNature.entries.firstOrNull {
            it.displayName.equals(encounter.wildNatureName, ignoreCase = true)
        } ?: com.example.data.model.CompanionNature.BRAVE
        val defaultRelic = com.example.data.model.HeldRelicItem.SEARING_CHARCOAL
        val shinyFlag = if (encounter.isChromaticShiny) 1 else 0
        val friendOrbBondBoost = if (orbUsed == AcornOrbType.VERDANT_FRIEND_ORB) 25 else 0
        val startingBond = ((if (encounter.isChromaticShiny) 35 else 20) + friendOrbBondBoost).coerceAtMost(100)
        val initialForms = buildList {
            add(spec.speciesId)
            if (tamedBaseBonus > 0) add("bn:$tamedBaseBonus")
            add("rg:${matchedNature.code}:${defaultRelic.code}:$shinyFlag:0")
        }
        val tamedCreature = DigimonCreature(
            id = localId,
            userId = currentUserId,
            nickname = if (encounter.isChromaticShiny) "★${spec.speciesName}" else spec.speciesName,
            speciesId = spec.speciesId,
            speciesName = spec.speciesName,
            stage = spec.stage,
            attribute = spec.attribute,
            health = 100,
            stamina = 100,
            hunger = 85,
            evolutionProgress = 0,
            attack = spec.baseAttack + shinyStatBoost,
            defense = spec.baseDefense + shinyStatBoost,
            speed = spec.baseSpeed + shinyStatBoost,
            wisdom = spec.baseWisdom + shinyStatBoost,
            nonTrainingBonusStats = tamedBaseBonus,
            bondLevel = startingBond,
            careMistakes = 0,
            isResting = false,
            unlockedForms = initialForms,
            lastUpdatedMs = nowMs
        )

        val currentSquad = uiState.value.creatures
        val goesToPartyDirectly = currentSquad.size < DigimonCatalog.MAX_SQUAD_SIZE

        // Record in the offline-cached + Firestore Emerald Reserve Collection Inventory!
        val reserveEntry = EmeraldReserveEntry(
            id = localId,
            userId = currentUserId,
            speciesId = spec.speciesId,
            speciesName = spec.speciesName,
            nickname = tamedCreature.nickname,
            stage = spec.stage,
            elementAttribute = spec.attribute,
            rarityTier = encounter.rarityTier.name,
            capturedLevel = encounter.wildLevel.coerceIn(1, 100),
            attack = tamedCreature.attack,
            defense = tamedCreature.defense,
            speed = tamedCreature.speed,
            wisdom = tamedCreature.wisdom,
            bondLevel = startingBond,
            natureCode = matchedNature.code,
            heldRelicCode = defaultRelic.code,
            chromaticShiny = encounter.isChromaticShiny,
            orbUsedId = orbUsed.orbTypeId,
            biomeZone = "Emerald Reserve Safari",
            inActiveParty = goesToPartyDirectly,
            capturedAtMs = nowMs
        )
        _optimisticEmeraldReserve.value = listOf(reserveEntry) + _optimisticEmeraldReserve.value.filter { it.id != reserveEntry.id }
        if (isGoogleSignedIn) {
            viewModelScope.launch {
                repository.recordEmeraldReserveCapture(reserveEntry)
            }
        }

        recordQuestProgress(TamerQuestType.CATCH_WILD, 1)
        advanceDaycareIncubation(15)
        if (!goesToPartyDirectly) {
            _pendingTamedCreature.value = tamedCreature
            _statusBannerMessage.value =
                "Captured ${if (encounter.isChromaticShiny) "✨ STARLIGHT CHROMATIC " else ""}${spec.speciesName} in a ${orbUsed.displayName}! Saved to your Emerald Reserve Vault — choose a party member to swap or keep in Reserve."
            return
        }

        addTamedCreatureToSquad(tamedCreature)
    }

    fun deployFromEmeraldReserve(entry: EmeraldReserveEntry) {
        val currentSquad = uiState.value.creatures
        val creatureFromVault = entry.toDigimonCreature()
        if (currentSquad.any { it.id == entry.id || it.speciesId == entry.speciesId }) {
            _statusBannerMessage.value = "${entry.nickname} is already in your active 4-Companion Party!"
            return
        }
        if (currentSquad.size >= DigimonCatalog.MAX_SQUAD_SIZE) {
            _pendingTamedCreature.value = creatureFromVault
            _statusBannerMessage.value =
                "Select which companion in your 4-Companion Party to swap with ${entry.nickname} from the Emerald Reserve Vault!"
            return
        }
        addTamedCreatureToSquad(creatureFromVault)
        _optimisticEmeraldReserve.value = _optimisticEmeraldReserve.value.map {
            if (it.id == entry.id) it.copy(inActiveParty = true) else it
        }
        if (isGoogleSignedIn && entry.id.isNotBlank()) {
            viewModelScope.launch {
                repository.updateEmeraldReservePartyStatus(entry.id, inActiveParty = true)
            }
        }
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
        if (checkBlockedBy1HpRecovery(active)) return
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
        if (checkBlockedBy1HpRecovery(active)) return
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
        val newBonusStats = active.persistedNonTrainingBonusStats() + statBonusTotal
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
        if (checkBlockedBy1HpRecovery(active)) return
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
        if (friendUserId.isNotBlank() && DigimonCatalog.isNpcOrBotIdentifier(friendUserId)) {
            _statusBannerMessage.value = "NPCs are removed from Chat! You can only chat with real-life players."
            _activeDirectChatFriendId.value = ""
            return
        }
        _activeDirectChatFriendId.value = friendUserId
    }

    fun sendChatMessage(messageText: String, recipientUserId: String = _activeDirectChatFriendId.value) {
        if (!isGoogleSignedIn) {
            _statusBannerMessage.value = "You can only use Friends & Player Chat when logged into Google!"
            return
        }
        if (recipientUserId.isNotBlank() && DigimonCatalog.isNpcOrBotIdentifier(recipientUserId)) {
            _statusBannerMessage.value = "NPCs are removed from Chat! You can only message real-life players."
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
        if (DigimonCatalog.isNpcOrBotIdentifier(cleanTargetId) || DigimonCatalog.isNpcOrBotIdentifier(targetTrainerName)) {
            _statusBannerMessage.value = "NPCs are removed from Friends & Chat! You can only add real-life players."
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

    // =========================================================================
    // LONG-PLAY RPG EXPANSION:
    // 1. Visual World Map & 4-Move Turn-Based Battle Engine (Speed & Switching)
    // 2. Sanctuary Daycare Breeding & Starlight Chromatic Shiny Hunting
    // 3. Endless Battle Frontier Tower & BP Exchange
    // 4. Daily & Weekly Tamer Quests + Milestone Reward Pass
    // 5. Relic Crafting Forge (+1..+5) & Resonance Rune Sockets
    // =========================================================================

    fun selectWorldRegion(regionId: String) {
        val region = LongPlayRpgCatalog.worldRegions.firstOrNull { it.regionId == regionId }
            ?: LongPlayRpgCatalog.worldRegions.first()
        _selectedWorldRegionId.value = region.regionId
        val preferredSpecies = region.featuredSpeciesIds[Random.nextInt(region.featuredSpeciesIds.size)]
        scanWildMonster(preferredSpecies)
        _statusBannerMessage.value =
            "🗺️ Traveled to ${region.regionTitle} (${region.realmName} • ${region.weatherLabel}): ${region.weatherEffectSummary}"
    }

    fun syncTurnBasedBattleFromWild() {
        val encounter = _wildEncounter.value ?: return
        val currentTb = _turnBasedBattle.value
        if (currentTb != null && currentTb.mode != TurnBasedBattleMode.WILD_ENCOUNTER && !currentTb.isFinished) {
            // Do not overwrite an active Sanctum Leader or Frontier Tower battle
            return
        }
        val spec = DigimonCatalog.getSpec(encounter.speciesId)
        val maxHp = (90 + encounter.wildLevel * 12).coerceAtLeast(100)
        val curHp = ((maxHp * encounter.wildHpPercent) / 100).coerceIn(1, maxHp)
        _turnBasedBattle.value = TurnBasedBattleState(
            battleId = "wild_${spec.speciesId}_${encounter.wildLevel}",
            mode = TurnBasedBattleMode.WILD_ENCOUNTER,
            regionId = _selectedWorldRegionId.value,
            turnNumber = currentTb?.turnNumber ?: 1,
            enemyName = "${if (encounter.isChromaticShiny) "✨ " else ""}Wild ${spec.speciesName}",
            enemyTitle = "${encounter.rarityTier.displayName.uppercase()} • LVL ${encounter.wildLevel} • ${encounter.wildNatureName.uppercase()}",
            enemySpeciesId = spec.speciesId,
            enemyStage = spec.stage,
            enemyAttribute = spec.attribute,
            enemyLevel = encounter.wildLevel,
            enemyCurrentHp = curHp,
            enemyMaxHp = maxHp,
            enemyAttack = spec.baseAttack + encounter.wildLevel * 3,
            enemyDefense = spec.baseDefense + encounter.wildLevel * 3,
            enemySpeed = spec.baseSpeed + encounter.wildLevel * 3,
            enemyStatusEffect = encounter.statusEffect,
            isChromaticShiny = encounter.isChromaticShiny,
            isBossTier = encounter.rarityTier == com.example.data.model.WildRarityTier.LEGENDARY_APEX ||
                encounter.rarityTier == com.example.data.model.WildRarityTier.ULTRA_RARE,
            rarityTier = encounter.rarityTier,
            baseCaptureRatePercent = encounter.baseSuccessRatePercent,
            combatLog = listOf(
                "🌿 Encountered ${if (encounter.isChromaticShiny) "✨ STARLIGHT CHROMATIC " else ""}${spec.speciesName} (LVL ${encounter.wildLevel})!",
                encounter.lastRollOutcomeDetail
            ),
            lastActionHeadline = encounter.lastRollOutcomeTitle,
            lastActionDetail = encounter.lastRollOutcomeDetail
        )
    }

    fun startSanctumLeaderTurnBasedBattle(leader: SanctumLeaderSpec) {
        val active = uiState.value.activeCreature ?: return
        if (checkBlockedBy1HpRecovery(active)) return
        val spec = DigimonCatalog.getSpec(leader.aceSpeciesId)
        val maxHp = 140 + (leader.recommendedLevel * 16)
        val atk = spec.baseAttack + (leader.recommendedLevel * 5)
        val def = spec.baseDefense + (leader.recommendedLevel * 4)
        val spd = spec.baseSpeed + (leader.recommendedLevel * 4)
        _turnBasedBattle.value = TurnBasedBattleState(
            battleId = "sanctum_${leader.badgeNumber}",
            mode = TurnBasedBattleMode.SANCTUM_LEADER,
            regionId = _selectedWorldRegionId.value,
            turnNumber = 1,
            enemyName = "Leader ${leader.leaderName}'s ${leader.aceSpeciesName}",
            enemyTitle = "${leader.realmName.uppercase()} CREST #${leader.badgeNumber} • ${leader.badgeName.uppercase()}",
            enemySpeciesId = leader.aceSpeciesId,
            enemyStage = spec.stage,
            enemyAttribute = leader.elementAttribute,
            enemyLevel = leader.recommendedLevel,
            enemyCurrentHp = maxHp,
            enemyMaxHp = maxHp,
            enemyAttack = atk,
            enemyDefense = def,
            enemySpeed = spd,
            isBossTier = true,
            sanctumBadgeNumber = leader.badgeNumber,
            combatLog = listOf(
                "🏛️ Sanctum Leader ${leader.leaderName} of ${leader.cityTitle} challenged you for the ${leader.badgeName}!",
                "⚔️ ${leader.leaderName} sent out ${leader.aceSpeciesName} (LVL ${leader.recommendedLevel} • ${leader.elementAttribute})!"
            ),
            lastActionHeadline = "SANCTUM TRIAL: LEADER ${leader.leaderName.uppercase()}",
            lastActionDetail = "Use 4-Move Turn-Based Combat & Party Switching to defeat ${leader.aceSpeciesName} and earn the ${leader.badgeName}!"
        )
        _statusBannerMessage.value =
            "🏛️ Entered Turn-Based Sanctum Trial vs Leader ${leader.leaderName}'s ${leader.aceSpeciesName} (LVL ${leader.recommendedLevel})!"
    }

    fun startFrontierTowerFloorBattle() {
        val active = uiState.value.activeCreature ?: return
        if (checkBlockedBy1HpRecovery(active)) return
        val floor = _frontierTowerState.value.currentFloor.coerceAtLeast(1)
        val battleState = LongPlayRpgCatalog.buildFrontierOpponentForFloor(floor)
        _turnBasedBattle.value = battleState
        _statusBannerMessage.value =
            "🗼 Battle Frontier Tower Floor $floor started! ${battleState.enemyName} (LVL ${battleState.enemyLevel}) awaits!"
    }

    fun executeTurnBasedMove(moveIndex: Int) {
        val active = uiState.value.activeCreature ?: return
        val nowMs = System.currentTimeMillis()
        if (checkBlockedBy1HpRecovery(active, nowMs)) return

        val battle = _turnBasedBattle.value ?: run {
            syncTurnBasedBattleFromWild()
            _turnBasedBattle.value ?: return
        }
        if (battle.isFinished) {
            when (battle.mode) {
                TurnBasedBattleMode.WILD_ENCOUNTER -> scanWildMonster()
                TurnBasedBattleMode.FRONTIER_TOWER -> startFrontierTowerFloorBattle()
                TurnBasedBattleMode.SANCTUM_LEADER -> {
                    val leader = com.example.data.model.DigiWorldExpansionCatalog.allSanctumLeaders
                        .firstOrNull { it.badgeNumber == battle.sanctumBadgeNumber }
                        ?: com.example.data.model.DigiWorldExpansionCatalog.allSanctumLeaders.first()
                    startSanctumLeaderTurnBasedBattle(leader)
                }
            }
            return
        }

        // If in WILD_ENCOUNTER mode, also keep _wildEncounter synchronized
        val moves = DigimonCatalog.getFourBattleMoves(active)
        val move = moves.getOrElse(moveIndex.coerceIn(0, moves.lastIndex)) { moves.first() }
        val currentPp = battle.movePpRemaining[move.moveIndex] ?: move.maxPp
        if (currentPp <= 0) {
            _statusBannerMessage.value = "${move.moveName} has 0 PP remaining! Choose another move or switch companions."
            return
        }

        val relicUpgrade = uiState.value.getForgedRelicState(active.heldRelic())
        val playerFirst = battle.playerActsFirst(active, relicUpgrade.totalBonusSpdPct())
        val elemMult = com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
            attackerAttr = move.elementAttribute,
            defenderAttr = battle.enemyAttribute
        )
        val elemBadge = com.example.data.model.ElementalMatchupEngine.effectivenessBadge(elemMult)
        val stabMult = if (move.elementAttribute.equals(active.attribute, ignoreCase = true)) 1.25f else 1.0f
        val isCrit = Random.nextInt(100) < relicUpgrade.totalCritChancePct()
        val critMult = if (isCrit) 1.5f else 1.0f
        val relicAtkMult = 1f + (relicUpgrade.totalBonusAtkPct() / 100f)

        val rawPlayerPower = when (move.category) {
            com.example.data.model.BattleMoveCategory.ELEMENTAL_STRIKE ->
                ((active.effectiveAttack() * 0.55f + move.power * 0.65f) * elemMult * stabMult * critMult * relicAtkMult).toInt()
            com.example.data.model.BattleMoveCategory.MERCY_SUBDUE ->
                ((active.effectiveAttack() * 0.45f + move.power * 0.55f) * relicAtkMult).toInt()
            com.example.data.model.BattleMoveCategory.STATUS_WAVE ->
                ((active.effectiveWisdom() * 0.30f + 18) * elemMult).toInt()
            com.example.data.model.BattleMoveCategory.CREST_SURGE ->
                ((active.effectiveWisdom() * 0.45f + active.effectiveAttack() * 0.25f + 28) * critMult * relicAtkMult).toInt()
        }.coerceAtLeast(12)

        var newStatus = battle.enemyStatusEffect
        if (move.category == com.example.data.model.BattleMoveCategory.STATUS_WAVE) {
            newStatus = when {
                move.moveName.contains("Sleep", ignoreCase = true) -> com.example.data.model.WildStatusEffect.SLEEP
                move.moveName.contains("Paralyze", ignoreCase = true) -> com.example.data.model.WildStatusEffect.PARALYZED
                else -> com.example.data.model.WildStatusEffect.BURNED
            }
        }

        val minEnemyHpAllowed = if (
            battle.mode == TurnBasedBattleMode.WILD_ENCOUNTER &&
            move.category != com.example.data.model.BattleMoveCategory.ELEMENTAL_STRIKE
        ) {
            maxOf(1, battle.enemyMaxHp / 100)
        } else {
            0
        }

        val newEnemyHp = (battle.enemyCurrentHp - rawPlayerPower).coerceAtLeast(minEnemyHpAllowed)
        val lifestealHeal = ((rawPlayerPower * relicUpgrade.totalLifestealPct()) / 100).coerceAtLeast(0)
        val crestSurgeHeal = if (move.category == com.example.data.model.BattleMoveCategory.CREST_SURGE) 18 else 0
        val passiveRelicHeal = active.heldRelic().passiveHealBonus
        val totalSelfHeal = lifestealHeal + crestSurgeHeal + passiveRelicHeal

        val enemyDefeated = newEnemyHp <= 0
        val statusImmobilized = !enemyDefeated && when (newStatus) {
            com.example.data.model.WildStatusEffect.SLEEP -> Random.nextInt(100) < 72
            com.example.data.model.WildStatusEffect.PARALYZED -> Random.nextInt(100) < 48
            else -> false
        }

        val enemyCounterMult = com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
            attackerAttr = battle.enemyAttribute,
            defenderAttr = active.attribute
        )
        val guardReductionMult = (1f - (relicUpgrade.totalBonusDefPct().coerceAtMost(60) / 150f)).coerceIn(0.45f, 1.0f)
        val burnWeakenedMult = if (newStatus == com.example.data.model.WildStatusEffect.BURNED) 0.65f else 1.0f
        val enemyDamage = if (enemyDefeated || statusImmobilized) {
            0
        } else {
            val baseEnemyHit = (10 + (battle.enemyLevel / 3) + Random.nextInt(4, 12))
            (baseEnemyHit * enemyCounterMult * guardReductionMult * burnWeakenedMult).toInt().coerceIn(3, 42)
        }

        val newPlayerHp = (active.health - enemyDamage + totalSelfHeal).coerceIn(1, 100)
        val hit1Hp = newPlayerHp <= 1 && !enemyDefeated
        val recoveryEndMs = if (hit1Hp) nowMs + DigimonCatalog.BATTLE_RECOVERY_5MIN_DURATION_MS else active.persistedRecoveryEndsAtMs()

        advanceDaycareIncubation(10)

        val turnLogs = mutableListOf<String>()
        val speedOrderTag = if (playerFirst) "⚡ First Strike (SPD ${active.effectiveSpeed()})" else "🛡️ Counter Turn (Enemy SPD ${battle.enemySpeed})"
        val critTag = if (isCrit) " 💥 CRITICAL HIT!" else ""
        turnLogs.add("Turn ${battle.turnNumber} [$speedOrderTag]: ${active.nickname} used ${move.moveName}!$critTag (-$rawPlayerPower HP • $elemBadge)")
        if (newStatus != battle.enemyStatusEffect && newStatus != com.example.data.model.WildStatusEffect.NONE) {
            turnLogs.add("✨ ${battle.enemyName} is now ${newStatus.badgeLabel}!")
        }
        if (totalSelfHeal > 0) {
            turnLogs.add("💚 ${active.nickname} restored +$totalSelfHeal HP (Relic/Rune/Crest Resonance).")
        }
        if (!enemyDefeated) {
            if (statusImmobilized) {
                turnLogs.add("⚡ ${battle.enemyName} is immobilized by ${newStatus.name} and couldn't attack!")
            } else {
                turnLogs.add("⚔️ ${battle.enemyName} struck back for -$enemyDamage HP!")
            }
        }

        val updatedPpMap = battle.movePpRemaining.toMutableMap().apply {
            put(move.moveIndex, (currentPp - 1).coerceAtLeast(0))
        }

        if (enemyDefeated) {
            handleTurnBasedVictory(active, battle, turnLogs, newPlayerHp, nowMs)
            return
        }

        val updatedCreature = active.copy(
            health = newPlayerHp,
            stamina = (active.stamina - 4).coerceIn(0, 100),
            bondLevel = (active.bondLevel + 1).coerceIn(0, 100),
            recoveryEndsAtMs = recoveryEndMs,
            unlockedForms = active.withUpdatedMetadataInForms(newRecoveryEndsMs = recoveryEndMs),
            lastUpdatedMs = nowMs
        )
        upsertOptimisticCreature(updatedCreature)
        if (isGoogleSignedIn) {
            viewModelScope.launch { repository.updateCreature(updatedCreature) }
        }

        val updatedBattle = battle.copy(
            turnNumber = battle.turnNumber + 1,
            enemyCurrentHp = newEnemyHp,
            enemyStatusEffect = newStatus,
            movePpRemaining = updatedPpMap,
            combatLog = (turnLogs + battle.combatLog).take(10),
            lastActionHeadline = "${active.nickname} used ${move.moveName}!$critTag",
            lastActionDetail = turnLogs.joinToString(" • "),
            lastEffectivenessMultiplier = elemMult
        )
        _turnBasedBattle.value = updatedBattle

        // If in WILD_ENCOUNTER mode, mirror HP % and status to _wildEncounter so Acorn Orb throws use the weakened HP & status!
        if (battle.mode == TurnBasedBattleMode.WILD_ENCOUNTER) {
            _wildEncounter.value = _wildEncounter.value?.copy(
                wildHpPercent = updatedBattle.enemyHpPercent.coerceIn(1, 100),
                statusEffect = newStatus,
                lastEffectivenessLabel = elemBadge,
                lastRollOutcomeTitle = updatedBattle.lastActionHeadline,
                lastRollOutcomeDetail = updatedBattle.lastActionDetail
            )
        }
    }

    private fun handleTurnBasedVictory(
        active: DigimonCreature,
        battle: TurnBasedBattleState,
        turnLogs: List<String>,
        remainingPlayerHp: Int,
        nowMs: Long
    ) {
        recordQuestProgress(TamerQuestType.WIN_BATTLES, 1)
        when (battle.mode) {
            TurnBasedBattleMode.WILD_ENCOUNTER -> {
                battleWildMonster()
                return
            }
            TurnBasedBattleMode.SANCTUM_LEADER -> {
                val leader = com.example.data.model.DigiWorldExpansionCatalog.allSanctumLeaders
                    .firstOrNull { it.badgeNumber == battle.sanctumBadgeNumber }
                if (leader != null) {
                    val alreadyEarned = active.hasSanctumBadge(leader.badgeNumber)
                    val bit = 1 shl (leader.badgeNumber - 1)
                    val newMask = active.earnedSanctumBadgesMask() or bit
                    val coinsWon = if (!alreadyEarned) leader.rewardCoins else (leader.rewardCoins / 2).coerceAtLeast(15)
                    _digiCoins.value = _digiCoins.value + coinsWon
                    _ultraRareMonsterPartsCount.value = _ultraRareMonsterPartsCount.value + 1
                    recordQuestProgress(TamerQuestType.DEFEAT_BOSS_OR_LEADER, 1)

                    val updated = active.copy(
                        health = remainingPlayerHp.coerceIn(15, 100),
                        attack = active.attack + 2,
                        defense = active.defense + 2,
                        speed = active.speed + 2,
                        wisdom = active.wisdom + 3,
                        evolutionProgress = (active.evolutionProgress + 12).coerceIn(0, 100),
                        bondLevel = (active.bondLevel + 4).coerceIn(0, 100),
                        unlockedForms = active.withUpdatedMetadataInForms(newBadgesMask = newMask),
                        lastUpdatedMs = nowMs
                    )
                    upsertOptimisticCreature(updated)
                    val victoryHeadline = "🏅 DEFEATED LEADER ${leader.leaderName.uppercase()}! EARNED ${leader.badgeName.uppercase()}!"
                    val victoryDetail = "Earned +$coinsWon Crest Coins, +1x Relic Shard, & +1 Companion Level!"
                    _turnBasedBattle.value = battle.copy(
                        enemyCurrentHp = 0,
                        isFinished = true,
                        playerWon = true,
                        rewardCoinsEarned = coinsWon,
                        combatLog = listOf(victoryHeadline, victoryDetail) + turnLogs + battle.combatLog,
                        lastActionHeadline = victoryHeadline,
                        lastActionDetail = victoryDetail
                    )
                    _statusBannerMessage.value = "$victoryHeadline $victoryDetail"
                    if (isGoogleSignedIn) {
                        viewModelScope.launch {
                            repository.updateCreature(updated)
                            syncLeaderboardWithSquad(extraSparringWin = 1)
                        }
                    }
                }
            }
            TurnBasedBattleMode.FRONTIER_TOWER -> {
                val curTower = _frontierTowerState.value
                val floorCleared = battle.frontierFloorNumber.coerceAtLeast(curTower.currentFloor)
                val isBoss = floorCleared % 5 == 0
                val bpEarned = (if (isBoss) 18 else 8) + (curTower.currentWinStreak.coerceAtMost(10))
                val coinsEarned = (15 + floorCleared * 3 + if (isBoss) 25 else 0)
                val nextFloor = floorCleared + 1
                val newStreak = curTower.currentWinStreak + 1
                val updatedTower = curTower.copy(
                    currentFloor = nextFloor,
                    highestFloorReached = maxOf(curTower.highestFloorReached, nextFloor),
                    currentWinStreak = newStreak,
                    bestWinStreak = maxOf(curTower.bestWinStreak, newStreak),
                    battlePoints = curTower.battlePoints + bpEarned,
                    totalBossesDefeated = curTower.totalBossesDefeated + (if (isBoss) 1 else 0)
                )
                _frontierTowerState.value = updatedTower
                saveFrontierStateToStorage(updatedTower)

                _digiCoins.value = _digiCoins.value + coinsEarned
                if (isBoss) {
                    _ultraRareMonsterPartsCount.value = _ultraRareMonsterPartsCount.value + 2
                    recordQuestProgress(TamerQuestType.DEFEAT_BOSS_OR_LEADER, 1)
                } else {
                    _standardMonsterPartsCount.value = _standardMonsterPartsCount.value + 2
                }
                recordQuestProgress(TamerQuestType.CLIMB_FRONTIER_TOWER, 1)

                val updated = active.copy(
                    health = remainingPlayerHp.coerceIn(20, 100),
                    attack = active.attack + 2,
                    defense = active.defense + 2,
                    speed = active.speed + 2,
                    wisdom = active.wisdom + 3,
                    evolutionProgress = (active.evolutionProgress + 10).coerceIn(0, 100),
                    bondLevel = (active.bondLevel + 3).coerceIn(0, 100),
                    lastUpdatedMs = nowMs
                )
                upsertOptimisticCreature(updated)
                val headline = if (isBoss) {
                    "👑 FRONTIER BOSS FLOOR $floorCleared CONQUERED!"
                } else {
                    "🗼 FRONTIER FLOOR $floorCleared CLEARED! (STREAK x$newStreak)"
                }
                val detail = "Earned +$bpEarned Battle Points (BP), +$coinsEarned Crest Coins, & +1 Companion Level! Next: Floor $nextFloor."
                _turnBasedBattle.value = battle.copy(
                    enemyCurrentHp = 0,
                    isFinished = true,
                    playerWon = true,
                    rewardCoinsEarned = coinsEarned,
                    rewardBpEarned = bpEarned,
                    combatLog = listOf(headline, detail) + turnLogs + battle.combatLog,
                    lastActionHeadline = headline,
                    lastActionDetail = detail
                )
                _statusBannerMessage.value = "$headline $detail"
                if (isGoogleSignedIn) {
                    viewModelScope.launch {
                        repository.updateCreature(updated)
                        syncLeaderboardWithSquad(extraSparringWin = 1)
                    }
                }
            }
        }
    }

    fun switchPartyCompanionMidBattle(targetCreatureId: String) {
        val currentState = uiState.value
        val target = currentState.creatures.firstOrNull { it.id == targetCreatureId } ?: return
        if (target.id == currentState.activeCreature?.id) {
            _statusBannerMessage.value = "${target.nickname} is already leading the battle!"
            return
        }
        if (checkBlockedBy1HpRecovery(target)) return
        _selectedCreatureId.value = target.id
        val battle = _turnBasedBattle.value
        if (battle != null && !battle.isFinished) {
            val elemMult = com.example.data.model.ElementalMatchupEngine.effectivenessMultiplier(
                attackerAttr = target.attribute,
                defenderAttr = battle.enemyAttribute
            )
            val matchupNote = com.example.data.model.ElementalMatchupEngine.effectivenessBadge(elemMult)
            val switchMsg = "🔄 Switched in ${target.nickname} (${target.attribute} • LVL ${target.currentLevel()}) with Tactical Aegis Ward (+15% Guard)! Matchup: $matchupNote"
            _turnBasedBattle.value = battle.copy(
                turnNumber = battle.turnNumber + 1,
                movePpRemaining = mapOf(0 to 15, 1 to 25, 2 to 15, 3 to 10),
                playerTempDefStage = (battle.playerTempDefStage + 1).coerceAtMost(3),
                combatLog = (listOf(switchMsg) + battle.combatLog).take(10),
                lastActionHeadline = "TACTICAL SWITCH: ${target.nickname.uppercase()}!",
                lastActionDetail = switchMsg
            )
            _statusBannerMessage.value = switchMsg
        } else {
            _statusBannerMessage.value = "Switched lead companion to ${target.nickname}!"
        }
    }

    fun assignDaycareParents(parentOneId: String, parentTwoId: String) {
        val allPool = uiState.value.creatures + uiState.value.emeraldReserveEntries.map { it.toDigimonCreature() }
        val p1 = allPool.firstOrNull { it.id == parentOneId } ?: uiState.value.activeCreature ?: return
        val p2 = allPool.firstOrNull { it.id == parentTwoId && it.id != p1.id }
            ?: allPool.firstOrNull { it.id != p1.id }

        val p2Name = p2?.nickname ?: "Sanctuary Astral Spirit"
        val p2SpeciesId = p2?.speciesId ?: p1.speciesId
        val p2Attr = p2?.attribute ?: "Astral"
        val p2Shiny = p2?.isChromaticShiny() ?: false

        val originRookieId = p1.unlockedEvolutionFormIds().firstOrNull { id ->
            DigimonCatalog.allSpecies[id]?.stage == "Rookie"
        } ?: DigimonCatalog.starterSpecs.firstOrNull {
            it.attribute.equals(p1.attribute, ignoreCase = true)
        }?.speciesId ?: "patamon"
        val offspringSpec = DigimonCatalog.requireRookieStarterSpec(originRookieId)

        val sameElement = p1.attribute.equals(p2Attr, ignoreCase = true)
        val shinyCharmBonus = if (_frontierTowerState.value.shinyCharmUnlocked) 15 else 0
        val parentShinyBonus = (if (p1.isChromaticShiny()) 10 else 0) + (if (p2Shiny) 10 else 0)
        val runeShinyBonus = uiState.value.getForgedRelicState(p1.heldRelic()).totalCaptureAndShinyBonusPct() / 2
        val totalShinyOdds = (15 + (if (sameElement) 8 else 4) + shinyCharmBonus + parentShinyBonus + runeShinyBonus).coerceIn(15, 65)
        val inheritedIvBonus = (18 + (p1.bondLevel / 10) + (if (sameElement) 8 else 4)).coerceIn(18, 36)

        val newDaycare = _daycareState.value.copy(
            parentOneId = p1.id,
            parentOneName = p1.nickname,
            parentOneSpeciesId = p1.speciesId,
            parentOneAttribute = p1.attribute,
            parentOneShiny = p1.isChromaticShiny(),
            parentTwoId = p2?.id ?: "astral_spirit",
            parentTwoName = p2Name,
            parentTwoSpeciesId = p2SpeciesId,
            parentTwoAttribute = p2Attr,
            parentTwoShiny = p2Shiny,
            hasActiveEgg = true,
            offspringSpeciesId = offspringSpec.speciesId,
            offspringSpeciesName = offspringSpec.speciesName,
            inheritedNatureCode = p1.companionNature().code,
            inheritedBonusStats = inheritedIvBonus,
            inheritedEggMove = "${p1.attribute}-${p2Attr} Crest Burst",
            chromaticShinyOddsPercent = totalShinyOdds,
            currentIncubationPoints = 25,
            targetIncubationPoints = 100
        )
        _daycareState.value = newDaycare
        saveDaycareStateToStorage(newDaycare)
        recordQuestProgress(TamerQuestType.HATCH_OR_NURTURE_EGG, 1)
        _statusBannerMessage.value =
            "🥚 Sanctuary Daycare Egg Paired! Parents: ${p1.nickname} & $p2Name ➔ ${offspringSpec.speciesName} Egg (+$inheritedIvBonus Bonus IVs • $totalShinyOdds% ✨ Chromatic Shiny Odds)!"
    }

    fun advanceDaycareIncubation(points: Int) {
        val current = _daycareState.value
        if (!current.hasActiveEgg) return
        val updated = current.copy(
            currentIncubationPoints = (current.currentIncubationPoints + points).coerceAtMost(current.targetIncubationPoints)
        )
        _daycareState.value = updated
        saveDaycareStateToStorage(updated)
    }

    fun nurtureDaycareEgg() {
        val current = _daycareState.value
        if (!current.hasActiveEgg) {
            val lead = uiState.value.activeCreature ?: return
            assignDaycareParents(lead.id, "")
            return
        }
        if (current.isReadyToHatch) {
            hatchDaycareEgg()
            return
        }
        val costCoins = 4
        if (_digiCoins.value >= costCoins) {
            _digiCoins.value = (_digiCoins.value - costCoins).coerceAtLeast(0)
        }
        val nextPts = (current.currentIncubationPoints + 38).coerceAtMost(current.targetIncubationPoints)
        val updated = current.copy(currentIncubationPoints = nextPts)
        _daycareState.value = updated
        saveDaycareStateToStorage(updated)
        recordQuestProgress(TamerQuestType.HATCH_OR_NURTURE_EGG, 1)
        _statusBannerMessage.value = if (updated.isReadyToHatch) {
            "🥚 Sanctuary Egg reached 100% Incubation! Tap HATCH SANCTUARY EGG to welcome your newborn companion!"
        } else {
            "🔥 Warmed Sanctuary Egg (+38% Incubation ➔ ${updated.incubationPercent}%/100%)!"
        }
    }

    fun hatchDaycareEgg() {
        val current = _daycareState.value
        if (!current.hasActiveEgg) return
        val spec = DigimonCatalog.requireRookieStarterSpec(current.offspringSpeciesId)
        val isShiny = Random.nextInt(100) < current.chromaticShinyOddsPercent
        val nature = com.example.data.model.CompanionNature.fromCode(current.inheritedNatureCode)
        val ivPerStat = (current.inheritedBonusStats / 4).coerceAtLeast(4) + (if (isShiny) 4 else 0)
        val totalBonus = ivPerStat * 4
        val nowMs = System.currentTimeMillis()
        val newbornId = "bred_${nowMs}_${spec.speciesId}"
        val shinyFlag = if (isShiny) 1 else 0
        val defaultRelic = HeldRelicItem.VERDANT_SEED
        val initialForms = listOf(
            spec.speciesId,
            "bn:$totalBonus",
            "rg:${nature.code}:${defaultRelic.code}:$shinyFlag:0"
        )
        val newborn = DigimonCreature(
            id = newbornId,
            userId = currentUserId,
            nickname = if (isShiny) "★${spec.speciesName}" else "${spec.speciesName}+",
            speciesId = spec.speciesId,
            speciesName = spec.speciesName,
            stage = "Rookie",
            attribute = spec.attribute,
            health = 100,
            stamina = 100,
            hunger = 100,
            evolutionProgress = 20,
            attack = spec.baseAttack + ivPerStat,
            defense = spec.baseDefense + ivPerStat,
            speed = spec.baseSpeed + ivPerStat,
            wisdom = spec.baseWisdom + ivPerStat,
            nonTrainingBonusStats = totalBonus,
            bondLevel = 55,
            unlockedForms = initialForms,
            lastUpdatedMs = nowMs
        )

        val goesToParty = uiState.value.creatures.size < DigimonCatalog.MAX_SQUAD_SIZE
        val reserveEntry = EmeraldReserveEntry(
            id = newbornId,
            userId = currentUserId,
            speciesId = spec.speciesId,
            speciesName = spec.speciesName,
            nickname = newborn.nickname,
            stage = "Rookie",
            elementAttribute = spec.attribute,
            rarityTier = if (isShiny) "ULTRA_RARE" else "RARE",
            capturedLevel = 1,
            attack = newborn.attack,
            defense = newborn.defense,
            speed = newborn.speed,
            wisdom = newborn.wisdom,
            bondLevel = 55,
            natureCode = nature.code,
            heldRelicCode = defaultRelic.code,
            chromaticShiny = isShiny,
            orbUsedId = AcornOrbType.VERDANT_FRIEND_ORB.orbTypeId,
            biomeZone = "Sanctuary Daycare Nursery",
            inActiveParty = goesToParty,
            capturedAtMs = nowMs
        )
        _optimisticEmeraldReserve.value = listOf(reserveEntry) + _optimisticEmeraldReserve.value
        if (isGoogleSignedIn) {
            viewModelScope.launch { repository.recordEmeraldReserveCapture(reserveEntry) }
        }
        if (goesToParty) {
            addTamedCreatureToSquad(newborn)
        } else {
            _pendingTamedCreature.value = newborn
        }

        val updatedDaycare = current.copy(
            currentIncubationPoints = 0,
            totalEggsHatched = current.totalEggsHatched + 1,
            totalShinyHatched = current.totalShinyHatched + (if (isShiny) 1 else 0)
        )
        _daycareState.value = updatedDaycare
        saveDaycareStateToStorage(updatedDaycare)
        recordQuestProgress(TamerQuestType.HATCH_OR_NURTURE_EGG, 1)

        val shinyCallout = if (isShiny) "✨ STARLIGHT CHROMATIC SHINY HATCHED! " else "🐣 SANCTUARY EGG HATCHED! "
        _statusBannerMessage.value =
            "$shinyCallout${newborn.nickname} (${nature.displayName} • +$totalBonus Inherited IVs • Egg Move: ${current.inheritedEggMove}) joined your Sanctuary!"
    }

    fun redeemFrontierBpItem(item: FrontierBpShopItem) {
        val tower = _frontierTowerState.value
        if (tower.battlePoints < item.bpCost) {
            _statusBannerMessage.value =
                "Need ${item.bpCost} Frontier BP for ${item.title} (You have ${tower.battlePoints} BP)! Climb Battle Frontier Tower floors to earn BP."
            return
        }
        val remainingBp = (tower.battlePoints - item.bpCost).coerceAtLeast(0)
        when (item) {
            FrontierBpShopItem.SHINY_STARLIGHT_CHARM -> {
                val updatedTower = tower.copy(battlePoints = remainingBp, shinyCharmUnlocked = true)
                _frontierTowerState.value = updatedTower
                saveFrontierStateToStorage(updatedTower)
                _statusBannerMessage.value =
                    "✨ Unlocked the Permanent Starlight Chromatic Charm! Wild & Daycare Shiny odds increased by +15%!"
            }
            FrontierBpShopItem.LUNAR_ORB_BUNDLE -> {
                val updatedTower = tower.copy(battlePoints = remainingBp)
                _frontierTowerState.value = updatedTower
                saveFrontierStateToStorage(updatedTower)
                val map = _acornOrbInventory.value.toMutableMap()
                val cur = map[AcornOrbType.LUNAR_MOON_ORB] ?: 0
                map[AcornOrbType.LUNAR_MOON_ORB] = cur + 3
                _acornOrbInventory.value = map
                syncOrbTypeToFirestore(AcornOrbType.LUNAR_MOON_ORB)
                _statusBannerMessage.value = "🌙 Redeemed +3x Lunar Moon Orbs from the Frontier BP Exchange!"
            }
            FrontierBpShopItem.RELIC_FORGE_CORE -> {
                val updatedTower = tower.copy(battlePoints = remainingBp)
                _frontierTowerState.value = updatedTower
                saveFrontierStateToStorage(updatedTower)
                _ultraRareMonsterPartsCount.value = _ultraRareMonsterPartsCount.value + 5
                _standardMonsterPartsCount.value = _standardMonsterPartsCount.value + 10
                _statusBannerMessage.value = "⚒️ Redeemed +5x Relic Shards & +10x Gilded Acorns for the Relic Forge!"
            }
            FrontierBpShopItem.SOVEREIGN_STAT_TOME -> {
                val active = uiState.value.activeCreature ?: return
                val updatedTower = tower.copy(battlePoints = remainingBp)
                _frontierTowerState.value = updatedTower
                saveFrontierStateToStorage(updatedTower)
                val newBonus = active.persistedNonTrainingBonusStats() + 24
                val updated = active.copy(
                    attack = active.attack + 6,
                    defense = active.defense + 6,
                    speed = active.speed + 6,
                    wisdom = active.wisdom + 6,
                    evolutionProgress = (active.evolutionProgress + 25).coerceIn(0, 100),
                    nonTrainingBonusStats = newBonus,
                    unlockedForms = active.withUpdatedMetadataInForms(newBonusStats = newBonus),
                    lastUpdatedMs = System.currentTimeMillis()
                )
                upsertOptimisticCreature(updated)
                if (isGoogleSignedIn) {
                    viewModelScope.launch { repository.updateCreature(updated) }
                }
                _statusBannerMessage.value =
                    "📖 Bestowed Sovereign Ascendant Tome upon ${active.nickname}: +6 ALL STATS (+24 Total) & +25% Crest Sync!"
            }
        }
    }

    private fun recordQuestProgress(type: TamerQuestType, delta: Int = 1) {
        val updated = _tamerQuests.value.map { quest ->
            if (quest.questType == type && !quest.isClaimed) {
                quest.copy(currentProgress = (quest.currentProgress + delta).coerceAtMost(quest.targetProgress))
            } else {
                quest
            }
        }
        _tamerQuests.value = updated
        saveQuestsStateToStorage(updated, _tamerPassState.value)
    }

    fun claimTamerQuestReward(questId: String) {
        val quest = _tamerQuests.value.firstOrNull { it.questId == questId } ?: return
        if (!quest.isCompleted || quest.isClaimed) return
        _digiCoins.value = _digiCoins.value + quest.rewardCoins
        _ultraRareMonsterPartsCount.value = _ultraRareMonsterPartsCount.value + quest.rewardRelicShards
        val updatedTower = _frontierTowerState.value.copy(
            battlePoints = _frontierTowerState.value.battlePoints + quest.rewardBp
        )
        _frontierTowerState.value = updatedTower
        saveFrontierStateToStorage(updatedTower)

        val updatedPass = _tamerPassState.value.copy(
            totalPassXp = _tamerPassState.value.totalPassXp + quest.rewardPassXp
        )
        _tamerPassState.value = updatedPass
        val updatedQuests = _tamerQuests.value.map {
            if (it.questId == questId) it.copy(isClaimed = true) else it
        }
        _tamerQuests.value = updatedQuests
        saveQuestsStateToStorage(updatedQuests, updatedPass)

        _statusBannerMessage.value =
            "🏆 Claimed '${quest.title}': +${quest.rewardCoins} Crest Coins, +${quest.rewardRelicShards}x Relic Shards, +${quest.rewardBp} BP, & +${quest.rewardPassXp} Tamer Pass XP!"
    }

    fun claimTamerPassMilestone(tier: Int) {
        val pass = _tamerPassState.value
        if (tier > pass.currentTier || tier in pass.claimedMilestoneTiers) return
        val coins = 40 + (tier * 10)
        val shards = 2
        _digiCoins.value = _digiCoins.value + coins
        _ultraRareMonsterPartsCount.value = _ultraRareMonsterPartsCount.value + shards
        val map = _acornOrbInventory.value.toMutableMap()
        map[AcornOrbType.LUNAR_MOON_ORB] = (map[AcornOrbType.LUNAR_MOON_ORB] ?: 0) + 1
        _acornOrbInventory.value = map

        val updatedPass = pass.copy(claimedMilestoneTiers = pass.claimedMilestoneTiers + tier)
        _tamerPassState.value = updatedPass
        saveQuestsStateToStorage(_tamerQuests.value, updatedPass)
        _statusBannerMessage.value =
            "🎁 Claimed Tamer Pass Tier $tier Milestone Chest: +$coins Crest Coins, +${shards}x Relic Shards, & +1x Lunar Moon Orb!"
    }

    fun upgradeHeldRelicInForge(relic: HeldRelicItem) {
        val currentMap = _forgedRelics.value.toMutableMap()
        val entry = currentMap[relic.code] ?: ForgedRelicUpgradeState(relicCode = relic.code)
        if (entry.upgradeLevel >= 5) {
            _statusBannerMessage.value = "${relic.displayName} is already at Maximum Sovereign +5 Enhancement!"
            return
        }
        val nextLevel = entry.upgradeLevel + 1
        val acornCost = nextLevel * 2
        val shardCost = if (nextLevel >= 3) 2 else 1
        val coinFallbackCost = nextLevel * 12

        if (_standardMonsterPartsCount.value >= acornCost && _ultraRareMonsterPartsCount.value >= shardCost) {
            _standardMonsterPartsCount.value = (_standardMonsterPartsCount.value - acornCost).coerceAtLeast(0)
            _ultraRareMonsterPartsCount.value = (_ultraRareMonsterPartsCount.value - shardCost).coerceAtLeast(0)
        } else if (_digiCoins.value >= coinFallbackCost) {
            _digiCoins.value = (_digiCoins.value - coinFallbackCost).coerceAtLeast(0)
        } else {
            _statusBannerMessage.value =
                "Need ${acornCost}x Gilded Acorns + ${shardCost}x Relic Shards (or $coinFallbackCost Crest Coins) to forge ${relic.displayName} +$nextLevel!"
            return
        }

        val upgraded = entry.copy(upgradeLevel = nextLevel)
        currentMap[relic.code] = upgraded
        _forgedRelics.value = currentMap
        saveRelicForgeToStorage(currentMap, _unlockedRuneCodes.value)
        recordQuestProgress(TamerQuestType.FORGE_OR_UPGRADE_RELIC, 1)
        _statusBannerMessage.value =
            "⚒️ Forged ${relic.displayName} +$nextLevel! Total Bonuses: +${upgraded.totalBonusAtkPct()}% ATK, +${upgraded.totalBonusDefPct()}% DEF, +${upgraded.totalBonusSpdPct()}% SPD, +${upgraded.totalCritChancePct()}% Crit!"
    }

    fun craftOrSocketResonanceRune(relic: HeldRelicItem, socketNumber: Int, rune: ResonanceRune) {
        val unlocked = _unlockedRuneCodes.value.toMutableSet()
        if (rune.runeCode !in unlocked) {
            val gCost = rune.craftGildedAcornsCost
            val sCost = rune.craftRelicShardsCost
            val coinFallback = 18
            if (_standardMonsterPartsCount.value >= gCost && _ultraRareMonsterPartsCount.value >= sCost) {
                _standardMonsterPartsCount.value = (_standardMonsterPartsCount.value - gCost).coerceAtLeast(0)
                _ultraRareMonsterPartsCount.value = (_ultraRareMonsterPartsCount.value - sCost).coerceAtLeast(0)
            } else if (_digiCoins.value >= coinFallback) {
                _digiCoins.value = (_digiCoins.value - coinFallback).coerceAtLeast(0)
            } else {
                _statusBannerMessage.value =
                    "Need ${gCost}x Gilded Acorns + ${sCost}x Relic Shards (or $coinFallback Coins) to craft ${rune.displayName}!"
                return
            }
            unlocked.add(rune.runeCode)
            _unlockedRuneCodes.value = unlocked
        }

        val currentMap = _forgedRelics.value.toMutableMap()
        val entry = currentMap[relic.code] ?: ForgedRelicUpgradeState(relicCode = relic.code)
        val updated = if (socketNumber <= 1) {
            entry.copy(socketOneRuneCode = rune.runeCode)
        } else {
            entry.copy(socketTwoRuneCode = rune.runeCode)
        }
        currentMap[relic.code] = updated
        _forgedRelics.value = currentMap
        saveRelicForgeToStorage(currentMap, unlocked)
        recordQuestProgress(TamerQuestType.FORGE_OR_UPGRADE_RELIC, 1)
        _statusBannerMessage.value =
            "💎 Socketed ${rune.displayName} (${rune.shortBadge}) into ${relic.displayName} Socket #$socketNumber!"
    }

    private fun saveDaycareStateToStorage(state: DaycareBreedingState) {
        runCatching {
            val obj = JSONObject().apply {
                put("parentOneId", state.parentOneId)
                put("parentOneName", state.parentOneName)
                put("parentOneSpeciesId", state.parentOneSpeciesId)
                put("parentOneAttribute", state.parentOneAttribute)
                put("parentOneShiny", state.parentOneShiny)
                put("parentTwoId", state.parentTwoId)
                put("parentTwoName", state.parentTwoName)
                put("parentTwoSpeciesId", state.parentTwoSpeciesId)
                put("parentTwoAttribute", state.parentTwoAttribute)
                put("parentTwoShiny", state.parentTwoShiny)
                put("hasActiveEgg", state.hasActiveEgg)
                put("offspringSpeciesId", state.offspringSpeciesId)
                put("offspringSpeciesName", state.offspringSpeciesName)
                put("inheritedNatureCode", state.inheritedNatureCode)
                put("inheritedBonusStats", state.inheritedBonusStats)
                put("inheritedEggMove", state.inheritedEggMove)
                put("chromaticShinyOddsPercent", state.chromaticShinyOddsPercent)
                put("currentIncubationPoints", state.currentIncubationPoints)
                put("targetIncubationPoints", state.targetIncubationPoints)
                put("totalEggsHatched", state.totalEggsHatched)
                put("totalShinyHatched", state.totalShinyHatched)
            }
            repository.saveLongPlayRpgJson(currentUserId, "daycare", obj.toString())
        }
    }

    private fun saveFrontierStateToStorage(state: FrontierTowerState) {
        runCatching {
            val obj = JSONObject().apply {
                put("currentFloor", state.currentFloor)
                put("highestFloorReached", state.highestFloorReached)
                put("currentWinStreak", state.currentWinStreak)
                put("bestWinStreak", state.bestWinStreak)
                put("battlePoints", state.battlePoints)
                put("totalBossesDefeated", state.totalBossesDefeated)
                put("shinyCharmUnlocked", state.shinyCharmUnlocked)
            }
            repository.saveLongPlayRpgJson(currentUserId, "frontier", obj.toString())
        }
    }

    private fun saveQuestsStateToStorage(quests: List<TamerQuestItem>, pass: TamerPassState) {
        runCatching {
            val arr = JSONArray()
            for (q in quests) {
                arr.put(
                    JSONObject().apply {
                        put("questId", q.questId)
                        put("currentProgress", q.currentProgress)
                        put("isClaimed", q.isClaimed)
                    }
                )
            }
            repository.saveLongPlayRpgJson(currentUserId, "quests", arr.toString())
            val passObj = JSONObject().apply {
                put("totalPassXp", pass.totalPassXp)
                put("claimedTiers", JSONArray(pass.claimedMilestoneTiers.toList()))
            }
            repository.saveLongPlayRpgJson(currentUserId, "pass", passObj.toString())
        }
    }

    private fun saveRelicForgeToStorage(relicMap: Map<String, ForgedRelicUpgradeState>, runes: Set<String>) {
        runCatching {
            val arr = JSONArray()
            for ((_, st) in relicMap) {
                arr.put(
                    JSONObject().apply {
                        put("relicCode", st.relicCode)
                        put("upgradeLevel", st.upgradeLevel)
                        put("socketOneRuneCode", st.socketOneRuneCode ?: "")
                        put("socketTwoRuneCode", st.socketTwoRuneCode ?: "")
                    }
                )
            }
            repository.saveLongPlayRpgJson(currentUserId, "relic_forge", arr.toString())
            repository.saveLongPlayRpgJson(currentUserId, "runes", JSONArray(runes.toList()).toString())
        }
    }

    private fun loadLongPlayRpgStateFromStorage() {
        runCatching {
            repository.loadLongPlayRpgJson(currentUserId, "daycare")?.let { raw ->
                val obj = JSONObject(raw)
                _daycareState.value = DaycareBreedingState(
                    parentOneId = obj.optString("parentOneId"),
                    parentOneName = obj.optString("parentOneName"),
                    parentOneSpeciesId = obj.optString("parentOneSpeciesId"),
                    parentOneAttribute = obj.optString("parentOneAttribute"),
                    parentOneShiny = obj.optBoolean("parentOneShiny", false),
                    parentTwoId = obj.optString("parentTwoId"),
                    parentTwoName = obj.optString("parentTwoName"),
                    parentTwoSpeciesId = obj.optString("parentTwoSpeciesId"),
                    parentTwoAttribute = obj.optString("parentTwoAttribute"),
                    parentTwoShiny = obj.optBoolean("parentTwoShiny", false),
                    hasActiveEgg = obj.optBoolean("hasActiveEgg", false),
                    offspringSpeciesId = obj.optString("offspringSpeciesId", "patamon"),
                    offspringSpeciesName = obj.optString("offspringSpeciesName", "Sproutling"),
                    inheritedNatureCode = obj.optString("inheritedNatureCode", "ADM"),
                    inheritedBonusStats = obj.optInt("inheritedBonusStats", 18),
                    inheritedEggMove = obj.optString("inheritedEggMove", "Starlight Resonance"),
                    chromaticShinyOddsPercent = obj.optInt("chromaticShinyOddsPercent", 15),
                    currentIncubationPoints = obj.optInt("currentIncubationPoints", 0),
                    targetIncubationPoints = obj.optInt("targetIncubationPoints", 100),
                    totalEggsHatched = obj.optInt("totalEggsHatched", 0),
                    totalShinyHatched = obj.optInt("totalShinyHatched", 0)
                )
            }
            repository.loadLongPlayRpgJson(currentUserId, "frontier")?.let { raw ->
                val obj = JSONObject(raw)
                _frontierTowerState.value = FrontierTowerState(
                    currentFloor = obj.optInt("currentFloor", 1),
                    highestFloorReached = obj.optInt("highestFloorReached", 1),
                    currentWinStreak = obj.optInt("currentWinStreak", 0),
                    bestWinStreak = obj.optInt("bestWinStreak", 0),
                    battlePoints = obj.optInt("battlePoints", 25),
                    totalBossesDefeated = obj.optInt("totalBossesDefeated", 0),
                    shinyCharmUnlocked = obj.optBoolean("shinyCharmUnlocked", false)
                )
            }
            repository.loadLongPlayRpgJson(currentUserId, "quests")?.let { raw ->
                val arr = JSONArray(raw)
                val progressById = mutableMapOf<String, Pair<Int, Boolean>>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    progressById[o.optString("questId")] =
                        o.optInt("currentProgress", 0) to o.optBoolean("isClaimed", false)
                }
                _tamerQuests.value = LongPlayRpgCatalog.defaultTamerQuests().map { def ->
                    val saved = progressById[def.questId]
                    if (saved != null) def.copy(currentProgress = saved.first, isClaimed = saved.second) else def
                }
            }
            repository.loadLongPlayRpgJson(currentUserId, "pass")?.let { raw ->
                val obj = JSONObject(raw)
                val tiersArr = obj.optJSONArray("claimedTiers")
                val tiers = mutableSetOf<Int>()
                if (tiersArr != null) {
                    for (i in 0 until tiersArr.length()) tiers.add(tiersArr.optInt(i))
                }
                _tamerPassState.value = TamerPassState(
                    totalPassXp = obj.optInt("totalPassXp", 0),
                    claimedMilestoneTiers = tiers
                )
            }
            repository.loadLongPlayRpgJson(currentUserId, "relic_forge")?.let { raw ->
                val arr = JSONArray(raw)
                val map = mutableMapOf<String, ForgedRelicUpgradeState>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val code = o.optString("relicCode")
                    if (code.isNotBlank()) {
                        map[code] = ForgedRelicUpgradeState(
                            relicCode = code,
                            upgradeLevel = o.optInt("upgradeLevel", 0).coerceIn(0, 5),
                            socketOneRuneCode = o.optString("socketOneRuneCode").takeIf { it.isNotBlank() },
                            socketTwoRuneCode = o.optString("socketTwoRuneCode").takeIf { it.isNotBlank() }
                        )
                    }
                }
                _forgedRelics.value = map
            }
            repository.loadLongPlayRpgJson(currentUserId, "runes")?.let { raw ->
                val arr = JSONArray(raw)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    val code = arr.optString(i)
                    if (code.isNotBlank()) set.add(code)
                }
                if (set.isNotEmpty()) {
                    _unlockedRuneCodes.value = set
                }
            }
        }
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
