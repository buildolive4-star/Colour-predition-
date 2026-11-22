package com.example.ui.components

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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfigEntity
import com.example.data.UserProfile
import com.example.data.VirtualVoucherEntity
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeDeep
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.security.MessageDigest

/**
 * Deterministic 25x25 QR-style matrix rendered directly on Jetpack Compose Canvas
 * with authentic 7x7 finder patterns, alignment pattern, timing bars, and SHA-256 payload modules.
 */
@Composable
fun QrMatrixCanvas(
    payload: String,
    size: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val modules = remember(payload) { buildDeterministicQrMatrix(payload) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val grid = modules.size
            val cell = this.size.minDimension / grid.toFloat()
            val darkInk = Color(0xFF10142B)

            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    if (modules[r][c]) {
                        drawRect(
                            color = darkInk,
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell + 0.4f, cell + 0.4f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildDeterministicQrMatrix(payload: String): Array<BooleanArray> {
    val n = 25
    val grid = Array(n) { BooleanArray(n) }
    val reserved = Array(n) { BooleanArray(n) }

    fun placeFinder(top: Int, left: Int) {
        for (r in -1..7) {
            for (c in -1..7) {
                val rr = top + r
                val cc = left + c
                if (rr in 0 until n && cc in 0 until n) {
                    reserved[rr][cc] = true
                    val isBorder = r in 0..6 && c in 0..6 && (r == 0 || r == 6 || c == 0 || c == 6)
                    val isInner = r in 2..4 && c in 2..4
                    grid[rr][cc] = isBorder || isInner
                }
            }
        }
    }

    placeFinder(0, 0)
    placeFinder(0, n - 7)
    placeFinder(n - 7, 0)

    // Alignment pattern at (16, 16)
    for (r in -2..2) {
        for (c in -2..2) {
            val rr = 18 + r
            val cc = 18 + c
            if (rr in 0 until n && cc in 0 until n) {
                reserved[rr][cc] = true
                grid[rr][cc] = (kotlin.math.abs(r) == 2 || kotlin.math.abs(c) == 2 || (r == 0 && c == 0))
            }
        }
    }

    // Timing patterns
    for (i in 8 until n - 8) {
        reserved[6][i] = true
        grid[6][i] = i % 2 == 0
        reserved[i][6] = true
        grid[i][6] = i % 2 == 0
    }

    // Fill data modules deterministically from SHA-256 stream of payload
    val digest = MessageDigest.getInstance("SHA-256")
    var hashBlock = digest.digest(payload.toByteArray(Charsets.UTF_8))
    var bitIdx = 0

    for (r in 0 until n) {
        for (c in 0 until n) {
            if (!reserved[r][c]) {
                val bytePos = (bitIdx / 8) % hashBlock.size
                if (bitIdx > 0 && bitIdx % (hashBlock.size * 8) == 0) {
                    hashBlock = digest.digest(hashBlock)
                }
                val bit = (hashBlock[bytePos].toInt() shr (bitIdx % 8)) and 1
                grid[r][c] = bit == 1
                bitIdx++
            }
        }
    }
    return grid
}

@Composable
fun QrCenterScreen(
    profile: UserProfile?,
    appConfig: AppConfigEntity,
    vouchers: List<VirtualVoucherEntity>,
    onRedeemVoucher: (String) -> Unit,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = My QR & Counter, 1 = Redeem Voucher QR
    var voucherInput by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val playerId = profile?.publicUserId ?: "CP-DEMO01"
    val playerQrPayload = "CHROMAPULSE:PLAYER:$playerId:${profile?.displayName ?: "Player"}"

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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("qr_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column {
                        Text(
                            text = "Virtual Coin QR Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberGoldTop
                        )
                        Text(
                            text = "Player QR • Demo Counter QR • Gift Vouchers",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = null,
                    tint = AmberGoldTop,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(28.dp)
                )
            }
        }

        // Virtual Disclaimer
        item {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                VirtualDisclaimerBanner()
            }
        }

        // 2-Tab Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("My QR & Counter", "Voucher QR Redeem").forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) Brush.horizontalGradient(listOf(AmberGoldTop, AmberOrangeMid))
                                else Brush.horizontalGradient(listOf(ObsidianCard, ObsidianCard))
                            )
                            .clickable { selectedTab = idx }
                            .testTag("qr_tab_$idx")
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

        if (selectedTab == 0) {
            // Player Personal QR Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "MY PLAYER ID QR",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AmberGoldTop
                        )
                        QrMatrixCanvas(payload = playerQrPayload, size = 196.dp)
                        Text(
                            text = "${profile?.displayName ?: "Chroma Player"} • $playerId",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Balance: ${formatCoins(profile?.virtualCoins ?: 0L)} Virtual Coins",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CoinGoldSoft
                        )
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(playerId))
                                copiedNotice = "Copied $playerId"
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeMid, contentColor = Color.White),
                            modifier = Modifier.testTag("qr_copy_player_id")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(copiedNotice ?: "Copy Player ID ($playerId)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Admin Configurable Demo Counter QR
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = appConfig.adminQrLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Configurable live from Admin Panel (Virtual Demo Only)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        QrMatrixCanvas(payload = appConfig.adminQrPayload, size = 168.dp)
                        Text(
                            text = appConfig.adminQrPayload,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = CyanPulse,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Manual Voucher Code Entry
            item {
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
                        Text(
                            text = "Redeem Virtual Coin Voucher Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        OutlinedTextField(
                            value = voucherInput,
                            onValueChange = { voucherInput = it.uppercase() },
                            singleLine = true,
                            placeholder = { Text("Enter code e.g. CP-WELCOME-500", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGoldTop,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_voucher_input")
                        )
                        Button(
                            onClick = {
                                if (voucherInput.isNotBlank()) {
                                    onRedeemVoucher(voucherInput)
                                    voucherInput = ""
                                }
                            },
                            enabled = voucherInput.isNotBlank(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("qr_redeem_submit_button")
                        ) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Claim Virtual Coins", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Available QR Vouchers List (Pre-seeded + Admin Generated)
            item {
                Text(
                    text = "Available & Redeemed QR Vouchers (${vouchers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            items(vouchers, key = { it.code }) { voucher ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(
                        1.dp,
                        if (voucher.redeemed) ObsidianBorder else NeonEmerald.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QrMatrixCanvas(
                            payload = "CHROMAPULSE:VOUCHER:${voucher.code}:${voucher.coinsAmount}",
                            size = 76.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = voucher.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = voucher.code,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AmberGoldTop
                            )
                            Text(
                                text = "+${formatCoins(voucher.coinsAmount)} Virtual Coins",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeonEmeraldBright
                            )
                        }
                        if (voucher.redeemed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Text("Used", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                        } else {
                            Button(
                                onClick = { onRedeemVoucher(voucher.code) },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberOrangeDeep, contentColor = Color.White),
                                modifier = Modifier.testTag("qr_claim_${voucher.code}")
                            ) {
                                Text("Claim", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
