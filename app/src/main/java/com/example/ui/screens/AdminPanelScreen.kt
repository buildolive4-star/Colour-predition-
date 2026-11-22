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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminAuditLogEntity
import com.example.data.AppConfigEntity
import com.example.data.CoAdminGrantEntity
import com.example.data.CoAdminInviteEntity
import com.example.data.UserProfile
import com.example.data.VirtualVoucherEntity
import com.example.security.PasswordHasher
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
import com.example.ui.viewmodel.AdminAccessRole
import java.util.Locale

@Composable
fun AdminPanelScreen(
    currentUserProfile: UserProfile?,
    adminRole: AdminAccessRole,
    isVerifyingPassword: Boolean,
    appConfig: AppConfigEntity,
    allUsers: List<UserProfile>,
    vouchers: List<VirtualVoucherEntity>,
    adminLogs: List<AdminAuditLogEntity>,
    coAdminInvites: List<CoAdminInviteEntity>,
    coAdminGrants: List<CoAdminGrantEntity>,
    lastCreatedPassword: String?,
    totalPredictionsCount: Int,
    onUnlockWithPassword: (String, (Boolean, String?) -> Unit) -> Unit,
    onCreateCoAdminPassword: (String, String) -> Unit,
    onClearLastCreatedPassword: () -> Unit,
    onToggleCoAdminInvite: (String, Boolean, String) -> Unit,
    onDeleteCoAdminInvite: (String, String) -> Unit,
    onRevokeCoAdminUser: (String, String) -> Unit,
    onSaveConfig: (AppConfigEntity, String) -> Unit,
    onAdjustUserCoins: (String, Long, String) -> Unit,
    onCreateVoucher: (Long, String) -> Unit,
    onBack: () -> Unit
) {
    var enteredPanel by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPlayerUid by remember(currentUserProfile?.userId, allUsers) {
        mutableStateOf(currentUserProfile?.userId ?: allUsers.firstOrNull()?.userId.orEmpty())
    }

    val isSuperAdmin = adminRole == AdminAccessRole.SUPER_ADMIN
    val isAuthorized = adminRole != AdminAccessRole.NONE

    // Hidden Admin / Co-Admin Password Login Page
    if (!enteredPanel || !isAuthorized) {
        AdminPasswordAccessScreen(
            adminRole = adminRole,
            userEmail = currentUserProfile?.email.orEmpty(),
            passwordInput = passwordInput,
            passwordError = passwordError,
            isVerifying = isVerifyingPassword,
            onPasswordChange = {
                if (it.length <= 48) {
                    passwordInput = it
                    passwordError = null
                }
            },
            onEnterAsAuthorized = { enteredPanel = true },
            onSubmitPassword = {
                passwordError = null
                onUnlockWithPassword(passwordInput) { ok, err ->
                    if (ok) {
                        passwordInput = ""
                        enteredPanel = true
                    } else {
                        passwordError = err ?: "Invalid Co-Admin password."
                    }
                }
            },
            onBack = onBack
        )
        return
    }

    val tabs = remember(isSuperAdmin) {
        if (isSuperAdmin) listOf("Overview", "Games", "Users", "QR & Logs", "Co-Admins")
        else listOf("Overview", "Games", "Users", "QR & Logs")
    }
    if (selectedTab >= tabs.size) selectedTab = 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(bottom = 28.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isSuperAdmin) "Main Admin Console" else "Co-Admin Console",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberGoldTop
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSuperAdmin) NeonEmerald.copy(alpha = 0.2f) else ElectricViolet.copy(alpha = 0.24f)
                            ) {
                                Text(
                                    text = if (isSuperAdmin) "OWNER" else "CO-ADMIN",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuperAdmin) NeonEmeraldBright else ElectricVioletBright,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Cloud Synced • Firebase Live Engine & Team Controls",
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

        // Scrollable Admin Tabs
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tabs) { idx, label ->
                    val active = selectedTab == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                                else Brush.horizontalGradient(listOf(ObsidianCard, ObsidianCard))
                            )
                            .clickable { selectedTab = idx }
                            .padding(horizontal = 16.dp)
                            .testTag("admin_tab_$idx")
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

        when (selectedTab) {
            0 -> {
                // Live Cloud KPI Metrics
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminKpiCard(
                                title = "CLOUD PLAYERS",
                                value = allUsers.size.coerceAtLeast(1).toString(),
                                accent = CyanPulse,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "YOUR WALLET",
                                value = "${formatCoins(currentUserProfile?.virtualCoins ?: 0L)} VC",
                                accent = CoinGoldSoft,
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
                                title = "CLOUD VOUCHERS",
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
                                    text = "Global Live Announcement (All Devices)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            OutlinedTextField(
                                value = announcementDraft,
                                onValueChange = { if (it.length <= 220) announcementDraft = it },
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
                                        "Broadcasted announcement to all devices"
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("admin_save_announcement_button")
                            ) {
                                Text("Broadcast Live via Firebase", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Game Modes Toggle, Multipliers & Engine Mode
                item {
                    var rgMult by remember(appConfig.redGreenMultiplier) { mutableDoubleStateOf(appConfig.redGreenMultiplier) }
                    var vMult by remember(appConfig.violetMultiplier) { mutableDoubleStateOf(appConfig.violetMultiplier) }

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                                Text("Global WinGo Arena Switches", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                                    Text("Sync Multipliers to Cloud", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

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
                                Text("Engine Result Mode (Demo)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "Active Mode: ${appConfig.forcedNextColor?.let { "Override -> $it" } ?: "Auto Fair (SHA-256)"}",
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
                                                textAlign = TextAlign.Center
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
                // Cloud Users & Multi-Player Wallet Manager
                item {
                    val playersList = allUsers.ifEmpty { listOfNotNull(currentUserProfile) }
                    val targetPlayer = playersList.firstOrNull { it.userId == selectedPlayerUid } ?: playersList.firstOrNull()
                    var reasonInput by remember { mutableStateOf("Admin virtual demo reward") }

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select Cloud Player (${playersList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        playersList.forEach { player ->
                            val isSelected = player.userId == targetPlayer?.userId
                            Surface(
                                onClick = { selectedPlayerUid = player.userId },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) ObsidianCardElevated else ObsidianCard,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AmberGoldTop else ObsidianBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(player.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = "${player.publicUserId} • ${player.email}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "${formatCoins(player.virtualCoins)} VC",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = CoinGoldSoft
                                    )
                                }
                            }
                        }

                        if (targetPlayer != null) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Adjust Coins for ${targetPlayer.displayName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = AmberGoldTop
                                    )
                                    OutlinedTextField(
                                        value = reasonInput,
                                        onValueChange = { if (it.length <= 160) reasonInput = it },
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
                                                onClick = { onAdjustUserCoins(targetPlayer.userId, amt, reasonInput) },
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
                                        onClick = { onAdjustUserCoins(targetPlayer.userId, -500L, reasonInput) },
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
                }
            }

            3 -> {
                // Cloud Counter QR + Gift Voucher QR Generator + Audit Logs
                item {
                    var qrLabelDraft by remember(appConfig.adminQrLabel) { mutableStateOf(appConfig.adminQrLabel) }
                    var qrPayloadDraft by remember(appConfig.adminQrPayload) { mutableStateOf(appConfig.adminQrPayload) }

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                                    Text("Global Demo Counter QR Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                }
                                QrMatrixCanvas(payload = qrPayloadDraft.ifBlank { "CHROMAPULSE:DEMO" }, size = 148.dp)
                                OutlinedTextField(
                                    value = qrLabelDraft,
                                    onValueChange = { if (it.length <= 80) qrLabelDraft = it },
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
                                    onValueChange = { if (it.length <= 200) qrPayloadDraft = it },
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
                                            "Updated global Demo Counter QR"
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeMid, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sync Counter QR to All Users", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

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
                                Text("Generate Cloud Gift Voucher QR", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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

                        Text("Cloud Audit Trail (${adminLogs.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }

                items(adminLogs.take(25), key = { it.id.ifBlank { it.action + it.createdAtMs } }) { log ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${log.action} • ${log.actorRole}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = AmberGoldTop
                                )
                                Text(
                                    text = formatTimestamp(log.createdAtMs),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(log.details, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text(log.actorEmail, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }

            4 -> {
                // Main Admin ONLY: Create & Manage Co-Admin Passwords & Active Co-Admins
                item {
                    val clipboard = LocalClipboardManager.current
                    var coAdminLabel by remember { mutableStateOf("") }
                    var coAdminPassword by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Key, contentDescription = null, tint = AmberGoldTop)
                                    Text("Create Co-Admin Password", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                }
                                Text(
                                    text = "Password is stored as a one-way SHA-256 hash in Cloud Firestore. Share the password with your Co-Admin — they tap 'ChromaPulse' 5 times and enter it to open the Admin Panel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                OutlinedTextField(
                                    value = coAdminLabel,
                                    onValueChange = { if (it.length <= 50) coAdminLabel = it },
                                    label = { Text("Co-Admin Name / Shift Label") },
                                    placeholder = { Text("e.g., Night Moderator") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGoldTop,
                                        unfocusedBorderColor = ObsidianBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("coadmin_label_input")
                                )

                                OutlinedTextField(
                                    value = coAdminPassword,
                                    onValueChange = { if (it.length <= 48) coAdminPassword = it },
                                    label = { Text("Co-Admin Password (min 6 chars)") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            coAdminPassword = PasswordHasher.generateStrongPassword(10)
                                        }) {
                                            Icon(Icons.Filled.Refresh, contentDescription = "Generate password", tint = AmberGoldTop)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGoldTop,
                                        unfocusedBorderColor = ObsidianBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("coadmin_password_input")
                                )

                                Button(
                                    onClick = {
                                        if (coAdminPassword.trim().length >= 6) {
                                            onCreateCoAdminPassword(
                                                coAdminLabel.trim().ifBlank { "Co-Admin" },
                                                coAdminPassword.trim()
                                            )
                                            coAdminLabel = ""
                                            coAdminPassword = ""
                                        }
                                    },
                                    enabled = coAdminPassword.trim().length >= 6,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("coadmin_create_button")
                                ) {
                                    Text("Create Co-Admin Password", fontWeight = FontWeight.ExtraBold)
                                }

                                if (lastCreatedPassword != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = NeonEmerald.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, NeonEmerald),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "New Co-Admin Password Ready (Copy Now):",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = NeonEmeraldBright
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = lastCreatedPassword,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    color = CoinGoldSoft
                                                )
                                                Row {
                                                    IconButton(onClick = {
                                                        clipboard.setText(AnnotatedString(lastCreatedPassword))
                                                    }) {
                                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy password", tint = AmberGoldTop)
                                                    }
                                                    OutlinedButton(
                                                        onClick = onClearLastCreatedPassword,
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Hide", fontSize = 11.sp, color = TextSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Created Co-Admin Passkeys (${coAdminInvites.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        if (coAdminInvites.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No Co-Admin passkeys created yet. Create one above to share Admin access securely.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            coAdminInvites.forEach { invite ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ObsidianCard,
                                    border = BorderStroke(
                                        1.dp,
                                        if (invite.active) NeonEmerald.copy(alpha = 0.5f) else RubyRed.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(invite.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(
                                                text = "SHA-256: ${invite.inviteHash.take(14)}… • ${if (invite.active) "ACTIVE" else "REVOKED"}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = if (invite.active) NeonEmeraldBright else RubyRedBright
                                            )
                                        }
                                        Switch(
                                            checked = invite.active,
                                            onCheckedChange = { enabled ->
                                                onToggleCoAdminInvite(invite.inviteHash, enabled, invite.label)
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonEmerald)
                                        )
                                        IconButton(onClick = { onDeleteCoAdminInvite(invite.inviteHash, invite.label) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete passkey", tint = RubyRedBright)
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Active Co-Admin Accounts (${coAdminGrants.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        coAdminGrants.forEach { grant ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(grant.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = "${grant.email} • Key: ${grant.label}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(onClick = { onRevokeCoAdminUser(grant.userId, grant.email) }) {
                                        Icon(Icons.Filled.PersonRemove, contentDescription = "Remove Co-Admin", tint = RubyRedBright)
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

@Composable
private fun AdminPasswordAccessScreen(
    adminRole: AdminAccessRole,
    userEmail: String,
    passwordInput: String,
    passwordError: String?,
    isVerifying: Boolean,
    onPasswordChange: (String) -> Unit,
    onEnterAsAuthorized: () -> Unit,
    onSubmitPassword: () -> Unit,
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
            border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = if (adminRole != AdminAccessRole.NONE) Icons.Filled.VerifiedUser else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (adminRole != AdminAccessRole.NONE) NeonEmeraldBright else AmberGoldTop,
                    modifier = Modifier.size(44.dp)
                )

                Text(
                    text = "Admin & Co-Admin Portal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                if (adminRole == AdminAccessRole.SUPER_ADMIN) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonEmerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, NeonEmerald)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = NeonEmeraldBright, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Main Admin Verified ($userEmail)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = NeonEmeraldBright
                            )
                        }
                    }

                    Button(
                        onClick = onEnterAsAuthorized,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_super_enter_button")
                    ) {
                        Text("Open Main Admin Console", fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    if (adminRole == AdminAccessRole.CO_ADMIN) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricViolet.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, ElectricVioletBright)
                        ) {
                            Text(
                                text = "Active Co-Admin Session Verified",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = ElectricVioletBright,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                        Button(
                            onClick = onEnterAsAuthorized,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_coadmin_resume_button")
                        ) {
                            Text("Enter Co-Admin Panel", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Text(
                        text = "Enter the Co-Admin password created by the Main Admin to unlock Cloud Admin controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = onPasswordChange,
                        label = { Text("Co-Admin Password") },
                        singleLine = true,
                        isError = passwordError != null,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGoldTop,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    if (passwordError != null) {
                        Text(
                            text = passwordError,
                            style = MaterialTheme.typography.bodySmall,
                            color = RubyRedBright,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onSubmitPassword,
                        enabled = !isVerifying && passwordInput.trim().length >= 4,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_unlock_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Unlock Admin Panel", fontWeight = FontWeight.ExtraBold)
                        }
                    }
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
