package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.components.VirtualCoinBalancePill
import com.example.ui.components.WinCelebrationDialog
import com.example.ui.components.WinPopupData
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

private data class WheelSlice(
    val shortValue: String,
    val label: String,
    val rewardCoins: Long,
    val lightColor: Color,
    val darkColor: Color
)

private val WHEEL_SLICES = listOf(
    WheelSlice("50", "50 VC", 50L, Color(0xFF4F8BFF), Color(0xFF1D4ED8)),
    WheelSlice("250", "250 VC", 250L, Color(0xFF34D399), Color(0xFF047857)),
    WheelSlice("0", "0 VC", 0L, Color(0xFF64748B), Color(0xFF1E293B)),
    WheelSlice("500", "500 VC", 500L, Color(0xFFFB923C), Color(0xFFC2410C)),
    WheelSlice("100", "100 VC", 100L, Color(0xFFA78BFA), Color(0xFF6D28D9)),
    WheelSlice("150", "150 VC", 150L, Color(0xFFF87171), Color(0xFFB91C1C)),
    WheelSlice("1000", "1000 VC", 1000L, Color(0xFFFDE047), Color(0xFFD97706)),
    WheelSlice("200", "200 VC", 200L, Color(0xFF38BDF8), Color(0xFF0369A1))
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
        // Top Header (fixed responsive spacing so wallet pill never clips)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("minigames_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mini Games Arcade",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberGoldTop,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "3D Spin • Mines • Cyber Dice",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                listOf("3D Spin", "Color Mines", "Cyber Dice").forEachIndexed { idx, label ->
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
    var winPopup by remember { mutableStateOf<WinPopupData?>(null) }
    val spinCost = 100L

    // Animated chasing marquee lights & subtle 3D aura shimmer
    val infinite = rememberInfiniteTransition(label = "wheel_3d_fx")
    val lightPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpinning) 280 else 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marquee_lights"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wheel_glow"
    )

    // Pointer deflection ticks dynamically as slices pass beneath it while spinning
    val pointerTickAngle = if (isSpinning) {
        val sliceProgress = (rotation.value % 45f) / 45f
        -(sin(sliceProgress * PI).toFloat() * 18f)
    } else {
        0f
    }

    val triggerSpin = {
        if (!isSpinning && availableCoins >= spinCost) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isSpinning = true
            scope.launch {
                val winningIndex = Random.nextInt(WHEEL_SLICES.size)
                val sliceAngle = 360f / WHEEL_SLICES.size
                val currentMod = ((rotation.value % 360f) + 360f) % 360f
                val desiredAngle = (360f - (winningIndex * sliceAngle)) % 360f
                val delta = ((desiredAngle - currentMod) + 360f) % 360f
                val target = rotation.value + (360f * 6f) + delta

                rotation.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 3400, easing = FastOutSlowInEasing)
                )

                val prize = WHEEL_SLICES[winningIndex]
                lastResult = if (prize.rewardCoins > 0L) {
                    "Landed on ${prize.label}! +${formatCoins(prize.rewardCoins)} VC credited!"
                } else {
                    "Landed on 0 VC — Spin again!"
                }
                onFinishSpin(spinCost, prize.rewardCoins, prize.label)
                if (prize.rewardCoins > 0L) {
                    winPopup = WinPopupData(
                        title = if (prize.rewardCoins >= 500L) "MEGA SPIN JACKPOT!" else "LUCKY SPIN WINNER!",
                        amountWon = prize.rewardCoins,
                        subtitle = "3D Wheel landed directly on ${prize.label}",
                        badgeText = if (prize.rewardCoins >= 500L) "JACKPOT WIN" else "SPIN REWARD"
                    )
                }
                isSpinning = false
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianCard,
        border = BorderStroke(1.2.dp, AmberGoldTop.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3D LUCKY SPIN WHEEL",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = AmberGoldTop
                )
            }

            // 3D Stage: Ambient Aura + Pedestal Base + Rotating 3D Wheel + Ticking Pointer
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(322.dp)
            ) {
                // Static 3D Base Pedestal & Ambient Backlight
                Canvas(
                    modifier = Modifier
                        .size(308.dp)
                        .align(Alignment.Center)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.48f

                    // Golden radial aura behind wheel
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmberGoldTop.copy(alpha = glowPulse),
                                AmberOrangeDeep.copy(alpha = glowPulse * 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = size.minDimension * 0.54f
                        ),
                        radius = size.minDimension * 0.54f,
                        center = Offset(cx, cy)
                    )

                    // 3D Floor Ground Shadow
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                            center = Offset(cx, size.height * 0.94f),
                            radius = size.width * 0.36f
                        ),
                        topLeft = Offset(cx - size.width * 0.34f, size.height * 0.89f),
                        size = Size(size.width * 0.68f, size.height * 0.11f)
                    )

                    // 3D Metallic Stand Pedestal
                    val standPath = Path().apply {
                        moveTo(cx - 34.dp.toPx(), size.height * 0.78f)
                        lineTo(cx + 34.dp.toPx(), size.height * 0.78f)
                        lineTo(cx + 56.dp.toPx(), size.height * 0.93f)
                        lineTo(cx - 56.dp.toPx(), size.height * 0.93f)
                        close()
                    }
                    drawPath(
                        path = standPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF92400E), AmberGoldTop, Color(0xFF78350F)),
                            startY = size.height * 0.78f,
                            endY = size.height * 0.93f
                        )
                    )
                    // Base metallic bar lip
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF78350F), CoinGoldSoft, AmberGoldTop, Color(0xFF78350F))
                        ),
                        topLeft = Offset(cx - 64.dp.toPx(), size.height * 0.905f),
                        size = Size(128.dp.toPx(), 10.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                }

                // 3D Wheel Body + Rotating Slices + Crisp Slice Numbers + Stationary Specular Glint
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .size(274.dp)
                ) {
                    // Rotating 3D Disc & Prize Labels
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation.value)
                    ) {
                        val d = size.minDimension
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerRadius = d / 2f
                        val rimThickness = 15.dp.toPx()
                        val wheelRadius = outerRadius - rimThickness
                        val sweep = 360f / WHEEL_SLICES.size

                        // 3D Outer Metallic Gold Rim with Bevel Depth
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFF3B0),
                                    AmberGoldTop,
                                    Color(0xFFB45309),
                                    AmberOrangeDeep,
                                    Color(0xFFFDE047)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(d, d)
                            ),
                            radius = outerRadius,
                            center = Offset(cx, cy)
                        )

                        // Dark recessed channel inside metallic rim
                        drawCircle(
                            color = Color(0xFF1A120B),
                            radius = wheelRadius + 3.dp.toPx(),
                            center = Offset(cx, cy)
                        )

                        // Draw each 3D shaded slice + gold spoke dividers
                        val wheelTopLeft = Offset(cx - wheelRadius, cy - wheelRadius)
                        val wheelSize = Size(wheelRadius * 2f, wheelRadius * 2f)

                        WHEEL_SLICES.forEachIndexed { i, slice ->
                            val startAngle = -90f + (i * sweep) - (sweep / 2f)
                            drawArc(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        slice.lightColor,
                                        slice.lightColor.copy(alpha = 0.92f),
                                        slice.darkColor
                                    ),
                                    center = Offset(cx, cy),
                                    radius = wheelRadius
                                ),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = wheelTopLeft,
                                size = wheelSize
                            )
                        }

                        // Metallic Gold Divider Spokes & Rim Studs
                        WHEEL_SLICES.forEachIndexed { i, _ ->
                            val borderDeg = -90f + (i * sweep) - (sweep / 2f)
                            val rad = Math.toRadians(borderDeg.toDouble())
                            val edgeX = cx + (cos(rad) * wheelRadius).toFloat()
                            val edgeY = cy + (sin(rad) * wheelRadius).toFloat()
                            drawLine(
                                color = CoinGoldSoft.copy(alpha = 0.85f),
                                start = Offset(cx, cy),
                                end = Offset(edgeX, edgeY),
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            // Brass pin on slice boundary
                            drawCircle(
                                color = Color(0xFFFFFBEB),
                                radius = 3.dp.toPx(),
                                center = Offset(
                                    cx + (cos(rad) * (wheelRadius - 4.dp.toPx())).toFloat(),
                                    cy + (sin(rad) * (wheelRadius - 4.dp.toPx())).toFloat()
                                )
                            )
                        }

                        // Draw crisp 3D-shadowed prize numbers DIRECTLY inside every slice!
                        val valuePaint = Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.WHITE
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                            setShadowLayer(6f, 2f, 3f, android.graphics.Color.argb(210, 0, 0, 0))
                        }
                        val unitPaint = Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.argb(245, 255, 244, 194)
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                            textSize = 9.5.sp.toPx()
                            setShadowLayer(4f, 1f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
                        }

                        val nc = drawContext.canvas.nativeCanvas
                        WHEEL_SLICES.forEachIndexed { i, slice ->
                            val midAngle = -90f + (i * sweep)
                            // Rotate so the slice center points straight UP (-90deg -> 0deg upright at top!)
                            val uprightRotation = midAngle + 90f
                            nc.save()
                            nc.rotate(uprightRotation, cx, cy)

                            valuePaint.textSize = if (slice.shortValue.length >= 4) 15.5.sp.toPx() else 18.sp.toPx()
                            val textY = cy - wheelRadius * 0.64f
                            nc.drawText(slice.shortValue, cx, textY, valuePaint)
                            nc.drawText("VC", cx, textY + 12.5.dp.toPx(), unitPaint)
                            nc.restore()
                        }

                        // Subtle inner shadow ring for 3D bowl depth
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.28f)
                                ),
                                center = Offset(cx, cy),
                                radius = wheelRadius
                            ),
                            radius = wheelRadius,
                            center = Offset(cx, cy)
                        )
                    }

                    // Stationary 3D Chasing Marquee LED Bulbs & Glass Specular Sheen
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val d = size.minDimension
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerRadius = d / 2f
                        val rimMidRadius = outerRadius - 7.5.dp.toPx()
                        val bulbCount = 16
                        val activeParity = lightPhase.toInt() % 2

                        for (b in 0 until bulbCount) {
                            val angleRad = Math.toRadians((b * (360.0 / bulbCount)) - 90.0)
                            val bx = cx + (cos(angleRad) * rimMidRadius).toFloat()
                            val by = cy + (sin(angleRad) * rimMidRadius).toFloat()
                            val lit = (b % 2) == activeParity
                            if (lit) {
                                drawCircle(
                                    color = Color(0xFFFFFBEB).copy(alpha = 0.45f),
                                    radius = 5.5.dp.toPx(),
                                    center = Offset(bx, by)
                                )
                            }
                            drawCircle(
                                color = if (lit) Color(0xFFFFFBEB) else Color(0xFF78350F),
                                radius = 3.2.dp.toPx(),
                                center = Offset(bx, by)
                            )
                        }

                        // Top-left 3D glass highlight arc for glossy polish
                        drawArc(
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                            ),
                            startAngle = 205f,
                            sweepAngle = 130f,
                            useCenter = false,
                            topLeft = Offset(19.dp.toPx(), 19.dp.toPx()),
                            size = Size(d - 38.dp.toPx(), d - 38.dp.toPx()),
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 3D Metallic Golden Center Hub (clickable to spin!)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .clickable(enabled = !isSpinning && availableCoins >= spinCost) { triggerSpin() }
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFFBEB),
                                        AmberGoldTop,
                                        AmberOrangeDeep
                                    ),
                                    center = Offset(22f, 18f)
                                )
                            )
                    ) {
                        Text(
                            text = if (isSpinning) "..." else "SPIN",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = ObsidianCanvas
                        )
                    }
                }

                // 3D Sculpted Animated Ticking Pointer Needle at Top
                Canvas(
                    modifier = Modifier
                        .size(width = 36.dp, height = 42.dp)
                        .offset(y = 2.dp)
                        .rotate(pointerTickAngle)
                ) {
                    val w = size.width
                    val h = size.height
                    // Drop shadow
                    val shadowPath = Path().apply {
                        moveTo(w / 2f, h)
                        lineTo(w * 0.14f, h * 0.28f)
                        lineTo(w * 0.86f, h * 0.28f)
                        close()
                    }
                    drawPath(shadowPath, color = Color.Black.copy(alpha = 0.45f))

                    // Ruby-gold 3D dart body
                    val dartPath = Path().apply {
                        moveTo(w / 2f, h * 0.94f)
                        lineTo(w * 0.12f, h * 0.22f)
                        lineTo(w / 2f, h * 0.04f)
                        lineTo(w * 0.88f, h * 0.22f)
                        close()
                    }
                    drawPath(
                        path = dartPath,
                        brush = Brush.linearGradient(
                            colors = listOf(RubyRedBright, RubyRed, Color(0xFF991B1B)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                    drawPath(
                        path = dartPath,
                        color = CoinGoldSoft,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Metallic rivet cap
                    drawCircle(
                        color = CoinGoldSoft,
                        radius = 4.5.dp.toPx(),
                        center = Offset(w / 2f, h * 0.28f)
                    )
                }
            }

            // Prize legend chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("50", "150", "250", "500", "1000").forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianCanvas,
                        border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$tag VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoinGoldSoft,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
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
                onClick = triggerSpin,
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
                        isSpinning -> "Spinning 3D Wheel..."
                        availableCoins < spinCost -> "Need 100 VC to Spin"
                        else -> "SPIN 3D WHEEL • 100 VC"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }

    winPopup?.let { data ->
        WinCelebrationDialog(
            data = data,
            onDismiss = { winPopup = null }
        )
    }
}

@Composable
private fun ColorMinesGame(
    availableCoins: Long,
    onSettleGame: (stake: Long, reward: Long, summary: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var stake by remember { mutableLongStateOf(50L) }
    var mineCount by remember { mutableIntStateOf(3) }
    var inProgress by remember { mutableStateOf(false) }
    var hitMine by remember { mutableStateOf(false) }
    var minesSet by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var revealedSafe by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var statusText by remember { mutableStateOf("Pick stake & mines, then tap Start to uncover emerald gems!") }
    var winPopup by remember { mutableStateOf<WinPopupData?>(null) }

    val safeRevealedCount = revealedSafe.size
    val totalSafeTiles = 25 - mineCount
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

            // Stake Chips (Min 10 VC!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(10L, 50L, 100L, 250L, 500L).forEach { chip ->
                    val selected = stake == chip
                    Surface(
                        onClick = { if (!inProgress) stake = chip },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AmberOrangeMid else ObsidianCanvas,
                        border = BorderStroke(1.dp, if (selected) AmberGoldTop else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$chip",
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

                            val tileBrush = when {
                                isRevealedMine -> Brush.verticalGradient(listOf(RubyRedBright, RubyRed))
                                isSafeTile -> Brush.verticalGradient(listOf(NeonEmeraldBright, NeonEmerald))
                                else -> Brush.verticalGradient(listOf(ObsidianCardElevated, ObsidianCanvas))
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tileBrush)
                                    .clickable(enabled = inProgress && !isSafeTile) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (idx in minesSet) {
                                            hitMine = true
                                            inProgress = false
                                            statusText = "Boom! Hit a mine. Better luck next round!"
                                            onSettleGame(stake, 0L, "Hit a mine after $safeRevealedCount gems")
                                        } else {
                                            val updatedSafe = revealedSafe + idx
                                            revealedSafe = updatedSafe
                                            if (updatedSafe.size >= totalSafeTiles) {
                                                // Cleared the entire board! Auto-win with celebration popup
                                                val fullMult = (1.0 + mineCount * 0.08)
                                                    .pow(updatedSafe.size.toDouble())
                                                    .coerceAtMost(25.0)
                                                val fullPayout = (stake * fullMult).roundToLong()
                                                inProgress = false
                                                statusText = "Board Cleared! Won +${formatCoins(fullPayout)} VC!"
                                                onSettleGame(
                                                    stake,
                                                    fullPayout,
                                                    "Cleared all ${updatedSafe.size} gems (${String.format(Locale.US, "%.2fx", fullMult)})"
                                                )
                                                winPopup = WinPopupData(
                                                    title = "COLOR MINES JACKPOT!",
                                                    amountWon = fullPayout,
                                                    subtitle = "Cleared all ${updatedSafe.size} Emerald Gems (${String.format(Locale.US, "%.2fx", fullMult)})",
                                                    badgeText = "BOARD CLEARED"
                                                )
                                            } else {
                                                statusText = "Gem found! Collect now or uncover another tile."
                                            }
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
                            statusText = "Round active! Tap tiles to find emerald gems."
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
                        val multLabel = String.format(Locale.US, "%.2fx", currentMultiplier)
                        statusText = "Collected +${formatCoins(payout)} VC ($safeRevealedCount gems)!"
                        onSettleGame(stake, payout, "Collected $safeRevealedCount gems ($multLabel)")
                        winPopup = WinPopupData(
                            title = "COLOR MINES WINNER!",
                            amountWon = payout,
                            subtitle = "Collected $safeRevealedCount Emerald Gems at $multLabel",
                            badgeText = "MINES CASHOUT"
                        )
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

    winPopup?.let { data ->
        WinCelebrationDialog(
            data = data,
            onDismiss = { winPopup = null }
        )
    }
}

@Composable
private fun CyberDiceGame(
    availableCoins: Long,
    onSettleRoll: (stake: Long, reward: Long, summary: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var stake by remember { mutableLongStateOf(10L) }
    var target by remember { mutableFloatStateOf(50f) }
    var rollOver by remember { mutableStateOf(true) }
    var lastRolledNumber by remember { mutableStateOf<Int?>(null) }
    var lastHash by remember { mutableStateOf<String?>(null) }
    var lastWon by remember { mutableStateOf<Boolean?>(null) }
    var rollPulseTrigger by remember { mutableIntStateOf(0) }
    var winPopup by remember { mutableStateOf<WinPopupData?>(null) }

    var isBouncing by remember { mutableStateOf(false) }
    LaunchedEffect(rollPulseTrigger) {
        if (rollPulseTrigger > 0) {
            isBouncing = true
            delay(220)
            isBouncing = false
        }
    }
    val orbScale by animateFloatAsState(
        targetValue = if (isBouncing) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f),
        label = "dice_orb_scale"
    )

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
                text = "CYBER DICE (1 – 100) • MIN 10 VC",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = CyanPulse
            )

            // Big 3D Neon Result Readout Orb
            Surface(
                shape = CircleShape,
                color = ObsidianCanvas,
                border = BorderStroke(
                    2.5.dp,
                    when (lastWon) {
                        true -> NeonEmerald
                        false -> RubyRed
                        null -> AmberGoldTop
                    }
                ),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(116.dp)
                    .scale(orbScale)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(
                        Brush.radialGradient(
                            colors = listOf(
                                when (lastWon) {
                                    true -> NeonEmerald.copy(alpha = 0.25f)
                                    false -> RubyRed.copy(alpha = 0.25f)
                                    null -> AmberGoldTop.copy(alpha = 0.18f)
                                },
                                ObsidianCanvas
                            )
                        )
                    )
                ) {
                    if (lastRolledNumber != null) {
                        Text(
                            text = lastRolledNumber.toString(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 40.sp,
                            color = if (lastWon == true) NeonEmeraldBright else RubyRedBright
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = null,
                            tint = AmberGoldTop,
                            modifier = Modifier.size(46.dp)
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

            // Stake Chips (Min 10 VC!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(10L, 50L, 100L, 250L, 500L).forEach { chip ->
                    val selected = stake == chip
                    Surface(
                        onClick = { stake = chip },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) NeonEmerald else ObsidianCanvas,
                        border = BorderStroke(1.dp, if (selected) NeonEmeraldBright else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dice_stake_$chip")
                    ) {
                        Text(
                            text = "$chip",
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
                        rollPulseTrigger++
                        val rule = if (rollOver) "Over $targetInt" else "Under $targetInt"
                        onSettleRoll(stake, reward, "Rolled $rolled ($rule)")
                        if (won && reward > 0L) {
                            val multStr = String.format(Locale.US, "%.2fx", multiplier)
                            winPopup = WinPopupData(
                                title = "CYBER DICE WINNER!",
                                amountWon = reward,
                                subtitle = "Rolled $rolled ($rule • $multStr)",
                                badgeText = "DICE WIN"
                            )
                        }
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

    winPopup?.let { data ->
        WinCelebrationDialog(
            data = data,
            onDismiss = { winPopup = null }
        )
    }
}
