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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.components.ColorOrbBadge
import com.example.ui.components.RoundStatusChip
import com.example.ui.components.RoundVerificationDialog
import com.example.ui.components.VirtualCoinBalancePill
import com.example.ui.components.VirtualDisclaimerBanner
import com.example.ui.components.formatCoins
import com.example.ui.components.formatEpochMs
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
    onClaimDailyBonus: () -> Unit
) {
    val todayUtc = remember { ColorPredictionEngine.formatUtcIsoDate(System.currentTimeMillis()) }
    val bonusClaimedToday = profile?.lastDailyBonusDate == todayUtc
    val featuredRound = activeRounds[GameMode.MODE_30S]
    val recent30s = recentOutcomesByMode[GameMode.MODE_30S].orEmpty().take(12)
    var inspectedOutcome by remember { mutableStateOf<VerifiableRoundOutcome?>(null) }

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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Bar
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
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
                            .size(48.dp)
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

        // Virtual Coins Disclaimer
        item {
            VirtualDisclaimerBanner()
        }

        // Featured Live Game Hero Card
        item {
            FeaturedArenaHeroCard(
                round = featuredRound,
                onPlayNow = { onOpenGameMode(GameMode.MODE_30S) }
            )
        }

        // Player Pulse Quick Stats Strip
        item {
            PlayerPulseStatsStrip(
                profile = profile,
                onOpenProfile = onOpenProfile
            )
        }

        // Daily Login Bonus Banner
        item {
            DailyBonusCard(
                claimedToday = bonusClaimedToday,
                isLoading = isClaimingBonus,
                onClaim = onClaimDailyBonus
            )
        }

        // Game Modes Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live Prediction Arenas",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "4 independent real-time synchronized timers",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2x2 Grid of all 4 Game Modes
        item {
            val modes = GameMode.entries
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameModeLiveCard(
                        mode = modes[0],
                        round = activeRounds[modes[0]],
                        hasActivePick = modes[0] in activePickedModes,
                        accent = RubyRed,
                        onClick = { onOpenGameMode(modes[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    GameModeLiveCard(
                        mode = modes[1],
                        round = activeRounds[modes[1]],
                        hasActivePick = modes[1] in activePickedModes,
                        accent = NeonEmerald,
                        onClick = { onOpenGameMode(modes[1]) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameModeLiveCard(
                        mode = modes[2],
                        round = activeRounds[modes[2]],
                        hasActivePick = modes[2] in activePickedModes,
                        accent = ElectricViolet,
                        onClick = { onOpenGameMode(modes[2]) },
                        modifier = Modifier.weight(1f)
                    )
                    GameModeLiveCard(
                        mode = modes[3],
                        round = activeRounds[modes[3]],
                        hasActivePick = modes[3] in activePickedModes,
                        accent = CyanPulse,
                        onClick = { onOpenGameMode(modes[3]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Results Strip
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recent Turbo 30s Results",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap any orb to verify SHA-256 hash",
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
                                    color = ElectricVioletBright
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = ElectricVioletBright,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recent30s, key = { it.roundDocId }) { outcome ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { inspectedOutcome = outcome }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                ColorOrbBadge(
                                    color = outcome.winningColor,
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

        item { Spacer(modifier = Modifier.height(12.dp)) }
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
private fun FeaturedArenaHeroCard(
    round: ActiveRoundSnapshot?,
    onPlayNow: () -> Unit
) {
    Surface(
        onClick = onPlayNow,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("featured_arena_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(198.dp)
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
                            0.65f to ObsidianCanvas.copy(alpha = 0.72f),
                            1.0f to ObsidianCanvas.copy(alpha = 0.45f)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ElectricViolet.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, ElectricVioletBright.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "FEATURED • TURBO 30S",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricVioletBright,
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        color = if (round?.phase == RoundPhase.LOCKED) RubyRed else TextPrimary
                    )
                    Text(
                        text = "Period ${round?.periodId ?: "--"}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Red 2x • Green 2x • Violet 4.5x",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoinGoldSoft
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricViolet
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Enter",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBonusCard(
    claimedToday: Boolean,
    isLoading: Boolean,
    onClaim: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(
            1.dp,
            if (claimedToday) ObsidianBorder else CoinGold.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CoinGold.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = CoinGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Daily Virtual Bonus",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (claimedToday) "Claimed for today (UTC)" else "+500 virtual demo coins ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClaim,
                enabled = !claimedToday && !isLoading,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoinGold,
                    contentColor = ObsidianCanvas,
                    disabledContainerColor = ObsidianCardElevated,
                    disabledContentColor = TextSecondary
                ),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("home_claim_daily_bonus_button")
            ) {
                Text(
                    text = if (claimedToday) "Claimed" else "+500 VC",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
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
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_stats_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseMiniMetric(label = "WIN RATE", value = "$winRate%", color = NeonEmeraldBright)
            PulseMiniMetric(label = "STREAK", value = "${streak}x", color = CyanPulse)
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
private fun GameModeLiveCard(
    mode: GameMode,
    round: ActiveRoundSnapshot?,
    hasActivePick: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = round?.phase == RoundPhase.LOCKED
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCard,
        border = BorderStroke(
            1.dp,
            when {
                hasActivePick -> CoinGold.copy(alpha = 0.75f)
                isLocked -> RubyRed.copy(alpha = 0.55f)
                else -> accent.copy(alpha = 0.35f)
            }
        ),
        modifier = modifier.testTag("mode_card_${mode.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = mode.shortLabel.uppercase(Locale.US),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = accent
                        )
                    }
                }
                Text(
                    text = when {
                        hasActivePick -> "PICKED"
                        isLocked -> "LOCKED"
                        else -> "LIVE"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = when {
                        hasActivePick -> CoinGoldSoft
                        isLocked -> RubyRed
                        else -> NeonEmerald
                    }
                )
            }

            Column {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = round?.let { ColorPredictionEngine.formatCountdown(it.remainingMs) } ?: "--:--",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = if (isLocked) RubyRed else TextPrimary
                )
            }

            LinearProgressIndicator(
                progress = { 1f - (round?.progress ?: 0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isLocked) RubyRed else accent,
                trackColor = ObsidianCanvas
            )

            Text(
                text = "Period #${round?.periodId?.takeLast(5) ?: "-----"}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
