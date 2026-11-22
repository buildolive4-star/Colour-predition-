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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PredictionRecord
import com.example.data.UserProfile
import com.example.domain.ActiveRoundSnapshot
import com.example.domain.BetType
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.SizeChoice
import com.example.domain.VerifiableRoundOutcome
import com.example.domain.WinGoBetTarget
import com.example.ui.components.ColorOrbBadge
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.HowToPlayDialog
import com.example.ui.components.RoundVerificationDialog
import com.example.ui.components.WinCelebrationDialog
import com.example.ui.components.WinGoHistoryTable
import com.example.ui.components.WinGoModeSelectorBar
import com.example.ui.components.WinGoNumberBall
import com.example.ui.components.WinGoTicketCountdownCard
import com.example.ui.components.WinPopupData
import com.example.ui.components.accentColor
import com.example.ui.components.brightColor
import com.example.ui.components.deepColor
import com.example.ui.components.digitBigSmallLabel
import com.example.ui.components.digitToPredictionColor
import com.example.ui.components.formatCoins
import com.example.ui.components.formatEpochMs
import com.example.ui.components.formatTimestamp
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
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RubyRed
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RoundSettlementBanner
import kotlin.math.roundToLong
import kotlin.random.Random

private val MULTIPLIERS = listOf(1, 5, 10, 20, 50, 100)
private const val BASE_UNIT_COINS = 10L
private const val HISTORY_PAGE_SIZE = 10
private val SMALL_BLUE = Color(0xFF5FA5FA)
private val SMALL_BLUE_DEEP = Color(0xFF3B82F6)

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
    onSubmitPrediction: (GameMode, WinGoBetTarget, Long) -> Unit,
    onOpenDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onDismissBanner: () -> Unit,
    onOpenWallet: () -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val activeRound = activeRounds[selectedMode]
    val modePredictions = remember(predictions, selectedMode) {
        predictions.filter { it.modeId == selectedMode.id }
    }
    // Unlimited multiple bets per round!
    val currentRoundPredictions = remember(modePredictions, activeRound?.periodId) {
        modePredictions.filter { it.periodId == activeRound?.periodId }
    }
    val recentOutcomes = recentOutcomesByMode[selectedMode].orEmpty()

    var currentTarget by remember {
        mutableStateOf(WinGoBetTarget(BetType.COLOR, PredictionColor.GREEN))
    }
    var activeBetSheetTarget by remember { mutableStateOf<WinGoBetTarget?>(null) }
    var stakeCoins by remember { mutableLongStateOf(100L) }
    var activeMultiplier by remember { mutableIntStateOf(10) }
    var bottomTab by remember { mutableIntStateOf(0) }
    var historyPage by remember(selectedMode) { mutableIntStateOf(0) }
    var showHowToPlay by remember { mutableStateOf(false) }
    var inspectedOutcome by remember { mutableStateOf<VerifiableRoundOutcome?>(null) }
    var winPopupData by remember { mutableStateOf<WinPopupData?>(null) }
    var lastCelebratedPeriod by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(latestBanner) {
        if (latestBanner != null && latestBanner.userPredicted && latestBanner.didWin && latestBanner.rewardCoins > 0L) {
            val key = "${latestBanner.mode.id}_${latestBanner.periodId}_${latestBanner.rewardCoins}"
            if (lastCelebratedPeriod != key) {
                lastCelebratedPeriod = key
                val sizeLbl = digitBigSmallLabel(latestBanner.resultDigit)
                winPopupData = WinPopupData(
                    title = "WinGo ${latestBanner.mode.shortLabel.uppercase()} Win!",
                    amountWon = latestBanner.rewardCoins,
                    subtitle = "Period ${latestBanner.periodId} • Result #${latestBanner.resultDigit} (${latestBanner.winningColor.displayName} • $sizeLbl)",
                    badgeText = "JACKPOT WINNER!"
                )
            }
        }
    }

    val availableCoins = profile?.virtualCoins ?: 0L
    val isLockedOrDone = activeRound == null || !activeRound.canPredict
    val canInteract = !isLockedOrDone && !isSubmitting
    val projectedReward = (stakeCoins.toDouble() * currentTarget.multiplier()).roundToLong()

    fun openBetFor(target: WinGoBetTarget) {
        if (!canInteract) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        currentTarget = target
        activeBetSheetTarget = target
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Curved Amber Backdrop + Floating Navy Wallet Card (Withdraw left | Deposit right) + 4-Mode Clock Bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
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

                Box(modifier = Modifier.fillMaxWidth()) {
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
                        // Floating Dark Navy Wallet Card: Left = Withdraw, Right = Deposit
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
                                        text = "Wallet balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Left Coral-Red Pill: Withdraw
                                    Button(
                                        onClick = onOpenWithdraw,
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RubyRed,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("arena_withdraw_pill")
                                    ) {
                                        Text(
                                            text = "Withdraw",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Right Emerald-Green Pill: Deposit
                                    Button(
                                        onClick = onOpenDeposit,
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonEmerald,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("arena_deposit_pill")
                                    ) {
                                        Text(
                                            text = "Deposit",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

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

        // Green / Violet / Red Color Buttons — Tap opens instant bet popup!
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
                        isSelected = currentTarget.betType == BetType.COLOR && currentTarget.color == color,
                        enabled = canInteract,
                        onClick = {
                            openBetFor(WinGoBetTarget(BetType.COLOR, color))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 0-9 Glossy 3D Number Balls Board (9.0x exact match!) — Tap ANY number opens instant bet popup!
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap Any Number to Bet",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Exact Number = 9.0x",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoinGoldSoft
                        )
                    }
                    listOf(0..4, 5..9).forEach { range ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            range.forEach { digit ->
                                val isPicked = currentTarget.betType == BetType.NUMBER && currentTarget.digit == digit
                                WinGoNumberBall(
                                    digit = digit,
                                    size = 54.dp,
                                    isSelected = isPicked,
                                    onClick = if (canInteract) {
                                        {
                                            openBetFor(
                                                WinGoBetTarget(
                                                    betType = BetType.NUMBER,
                                                    color = digitToPredictionColor(digit),
                                                    digit = digit
                                                )
                                            )
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

        // Random + X1..X100 + Iconic BIG / SMALL Split Pill + Quick Place Bet CTA
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
                    // Random + X1..X100 Multiplier Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Surface(
                                onClick = {
                                    if (canInteract) {
                                        val randomDigit = Random.nextInt(0, 10)
                                        openBetFor(
                                            WinGoBetTarget(
                                                betType = BetType.NUMBER,
                                                color = digitToPredictionColor(randomDigit),
                                                digit = randomDigit
                                            )
                                        )
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

                    // BIG (5-9) | SMALL (0-4) Signature Split Pill Bar — Tap to bet Big or Small!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(50))
                    ) {
                        val isBigSelected = currentTarget.betType == BetType.SIZE && currentTarget.sizeChoice == SizeChoice.BIG
                        val isSmallSelected = currentTarget.betType == BetType.SIZE && currentTarget.sizeChoice == SizeChoice.SMALL

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(
                                    if (canInteract) Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                                    else Brush.horizontalGradient(listOf(ObsidianCardElevated, ObsidianCardElevated))
                                )
                                .border(
                                    width = if (isBigSelected && canInteract) 2.5.dp else 0.dp,
                                    color = if (isBigSelected && canInteract) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                                )
                                .clickable(enabled = canInteract) {
                                    openBetFor(
                                        WinGoBetTarget(
                                            betType = BetType.SIZE,
                                            color = PredictionColor.GREEN,
                                            sizeChoice = SizeChoice.BIG
                                        )
                                    )
                                }
                                .testTag("wingo_big_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Big",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (canInteract) Color.White else TextSecondary
                                )
                                Text(
                                    text = "5–9 • 2.0x",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canInteract) Color.White.copy(alpha = 0.9f) else TextSecondary
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(
                                    if (canInteract) Brush.horizontalGradient(listOf(SMALL_BLUE, SMALL_BLUE_DEEP))
                                    else Brush.horizontalGradient(listOf(ObsidianCardElevated, ObsidianCardElevated))
                                )
                                .border(
                                    width = if (isSmallSelected && canInteract) 2.5.dp else 0.dp,
                                    color = if (isSmallSelected && canInteract) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                                )
                                .clickable(enabled = canInteract) {
                                    openBetFor(
                                        WinGoBetTarget(
                                            betType = BetType.SIZE,
                                            color = PredictionColor.RED,
                                            sizeChoice = SizeChoice.SMALL
                                        )
                                    )
                                }
                                .testTag("wingo_small_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Small",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (canInteract) Color.White else TextSecondary
                                )
                                Text(
                                    text = "0–4 • 2.0x",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canInteract) Color.White.copy(alpha = 0.9f) else TextSecondary
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
                                    stakeCoins = (stakeCoins - 10L).coerceAtLeast(ColorPredictionEngine.MIN_STAKE_COINS)
                                },
                                enabled = canInteract && stakeCoins > ColorPredictionEngine.MIN_STAKE_COINS,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCardElevated)
                                    .testTag("stake_decrease_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease stake", tint = TextPrimary, modifier = Modifier.size(16.dp))
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
                                    stakeCoins = (stakeCoins + 10L).coerceAtMost(ColorPredictionEngine.MAX_STAKE_COINS)
                                },
                                enabled = canInteract,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCardElevated)
                                    .testTag("stake_increase_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase stake", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${currentTarget.label} (${currentTarget.multiplierLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Win +${formatCoins(projectedReward)} VC",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NeonEmeraldBright
                            )
                        }
                    }

                    // Quick 1-Tap Place Bet CTA (Supports Unlimited Multiple Bets!)
                    val ctaColor = when (currentTarget.betType) {
                        BetType.SIZE -> if (currentTarget.sizeChoice == SizeChoice.BIG) AmberOrangeMid else SMALL_BLUE_DEEP
                        else -> currentTarget.color.accentColor()
                    }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmitPrediction(selectedMode, currentTarget, stakeCoins)
                        },
                        enabled = canInteract && availableCoins >= stakeCoins,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ctaColor,
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
                                    text = "Insufficient Coins • Tap Deposit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            else -> {
                                Text(
                                    text = "Place Bet: ${currentTarget.label} • ${formatCoins(stakeCoins)} VC",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Multiple Active Bets Tracker for Current Round
        if (currentRoundPredictions.isNotEmpty()) {
            item {
                val totalRoundStake = currentRoundPredictions.sumOf { it.stakeCoins }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonEmeraldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Bet Successful • ${currentRoundPredictions.size} Active Bet${if (currentRoundPredictions.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmeraldBright
                                )
                            }
                            Text(
                                text = "Total ${formatCoins(totalRoundStake)} VC",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoinGoldSoft
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentRoundPredictions, key = { it.id.ifBlank { "${it.periodId}_${it.createdAtMs}" } }) { bet ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ObsidianCanvas,
                                    border = BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Text(
                                        text = "${bet.displayPickLabel} • ${formatCoins(bet.stakeCoins)} VC",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3-Tab History Bar (Game history | Chart | My history)
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
                            title = "No bets in ${selectedMode.title} yet",
                            subtitle = "Tap Green, Violet, Red, any Number (0–9), or Big/Small above to place bets.",
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                } else {
                    items(modePredictions.take(25), key = { it.id.ifBlank { "${it.periodId}_${it.createdAtMs}" } }) { pred ->
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            PredictionRowCard(prediction = pred)
                        }
                    }
                }
            }
        }
    }

    // Instant Tap-to-Bet Modal Sheet (Opens when user clicks Green/Violet/Red, 0-9 Ball, or Big/Small!)
    activeBetSheetTarget?.let { target ->
        WinGoQuickBetSheet(
            mode = selectedMode,
            periodId = activeRound?.periodId ?: "--",
            target = target,
            initialStake = stakeCoins,
            availableCoins = availableCoins,
            isSubmitting = isSubmitting,
            onPlaceBet = { chosenAmount ->
                stakeCoins = chosenAmount
                onSubmitPrediction(selectedMode, target, chosenAmount)
                activeBetSheetTarget = null
            },
            onDismiss = { activeBetSheetTarget = null }
        )
    }

    winPopupData?.let { data ->
        WinCelebrationDialog(
            data = data,
            onDismiss = { winPopupData = null }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WinGoQuickBetSheet(
    mode: GameMode,
    periodId: String,
    target: WinGoBetTarget,
    initialStake: Long,
    availableCoins: Long,
    isSubmitting: Boolean,
    onPlaceBet: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var unitAmount by remember { mutableLongStateOf(10L) }
    var quantity by remember {
        mutableIntStateOf((initialStake / 10L).toInt().coerceIn(1, 100))
    }

    val totalBet = (unitAmount * quantity)
        .coerceIn(ColorPredictionEngine.MIN_STAKE_COINS, ColorPredictionEngine.MAX_STAKE_COINS)
    val projectedWin = (totalBet.toDouble() * target.multiplier()).roundToLong()

    val headerGradient = when (target.betType) {
        BetType.SIZE -> if (target.sizeChoice == SizeChoice.BIG) {
            listOf(AmberGoldTop, AmberOrangeDeep)
        } else {
            listOf(SMALL_BLUE, SMALL_BLUE_DEEP)
        }
        else -> listOf(target.color.accentColor(), target.color.deepColor())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Colored Bet Target Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(headerGradient))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${mode.title} • #$periodId",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Text(
                        text = "Select: ${target.label} (${target.multiplierLabel})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Base Balance Unit Row (10, 50, 100, 500, 1000)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bet Amount", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10L, 50L, 100L, 500L, 1000L).forEach { unit ->
                        val active = unitAmount == unit
                        Surface(
                            onClick = { unitAmount = unit },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) NeonEmerald else ObsidianCard,
                            border = BorderStroke(1.dp, if (active) NeonEmeraldBright else ObsidianBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$unit",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (active) Color.White else TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            // Quantity Stepper + X1..X100 Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantity", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { quantity = (quantity - 1).coerceAtLeast(1) },
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianCardElevated,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = "$quantity",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = CoinGoldSoft,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        onClick = { quantity = (quantity + 1).coerceAtMost(100) },
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianCardElevated,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MULTIPLIERS.forEach { m ->
                    val active = quantity == m
                    Surface(
                        onClick = { quantity = m },
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) AmberOrangeMid else ObsidianCard,
                        border = BorderStroke(1.dp, if (active) AmberGoldTop else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "X$m",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (active) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Summary Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Bet", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "${formatCoins(totalBet)} VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = CoinGoldSoft
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Potential Win", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "+${formatCoins(projectedWin)} VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = NeonEmeraldBright
                        )
                    }
                }
            }

            // Cancel + Confirm Place Bet Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianCardElevated,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(0.4f)
                        .height(50.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onPlaceBet(totalBet) },
                    enabled = !isSubmitting && availableCoins >= totalBet,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = headerGradient.first(),
                        contentColor = Color.White,
                        disabledContainerColor = ObsidianCardElevated
                    ),
                    modifier = Modifier
                        .weight(0.6f)
                        .height(50.dp)
                        .testTag("quick_bet_confirm_button")
                ) {
                    Text(
                        text = if (availableCoins < totalBet) "Low Balance" else "Place Bet • ${formatCoins(totalBet)} VC",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
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
                            banner.userPredicted -> "Round Settled • #${banner.resultDigit} (${digitBigSmallLabel(banner.resultDigit)})"
                            else -> "Result Declared • #${banner.resultDigit} (${banner.winningColor.displayName})"
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
                if (prediction.betType == "NUMBER" && prediction.selectedDigit != null) {
                    WinGoNumberBall(digit = prediction.selectedDigit, size = 34.dp)
                } else if (prediction.betType == "SIZE") {
                    val isBig = prediction.selectedSize != "SMALL"
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isBig) AmberOrangeMid else SMALL_BLUE_DEEP)
                    ) {
                        Text(
                            text = if (isBig) "B" else "S",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else {
                    ColorOrbBadge(color = selected, size = 34.dp)
                }

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
                            append("Bet: ${prediction.displayPickLabel} (${formatCoins(prediction.stakeCoins)} VC)")
                            if (prediction.winningDigit != null) {
                                append(" • Result: #${prediction.winningDigit} (${digitBigSmallLabel(prediction.winningDigit)})")
                            } else if (winning != null) {
                                append(" • Result: ${winning.displayName}")
                            }
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
