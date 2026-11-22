package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserNotification
import com.example.data.UserProfile
import com.example.data.WalletTransaction
import com.example.domain.ColorPredictionEngine
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.VirtualDisclaimerBanner
import com.example.ui.components.formatCoins
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinGoldSoft
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualWalletSheet(
    profile: UserProfile?,
    transactions: List<WalletTransaction>,
    isClaimingBonus: Boolean,
    onClaimDailyBonus: () -> Unit,
    onClaimDemoRefill: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val todayUtc = remember { ColorPredictionEngine.formatUtcIsoDate(System.currentTimeMillis()) }
    val claimedToday = profile?.lastDailyBonusDate == todayUtc
    val canClaimRefill = (profile?.virtualCoins ?: 0L) < ColorPredictionEngine.DEMO_REFILL_THRESHOLD

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Virtual Coin Wallet",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AVAILABLE DEMO BALANCE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoinGold
                    )
                    Text(
                        text = "${formatCoins(profile?.virtualCoins ?: 0L)} VC",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = CoinGoldSoft
                    )
                    Text(
                        text = "Entertainment virtual coins only — no deposits, no withdrawals, no monetary value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onClaimDailyBonus,
                        enabled = !claimedToday && !isClaimingBonus,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoinGold,
                            contentColor = ObsidianCanvas,
                            disabledContainerColor = ObsidianCardElevated,
                            disabledContentColor = TextSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wallet_sheet_claim_bonus_button")
                    ) {
                        Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (claimedToday) "Daily +500 VC Already Claimed" else "Claim +500 Daily Virtual Coins",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (canClaimRefill) {
                        OutlinedButton(
                            onClick = onClaimDemoRefill,
                            enabled = !isClaimingBonus,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyanPulse.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPulse),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("wallet_sheet_demo_refill_button")
                        ) {
                            Text(
                                text = "Low Balance? Free Virtual Refill (+1,000 VC)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            VirtualDisclaimerBanner()

            Text(
                text = "Immutable Ledger (${transactions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            if (transactions.isEmpty()) {
                EmptyStatePanel(
                    title = "No transactions yet",
                    subtitle = "Your welcome bonus and game entries will appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions.take(25), key = { it.id.ifBlank { it.referenceId + it.amount } }) { tx ->
                        WalletTransactionRow(tx = tx)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    notifications: List<UserNotification>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val unreadCount = remember(notifications) { notifications.count { !it.read } }

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = ElectricVioletBright
                    )
                    Text(
                        text = "Live Notifications",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
                if (unreadCount > 0) {
                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.testTag("notifications_mark_all_read")
                    ) {
                        Text(
                            text = "Mark all read ($unreadCount)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = CyanPulse
                        )
                    }
                } else {
                    Text(
                        text = "0 unread",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            if (notifications.isEmpty()) {
                EmptyStatePanel(
                    title = "All caught up!",
                    subtitle = "Round results, virtual rewards, and daily bonus alerts will stream here in real time."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id.ifBlank { it.title + it.createdAtMs } }) { notif ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (!notif.read) ElectricViolet.copy(alpha = 0.14f) else ObsidianCard,
                            border = BorderStroke(
                                1.dp,
                                if (!notif.read) ElectricVioletBright.copy(alpha = 0.5f) else ObsidianBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!notif.read && notif.id.isNotBlank()) {
                                        onMarkRead(notif.id)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (!notif.read) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(NeonEmerald)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = notif.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = formatTimestamp(notif.createdAtMs),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = TextSecondary.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (notif.id.isNotBlank()) {
                                    IconButton(
                                        onClick = { onDelete(notif.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete notification",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
