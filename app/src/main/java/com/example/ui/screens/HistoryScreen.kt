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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRoundRecord
import com.example.data.PredictionRecord
import com.example.data.WalletTransaction
import com.example.domain.GameMode
import com.example.domain.HistoryDateFilter
import com.example.domain.PredictionColor
import com.example.domain.VerifiableRoundOutcome
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RoundVerificationDialog
import com.example.ui.components.VirtualDisclaimerBanner
import com.example.ui.components.WinGoColorDots
import com.example.ui.components.digitBigSmallLabel
import com.example.ui.components.formatCoins
import com.example.ui.components.formatEpochMs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeDeep
import com.example.ui.theme.AmberOrangeMid
import com.example.ui.theme.CoinGoldSoft
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.RubyRed
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

private data class DisplayRoundRow(
    val id: String,
    val mode: GameMode,
    val periodId: String,
    val endTimeMs: Long,
    val winningColor: PredictionColor,
    val resultDigit: Int,
    val resultHash: String,
    val status: String
)

@Composable
fun HistoryScreen(
    firestoreRounds: List<GameRoundRecord>,
    recentOutcomesByMode: Map<GameMode, List<VerifiableRoundOutcome>>,
    predictions: List<PredictionRecord>,
    walletTransactions: List<WalletTransaction>
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0=Rounds, 1=My Predictions, 2=Wallet Ledger
    var dateFilter by remember { mutableStateOf(HistoryDateFilter.ALL) }
    var selectedModeFilter by remember { mutableStateOf<GameMode?>(null) }
    var inspectedRound by remember { mutableStateOf<DisplayRoundRow?>(null) }

    val mergedRounds = remember(firestoreRounds, recentOutcomesByMode, selectedModeFilter, dateFilter) {
        val byKey = linkedMapOf<String, DisplayRoundRow>()
        firestoreRounds.forEach { rec ->
            val mode = GameMode.fromId(rec.modeId)
            val key = rec.id.ifBlank { "${mode.id}_${rec.periodId}" }
            byKey[key] = DisplayRoundRow(
                id = key,
                mode = mode,
                periodId = rec.periodId,
                endTimeMs = rec.endTimeMs,
                winningColor = PredictionColor.fromRaw(rec.winningColor),
                resultDigit = rec.resultDigit.toInt(),
                resultHash = rec.resultHash,
                status = rec.status
            )
        }
        recentOutcomesByMode.values.flatten().forEach { outcome ->
            byKey.putIfAbsent(
                outcome.roundDocId,
                DisplayRoundRow(
                    id = outcome.roundDocId,
                    mode = outcome.mode,
                    periodId = outcome.periodId,
                    endTimeMs = outcome.endTimeMs,
                    winningColor = outcome.winningColor,
                    resultDigit = outcome.resultDigit,
                    resultHash = outcome.resultHash,
                    status = "COMPLETED"
                )
            )
        }
        byKey.values
            .filter { selectedModeFilter == null || it.mode == selectedModeFilter }
            .filter { matchesDateFilter(it.endTimeMs, dateFilter) }
            .sortedByDescending { it.endTimeMs }
    }

    val filteredPredictions = remember(predictions, selectedModeFilter, dateFilter) {
        predictions
            .filter { selectedModeFilter == null || it.modeId == selectedModeFilter?.id }
            .filter { matchesDateFilter(it.createdAtMs, dateFilter) }
    }

    val filteredTransactions = remember(walletTransactions, dateFilter) {
        walletTransactions
            .filter { matchesDateFilter(it.createdAtMs, dateFilter) }
    }

    val wonCount = remember(filteredPredictions) { filteredPredictions.count { it.status == "WON" } }
    val settledCount = remember(filteredPredictions) { filteredPredictions.count { it.status != "PENDING" } }
    val totalRewards = remember(filteredPredictions) { filteredPredictions.sumOf { it.rewardCoins } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Game & Wallet History",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Verifiable round results, predictions & immutable ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Section Switcher (ShreeWin Sunset Amber active tab)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Game history", "My Picks", "Ledger").forEachIndexed { idx, label ->
                    val active = selectedSection == idx
                    Surface(
                        onClick = { selectedSection = idx },
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) AmberOrangeMid else ObsidianCard,
                        border = BorderStroke(1.dp, if (active) AmberGoldTop else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_tab_$idx")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp)
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
        }

        // Date Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryDateFilter.entries) { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { dateFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberOrangeMid.copy(alpha = 0.25f),
                            selectedLabelColor = AmberGoldTop,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = dateFilter == filter,
                            borderColor = ObsidianBorder,
                            selectedBorderColor = AmberGoldTop
                        ),
                        modifier = Modifier.testTag("history_date_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // Mode Filter (for Rounds and My Picks)
        if (selectedSection != 2) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ModeChip(
                            label = "All Modes",
                            selected = selectedModeFilter == null,
                            onClick = { selectedModeFilter = null }
                        )
                    }
                    items(GameMode.entries) { mode ->
                        ModeChip(
                            label = mode.title,
                            selected = selectedModeFilter == mode,
                            onClick = { selectedModeFilter = mode }
                        )
                    }
                }
            }
        }

        when (selectedSection) {
            0 -> {
                if (mergedRounds.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "No rounds for this filter",
                            subtitle = "Try selecting All dates or another game duration."
                        )
                    }
                } else {
                    item {
                        Surface(
                            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                            color = AmberOrangeMid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.horizontalGradient(listOf(AmberOrangeMid, AmberOrangeDeep)))
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Period", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2.1f))
                                Text("Number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1.0f))
                                Text("Big Small", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1.1f))
                                Text("Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                            }
                        }
                    }
                    items(mergedRounds.take(50), key = { it.id }) { row ->
                        val digitColor = when (row.resultDigit) {
                            0, 5 -> ElectricViolet
                            1, 3, 7, 9 -> NeonEmerald
                            else -> RubyRed
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianCard,
                            border = BorderStroke(0.6.dp, ObsidianBorder.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inspectedRound = row }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.periodId,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(2.1f)
                                )
                                Text(
                                    text = row.resultDigit.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = digitColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.0f)
                                )
                                Text(
                                    text = digitBigSmallLabel(row.resultDigit),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    WinGoColorDots(digit = row.resultDigit)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianCardElevated,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MiniStat("Entries", filteredPredictions.size.toString(), TextPrimary)
                            MiniStat(
                                "Win Rate",
                                if (settledCount == 0) "0%" else "${(wonCount * 100) / settledCount}%",
                                NeonEmeraldBright
                            )
                            MiniStat("Rewards", "+${formatCoins(totalRewards)} VC", CoinGoldSoft)
                        }
                    }
                }

                if (filteredPredictions.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "No predictions recorded",
                            subtitle = "Jump into any Arena tab to lock virtual color predictions."
                        )
                    }
                } else {
                    items(filteredPredictions, key = { it.id.ifBlank { "${it.modeId}_${it.periodId}" } }) { pred ->
                        PredictionRowCard(prediction = pred)
                    }
                }
            }

            else -> {
                item { VirtualDisclaimerBanner() }

                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "No wallet entries yet",
                            subtitle = "Virtual coin bonuses, prediction entries, and rewards appear here."
                        )
                    }
                } else {
                    items(filteredTransactions, key = { it.id.ifBlank { it.referenceId + it.amount } }) { tx ->
                        WalletTransactionRow(tx = tx)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    inspectedRound?.let { round ->
        RoundVerificationDialog(
            periodId = round.periodId,
            modeTitle = round.mode.title,
            winningColor = round.winningColor,
            resultDigit = round.resultDigit,
            resultHash = round.resultHash,
            completedTimeText = formatEpochMs(round.endTimeMs),
            onDismiss = { inspectedRound = null }
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) CyanPulse.copy(alpha = 0.2f) else ObsidianCard,
        border = BorderStroke(1.dp, if (selected) CyanPulse else ObsidianBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CyanPulse else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun WalletTransactionRow(tx: WalletTransaction) {
    val isCredit = tx.amount > 0L
    val accent = if (isCredit) NeonEmerald else RubyRed

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Column {
                    Text(
                        text = tx.txType.replace("_", " "),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${formatCoins(tx.previousBalance)} -> ${formatCoins(tx.newBalance)} VC • ${formatTimestamp(tx.createdAtMs)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = "${if (isCredit) "+" else ""}${formatCoins(tx.amount)} VC",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isCredit) NeonEmeraldBright else RubyRedBright
            )
        }
    }
}

private fun matchesDateFilter(epochMs: Long, filter: HistoryDateFilter): Boolean {
    if (filter == HistoryDateFilter.ALL) return true
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis
    val startOfYesterday = startOfToday - 86_400_000L
    val startOf7Days = startOfToday - (6L * 86_400_000L)
    return when (filter) {
        HistoryDateFilter.ALL -> true
        HistoryDateFilter.TODAY -> epochMs >= startOfToday
        HistoryDateFilter.YESTERDAY -> epochMs in startOfYesterday until startOfToday
        HistoryDateFilter.LAST_7_DAYS -> epochMs >= startOf7Days
    }
}
