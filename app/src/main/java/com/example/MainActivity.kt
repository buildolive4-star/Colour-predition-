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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.example.data.ChromaPulseRepository
import com.example.domain.UiState
import com.example.ui.screens.GameArenaScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
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

const val ROUTE_HOME = "home"
const val ROUTE_ARENA = "arena"
const val ROUTE_HISTORY = "history"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_PROFILE = "profile"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopLevelDestinations = listOf(
    NavDestination(ROUTE_HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    NavDestination(ROUTE_ARENA, R.string.nav_game, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    NavDestination(ROUTE_HISTORY, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    NavDestination(ROUTE_LEADERBOARD, R.string.nav_leaderboard, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    NavDestination(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
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
                ChromaPulseApp()
            }
        }
    }
}

@Composable
fun ChromaPulseApp() {
    val viewModel: ChromaPulseViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                ChromaPulseViewModel(
                    repository = ChromaPulseRepository(app)
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
                if (!useNavRail) {
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
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
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
                if (useNavRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
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
                                activeRounds = activeRounds,
                                recentOutcomesByMode = recentOutcomes,
                                predictions = predictions,
                                unreadNotificationsCount = unreadCount,
                                isClaimingBonus = isClaimingBonus,
                                onOpenGameMode = { mode ->
                                    viewModel.selectMode(mode)
                                    navigateTopLevel(ROUTE_ARENA)
                                },
                                onOpenWallet = { showWalletSheet = true },
                                onOpenNotifications = { showNotificationsSheet = true },
                                onOpenProfile = { navigateTopLevel(ROUTE_PROFILE) },
                                onOpenHistory = { navigateTopLevel(ROUTE_HISTORY) },
                                onOpenLeaderboard = { navigateTopLevel(ROUTE_LEADERBOARD) },
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
                                currentUserId = viewModel.currentUserId
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                profile = profile,
                                predictions = predictions,
                                onOpenWallet = { showWalletSheet = true },
                                onUpdateSettings = { name, res, close, bonus ->
                                    viewModel.updateProfileSettings(name, res, close, bonus)
                                }
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
