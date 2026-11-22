package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ActiveRoundSnapshot
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.RoundPhase
import com.example.domain.VerifiableRoundOutcome
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeDeep
import com.example.ui.theme.AmberOrangeMid
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.RubyRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Custom Canvas circular clock dial matching the WinGo 4-mode selector bar.
 */
@Composable
fun WinGoClockDialIcon(
    active: Boolean,
    size: Dp = 36.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer glowing halo when active
        drawCircle(
            brush = if (active) {
                Brush.radialGradient(
                    colors = listOf(NeonEmeraldBright, NeonEmerald, Color(0xFF0B5F35)),
                    center = c,
                    radius = r
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xFFD5DCEB), Color(0xFF8C97B5)),
                    center = c,
                    radius = r
                )
            },
            radius = r,
            center = c
        )

        // Inner clock face
        drawCircle(
            color = if (active) Color(0xFFE9FFF3) else Color(0xFFE6EAF5),
            radius = r * 0.72f,
            center = c
        )

        // Clock bezel rim
        drawCircle(
            color = if (active) NeonEmerald else Color(0xFF7B86A6),
            radius = r * 0.72f,
            center = c,
            style = Stroke(width = r * 0.10f)
        )

        val handColor = if (active) Color(0xFF0A6636) else Color(0xFF475569)
        // Hour hand
        drawLine(
            color = handColor,
            start = c,
            end = Offset(c.x - r * 0.25f, c.y - r * 0.22f),
            strokeWidth = r * 0.13f,
            cap = StrokeCap.Round
        )
        // Minute hand
        drawLine(
            color = handColor,
            start = c,
            end = Offset(c.x + r * 0.30f, c.y - r * 0.28f),
            strokeWidth = r * 0.11f,
            cap = StrokeCap.Round
        )
        // Center pin
        drawCircle(color = handColor, radius = r * 0.10f, center = c)
    }
}

/**
 * 4-column WinGo timer mode switcher with circular glowing clock icons.
 */
@Composable
fun WinGoModeSelectorBar(
    selectedMode: GameMode,
    activeRounds: Map<GameMode, ActiveRoundSnapshot>,
    onSelectMode: (GameMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GameMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                val labelTop = "WinGo"
                val labelBottom = when (mode) {
                    GameMode.MODE_30S -> "30sec"
                    GameMode.MODE_60S -> "1 Min"
                    GameMode.MODE_3M -> "3 Min"
                    GameMode.MODE_5M -> "5 Min"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(ObsidianCardElevated, Color(0xFF1F2B52))
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .testTag("arena_mode_tab_${mode.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        WinGoClockDialIcon(active = isSelected, size = 38.dp)
                        Text(
                            text = "$labelTop\n$labelBottom",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Iconic Sunset Amber-Orange Ticket Countdown Card with side cut-out notches,
 * vertical dashed divider, How To Play button, last 5 glossy balls, and 5 white digit tiles.
 */
@Composable
fun WinGoTicketCountdownCard(
    mode: GameMode,
    round: ActiveRoundSnapshot?,
    recentOutcomes: List<VerifiableRoundOutcome>,
    onHowToPlay: () -> Unit,
    onInspectOutcome: (VerifiableRoundOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val countdown = round?.let { ColorPredictionEngine.formatCountdown(it.remainingMs) } ?: "00:00"
    val isLocked = round?.phase == RoundPhase.LOCKED
    val modeLabel = when (mode) {
        GameMode.MODE_30S -> "WinGo 30sec"
        GameMode.MODE_60S -> "WinGo 1 Min"
        GameMode.MODE_3M -> "WinGo 3 Min"
        GameMode.MODE_5M -> "WinGo 5 Min"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(AmberGoldTop, AmberOrangeMid, AmberOrangeDeep)
                )
            )
    ) {
        // Ticket notches & vertical dashed separator
        Canvas(modifier = Modifier.matchParentSize()) {
            val midX = size.width * 0.50f
            val notchRadius = 9.dp.toPx()

            // Top & bottom semicircular ticket notches
            drawCircle(
                color = ObsidianCanvas,
                radius = notchRadius,
                center = Offset(midX, 0f)
            )
            drawCircle(
                color = ObsidianCanvas,
                radius = notchRadius,
                center = Offset(midX, size.height)
            )

            // Dashed vertical divider line
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(midX, notchRadius + 6.dp.toPx()),
                end = Offset(midX, size.height - notchRadius - 6.dp.toPx()),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Half: How To Play + Mode Title + Last 5 Glossy Balls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onHowToPlay,
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("how_to_play_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "How To Play",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recentOutcomes.take(5).forEach { outcome ->
                        WinGoNumberBall(
                            digit = outcome.resultDigit,
                            size = 26.dp,
                            onClick = { onInspectOutcome(outcome) }
                        )
                    }
                }
            }

            // Right Half: Time remaining + 5 Crisp White Flip-Digit Tiles + Period ID
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = if (isLocked) "Round Locked" else "Time remaining",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    countdown.forEach { ch ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(if (ch == ':') 16.dp else 24.dp)
                                    .height(34.dp)
                            ) {
                                Text(
                                    text = ch.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = if (isLocked) RubyRed else AmberOrangeDeep
                                )
                            }
                        }
                    }
                }

                Text(
                    text = round?.periodId ?: "Syncing...",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Reusable 4-Column WinGo History Table with Sunset Amber-Orange Header:
 * Period | Number | Big Small | Color (single/dual dots)
 */
@Composable
fun WinGoHistoryTable(
    outcomes: List<VerifiableRoundOutcome>,
    onInspectOutcome: (VerifiableRoundOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.65f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Orange-Amber Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(AmberOrangeMid, AmberOrangeDeep))
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Period",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(2.1f)
                )
                Text(
                    text = "Number",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.0f)
                )
                Text(
                    text = "Big Small",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.9f)
                )
            }

            outcomes.forEachIndexed { idx, outcome ->
                val digitColor = when (outcome.resultDigit) {
                    0, 5 -> ElectricViolet
                    1, 3, 7, 9 -> NeonEmerald
                    else -> RubyRed
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (idx % 2 == 0) ObsidianCard else ObsidianSurfaceRow)
                        .clickable { onInspectOutcome(outcome) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = outcome.periodId,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(2.1f)
                    )
                    Text(
                        text = outcome.resultDigit.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = digitColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        text = digitBigSmallLabel(outcome.resultDigit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        WinGoColorDots(digit = outcome.resultDigit)
                    }
                }
                if (idx < outcomes.lastIndex) {
                    HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.35f), thickness = 0.6.dp)
                }
            }
        }
    }
}

private val ObsidianSurfaceRow = Color(0xFF1A2243)
