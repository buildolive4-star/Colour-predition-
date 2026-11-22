package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PredictionRecord
import com.example.data.UserProfile
import com.example.domain.ActiveRoundSnapshot
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.RoundPhase
import com.example.domain.VerifiableRoundOutcome
import com.example.ui.components.HowToPlayDialog
import com.example.ui.components.RoundStatusChip
import com.example.ui.components.RoundVerificationDialog
import com.example.ui.components.VirtualCoinBalancePill
import com.example.ui.components.WinGoNumberBall
import com.example.ui.components.formatCoins
import com.example.ui.components.formatEpochMs
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeDeep
import com.example.ui.theme.AmberOrangeMid
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinGoldSoft
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.RoyalIndigoPill
import com.example.ui.theme.RoyalIndigoPillBright
import com.example.ui.theme.RubyRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun HomeScreen(
    profile: UserProfile?,
    activeRounds: Map<GameMode, ActiveRoundSnapshot>,
    recentOutcomesByMode: Map<GameMode, List<VerifiableRoundOutcome>>,
    predictions: List<PredictionRecord>,
    unreadNotificationsCount: Int,
    isClaimingBonus: Boolean,
    onOpenGameMode: (GameMode) -> Unit,
    onOpenWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onClaimDailyBonus: () -> Unit
) {
    val todayUtc = remember { ColorPredictionEngine.formatUtcIsoDate(System.currentTimeMillis()) }
    val bonusClaimedToday = profile?.lastDailyBonusDate == todayUtc
    val featuredRound = activeRounds[GameMode.MODE_30S]
    val recent30s = recentOutcomesByMode[GameMode.MODE_30S].orEmpty().take(12)
    var inspectedOutcome by remember { mutableStateOf<VerifiableRoundOutcome?>(null) }
    var showHowToPlay by remember { mutableStateOf(false) }

    val activePickedModes = remember(predictions, activeRounds) {
        GameMode.entries.filter { mode ->
            val currentPeriod = activeRounds[mode]?.periodId ?: return@filter false
            predictions.any { it.modeId == mode.id && it.periodId == currentPeriod }
        }.toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ShreeWin Style Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenProfile)
                        .padding(end = 4.dp)
                        .testTag("home_profile_chip")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_mark),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberGoldTop
                        )
                        Text(
                            text = "Hi, ${profile?.displayName?.split(" ")?.firstOrNull() ?: "Player"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VirtualCoinBalancePill(
                        coins = profile?.virtualCoins ?: ColorPredictionEngine.INITIAL_DEMO_COINS,
                        onClick = onOpenWallet
                    )
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(containerColor = RubyRed, contentColor = Color.White) {
                                        Text(
                                            text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = stringResource(R.string.cd_notifications),
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Signature ShreeWin Dual Top Action Pills (Warm Amber-Gold + Royal Violet-Indigo)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                        )
                        .clickable { onOpenGameMode(GameMode.MODE_30S) }
                        .testTag("home_quick_play_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = ObsidianCanvas,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Play WinGo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianCanvas
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(RoyalIndigoPill, RoyalIndigoPillBright))
                        )
                        .clickable(enabled = !isClaimingBonus) {
                            if (bonusClaimedToday) onOpenWallet() else onClaimDailyBonus()
                        }
                        .testTag("home_claim_daily_bonus_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (bonusClaimedToday) "Virtual Wallet" else "Get +500 VC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ShreeWin Style Announcement Bar with Amber Megaphone & Detail Pill
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = AmberGoldTop,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Virtual Coins Entertainment Arena — Pick Green, Violet, or Red with SHA-256 verifiable results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        onClick = { showHowToPlay = true },
                        shape = RoundedCornerShape(50),
                        color = RoyalIndigoPill,
                        modifier = Modifier.testTag("home_announcement_detail")
                    ) {
                        Text(
                            text = "Detail",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Featured Live Hero Banner (ShreeWin Gold-Framed Banner)
        item {
            FeaturedArenaHeroCard(
                round = featuredRound,
                onPlayNow = { onOpenGameMode(GameMode.MODE_30S) }
            )
        }

        // 5 Circular Category Badges Strip (Matching Screenshot #3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryBadgeItem(
                    label = "30s WinGo",
                    icon = Icons.Filled.Bolt,
                    gradient = listOf(AmberGoldTop, AmberOrangeDeep),
                    onClick = { onOpenGameMode(GameMode.MODE_30S) }
                )
                CategoryBadgeItem(
                    label = "1 Min",
                    icon = Icons.Filled.Timer,
                    gradient = listOf(NeonEmerald, Color(0xFF0A8A45)),
                    onClick = { onOpenGameMode(GameMode.MODE_60S) }
                )
                CategoryBadgeItem(
                    label = "3 Min",
                    icon = Icons.Filled.SportsEsports,
                    gradient = listOf(ElectricViolet, RoyalIndigoPill),
                    onClick = { onOpenGameMode(GameMode.MODE_3M) }
                )
                CategoryBadgeItem(
                    label = "5 Min",
                    icon = Icons.Filled.Star,
                    gradient = listOf(CyanPulse, Color(0xFF3B6BE8)),
                    onClick = { onOpenGameMode(GameMode.MODE_5M) }
                )
                CategoryBadgeItem(
                    label = "Rankings",
                    icon = Icons.Filled.EmojiEvents,
                    gradient = listOf(RubyRed, Color(0xFFD93654)),
                    onClick = onOpenLeaderboard
                )
            }
        }

        // Player Pulse Quick Stats Strip
        item {
            PlayerPulseStatsStrip(
                profile = profile,
                onOpenProfile = onOpenProfile
            )
        }

        // Section Header: WinGo Live Games (Matching Screenshot #3 "Lottery ... All")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AmberGoldTop)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = ObsidianCanvas,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "WinGo Color Arenas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                Surface(
                    onClick = { onOpenGameMode(GameMode.MODE_30S) },
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianCardElevated
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 2x2 Vibrant ShreeWin Gradient Game Mode Cards
        item {
            val modes = GameMode.entries
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShreeWinArenaCard(
                        mode = modes[0],
                        round = activeRounds[modes[0]],
                        hasActivePick = modes[0] in activePickedModes,
                        gradient = listOf(Color(0xFF9348F5), Color(0xFF5D2EC6)),
                        accent = AmberGoldTop,
                        sampleDigit = recentOutcomesByMode[modes[0]]?.firstOrNull()?.resultDigit ?: 5,
                        onClick = { onOpenGameMode(modes[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    ShreeWinArenaCard(
                        mode = modes[1],
                        round = activeRounds[modes[1]],
                        hasActivePick = modes[1] in activePickedModes,
                        gradient = listOf(Color(0xFF14A45B), Color(0xFF0A663B)),
                        accent = NeonEmeraldBright,
                        sampleDigit = recentOutcomesByMode[modes[1]]?.firstOrNull()?.resultDigit ?: 7,
                        onClick = { onOpenGameMode(modes[1]) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShreeWinArenaCard(
                        mode = modes[2],
                        round = activeRounds[modes[2]],
                        hasActivePick = modes[2] in activePickedModes,
                        gradient = listOf(Color(0xFFFF8627), Color(0xFFD94A1E)),
                        accent = CoinGoldSoft,
                        sampleDigit = recentOutcomesByMode[modes[2]]?.firstOrNull()?.resultDigit ?: 4,
                        onClick = { onOpenGameMode(modes[2]) },
                        modifier = Modifier.weight(1f)
                    )
                    ShreeWinArenaCard(
                        mode = modes[3],
                        round = activeRounds[modes[3]],
                        hasActivePick = modes[3] in activePickedModes,
                        gradient = listOf(Color(0xFF3978F6), Color(0xFF2342B5)),
                        accent = CyanPulse,
                        sampleDigit = recentOutcomesByMode[modes[3]]?.firstOrNull()?.resultDigit ?: 0,
                        onClick = { onOpenGameMode(modes[3]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent 30s Glossy Ball Results Strip
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Latest WinGo 30s Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap any ball to verify SHA-256 hash",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            onClick = onOpenHistory,
                            shape = RoundedCornerShape(50),
                            color = ObsidianCardElevated,
                            modifier = Modifier.testTag("home_view_all_history")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "History",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberGoldTop
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = AmberGoldTop,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recent30s, key = { it.roundDocId }) { outcome ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { inspectedOutcome = outcome }
                                    .padding(4.dp)
                            ) {
                                WinGoNumberBall(
                                    digit = outcome.resultDigit,
                                    size = 38.dp
                                )
                                Text(
                                    text = "#${outcome.periodId.takeLast(4)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
    }

    inspectedOutcome?.let { outcome ->
        RoundVerificationDialog(
            periodId = outcome.periodId,
            modeTitle = outcome.mode.title,
            winningColor = outcome.winningColor,
            resultDigit = outcome.resultDigit,
            resultHash = outcome.resultHash,
            completedTimeText = formatEpochMs(outcome.endTimeMs),
            onDismiss = { inspectedOutcome = null }
        )
    }
}

@Composable
private fun CategoryBadgeItem(
    label: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradient))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun FeaturedArenaHeroCard(
    round: ActiveRoundSnapshot?,
    onPlayNow: () -> Unit
) {
    Surface(
        onClick = onPlayNow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, AmberGoldTop.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("featured_arena_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_arena),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to ObsidianCanvas.copy(alpha = 0.92f),
                            0.6f to Color(0xFF1F1648).copy(alpha = 0.75f),
                            1.0f to ObsidianCanvas.copy(alpha = 0.35f)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AmberOrangeMid.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, AmberGoldTop)
                    ) {
                        Text(
                            text = "LIVE • WINGO 30SEC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGoldTop,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (round != null) {
                        RoundStatusChip(phase = round.phase)
                    }
                }

                Column {
                    Text(
                        text = round?.let { ColorPredictionEngine.formatCountdown(it.remainingMs) } ?: "00:30",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        color = if (round?.phase == RoundPhase.LOCKED) RubyRed else AmberGoldTop
                    )
                    Text(
                        text = "Period ${round?.periodId ?: "--"}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextPrimary.copy(alpha = 0.85f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Green 2x • Violet 4.5x • Red 2x",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoinGoldSoft
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeDeep)))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PLAY NOW",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerPulseStatsStrip(
    profile: UserProfile?,
    onOpenProfile: () -> Unit
) {
    val totalGames = profile?.totalGames ?: 0L
    val totalWins = profile?.totalWins ?: 0L
    val winRate = if (totalGames > 0L) ((totalWins * 100L) / totalGames).toInt() else 0
    val streak = profile?.currentStreak ?: 0L
    val points = profile?.totalVirtualPoints ?: 0L

    Surface(
        onClick = onOpenProfile,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_stats_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseMiniMetric(label = "WIN RATE", value = "$winRate%", color = NeonEmeraldBright)
            PulseMiniMetric(label = "STREAK", value = "${streak}x", color = AmberGoldTop)
            PulseMiniMetric(label = "POINTS", value = formatCoins(points), color = CoinGoldSoft)
            PulseMiniMetric(label = "GAMES", value = totalGames.toString(), color = ElectricVioletBright)
        }
    }
}

@Composable
private fun PulseMiniMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = color
        )
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun ShreeWinArenaCard(
    mode: GameMode,
    round: ActiveRoundSnapshot?,
    hasActivePick: Boolean,
    gradient: List<Color>,
    accent: Color,
    sampleDigit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = round?.phase == RoundPhase.LOCKED
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("mode_card_${mode.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = when {
                            hasActivePick -> "PICKED"
                            isLocked -> "LOCKED"
                            else -> "LIVE"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (hasActivePick) AmberGoldTop else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                WinGoNumberBall(digit = sampleDigit, size = 30.dp)
            }

            Column {
                Text(
                    text = "WinGo ${mode.shortLabel.uppercase(Locale.US)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = round?.let { ColorPredictionEngine.formatCountdown(it.remainingMs) } ?: "--:--",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = if (isLocked) Color(0xFFFFD6D6) else Color.White
                )
            }

            LinearProgressIndicator(
                progress = { 1f - (round?.progress ?: 0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = Color.Black.copy(alpha = 0.25f)
            )

            Text(
                text = "#${round?.periodId?.takeLast(5) ?: "-----"}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
