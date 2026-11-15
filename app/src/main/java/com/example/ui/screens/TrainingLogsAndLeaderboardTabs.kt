package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PetNotification
import com.example.data.model.TradeListing
import com.example.data.model.TrainingLogEntry
import com.example.ui.components.DigimonHoloCrestAvatar
import com.example.ui.theme.BioEmeraldHealth
import com.example.ui.theme.CyberVoidBg
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCardElevated
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.HoloSlateSurface
import com.example.ui.theme.PlasmaVioletStamina
import com.example.ui.theme.SolarCoralHunger
import com.example.ui.theme.VirusCrimsonError
import com.example.util.PetNotificationHelper
import com.example.viewmodel.SparringBattleResult

@Composable
fun DailyTrainingLogsTab(
    logs: List<TrainingLogEntry>,
    creatures: List<DigimonCreature>,
    modifier: Modifier = Modifier
) {
    var selectedCreatureFilter by remember { mutableStateOf("ALL") }
    val filteredLogs = remember(logs, selectedCreatureFilter) {
        if (selectedCreatureFilter == "ALL") logs
        else logs.filter { it.creatureId == selectedCreatureFilter || it.creatureName == selectedCreatureFilter }
    }

    val totalXp = filteredLogs.sumOf { it.xpGained }
    val sRankCount = filteredLogs.count { it.grade == "S" || it.grade == "EVO" }
    val streakDays = logs.map { it.dateKey }.distinct().size.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DAILY TRAINING TELEMETRY LOGS",
            style = MaterialTheme.typography.headlineLarge,
            color = HoloCyanPrimary
        )

        // Daily KPI Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogKpiCard("SESSIONS", filteredLogs.size.toString(), HoloCyanPrimary, Modifier.weight(1f))
            LogKpiCard("TOTAL XP", "+$totalXp", EvolutionGoldSecondary, Modifier.weight(1f))
            LogKpiCard("S-RANK", sRankCount.toString(), BioEmeraldHealth, Modifier.weight(1f))
            LogKpiCard("STREAK", "${streakDays}d", PlasmaVioletStamina, Modifier.weight(1f))
        }

        // Creature Filter Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCreatureFilter == "ALL",
                onClick = { selectedCreatureFilter = "ALL" },
                label = { Text("All Partners") }
            )
            creatures.forEach { creature ->
                FilterChip(
                    selected = selectedCreatureFilter == creature.id,
                    onClick = { selectedCreatureFilter = creature.id },
                    label = { Text(creature.nickname) }
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HoloSlateSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = HoloCyanPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No Training Sessions Logged Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Complete an interactive drill, feed a Cyber Ration, or trigger Digivolution to record telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("training_logs_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id.ifBlank { it.hashCode().toString() } }) { entry ->
                    TrainingLogItemCard(entry)
                }
            }
        }
    }
}

