package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PredictionRecord
import com.example.data.UserProfile
import com.example.domain.ActiveRoundSnapshot
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.VerifiableRoundOutcome
import com.example.ui.components.ColorOrbBadge
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.HowToPlayDialog
import com.example.ui.components.RoundVerificationDialog
import com.example.ui.components.WinGoHistoryTable
import com.example.ui.components.WinGoModeSelectorBar
import com.example.ui.components.WinGoNumberBall
import com.example.ui.components.WinGoTicketCountdownCard
import com.example.ui.components.accentColor
import com.example.ui.components.brightColor
import com.example.ui.components.deepColor
import com.example.ui.components.digitToPredictionColor
import com.example.ui.components.formatCoins
import com.example.ui.components.formatEpochMs
import com.example.ui.components.formatTimestamp
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
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RubyRed
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RoundSettlementBanner
import kotlin.random.Random

private val MULTIPLIERS = listOf(1, 5, 10, 20, 50, 100)
private const val BASE_UNIT_COINS = 10L
private const val HISTORY_PAGE_SIZE = 10

@Composable
fun GameArenaScreen(
    selectedMode: GameMode,
    profile: UserProfile?,
    activeRounds: Map<GameMode, ActiveRoundSnapshot>,
    recentOutcomesByMode: Map<GameMode, List<VerifiableRoundOutcome>>,
    predictions: List<PredictionRecord>,
    latestBanner: RoundSettlementBanner?,
    isSubmitting: Boolean,
    isClaimingBonus: Boolean,
    onSelectMode: (GameMode) -> Unit,
    onSubmitPrediction: (GameMode, PredictionColor, Long) -> Unit,
    onClaimDailyBonus: () -> Unit,
    onClaimDemoRefill: () -> Unit,
    onDismissBanner: () -> Unit,
    onOpenWallet: () -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val activeRound = activeRounds[selectedMode]
    val modePredictions = remember(predictions, selectedMode) {
        predictions.filter { it.modeId == selectedMode.id }
    }
    val currentRoundPrediction = remember(modePredictions, activeRound?.periodId) {
        modePredictions.firstOrNull { it.periodId == activeRound?.periodId }
    }
    val recentOutcomes = recentOutcomesByMode[selectedMode].orEmpty()

    var selectedColor by remember { mutableStateOf(PredictionColor.GREEN) }
    var selectedBallDigit by remember { mutableStateOf<Int?>(null) }
    var stakeCoins by remember { mutableLongStateOf(100L) }
    var activeMultiplier by remember { mutableIntStateOf(10) }
    var bottomTab by remember { mutableIntStateOf(0) } // 0 = Game history, 1 = Chart, 2 = My history
    var historyPage by remember(selectedMode) { mutableIntStateOf(0) }
    var showHowToPlay by remember { mutableStateOf(false) }
    var inspectedOutcome by remember { mutableStateOf<VerifiableRoundOutcome?>(null) }

    val todayUtc = remember { ColorPredictionEngine.formatUtcIsoDate(System.currentTimeMillis()) }
    val bonusClaimedToday = profile?.lastDailyBonusDate == todayUtc
    val availableCoins = profile?.virtualCoins ?: 0L
    val isLockedOrDone = activeRound == null || !activeRound.canPredict
    val alreadyPredicted = currentRoundPrediction != null
    val canInteract = !isLockedOrDone && !alreadyPredicted && !isSubmitting
    val projectedReward = ColorPredictionEngine.calculateRewardCoins(selectedColor, selectedColor, stakeCoins)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top ShreeWin / WinGo Curved Amber Backdrop + Floating Navy Wallet Card + 4-Mode Clock Bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Dark Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("arena_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = AmberGoldTop,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onOpenWallet,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("arena_top_wallet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = stringResource(R.string.cd_wallet),
                            tint = AmberGoldTop
                        )
                    }
                }

                // Curved Sunset Amber Backdrop with Floating Navy Wallet Card & Mode Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Amber curved background header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(AmberGoldTop, AmberOrangeMid, AmberOrangeDeep)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Floating Dark Navy Wallet Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${formatCoins(availableCoins)} VC",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp,
                                    color = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = NeonEmeraldBright,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Virtual Coin Wallet • Demo Only",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Coral-Red Pill: Daily Bonus
                                    Button(
                                        onClick = onClaimDailyBonus,
                                        enabled = !bonusClaimedToday && !isClaimingBonus,
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RubyRed,
                                            contentColor = Color.White,
                                            disabledContainerColor = ObsidianCardElevated,
                                            disabledContentColor = TextSecondary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("arena_daily_bonus_pill")
                                    ) {
                                        Text(
                                            text = if (bonusClaimedToday) "Daily Claimed" else "Daily +500 VC",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Emerald-Green Pill: Wallet or Free Refill when low
                                    val needsRefill = availableCoins < ColorPredictionEngine.DEMO_REFILL_THRESHOLD
                                    Button(
                                        onClick = {
                                            if (needsRefill) onClaimDemoRefill() else onOpenWallet()
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonEmerald,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("arena_wallet_action_pill")
                                    ) {
                                        Text(
                                            text = if (needsRefill) "Refill +1,000 VC" else "Wallet Ledger",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 4-Column Circular Clock Mode Selector
                        WinGoModeSelectorBar(
                            selectedMode = selectedMode,
                            activeRounds = activeRounds,
                            onSelectMode = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectMode(it)
                            }
                        )
                    }
                }
            }
        }

        // Animated Result Settlement Banner
        item {
            AnimatedVisibility(
                visible = latestBanner != null && latestBanner.mode == selectedMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                latestBanner?.let { banner ->
                    ResultBannerCard(banner = banner, onDismiss = onDismissBanner)
                }
            }
        }

        // Iconic Amber-Orange Ticket Countdown Card
        item {
            WinGoTicketCountdownCard(
                mode = selectedMode,
                round = activeRound,
                recentOutcomes = recentOutcomes,
                onHowToPlay = { showHowToPlay = true },
                onInspectOutcome = { inspectedOutcome = it },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        // Green / Violet / Red Vibrant WinGo Color Buttons (Order matches Screenshot #1)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    PredictionColor.GREEN,
                    PredictionColor.VIOLET,
                    PredictionColor.RED
                ).forEach { color ->
                    WinGoColorChoiceButton(
                        color = color,
                        isSelected = selectedColor == color,
                        enabled = canInteract,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedColor = color
                            selectedBallDigit = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 0-9 Glossy 3D Number Balls Board (2 rows x 5 columns)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(0..4, 5..9).forEach { range ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            range.forEach { digit ->
                                val isPicked = selectedBallDigit == digit
                                WinGoNumberBall(
                                    digit = digit,
                                    size = 54.dp,
                                    isSelected = isPicked,
                                    onClick = if (canInteract) {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedBallDigit = digit
                                            selectedColor = digitToPredictionColor(digit)
                                        }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.testTag("wingo_ball_$digit")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Random + X1/X5/X10/X20/X50/X100 Multiplier Strip + Virtual Coin Entry CTA
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Random + X1..X100 row (Screenshot #1 & #2)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Surface(
                                onClick = {
                                    if (canInteract) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val randomDigit = Random.nextInt(0, 10)
                                        selectedBallDigit = randomDigit
                                        selectedColor = digitToPredictionColor(randomDigit)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianCanvas,
                                border = BorderStroke(1.dp, RubyRed.copy(alpha = 0.85f)),
                                modifier = Modifier.testTag("wingo_random_button")
                            ) {
                                Text(
                                    text = "Random",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = RubyRedBright,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }
                        items(MULTIPLIERS) { mult ->
                            val isSelected = activeMultiplier == mult && stakeCoins == mult * BASE_UNIT_COINS
                            Surface(
                                onClick = {
                                    if (canInteract) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeMultiplier = mult
                                        stakeCoins = (mult * BASE_UNIT_COINS)
                                            .coerceIn(ColorPredictionEngine.MIN_STAKE_COINS, ColorPredictionEngine.MAX_STAKE_COINS)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonEmerald else ObsidianCanvas,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonEmeraldBright else ObsidianBorder
                                ),
                                modifier = Modifier.testTag("stake_chip_${mult * BASE_UNIT_COINS}")
                            ) {
                                Text(
                                    text = "X$mult",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }

                    // Fine-tune +/- row + Projected Reward
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    stakeCoins = (stakeCoins - 50L).coerceAtLeast(ColorPredictionEngine.MIN_STAKE_COINS)
                                },
                                enabled = canInteract && stakeCoins > ColorPredictionEngine.MIN_STAKE_COINS,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCardElevated)
                                    .testTag("stake_decrease_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease virtual stake", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${formatCoins(stakeCoins)} VC",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CoinGoldSoft
                            )
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    stakeCoins = (stakeCoins + 50L).coerceAtMost(ColorPredictionEngine.MAX_STAKE_COINS)
                                },
                                enabled = canInteract,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCardElevated)
                                    .testTag("stake_increase_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase virtual stake", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Win Reward (${selectedColor.multiplierLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "+${formatCoins(projectedReward)} VC",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = selectedColor.brightColor()
                            )
                        }
                    }

                    // Lock Prediction Primary CTA
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmitPrediction(selectedMode, selectedColor, stakeCoins)
                        },
                        enabled = canInteract && availableCoins >= stakeCoins,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedColor.accentColor(),
                            contentColor = Color.White,
                            disabledContainerColor = ObsidianCardElevated,
                            disabledContentColor = TextSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("lock_prediction_button")
                    ) {
                        when {
                            isSubmitting -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            alreadyPredicted -> {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Locked ${currentRoundPrediction?.selectedColor} (${formatCoins(currentRoundPrediction?.stakeCoins ?: 0L)} VC)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            isLockedOrDone -> {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.round_locked_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            availableCoins < stakeCoins -> {
                                Text(
                                    text = "Insufficient Virtual Coins",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            else -> {
                                Text(
                                    text = "Lock ${selectedColor.displayName} • ${formatCoins(stakeCoins)} VC",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3-Tab History Bar (Game history | Chart | My history) matching Screenshot #2
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Game history", "Chart", "My history").forEachIndexed { idx, label ->
                    val isSelected = bottomTab == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                                } else {
                                    Brush.horizontalGradient(listOf(ObsidianCard, ObsidianCard))
                                }
                            )
                            .clickable { bottomTab = idx }
                            .testTag("arena_bottom_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // Tab Content
        when (bottomTab) {
            0 -> {
                val totalPages = ((recentOutcomes.size + HISTORY_PAGE_SIZE - 1) / HISTORY_PAGE_SIZE).coerceAtLeast(1)
                val safePage = historyPage.coerceIn(0, totalPages - 1)
                val pageItems = recentOutcomes.drop(safePage * HISTORY_PAGE_SIZE).take(HISTORY_PAGE_SIZE)

                item {
                    WinGoHistoryTable(
                        outcomes = pageItems,
                        onInspectOutcome = { inspectedOutcome = it },
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }

                // Pagination Bar matching Screenshot #2 (<   1/2   >)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { if (safePage > 0) historyPage = safePage - 1 },
                                enabled = safePage > 0,
                                shape = RoundedCornerShape(8.dp),
                                color = if (safePage > 0) AmberOrangeMid else ObsidianCardElevated,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous page",
                                        tint = if (safePage > 0) Color.White else TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "${safePage + 1}/$totalPages",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Surface(
                                onClick = { if (safePage < totalPages - 1) historyPage = safePage + 1 },
                                enabled = safePage < totalPages - 1,
                                shape = RoundedCornerShape(8.dp),
                                color = if (safePage < totalPages - 1) AmberOrangeMid else ObsidianCardElevated,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next page",
                                        tint = if (safePage < totalPages - 1) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    ColorTrendChartCard(
                        recentOutcomes = recentOutcomes,
                        onInspectOutcome = { inspectedOutcome = it },
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }

            else -> {
                if (modePredictions.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "No predictions in ${selectedMode.title} yet",
                            subtitle = "Pick Green, Violet, Red, or a numbered ball above to lock your virtual entry.",
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                } else {
                    items(modePredictions.take(15), key = { it.id.ifBlank { it.periodId } }) { pred ->
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            PredictionRowCard(prediction = pred)
                        }
                    }
                }
            }
        }
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
private fun WinGoColorChoiceButton(
    color: PredictionColor,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected && enabled) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "wingo_color_scale"
    )
    // Signature asymmetric WinGo corners matching Screenshot #1
    val shape = when (color) {
        PredictionColor.GREEN -> RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        PredictionColor.VIOLET -> RoundedCornerShape(10.dp)
        PredictionColor.RED -> RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }
    val brush = if (enabled) {
        Brush.verticalGradient(
            listOf(color.accentColor(), color.deepColor().copy(alpha = 0.92f))
        )
    } else {
        Brush.verticalGradient(listOf(ObsidianCardElevated, ObsidianCard))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(54.dp)
            .clip(shape)
            .background(brush)
            .border(
                width = if (isSelected && enabled) 2.5.dp else 1.dp,
                color = if (isSelected && enabled) AmberGoldTop else Color.White.copy(alpha = 0.18f),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("color_button_${color.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = color.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) Color.White else TextSecondary
            )
            Text(
                text = color.multiplierLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (enabled) Color.White.copy(alpha = 0.9f) else TextSecondary
            )
        }
    }
}

@Composable
private fun ColorTrendChartCard(
    recentOutcomes: List<VerifiableRoundOutcome>,
    onInspectOutcome: (VerifiableRoundOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = recentOutcomes.size.coerceAtLeast(1)
    val redCount = recentOutcomes.count { it.winningColor == PredictionColor.RED }
    val greenCount = recentOutcomes.count { it.winningColor == PredictionColor.GREEN }
    val violetCount = recentOutcomes.count { it.winningColor == PredictionColor.VIOLET }
    val redPct = (redCount * 100) / totalCount
    val greenPct = (greenCount * 100) / totalCount
    val violetPct = (violetCount * 100) / totalCount

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last ${recentOutcomes.size} Rounds Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = AmberGoldTop,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SHA-256",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = AmberGoldTop
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ObsidianCanvas),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (greenCount > 0) {
                    Box(modifier = Modifier.weight(greenCount.toFloat()).fillMaxSize().background(NeonEmerald))
                }
                if (violetCount > 0) {
                    Box(modifier = Modifier.weight(violetCount.toFloat()).fillMaxSize().background(ElectricViolet))
                }
                if (redCount > 0) {
                    Box(modifier = Modifier.weight(redCount.toFloat()).fillMaxSize().background(RubyRed))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GREEN $greenPct% ($greenCount)", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeonEmeraldBright)
                Text("VIOLET $violetPct% ($violetCount)", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ElectricVioletBright)
                Text("RED $redPct% ($redCount)", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RubyRedBright)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recentOutcomes.take(15), key = { it.roundDocId }) { outcome ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onInspectOutcome(outcome) }
                            .padding(4.dp)
                    ) {
                        WinGoNumberBall(digit = outcome.resultDigit, size = 36.dp)
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

@Composable
private fun ResultBannerCard(
    banner: RoundSettlementBanner,
    onDismiss: () -> Unit
) {
    val accent = when {
        banner.userPredicted && banner.didWin -> NeonEmerald
        banner.userPredicted && !banner.didWin -> RubyRed
        else -> AmberOrangeMid
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                WinGoNumberBall(digit = banner.resultDigit, size = 40.dp)
                Column {
                    Text(
                        text = when {
                            banner.userPredicted && banner.didWin -> "You Won +${formatCoins(banner.rewardCoins)} VC!"
                            banner.userPredicted -> "Round Settled • ${banner.winningColor.displayName}"
                            else -> "Result Declared • ${banner.winningColor.displayName}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Period ${banner.periodId} • Proof ${banner.resultHash.take(10)}...",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss result banner", tint = TextPrimary)
            }
        }
    }
}

@Composable
fun PredictionRowCard(prediction: PredictionRecord) {
    val selected = PredictionColor.fromRaw(prediction.selectedColor)
    val winning = prediction.winningColor?.let { PredictionColor.fromRaw(it) }
    val statusColor = when (prediction.status) {
        "WON" -> NeonEmeraldBright
        "LOST" -> RubyRedBright
        else -> CoinGoldSoft
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                ColorOrbBadge(color = selected, size = 34.dp)
                Column {
                    Text(
                        text = "${GameMode.fromId(prediction.modeId).shortLabel.uppercase()} • ${prediction.periodId}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = buildString {
                            append("Picked ${selected.displayName} (${formatCoins(prediction.stakeCoins)} VC)")
                            if (winning != null) append(" • Result: ${winning.displayName}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formatTimestamp(prediction.createdAtMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.75f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = prediction.status,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = statusColor
                )
                if (prediction.rewardCoins > 0L) {
                    Text(
                        text = "+${formatCoins(prediction.rewardCoins)} VC",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeonEmeraldBright
                    )
                }
            }
        }
    }
}
