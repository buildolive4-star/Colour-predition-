package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReferralClaimEntity
import com.example.data.UserProfile
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.WinCelebrationDialog
import com.example.ui.components.WinPopupData
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PromotionScreen(
    profile: UserProfile?,
    myReferrals: List<ReferralClaimEntity>,
    onApplyReferralCode: (String, (String) -> Unit) -> Unit,
    onCollectReferralBonus: (String, (Long) -> Unit) -> Unit,
    onCollectAllReferralBonuses: ((Long) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val referralCode = profile?.publicUserId?.ifBlank { "CP-000000" } ?: "CP-000000"
    val promoLink = "https://chromapulse.app/invite?ref=$referralCode"
    val shareMessage = buildString {
        append("Join ChromaPulse WinGo, 3D Spin Wheel, Mines & Dice! ")
        append("Use my Referral Code: $referralCode and get +500 VC Bonus instantly!\n")
        append("Invite Link: $promoLink")
    }

    val pendingClaims = remember(myReferrals) { myReferrals.filter { !it.referrerRewardClaimed } }
    val pendingTotalCoins = remember(pendingClaims) { pendingClaims.sumOf { it.bonusCoins } }
    val collectedCount = remember(myReferrals) { myReferrals.count { it.referrerRewardClaimed } }

    var inputCode by remember { mutableStateOf("") }
    var winPopupData by remember { mutableStateOf<WinPopupData?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("promotion_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Promotion & Referrals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Invite Friends • Collect +500 VC per Referral",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoinGoldSoft
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = AmberOrangeMid.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.65f))
            ) {
                Text(
                    text = "+500 VC",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = AmberGoldTop,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ShreeWin-Style Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ElectricViolet, Color(0xFF4C1D95), AmberOrangeDeep)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AmberGoldTop.copy(alpha = 0.25f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CardGiftcard,
                                    contentDescription = null,
                                    tint = AmberGoldTop,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "REFER FRIENDS & EARN",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CoinGoldSoft
                                )
                                Text(
                                    text = "+500 VC Every Invite!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "Share your unique Promotion Link or Referral Code. Both you and your friend receive +500 Virtual Coins when they join with your code!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PromoMiniStat(
                                label = "Total Invites",
                                value = "${myReferrals.size}",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            PromoMiniStat(
                                label = "Pending Collect",
                                value = "${formatCoins(pendingTotalCoins)} VC",
                                color = AmberGoldTop,
                                modifier = Modifier.weight(1f)
                            )
                            PromoMiniStat(
                                label = "Collected",
                                value = "${formatCoins(profile?.referralEarnedCoins ?: (collectedCount * 500L))} VC",
                                color = NeonEmeraldBright,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Pending "+500 VC Collect" Banner Card (Highlighted whenever rewards are ready to collect)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianCard,
                    border = BorderStroke(
                        1.5.dp,
                        if (pendingClaims.isNotEmpty()) NeonEmerald else ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    imageVector = Icons.Filled.Redeem,
                                    contentDescription = null,
                                    tint = if (pendingClaims.isNotEmpty()) NeonEmeraldBright else CoinGoldSoft
                                )
                                Column {
                                    Text(
                                        text = "Referral Bonus Vault",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (pendingClaims.isNotEmpty()) {
                                            "${pendingClaims.size} friend(s) joined • Ready to collect!"
                                        } else {
                                            "Share your link below to earn +500 VC per friend"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (pendingClaims.isNotEmpty()) NeonEmeraldBright else TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "+${formatCoins(pendingTotalCoins)} VC",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (pendingClaims.isNotEmpty()) NeonEmeraldBright else TextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                onCollectAllReferralBonuses { total ->
                                    winPopupData = WinPopupData(
                                        title = "Referral Bonus Collected!",
                                        amountWon = total,
                                        subtitle = "Your +500 VC per friend referral reward has been added to your wallet!",
                                        badgeText = "PROMOTION REWARD"
                                    )
                                }
                            },
                            enabled = pendingClaims.isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = Color.White,
                                disabledContainerColor = ObsidianCardElevated,
                                disabledContentColor = TextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("promo_collect_all_button")
                        ) {
                            Icon(Icons.Filled.Stars, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pendingClaims.isNotEmpty()) {
                                    "Collect +${formatCoins(pendingTotalCoins)} VC Now"
                                } else {
                                    "0 Pending • Invite Friends for +500 VC"
                                },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Your Promotion Link & Referral Code Generator Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = AmberGoldTop)
                            Text(
                                text = "Your Promotion Link & Invite Code",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Referral Code Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianCanvas,
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("REFERRAL CODE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = referralCode,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = CoinGoldSoft
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(referralCode))
                                        onShowMessage("Referral Code copied: $referralCode")
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, AmberGoldTop),
                                    modifier = Modifier.testTag("promo_copy_code_button")
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = AmberGoldTop, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Code", color = AmberGoldTop, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Promotion Link Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianCanvas,
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("PROMOTION INVITE LINK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = promoLink,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = CyanPulse,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Copy Link + Share Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(promoLink))
                                    onShowMessage("Promotion link copied!")
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, CyanPulse),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("promo_copy_link_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = CyanPulse, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Link", color = CyanPulse, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "ChromaPulse +500 VC Invite")
                                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Promotion Link"))
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberOrangeMid,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("promo_share_link_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Link", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Enter Friend's Referral Code (+500 VC Welcome Bonus)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.6f)),
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
                            Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = ElectricVioletBright)
                            Text(
                                text = "Have a Friend's Referral Code?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (!profile?.referredByCode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeonEmerald.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NeonEmeraldBright)
                                    Text(
                                        text = "Applied Code: ${profile?.referredByCode} (+500 VC Claimed)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonEmeraldBright
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Enter your friend's code (e.g. CP-C03732) or paste their invite link to instantly collect +500 VC!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it.take(120) },
                                label = { Text("Referral Code or Invite Link") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricVioletBright,
                                    unfocusedBorderColor = ObsidianBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("promo_enter_code_input")
                            )
                            Button(
                                onClick = {
                                    val codeToSubmit = inputCode.trim()
                                    if (codeToSubmit.isNotBlank()) {
                                        onApplyReferralCode(codeToSubmit) { referrerName ->
                                            inputCode = ""
                                            winPopupData = WinPopupData(
                                                title = "Referral Bonus Claimed!",
                                                amountWon = 500L,
                                                subtitle = "Invited by $referrerName • +500 VC credited to your wallet!",
                                                badgeText = "WELCOME REFERRAL"
                                            )
                                        }
                                    }
                                },
                                enabled = inputCode.trim().length >= 3,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricViolet,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("promo_apply_code_button")
                            ) {
                                Text("Apply & Collect +500 VC", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Invited Friends History List
            item {
                Text(
                    text = "My Invited Friends (${myReferrals.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (myReferrals.isEmpty()) {
                item {
                    EmptyStatePanel(
                        title = "No referrals yet",
                        subtitle = "Share your Promotion Link on WhatsApp or Telegram. Every friend who enters your code unlocks +500 VC for you to collect!"
                    )
                }
            } else {
                items(myReferrals, key = { it.referredUid.ifBlank { it.id } }) { claim ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (!claim.referrerRewardClaimed) NeonEmerald.copy(alpha = 0.7f) else ObsidianBorder
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
                                Text(
                                    text = claim.referredName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Joined • ${formatTimestamp(claim.createdAtMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            if (!claim.referrerRewardClaimed) {
                                Button(
                                    onClick = {
                                        val targetUid = claim.referredUid.ifBlank { claim.id }
                                        onCollectReferralBonus(targetUid) { bonus ->
                                            winPopupData = WinPopupData(
                                                title = "Referral Reward Collected!",
                                                amountWon = bonus,
                                                subtitle = "Friend ${claim.referredName} joined using your code!",
                                                badgeText = "+500 VC COLLECTED"
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonEmerald,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("promo_collect_item_${claim.referredUid}")
                                ) {
                                    Text(
                                        text = "Collect +${claim.bonusCoins} VC",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = NeonEmerald.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "COLLECTED +${claim.bonusCoins}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonEmeraldBright,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }

    winPopupData?.let { data ->
        WinCelebrationDialog(
            data = data,
            onDismiss = { winPopupData = null }
        )
    }
}

@Composable
private fun PromoMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.28f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
