package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.PredictionColor
import com.example.domain.RoundPhase
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeMid
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinGoldSoft
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.ElectricVioletDeep
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldBright
import com.example.ui.theme.NeonEmeraldDeep
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.RubyRed
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.RubyRedDeep
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun PredictionColor.accentColor(): Color = when (this) {
    PredictionColor.RED -> RubyRed
    PredictionColor.GREEN -> NeonEmerald
    PredictionColor.VIOLET -> ElectricViolet
}

fun PredictionColor.brightColor(): Color = when (this) {
    PredictionColor.RED -> RubyRedBright
    PredictionColor.GREEN -> NeonEmeraldBright
    PredictionColor.VIOLET -> ElectricVioletBright
}

fun PredictionColor.deepColor(): Color = when (this) {
    PredictionColor.RED -> RubyRedDeep
    PredictionColor.GREEN -> NeonEmeraldDeep
    PredictionColor.VIOLET -> ElectricVioletDeep
}

fun digitToPredictionColor(digit: Int): PredictionColor = when (digit) {
    0, 5 -> PredictionColor.VIOLET
    1, 3, 7, 9 -> PredictionColor.GREEN
    else -> PredictionColor.RED
}

fun digitBigSmallLabel(digit: Int): String = if (digit >= 5) "Big" else "Small"

fun formatCoins(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.US).format(amount)

fun formatTimestamp(epochMs: Long?): String {
    val date = Date(epochMs ?: System.currentTimeMillis())
    return SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US).format(date)
}

fun formatEpochMs(epochMs: Long): String =
    SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US).format(Date(epochMs))

@Composable
fun VirtualCoinBalancePill(
    coins: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = ObsidianCardElevated,
        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.55f)),
        modifier = modifier
            .height(40.dp)
            .testTag("wallet_balance_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = stringResource(R.string.cd_wallet),
                tint = CoinGold,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = formatCoins(coins),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CoinGoldSoft,
                maxLines = 1
            )
            Text(
                text = "VC",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = CoinGold.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun VirtualDisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = AmberGoldTop,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.virtual_coins_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Glossy 3D WinGo-style numbered ball (0-9) matching the classic casino/lottery sphere look:
 * - 0: Red + Violet split outer ring
 * - 5: Green + Violet split outer ring
 * - 1,3,7,9: Emerald Green ring
 * - 2,4,6,8: Coral Red ring
 */
@Composable
fun WinGoNumberBall(
    digit: Int,
    size: Dp = 48.dp,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = when (digit) {
        0 -> RubyRed
        5 -> NeonEmerald
        1, 3, 7, 9 -> NeonEmerald
        else -> RubyRed
    }
    val secondaryColor = when (digit) {
        0, 5 -> ElectricViolet
        else -> primaryColor
    }
    val numberTextColor = when (digit) {
        0, 5 -> ElectricViolet
        1, 3, 7, 9 -> NeonEmerald
        else -> RubyRed
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, AmberGoldTop, CircleShape)
                } else {
                    Modifier
                }
            )
            .let { mod -> if (onClick != null) mod.clickable(onClick = onClick) else mod }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = this.size.minDimension
            val radius = diameter / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer sphere base (split halves for 0 and 5)
            if (primaryColor != secondaryColor) {
                drawArc(
                    color = primaryColor,
                    startAngle = 135f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = Size(diameter, diameter)
                )
                drawArc(
                    color = secondaryColor,
                    startAngle = -45f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = Size(diameter, diameter)
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.9f), primaryColor),
                        center = Offset(radius * 0.7f, radius * 0.6f),
                        radius = radius * 1.2f
                    ),
                    radius = radius,
                    center = center
                )
            }

            // Soft metallic ring rim
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = radius * 0.94f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = diameter * 0.045f)
            )

            // Crisp glossy white center disk
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE7ECF8)),
                    center = Offset(radius * 0.85f, radius * 0.75f),
                    radius = radius * 0.72f
                ),
                radius = radius * 0.62f,
                center = center
            )

            // Top specular highlight dot
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.16f,
                center = Offset(radius * 0.65f, radius * 0.42f)
            )
        }

        Text(
            text = digit.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.43f).sp,
            color = numberTextColor
        )
    }
}

/**
 * Single or dual WinGo color dot indicator used in History tables (0 = Red+Violet, 5 = Green+Violet).
 */
