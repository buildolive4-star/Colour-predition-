package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.45f)),
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
                color = CoinGold.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun VirtualDisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard.copy(alpha = 0.85f),
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
                tint = CyanPulse,
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

@Composable
fun ColorOrbBadge(
    color: PredictionColor,
    digit: Int? = null,
    size: Dp = 32.dp,
    onClick: (() -> Unit)? = null
) {
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
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun RoundStatusChip(phase: RoundPhase) {
    val (bg, fg, icon) = when (phase) {
        RoundPhase.ACTIVE -> Triple(
            NeonEmerald.copy(alpha = 0.16f),
            NeonEmeraldBright,
            Icons.Filled.CheckCircle
        )
        RoundPhase.LOCKED -> Triple(
            RubyRed.copy(alpha = 0.18f),
            RubyRedBright,
            Icons.Filled.Lock
        )
        RoundPhase.RESULT_PENDING -> Triple(
            CoinGold.copy(alpha = 0.18f),
            CoinGoldSoft,
            Icons.Filled.Verified
        )
        RoundPhase.COMPLETED -> Triple(
            ElectricViolet.copy(alpha = 0.18f),
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
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
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
                ColorOrbBadge(color = winningColor, digit = resultDigit, size = 34.dp)
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
                    text = "Winning Color: ${winningColor.displayName} (Digit #$resultDigit)",
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
                            color = CyanPulse
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
                    text = "Every round result is deterministically derived from a canonical UTC period seed using SHA-256 and stored immutably in Cloud Firestore.",
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
                Text("Close", color = ElectricVioletBright)
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
                ColorOrbBadge(PredictionColor.RED, size = 14.dp)
                ColorOrbBadge(PredictionColor.GREEN, size = 14.dp)
                ColorOrbBadge(PredictionColor.VIOLET, size = 14.dp)
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