@Composable
private fun LogKpiCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun TrainingLogItemCard(entry: TrainingLogEntry) {
    val badgeColor = when (entry.grade) {
        "S", "EVO" -> EvolutionGoldSecondary
        "A" -> HoloCyanPrimary
        "CARE" -> BioEmeraldHealth
        else -> PlasmaVioletStamina
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.grade,
                    style = MaterialTheme.typography.headlineMedium,
                    color = badgeColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${entry.activityType} • ${entry.creatureName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "+${entry.xpGained} XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = EvolutionGoldSecondary
                    )
                }
                Text(
                    text = entry.statDeltaSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HoloCyanPrimary
                )
                Text(
                    text = "Date: ${entry.dateKey}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SocialLeaderboardTab(
    leaderboard: List<LeaderboardEntry>,
    currentUserId: String,
    trainerName: String,
    tamerTitle: String,
    isGoogleSignedIn: Boolean = true,
    onRequestGoogleSignIn: () -> Unit = {},
    onAddFriend: (LeaderboardEntry) -> Unit = {},
    latestSparringResult: SparringBattleResult?,
    onChallengeTrainer: (LeaderboardEntry) -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onDismissSparringResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GLOBAL TAMER LEADERBOARD",
                    style = MaterialTheme.typography.headlineLarge,
                    color = EvolutionGoldSecondary
                )
                Text(
                    text = if (isGoogleSignedIn) {
                        "Your Callsign: $trainerName ($tamerTitle)"
                    } else {
                        "Local Mode ($trainerName) • Rankings Progression Locked"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isGoogleSignedIn) HoloCyanPrimary else VirusCrimsonError
                )
            }
            if (isGoogleSignedIn) {
                OutlinedButton(
                    onClick = { showProfileDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HoloCyanPrimary),
                    modifier = Modifier.testTag("edit_callsign_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Callsign",
                        tint = HoloCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Callsign", color = HoloCyanPrimary)
                }
            }
        }

        if (!isGoogleSignedIn) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rankings_google_locked_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                border = BorderStroke(1.5.dp, VirusCrimsonError.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RANKINGS PROGRESSION LOCKED (GOOGLE SIGN-IN REQUIRED)",
                        style = MaterialTheme.typography.labelLarge,
                        color = VirusCrimsonError
                    )
                    Text(
                        text = "You can play and evolve your monsters without logging into Google, but you cannot progress on global Rankings, record sparring wins, or use Friends & Chat until you sign in with Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Button(
                        onClick = onRequestGoogleSignIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HoloCyanPrimary,
                            contentColor = CyberVoidBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rankings_google_sign_in_button")
                    ) {
                        Text("SIGN IN WITH GOOGLE TO UNLOCK RANKINGS")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("leaderboard_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(leaderboard, key = { _, item -> item.userId }) { index, entry ->
                val rank = index + 1
                val isCurrentUser = isGoogleSignedIn && entry.userId == currentUserId
                val rankColor = when (rank) {
                    1 -> EvolutionGoldSecondary
                    2 -> HoloCyanPrimary
                    3 -> SolarCoralHunger
                    else -> PlasmaVioletStamina
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leaderboard_row_$rank"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentUser) HoloCardElevated else HoloSlateSurface
                    ),
                    border = BorderStroke(
                        width = if (isCurrentUser) 2.dp else 1.dp,
                        color = if (isCurrentUser) HoloCyanPrimary else rankColor.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(rankColor.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.headlineMedium,
                                color = rankColor
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isCurrentUser) "${entry.trainerName} (YOU)" else entry.trainerName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${entry.tamerTitle} • Partner: ${entry.partnerName} (${entry.partnerStage})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "CP ${entry.totalCombatPower} • ${entry.evolutionsUnlocked} Forms • ${entry.trainingStreakDays}d Streak • ${entry.sparringWins} Wins",
                                style = MaterialTheme.typography.labelMedium,
                                color = rankColor
                            )
                        }

                        if (!isCurrentUser) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (isGoogleSignedIn) {
                                            onChallengeTrainer(entry)
                                        } else {
                                            onRequestGoogleSignIn()
                                        }
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isGoogleSignedIn) {
                                            HoloCyanPrimary.copy(alpha = 0.2f)
                                        } else {
                                            VirusCrimsonError.copy(alpha = 0.2f)
                                        },
                                        contentColor = if (isGoogleSignedIn) HoloCyanPrimary else VirusCrimsonError
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("spar_button_$rank")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsKabaddi,
                                        contentDescription = "Spar",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isGoogleSignedIn) "SPAR" else "LOCKED",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                if (isGoogleSignedIn) {
                                    OutlinedButton(
                                        onClick = { onAddFriend(entry) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BioEmeraldHealth.copy(alpha = 0.7f)),
                                        modifier = Modifier.testTag("leaderboard_add_friend_$rank")
                                    ) {
                                        Text(
                                            text = "+ FRIEND",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BioEmeraldHealth
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        var nameInput by remember { mutableStateOf(trainerName) }
        var titleInput by remember { mutableStateOf(tamerTitle) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = HoloSlateSurface,
            title = {
                Text("Update Tamer Callsign", style = MaterialTheme.typography.headlineMedium, color = HoloCyanPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Tamer Callsign") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Tamer Title Badge") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, titleInput)
                        showProfileDialog = false
                    }
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (latestSparringResult != null) {
        AlertDialog(
            onDismissRequest = onDismissSparringResult,
            containerColor = HoloCardElevated,
            icon = {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (latestSparringResult.won) EvolutionGoldSecondary else HoloCyanPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (latestSparringResult.won) "ARENA SPARRING VICTORY!" else "TACTICAL ARENA DRAW!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (latestSparringResult.won) EvolutionGoldSecondary else HoloCyanPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = latestSparringResult.summaryText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Your Squad Resonance: ${latestSparringResult.playerCp} CP vs ${latestSparringResult.opponentName}: ${latestSparringResult.opponentCp} CP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rewards: +${latestSparringResult.evoSyncBonus}% Digivolution Sync & +${latestSparringResult.xpBonus} XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = BioEmeraldHealth
                    )
                }
            },
            confirmButton = {
                Button(onClick = onDismissSparringResult) {
                    Text("CONTINUE")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    notifications: List<PetNotification>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasSysPerm by remember {
        mutableStateOf(PetNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSysPerm = granted
        if (granted) {
            PetNotificationHelper.dispatchSystemAlert(
                context = context,
                notificationId = 777,
                title = "Digi Verse System Telemetry Active",
                message = "You will now receive real-time Digimon care and Digivolution alerts."
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HoloSlateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PET STATUS ALERT FEED",
                    style = MaterialTheme.typography.titleLarge,
                    color = HoloCyanPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onMarkAllRead,
                        enabled = notifications.any { !it.isRead },
                        modifier = Modifier.testTag("mark_all_notifications_read_button")
                    ) {
                        Text(
                            text = "Mark All Read",
                            color = if (notifications.any { !it.isRead }) EvolutionGoldSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onClearAll,
                        enabled = notifications.isNotEmpty(),
                        modifier = Modifier.testTag("clear_all_notifications_button")
                    ) {
                        Text(
                            text = "Clear All",
                            color = if (notifications.isNotEmpty()) VirusCrimsonError else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // System Notification Permission Card (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasSysPerm) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.dp, HoloCyanPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Status Bar Push Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Get Android notifications when your Digimon is hungry or ready to Digivolve.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
                            Text("ENABLE")
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                Text(
                    text = "All Digimon biometrics nominal. No status alerts in your feed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id.ifBlank { "${it.creatureId}_${it.title}_${it.message}" } }) { notif ->
                        val sevColor = when (notif.severity) {
                            "CRITICAL" -> VirusCrimsonError
                            "WARNING" -> SolarCoralHunger
                            "EVOLUTION" -> EvolutionGoldSecondary
                            else -> HoloCyanPrimary
                        }
                        val sevIcon = when (notif.severity) {
                            "CRITICAL", "WARNING" -> Icons.Default.Warning
                            "EVOLUTION" -> Icons.Default.Bolt
                            else -> Icons.Default.NotificationsActive
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!notif.isRead) {
                                        onMarkRead(notif.id.ifBlank { "${notif.creatureId}_${notif.title}_${notif.message}" })
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.isRead) HoloSlateSurface else HoloCardElevated
                            ),
                            border = BorderStroke(1.dp, sevColor.copy(alpha = if (notif.isRead) 0.25f else 0.75f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = sevIcon,
                                    contentDescription = notif.severity,
                                    tint = sevColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!notif.isRead) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Mark Read",
                                        tint = HoloCyanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GlobalTradeExchangeTab(
    currentUserId: String,
    myCreatures: List<DigimonCreature>,
    openTradeListings: List<TradeListing>,
    myTradeListings: List<TradeListing>,
    onPostCreatureForTrade: (String, String) -> Unit,
    onAcceptTradeListing: (TradeListing, String) -> Unit,
    onCancelMyTradeListing: (TradeListing) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateTradeDialog by remember { mutableStateOf(false) }
    var tradeToAccept by remember { mutableStateOf<TradeListing?>(null) }

    val otherPlayersOpenListings = remember(openTradeListings, currentUserId) {
        openTradeListings.filter { it.creatorUserId != currentUserId && it.status == "OPEN" }
    }
    val myOpenListings = remember(myTradeListings) {
        myTradeListings.filter { it.status == "OPEN" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("trade_exchange_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIVE PLAYER MONSTER TRADE",
                        style = MaterialTheme.typography.headlineLarge,
                        color = HoloCyanPrimary
                    )
                    Text(
                        text = "Trade raised monsters with real-life Tamers worldwide • Keep at least 1 partner in your squad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showCreateTradeDialog = true },
                    enabled = myCreatures.size > 1,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvolutionGoldSecondary,
                        contentColor = CyberVoidBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_post_trade_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Post Trade",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("POST TRADE", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (myCreatures.size <= 1) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
                    border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Tip: You currently have ${myCreatures.size} partner in your squad. Hatch a Core-Egg on the Holo-Deck so you have 2+ partners to post a monster on the Trade Exchange (or accept a 1-for-1 swap below anytime!).",
                        style = MaterialTheme.typography.bodySmall,
                        color = EvolutionGoldSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // My Active Trade Listings Section
        if (myOpenListings.isNotEmpty()) {
            item {
                Text(
                    text = "YOUR ACTIVE TRADE OFFERS (${myOpenListings.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = EvolutionGoldSecondary
                )
            }
            items(myOpenListings, key = { "my_${it.id}" }) { listing ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_trade_listing_${listing.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                    border = BorderStroke(1.2.dp, EvolutionGoldSecondary.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DigimonHoloCrestAvatar(
                                speciesId = listing.offeredSpeciesId,
                                stage = listing.offeredStage,
                                isResting = false,
                                evolutionReady = listing.offeredLevel >= 100,
                                sizeDp = 50.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${listing.offeredNickname} (${listing.offeredSpeciesName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "LVL ${listing.offeredLevel}/100 • ${listing.offeredStage} • CP ${listing.offeredCombatPower()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EvolutionGoldSecondary
                                )
                                Text(
                                    text = "Looking For: ${listing.requestedStageOrSpecies} • Waiting for another player...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HoloCyanPrimary
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onCancelMyTradeListing(listing) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VirusCrimsonError),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cancel_trade_${listing.id}")
                        ) {
                            Text("CANCEL & RETURN TO SQUAD", color = VirusCrimsonError, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Other Real Players' Open Listings Section
        item {
            Text(
                text = "OPEN REAL-PLAYER TRADE OFFERS (${otherPlayersOpenListings.size})",
                style = MaterialTheme.typography.labelLarge,
                color = HoloCyanPrimary
            )
        }

        if (otherPlayersOpenListings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
                    border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = HoloCyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Other Player Trades Open Right Now",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Tap POST TRADE above to list one of your monsters for other real-life players to swap with!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(otherPlayersOpenListings, key = { it.id }) { listing ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_trade_row_${listing.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
                    border = BorderStroke(1.2.dp, HoloCyanPrimary.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DigimonHoloCrestAvatar(
                                speciesId = listing.offeredSpeciesId,
                                stage = listing.offeredStage,
                                isResting = false,
                                evolutionReady = listing.offeredLevel >= 100,
                                sizeDp = 52.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${listing.offeredNickname} (${listing.offeredSpeciesName})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tamer: ${listing.creatorTrainerName} • LVL ${listing.offeredLevel}/100 • ${listing.offeredStage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EvolutionGoldSecondary
                                )
                                Text(
                                    text = "ATK ${listing.offeredAttack} • DEF ${listing.offeredDefense} • SPD ${listing.offeredSpeed} • WIS ${listing.offeredWisdom} (CP ${listing.offeredCombatPower()})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Requested in Return: ${listing.requestedStageOrSpecies}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HoloCyanPrimary
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { tradeToAccept = listing },
                            enabled = myCreatures.isNotEmpty(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BioEmeraldHealth.copy(alpha = 0.22f),
                                contentColor = BioEmeraldHealth
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("accept_trade_button_${listing.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Trade",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SWAP & TRADE NOW", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    // Dialog to Post a Monster for Trade
    if (showCreateTradeDialog && myCreatures.isNotEmpty()) {
        var selectedCreatureId by remember { mutableStateOf(myCreatures.first().id) }
        val requestOptions = listOf(
            "Any Stage",
            "Rookie",
            "Champion",
            "Ultimate",
            "Mega",
            "Mythic Lv.100"
        ) + DigimonCatalog.starterSpecs.map { it.speciesName } + DigimonCatalog.level100RareBranches.map { it.targetSpeciesName }
        var selectedRequest by remember { mutableStateOf("Any Stage") }

        AlertDialog(
            onDismissRequest = { showCreateTradeDialog = false },
            containerColor = HoloSlateSurface,
            title = {
                Text(
                    text = "Post Monster to Player Trade",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HoloCyanPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Select which of your monsters to offer:",
                        style = MaterialTheme.typography.labelLarge,
                        color = EvolutionGoldSecondary
                    )
                    myCreatures.forEach { creature ->
                        val isChosen = creature.id == selectedCreatureId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCreatureId = creature.id },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChosen) HoloCardElevated else CyberVoidBg
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) HoloCyanPrimary else HoloCyanPrimary.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${creature.nickname} (${creature.speciesName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "LVL ${creature.currentLevel()}/100 • ${creature.stage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EvolutionGoldSecondary
                                )
                            }
                        }
                    }

                    Text(
                        text = "2. Requested Stage or Species in return:",
                        style = MaterialTheme.typography.labelLarge,
                        color = EvolutionGoldSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        requestOptions.take(12).forEach { reqOption ->
                            FilterChip(
                                selected = selectedRequest == reqOption,
                                onClick = { selectedRequest = reqOption },
                                label = { Text(reqOption) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPostCreatureForTrade(selectedCreatureId, selectedRequest)
                        showCreateTradeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvolutionGoldSecondary,
                        contentColor = CyberVoidBg
                    ),
                    modifier = Modifier.testTag("confirm_post_trade_button")
                ) {
                    Text("POST LIVE OFFER")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTradeDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Dialog to Accept Another Player's Trade Offer
    val activeOffer = tradeToAccept
    if (activeOffer != null && myCreatures.isNotEmpty()) {
        val matchingCreatures = myCreatures.filter { c ->
            activeOffer.requestedStageOrSpecies.isBlank() ||
                activeOffer.requestedStageOrSpecies.equals("Any Stage", ignoreCase = true) ||
                c.stage.equals(activeOffer.requestedStageOrSpecies, ignoreCase = true) ||
                c.speciesName.equals(activeOffer.requestedStageOrSpecies, ignoreCase = true)
        }
        val selectableList = matchingCreatures.ifEmpty { myCreatures }
        var chosenMyCreatureId by remember(activeOffer.id) { mutableStateOf(selectableList.first().id) }

        AlertDialog(
            onDismissRequest = { tradeToAccept = null },
            containerColor = HoloSlateSurface,
            title = {
                Text(
                    text = "Swap with ${activeOffer.creatorTrainerName}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BioEmeraldHealth
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You will receive ${activeOffer.offeredNickname} (${activeOffer.offeredSpeciesName} • LVL ${activeOffer.offeredLevel}/100 • ${activeOffer.offeredStage}). Choose which monster from your squad to send:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    myCreatures.forEach { creature ->
                        val isChosen = creature.id == chosenMyCreatureId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenMyCreatureId = creature.id },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChosen) HoloCardElevated else CyberVoidBg
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) BioEmeraldHealth else HoloCyanPrimary.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${creature.nickname} (${creature.speciesName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "LVL ${creature.currentLevel()}/100 • ${creature.stage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EvolutionGoldSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAcceptTradeListing(activeOffer, chosenMyCreatureId)
                        tradeToAccept = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BioEmeraldHealth,
                        contentColor = CyberVoidBg
                    ),
                    modifier = Modifier.testTag("confirm_accept_trade_button")
                ) {
                    Text("COMPLETE TRADE SWAP")
                }
            },
            dismissButton = {
                TextButton(onClick = { tradeToAccept = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

