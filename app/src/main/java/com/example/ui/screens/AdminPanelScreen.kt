package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminAuditLogEntity
import com.example.data.AppConfigEntity
import com.example.data.UserProfile
import com.example.data.VirtualVoucherEntity
import com.example.ui.components.QrMatrixCanvas
import com.example.ui.components.formatCoins
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
import java.util.Locale

private const val DEMO_ADMIN_PIN = "1234"

@Composable
fun AdminPanelScreen(
    profile: UserProfile?,
    appConfig: AppConfigEntity,
    vouchers: List<VirtualVoucherEntity>,
    adminLogs: List<AdminAuditLogEntity>,
    totalPredictionsCount: Int,
    onSaveConfig: (AppConfigEntity, String) -> Unit,
    onAdjustUserCoins: (Long, String) -> Unit,
    onCreateVoucher: (Long, String) -> Unit,
    onBack: () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Overview, 1=Games, 2=Users, 3=QR & Logs

    if (!isUnlocked) {
        AdminPinGate(
            pinInput = pinInput,
            pinError = pinError,
            onPinChange = {
                if (it.length <= 6) {
                    pinInput = it
                    pinError = false
                }
            },
            onSubmit = {
                if (pinInput == DEMO_ADMIN_PIN) {
                    isUnlocked = true
                } else {
                    pinError = true
                }
            },
            onQuickDemoFill = {
                pinInput = DEMO_ADMIN_PIN
                isUnlocked = true
            },
            onBack = onBack
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column {
                        Text(
                            text = "Admin Command Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberGoldTop
                        )
                        Text(
                            text = "Live Game Engine • Virtual Wallet • QR Manager",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = NeonEmeraldBright,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(28.dp)
                )
            }
        }

        // 4 Admin Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Overview", "Games", "Users", "QR & Logs").forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                                else Brush.horizontalGradient(listOf(ObsidianCard, ObsidianCard))
                            )
                            .clickable { selectedTab = idx }
                            .testTag("admin_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (active) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Live KPI Metrics
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminKpiCard(
                                title = "USER WALLET",
                                value = "${formatCoins(profile?.virtualCoins ?: 0L)} VC",
                                accent = CoinGoldSoft,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "PREDICTIONS",
                                value = totalPredictionsCount.toString(),
                                accent = CyanPulse,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val activeModes = listOf(
                                appConfig.mode30sEnabled,
                                appConfig.mode60sEnabled,
                                appConfig.mode3mEnabled,
                                appConfig.mode5mEnabled
                            ).count { it }
                            AdminKpiCard(
                                title = "ACTIVE ARENAS",
                                value = "$activeModes / 4",
                                accent = NeonEmeraldBright,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "QR VOUCHERS",
                                value = "${vouchers.count { !it.redeemed }} Active",
                                accent = ElectricVioletBright,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Live Global Announcement Broadcast Editor
                item {
                    var announcementDraft by remember(appConfig.announcementText) {
                        mutableStateOf(appConfig.announcementText)
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Campaign, contentDescription = null, tint = AmberGoldTop)
                                Text(
                                    text = "Global Home Announcement Ticker",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            OutlinedTextField(
                                value = announcementDraft,
                                onValueChange = { if (it.length <= 180) announcementDraft = it },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberGoldTop,
                                    unfocusedBorderColor = ObsidianBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_announcement_input")
                            )
                            Button(
                                onClick = {
                                    onSaveConfig(
                                        appConfig.copy(announcementText = announcementDraft.trim()),
                                        "Updated Home announcement banner"
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("admin_save_announcement_button")
                            ) {
                                Text("Broadcast to Home Lobby", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Game Modes Toggle & Multipliers & Next Round Override
                item {
                    var rgMult by remember(appConfig.redGreenMultiplier) { mutableDoubleStateOf(appConfig.redGreenMultiplier) }
                    var vMult by remember(appConfig.violetMultiplier) { mutableDoubleStateOf(appConfig.violetMultiplier) }

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Arena Enable/Disable Switches
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
                                Text("WinGo Arena Switches", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                AdminToggleRow("WinGo 30sec Arena", appConfig.mode30sEnabled) {
                                    onSaveConfig(appConfig.copy(mode30sEnabled = it), "WinGo 30s ${if (it) "enabled" else "paused"}")
                                }
                                AdminToggleRow("WinGo 1 Min Arena", appConfig.mode60sEnabled) {
                                    onSaveConfig(appConfig.copy(mode60sEnabled = it), "WinGo 1m ${if (it) "enabled" else "paused"}")
                                }
                                AdminToggleRow("WinGo 3 Min Arena", appConfig.mode3mEnabled) {
                                    onSaveConfig(appConfig.copy(mode3mEnabled = it), "WinGo 3m ${if (it) "enabled" else "paused"}")
                                }
                                AdminToggleRow("WinGo 5 Min Arena", appConfig.mode5mEnabled) {
                                    onSaveConfig(appConfig.copy(mode5mEnabled = it), "WinGo 5m ${if (it) "enabled" else "paused"}")
                                }
                            }
                        }

                        // Payout Multipliers
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Virtual Reward Multipliers", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "Green / Red Multiplier: ${String.format(Locale.US, "%.1fx", rgMult)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    color = NeonEmeraldBright
                                )
                                Slider(
                                    value = rgMult.toFloat(),
                                    onValueChange = { rgMult = (kotlin.math.round(it * 10.0) / 10.0) },
                                    valueRange = 1.5f..3.0f,
                                    colors = SliderDefaults.colors(thumbColor = NeonEmerald, activeTrackColor = NeonEmerald)
                                )
                                Text(
                                    text = "Violet Multiplier: ${String.format(Locale.US, "%.1fx", vMult)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    color = ElectricVioletBright
                                )
                                Slider(
                                    value = vMult.toFloat(),
                                    onValueChange = { vMult = (kotlin.math.round(it * 10.0) / 10.0) },
                                    valueRange = 3.0f..6.0f,
                                    colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet)
                                )
                                Button(
                                    onClick = {
                                        onSaveConfig(
                                            appConfig.copy(redGreenMultiplier = rgMult, violetMultiplier = vMult),
                                            "Multipliers set: R/G ${rgMult}x, Violet ${vMult}x"
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeMid, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply Multipliers", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Next-Round Demo Override Control
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Next Round Engine Mode (Demo)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "Active Mode: ${appConfig.forcedNextColor?.let { "Next Settlement -> $it" } ?: "Auto Fair (SHA-256)"}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = CoinGoldSoft
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        null to "Auto",
                                        "GREEN" to "Green",
                                        "VIOLET" to "Violet",
                                        "RED" to "Red"
                                    ).forEach { (overrideVal, label) ->
                                        val active = appConfig.forcedNextColor == overrideVal
                                        Surface(
                                            onClick = {
                                                onSaveConfig(
                                                    appConfig.copy(forcedNextColor = overrideVal),
                                                    "Engine mode set to ${overrideVal ?: "Auto Fair SHA-256"}"
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (active) AmberOrangeMid else ObsidianCanvas,
                                            border = BorderStroke(1.dp, if (active) AmberGoldTop else ObsidianBorder),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) Color.White else TextSecondary,
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Users & Virtual Coin Wallet Management
                item {
                    var reasonInput by remember { mutableStateOf("Admin demo reward") }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Active Device Player", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCanvas,
                                border = BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(profile?.displayName ?: "Chroma Player", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = "${profile?.publicUserId ?: "CP-DEMO01"} • ${profile?.totalWins ?: 0}/${profile?.totalGames ?: 0} Wins",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "${formatCoins(profile?.virtualCoins ?: 0L)} VC",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = CoinGoldSoft
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = reasonInput,
                                onValueChange = { reasonInput = it },
                                label = { Text("Ledger Note") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberGoldTop,
                                    unfocusedBorderColor = ObsidianBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(500L to "+500", 1000L to "+1K", 5000L to "+5K").forEach { (amt, lbl) ->
                                    Button(
                                        onClick = { onAdjustUserCoins(amt, reasonInput) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.White),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_credit_$amt")
                                    ) {
                                        Text("$lbl VC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { onAdjustUserCoins(-500L, reasonInput) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RubyRed),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RubyRedBright),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Debit -500 Virtual Coins", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {
                // QR Manager + Voucher Generator + Audit Log
                item {
                    var qrLabelDraft by remember(appConfig.adminQrLabel) { mutableStateOf(appConfig.adminQrLabel) }
                    var qrPayloadDraft by remember(appConfig.adminQrPayload) { mutableStateOf(appConfig.adminQrPayload) }

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Admin Counter QR Customization
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.QrCode2, contentDescription = null, tint = AmberGoldTop)
                                    Text("Demo Counter QR Code Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                }
                                QrMatrixCanvas(payload = qrPayloadDraft.ifBlank { "CHROMAPULSE:DEMO" }, size = 148.dp)
                                OutlinedTextField(
                                    value = qrLabelDraft,
                                    onValueChange = { qrLabelDraft = it },
                                    label = { Text("QR Title") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGoldTop,
                                        unfocusedBorderColor = ObsidianBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = qrPayloadDraft,
                                    onValueChange = { qrPayloadDraft = it },
                                    label = { Text("Virtual Demo QR Data") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGoldTop,
                                        unfocusedBorderColor = ObsidianBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onSaveConfig(
                                            appConfig.copy(
                                                adminQrLabel = qrLabelDraft.trim().ifBlank { "ChromaPulse Counter" },
                                                adminQrPayload = qrPayloadDraft.trim().ifBlank { "CHROMAPULSE:DEMO" }
                                            ),
                                            "Updated Demo Counter QR"
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeMid, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Counter QR", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Generate Gift Voucher QR
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Generate Virtual Gift Voucher QR", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(500L to "+500", 1000L to "+1K", 2500L to "+2.5K").forEach { (amt, lbl) ->
                                        Button(
                                            onClick = { onCreateVoucher(amt, "Admin Gift QR ($lbl VC)") },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.White),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("admin_gen_voucher_$amt")
                                        ) {
                                            Text(lbl, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Text("Admin Audit Trail (${adminLogs.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }

                items(adminLogs.take(20), key = { it.id }) { log ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.action, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AmberGoldTop)
                                Text(formatTimestamp(log.createdAtMs), fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextSecondary)
                            }
                            Text(log.details, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPinGate(
    pinInput: String,
    pinError: Boolean,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onQuickDemoFill: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = AmberGoldTop, modifier = Modifier.size(40.dp))
                Text("Admin Panel Access", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    text = "Enter Admin Demo PIN (default: 1234) to manage games, virtual coins, announcements & QR vouchers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = onPinChange,
                    label = { Text("Admin PIN") },
                    singleLine = true,
                    isError = pinError,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGoldTop,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
                if (pinError) {
                    Text("Incorrect PIN. Default demo PIN is 1234.", style = MaterialTheme.typography.bodySmall, color = RubyRedBright)
                }
                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_unlock_button")
                ) {
                    Text("Unlock Admin Panel", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = onQuickDemoFill,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, NeonEmerald),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmeraldBright),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_quick_enter_button")
                ) {
                    Text("Instant Demo Enter (PIN 1234)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to App")
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextSecondary)
            Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = accent)
        }
    }
}

@Composable
private fun AdminToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonEmerald)
        )
    }
}
