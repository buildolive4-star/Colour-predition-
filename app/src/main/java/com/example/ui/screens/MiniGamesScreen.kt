package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.components.VirtualCoinBalancePill
import com.example.ui.components.formatCoins
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
import com.example.ui.theme.RoyalIndigoPill
import com.example.ui.theme.RubyRed
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

private data class WheelSlice(val label: String, val rewardCoins: Long, val color: Color)

private val WHEEL_SLICES = listOf(
    WheelSlice("50 VC", 50L, Color(0xFF3978F6)),
    WheelSlice("250 VC", 250L, NeonEmerald),
    WheelSlice("0 VC", 0L, Color(0xFF475569)),
    WheelSlice("500 VC", 500L, AmberOrangeMid),
    WheelSlice("100 VC", 100L, ElectricViolet),
    WheelSlice("150 VC", 150L, RubyRed),
    WheelSlice("1000 VC", 1000L, AmberGoldTop),
    WheelSlice("200 VC", 200L, Color(0xFF0EA5E9))
)

@Composable
fun MiniGamesScreen(
    initialGameTab: Int = 0,
    profile: UserProfile?,
    onPlayMiniGameRound: (gameTitle: String, stakeCoins: Long, rewardCoins: Long, summary: String) -> Unit,
    onOpenWallet: () -> Unit,
    onBack: () -> Unit
) {
    var selectedGame by remember(initialGameTab) { mutableIntStateOf(initialGameTab.coerceIn(0, 2)) }
    val availableCoins = profile?.virtualCoins ?: 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("minigames_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column {
                        Text(
                            text = "Mini Games Arcade",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberGoldTop
                        )
                        Text(
                            text = "Virtual Coins Only • Instant Settlement",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                VirtualCoinBalancePill(coins = availableCoins, onClick = onOpenWallet)
            }
        }

        // 3-Game Switcher Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Spin Wheel", "Color Mines", "Cyber Dice").forEachIndexed { idx, label ->
                    val active = selectedGame == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                                else Brush.horizontalGradient(listOf(ObsidianCard, ObsidianCard))
                            )
                            .clickable { selectedGame = idx }
                            .testTag("minigame_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (active) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                when (selectedGame) {
                    0 -> LuckySpinWheelGame(
                        availableCoins = availableCoins,
                        onFinishSpin = { stake, reward, label ->
                            onPlayMiniGameRound("Lucky Spin", stake, reward, "Wheel landed on $label")
                        }
                    )
                    1 -> ColorMinesGame(
                        availableCoins = availableCoins,
                        onSettleGame = { stake, reward, summary ->
                            onPlayMiniGameRound("Color Mines", stake, reward, summary)
                        }
                    )
                    else -> CyberDiceGame(
                        availableCoins = availableCoins,
                        onSettleRoll = { stake, reward, summary ->
                            onPlayMiniGameRound("Cyber Dice", stake, reward, summary)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LuckySpinWheelGame(
    availableCoins: Long,
    onFinishSpin: (stake: Long, reward: Long, sliceLabel: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf("Tap SPIN (100 VC) to win up to 1,000 VC!") }
    val spinCost = 100L

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "LUCKY SPIN WHEEL",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = AmberGoldTop
            )

            // Pointer Arrow + Animated Wheel
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.size(264.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .size(246.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation.value)
                    ) {
                        val sweep = 360f / WHEEL_SLICES.size
                        val d = this.size.minDimension
                        val topLeft = Offset((this.size.width - d) / 2f, (this.size.height - d) / 2f)

                        // Outer golden bezel
                        drawCircle(color = AmberGoldTop, radius = d / 2f)

                        WHEEL_SLICES.forEachIndexed { i, slice ->
                            val startAngle = -90f + (i * sweep) - (sweep / 2f)
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = Offset(topLeft.x + 8.dp.toPx(), topLeft.y + 8.dp.toPx()),
                                size = Size(d - 16.dp.toPx(), d - 16.dp.toPx())
                            )
                        }

                        // Center dark hub
                        drawCircle(color = ObsidianCanvas, radius = d * 0.16f)
                        drawCircle(color = AmberGoldTop, radius = d * 0.12f)
                    }

                    Text(
                        text = "SPIN",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = ObsidianCanvas
                    )
                }

                // Top Red/Gold Triangle Pointer
                Canvas(modifier = Modifier.size(26.dp)) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path, color = RubyRed)
                }
            }

            // Prize legend grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("50 VC", "150 VC", "250 VC", "500 VC", "1000 VC").forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianCanvas,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = tag,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoinGoldSoft,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }

            Text(
                text = lastResult,
                style = MaterialTheme.typography.titleSmall,
                color = NeonEmeraldBright,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    if (!isSpinning && availableCoins >= spinCost) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSpinning = true
                        scope.launch {
                            val winningIndex = Random.nextInt(WHEEL_SLICES.size)
                            val sliceAngle = 360f / WHEEL_SLICES.size
                            val currentMod = ((rotation.value % 360f) + 360f) % 360f
                            val desiredAngle = (360f - (winningIndex * sliceAngle)) % 360f
                            val delta = ((desiredAngle - currentMod) + 360f) % 360f
                            val target = rotation.value + (360f * 5f) + delta

                            rotation.animateTo(
                                targetValue = target,
                                animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
                            )

                            val prize = WHEEL_SLICES[winningIndex]
                            lastResult = if (prize.rewardCoins > 0L) {
                                "Landed on ${prize.label}! +${prize.rewardCoins} VC credited!"
                            } else {
                                "Landed on 0 VC — Try again!"
                            }
                            onFinishSpin(spinCost, prize.rewardCoins, prize.label)
                            isSpinning = false
                        }
                    }
                },
                enabled = !isSpinning && availableCoins >= spinCost,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberOrangeDeep,
                    contentColor = Color.White,
                    disabledContainerColor = ObsidianCardElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("spin_wheel_button")
            ) {
                Text(
                    text = when {
                        isSpinning -> "Spinning..."
                        availableCoins < spinCost -> "Need 100 VC to Spin"
                        else -> "SPIN WHEEL • 100 VC"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ColorMinesGame(
    availableCoins: Long,
    onSettleGame: (stake: Long, reward: Long, summary: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var stake by remember { mutableLongStateOf(100L) }
    var mineCount by remember { mutableIntStateOf(3) }
    var inProgress by remember { mutableStateOf(false) }
    var hitMine by remember { mutableStateOf(false) }
    var minesSet by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var revealedSafe by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var statusText by remember { mutableStateOf("Pick mines & tap Start Round to uncover emerald gems!") }

    val safeRevealedCount = revealedSafe.size
    val currentMultiplier = remember(safeRevealedCount, mineCount) {
        if (safeRevealedCount == 0) 1.0
        else {
            val base = 1.0 + (mineCount * 0.08)
            base.pow(safeRevealedCount.toDouble()).coerceAtMost(25.0)
        }
    }
    val cashoutAmount = (stake * currentMultiplier).roundToLong()

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COLOR MINES (5×5)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = NeonEmeraldBright
                )
                Text(
                    text = String.format(Locale.US, "%.2fx • %s VC", currentMultiplier, formatCoins(cashoutAmount)),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CoinGoldSoft
                )
            }

            // Stake & Mines selectors (editable when not in progress)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50L, 100L, 250L, 500L).forEach { chip ->
                    val selected = stake == chip
                    Surface(
                        onClick = { if (!inProgress) stake = chip },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AmberOrangeMid else ObsidianCanvas,
                        border = BorderStroke(1.dp, if (selected) AmberGoldTop else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${chip} VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 5, 7).forEach { mines ->
                    val selected = mineCount == mines
                    Surface(
                        onClick = { if (!inProgress) mineCount = mines },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) RoyalIndigoPill else ObsidianCanvas,
                        border = BorderStroke(1.dp, if (selected) ElectricVioletBright else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$mines Mines",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }

            // 5x5 Mines Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until 5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 5) {
                            val idx = row * 5 + col
                            val isSafeTile = idx in revealedSafe
                            val isRevealedMine = hitMine && idx in minesSet

                            val tileColor = when {
                                isRevealedMine -> RubyRed
                                isSafeTile -> NeonEmerald
                                else -> ObsidianCardElevated
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tileColor)
                                    .clickable(enabled = inProgress && !isSafeTile) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (idx in minesSet) {
                                            hitMine = true
                                            inProgress = false
                                            statusText = "Boom! Hit a mine. Better luck next round!"
                                            onSettleGame(stake, 0L, "Hit a mine after $safeRevealedCount gems")
                                        } else {
                                            revealedSafe = revealedSafe + idx
                                            statusText = "Gem found! Collect now or uncover another tile."
                                        }
                                    }
                                    .testTag("mines_tile_$idx")
                            ) {
                                when {
                                    isRevealedMine -> Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Mine", tint = Color.White, modifier = Modifier.size(22.dp))
                                    isSafeTile -> Icon(Icons.Filled.Diamond, contentDescription = "Gem", tint = Color.White, modifier = Modifier.size(22.dp))
                                    else -> Icon(Icons.Filled.QuestionMark, contentDescription = "Hidden tile", tint = TextSecondary.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (hitMine) RubyRedBright else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (!inProgress) {
                Button(
                    onClick = {
                        if (availableCoins >= stake) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            minesSet = (0 until 25).shuffled().take(mineCount).toSet()
                            revealedSafe = emptySet()
                            hitMine = false
                            inProgress = true
                            statusText = "Round active! Tap tiles to find gems."
                        }
                    },
                    enabled = availableCoins >= stake,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("mines_start_button")
                ) {
                    Text(
                        text = if (availableCoins < stake) "Insufficient VC" else "START MINES • $stake VC",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        inProgress = false
                        val payout = cashoutAmount
                        statusText = "Collected +${formatCoins(payout)} VC ($safeRevealedCount gems)!"
                        onSettleGame(stake, payout, "Collected $safeRevealedCount gems (${String.format(Locale.US, "%.2fx", currentMultiplier)})")
                    },
                    enabled = safeRevealedCount > 0,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("mines_cashout_button")
                ) {
                    Text(
                        text = if (safeRevealedCount == 0) "Reveal 1 Gem to Collect" else "COLLECT +${formatCoins(cashoutAmount)} VC",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberDiceGame(
    availableCoins: Long,
    onSettleRoll: (stake: Long, reward: Long, summary: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var stake by remember { mutableLongStateOf(100L) }
    var target by remember { mutableFloatStateOf(50f) }
    var rollOver by remember { mutableStateOf(true) }
    var lastRolledNumber by remember { mutableStateOf<Int?>(null) }
    var lastHash by remember { mutableStateOf<String?>(null) }
    var lastWon by remember { mutableStateOf<Boolean?>(null) }

    val targetInt = target.toInt().coerceIn(10, 90)
    val winChance = if (rollOver) (100 - targetInt) else targetInt
    val multiplier = (96.0 / winChance.coerceAtLeast(5)).coerceIn(1.08, 12.0)
    val projectedReward = (stake * multiplier).roundToLong()

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, CyanPulse.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CYBER DICE (1 – 100)",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = CyanPulse
            )

            // Big Neon Result Readout
            Surface(
                shape = CircleShape,
                color = ObsidianCanvas,
                border = BorderStroke(
                    2.dp,
                    when (lastWon) {
                        true -> NeonEmerald
                        false -> RubyRed
                        null -> AmberGoldTop
                    }
                ),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (lastRolledNumber != null) {
                        Text(
                            text = lastRolledNumber.toString(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp,
                            color = if (lastWon == true) NeonEmeraldBright else RubyRedBright
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = null,
                            tint = AmberGoldTop,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            // Roll Over / Roll Under Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(true to "Roll Over $targetInt", false to "Roll Under $targetInt").forEach { (isOver, label) ->
                    val selected = rollOver == isOver
                    Surface(
                        onClick = { rollOver = isOver },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) AmberOrangeMid else ObsidianCanvas,
                        border = BorderStroke(1.dp, if (selected) AmberGoldTop else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Target Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Win Chance: $winChance%", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = TextSecondary)
                    Text(
                        text = String.format(Locale.US, "%.2fx (+%s VC)", multiplier, formatCoins(projectedReward)),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CoinGoldSoft
                    )
                }
                Slider(
                    value = target,
                    onValueChange = { target = it },
                    valueRange = 10f..90f,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberGoldTop,
                        activeTrackColor = AmberOrangeMid,
                        inactiveTrackColor = ObsidianCanvas
                    )
                )
            }

            // Stake Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50L, 100L, 250L, 500L).forEach { chip ->
                    val selected = stake == chip
                    Surface(
                        onClick = { stake = chip },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) NeonEmerald else ObsidianCanvas,
                        border = BorderStroke(1.dp, if (selected) NeonEmeraldBright else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$chip VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            if (lastHash != null) {
                Text(
                    text = "SHA-256 Proof: ${lastHash?.take(22)}...",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = {
                    if (availableCoins >= stake) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val seed = "CYBERDICE:${System.nanoTime()}:${Random.nextLong()}"
                        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
                        val hex = digest.joinToString("") { "%02x".format(it) }
                        val rolled = (((digest[0].toInt() and 0xFF) * 256 + (digest[1].toInt() and 0xFF)) % 100) + 1
                        val won = if (rollOver) rolled > targetInt else rolled < targetInt
                        val reward = if (won) projectedReward else 0L

                        lastRolledNumber = rolled
                        lastHash = hex
                        lastWon = won
                        val rule = if (rollOver) "Over $targetInt" else "Under $targetInt"
                        onSettleRoll(stake, reward, "Rolled $rolled ($rule)")
                    }
                },
                enabled = availableCoins >= stake,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("cyber_dice_roll_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (availableCoins < stake) "Insufficient Virtual Coins" else "ROLL DICE • $stake VC",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
