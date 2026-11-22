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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfigEntity
import com.example.data.UserProfile
import com.example.data.WalletRequestEntity
import com.example.ui.components.QrMatrixCanvas
import com.example.ui.components.formatCoins
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeMid
import com.example.ui.theme.CoinGoldSoft
import com.example.ui.theme.CyanPulse
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

fun buildScannableQrPayload(rawPayload: String, label: String): String {
    val trimmed = rawPayload.trim()
    if (trimmed.startsWith("upi://", ignoreCase = true)) return trimmed
    if (trimmed.contains("@") && !trimmed.contains(" ")) {
        val safeLabel = label.trim().ifBlank { "ChromaPulse" }.replace(" ", "%20")
        return "upi://pay?pa=$trimmed&pn=$safeLabel&cu=INR"
    }
    return trimmed.ifBlank { "CHROMAPULSE:COUNTER-01" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositModalSheet(
    appConfig: AppConfigEntity,
    userRequests: List<WalletRequestEntity>,
    onSubmitDeposit: (amount: Long, utrRef: String, onDone: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var amountText by remember { mutableStateOf("500") }
    var utrInput by remember { mutableStateOf("") }
    var copiedBanner by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val depositRequests = remember(userRequests) {
        userRequests.filter { it.requestType == "DEPOSIT" }
    }
    val scannablePayload = remember(appConfig.adminQrPayload, appConfig.adminQrLabel) {
        buildScannableQrPayload(appConfig.adminQrPayload, appConfig.adminQrLabel)
    }
    val parsedAmount = amountText.toLongOrNull() ?: 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = NeonEmeraldBright)
                Text(
                    text = "Deposit • Add Coins via QR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            // Admin Live QR Code Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.5.dp, NeonEmerald.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null, tint = AmberGoldTop, modifier = Modifier.size(18.dp))
                        Text(
                            text = appConfig.adminQrLabel.ifBlank { "Admin Payment QR" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberGoldTop
                        )
                    }

                    QrMatrixCanvas(payload = scannablePayload, size = 164.dp)

                    Surface(
                        onClick = {
                            clipboard.setText(AnnotatedString(appConfig.adminQrPayload))
                            copiedBanner = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCanvas,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.testTag("deposit_copy_qr_id")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = appConfig.adminQrPayload,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = CoinGoldSoft
                            )
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Payment ID",
                                tint = AmberGoldTop,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = if (copiedBanner) "Copied! Pay via QR/UPI, then enter Amount & UTR below."
                        else "Scan Admin QR above to pay, then submit Amount & UTR Reference so Admin can add coins.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (copiedBanner) NeonEmeraldBright else TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Quick Amount Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100L, 200L, 500L, 1000L, 2500L).forEach { chip ->
                    val selected = parsedAmount == chip
                    Surface(
                        onClick = { amountText = chip.toString() },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) NeonEmerald else ObsidianCard,
                        border = BorderStroke(1.dp, if (selected) NeonEmeraldBright else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$chip",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { text -> amountText = text.filter { it.isDigit() }.take(6) },
                label = { Text("Deposit Coins Amount (Min 10)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonEmeraldBright,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deposit_amount_input")
            )

            OutlinedTextField(
                value = utrInput,
                onValueChange = { if (it.length <= 60) utrInput = it },
                label = { Text("UTR / Transaction Reference ID") },
                placeholder = { Text("e.g. 432518923456 or Paid via PhonePe") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonEmeraldBright,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deposit_utr_input")
            )

            Button(
                onClick = {
                    if (!isSubmitting && parsedAmount >= 10L && utrInput.trim().length >= 4) {
                        isSubmitting = true
                        onSubmitDeposit(parsedAmount, utrInput.trim()) { ok ->
                            isSubmitting = false
                            if (ok) utrInput = ""
                        }
                    }
                },
                enabled = !isSubmitting && parsedAmount >= 10L && utrInput.trim().length >= 4,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonEmerald,
                    contentColor = Color.White,
                    disabledContainerColor = ObsidianCardElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("deposit_submit_button")
            ) {
                Text(
                    text = if (isSubmitting) "Submitting..." else "Submit Deposit Request • ${formatCoins(parsedAmount)} VC",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (depositRequests.isNotEmpty()) {
                Text(
                    text = "My Deposit Requests (${depositRequests.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                depositRequests.take(8).forEach { req ->
                    WalletRequestStatusCard(req = req)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawModalSheet(
    profile: UserProfile?,
    userRequests: List<WalletRequestEntity>,
    onSubmitWithdraw: (amount: Long, method: String, details: String, onDone: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableCoins = profile?.virtualCoins ?: 0L
    var method by remember { mutableStateOf("UPI_ID") } // UPI_ID or PAYTM
    var detailsInput by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("100") }
    var isSubmitting by remember { mutableStateOf(false) }

    val withdrawalRequests = remember(userRequests) {
        userRequests.filter { it.requestType == "WITHDRAWAL" }
    }
    val parsedAmount = amountText.toLongOrNull() ?: 0L
    val canSubmit = !isSubmitting && parsedAmount >= 50L && parsedAmount <= availableCoins && detailsInput.trim().length >= 5

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = RubyRedBright)
                Text(
                    text = "Withdraw • UPI / Paytm",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            // Available Balance Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, RubyRed.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WITHDRAWABLE BALANCE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${formatCoins(availableCoins)} VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = CoinGoldSoft
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = AmberGoldTop,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Payment Method Switch: UPI ID vs Paytm Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("UPI_ID" to "UPI ID", "PAYTM" to "Paytm Number").forEach { (key, label) ->
                    val selected = method == key
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) AmberOrangeMid else ObsidianCard)
                            .clickable {
                                method = key
                                detailsInput = ""
                            }
                            .testTag("withdraw_method_$key")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = detailsInput,
                onValueChange = { if (it.length <= 80) detailsInput = it },
                label = {
                    Text(if (method == "PAYTM") "Your Paytm Mobile Number" else "Your UPI ID")
                },
                placeholder = {
                    Text(if (method == "PAYTM") "e.g. 9876543210" else "e.g. yourname@ybl / @paytm")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (method == "PAYTM") KeyboardType.Phone else KeyboardType.Email
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RubyRedBright,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_details_input")
            )

            // Quick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100L, 250L, 500L, 1000L, 2500L).forEach { chip ->
                    val selected = parsedAmount == chip
                    Surface(
                        onClick = { amountText = chip.toString() },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) RubyRed else ObsidianCard,
                        border = BorderStroke(1.dp, if (selected) RubyRedBright else ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$chip",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { text -> amountText = text.filter { it.isDigit() }.take(6) },
                label = { Text("Withdraw Amount (Min 50 VC)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RubyRedBright,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_amount_input")
            )

            Button(
                onClick = {
                    if (canSubmit) {
                        isSubmitting = true
                        onSubmitWithdraw(parsedAmount, method, detailsInput.trim()) { ok ->
                            isSubmitting = false
                            if (ok) detailsInput = ""
                        }
                    }
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RubyRed,
                    contentColor = Color.White,
                    disabledContainerColor = ObsidianCardElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("withdraw_submit_button")
            ) {
                Text(
                    text = when {
                        isSubmitting -> "Submitting..."
                        parsedAmount > availableCoins -> "Insufficient Balance"
                        else -> "Request Withdrawal • ${formatCoins(parsedAmount)} VC"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (withdrawalRequests.isNotEmpty()) {
                Text(
                    text = "My Withdrawal Requests (${withdrawalRequests.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                withdrawalRequests.take(8).forEach { req ->
                    WalletRequestStatusCard(req = req)
                }
            }
        }
    }
}

@Composable
fun WalletRequestStatusCard(req: WalletRequestEntity) {
    val statusColor = when (req.status) {
        "APPROVED", "VERIFIED" -> NeonEmeraldBright
        "REJECTED" -> RubyRedBright
        else -> AmberGoldTop
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (req.requestType == "DEPOSIT") "Deposit" else "Withdrawal"} • ${formatCoins(req.amountCoins)} VC",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (req.status == "APPROVED" || req.status == "VERIFIED") {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = req.status,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = statusColor
                        )
                    }
                }
            }
            Text(
                text = "${req.paymentMethod}: ${req.paymentDetails}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = CyanPulse
            )
            if (!req.adminNote.isNullOrBlank()) {
                Text(
                    text = "Admin: ${req.adminNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = formatTimestamp(req.createdAtMs),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
