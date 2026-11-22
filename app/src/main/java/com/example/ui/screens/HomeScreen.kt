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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
    announcementText: String,
    activeRounds: Map<GameMode, ActiveRoundSnapshot>,
    recentOutcomesByMode: Map<GameMode, List<VerifiableRoundOutcome>>,
    predictions: List<PredictionRecord>,
    unreadNotificationsCount: Int,
    isClaimingBonus: Boolean,
    onOpenGameMode: (GameMode) -> Unit,
    onOpenMiniGame: (Int) -> Unit,
    onOpenQrCenter: () -> Unit,
    onOpenAdminPanel: () -> Unit,
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
        // ShreeWin Style Brand Header with Wallet, QR & Notification Icons
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
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    VirtualCoinBalancePill(
                        coins = profile?.virtualCoins ?: ColorPredictionEngine.INITIAL_DEMO_COINS,
                        onClick = onOpenWallet
                    )
                    IconButton(
                        onClick = onOpenQrCenter,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("home_qr_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = "Open QR Center",
                            tint = AmberGoldTop
                        )
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(40.dp)
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

        // Signature ShreeWin Dual Top Action Pills (QR & Bonus + Admin Panel)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                        )
                        .clickable(enabled = !isClaimingBonus) {
                            if (bonusClaimedToday) onOpenQrCenter() else onClaimDailyBonus()
                        }
                        .testTag("home_claim_daily_bonus_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (bonusClaimedToday) Icons.Filled.QrCode2 else Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = ObsidianCanvas,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (bonusClaimedToday) "QR Vouchers" else "Get +500 VC",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianCanvas
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(RoyalIndigoPill, RoyalIndigoPillBright))
                        )
                        .clickable(onClick = onOpenAdminPanel)
                        .testTag("home_admin_panel_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Admin Panel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ShreeWin Announcement Bar (Synced live with Admin Panel Broadcast!)
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
                        text = announcementText,
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

        // Featured Live Hero Banner
        item {
            FeaturedArenaHeroCard(
                round = featuredRound,
                onPlayNow = { onOpenGameMode(GameMode.MODE_30S) }
            )
        }

        // 5 Circular Category Badges Strip (Matching Screenshot #3: WinGo, Spin, Mines, Dice, QR)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryBadgeItem(
                    label = "WinGo",
                    icon = Icons.Filled.Bolt,
                    gradient = listOf(AmberGoldTop, AmberOrangeDeep),
                    onClick = { onOpenGameMode(GameMode.MODE_30S) }
                )
                CategoryBadgeItem(
                    label = "Spin",
                    icon = Icons.Filled.Refresh,
                    gradient = listOf(ElectricViolet, RoyalIndigoPill),
                    onClick = { onOpenMiniGame(0) }
                )
                CategoryBadgeItem(
                    label = "Mines",
                    icon = Icons.Filled.Diamond,
                    gradient = listOf(NeonEmerald, Color(0xFF0A8A45)),
                    onClick = { onOpenMiniGame(1) }
                )
                CategoryBadgeItem(
                    label = "Dice",
                    icon = Icons.Filled.Casino,
                    gradient = listOf(CyanPulse, Color(0xFF3B6BE8)),
                    onClick = { onOpenMiniGame(2) }
                )
                CategoryBadgeItem(
                    label = "QR Hub",
                    icon = Icons.Filled.QrCode2,
                    gradient = listOf(RubyRed, Color(0xFFD93654)),
                    onClick = onOpenQrCenter
                )
            }
        }

        // Player Pulse Quick Stats Strip
        item {
            PlayerPulseStatsStrip(
                profile = profile,
                onOpenProfile = onOpenLeaderboard
            )
        }

        // Section Header: WinGo Live Games
        item {
            LobbySectionHeader(
                title = "WinGo Color Arenas",
                actionLabel = "All",
                onAction = { onOpenGameMode(GameMode.MODE_30S) }
            )
        }

        // 2x2 Vibrant ShreeWin Gradient WinGo Cards
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

        // Section Header: Mini Games Arcade (Spin Wheel, Color Mines, Cyber Dice)
        item {
            LobbySectionHeader(
                title = "Recommended Mini Games",
                actionLabel = "Play",
                onAction = { onOpenMiniGame(0) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniGamePromoTile(
                    title = "Spin Wheel",
                    subtitle = "Win 1,000 VC",
                    icon = Icons.Filled.Refresh,
                    gradient = listOf(Color(0xFFEC4899), Color(0xFF7E22CE)),
                    onClick = { onOpenMiniGame(0) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_minigame_spin")
                )
                MiniGamePromoTile(
                    title = "Color Mines",
                    subtitle = "Up to 25x",
                    icon = Icons.Filled.Diamond,
                    gradient = listOf(Color(0xFF10B981), Color(0xFF047857)),
                    onClick = { onOpenMiniGame(1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_minigame_mines")
                )
                MiniGamePromoTile(
                    title = "Cyber Dice",
                    subtitle = "1–100 Fair",
                    icon = Icons.Filled.Casino,
                    gradient = listOf(Color(0xFF0EA5E9), Color(0xFF1D4ED8)),
                    onClick = { onOpenMiniGame(2) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_minigame_dice")
                )
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
private fun LobbySectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }

        Surface(
            onClick = onAction,
            shape = RoundedCornerShape(8.dp),
            color = ObsidianCardElevated
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun MiniGamePromoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CoinGoldSoft,
                maxLines = 1
            )
        }
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
                .height(172.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = "PLAY",
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = "Leaderboard", tint = AmberGoldTop, modifier = Modifier.size(16.dp))
                Text("Ranks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
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
