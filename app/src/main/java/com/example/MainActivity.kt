package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.DigiPulseRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.CommandDeckTab
import com.example.ui.screens.CyberFeedBottomSheet
import com.example.ui.screens.DailyTrainingLogsTab
import com.example.ui.screens.DrillsHubTab
import com.example.ui.screens.EvolutionMatrixBottomSheet
import com.example.ui.screens.GlobalTradeExchangeTab
import com.example.ui.screens.HatchDigiEggBottomSheet
import com.example.ui.screens.NotificationsBottomSheet
import com.example.ui.screens.PlayerChatAndFriendsTab
import com.example.ui.screens.SocialLeaderboardTab
import com.example.ui.screens.StarterTrioDraftBottomSheet
import com.example.ui.screens.StarterTrioDraftContent
import com.example.ui.screens.StoreHubTab
import com.example.ui.theme.CyberVoidBg
import com.example.ui.theme.DigiPulseTheme
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCardElevated
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.HoloSlateSurface
import com.example.ui.theme.VirusCrimsonError
import com.example.util.AppUpdateChecker
import com.example.util.GameUpdateCheckResult
import com.example.util.PetNotificationHelper
import com.example.viewmodel.DigiPulseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class DigiPulseTab {
    DECK,
    WORLD,
    DRILLS,
    STORE,
    TRADE,
    CHAT,
    LOGS,
    LEADERBOARD
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PetNotificationHelper.ensureChannelCreated(this)
        com.example.widget.DigiWidgetSyncManager.scheduleAutoRefresh(this)
        setContent {
            DigiPulseTheme {
                DigiPulseRootApp()
            }
        }
    }
}

