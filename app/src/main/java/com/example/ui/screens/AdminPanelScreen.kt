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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminAuditLogEntity
import com.example.data.AppConfigEntity
import com.example.data.CoAdminGrantEntity
import com.example.data.CoAdminInviteEntity
import com.example.data.UserProfile
import com.example.data.VirtualVoucherEntity
import com.example.data.WalletRequestEntity
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.security.PasswordHasher
import com.example.ui.components.QrMatrixCanvas
import com.example.ui.components.WinGoNumberBall
import com.example.ui.components.digitBigSmallLabel
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
    walletRequests: List<WalletRequestEntity>,
    lastCreatedPassword: String?,
    totalPredictionsCount: Int,
    onUnlockWithPassword: (String, (Boolean, String?) -> Unit) -> Unit,
    onCreateCoAdminPassword: (String, String) -> Unit,
    onClearLastCreatedPassword: () -> Unit,
    onToggleCoAdminInvite: (String, Boolean, String) -> Unit,
    onDeleteCoAdminInvite: (String, String) -> Unit,
    onRevokeCoAdminUser: (String, String) -> Unit,
    onApproveDeposit: (String, String) -> Unit,
    onVerifyWithdrawal: (String, String) -> Unit,
    onRejectWalletRequest: (String, String) -> Unit,
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

    val pendingRequestsCount = remember(walletRequests) {
        walletRequests.count { it.status == "PENDING" }
    }
    val tabs = remember(isSuperAdmin, pendingRequestsCount) {
        val reqLabel = if (pendingRequestsCount > 0) "Requests ($pendingRequestsCount)" else "Requests"
        if (isSuperAdmin) listOf("Overview", reqLabel, "Games", "Users", "QR & Logs", "Co-Admins")
        else listOf("Overview", reqLabel, "Games", "Users", "QR & Logs")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isSuperAdmin) "Main Admin Console" else "Co-Admin Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberGoldTop,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
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
                            text = "Cloud Synced • Live Engine & Team Controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = NeonEmeraldBright,
                    modifier = Modifier
                        .padding(start = 6.dp, end = 10.dp)
                        .size(26.dp)
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
                                title = "PENDING REQUESTS",
                                value = "$pendingRequestsCount Pending",
                                accent = if (pendingRequestsCount > 0) RubyRedBright else NeonEmeraldBright,
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
                // Deposit & Withdrawal Requests Verification Queue
                item {
                    var reqFilter by remember { mutableIntStateOf(0) } // 0=Pending, 1=Deposits, 2=Withdrawals, 3=All
                    val filteredRequests = remember(walletRequests, reqFilter) {
                        when (reqFilter) {
                            0 -> walletRequests.filter { it.status == "PENDING" }
                            1 -> walletRequests.filter { it.requestType == "DEPOSIT" }
                            2 -> walletRequests.filter { it.requestType == "WITHDRAWAL" }
                            else -> walletRequests
                        }
                    }
                    val clipboard = LocalClipboardManager.current

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Deposit & Withdrawal Verification",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Pending ($pendingRequestsCount)", "Deposits", "Withdrawals", "All").forEachIndexed { idx, label ->
                                val active = reqFilter == idx
                                Surface(
                                    onClick = { reqFilter = idx },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) AmberOrangeMid else ObsidianCard,
                                    border = BorderStroke(1.dp, if (active) AmberGoldTop else ObsidianBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        if (filteredRequests.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No matching deposit or withdrawal requests right now.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            filteredRequests.forEach { req ->
                                val isDeposit = req.requestType == "DEPOSIT"
                                val isPending = req.status == "PENDING"
                                val accent = when (req.status) {
                                    "APPROVED", "VERIFIED" -> NeonEmeraldBright
                                    "REJECTED" -> RubyRedBright
                                    else -> AmberGoldTop
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianCard,
                                    border = BorderStroke(1.2.dp, accent.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
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
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isDeposit) NeonEmerald.copy(alpha = 0.2f) else RubyRed.copy(alpha = 0.22f)
                                                ) {
                                                    Text(
                                                        text = if (isDeposit) "DEPOSIT" else "WITHDRAW",
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDeposit) NeonEmeraldBright else RubyRedBright,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${formatCoins(req.amountCoins)} VC",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    color = CoinGoldSoft
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = accent.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = req.status,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${req.userName} • ${req.publicUserId}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${req.userEmail} • ${formatTimestamp(req.createdAtMs)}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )

                                        // Copyable UTR / UPI ID / Paytm Number box
                                        Surface(
                                            onClick = { clipboard.setText(AnnotatedString(req.paymentDetails)) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = ObsidianCanvas,
                                            border = BorderStroke(1.dp, ObsidianBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = when (req.paymentMethod) {
                                                            "QR_UPI" -> "QR Payment UTR Reference"
                                                            "PAYTM" -> "User Paytm Number"
                                                            else -> "User UPI ID"
                                                        },
                                                        fontSize = 10.sp,
                                                        color = TextSecondary
                                                    )
                                                    Text(
                                                        text = req.paymentDetails,
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = CyanPulse
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy details",
                                                    tint = AmberGoldTop,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (isPending) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (isDeposit) {
                                                            onApproveDeposit(req.id, "QR payment verified & coins credited")
                                                        } else {
                                                            onVerifyWithdrawal(req.id, "Withdrawal verified & paid via ${req.paymentMethod}")
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.White),
                                                    modifier = Modifier
                                                        .weight(1.3f)
                                                        .testTag("admin_approve_req_${req.id}")
                                                ) {
                                                    Text(
                                                        text = if (isDeposit) "Approve & Add Coins" else "Verify & Mark Paid",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        val msg = if (isDeposit) "QR payment not verified" else "Declined & coins refunded"
                                                        onRejectWalletRequest(req.id, msg)
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, RubyRed),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RubyRedBright),
                                                    modifier = Modifier
                                                        .weight(0.9f)
                                                        .testTag("admin_reject_req_${req.id}")
                                                ) {
                                                    Text(
                                                        text = if (isDeposit) "Reject" else "Refund",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        } else if (!req.adminNote.isNullOrBlank()) {
                                            Text(
                                                text = "Processed: ${req.adminNote}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
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

                        // Live Per-Arena WinGo Result Controller (30s, 1m, 3m, 5m • 0-9 Numbers, Big/Small, Colors)
                        var selectedControlMode by remember { mutableStateOf(GameMode.MODE_30S) }
                        val currentForcedDigit = appConfig.forcedDigitForMode(selectedControlMode)

                        fun applyDigitForMode(mode: GameMode, digit: Int?, label: String) {
                            val updated = when (mode) {
                                GameMode.MODE_30S -> appConfig.copy(forced30sDigit = digit, forcedNextColor = null)
                                GameMode.MODE_60S -> appConfig.copy(forced60sDigit = digit, forcedNextColor = null)
                                GameMode.MODE_3M -> appConfig.copy(forced3mDigit = digit, forcedNextColor = null)
                                GameMode.MODE_5M -> appConfig.copy(forced5mDigit = digit, forcedNextColor = null)
                            }
                            onSaveConfig(updated, "${mode.title}: $label")
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.5.dp, AmberGoldTop.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Live WinGo Result Controller (0–9 / Big-Small)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AmberGoldTop
                                )
                                Text(
                                    text = "Step 1: Select 30s / 1m / 3m / 5m arena. Step 2: Tap any Number (0–9), Big/Small, or Color — next round lands on that exact result for ALL players!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                // Step 1: 4 Arena Mode Pills with live active badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    GameMode.entries.forEach { mode ->
                                        val isModeSelected = selectedControlMode == mode
                                        val modeDigit = appConfig.forcedDigitForMode(mode)
                                        Surface(
                                            onClick = { selectedControlMode = mode },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isModeSelected) AmberOrangeMid else ObsidianCanvas,
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    isModeSelected -> AmberGoldTop
                                                    modeDigit != null -> NeonEmerald
                                                    else -> ObsidianBorder
                                                }
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("admin_mode_ctrl_${mode.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = mode.shortLabel,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isModeSelected) Color.White else TextPrimary
                                                )
                                                Text(
                                                    text = modeDigit?.let { "#$it" } ?: "Auto",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isModeSelected -> Color.White.copy(alpha = 0.92f)
                                                        modeDigit != null -> NeonEmeraldBright
                                                        else -> TextSecondary
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Live status indicator for selected arena
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ObsidianCanvas,
                                    border = BorderStroke(
                                        1.dp,
                                        if (currentForcedDigit != null) NeonEmerald.copy(alpha = 0.7f) else ObsidianBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val statusText = if (currentForcedDigit != null) {
                                        val col = ColorPredictionEngine.digitToColor(currentForcedDigit).displayName
                                        val bs = digitBigSmallLabel(currentForcedDigit)
                                        "Next ${selectedControlMode.title} Result: #$currentForcedDigit ($col • $bs)"
                                    } else {
                                        "Next ${selectedControlMode.title} Result: Auto Fair (SHA-256)"
                                    }
                                    Text(
                                        text = statusText,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentForcedDigit != null) NeonEmeraldBright else CoinGoldSoft,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }

                                // Step 2: Tap Exact Number 0-9 (2 rows of 5 glossy balls)
                                Text(
                                    text = "Tap Exact Winning Number (0–9)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                listOf(0..4, 5..9).forEach { range ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        range.forEach { digit ->
                                            val picked = currentForcedDigit == digit
                                            WinGoNumberBall(
                                                digit = digit,
                                                size = 50.dp,
                                                isSelected = picked,
                                                onClick = {
                                                    val col = ColorPredictionEngine.digitToColor(digit).displayName
                                                    val bs = digitBigSmallLabel(digit)
                                                    applyDigitForMode(selectedControlMode, digit, "Forced #$digit ($col • $bs)")
                                                },
                                                modifier = Modifier.testTag("admin_force_digit_$digit")
                                            )
                                        }
                                    }
                                }

                                // Step 3: Quick Big / Small / Color / Auto Presets
                                Text(
                                    text = "Quick Win Presets (${selectedControlMode.shortLabel})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdminQuickPresetButton(
                                        label = "Auto Fair",
                                        active = currentForcedDigit == null,
                                        accent = CyanPulse,
                                        onClick = { applyDigitForMode(selectedControlMode, null, "Auto Fair SHA-256") },
                                        modifier = Modifier.weight(1f).testTag("admin_preset_auto")
                                    )
                                    AdminQuickPresetButton(
                                        label = "Big (#7)",
                                        active = currentForcedDigit != null && currentForcedDigit >= 5,
                                        accent = AmberOrangeMid,
                                        onClick = { applyDigitForMode(selectedControlMode, 7, "Forced Big #7 (Green)") },
                                        modifier = Modifier.weight(1f).testTag("admin_preset_big")
                                    )
                                    AdminQuickPresetButton(
                                        label = "Small (#2)",
                                        active = currentForcedDigit != null && currentForcedDigit <= 4,
                                        accent = Color(0xFF3B82F6),
                                        onClick = { applyDigitForMode(selectedControlMode, 2, "Forced Small #2 (Red)") },
                                        modifier = Modifier.weight(1f).testTag("admin_preset_small")
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdminQuickPresetButton(
                                        label = "Green (#3)",
                                        active = currentForcedDigit in listOf(1, 3, 7, 9),
                                        accent = NeonEmerald,
                                        onClick = { applyDigitForMode(selectedControlMode, 3, "Forced Green #3") },
                                        modifier = Modifier.weight(1f).testTag("admin_preset_green")
                                    )
                                    AdminQuickPresetButton(
                                        label = "Violet (#5)",
                                        active = currentForcedDigit in listOf(0, 5),
                                        accent = ElectricViolet,
                                        onClick = { applyDigitForMode(selectedControlMode, 5, "Forced Violet #5 (Big)") },
                                        modifier = Modifier.weight(1f).testTag("admin_preset_violet")
                                    )
                                    AdminQuickPresetButton(
                                        label = "Red (#4)",
                                        active = currentForcedDigit in listOf(2, 4, 6, 8),
                                        accent = RubyRed,
                                        onClick = { applyDigitForMode(selectedControlMode, 4, "Forced Red #4 (Small)") },
                                        modifier = Modifier.weight(1f).testTag("admin_preset_red")
                                    )
                                }

                                // Bulk Controls: Apply current number to ALL 4 Arenas or Reset All
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val d = currentForcedDigit ?: 5
                                            onSaveConfig(
                                                appConfig.copy(
                                                    forcedNextColor = null,
                                                    forced30sDigit = d,
                                                    forced60sDigit = d,
                                                    forced3mDigit = d,
                                                    forced5mDigit = d
                                                ),
                                                "Applied #$d to ALL 4 WinGo Arenas"
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeMid, contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Apply #${currentForcedDigit ?: 5} to All 4",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onSaveConfig(
                                                appConfig.copy(
                                                    forcedNextColor = null,
                                                    forced30sDigit = null,
                                                    forced60sDigit = null,
                                                    forced3mDigit = null,
                                                    forced5mDigit = null
                                                ),
                                                "Reset ALL 4 WinGo Arenas to Auto Fair"
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, ObsidianBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Reset All Auto",
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Step 4: 3D Spin & Win Exact Result Controller (0..7 matching WHEEL_SLICES)
                        val spinOptions = remember {
                            listOf(
                                null to "Auto Fair",
                                2 to "0 VC",
                                0 to "50 VC",
                                4 to "100 VC",
                                5 to "150 VC",
                                7 to "200 VC",
                                1 to "250 VC",
                                3 to "500 VC",
                                6 to "1000 VC"
                            )
                        }
                        val activeSpinLabel = spinOptions.firstOrNull { it.first == appConfig.forcedSpinSliceIndex }?.second ?: "Auto Fair"

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.5.dp, ElectricViolet.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "3D Spin & Win Result Controller",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricVioletBright
                                )
                                Text(
                                    text = "Active Spin Outcome: $activeSpinLabel (tap any prize below — wheel lands directly on that exact slice!)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = if (appConfig.forcedSpinSliceIndex != null) NeonEmeraldBright else CoinGoldSoft
                                )

                                spinOptions.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { (sliceIdx, label) ->
                                            val active = appConfig.forcedSpinSliceIndex == sliceIdx
                                            Surface(
                                                onClick = {
                                                    onSaveConfig(
                                                        appConfig.copy(forcedSpinSliceIndex = sliceIdx),
                                                        "3D Spin Wheel forced result: $label"
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (active) ElectricViolet else ObsidianCanvas,
                                                border = BorderStroke(1.dp, if (active) ElectricVioletBright else ObsidianBorder),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("admin_spin_slice_${sliceIdx ?: "auto"}")
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (active) Color.White else TextSecondary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 10.dp)
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

            3 -> {
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

            4 -> {
                // Cloud Deposit QR + Gift Voucher QR Generator + Audit Logs
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
                                    Text("Global Deposit QR & UPI Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                }
                                QrMatrixCanvas(
                                    payload = buildScannableQrPayload(qrPayloadDraft, qrLabelDraft),
                                    size = 152.dp
                                )
                                OutlinedTextField(
                                    value = qrLabelDraft,
                                    onValueChange = { if (it.length <= 80) qrLabelDraft = it },
                                    label = { Text("Deposit Counter Name") },
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
                                    label = { Text("Admin UPI ID (e.g. name@ybl) or QR Link") },
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
                                            "Updated global Deposit QR / UPI ID"
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeMid, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sync Deposit QR to All Users", fontWeight = FontWeight.Bold)
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

            5 -> {
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

@Composable
private fun AdminQuickPresetButton(
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (active) accent else ObsidianCanvas,
        border = BorderStroke(1.dp, if (active) accent else ObsidianBorder),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (active) Color.White else TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
        )
    }
}
