package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.GoogleAuthHelper
import com.example.data.ChromaPulseRepository
import com.example.domain.UiState
import com.example.ui.components.QrCenterScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GameArenaScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MiniGamesScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VirtualWalletSheet
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeMid
import com.example.ui.theme.ChromaPulseTheme
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ChromaPulseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

const val ROUTE_HOME = "home"
const val ROUTE_ARENA = "arena"
const val ROUTE_MINIGAMES = "minigames"
const val ROUTE_HISTORY = "history"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_PROFILE = "profile"
const val ROUTE_QR_CENTER = "qr_center"
const val ROUTE_ADMIN = "admin"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopLevelDestinations = listOf(
    NavDestination(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestination(ROUTE_ARENA, "WinGo", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    NavDestination(ROUTE_MINIGAMES, "Games", Icons.Filled.Casino, Icons.Outlined.Casino),
    NavDestination(ROUTE_HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
    NavDestination(ROUTE_PROFILE, "Account", Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            ChromaPulseTheme {
                ChromaPulseRoot()
            }
        }
    }
}

@Composable
fun ChromaPulseRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val signedInUser = currentUser
    if (signedInUser == null || signedInUser.uid.isBlank()) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = signedInUser.uid
        key(uid) {
            AuthenticatedChromaPulseApp(currentUserId = uid)
        }
    }
}

