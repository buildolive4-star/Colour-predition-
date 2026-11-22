package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.LeaderboardEntry
import com.example.domain.LeaderboardPeriodFilter
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.formatCoins
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinGoldSoft
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmeraldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    currentUserId: String,
    onBack: (() -> Unit)? = null
) {
    var periodFilter by remember { mutableStateOf(LeaderboardPeriodFilter.ALL_TIME) }

    val filteredEntries = remember(entries, periodFilter) {
        val now = System.currentTimeMillis()
        val cutoff = when (periodFilter) {
            LeaderboardPeriodFilter.DAILY -> now - 86_400_000L
            LeaderboardPeriodFilter.WEEKLY -> now - (7L * 86_400_000L)
            LeaderboardPeriodFilter.MONTHLY -> now - (30L * 86_400_000L)
            LeaderboardPeriodFilter.ALL_TIME -> 0L
        }
        entries.filter { entry ->
            if (cutoff == 0L) true
            else entry.updatedAtMs >= cutoff
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (onBack != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("leaderboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "Rankings & Leaderboard",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Trophy Banner
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_trophy_banner),
                        contentDescription = stringResource(R.string.cd_trophy_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to ObsidianCanvas.copy(alpha = 0.92f),
                                    0.65f to ObsidianCanvas.copy(alpha = 0.65f),
                                    1.0f to ObsidianCanvas.copy(alpha = 0.35f)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CoinGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "VIRTUAL POINTS RANKINGS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoinGoldSoft,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Global Leaderboard",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Compete for streaks & virtual points — entertainment only",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Period Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LeaderboardPeriodFilter.entries) { filter ->
                    FilterChip(
                        selected = periodFilter == filter,
                        onClick = { periodFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                            selectedLabelColor = ElectricVioletBright,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = periodFilter == filter,
                            borderColor = ObsidianBorder,
                            selectedBorderColor = ElectricViolet
                        ),
                        modifier = Modifier.testTag("leaderboard_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // Top 3 Podium
        if (filteredEntries.isNotEmpty()) {
            item {
                val top3 = filteredEntries.take(3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    top3.forEachIndexed { index, entry ->
                        val rank = index + 1
                        PodiumCard(
                            rank = rank,
                            entry = entry,
                            isCurrentUser = entry.userId == currentUserId,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            item {
                EmptyStatePanel(
                    title = "No players in this window yet",
                    subtitle = "Play any live round to climb the virtual points leaderboard."
                )
            }
        }

        // Ranked List
        itemsIndexed(filteredEntries, key = { _, item -> item.userId.ifBlank { item.id } }) { idx, entry ->
            val rank = idx + 1
            val isMe = entry.userId == currentUserId
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isMe) ElectricViolet.copy(alpha = 0.16f) else ObsidianCard,
                border = BorderStroke(
                    1.dp,
                    if (isMe) ElectricVioletBright else ObsidianBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "#$rank",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = when (rank) {
                                1 -> CoinGold
                                2 -> Color(0xFFE2E8F0)
                                3 -> Color(0xFFF59E0B)
                                else -> TextSecondary
                            }
                        )
                        PlayerAvatar(
                            displayName = entry.displayName,
                            photoUrl = entry.photoUrl,
                            size = 40
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.badgeTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanPulse
                                )
                                Text(
                                    text = "• ${entry.totalWins}/${entry.totalGames} Wins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${formatCoins(entry.totalVirtualPoints)} pts",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CoinGoldSoft
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = RubyRedBright,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Best ${entry.bestStreak}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
private fun PodiumCard(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val crownColor = when (rank) {
        1 -> CoinGold
        2 -> Color(0xFFCBD5E1)
        else -> Color(0xFFD97706)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isCurrentUser) ObsidianCardElevated else ObsidianCard,
        border = BorderStroke(1.dp, crownColor.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = crownColor,
                modifier = Modifier.size(20.dp)
            )
            PlayerAvatar(displayName = entry.displayName, photoUrl = entry.photoUrl, size = 44)
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatCoins(entry.totalVirtualPoints)} pts",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = NeonEmeraldBright
            )
            Text(
                text = entry.badgeTitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlayerAvatar(
    displayName: String,
    photoUrl: String?,
    size: Int
) {
    val initials = remember(displayName) {
        displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "CP" }
    }

    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = stringResource(R.string.cd_profile_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(ElectricViolet, CyanPulse))
                )
        ) {
            Text(
                text = initials,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.36f).sp,
                color = Color.White
            )
        }
    }
}
