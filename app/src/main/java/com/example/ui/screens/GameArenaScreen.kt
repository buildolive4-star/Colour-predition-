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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.RoundPhase
import com.example.domain.VerifiableRoundOutcome
import com.example.ui.components.ColorOrbBadge
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RoundStatusChip
import com.example.ui.components.RoundVerificationDialog
import com.example.ui.components.VirtualCoinBalancePill
import com.example.ui.components.accentColor
import com.example.ui.components.brightColor
import com.example.ui.components.deepColor
import com.example.ui.components.formatCoins
import com.example.ui.components.formatEpochMs
import com.example.ui.components.formatTimestamp
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
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RoundSettlementBanner

private val STAKE_PRESETS = listOf(50L, 100L, 250L, 500L, 1000L)

@Composable
fun GameArenaScreen(
    selectedMode: GameMode,
    profile: UserProfile?,
    activeRounds: Map<GameMode, ActiveRoundSnapshot>,
    recentOutcomesByMode: Map<GameMode, List<VerifiableRoundOutcome>>,
    predictions: List<PredictionRecord>,
    latestBanner: RoundSettlementBanner?,
    isSubmitting: Boolean,
    onSelectMode: (GameMode) -> Unit,
    onSubmitPrediction: (GameMode, PredictionColor, Long) -> Unit,
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
    var stakeCoins by remember { mutableLongStateOf(100L) }
    var inspectedOutcome by remember { mutableStateOf<VerifiableRoundOutcome?>(null) }

    val availableCoins = profile?.virtualCoins ?: 0L
    val isLockedOrDone = activeRound == null || !activeRound.canPredict
    val alreadyPredicted = currentRoundPrediction != null
    val canInteract = !isLockedOrDone && !alreadyPredicted && !isSubmitting
    val projectedReward = ColorPredictionEngine.calculateRewardCoins(selectedColor, selectedColor, stakeCoins)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back, Game Title & Virtual Balance
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("arena_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = selectedMode.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = selectedMode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                VirtualCoinBalancePill(coins = availableCoins, onClick = onOpenWallet)
            }
        }

        // Independent Duration Switcher Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val modeRound = activeRounds[mode]
                    Surface(
                        onClick = { onSelectMode(mode) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) ElectricViolet else ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ElectricVioletBright else ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arena_mode_tab_${mode.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.shortLabel.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                            Text(
                                text = modeRound?.let { ColorPredictionEngine.formatCountdown(it.remainingMs) } ?: "--:--",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.88f) else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Animated Result Settlement Banner
        item {
            AnimatedVisibility(
                visible = latestBanner != null && latestBanner.mode == selectedMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                latestBanner?.let { banner ->
                    ResultBannerCard(banner = banner, onDismiss = onDismissBanner)
                }
            }
        }

        // Live Countdown Stage Card
        item {
            LiveRoundStageCard(round = activeRound)
        }

        // Glowing Color Selection Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Prediction Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Virtual Rewards Only",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanPulse
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PredictionColor.entries.forEach { color ->
                        GlowingColorChoiceButton(
                            color = color,
                            isSelected = selectedColor == color,
                            enabled = canInteract,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedColor = color
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Virtual Stake Selector & Lock CTA
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Virtual Coin Entry",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { stakeCoins = (stakeCoins - 50L).coerceAtLeast(10L) },
                                enabled = canInteract && stakeCoins > 10L,
                                modifier = Modifier
                                    .size(36.dp)
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
                                onClick = { stakeCoins = (stakeCoins + 50L).coerceAtMost(ColorPredictionEngine.MAX_STAKE_COINS) },
                                enabled = canInteract,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCardElevated)
                                    .testTag("stake_increase_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase virtual stake", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        STAKE_PRESETS.forEach { preset ->
                            val isSelected = stakeCoins == preset
                            Surface(
                                onClick = {
                                    if (canInteract) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        stakeCoins = preset
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CoinGold.copy(alpha = 0.22f) else ObsidianCanvas,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CoinGold else ObsidianBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stake_chip_$preset")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = preset.toString(),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) CoinGoldSoft else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Quick Scale Helpers (1/2, 2x, MAX)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val maxStake = availableCoins.coerceIn(
                            ColorPredictionEngine.MIN_STAKE_COINS,
                            ColorPredictionEngine.MAX_STAKE_COINS
                        )
                        listOf(
                            "½" to { (stakeCoins / 2L).coerceAtLeast(ColorPredictionEngine.MIN_STAKE_COINS) },
                            "2x" to { (stakeCoins * 2L).coerceAtMost(maxStake) },
                            "MAX" to { maxStake }
                        ).forEach { (label, nextStake) ->
                            Surface(
                                onClick = {
                                    if (canInteract && availableCoins >= ColorPredictionEngine.MIN_STAKE_COINS) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        stakeCoins = nextStake()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianCardElevated,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stake_scale_${label.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CyanPulse
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Projected Virtual Reward (${selectedColor.multiplierLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "+${formatCoins(projectedReward)} VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = selectedColor.brightColor()
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmitPrediction(selectedMode, selectedColor, stakeCoins)
                        },
                        enabled = canInteract && availableCoins >= stakeCoins,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedColor.accentColor(),
                            contentColor = Color.White,
                            disabledContainerColor = ObsidianCardElevated,
                            disabledContentColor = TextSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
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
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            isLockedOrDone -> {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.round_locked_label),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            availableCoins < stakeCoins -> {
                                Text(
                                    text = "Insufficient Virtual Coins",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            else -> {
                                Text(
                                    text = "Lock ${selectedColor.displayName} • ${formatCoins(stakeCoins)} VC",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (availableCoins < ColorPredictionEngine.DEMO_REFILL_THRESHOLD) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClaimDemoRefill()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoinGold,
                                contentColor = ObsidianCanvas
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("arena_free_refill_button")
                        ) {
                            Text(
                                text = "Free Virtual Refill (+1,000 VC)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Last 20 Verifiable Results & Live Color Trend
        item {
            val totalCount = recentOutcomes.size.coerceAtLeast(1)
            val redCount = recentOutcomes.count { it.winningColor == PredictionColor.RED }
            val greenCount = recentOutcomes.count { it.winningColor == PredictionColor.GREEN }
            val violetCount = recentOutcomes.count { it.winningColor == PredictionColor.VIOLET }
            val redPct = (redCount * 100) / totalCount
            val greenPct = (greenCount * 100) / totalCount
            val violetPct = (violetCount * 100) / totalCount

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
                        Text(
                            text = "Last ${recentOutcomes.size} Rounds & Trend",
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
                                tint = CyanPulse,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SHA-256",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = CyanPulse
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented Color Trend Distribution Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ObsidianCanvas),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (redCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(redCount.toFloat())
                                    .fillMaxSize()
                                    .background(RubyRed)
                            )
                        }
                        if (greenCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(greenCount.toFloat())
                                    .fillMaxSize()
                                    .background(NeonEmerald)
                            )
                        }
                        if (violetCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(violetCount.toFloat())
                                    .fillMaxSize()
                                    .background(ElectricViolet)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RED $redPct%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RubyRedBright
                        )
                        Text(
                            text = "GREEN $greenPct%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NeonEmeraldBright
                        )
                        Text(
                            text = "VIOLET $violetPct%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ElectricVioletBright
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentOutcomes.take(15), key = { it.roundDocId }) { outcome ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { inspectedOutcome = outcome }
                                    .padding(4.dp)
                            ) {
                                ColorOrbBadge(
                                    color = outcome.winningColor,
                                    digit = outcome.resultDigit,
                                    size = 36.dp
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

        // My Predictions in This Mode
        item {
            Text(
                text = "My ${selectedMode.shortLabel.uppercase()} Predictions",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        if (modePredictions.isEmpty()) {
            item {
                EmptyStatePanel(
                    title = "No predictions in ${selectedMode.title} yet",
                    subtitle = "Choose Red, Green, or Violet above to lock your first virtual entry."
                )
            }
        } else {
            items(modePredictions.take(10), key = { it.id.ifBlank { it.periodId } }) { pred ->
                PredictionRowCard(prediction = pred)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
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
private fun LiveRoundStageCard(round: ActiveRoundSnapshot?) {
    val isLocked = round?.phase == RoundPhase.LOCKED
    val isPending = round?.phase == RoundPhase.RESULT_PENDING
    val borderColor = when {
        isLocked -> RubyRed.copy(alpha = 0.7f)
        isPending -> CoinGold.copy(alpha = 0.7f)
        else -> ElectricViolet.copy(alpha = 0.55f)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianCard,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            borderColor.copy(alpha = 0.14f),
                            ObsidianCard
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT PERIOD",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = round?.periodId ?: "Syncing...",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                RoundStatusChip(phase = round?.phase ?: RoundPhase.WAITING)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isLocked) "LOCKING IN" else "TIME REMAINING",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (isLocked) RubyRedBright else TextSecondary
                    )
                    Text(
                        text = round?.let { ColorPredictionEngine.formatCountdown(it.remainingMs) } ?: "--:--",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        color = if (isLocked) RubyRed else TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Locks ${round?.mode?.lockBeforeEndSeconds ?: 5}s before close",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Auto-advances on close",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanPulse
                    )
                }
            }

            LinearProgressIndicator(
                progress = { 1f - (round?.progress ?: 0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isLocked) RubyRed else NeonEmerald,
                trackColor = ObsidianCanvas
            )
        }
    }
}

@Composable
private fun GlowingColorChoiceButton(
    color: PredictionColor,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected && enabled) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "color_choice_scale"
    )
    val brush = if (enabled) {
        Brush.verticalGradient(
            listOf(
                color.accentColor().copy(alpha = if (isSelected) 0.95f else 0.70f),
                color.deepColor().copy(alpha = if (isSelected) 0.95f else 0.72f)
            )
        )
    } else {
        Brush.verticalGradient(listOf(ObsidianCardElevated, ObsidianCard))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(92.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .border(
                width = if (isSelected && enabled) 2.5.dp else 1.dp,
                color = if (isSelected && enabled) Color.White else color.brightColor().copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("color_button_${color.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = color.displayName.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else TextSecondary
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.28f)
            ) {
                Text(
                    text = color.multiplierLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (enabled) Color.White else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
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
        else -> banner.winningColor.accentColor()
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
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
                ColorOrbBadge(
                    color = banner.winningColor,
                    digit = banner.resultDigit,
                    size = 42.dp
                )
                Column {
                    Text(
                        text = when {
                            banner.userPredicted && banner.didWin -> "You Won +${formatCoins(banner.rewardCoins)} VC!"
                            banner.userPredicted -> "Round Settled • ${banner.winningColor.displayName}"
                            else -> "Result Declared • ${banner.winningColor.displayName}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Period ${banner.periodId} • Proof ${banner.resultHash.take(10)}...",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
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
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
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