@Composable
private fun AuthenticatedChromaPulseApp(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ChromaPulseViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                ChromaPulseViewModel(
                    repository = ChromaPulseRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val predictionsState by viewModel.predictionsState.collectAsStateWithLifecycle()
    val walletState by viewModel.walletTransactionsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val roundsState by viewModel.gameRoundsState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfigState.collectAsStateWithLifecycle()
    val vouchers by viewModel.vouchersState.collectAsStateWithLifecycle()
    val adminRole by viewModel.adminRoleState.collectAsStateWithLifecycle()
    val adminAllUsers by viewModel.adminAllUsersState.collectAsStateWithLifecycle()
    val adminLogs by viewModel.adminLogsState.collectAsStateWithLifecycle()
    val coAdminInvites by viewModel.coAdminInvitesState.collectAsStateWithLifecycle()
    val coAdminGrants by viewModel.allCoAdminGrantsState.collectAsStateWithLifecycle()
    val lastCreatedPassword by viewModel.lastCreatedCoAdminPassword.collectAsStateWithLifecycle()
    val isVerifyingAdminPass by viewModel.isVerifyingAdminPass.collectAsStateWithLifecycle()

    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val activeRounds by viewModel.activeRounds.collectAsStateWithLifecycle()
    val recentOutcomes by viewModel.recentOutcomesByMode.collectAsStateWithLifecycle()
    val latestBanner by viewModel.latestBanner.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmittingPrediction.collectAsStateWithLifecycle()
    val isClaimingBonus by viewModel.isClaimingBonus.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val predictions = (predictionsState as? UiState.Success)?.data.orEmpty()
    val transactions = (walletState as? UiState.Success)?.data.orEmpty()
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val savedRounds = (roundsState as? UiState.Success)?.data.orEmpty()
    val leaderboardEntries = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val unreadCount = remember(notifications) { notifications.count { !it.read } }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val snackbarHostState = remember { SnackbarHostState() }
    var showWalletSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var selectedMiniGameTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isFullScreenSubRoute =
        currentRoute == ROUTE_ADMIN || currentRoute == ROUTE_QR_CENTER || currentRoute == ROUTE_LEADERBOARD

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianCanvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail && !isFullScreenSubRoute) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AmberGoldTop,
                                    selectedTextColor = AmberGoldTop,
                                    indicatorColor = AmberOrangeMid.copy(alpha = 0.22f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !isFullScreenSubRoute) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = AmberGoldTop,
                                    selectedTextColor = AmberGoldTop,
                                    indicatorColor = AmberOrangeMid.copy(alpha = 0.22f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                profile = profile,
                                announcementText = appConfig.announcementText,
                                activeRounds = activeRounds,
                                recentOutcomesByMode = recentOutcomes,
                                predictions = predictions,
                                unreadNotificationsCount = unreadCount,
                                isClaimingBonus = isClaimingBonus,
                                onOpenGameMode = { mode ->
                                    viewModel.selectMode(mode)
                                    navigateTopLevel(ROUTE_ARENA)
                                },
                                onOpenMiniGame = { tabIdx ->
                                    selectedMiniGameTab = tabIdx
                                    navigateTopLevel(ROUTE_MINIGAMES)
                                },
                                onOpenQrCenter = { navController.navigate(ROUTE_QR_CENTER) },
                                onOpenAdminPanel = { navController.navigate(ROUTE_ADMIN) },
                                onOpenWallet = { showWalletSheet = true },
                                onOpenNotifications = { showNotificationsSheet = true },
                                onOpenProfile = { navigateTopLevel(ROUTE_PROFILE) },
                                onOpenHistory = { navigateTopLevel(ROUTE_HISTORY) },
                                onOpenLeaderboard = { navController.navigate(ROUTE_LEADERBOARD) },
                                onClaimDailyBonus = { viewModel.claimDailyBonus() }
                            )
                        }

                        composable(ROUTE_ARENA) {
                            GameArenaScreen(
                                selectedMode = selectedMode,
                                profile = profile,
                                activeRounds = activeRounds,
                                recentOutcomesByMode = recentOutcomes,
                                predictions = predictions,
                                latestBanner = latestBanner,
                                isSubmitting = isSubmitting,
                                isClaimingBonus = isClaimingBonus,
                                onSelectMode = { viewModel.selectMode(it) },
                                onSubmitPrediction = { mode, color, stake ->
                                    viewModel.submitPrediction(mode, color, stake)
                                },
                                onClaimDailyBonus = { viewModel.claimDailyBonus() },
                                onClaimDemoRefill = { viewModel.claimDemoRefill() },
                                onDismissBanner = { viewModel.dismissBanner() },
                                onOpenWallet = { showWalletSheet = true },
                                onBack = { navigateTopLevel(ROUTE_HOME) }
                            )
                        }

                        composable(ROUTE_MINIGAMES) {
                            MiniGamesScreen(
                                initialGameTab = selectedMiniGameTab,
                                profile = profile,
                                onPlayMiniGameRound = { gameTitle, stake, reward, summary ->
                                    viewModel.playMiniGameRound(gameTitle, stake, reward, summary)
                                },
                                onOpenWallet = { showWalletSheet = true },
                                onBack = { navigateTopLevel(ROUTE_HOME) }
                            )
                        }

                        composable(ROUTE_HISTORY) {
                            HistoryScreen(
                                firestoreRounds = savedRounds,
                                recentOutcomesByMode = recentOutcomes,
                                predictions = predictions,
                                walletTransactions = transactions
                            )
                        }

                        composable(ROUTE_LEADERBOARD) {
                            LeaderboardScreen(
                                entries = leaderboardEntries,
                                currentUserId = viewModel.currentUserId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                profile = profile,
                                predictions = predictions,
                                onOpenWallet = { showWalletSheet = true },
                                onOpenQrCenter = { navController.navigate(ROUTE_QR_CENTER) },
                                onOpenAdminPanel = { navController.navigate(ROUTE_ADMIN) },
                                onOpenLeaderboard = { navController.navigate(ROUTE_LEADERBOARD) },
                                onUpdateSettings = { name, res, close, bonus ->
                                    viewModel.updateProfileSettings(name, res, close, bonus)
                                },
                                onSignOut = {
                                    GoogleAuthHelper.signOut(credentialManager, scope)
                                }
                            )
                        }

                        composable(ROUTE_QR_CENTER) {
                            QrCenterScreen(
                                profile = profile,
                                appConfig = appConfig,
                                vouchers = vouchers,
                                onRedeemVoucher = { code -> viewModel.redeemVoucherCode(code) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ADMIN) {
                            AdminPanelScreen(
                                currentUserProfile = profile,
                                adminRole = adminRole,
                                isVerifyingPassword = isVerifyingAdminPass,
                                appConfig = appConfig,
                                allUsers = adminAllUsers,
                                vouchers = vouchers,
                                adminLogs = adminLogs,
                                coAdminInvites = coAdminInvites,
                                coAdminGrants = coAdminGrants,
                                lastCreatedPassword = lastCreatedPassword,
                                totalPredictionsCount = predictions.size,
                                onUnlockWithPassword = { pass, callback ->
                                    viewModel.unlockWithCoAdminPassword(pass, callback)
                                },
                                onCreateCoAdminPassword = { label, pass ->
                                    viewModel.createCoAdminPassword(label, pass)
                                },
                                onClearLastCreatedPassword = { viewModel.clearLastCreatedPassword() },
                                onToggleCoAdminInvite = { hash, active, label ->
                                    viewModel.toggleCoAdminInvite(hash, active, label)
                                },
                                onDeleteCoAdminInvite = { hash, label ->
                                    viewModel.deleteCoAdminInvite(hash, label)
                                },
                                onRevokeCoAdminUser = { targetUid, email ->
                                    viewModel.revokeCoAdminUser(targetUid, email)
                                },
                                onSaveConfig = { updated, summary ->
                                    viewModel.adminSaveConfig(updated, summary)
                                },
                                onAdjustUserCoins = { targetUid, delta, reason ->
                                    viewModel.adminAdjustCoins(targetUid, delta, reason)
                                },
                                onCreateVoucher = { amount, label ->
                                    viewModel.adminCreateVoucher(amount, label)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        if (showWalletSheet) {
            VirtualWalletSheet(
                profile = profile,
                transactions = transactions,
                isClaimingBonus = isClaimingBonus,
                onClaimDailyBonus = { viewModel.claimDailyBonus() },
                onClaimDemoRefill = { viewModel.claimDemoRefill() },
                onDismiss = { showWalletSheet = false }
            )
        }

        if (showNotificationsSheet) {
            NotificationsSheet(
                notifications = notifications,
                onMarkRead = { viewModel.markNotificationRead(it) },
                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                onDelete = { viewModel.deleteNotification(it) },
                onDismiss = { showNotificationsSheet = false }
            )
        }
    }
}