@Composable
fun DigiPulseRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionPrefs = remember(context) {
        context.applicationContext.getSharedPreferences("digiverse_player_state", android.content.Context.MODE_PRIVATE)
    }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isLocalGuestMode by remember {
        mutableStateOf(sessionPrefs.getBoolean("is_local_guest_mode", false))
    }
    var updateCheckResult by remember { mutableStateOf<GameUpdateCheckResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingForUpdate by remember { mutableStateOf(false) }

    val triggerUpdateCheck: (Boolean) -> Unit = { openDialogEvenIfUpToDate ->
        if (!isCheckingForUpdate) {
            isCheckingForUpdate = true
            scope.launch {
                val lastAlertedVersion = sessionPrefs.getString("last_update_alert_version", "").orEmpty()
                val isFirstLaunchOfThisVersion = lastAlertedVersion != BuildConfig.VERSION_NAME
                val result = AppUpdateChecker.checkForUpdateOnLaunch(
                    isFirstLaunchOfThisVersion = isFirstLaunchOfThisVersion
                )
                updateCheckResult = result
                isCheckingForUpdate = false
                if (result.isUpdateAvailable) {
                    showUpdateDialog = true
                    PetNotificationHelper.dispatchSystemAlert(
                        context = context,
                        notificationId = 9901,
                        title = "Digi Verse Update Available (v${result.latestVersionName})!",
                        message = "A newer version (v${result.latestVersionName}) is available! Tap to download the latest APK."
                    )
                } else if (result.isNewVersionFirstLaunch) {
                    showUpdateDialog = true
                    sessionPrefs.edit()
                        .putString("last_update_alert_version", BuildConfig.VERSION_NAME)
                        .apply()
                    PetNotificationHelper.dispatchSystemAlert(
                        context = context,
                        notificationId = 9901,
                        title = "Digi Verse v${result.installedVersionName} Launch Update Ready!",
                        message = "Welcome to Digi Verse v${result.installedVersionName}! Launch Update Checker is active."
                    )
                } else if (openDialogEvenIfUpToDate) {
                    showUpdateDialog = true
                }
            }
        }
    }

    // Automatically check if there is a game update available when launching the game
    LaunchedEffect(Unit) {
        triggerUpdateCheck(false)
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isLocalGuestMode = false
                sessionPrefs.edit().putBoolean("is_local_guest_mode", false).apply()
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null && !isLocalGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isLocalGuestMode = false
                sessionPrefs.edit().putBoolean("is_local_guest_mode", false).apply()
            },
            onContinueWithoutLogin = {
                isLocalGuestMode = true
                sessionPrefs.edit().putBoolean("is_local_guest_mode", true).apply()
            }
        )
    } else {
        val isGoogleSignedIn = user != null
        val uid = user?.uid ?: "local_guest_tamer"
        val displayName = user?.displayName
            ?: user?.email?.substringBefore("@")
            ?: if (isGoogleSignedIn) "CyberTamer" else "LocalTamer"
        key(uid, isGoogleSignedIn) {
            AuthenticatedHoloDashboard(
                currentUserId = uid,
                initialTrainerName = displayName,
                isGoogleSignedIn = isGoogleSignedIn,
                updateCheckResult = updateCheckResult,
                onCheckForUpdateClick = {
                    if (updateCheckResult?.isUpdateAvailable == true) {
                        showUpdateDialog = true
                    } else {
                        triggerUpdateCheck(true)
                    }
                },
                onRequestGoogleSignIn = {
                    isLocalGuestMode = false
                    sessionPrefs.edit().putBoolean("is_local_guest_mode", false).apply()
                }
            )
        }
    }

    val activeUpdateInfo = updateCheckResult
    if (showUpdateDialog && activeUpdateInfo != null) {
        AlertDialog(
            onDismissRequest = {
                showUpdateDialog = false
                sessionPrefs.edit()
                    .putString("last_update_alert_version", BuildConfig.VERSION_NAME)
                    .apply()
            },
            containerColor = HoloCardElevated,
            modifier = Modifier.testTag("game_update_available_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdateAlt,
                    contentDescription = "Game Update Check",
                    tint = if (activeUpdateInfo.isUpdateAvailable || activeUpdateInfo.isNewVersionFirstLaunch) {
                        EvolutionGoldSecondary
                    } else {
                        HoloCyanPrimary
                    },
                    modifier = Modifier.size(42.dp)
                )
            },
            title = {
                Text(
                    text = when {
                        activeUpdateInfo.isUpdateAvailable ->
                            "UPDATE AVAILABLE: v${activeUpdateInfo.latestVersionName}!"
                        activeUpdateInfo.isNewVersionFirstLaunch ->
                            "GAME UPDATE ALERT (v${activeUpdateInfo.installedVersionName})"
                        else ->
                            "GAME UP TO DATE (v${activeUpdateInfo.installedVersionName})"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (activeUpdateInfo.isUpdateAvailable || activeUpdateInfo.isNewVersionFirstLaunch) {
                        EvolutionGoldSecondary
                    } else {
                        HoloCyanPrimary
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when {
                            activeUpdateInfo.isUpdateAvailable ->
                                "A newer version of Digi Verse is available! Update now to get the latest features, balance updates, and fixes."
                            activeUpdateInfo.isNewVersionFirstLaunch ->
                                "Launch Update Alert Active! You are running Digi Verse v${activeUpdateInfo.installedVersionName}. Whenever a newer update is published on GitHub, you will be alerted automatically on game launch."
                            else ->
                                "You are running Digi Verse v${activeUpdateInfo.installedVersionName}. Launch update checks run automatically every time you start the game."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Surface(
                        color = HoloSlateSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (activeUpdateInfo.isUpdateAvailable || activeUpdateInfo.isNewVersionFirstLaunch) {
                                EvolutionGoldSecondary
                            } else {
                                HoloCyanPrimary.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Installed Version: v${activeUpdateInfo.installedVersionName} • Latest Version: v${activeUpdateInfo.latestVersionName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (activeUpdateInfo.isUpdateAvailable) {
                                    EvolutionGoldSecondary
                                } else {
                                    com.example.ui.theme.BioEmeraldHealth
                                }
                            )
                            Text(
                                text = activeUpdateInfo.releaseNotes.take(280),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!activeUpdateInfo.isUpdateAvailable) {
                        OutlinedButton(
                            onClick = {
                                val nextMajor = (BuildConfig.VERSION_CODE + 1).toString() + ".0"
                                updateCheckResult = activeUpdateInfo.copy(
                                    latestVersionName = nextMajor,
                                    isUpdateAvailable = true,
                                    releaseNotes = "Simulated Update Alert (v$nextMajor): Tap DOWNLOAD UPDATE APK to open the GitHub Releases page and install the newest build."
                                )
                            },
                            border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_update_available_button")
                        ) {
                            Text(
                                text = "PREVIEW 'UPDATE AVAILABLE' ALERT BANNER & DIALOG",
                                style = MaterialTheme.typography.labelSmall,
                                color = EvolutionGoldSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        sessionPrefs.edit()
                            .putString("last_update_alert_version", BuildConfig.VERSION_NAME)
                            .apply()
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUpdateInfo.downloadUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeUpdateInfo.isUpdateAvailable) EvolutionGoldSecondary else HoloCyanPrimary,
                        contentColor = CyberVoidBg
                    ),
                    modifier = Modifier.testTag("download_game_update_button")
                ) {
                    Text(
                        text = if (activeUpdateInfo.isUpdateAvailable) "DOWNLOAD UPDATE APK" else "VIEW RELEASES"
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showUpdateDialog = false
                        sessionPrefs.edit()
                            .putString("last_update_alert_version", BuildConfig.VERSION_NAME)
                            .apply()
                    },
                    border = BorderStroke(1.dp, HoloCyanPrimary)
                ) {
                    Text(if (activeUpdateInfo.isUpdateAvailable) "LATER" else "CONTINUE", color = HoloCyanPrimary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedHoloDashboard(
    currentUserId: String,
    initialTrainerName: String,
    isGoogleSignedIn: Boolean = true,
    updateCheckResult: GameUpdateCheckResult? = null,
    onCheckForUpdateClick: () -> Unit = {},
    onRequestGoogleSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DigiPulseViewModel = viewModel(
        key = "${currentUserId}_$isGoogleSignedIn",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                DigiPulseViewModel(
                    repository = DigiPulseRepository(app),
                    currentUserId = currentUserId,
                    initialTrainerName = initialTrainerName,
                    isGoogleSignedIn = isGoogleSignedIn
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-refresh phone widgets on every state change AND every real-time telemetry tick
    LaunchedEffect(
        uiState.activeCreature,
        uiState.creatures,
        uiState.trainingLogs,
        uiState.nextStaminaTickSec,
        uiState.nextHungerTickSec
    ) {
        if (!uiState.isLoading) {
            if (uiState.activeCreature != null) {
                com.example.widget.DigiWidgetSyncManager.syncFromUiState(context, uiState)
            } else {
                com.example.widget.DigiWidgetSyncManager.clearAndRefreshWidgets(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val quickAction = activity?.intent?.getStringExtra(
            com.example.widget.DigiWidgetSyncManager.EXTRA_WIDGET_QUICK_ACTION
        )
        if (quickAction != null) {
            activity.intent?.removeExtra(com.example.widget.DigiWidgetSyncManager.EXTRA_WIDGET_QUICK_ACTION)
            com.example.widget.DigiWidgetSyncManager.applyOptimisticWidgetAction(context, quickAction)
            when (quickAction) {
                com.example.widget.DigiWidgetSyncManager.ACTION_QUICK_HEAL -> viewModel.healCreature()
                com.example.widget.DigiWidgetSyncManager.ACTION_TOGGLE_REST -> viewModel.toggleRestMode()
                com.example.widget.DigiWidgetSyncManager.ACTION_QUICK_FEED -> {
                    viewModel.feedCreature(com.example.data.model.DigiRation.FREE_CYBER_WAFER)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.systemAlertEvents.collect { alert ->
            PetNotificationHelper.dispatchSystemAlert(
                context = context,
                notificationId = alert.id,
                title = alert.title,
                message = alert.message
            )
        }
    }

    var currentTab by remember { mutableStateOf(DigiPulseTab.DECK) }
    var showFeedSheet by remember { mutableStateOf(false) }
    var showEvolutionSheet by remember { mutableStateOf(false) }
    var showWildAndExpeditionsSheet by remember { mutableStateOf(false) }
    var wildExpeditionsInitialTab by remember { mutableStateOf(0) }
    var showSkillTreeSheet by remember { mutableStateOf(false) }
    var showStarterDraftSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showDailyLoginDialog by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

    if (currentTab != DigiPulseTab.DECK) {
        BackHandler {
            currentTab = DigiPulseTab.DECK
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberVoidBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HoloSlateSurface,
                    titleContentColor = Color.White
                ),
                title = {
                    val coinMins = uiState.nextDigiCoinTickSec / 60
                    val coinSecs = uiState.nextDigiCoinTickSec % 60
                    val coinTimer = "%d:%02d".format(java.util.Locale.US, coinMins, coinSecs)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DIGI VERSE",
                                style = MaterialTheme.typography.headlineLarge,
                                color = HoloCyanPrimary,
                                maxLines = 1
                            )
                            Surface(
                                color = EvolutionGoldSecondary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = "${uiState.digiCoins} COINS (+5/$coinTimer)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvolutionGoldSecondary,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isGoogleSignedIn) {
                                "${uiState.trainerName} • Online Mode"
                            } else {
                                "Local Mode • Train (10 Coins) • Evolve LVL 20+"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCheckForUpdateClick,
                        modifier = Modifier.testTag("top_bar_check_update_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (updateCheckResult?.isUpdateAvailable == true) {
                                    Badge(
                                        containerColor = EvolutionGoldSecondary,
                                        contentColor = CyberVoidBg
                                    ) {
                                        Text("NEW")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdateAlt,
                                contentDescription = "Check for Game Update",
                                tint = if (updateCheckResult?.isUpdateAvailable == true) EvolutionGoldSecondary else HoloCyanPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDailyLoginDialog = true },
                        modifier = Modifier.testTag("daily_login_top_bar_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (!uiState.dailyLoginClaimedToday) {
                                    Badge(
                                        containerColor = EvolutionGoldSecondary,
                                        contentColor = CyberVoidBg
                                    ) {
                                        Text("!")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Daily Login Reward",
                                tint = if (uiState.dailyLoginClaimedToday) com.example.ui.theme.BioEmeraldHealth else EvolutionGoldSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showResetConfirmationDialog = true },
                        modifier = Modifier.testTag("top_bar_full_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Full Account Reset",
                            tint = VirusCrimsonError
                        )
                    }

                    IconButton(
                        onClick = { showNotificationsSheet = true },
                        modifier = Modifier.testTag("notifications_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = VirusCrimsonError,
                                        contentColor = Color.White
                                    ) {
                                        Text(uiState.unreadNotificationCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pet Status Notifications",
                                tint = HoloCyanPrimary
                            )
                        }
                    }

                    if (isGoogleSignedIn) {
                        IconButton(
                            onClick = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onRequestGoogleSignIn,
                            modifier = Modifier.testTag("top_bar_google_sign_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Sign In with Google",
                                tint = HoloCyanPrimary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            val isOnlineHubTab = currentTab in setOf(
                DigiPulseTab.TRADE,
                DigiPulseTab.CHAT,
                DigiPulseTab.LOGS,
                DigiPulseTab.LEADERBOARD
            )
            NavigationBar(
                containerColor = HoloSlateSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == DigiPulseTab.DECK,
                    onClick = { currentTab = DigiPulseTab.DECK },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_command_deck),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberVoidBg,
                        selectedTextColor = HoloCyanPrimary,
                        indicatorColor = HoloCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_deck")
                )
                NavigationBarItem(
                    selected = currentTab == DigiPulseTab.WORLD,
                    onClick = { currentTab = DigiPulseTab.WORLD },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = {
                        Text(
                            text = "World",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberVoidBg,
                        selectedTextColor = com.example.ui.theme.BioEmeraldHealth,
                        indicatorColor = com.example.ui.theme.BioEmeraldHealth
                    ),
                    modifier = Modifier.testTag("nav_tab_world")
                )
                NavigationBarItem(
                    selected = currentTab == DigiPulseTab.DRILLS,
                    onClick = { currentTab = DigiPulseTab.DRILLS },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_training_drills),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberVoidBg,
                        selectedTextColor = HoloCyanPrimary,
                        indicatorColor = HoloCyanPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_drills")
                )
                NavigationBarItem(
                    selected = currentTab == DigiPulseTab.STORE,
                    onClick = { currentTab = DigiPulseTab.STORE },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_store),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberVoidBg,
                        selectedTextColor = EvolutionGoldSecondary,
                        indicatorColor = EvolutionGoldSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_store")
                )
                NavigationBarItem(
                    selected = isOnlineHubTab,
                    onClick = {
                        if (!isOnlineHubTab) {
                            currentTab = DigiPulseTab.LEADERBOARD
                        }
                    },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = {
                        Text(
                            text = "Hub",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberVoidBg,
                        selectedTextColor = EvolutionGoldSecondary,
                        indicatorColor = EvolutionGoldSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_hub")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Streamlined Secondary Quick-Tab Bar for Social, Trade, Chat & Daily Logs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickHubTabs = listOf(
                    Triple(DigiPulseTab.LEADERBOARD, stringResource(R.string.nav_leaderboard), "nav_tab_leaderboard"),
                    Triple(DigiPulseTab.TRADE, stringResource(R.string.nav_player_trade), "nav_tab_trade"),
                    Triple(DigiPulseTab.CHAT, stringResource(R.string.nav_player_chat), "nav_tab_chat"),
                    Triple(DigiPulseTab.LOGS, stringResource(R.string.nav_daily_logs), "nav_tab_logs")
                )
                quickHubTabs.forEach { (tab, label, tag) ->
                    val selected = currentTab == tab
                    OutlinedButton(
                        onClick = { currentTab = tab },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) EvolutionGoldSecondary else HoloCyanPrimary.copy(alpha = 0.35f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) EvolutionGoldSecondary.copy(alpha = 0.2f) else HoloSlateSurface,
                            contentColor = if (selected) EvolutionGoldSecondary else Color.White
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tag)
                    ) {
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
            // Launch Update Available Alert Banner
            AnimatedVisibility(visible = updateCheckResult?.isUpdateAvailable == true) {
                Surface(
                    color = EvolutionGoldSecondary.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, EvolutionGoldSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onCheckForUpdateClick() }
                        .testTag("launch_update_available_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdateAlt,
                                contentDescription = "Update Available",
                                tint = EvolutionGoldSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UPDATE AVAILABLE (v${updateCheckResult?.latestVersionName.orEmpty()})! Tap to download latest APK.",
                                style = MaterialTheme.typography.labelLarge,
                                color = EvolutionGoldSecondary
                            )
                        }
                        Text(
                            text = "UPDATE ➔",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            // Live Status Telemetry Toast Banner
            AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                Surface(
                    color = HoloCardElevated,
                    border = BorderStroke(1.dp, HoloCyanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EvolutionGoldSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.statusBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Button(
                            onClick = { viewModel.dismissBanner() }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.needsStarterDraft && uiState.creatures.isEmpty()) {
                    // First-time or Post-Reset player 3-Starter Trio Selection Screen
                    StarterTrioDraftContent(
                        onConfirmDraft = viewModel::draftStarterTrio,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                } else {
                    when (currentTab) {
                        DigiPulseTab.DECK -> CommandDeckTab(
                            uiState = uiState,
                            onSelectCreature = viewModel::selectCreature,
                            onOpenWildTamingModal = {
                                wildExpeditionsInitialTab = 0
                                currentTab = DigiPulseTab.WORLD
                            },
                            onOpenExpeditionsModal = {
                                wildExpeditionsInitialTab = 1
                                currentTab = DigiPulseTab.WORLD
                            },
                            onOpenSkillTreeModal = { showSkillTreeSheet = true },
                            onOpenStarterDraftModal = { showStarterDraftSheet = true },
                            onRequestFullReset = { showResetConfirmationDialog = true },
                            onHealClicked = viewModel::healCreature,
                            onToggleRest = viewModel::toggleRestMode,
                            onOpenFeedSheet = { showFeedSheet = true },
                            onOpenEvolutionMatrix = { showEvolutionSheet = true },
                            onTriggerEvolution = viewModel::evolveCreature,
                            onUnlockSkillNode = viewModel::unlockSkillNode,
                            onOpenDrillsModal = { currentTab = DigiPulseTab.DRILLS }
                        )
                        DigiPulseTab.WORLD -> com.example.ui.screens.WildAndExpeditionsTabContent(
                            activeCreature = uiState.activeCreature,
                            squadCreatures = uiState.creatures,
                            digiCoins = uiState.digiCoins,
                            tammingBerryCount = uiState.tammingBerryCount,
                            standardMonsterPartsCount = uiState.standardMonsterPartsCount,
                            ultraRareMonsterPartsCount = uiState.ultraRareMonsterPartsCount,
                            wildEncounter = uiState.wildEncounter,
                            pendingTamedCreature = uiState.pendingTamedCreature,
                            initialTabIndex = wildExpeditionsInitialTab,
                            onScanWildMonster = viewModel::scanWildMonster,
                            onThrowTammingBerry = viewModel::throwTammingBerry,
                            onBattleWildMonster = viewModel::battleWildMonster,
                            onSellStandardPart = viewModel::sellStandardMonsterParts,
                            onSellUltraRarePart = viewModel::sellUltraRareMonsterParts,
                            onSellAllParts = viewModel::sellAllMonsterParts,
                            onReplaceSquadMonster = viewModel::replaceSquadMonsterWithTamed,
                            onDismissPendingTamed = viewModel::dismissPendingTamedCreature,
                            onStartExpedition = viewModel::startExpedition,
                            onCompleteOrRecallExpedition = viewModel::completeOrRecallExpedition,
                            onOpenFeedSheet = { showFeedSheet = true },
                            onGoToStoreTab = { currentTab = DigiPulseTab.STORE },
                            onTriggerCryoRest = viewModel::toggleRestMode
                        )
                        DigiPulseTab.DRILLS -> DrillsHubTab(
                            activeCreature = uiState.activeCreature,
                            digiCoins = uiState.digiCoins,
                            nextDigiCoinTickSec = uiState.nextDigiCoinTickSec,
                            onCompleteDrill = { drill, score ->
                                viewModel.completeTrainingDrill(drill, score)
                            },
                            onStart24hOfflineTraining = viewModel::start24hOfflineTraining,
                            onTogglePauseOfflineTraining = viewModel::togglePauseOfflineTraining
                        )
                        DigiPulseTab.STORE -> StoreHubTab(
                            activeCreature = uiState.activeCreature,
                            digiCoins = uiState.digiCoins,
                            nextDigiCoinTickSec = uiState.nextDigiCoinTickSec,
                            foodInventory = uiState.foodInventory,
                            tammingBerryCount = uiState.tammingBerryCount,
                            standardMonsterPartsCount = uiState.standardMonsterPartsCount,
                            ultraRareMonsterPartsCount = uiState.ultraRareMonsterPartsCount,
                            dailyLoginClaimedToday = uiState.dailyLoginClaimedToday,
                            dailyLoginStreakDays = uiState.dailyLoginStreakDays,
                            lastDailyLoginRewardText = uiState.lastDailyLoginRewardText,
                            onClaimDailyLoginReward = viewModel::claimDailyLoginReward,
                            onBuyFood = viewModel::buyFoodInStore,
                            onBuyTammingBerries = viewModel::buyTammingBerriesInStore,
                            onSellStandardPart = viewModel::sellStandardMonsterParts,
                            onSellUltraRarePart = viewModel::sellUltraRareMonsterParts,
                            onSellAllParts = viewModel::sellAllMonsterParts,
                            onFeedCreatureFromBag = viewModel::feedCreature,
                            onOpenWildTamingModal = {
                                wildExpeditionsInitialTab = 0
                                currentTab = DigiPulseTab.WORLD
                            }
                        )
                        DigiPulseTab.TRADE -> GlobalTradeExchangeTab(
                            currentUserId = currentUserId,
                            myCreatures = uiState.creatures,
                            openTradeListings = uiState.openTradeListings,
                            myTradeListings = uiState.myTradeListings,
                            onPostCreatureForTrade = viewModel::postCreatureForTrade,
                            onAcceptTradeListing = viewModel::acceptPlayerTrade,
                            onCancelMyTradeListing = viewModel::cancelMyTradeListing
                        )
                        DigiPulseTab.CHAT -> PlayerChatAndFriendsTab(
                            state = uiState,
                            currentUserId = currentUserId,
                            onRequestGoogleSignIn = onRequestGoogleSignIn,
                            onSendChatMessage = { text -> viewModel.sendChatMessage(text) },
                            onSelectDirectChatFriend = viewModel::selectDirectChatFriend,
                            onSendFriendRequest = viewModel::sendFriendRequest,
                            onAcceptFriendRequest = viewModel::acceptFriendRequest,
                            onRemoveFriend = viewModel::removeFriend
                        )
                        DigiPulseTab.LOGS -> DailyTrainingLogsTab(
                            logs = uiState.trainingLogs,
                            creatures = uiState.creatures
                        )
                        DigiPulseTab.LEADERBOARD -> SocialLeaderboardTab(
                            leaderboard = uiState.leaderboard,
                            currentUserId = currentUserId,
                            trainerName = uiState.trainerName,
                            tamerTitle = uiState.tamerTitle,
                            isGoogleSignedIn = uiState.isGoogleSignedIn,
                            onRequestGoogleSignIn = onRequestGoogleSignIn,
                            onAddFriend = { entry ->
                                viewModel.sendFriendRequest(
                                    targetUserId = entry.userId,
                                    targetTrainerName = entry.trainerName,
                                    targetPartnerName = entry.partnerName
                                )
                            },
                            latestSparringResult = uiState.latestSparringResult,
                            onChallengeTrainer = viewModel::runSparringMatch,
                            onUpdateProfile = viewModel::updateTrainerProfile,
                            onDismissSparringResult = viewModel::dismissSparringResult
                        )
                    }
                }
            }
        }
    }

    val activeCreature = uiState.activeCreature
    if (showFeedSheet && activeCreature != null) {
        CyberFeedBottomSheet(
            creature = activeCreature,
            foodInventory = uiState.foodInventory,
            onSelectRation = viewModel::feedCreature,
            onGoToStoreTab = { currentTab = DigiPulseTab.STORE },
            onDismiss = { showFeedSheet = false }
        )
    }

    if (showEvolutionSheet && activeCreature != null) {
        EvolutionMatrixBottomSheet(
            creature = activeCreature,
            trainingLogs = uiState.trainingLogs,
            onTriggerEvolution = viewModel::evolveCreature,
            onDismiss = { showEvolutionSheet = false }
        )
    }

    if (showWildAndExpeditionsSheet) {
        com.example.ui.screens.WildAndExpeditionsBottomSheet(
            activeCreature = activeCreature,
            squadCreatures = uiState.creatures,
            digiCoins = uiState.digiCoins,
            tammingBerryCount = uiState.tammingBerryCount,
            standardMonsterPartsCount = uiState.standardMonsterPartsCount,
            ultraRareMonsterPartsCount = uiState.ultraRareMonsterPartsCount,
            wildEncounter = uiState.wildEncounter,
            pendingTamedCreature = uiState.pendingTamedCreature,
            initialTabIndex = wildExpeditionsInitialTab,
            onScanWildMonster = viewModel::scanWildMonster,
            onThrowTammingBerry = viewModel::throwTammingBerry,
            onBattleWildMonster = viewModel::battleWildMonster,
            onSellStandardPart = viewModel::sellStandardMonsterParts,
            onSellUltraRarePart = viewModel::sellUltraRareMonsterParts,
            onSellAllParts = viewModel::sellAllMonsterParts,
            onReplaceSquadMonster = viewModel::replaceSquadMonsterWithTamed,
            onDismissPendingTamed = viewModel::dismissPendingTamedCreature,
            onStartExpedition = viewModel::startExpedition,
            onCompleteOrRecallExpedition = viewModel::completeOrRecallExpedition,
            onOpenFeedSheet = { showFeedSheet = true },
            onGoToStoreTab = { currentTab = DigiPulseTab.STORE },
            onTriggerCryoRest = viewModel::toggleRestMode,
            onDismiss = { showWildAndExpeditionsSheet = false }
        )
    }

    if (showSkillTreeSheet && activeCreature != null) {
        com.example.ui.screens.MonsterSkillTreeBottomSheet(
            creature = activeCreature,
            onUnlockNode = viewModel::unlockSkillNode,
            onDismiss = { showSkillTreeSheet = false }
        )
    }

    if (showStarterDraftSheet) {
        StarterTrioDraftBottomSheet(
            onConfirmDraft = { choices ->
                viewModel.draftStarterTrio(choices)
                showStarterDraftSheet = false
            },
            onDismiss = { showStarterDraftSheet = false }
        )
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            notifications = uiState.notifications,
            onMarkRead = viewModel::markNotificationRead,
            onMarkAllRead = viewModel::markAllNotificationsRead,
            onClearAll = viewModel::clearAllNotifications,
            onDismiss = { showNotificationsSheet = false }
        )
    }

    if (showDailyLoginDialog) {
        AlertDialog(
            onDismissRequest = { showDailyLoginDialog = false },
            containerColor = HoloCardElevated,
            modifier = Modifier.testTag("daily_login_reward_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Daily Login Reward",
                    tint = EvolutionGoldSecondary,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "DAILY LOGIN REWARD // DAY ${uiState.dailyLoginStreakDays}/7",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EvolutionGoldSecondary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Log in every day to build your 7-day retention streak and receive either a small amount of DigiCoins (10–25 DigiCoins) or a random Berry (1–2x Tamming Berries)!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    if (uiState.dailyLoginClaimedToday && !uiState.lastDailyLoginRewardText.isNullOrBlank()) {
                        Surface(
                            color = com.example.ui.theme.BioEmeraldHealth.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, com.example.ui.theme.BioEmeraldHealth)
                        ) {
                            Text(
                                text = "✓ ${uiState.lastDailyLoginRewardText}",
                                style = MaterialTheme.typography.labelLarge,
                                color = com.example.ui.theme.BioEmeraldHealth,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = EvolutionGoldSecondary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EvolutionGoldSecondary)
                        ) {
                            Text(
                                text = "Today's Mystery Drop: 10–25 DigiCoins OR 1–2x Random Tamming Berries!",
                                style = MaterialTheme.typography.labelLarge,
                                color = EvolutionGoldSecondary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.claimDailyLoginReward()
                    },
                    enabled = !uiState.dailyLoginClaimedToday,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvolutionGoldSecondary,
                        contentColor = CyberVoidBg
                    ),
                    modifier = Modifier.testTag("claim_daily_login_reward_button")
                ) {
                    Text(
                        text = if (uiState.dailyLoginClaimedToday) "CLAIMED TODAY" else "CLAIM DAILY REWARD"
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDailyLoginDialog = false },
                    border = BorderStroke(1.dp, HoloCyanPrimary)
                ) {
                    Text("CLOSE", color = HoloCyanPrimary)
                }
            }
        )
    }

    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            containerColor = HoloCardElevated,
            modifier = Modifier.testTag("full_reset_confirmation_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Reset Warning",
                    tint = VirusCrimsonError,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "ARE YOU SURE YOU WANT TO RESET?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VirusCrimsonError
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to start a New Game? This will permanently wipe your raised Digimon, evolution progress, daily training logs, notifications, and global leaderboard stats, returning you to the 1-Starter Rookie selection screen with 200 DigiCoins (New Game only). This action cannot be undone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmationDialog = false
                        currentTab = DigiPulseTab.DECK
                        com.example.widget.DigiWidgetSyncManager.clearAndRefreshWidgets(context)
                        viewModel.resetAllProgress()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VirusCrimsonError,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_full_reset_button")
                ) {
                    Text("YES, RESET EVERYTHING")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmationDialog = false },
                    border = BorderStroke(1.dp, HoloCyanPrimary),
                    modifier = Modifier.testTag("cancel_full_reset_button")
                ) {
                    Text("CANCEL", color = HoloCyanPrimary)
                }
            }
        )
    }

    if (uiState.recentEvolutionUnlocked != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEvolutionOverlay,
            containerColor = HoloCardElevated,
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EvolutionGoldSecondary,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "HOLO-DIGIVOLUTION COMPLETE!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EvolutionGoldSecondary
                )
            },
            text = {
                Text(
                    text = "Matrix Evolution Resonance achieved: ${uiState.recentEvolutionUnlocked}! All core stats and combat power have surged.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(onClick = viewModel::dismissEvolutionOverlay) {
                    Text("AWESOME!")
                }
            }
        )
    }
}
