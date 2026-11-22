package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PredictionRecord
import com.example.data.UserProfile
import com.example.domain.ColorPredictionEngine
import com.example.domain.PredictionColor
import com.example.ui.components.VirtualDisclaimerBanner
import com.example.ui.components.brightColor
import com.example.ui.components.formatCoins
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AmberGoldTop
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
import com.example.ui.theme.RoyalIndigoPill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    predictions: List<PredictionRecord>,
    onOpenWallet: () -> Unit,
    onOpenQrCenter: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onUpdateSettings: (String, Boolean, Boolean, Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var secretBrandTaps by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    val displayName = profile?.displayName ?: "Chroma Player"
    val totalGames = profile?.totalGames ?: 0L
    val totalWins = profile?.totalWins ?: 0L
    val totalPoints = profile?.totalVirtualPoints ?: 0L
    val winRate = if (totalGames > 0L) ((totalWins * 100L) / totalGames).toInt() else 0
    val badgeTitle = ColorPredictionEngine.resolveBadgeTitle(
        totalVirtualPoints = totalPoints,
        totalWins = totalWins,
        bestStreak = profile?.bestStreak ?: 0L
    )
    val (nextBadgeLabel, nextBadgeProgress) = ColorPredictionEngine.resolveNextBadgeProgress(totalPoints)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Player Profile",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
        }

        // Identity Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        PlayerAvatar(
                            displayName = displayName,
                            photoUrl = profile?.photoUrl,
                            size = 58
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = profile?.email.orEmpty().ifBlank { "Instant Play • Local Profile" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ObsidianCardElevated
                                ) {
                                    Text(
                                        text = profile?.publicUserId ?: "CP-000000",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanPulse,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonEmerald.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = badgeTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonEmeraldBright,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { showEditNameDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("profile_edit_name_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit display name",
                            tint = ElectricVioletBright
                        )
                    }
                }
            }
        }

        // Career Stats 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        label = "Virtual Balance",
                        value = "${formatCoins(profile?.virtualCoins ?: 0L)} VC",
                        accent = CoinGoldSoft,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Virtual Points",
                        value = formatCoins(profile?.totalVirtualPoints ?: 0L),
                        accent = ElectricVioletBright,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        label = "Total Games / Wins",
                        value = "$totalGames / $totalWins ($winRate%)",
                        accent = NeonEmeraldBright,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Win Streak (Best)",
                        value = "${profile?.currentStreak ?: 0L} (${profile?.bestStreak ?: 0L})",
                        accent = CyanPulse,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Rank Progression & Color Mastery Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
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
                            text = "Rank Progression",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(nextBadgeProgress * 100).toInt()}%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ElectricVioletBright
                        )
                    }
                    LinearProgressIndicator(
                        progress = { nextBadgeProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = ElectricVioletBright,
                        trackColor = ObsidianCanvas
                    )
                    Text(
                        text = nextBadgeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PredictionColor.entries.forEach { color ->
                            val picks = predictions.count { it.selectedColor.equals(color.name, ignoreCase = true) }
                            val wins = predictions.count {
                                it.selectedColor.equals(color.name, ignoreCase = true) && it.status == "WON"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianCanvas,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = color.displayName.uppercase(),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = color.brightColor()
                                    )
                                    Text(
                                        text = "$wins/$picks",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Wins",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wallet, QR Center, Leaderboard & Admin Panel Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenWallet,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianCardElevated,
                        contentColor = CoinGoldSoft
                    ),
                    border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_open_wallet_button")
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = CoinGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Virtual Coin Wallet & Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CoinGoldSoft
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenQrCenter,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberOrangeMid,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_open_qr_button")
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR Vouchers", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenLeaderboard,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianCardElevated,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_open_leaderboard_button")
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AmberGoldTop, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rankings", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianCardElevated,
                        contentColor = Color(0xFFFF6B6B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out of Google Account",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Notification Preferences
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Notification Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    PreferenceToggleRow(
                        title = "Round Results & Rewards",
                        subtitle = "Notify when my virtual predictions settle",
                        checked = profile?.notifyResults ?: true,
                        onCheckedChange = { checked ->
                            onUpdateSettings(
                                displayName,
                                checked,
                                profile?.notifyRoundClosing ?: true,
                                profile?.notifyBonuses ?: true
                            )
                        }
                    )
                    PreferenceToggleRow(
                        title = "Round Closing Alerts",
                        subtitle = "Highlight countdown lock windows in live arenas",
                        checked = profile?.notifyRoundClosing ?: true,
                        onCheckedChange = { checked ->
                            onUpdateSettings(
                                displayName,
                                profile?.notifyResults ?: true,
                                checked,
                                profile?.notifyBonuses ?: true
                            )
                        }
                    )
                    PreferenceToggleRow(
                        title = "Daily Bonus Reminders",
                        subtitle = "Log in-app receipts for daily virtual rewards",
                        checked = profile?.notifyBonuses ?: true,
                        onCheckedChange = { checked ->
                            onUpdateSettings(
                                displayName,
                                profile?.notifyResults ?: true,
                                profile?.notifyRoundClosing ?: true,
                                checked
                            )
                        }
                    )
                }
            }
        }

        // Account Info & Fair Play
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Account Security & Fair Play",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Account Status: ${profile?.accountStatus ?: "ACTIVE"} • Joined ${formatTimestamp(profile?.createdAtMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "ChromaPulse Cloud • Synced with Firebase Firestore. Virtual coins carry zero real-world monetary value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.clickable {
                            val now = System.currentTimeMillis()
                            secretBrandTaps = if (now - lastTapMs > 3000L) 1 else secretBrandTaps + 1
                            lastTapMs = now
                            if (secretBrandTaps >= 5) {
                                secretBrandTaps = 0
                                onOpenAdminPanel()
                            }
                        }
                    )
                }
            }
        }

        item { VirtualDisclaimerBanner() }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    if (showEditNameDialog) {
        var nameInput by remember(displayName) { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = ObsidianCard,
            title = { Text("Edit Display Name", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 60) nameInput = it },
                    singleLine = true,
                    label = { Text("Display Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.trim().isNotEmpty()) {
                            onUpdateSettings(
                                nameInput.trim(),
                                profile?.notifyResults ?: true,
                                profile?.notifyRoundClosing ?: true,
                                profile?.notifyBonuses ?: true
                            )
                            showEditNameDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_display_name_button")
                ) {
                    Text("Save", color = ElectricVioletBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = accent
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricViolet
            )
        )
    }
}