@Composable
fun WinGoColorDots(
    digit: Int,
    dotSize: Dp = 11.dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (digit) {
            0 -> {
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(RubyRed))
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(ElectricViolet))
            }
            5 -> {
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(NeonEmerald))
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(ElectricViolet))
            }
            1, 3, 7, 9 -> {
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(NeonEmerald))
            }
            else -> {
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(RubyRed))
            }
        }
    }
}

@Composable
fun ColorOrbBadge(
    color: PredictionColor,
    digit: Int? = null,
    size: Dp = 32.dp,
    onClick: (() -> Unit)? = null
) {
    if (digit != null) {
        WinGoNumberBall(
            digit = digit,
            size = size,
            onClick = onClick
        )
        return
    }
    val brush = Brush.radialGradient(
        colors = listOf(color.brightColor(), color.accentColor(), color.deepColor())
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .let { mod -> if (onClick != null) mod.clickable(onClick = onClick) else mod }
    ) {}
}

@Composable
fun RoundStatusChip(phase: RoundPhase) {
    val (bg, fg, icon) = when (phase) {
        RoundPhase.ACTIVE -> Triple(
            NeonEmerald.copy(alpha = 0.18f),
            NeonEmeraldBright,
            Icons.Filled.CheckCircle
        )
        RoundPhase.LOCKED -> Triple(
            RubyRed.copy(alpha = 0.20f),
            RubyRedBright,
            Icons.Filled.Lock
        )
        RoundPhase.RESULT_PENDING -> Triple(
            CoinGold.copy(alpha = 0.20f),
            CoinGoldSoft,
            Icons.Filled.Verified
        )
        RoundPhase.COMPLETED -> Triple(
            ElectricViolet.copy(alpha = 0.20f),
            ElectricVioletBright,
            Icons.Filled.Verified
        )
        RoundPhase.WAITING -> Triple(
            ObsidianCardElevated,
            TextSecondary,
            Icons.Filled.Shield
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = phase.label.uppercase(Locale.US),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = fg
            )
        }
    }
}

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "How To Play ChromaPulse",
                style = MaterialTheme.typography.titleLarge,
                color = AmberGoldTop
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Pick a timed arena (30s, 1 Min, 3 Min, or 5 Min). Each period locks 5 seconds before the timer hits 00:00.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "2. Choose Green (2.0x), Red (2.0x), or Violet (4.5x) — or tap any numbered ball (0–9) to pick its matching color:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCanvas,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• Green: Digits 1, 3, 7, 9 (2.0x VC)", style = MaterialTheme.typography.bodySmall, color = NeonEmeraldBright)
                        Text("• Red: Digits 2, 4, 6, 8 (2.0x VC)", style = MaterialTheme.typography.bodySmall, color = RubyRedBright)
                        Text("• Violet: Digits 0, 5 (4.5x VC)", style = MaterialTheme.typography.bodySmall, color = ElectricVioletBright)
                    }
                }
                Text(
                    text = "3. Every result is deterministically verified with SHA-256. Virtual demo coins only — zero real-money wagering.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("how_to_play_close")
            ) {
                Text("Got It", color = AmberOrangeMid, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun RoundVerificationDialog(
    periodId: String,
    modeTitle: String,
    winningColor: PredictionColor,
    resultDigit: Int,
    resultHash: String,
    completedTimeText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WinGoNumberBall(digit = resultDigit, size = 38.dp)
                Column {
                    Text(
                        text = "Verifiable Round Result",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "$modeTitle • $periodId",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Winning Color: ${winningColor.displayName} (Digit #$resultDigit • ${digitBigSmallLabel(resultDigit)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = winningColor.brightColor()
                )
                Text(
                    text = "Completed: $completedTimeText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCanvas,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "SHA-256 FAIRNESS PROOF",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGoldTop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resultHash.ifBlank { "Verified on settlement" },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
                Text(
                    text = "Every round result is deterministically derived from a canonical UTC period seed using SHA-256 and stored immutably on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("verification_dialog_close")
            ) {
                Text("Close", color = AmberOrangeMid, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun EmptyStatePanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WinGoNumberBall(digit = 4, size = 24.dp)
                WinGoNumberBall(digit = 7, size = 24.dp)
                WinGoNumberBall(digit = 5, size = 24.dp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
